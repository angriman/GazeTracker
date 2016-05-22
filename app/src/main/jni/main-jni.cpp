#include <string.h>
#include <jni.h>
#include <opencv2/opencv.hpp>

extern "C" {

JNIEXPORT void JNICALL
Java_com_teaminfernale_gazetracker_MainActivity_filterImage(JNIEnv *env, jobject instance,
                                                            jobject matAddr) {

    cv::Mat &src_img = *(cv::Mat *) matAddr;
    cv::GaussianBlur(src_img, src_img, cv::Size(51, 3), 80, 3);
    cv::cvtColor(src_img, src_img, CV_BGR2GRAY);
}


/** Function Headers */
void detectAndDisplay( cv::Mat frame );

/** Global variables */
//-- Note, either copy these two files from opencv/data/haarscascades to your current folder, or change these locations
cv::String face_cascade_name = "/haarcascade_frontalface_alt.xml";
cv::CascadeClassifier face_cascade;
std::string main_window_name = "Capture - Face detection";
std::string face_window_name = "Capture - Face";
cv::RNG rng(12345);
cv::Mat debugImage;
cv::Mat skinCrCbHist = cv::Mat::zeros(cv::Size(256, 256), CV_8UC1);


JNIEXPORT int JNICALL
Java_com_teaminfernale_gazetracker_MainActivity_findGaze(JNIEnv *env, jobject instance,
                                                            jobject matAddr) {

    cv::Mat& src_img  = *(cv::Mat*)matAddr;
    cv::Mat frame = src_img;
    if( !face_cascade.load( face_cascade_name ) )
        return -1;
    else
        return 0;
}

/*JNIEXPORT jint JNICALL
    Java_com_teaminfernale_gazetracker_MainActivity_getMessage(JNIEnv *env, jobject instance) {

        return 8;
    }*/
}
