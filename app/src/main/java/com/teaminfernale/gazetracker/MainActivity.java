package com.teaminfernale.gazetracker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.teaminfernale.gazetracker.MenuActivity.Algorithm;


public abstract class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";
    private static final String TAG2 = "MainActivity_lifeCycle";
    public static final int JAVA_DETECTOR = 0;
    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;

//    protected TrainedEyesContainer mTrainedEyesContainer = new TrainedEyesContainer();
//    private  GazeCalculator mGazeCalculator;
//    private boolean calibrating = false;
//    private int calibration_phase = 0;
    private int learn_frames = 0;
    private Mat teplateR;
    private Mat teplateL;

    private int method = TM_CCOEFF;

    private MenuItem mItemFace50;
    private MenuItem mItemFace40;
    private MenuItem mItemFace30;
    private MenuItem mItemFace20;
    private MenuItem mItemType;

    private Mat mRgba;
    private Mat mGray;
    // matrix for zooming
    private Mat mZoomWindow;
    private Mat mZoomWindow2;

    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorEye;


    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;

    private int mode = 0;
    private Algorithm mAlgorithm;


    protected abstract void onEyeFound(Point leftEye, Point rightEye, Bitmap le, Bitmap re);

    public void setModeRecognition() {
        mode = 1;
    }

    public void setMethod(int method) {

        this.method = method;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");


                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(cascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // Load left eye classificator
                        InputStream iser = getResources().openRawResource(
                                R.raw.haarcascade_lefteye_2splits);
                        File cascadeDirER = getDir("cascadeER",
                                Context.MODE_PRIVATE);
                        File cascadeFileER = new File(cascadeDirER,
                                "haarcascade_eye_right.xml");
                        FileOutputStream oser = new FileOutputStream(cascadeFileER);

                        byte[] bufferER = new byte[4096];
                        int bytesReadER;
                        while ((bytesReadER = iser.read(bufferER)) != -1) {
                            oser.write(bufferER, 0, bytesReadER);
                        }
                        iser.close();
                        oser.close();

                        mJavaDetector = new CascadeClassifier(
                                cascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from "
                                    + cascadeFile.getAbsolutePath());

                        mJavaDetectorEye = new CascadeClassifier(
                                cascadeFileER.getAbsolutePath());
                        if (mJavaDetectorEye.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetectorEye = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    //TODO Sistemare questi casini con la fotocamera
                    mOpenCvCameraView.setCameraIndex(0);
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

    public MainActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public Algorithm getAlgorithm() {
        return mAlgorithm;
    }

    //Serve affinchè venga settato il layout con la fd_activity_surface_view per poter lanciare la fotocamera
    //comando setContentView(R.layout.XXX_activity_layout);
    protected abstract void setLayout();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG2, "MainActivity onCreate() called");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setLayout();

        // Extracting extra from intent
        mAlgorithm = (Algorithm) getIntent().getSerializableExtra("algorithm");
        if (mAlgorithm == null) {
            mAlgorithm = Algorithm.JAVA;
        }

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        switch (mode) {
            case 0:
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
                break;
            default:
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.recognition_front_camera_view);
        }

        if (mOpenCvCameraView == null)
            Log.i(TAG, "Capito er bug");
        mOpenCvCameraView.setCvCameraViewListener(this);
        Log.i(TAG, "camera view cameraview initializated");

    }

    /**
     * Disables the camera to let a different activity the possibility
     * to use it without errors
     */
    public void closeCamera() {
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        Log.i(TAG, "Camera closed");
    }

    @Override
    public void onStart() { //Called when the activity is becoming visible to the user
        super.onStart();
        Log.i(TAG2, "Main Activity onStart() called");
    }

    @Override
    public void onStop() { //Called when the activity is no longer visible to the user
        super.onStop();
        Log.i(TAG2, "Main Activity onStop() called");
    }

    @Override
    public void onRestart() { //Called after the activity has been stopped, prior to it being started again
        super.onRestart();
        Log.i(TAG2, "Main Activity onRestart() called");
    }

    @Override
    public void onPause() {
        super.onPause();
        closeCamera();
        Log.i(TAG2, "Main Activity onPause() called");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        Log.i(TAG2, "Main Activity onResume() called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeCamera();
        Log.i(TAG2, "Main Activity onDestroy() called");
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mZoomWindow.release();
        mZoomWindow2.release();
    }

    /**
     * Called when a new frame has been recorded by the OpenCV Camera
     * @param inputFrame represents the recorded frame
     */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        if (mZoomWindow == null || mZoomWindow2 == null) {
            CreateAuxiliaryMats();
        }

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null) {
            mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }

        Point lMatchedEye = new Point();
        Point rMatchedEye = new Point();

        // Vector containing the recognized faces
        // inside the frame
        Rect[] facesArray = faces.toArray();

        if (facesArray.length > 0) {

            // Rectangle of the first face
            Rect faceRect = facesArray[0];
            int eyeAreaMargin = 16;

            Rect eyearea_right = new Rect(faceRect.x + faceRect.width / eyeAreaMargin, (int) (faceRect.y + (faceRect.height / 4.5)), (faceRect.width - 2 * faceRect.width / eyeAreaMargin) / 2, (int) (faceRect.height / 3.0));
            Rect eyearea_left = new Rect(faceRect.x + faceRect.width / eyeAreaMargin + (faceRect.width - 2 * faceRect.width / eyeAreaMargin) / 2, (int) (faceRect.y + (faceRect.height / 4.5)), (faceRect.width - 2 * faceRect.width / 16) / 2, (int) (faceRect.height / 3.0));

            if (learn_frames < 5) {
                teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
                teplateL = get_template(mJavaDetectorEye, eyearea_left, 24);
                learn_frames++;
            }
            else {
                // Learning finished, use the new templates for template matching

                switch (mAlgorithm) {
                    case JAVA:
                        lMatchedEye = match_eye(eyearea_left, teplateL, method, mJavaDetectorEye);
                        rMatchedEye = match_eye(eyearea_right, teplateR, method, mJavaDetectorEye);
                        Log.i(TAG, "Matched eye java");
                        break;
                    case CPP:
                        lMatchedEye = cpp_match_eye(eyearea_left);
                        rMatchedEye = cpp_match_eye(eyearea_right);
                        Log.i(TAG, "Matched eye CPP");
                        break;
                    default:
                        break;
                }

                String pointTag = "PointTag";
                if (lMatchedEye != null)
                    Log.i(pointTag, "Center CPP = ("+lMatchedEye.x+","+lMatchedEye.y+")");
                }

            // Cut eye areas and put them to zoom windows
            Imgproc.resize(mRgba.submat(eyearea_right), mZoomWindow, mZoomWindow.size());
            Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2, mZoomWindow2.size());
        }


        // On a separate thread it converts the eye mat into a bitmap
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

        final Point finalLMatchedEye = lMatchedEye;
        final Point finalRMatchedEye = rMatchedEye;

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {

                try {
                    Bitmap le = Bitmap.createBitmap(mZoomWindow.cols(), mZoomWindow.rows(), Bitmap.Config.ARGB_8888);
                    Bitmap re = Bitmap.createBitmap(mZoomWindow.cols(), mZoomWindow.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mZoomWindow.clone(), le);
                    Utils.matToBitmap(mZoomWindow2.clone(), re);
                    if (finalLMatchedEye != null && finalRMatchedEye != null)
                        onEyeFound(finalLMatchedEye, finalRMatchedEye, le, re);
                }
                catch (IllegalArgumentException e) {
                    Log.i(TAG, "EXCEPTION");
                }
            }
        };

        mainHandler.post(myRunnable);

        return mRgba;
    }

    public void setAlgorithm(Algorithm algorithm) {
        mAlgorithm = algorithm;
    }

    private Point cpp_match_eye(Rect area){

        String t = "PointTag";
        int[] result = findEyeCenter(mGray.getNativeObjAddr(), area.x, area.y, area.width, area.height);
        //Log.i(t, "mZoomWindow dimensions -> cols =" + mZoomWindow.cols())
        //Bitmap le = Bitmap.createBitmap(mZoomWindow.cols(), mZoomWindow.rows(), Bitmap.Config.ARGB_8888);
        //Bitmap re = Bitmap.createBitmap(mZoomWindow.cols(), mZoomWindow.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(mZoomWindow.clone(), le);
        //Utils.matToBitmap(mZoomWindow2.clone(), re);
        //Point leftEye = new Point(result[0]+area.x, result[1]+area.y);
        //Imgproc.circle(mGray, leftEye, 5, new Scalar(0, 255, 255, 255));
        //onEyeFound(leftEye, leftEye, le, re);

        return new Point(result[0], result[1]);
    }


    /**
     * Called to recognize eyes after training is complete. It uses the Java algorithm
     * with calls to OpenCV.
     * @param area Rectangle of the face
     * @param mTemplate Template Mat used by the algorithm to match eyes from the
     *                  original image
     * @param type Method used to recognize the eye (SQDIFF, CCOEFF...)
     * @param clasificator Cascade classificator used by the algorithm to detect
     *                     the eye
     */
    private Point match_eye(Rect area, Mat mTemplate, int type, CascadeClassifier clasificator) {
        //Point matchLoc;
        Mat mROI = mGray.submat(area);
        int result_cols = mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;

        // Check for bad template size
        if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
            return null;
        }

        Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

        switch (type) {
            case TM_SQDIFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
                break;
            case TM_SQDIFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF_NORMED);
                break;
            case TM_CCOEFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
                break;
            case TM_CCOEFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF_NORMED);
                break;
            case TM_CCORR:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
                break;
            case TM_CCORR_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR_NORMED);
                break;
        }


        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        clasificator.detectMultiScale(mROI, eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30), new Size());

        // It is > 0 if it detects an eye (I suppose). Recorded value: 0 or 1
        Rect[] eyesArray = eyes.toArray();

        if (eyesArray.length > 0) {
            Rect e = eyesArray[0];
            e.x += area.x;
            e.y += area.y;

            Rect eye_only_rectangle = new Rect((int) e.tl().x, (int) (e.tl().y + e.height * 0.4), e.width, (int) (e.height * 0.6));
            Mat mROI2 = mGray.submat(eye_only_rectangle);
            Mat yyrez = mRgba.submat(eye_only_rectangle);

            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI2);
            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            Imgproc.circle(yyrez, mmG.minLoc, 1, new Scalar(255, 255, 255, 255), 1);
            //Log.i(TAG, (eye == 0) ? "Left" : "Right" + " eye detected\t Center = ( " + mmG.minLoc.x + ", " + mmG.minLoc.y + " )");


            return mmG.minLoc;
            // Prendere un punto di riferimento del rettangolo e tracciare i movimenti dell'iride rispetto a quel punto
            // Tracciare i vari punti sullo schermo
        }

        return null;
    }

    // First 6 frames to train the model
    private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        Rect eye_template;
        clasificator.detectMultiScale(mROI, eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30), new Size());

        Rect[] eyesArray = eyes.toArray();
        if (eyesArray.length > 0) {
            Rect e = eyesArray[0];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x, (int) (e.tl().y + e.height * 0.4), (int) e.width, (int) (e.height * 0.6));
            mROI = mGray.submat(eye_only_rectangle);
            Mat vyrez = mRgba.submat(eye_only_rectangle);

            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            // Draws a point in the center of the eye
            //Imgproc.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            eye_template = new Rect((int) iris.x - size / 2, (int) iris.y - size / 2, size, size);

            // Draws a red rectangle around the center of the eye
           // Imgproc.rectangle(mRgba, eye_template.tl(), eye_template.br(), new Scalar(255, 0, 0, 255), 2);
            template = (mGray.submat(eye_template)).clone();

            return template;
        }

        return template;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void CreateAuxiliaryMats() {
        if (mGray.empty())
            return;

        int rows = mGray.rows();
        int cols = mGray.cols();

        if (mZoomWindow == null) {
            mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2 + cols / 10, cols);
            mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2 + cols / 10, cols);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        mItemType = menu.add(mDetectorName[mDetectorType]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50)
            setMinFaceSize(0.5f);
        else if (item == mItemFace40)
            setMinFaceSize(0.4f);
        else if (item == mItemFace30)
            setMinFaceSize(0.3f);
        else if (item == mItemFace20)
            setMinFaceSize(0.2f);
        else if (item == mItemType) {
            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
            item.setTitle(mDetectorName[tmpDetectorType]);
        }
        return true;
    }

    public void onRecreateClick(View v) {
        learn_frames = 0;
    }

    static {
        System.loadLibrary("main-jni");
    }

    private native int[] findEyeCenter(long matAddr, int x, int y, int width, int height);

}
