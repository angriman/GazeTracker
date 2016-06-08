#include <string.h>
#include <jni.h>
#include <opencv2/opencv.hpp>
#include <bits/stl_queue.h>
#include "constants.h"
#include "helpers.h"

extern "C" {

void scaleToFastSize(const cv::Mat &src,cv::Mat &dst) ;

cv::Mat computeMatXGradient(const cv::Mat &mat) ;

bool floodShouldPushPoint(const cv::Point &np, const cv::Mat &mat) ;

void testPossibleCentersFormula(int x, int y, const cv::Mat &weight,double gx, double gy, cv::Mat &out) ;

cv::Mat floodKillEdges(cv::Mat &mat) ;

cv::Point unscalePoint(cv::Point p, cv::Rect origSize) ;


JNIEXPORT jobject JNICALL
Java_com_teaminfernale_gazetracker_MainActivity_findEyeCenter(JNIEnv *env, jobject instance,
                                                              jlong matAddr, jint x, jint y, jint width, jint height) {

    // TODO
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
    // draw eye region
    rectangle(face,eye,1234);
    //-- Find the gradient
    cv::Mat gradientX = computeMatXGradient(eyeROI);
    cv::Mat gradientY = computeMatXGradient(eyeROI.t()).t();
    //-- Normalize and threshold the gradient
    // compute all the magnitudes
    cv::Mat mags = matrixMagnitude(gradientX, gradientY);
    //compute the threshold
    double gradientThresh = computeDynamicThreshold(mags, kGradientThreshold);
    //double gradientThresh = kGradientThreshold;
    //double gradientThresh = 0;
    //normalize
    for (int y1 = 0; y1 < eyeROI.rows; ++y1) {
        double *Xr = gradientX.ptr<double>(y1), *Yr = gradientY.ptr<double>(y1);
        const double *Mr = mags.ptr<double>(y1);
        for (int x1 = 0; x1 < eyeROI.cols; ++x1) {
            double gX = Xr[x1], gY = Yr[x1];
            double magnitude = Mr[x1];
            if (magnitude > gradientThresh) {
                Xr[x1] = gX/magnitude;
                Yr[x1] = gY/magnitude;
            } else {
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
            row[x1] = (255 - row[x1]);
        }
    }
    //imshow(debugWindow,weight);
    //-- Run the algorithm!
    cv::Mat outSum = cv::Mat::zeros(eyeROI.rows,eyeROI.cols,CV_64F);
    // for each possible gradient location
    // Note: these loops are reversed from the way the paper does them
    // it evaluates every possible center for each gradient location instead of
    // every possible gradient location for every center.
//    printf("Eye Size: %ix1%i\n",outSum.cols,outSum.rows);
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
    // scale all the values down, basically averaging them
    double numGradients = (weight.rows*weight.cols);
    cv::Mat out;
    outSum.convertTo(out, CV_32F,1.0/numGradients);
    //imshow(debugWindow,out);
    //-- Find the maximum point
    cv::Point maxP;
    double maxVal;
    cv::minMaxLoc(out, NULL,&maxVal,NULL,&maxP);
    //-- Flood fill the edges
    if(kEnablePostProcess) {
        cv::Mat floodClone;
        //double floodThresh = computeDynamicThreshold(out, 1.5);
        double floodThresh = maxVal * kPostProcessThreshold;
        cv::threshold(out, floodClone, floodThresh, 0.0f, cv::THRESH_TOZERO);
        if(kPlotVectorField) {
            //plotVecField(gradientX, gradientY, floodClone);
            imwrite("eyeFrame.png",eyeROIUnscaled);
        }
        cv::Mat mask = floodKillEdges(floodClone);
        //imshow(debugWindow + " Mask",mask);
        //imshow(debugWindow,out);
        // redo max
        cv::minMaxLoc(out, NULL,&maxVal,NULL,&maxP,mask);
    }
    cv::Point eyeCenter = unscalePoint(maxP,eye);

    jint fill[2];
    fill[0] = eyeCenter.x;
    fill[1] = eyeCenter.y;
    (env)->SetIntArrayRegion(result, 0, 2, fill);
    return result;
}

void scaleToFastSize(const cv::Mat &src,cv::Mat &dst) {
    cv::resize(src, dst, cv::Size(kFastEyeWidth,(((float)kFastEyeWidth)/src.cols) * src.rows));
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
    // for all possible centers
    for (int cy = 0; cy < out.rows; ++cy) {
        double *Or = out.ptr<double>(cy);
        const unsigned char *Wr = weight.ptr<uchar>(cy);
        for (int cx = 0; cx < out.cols; ++cx) {
            if (x == cx && y == cy) {
                continue;
            }
            // create a vector from the possible center to the gradient origin
            double dx = x - cx;
            double dy = y - cy;
            // normalize d
            double magnitude = sqrt((dx * dx) + (dy * dy));
            dx = dx / magnitude;
            dy = dy / magnitude;
            double dotProduct = dx*gx + dy*gy;
            dotProduct = std::max(0.0,dotProduct);
            // square and multiply by the weight
            if (kEnableWeight) {
                Or[cx] += dotProduct * dotProduct * (Wr[cx]/kWeightDivisor);
            } else {
                Or[cx] += dotProduct * dotProduct;
            }
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
        // add in every direction
        cv::Point np(p.x + 1, p.y); // right
        if (floodShouldPushPoint(np, mat)) toDo.push(np);
        np.x = p.x - 1; np.y = p.y; // left
        if (floodShouldPushPoint(np, mat)) toDo.push(np);
        np.x = p.x; np.y = p.y + 1; // down
        if (floodShouldPushPoint(np, mat)) toDo.push(np);
        np.x = p.x; np.y = p.y - 1; // up
        if (floodShouldPushPoint(np, mat)) toDo.push(np);
        // kill it
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
    int x = round(p.x / ratio);
    int y = round(p.y / ratio);
    return cv::Point(x,y);
}

JNIEXPORT void JNICALL
Java_com_teaminfernale_gazetracker_MainActivity_filterImage(JNIEnv *env, jobject instance,
                                                            jlong matAddr) {

    cv::Mat &src_img = *(cv::Mat *) matAddr;
    cv::GaussianBlur(src_img, src_img, cv::Size(51, 3), 80, 3);
//    cv::cvtColor( src_img, src_img, CV_BGR2GRAY );
}

JNIEXPORT jint JNICALL
Java_com_teaminfernale_gazetracker_MainActivity_getMessage(JNIEnv *env, jobject instance) {

    return 8;
}


}
