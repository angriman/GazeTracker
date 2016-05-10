#include <string.h>
#include <jni.h>
#include <opencv2/opencv.hpp>

extern "C" {

JNIEXPORT void JNICALL
Java_com_teaminfernale_gazetracker_MainActivity_filterImage(JNIEnv *env, jobject instance,
                                                            jlong matAddr) {

    cv::Mat& src_img  = *(cv::Mat*)matAddr;
   // cv::GaussianBlur(src_img, src_img, cv::Size(51,3), 80, 3);
//    cv::cvtColor( src_img, src_img, CV_BGR2GRAY );
}

JNIEXPORT jint JNICALL
    Java_com_teaminfernale_gazetracker_MainActivity_getMessage(JNIEnv *env, jobject instance) {

        return 8;
    }
}
