package com.teaminfernale.gazetracker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";
    private static final String TAG1 = "MainActivity_calibr";
    public static final int JAVA_DETECTOR = 0;
    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;

    private TrainedEyesContainer mTrainedEyesContainer = new TrainedEyesContainer();
    private  GazeCalculator mGazeCalculator;
    private boolean calibrating = false;
    private int calibration_phase = 0;
    private int learn_frames = 0;
    private Mat teplateR;
    private Mat teplateL;
    int method = 0;

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

    private TextView mValue;

    double xCenter = -1;
    double yCenter = -1;

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

    public MainActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    private String readFromFile() {

        String ret = "";

        try {
            InputStream inputStream = openFileInput("calibFile.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        ((ImageView) findViewById(R.id.left_eye)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.right_eye)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.top_left_image)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.top_right_image)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.down_left_image)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.down_right_image)).setImageResource(R.drawable.lena1);


        calibrating = true;
        findViewById(R.id.calibrate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Point R_upRight,L_upRight, R_upLeft, L_upLeft, R_downRight, L_downRight, R_downLeft, L_downLeft = new Point();

                SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                String savedStringX = prefs.getString("stringX", "");
                String savedStringY = prefs.getString("stringY", "");
                StringTokenizer stX = new StringTokenizer(savedStringX, ",");
                StringTokenizer stY = new StringTokenizer(savedStringY, ",");
                int[] savedListX = new int[8];
                int[] savedListY = new int[8];
                for (int i = 0; i < 8; i++) {
                    savedListX[i] = Integer.parseInt(stX.nextToken());
                    savedListY[i] = Integer.parseInt(stY.nextToken());
                }

                R_upRight = new Point(savedListX[0],savedListY[0]);
                L_upRight = new Point(savedListX[1],savedListY[1]);
                R_upLeft = new Point(savedListX[2],savedListY[2]);
                L_upLeft = new Point(savedListX[3],savedListY[3]);
                R_downRight = new Point(savedListX[4],savedListY[4]);
                L_downRight = new Point(savedListX[5],savedListY[5]);
                R_downLeft= new Point(savedListX[6],savedListY[6]);
                L_downLeft = new Point(savedListX[7],savedListY[7]);

                if (calibrating) {

                    int next_calibration_phase = calibration_phase + 1;

                    if (next_calibration_phase > 4) { // Calibration completed

                        mTrainedEyesContainer.meanSamples();

                        R_upRight = mTrainedEyesContainer.getR_upRight();
                        Log.d(TAG1, "Point R_upRight " + R_upRight.toString());

                        L_upRight = mTrainedEyesContainer.getL_upRight();
                        Log.d(TAG1, "Point L_upRight " + L_upRight.toString());

                        R_upLeft = mTrainedEyesContainer.getR_upLeft();
                        Log.d(TAG1, "Point R_upLeft "+ R_upLeft.toString());

                        L_upLeft = mTrainedEyesContainer.getL_upLeft();
                        Log.d(TAG1, "Point L_upLeft "+ L_upLeft.toString());

                        R_downRight = mTrainedEyesContainer.getR_downRight();
                        L_downRight = mTrainedEyesContainer.getL_downRight();
                        R_downLeft = mTrainedEyesContainer.getR_downLeft();
                        L_downLeft = mTrainedEyesContainer.getL_downLeft();

                        StringBuilder str_X = new StringBuilder();
                        StringBuilder str_Y = new StringBuilder();

                        Point[] pointsArray = {R_upRight,L_upRight, R_upLeft, L_upLeft, R_downRight, L_downRight, R_downLeft, L_downLeft};
                        for (int i=0; i<pointsArray.length; i++)
                        {
                            str_X.append(pointsArray[i].x).append(",");
                            str_Y.append(pointsArray[i].y).append(",");
                        }
                        prefs.edit().putString("stringX", str_X.toString());
                        prefs.edit().putString("stringY", str_Y.toString());

                        calibration_phase = 0;

                        Button calibrateButton = (Button) findViewById(R.id.calibrate_button);
                        //calibrateButton.setOnClickListener(null); // Deletes the current listener
                        calibrateButton.setText("Start simulation");

                        mGazeCalculator = new GazeCalculator(R_upRight, L_upRight, R_upLeft, L_upLeft, R_downRight, L_downRight, R_downLeft, L_downLeft);

                    }

                    Log.d(TAG, "Calibration phase: " + calibration_phase);
                    String toast_text = "Inizio acquisizione fase " + calibration_phase;
                    Toast t = Toast.makeText(MainActivity.this, toast_text, Toast.LENGTH_SHORT);
                    t.show();
                }

                else {

                    String toast_text = "Fine acquisizione fase " + calibration_phase;
                    Toast t = Toast.makeText(MainActivity.this, toast_text, Toast.LENGTH_SHORT);
                    t.show();
                    calibration_phase++;
                }
                calibrating = !calibrating;

            }
        });

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        SeekBar methodSeekbar = (SeekBar) findViewById(R.id.methodSeekBar);

        methodSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                method = progress;
                switch (method) {
                    case 0:
                        mValue.setText("TM_SQDIFF");
                        break;
                    case 1:
                        mValue.setText("TM_SQDIFF_NORMED");
                        break;
                    case 2:
                        mValue.setText("TM_CCOEFF");
                        break;
                    case 3:
                        mValue.setText("TM_CCOEFF_NORMED");
                        break;
                    case 4:
                        mValue.setText("TM_CCORR");
                        break;
                    case 5:
                        mValue.setText("TM_CCORR_NORMED");
                        break;
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
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
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
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

        Rect[] facesArray = faces.toArray();
        //Log.i(TAG, "FacesArray length = " + facesArray.length);
        if (facesArray.length > 0) {
            //for (int i = 0; i < facesArray.length; i++) {

            Rect r = facesArray[0];
            // Compute both eyes area
            // Rect eyearea = new Rect(r.x + r.width / 8, (int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8, (int) (r.height / 3.0));
            // split it

            // TODO: capire perché vengono usati questi numeri e salvarli in variabili (per capire meglio il codice)
            Rect eyearea_right = new Rect(r.x + r.width / 16, (int) (r.y + (r.height / 4.5)), (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
            Rect eyearea_left = new Rect(r.x + r.width / 16 + (r.width - 2 * r.width / 16) / 2, (int) (r.y + (r.height / 4.5)), (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));

            if (learn_frames < 5) {
                teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
                teplateL = get_template(mJavaDetectorEye, eyearea_left, 24);
                learn_frames++;
            }
            else {
                // Learning finished, use the new templates for template matching
                match_eye(eyearea_left, teplateL, method, mJavaDetectorEye, 0);
                match_eye(eyearea_right, teplateR, method, mJavaDetectorEye, 1);
            }

            // Cut eye areas and put them to zoom windows
            Imgproc.resize(mRgba.submat(eyearea_right), mZoomWindow, mZoomWindow.size());
            Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2, mZoomWindow2.size());
        }
      //  }

        // On a separate thread it converts the eye mat into a bitmap
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {

                Bitmap le = Bitmap.createBitmap(mZoomWindow.cols(), mZoomWindow.rows(), Bitmap.Config.ARGB_8888);
                Bitmap re = Bitmap.createBitmap(mZoomWindow.cols(), mZoomWindow.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mZoomWindow.clone(), le);
                Utils.matToBitmap(mZoomWindow2.clone(), re);
                ((ImageView) findViewById(R.id.left_eye)).setImageBitmap(le);
                ((ImageView) findViewById(R.id.right_eye)).setImageBitmap(re);
            }
        };

        mainHandler.post(myRunnable);

        return mRgba;
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

    // Called after model training is completed
    private void match_eye(Rect area, Mat mTemplate, int type, CascadeClassifier clasificator, int eye) {
        //Point matchLoc;
        Mat mROI = mGray.submat(area);
        int result_cols = mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;

        // Check for bad template size
        if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
            return;
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
            Log.i(TAG, (eye == 0) ? "Left" : "Right" + " eye detected\t Center = ( " + mmG.minLoc.x + ", " + mmG.minLoc.y + " )");

            if (calibrating) {
                mTrainedEyesContainer.addSample(eye, calibration_phase, mmG.minLoc);
            }
            // Prendere un punto di riferimento del rettangolo e tracciare i movimenti dell'iride rispetto a quel punto
            // Tracciare i vari punti sullo schermo
        }
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
        for (int i = 0; i < eyesArray.length;) {
            Rect e = eyesArray[i];
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

    public void onRecreateClick(View v)
    {
        learn_frames = 0;
    }


}
