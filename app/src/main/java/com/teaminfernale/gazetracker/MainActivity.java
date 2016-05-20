package com.teaminfernale.gazetracker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String TAG = "MainActivity";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mRgba;
    private Mat mGray;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //prova new branch

                    // Initialize camera
                    mOpenCvCameraView.setCameraIndex(0);
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.enableView();

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        // Load OpenCV for Android
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }


    private Bitmap lena = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

      //  mOpenCvCameraView.setCvCameraViewListener(this);


        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        ((TextView) findViewById(R.id.textView)).setText("" + getMessage());

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        ImageView imageView = (ImageView) findViewById(R.id.fd_activity_surface_view);
        Utils.matToBitmap(mRgba, lena); //BitmapFactory.decodeResource(getResources(), R.drawable.lena1);
        imageView.setImageBitmap(lena);

     //   gaussianBlur();

        return mRgba;
    }

    private void gaussianBlur() {
        Mat targetImage = new Mat();
        Utils.bitmapToMat(lena, targetImage);
        Imgproc.cvtColor(targetImage, targetImage, Imgproc.COLOR_BGR2RGB);

        filterImage(targetImage.getNativeObjAddr());

        Bitmap bitmap = Bitmap.createBitmap(targetImage.cols(), targetImage.rows(), Bitmap.Config.RGB_565);
        Imgproc.cvtColor(targetImage, targetImage, Imgproc.COLOR_RGB2BGR);
        Utils.matToBitmap(targetImage, bitmap);
        ((ImageView) findViewById(R.id.fd_activity_surface_view)).setImageBitmap(bitmap);
    }


    static {
        System.loadLibrary("main-jni");
        System.loadLibrary("opencv_java3");
    }

    public native int getMessage();

    public native void filterImage(long matAddr);

}
