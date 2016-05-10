#include <string.h>
#include <jni.h>
#include "include/opencv2/calib3d.hpp"
#include <opencv2/core/mat.hpp>
#include <opencv2/imgproc/types_c.h>
#include <opencv2/imgproc.hpp>

extern "C" {
JNIEXPORT jint JNICALL
    Java_com_teaminfernale_gazetracker_MainActivity_getMessage(JNIEnv *env, jobject instance) {

        return 8;
    }
}
