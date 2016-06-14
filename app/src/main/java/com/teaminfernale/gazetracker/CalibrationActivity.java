package com.teaminfernale.gazetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.core.Point;

import static com.teaminfernale.gazetracker.MenuActivity.Algorithm;
import static com.teaminfernale.gazetracker.MenuActivity.Algorithm.*;

/**
 * Created by the awesome Leonardo on 31/05/2016.
 */
public class CalibrationActivity extends MainActivity {

    private static final int mSamplePerEye = 15;
    private static int currentEyeSamples = 0;
    private boolean calibrating = false;
    private static final String TAG3 = "CalibActivity_lifeCycle";
    public enum SRegion {UP_RIGHT, UP_LEFT, DOWN_RIGHT, DOWN_LEFT, NONE}
    SRegion currentRegion = SRegion.UP_LEFT;
    TrainedEyesContainer mTrainedEyesContainer = new TrainedEyesContainer();
    private static final String TAG = "CalibrationActivity";
    private Algorithm mAlgorithm;
    private SeekBar methodSeekBar;
    private TextView mValue;

    /**
     * Called each time the parent activity matches the eyes of the user
     * @param rightEye Coordinates of the center of the right eye
     * @param leftEye Coordinates of the center of the left eye
     * @param re Image containing the right eye
     * @param le Image containing the left eye
     */
    @Override
    protected void onEyeFound(Point leftEye, Point rightEye, Bitmap le, Bitmap re) {

        //to show the eyes for debug
        ((ImageView) findViewById(R.id.left_eye)).setImageBitmap(le);
        ((ImageView) findViewById(R.id.right_eye)).setImageBitmap(re);

        if (calibrating && leftEye != null && rightEye != null) {
            Log.i(TAG, "Left eye = (" + leftEye.x + "," + leftEye.y +")");
            switch (currentRegion) {
                case UP_LEFT:
                    mTrainedEyesContainer.addSample(0, 0, leftEye);
                    mTrainedEyesContainer.addSample(1, 0, rightEye);
                    currentEyeSamples++;

                    if (currentEyeSamples >= mSamplePerEye) {
                        currentEyeSamples = 0;
                        currentRegion = SRegion.UP_RIGHT;
                        findViewById(R.id.top_left_image).setVisibility(View.INVISIBLE);
                        ((ImageView) findViewById(R.id.top_right_image)).setImageResource(R.drawable.lena1);
                    }
                    break;

                case UP_RIGHT:
                    mTrainedEyesContainer.addSample(0, 1, leftEye);
                    mTrainedEyesContainer.addSample(1, 1, rightEye);
                    currentEyeSamples++;

                    if (currentEyeSamples >= mSamplePerEye) {
                        currentEyeSamples = 0;
                        currentRegion = SRegion.DOWN_RIGHT;
                        findViewById(R.id.top_right_image).setVisibility(View.INVISIBLE);
                        ((ImageView) findViewById(R.id.down_right_image)).setImageResource(R.drawable.lena1);
                    }
                    break;

                case DOWN_RIGHT:
                    mTrainedEyesContainer.addSample(0, 2, leftEye);
                    mTrainedEyesContainer.addSample(1, 2, rightEye);
                    currentEyeSamples++;

                    if (currentEyeSamples >= mSamplePerEye) {
                        currentEyeSamples = 0;
                        currentRegion = SRegion.DOWN_LEFT;
                        findViewById(R.id.down_right_image).setVisibility(View.INVISIBLE);
                        ((ImageView) findViewById(R.id.down_left_image)).setImageResource(R.drawable.lena1);
                    }
                    break;

                case DOWN_LEFT:
                    mTrainedEyesContainer.addSample(0, 3, leftEye);
                    mTrainedEyesContainer.addSample(1, 3, rightEye);
                        currentEyeSamples++;

                    if (currentEyeSamples >= mSamplePerEye) { // Calibration completed
                        mTrainedEyesContainer.meanSamples();
                        currentEyeSamples = 0;
                        currentRegion = SRegion.NONE;
                        findViewById(R.id.down_left_image).setVisibility(View.INVISIBLE);
                        launchRecognitionActivity();
                    }
                    break;

                default:
                    break;
            }
        }
    }


    /**
     * Launches the recognition activity. Called when
     * the calibration has beeen completed
     */
    private void launchRecognitionActivity() {
        super.closeCamera();
        Log.i(TAG, "Creating new activity");

/*        Point[] pointsArray = mTrainedEyesContainer.getPoints();
        R_upRight = pointsArray[0];
        L_upRight= pointsArray[1];
        R_upLeft = pointsArray[2];
        L_upLeft = pointsArray[3];
        R_downRight = pointsArray[4];
        L_downRight = pointsArray[5];
        R_downLeft = pointsArray[6];
        L_downLeft = pointsArray[7];*/

        Intent launchMainIntent = new Intent(CalibrationActivity.this, RecognitionActivity.class);
        launchMainIntent.putExtra("algorithm", mAlgorithm);
        Point[] points = mTrainedEyesContainer.getPoints();
        Log.i(TAG,"Calibration points: "+points[0]+" "+points[1]+" "+points[2]+" "+points[3]+" "+points[4]+" "+points[5]+" "+points[6]+" "+points[7]);
        double[] pointsCoordinates = mTrainedEyesContainer.getPointsCoordinates();
        int[] thresholds = mTrainedEyesContainer.getThresholds();
        launchMainIntent.putExtra("trainedEyesContainer", pointsCoordinates);
        launchMainIntent.putExtra("tresholdsEyesContainer", thresholds);
        startActivity(launchMainIntent);
        //finish();
    }

