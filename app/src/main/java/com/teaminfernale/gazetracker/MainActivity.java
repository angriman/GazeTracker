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
import java.util.ArrayList;

import static com.teaminfernale.gazetracker.MenuActivity.Algorithm;

/**
 * Abstract activity that starts the opencv camera view and find the points of the eyes
 * found in the frames. Then in the implementation you can decide what to do with these informations.
 */
public abstract class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    /**
     * Debug tags
     */
    private static final String TAG = "MainActivity";
    private static final String TAG2 = "MainActivity_lifeCycle";
    private static final String TAG3 = "ZoomedWindow";

    /**
     * Index that tells what camera to open (0 := back, 1 := frontal).
     * We want to use the frontal camera in the phone (so index = 1).
     * Since OpenCv camera has a bug, when running the application in the virtual device the index
     * needs to be set to 0.
     */
    private static final int cameraIndex = 1;

    /**
     * Debug variable
     */
    private static final boolean DEBUG = false;

    public static final int JAVA_DETECTOR = 0;

    /**
     * Java eye recognition method identifiers.
     */
    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;

    /**
     * Default chosen method
     */
    private int method = TM_CCOEFF;

    /**
     * Number of frames to be used for the learning phase
     */
    private static final int mLearningFrames = 5;

    /**
     * Number of used frames during the learning phase.
     */
    private int learn_frames = 0;

    /**
     * Number of frames that can be lost after eyes recognized
     * before restarting the learning phase.
     */
    private static final int MISSED_FRAMES_LIMIT = 10;

    /**
     * Templates used to recognize the right and the left eye.
     */
    private Mat teplateR;
    private Mat teplateL;

    private MenuItem mItemFace50;
    private MenuItem mItemFace40;
    private MenuItem mItemFace30;
    private MenuItem mItemFace20;
    private MenuItem mItemType;

    /**
     * Matrix representing the colored frame captured by the camera.
     */
    private Mat mRgba;

    /**
     * Matrix representing the grey scale frame captured by the camera.
     */
    private Mat mGray;

    /**
     * Matrices used to zoom on the left and right eye.
     */
    private Mat mZoomWindow;
    private Mat mZoomWindow2;

    /**
     * Classifiers used to recognize the face and the eyes.
     */
    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorEye;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;

    /**
     * Instance of the OpenCV camera used to capture frames from the camera
     * without using the standard Camera class.
     */
    private CameraBridgeViewBase mOpenCvCameraView;

    /**
     * Camera mode.
     */
    private int mode = 0;

    /**
     * Currently used algorithm to recognize eyes (Java or C++).
     */
    private Algorithm mAlgorithm;

    /**
     * If both eyes have been detected after the training phase.
     */
    private boolean eyesFound = false;
    private int missedFrames = 0;

    /**
     * Thread handler to manage threads.
     */
    Handler mainHandler;

    /**
     * List of threads used to terminate unfinished threads
     */
    ArrayList<Runnable> threadList = new ArrayList<>();



    /**
     * Called when calibration can take place
     */
    protected abstract void updateUI();

    /**
     * Called when in a new frame both the eyes have been found. Decides what to do with the
     * eyes-position information.
     * @param leftEye left eye coordinates
     * @param rightEye right eye coordinates
     * @param le left eye image
     * @param re right eye image
     */
    protected abstract void onEyeFound(Point leftEye, Point rightEye, Bitmap le, Bitmap re);

    /**
     * Change the mode in 1 (meaning that the app is in not in calibration anymore and the
     * recognition is started).
     */
    public void setModeRecognition() {
        mode = 1;
    }

    /**
     * Set the method used for Java eye recognition
     */
    public void setMethod(int method) { this.method = method; }


    /**
     * OpenCV static loader
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    if (DEBUG) Log.i(TAG, "OpenCV loaded successfully");
                    try {
                        // Load cascade file from application resources
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

                        // Load left eye classifier
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
                            if(DEBUG) Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                        if(DEBUG) Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

                        mJavaDetectorEye = new CascadeClassifier(
                                cascadeFileER.getAbsolutePath());
                        if (mJavaDetectorEye.empty()) {
                            if(DEBUG) Log.e(TAG, "Failed to load cascade classifier");

                            mJavaDetectorEye = null;
                        } else
                            if(DEBUG) Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        if(DEBUG) Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.setCameraIndex(cameraIndex);
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

    /**
     * Getter method for eyesFound variable
     */
    public boolean eyesFound() {
        return eyesFound;
    }

    /**
     * Getter method for currently used algorithm mAlgorithm variable
     */
    public Algorithm getAlgorithm() {
        return mAlgorithm;
    }

    /**
     * Setter method for the variable mAlgorithm. Used to let other activities to
     * switch the algorithm used to recognize eyes.
     */
    public void setAlgorithm(Algorithm algorithm) {
        mAlgorithm = algorithm;
    }

    /**
     * Main constructor
     */
    public MainActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        if(DEBUG) Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called at the creation of the activity. Needs to set the content wiew inside.
     */
    protected abstract void setLayout();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(DEBUG) Log.i(TAG2, "MainActivity onCreate() called");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setLayout();

        // Extracting extra from intent
        mAlgorithm = (Algorithm) getIntent().getSerializableExtra("algorithm");
        if (mAlgorithm == null) {
            mAlgorithm = Algorithm.JAVA;
        }

        mainHandler= new Handler(getApplicationContext().getMainLooper());
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        switch (mode) {
            case 0:
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
                break;
            default:
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.recognition_front_camera_view);
        }

        if (mOpenCvCameraView == null)
            if(DEBUG) Log.i(TAG, "Capito er bug");
        mOpenCvCameraView.setCvCameraViewListener(this);
        if(DEBUG) Log.i(TAG, "camera view cameraview initializated");

    }


    /**
     * Disables the camera to let a different activity the possibility
     * to use it without errors
     */
    public void closeCamera() {
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        if(DEBUG) Log.i(TAG, "Camera closed");
    }

    /**
     * Called when the activity is becoming visible to the user.
     * */
    @Override
    public void onStart() {
        super.onStart();
        if(DEBUG) Log.i(TAG2, "Main Activity onStart() called");
    }

    /**
     * Called when the activity is no longer visible to the user.
     */
    @Override
    public void onStop() {
        super.onStop();
        if(DEBUG) Log.i(TAG2, "Main Activity onStop() called");
    }

    /**
     * Called after the activity has been stopped, prior to it being started again.
     * */
    @Override
    public void onRestart() {
        super.onRestart();
        if(DEBUG) Log.i(TAG2, "Main Activity onRestart() called");
    }

    @Override
    public void onPause() {
        super.onPause();
        if(DEBUG) Log.i(TAG2, "Main Activity onPause() called");
        closeCamera();

        //Close all the opened threads
        for (int i = 0; i < threadList.size(); ++i){
            Runnable currentR = threadList.get(i);
            mainHandler.removeCallbacks(currentR);
        }
        if(DEBUG) Log.i(TAG2, "All threads closed!");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            if(DEBUG) Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            if(DEBUG) Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        if(DEBUG) Log.i(TAG2, "Main Activity onResume() called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(DEBUG) Log.i(TAG2, "Main Activity onDestroy() called");
        closeCamera();
    }

    /**
     * Called when the OpenCV camera view has started. Initializes the two matrices
     * which represent the input frame mGray and mRgba.
     * @param width width in pixels if the input frame
     * @param height height in pixels of the input frame
     */
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    /**
     * Called when the OpenCV camera view has stopped. It releases the matrices
     * used for the input frame and to zoom to the eyes.
     */
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

        // Updates the input frame matrices
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        // Initializes the matrices for the eyes if not created yet.
        if (mZoomWindow == null || mZoomWindow2 == null) {
            CreateAuxiliaryMats();
        }
        if (mZoomWindow.empty() || mZoomWindow2.empty()) {
            CreateAuxiliaryMats();
        }

        MatOfRect faces = new MatOfRect();

        // Initializes the face detector
        if (mJavaDetector != null) {
            mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }

        // Variables for pupils center coordinates
        Point lMatchedEye = new Point();
        Point rMatchedEye = new Point();

        // Vector containing the recognized faces
        // inside the frame
        Rect[] facesArray = faces.toArray();

        // At least one face detected
        if (facesArray.length > 0) {

            // Rectangle of the first face detected
            Rect faceRect = facesArray[0];

            // Builds a rectangle to cut the eyes area
            // inside the face rectangle
            int eyeAreaMargin = 16;
            Rect eyearea_right = new Rect(faceRect.x + faceRect.width / eyeAreaMargin, (int) (faceRect.y + (faceRect.height / 4.5)), (faceRect.width - 2 * faceRect.width / eyeAreaMargin) / 2, (int) (faceRect.height / 3.0));
            Rect eyearea_left = new Rect(faceRect.x + faceRect.width / eyeAreaMargin + (faceRect.width - 2 * faceRect.width / eyeAreaMargin) / 2, (int) (faceRect.y + (faceRect.height / 4.5)), (faceRect.width - 2 * faceRect.width / 16) / 2, (int) (faceRect.height / 3.0));

            // Still in learning phase
            if (learn_frames < mLearningFrames) {
                teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
                teplateL = get_template(mJavaDetectorEye, eyearea_left, 24);
                learn_frames++;
            }
            else {
                // Learning finished, use the new templates for eye matching
                switch (mAlgorithm) {
                    case JAVA:
                        // Use Java algorithm to match eyes
                        lMatchedEye = match_eye(eyearea_left, teplateL, method, mJavaDetectorEye);
                        rMatchedEye = match_eye(eyearea_right, teplateR, method, mJavaDetectorEye);
                        if (!eyesFound) {
                            if (lMatchedEye != null && rMatchedEye != null) {
                                eyesFound = true;
                                // Eyes matched: calibration can start
                                updateUI();
                            }
                            else {
                                missedFrames++;
                                if (missedFrames == MISSED_FRAMES_LIMIT) {
                                    learn_frames = 0; // Simulates recreate button
                                    missedFrames = 0;
                                }
                            }
                        }
                        break;
                    case CPP:
                        // Use C++ algorithm to match eyes
                        lMatchedEye = cpp_match_eye(eyearea_left);
                        rMatchedEye = cpp_match_eye(eyearea_right);
                        if (!eyesFound) {
                            eyesFound = true;
                            // Eyes matched: calibration can start
                            updateUI();
                        }
                        break;
                    default:
                        break;
                }

            }

            // Cut eye areas and put them to zoom windows
            if (!mZoomWindow.empty() && !mZoomWindow2.empty()) {
                Imgproc.resize(mRgba.submat(eyearea_right), mZoomWindow, mZoomWindow.size());
                Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2, mZoomWindow2.size());
            }
        }

        // Launches a separate thread with the coordinates of the center of the pupil
        // of the matched eyes
        launchThread(lMatchedEye, rMatchedEye);

        return mRgba;
    }

    /**
     * Launches a separate thread to update images of the eyes and call
     * the "onEyeFound" method used during both the calibration and the recognition phases
     * @param lMatchedEye Point that represents the center of the pupil of the left eye
     * @param rMatchedEye Point that represents the center of the pupil of the right eye
     */
    private void launchThread(Point lMatchedEye, Point rMatchedEye) {

        final Point finalLMatchedEye = lMatchedEye;
        final Point finalRMatchedEye = rMatchedEye;

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {

                try {
                    if(DEBUG) Log.i(TAG3, "mZoomWindow = (" + mZoomWindow.toString() + ")");
                    Bitmap le = Bitmap.createBitmap(mZoomWindow.cols(), mZoomWindow.rows(), Bitmap.Config.ARGB_8888);
                    Bitmap re = Bitmap.createBitmap(mZoomWindow.cols(), mZoomWindow.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mZoomWindow.clone(), le);
                    Utils.matToBitmap(mZoomWindow2.clone(), re);
                    if (finalLMatchedEye != null && finalRMatchedEye != null)
                        onEyeFound(finalLMatchedEye, finalRMatchedEye, le, re);
                }
                catch (IllegalArgumentException e) {
                    if(DEBUG) Log.i(TAG, "THREAD EXCEPTION");
                }
            }
        };

        mainHandler.post(myRunnable);
        threadList.add(myRunnable);
    }

    /**
     * Called to recognize eyes after training is complete. It uses the CPP algorithm
     * with calls to OpenCV.
     * @param area Rectangle of the face
     */
    private Point cpp_match_eye(Rect area) {

        // The C++ algorithm returns an int array that represents a point
        int[] result = findEyeCenter(mGray.getNativeObjAddr(), area.x, area.y, area.width, area.height);
        return new Point(result[0], result[1]);
    }


    /**
     * Called to recognize a single eye after training is complete. It uses the Java algorithm
     * with calls to OpenCV.
     * @param area Rectangle of the face
     * @param mTemplate Template Mat used by the algorithm to match eyes from the
     *                  original image
     * @param type Method used to recognize the eye (SQDIFF, CCOEFF...)
     * @param classifier Cascade classifier used by the algorithm to detect
     *                     the eye
     */
    private Point match_eye(Rect area, Mat mTemplate, int type, CascadeClassifier classifier) {

        // Gets the eyes area that is the region of interest
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
        classifier.detectMultiScale(mROI, eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30), new Size());

        Rect[] eyesArray = eyes.toArray();

        // Checks if the eye has been detected.
        // If yes, cuts the area surrounding the eyes and detects
        // and returns the center of the pupil.
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

            return mmG.minLoc;
        }

        return null;
    }

    /**
     * Used during the first frames in order to train the model
     * @param classifier Eye classifier
     * @param area Rectangle area of the eye used for training and then detection
     * @param size Size of the eye square area
     */
    private Mat get_template(CascadeClassifier classifier, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        Rect eye_template;
        classifier.detectMultiScale(mROI, eyes, 1.15, 2,
                                    Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                                            | Objdetect.CASCADE_SCALE_IMAGE,
                                    new Size(30, 30), new Size());

        Rect[] eyesArray = eyes.toArray();

        // Eye detected by the model
        if (eyesArray.length > 0) {
            Rect e = eyesArray[0];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4),
                    e.width, (int) (e.height * 0.6));
            mROI = mGray.submat(eye_only_rectangle);

            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            eye_template = new Rect((int) iris.x - size / 2,
                                    (int) iris.y - size / 2, size, size);

            template = (mGray.submat(eye_template)).clone();

            return template;
        }

        return template;
    }

    /**
     * Initialization of the zoom windows, it also assigns the
     * proper eye images for the current frame
     */
    private void CreateAuxiliaryMats() {

        if (mGray.empty())
            return;

        int rows = mGray.rows();
        int cols = mGray.cols();

        if (mZoomWindow == null) {
            mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2 + cols / 10, cols);
            mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2 + cols / 10, cols);
        }
        if (mZoomWindow.empty()) {
            mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2 + cols / 10, cols);
            mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2 + cols / 10, cols);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(DEBUG) Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        mItemType = menu.add(mDetectorName[mDetectorType]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(DEBUG) Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
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

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    /**
     * Recreate button listener, resets the number of frames
     * used for learning.
     */
    public void onRecreateClick(View v) {
        learn_frames = 0;
    }

    static {
        System.loadLibrary("main-jni");
    }

    /**
     * Native method used to find the center of the eye of the input frame.
     * @param matAddr Memory address of the input matrix representing the input frame
     * @param x x coordinate of the rect representing the eye to detect area
     * @param y y coordinate of the rect representing the eye to detect area
     * @param height height of the rect representing the eye to detect area
     * @param width width of the rect representing the eye to detect area
     */
    private native int[] findEyeCenter(long matAddr, int x, int y, int width, int height);

}
