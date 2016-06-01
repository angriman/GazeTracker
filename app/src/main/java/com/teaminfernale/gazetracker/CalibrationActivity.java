package com.teaminfernale.gazetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.core.Point;

/**
 * Created by the awesome Leonardo on 31/05/2016.
 */
public class CalibrationActivity extends MainActivity{

    private static final int mSamplePerEye = 20;
    private static int currentEyeSamples = 0;
    private boolean calibrating = false;
    public enum SRegion {UP_RIGHT, UP_LEFT, DOWN_RIGHT, DOWN_LEFT, NONE}
    SRegion currentRegion = SRegion.UP_LEFT;
    TrainedEyesContainer mTrainedEyesContainer = new TrainedEyesContainer();

    private boolean wantToSave = false;
    private static final String TAG = "CalibrationActivity";

    @Override
    protected void onEyeFound(Point leftEye, Point rightEye, Bitmap le, Bitmap re) {

        //to show the eyes for debug
        ((ImageView) findViewById(R.id.left_eye)).setImageBitmap(le);
        ((ImageView) findViewById(R.id.right_eye)).setImageBitmap(re);

        if (calibrating) {
            Log.i(TAG, "Sample added");
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

    // Launches recognition activity
    private void launchRecognitionActivity() {
        Intent launchMainIntent = new Intent(CalibrationActivity.this, RecognitionActivity.class);
        launchMainIntent.putExtra("trainedEyesContainer", mTrainedEyesContainer);
        CalibrationActivity.this.startActivity(launchMainIntent);
        finish();
    }

    @Override
    protected void setLayout() {
        setContentView(R.layout.calibration_activity_layout);
        ((ImageView) findViewById(R.id.left_eye)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.right_eye)).setImageResource(R.drawable.lena1);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((ImageView) findViewById(R.id.top_left_image)).setImageResource(R.drawable.lena1);

        // Setting listener to save calibration button
        findViewById(R.id.save_calibration).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wantToSave = true;
            }
        });

        startCalibrationListener();
    }


    private void startCalibrationListener() {

        findViewById(R.id.calibrate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calibrating = true;
                Log.i(TAG, "Calibration started");
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();

        if (wantToSave) {
            SharedPreferences sp = getPreferences(MODE_PRIVATE);

            Point[] pointsArray = {R_upRight, L_upRight, R_upLeft, L_upLeft, R_downRight, L_downRight, R_downLeft, L_downLeft};

            StringBuilder str_X = new StringBuilder();
            StringBuilder str_Y = new StringBuilder();

            for (Point aPointsArray : pointsArray) {
                str_X.append((int)aPointsArray.x).append(",");
                str_Y.append((int)aPointsArray.y).append(",");
            }
            sp.edit().putString("stringX", str_X.toString()).apply();
            sp.edit().putString("stringY", str_Y.toString()).apply();

            Log.i(TAG, "Calibration saved");
        }
    }


   /* //DA SPOSTARE IN CALIBRATION!!!! ONCREATE


    if (!calibrated) {
        startCalibrationListener();
    }
    else {
        redefineButtonListener();
    }

    findViewById(R.id.reset_calibration).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.i(TAG, "reset pressed");
            startCalibrationListener();
        }
    });

    findViewById(R.id.reset_calibration).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startCalibrationListener();
        }
    }); //FINE DA SPOSTARE IN CALIBRATION */
}