    /**
     * Sets the corresponding layout and initializes the images of the image views
     */
    @Override
    protected void setLayout() {
        setContentView(R.layout.calibration_activity_layout);
        ((ImageView) findViewById(R.id.left_eye)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.right_eye)).setImageResource(R.drawable.lena1);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG3, "CalibActivity onCreate() called");

        mAlgorithm = super.getAlgorithm();
        methodSeekBar = (SeekBar) findViewById(R.id.methodSeekBar);
        mValue = (TextView) findViewById(R.id.method);

        ((ImageView) findViewById(R.id.top_left_image)).setImageResource(R.drawable.lena1);

        final Button calibrationButton = (Button) findViewById(R.id.calibrate_button);
        calibrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calibrating = true;
                calibrationButton.setVisibility(View.INVISIBLE);
                if (mAlgorithm == JAVA) {
                    methodSeekBar.setVisibility(View.INVISIBLE);
                }
            }
        });


        if (mAlgorithm == JAVA) {
            methodSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    setMethod(progress);
                    updateMethodLabelText(progress);
                }
            });
        }
        else {
            methodSeekBar.setVisibility(View.INVISIBLE);
            mValue.setVisibility(View.INVISIBLE);
        }

        int initialMethod = 5;
        setMethod(initialMethod);
        updateMethodLabelText(initialMethod);

        //SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        //String[] strings = getStrings();
        //final String savedStringX = prefs.getString(strings[0], "");
        //final String savedStringY = prefs.getString(strings[1], "");


        // BUTTON go_to_simulation_button da cancellare (se la calib è fatta va alla recog da solo, altrimenti non si può andare)
       /* if (savedStringX.length() > 0 && savedStringY.length() > 0) {
            findViewById(R.id.go_to_simulation_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    StringTokenizer stX = new StringTokenizer(savedStringX, ",");
                    StringTokenizer stY = new StringTokenizer(savedStringY, ",");

                    int[] savedListX = new int[8];
                    int[] savedListY = new int[8];

                    for (int i = 0; i < 8; ++i) {
                        savedListX[i] = Integer.parseInt(stX.nextToken());
                        savedListY[i] = Integer.parseInt(stY.nextToken());
                    }

                    R_upRight = new Point(savedListX[0], savedListY[0]);
                    L_upRight = new Point(savedListX[1], savedListY[1]);
                    R_upLeft = new Point(savedListX[2], savedListY[2]);
                    L_upLeft = new Point(savedListX[3], savedListY[3]);
                    R_downRight = new Point(savedListX[4], savedListY[4]);
                    L_downRight = new Point(savedListX[5], savedListY[5]);
                    R_downLeft = new Point(savedListX[6], savedListY[6]);
                    L_downLeft = new Point(savedListX[7], savedListY[7]);

                    mTrainedEyesContainer = new TrainedEyesContainer(R_upRight, L_upRight, R_upLeft, L_upLeft, R_downRight, L_downRight, R_downLeft, L_downLeft);
                    Intent launchMainIntent = new Intent(CalibrationActivity.this, RecognitionActivity.class);
                    launchMainIntent.putExtra("trainedEyesContainer", mTrainedEyesContainer.getPoints());
                    startActivity(launchMainIntent);
                    finish();
                }
            });
        }
        else {
            findViewById(R.id.go_to_simulation_button).setVisibility(View.INVISIBLE);
        }*/
    }

    /**
     * Updates the text of the text view above the seek bar
     */
    private void updateMethodLabelText(int newMethod) {
        switch (newMethod) {
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

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG3, "CalibActivity onPause() called");

        // Saves calibration
        //SharedPreferences sp = getPreferences(MODE_PRIVATE);

        //StringBuilder str_X = new StringBuilder();
        //StringBuilder str_Y = new StringBuilder();

        //for (Point aPointsArray : mTrainedEyesContainer.getPoints()) {
        //    str_X.append((int)aPointsArray.x).append(",");
        //    str_Y.append((int)aPointsArray.y).append(",");
        //}

        //String[] strings = getStrings();

        //sp.edit().putString(strings[0], str_X.toString()).apply();
        //sp.edit().putString(strings[1], str_Y.toString()).apply();

        Log.i(TAG, "Calibration saved");

    }

    /**
     * Returns the pair of string which identify the saved calibration
     */
    private String[] getStrings() {
        String idX = "stringX";
        String idY = "stringY";
        String java = "JAVA";

        switch (mAlgorithm) {
            case CPP:
                String cpp = "CPP";
                idX += cpp;
                idY += cpp;
                break;
            case JAVA:
                idX += java;
                idY += java;
                break;
            default:
                idX += java;
                idY += java;
                break;
        }
        return new String[] {idX, idY};
    }

}
