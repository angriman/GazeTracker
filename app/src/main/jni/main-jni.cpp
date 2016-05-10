#include <string.h>
#include <jni.h>
#include "include/opencv2/calib3d.hpp"
#include <opencv2/core/mat.hpp>
#include <opencv2/imgproc/types_c.h>
#include <opencv2/imgproc.hpp>


JNIEXPORT void JNICALL
Java_com_teaminfernale_gazetracker_MainActivity_detectAndDisplay(JNIEnv
*env,
jobject instance,
        jlong
matAddr) {

cv::Mat& frame = *(cv::Mat*)matAddr;
cv::cvtColor( frame, frame, CV_BGR2GRAY );
}

extern "C" {
JNIEXPORT jint JNICALL
    Java_com_teaminfernale_gazetracker_MainActivity_getMessage(JNIEnv *env, jobject instance) {

        return 8;
    }
}
