#include <string.h>
#include <jni.h>
#include <opencv2/opencv.hpp>
#include <bits/stl_queue.h>
#include "constants.h"
#include "helpers.h"

extern "C" {

/**
 * Methods declarations
 */

void scaleToFastSize(const cv::Mat &src,cv::Mat &dst) ;

cv::Mat computeMatXGradient(const cv::Mat &mat) ;

bool floodShouldPushPoint(const cv::Point &np, const cv::Mat &mat) ;

void testPossibleCentersFormula(int x, int y, const cv::Mat &weight,double gx, double gy, cv::Mat &out) ;

cv::Mat floodKillEdges(cv::Mat &mat) ;

cv::Point unscalePoint(cv::Point p, cv::Rect origSize) ;


JNIEXPORT jobject JNICALL
Java_com_teaminfernale_gazetracker_MainActivity_findEyeCenter(JNIEnv *env, jobject instance,
                                                              jlong matAddr, jint x, jint y, jint width, jint height) {

    cv::Mat &face = *(cv::Mat*)matAddr;
    jintArray result;
    result = (env)->NewIntArray(2);

    if (result == NULL) {
        return NULL; /* out of memory error thrown */
    }

    cv::Rect eye(x, y, width, height);

    cv::Mat eyeROIUnscaled = face(eye);
    cv::Mat eyeROI;
    scaleToFastSize(eyeROIUnscaled, eyeROI);

    //-- Find the gradient
    cv::Mat gradientX = computeMatXGradient(eyeROI);
    cv::Mat gradientY = computeMatXGradient(eyeROI.t()).t();

    //-- Normalize and threshold the gradient
    // compute all the magnitudes
    cv::Mat mags = matrixMagnitude(gradientX, gradientY);

    // Compute the threshold
    double gradientThresh = computeDynamicThreshold(mags, kGradientThreshold);

    for (int y1 = 0; y1 < eyeROI.rows; ++y1) {
        double *Xr = gradientX.ptr<double>(y1), *Yr = gradientY.ptr<double>(y1);
        const double *Mr = mags.ptr<double>(y1);
        for (int x1 = 0; x1 < eyeROI.cols; ++x1) {
            double gX = Xr[x1], gY = Yr[x1];
            double magnitude = Mr[x1];
            if (magnitude > gradientThresh) {
                Xr[x1] = gX/magnitude;
                Yr[x1] = gY/magnitude;
            }
            else {
                Xr[x1] = 0.0;
                Yr[x1] = 0.0;
            }
        }
    }

    cv::Mat weight;

    GaussianBlur( eyeROI, weight, cv::Size( kWeightBlurSize, kWeightBlurSize ), 0, 0 );
    for (int y1 = 0; y1 < weight.rows; ++y1) {
        unsigned char *row = weight.ptr<uchar>(y1);
        for (int x1 = 0; x1 < weight.cols; ++x1) {
            row[x1] = (unsigned char) (255 - row[x1]);
        }
    }

    //-- Run the algorithm!
    cv::Mat outSum = cv::Mat::zeros(eyeROI.rows,eyeROI.cols,CV_64F);
    for (int y1 = 0; y1 < weight.rows; ++y1) {
        const double *Xr = gradientX.ptr<double>(y1), *Yr = gradientY.ptr<double>(y1);
        for (int x1 = 0; x1 < weight.cols; ++x1) {
            double gX = Xr[x1], gY = Yr[x1];
            if (gX == 0.0 && gY == 0.0) {
                continue;
            }
            testPossibleCentersFormula(x1, y, weight, gX, gY, outSum);
        }
    }

    // Scale all the values down, basically averaging them
    double numGradients = (weight.rows*weight.cols);
    cv::Mat out;
    outSum.convertTo(out, CV_32F,1.0/numGradients);

    //-- Find the maximum point
    cv::Point maxP;
    double maxVal;
    cv::minMaxLoc(out, NULL, &maxVal, NULL, &maxP);

    //-- Flood fill the edges
        cv::Mat floodClone;
        double floodThresh = maxVal * kPostProcessThreshold;
        cv::threshold(out, floodClone, floodThresh, 0.0f, cv::THRESH_TOZERO);

        cv::Mat mask = floodKillEdges(floodClone);

        // Redo max
        cv::minMaxLoc(out, NULL, &maxVal, NULL, &maxP, mask);

    cv::Point eyeCenter = unscalePoint(maxP, eye);

    jint fill[2];
    fill[0] = eyeCenter.x;
    fill[1] = eyeCenter.y;
    (env)->SetIntArrayRegion(result, 0, 2, fill);
    return result;
}

void scaleToFastSize(const cv::Mat &src,cv::Mat &dst) {
    cv::resize(src, dst, cv::Size(kFastEyeWidth,
                                  (int) ((((float)kFastEyeWidth) / src.cols) * src.rows)));
}

cv::Mat computeMatXGradient(const cv::Mat &mat) {
    cv::Mat out(mat.rows,mat.cols,CV_64F);

    for (int y = 0; y < mat.rows; ++y) {
        const uchar *Mr = mat.ptr<uchar>(y);
        double *Or = out.ptr<double>(y);

        Or[0] = Mr[1] - Mr[0];
        for (int x = 1; x < mat.cols - 1; ++x) {
            Or[x] = (Mr[x+1] - Mr[x-1])/2.0;
        }

        Or[mat.cols-1] = Mr[mat.cols-1] - Mr[mat.cols-2];
    }

    return out;
}

void testPossibleCentersFormula(int x, int y, const cv::Mat &weight,double gx, double gy, cv::Mat &out) {

    // For all possible centers
    for (int cy = 0; cy < out.rows; ++cy) {
        double *Or = out.ptr<double>(cy);
        const unsigned char *Wr = weight.ptr<uchar>(cy);
        for (int cx = 0; cx < out.cols; ++cx) {

            if (x == cx && y == cy) {
                continue;
            }

            // Create a vector from the possible center to the gradient origin
            double dx = x - cx;
            double dy = y - cy;

            // Normalize d
            double magnitude = sqrt((dx * dx) + (dy * dy));
            dx = dx / magnitude;
            dy = dy / magnitude;

            double dotProduct = dx*gx + dy*gy;
            dotProduct = std::max(0.0,dotProduct);

            // Square and multiply by the weight
            Or[cx] += dotProduct * dotProduct * (Wr[cx]/kWeightDivisor);
        }
    }
}

cv::Mat floodKillEdges(cv::Mat &mat) {
    rectangle(mat,cv::Rect(0,0,mat.cols,mat.rows),255);

    cv::Mat mask(mat.rows, mat.cols, CV_8U, 255);
    std::queue<cv::Point> toDo;
    toDo.push(cv::Point(0,0));

    while (!toDo.empty()) {
        cv::Point p = toDo.front();
        toDo.pop();

        if (mat.at<float>(p) == 0.0f) {
            continue;
        }

        // Add in every direction
        cv::Point np(p.x + 1, p.y); // right
        if (floodShouldPushPoint(np, mat)) toDo.push(np);
        np.x = p.x - 1; np.y = p.y; // left
        if (floodShouldPushPoint(np, mat)) toDo.push(np);
        np.x = p.x; np.y = p.y + 1; // down
        if (floodShouldPushPoint(np, mat)) toDo.push(np);
        np.x = p.x; np.y = p.y - 1; // up
        if (floodShouldPushPoint(np, mat)) toDo.push(np);

        // Kill it
        mat.at<float>(p) = 0.0f;
        mask.at<uchar>(p) = 0;
    }
    return mask;
}

bool floodShouldPushPoint(const cv::Point &np, const cv::Mat &mat) {
    return inMat(np, mat.rows, mat.cols);
}

cv::Point unscalePoint(cv::Point p, cv::Rect origSize) {
    float ratio = (((float)kFastEyeWidth)/origSize.width);
    int x = (int) round(p.x / ratio);
    int y = (int) round(p.y / ratio);
    return cv::Point(x,y);
}


}
