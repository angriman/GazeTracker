package com.teaminfernale.gazetracker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

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
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    private Bitmap lena = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        lena = BitmapFactory.decodeResource(getResources(), R.drawable.lena1);
        imageView.setImageBitmap(lena);

        gaussianBlur();

        ((TextView) findViewById(R.id.textView)).setText("" + getMessage());

    }

    private Mat targetImage = null;

    private void gaussianBlur() {
        targetImage = new Mat();
        Utils.bitmapToMat(lena, targetImage);
        Imgproc.cvtColor(targetImage, targetImage, Imgproc.COLOR_BGR2RGB);

        detectAndDisplay(targetImage.getNativeObjAddr());

        Bitmap bitmap = Bitmap.createBitmap(targetImage.cols(), targetImage.rows(), Bitmap.Config.RGB_565);
        Imgproc.cvtColor(targetImage, targetImage, Imgproc.COLOR_RGB2BGR);
        Utils.matToBitmap(targetImage, bitmap);
        ((ImageView) findViewById(R.id.imageView)).setImageBitmap(bitmap);
    }


    static {
        System.loadLibrary("main-jni");
    }

    public native int getMessage();
    public native void detectAndDisplay(long matAddr);

    //public native void gaussianBlur(long matAddr);
}
