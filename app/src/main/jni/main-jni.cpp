#include <string.h>
#include <jni.h>
#include <opencv2/opencv.hpp>

JNIEXPORT void JNICALL
Java_com_teaminfernale_gazetracker_MainActivity_findEyeCenter(JNIEnv *env, jobject instance,
                                                              jobject jface, jobject jeye, jobject center) {

    cv::Mat* face = (cv::Mat*)jface;
    cv::Rect* eye = (cv::Rect*)jeye;
    cv::Point p = *((cv::Point*)center);
    p.x = 3;
    p.y = 4;
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_teaminfernale_gazetracker_MainActivity_filterImage(JNIEnv *env, jobject instance,
                                                            jlong matAddr) {

    cv::Mat& src_img  = *(cv::Mat*)matAddr;
    cv::GaussianBlur(src_img, src_img, cv::Size(51,3), 80, 3);
//    cv::cvtColor( src_img, src_img, CV_BGR2GRAY );
}

JNIEXPORT jint JNICALL
    Java_com_teaminfernale_gazetracker_MainActivity_getMessage(JNIEnv *env, jobject instance) {

        return 8;
    }
}
