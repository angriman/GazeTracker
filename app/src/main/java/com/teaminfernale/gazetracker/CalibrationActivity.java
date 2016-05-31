package com.teaminfernale.gazetracker;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Point;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * Created by Leonardo on 31/05/2016.
 */
public class CalibrationActivity extends MainActivity{

    private static int mSamplePerEye = 20;
    private static int doneSamplePerEye = 0;
    public enum SRegion {UP_RIGHT, UP_LEFT, DOWN_RIGHT, DOWN_LEFT, NONE};
    SRegion currentRegion = SRegion.UP_LEFT;
    TrainedEyesContainer mTrainedEyesContainer;
    private static boolean captionStarted = false;

    private boolean wantToSave = false;
    private static final String TAG = "CalibrationActivity";

    @Override
    protected void onEyeFound(Point leftEye, Point rightEye, Bitmap le, Bitmap re) {
        captionStarted = true;
        ((ImageView) findViewById(R.id.top_left_image)).setImageResource(R.drawable.lena1);
        //to show the eyes for debug
        ((ImageView) findViewById(R.id.left_eye)).setImageBitmap(le);
        ((ImageView) findViewById(R.id.right_eye)).setImageBitmap(re);

        switch (currentRegion){
            case UP_LEFT:
                mTrainedEyesContainer.addSample(0, 0, leftEye);
                mTrainedEyesContainer.addSample(1, 0, rightEye);
                doneSamplePerEye++;
                if (doneSamplePerEye>=mSamplePerEye){
                    doneSamplePerEye = 0;
                    currentRegion = SRegion.UP_RIGHT;
                    findViewById(R.id.top_left_image).setVisibility(View.INVISIBLE);
                    ((ImageView) findViewById(R.id.top_right_image)).setImageResource(R.drawable.lena1);
                }
                break;
            case UP_RIGHT:
                mTrainedEyesContainer.addSample(0, 1, leftEye);
                mTrainedEyesContainer.addSample(1, 1, rightEye);
                doneSamplePerEye++;
                if (doneSamplePerEye>=mSamplePerEye){
                    doneSamplePerEye = 0;
                    currentRegion = SRegion.DOWN_RIGHT;
                    findViewById(R.id.top_right_image).setVisibility(View.INVISIBLE);
                    ((ImageView) findViewById(R.id.down_right_image)).setImageResource(R.drawable.lena1);
                }
                break;
            case DOWN_RIGHT:
                mTrainedEyesContainer.addSample(0, 2, leftEye);
                mTrainedEyesContainer.addSample(1, 2, rightEye);
                doneSamplePerEye++;
                if (doneSamplePerEye>=mSamplePerEye){
                    doneSamplePerEye = 0;
                    currentRegion = SRegion.DOWN_LEFT;
                    findViewById(R.id.down_right_image).setVisibility(View.INVISIBLE);
                    ((ImageView) findViewById(R.id.down_left_image)).setImageResource(R.drawable.lena1);
                }
                break;
            case DOWN_LEFT:
                mTrainedEyesContainer.addSample(0, 3, leftEye);
                mTrainedEyesContainer.addSample(1, 3, rightEye);
                doneSamplePerEye++;
                if (doneSamplePerEye>=mSamplePerEye){
                    doneSamplePerEye = 0;
                    currentRegion = SRegion.NONE;
                    findViewById(R.id.down_left_image).setVisibility(View.INVISIBLE);
                }
                break;
            default:
                break;
        }



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
        //setCameraViewListener(this);
        //SETTARE LAYOUT

        findViewById(R.id.save_calibration).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wantToSave = true;
            }
        });

        mTrainedEyesContainer = new TrainedEyesContainer();

    }


    @Override
    public void onPause() {
        super.onPause();
        //DA SPOSTARE IN CALIBRATION
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


    //da cambiare con la versione nuova/ sistemare
    /*private void startCalibrationListener() {
        calibrated = false;
        calibrating = true;
        calibration_phase = 0;

        ((Button)findViewById(R.id.calibrate_button)).setText(getResources().getString(R.string.start_calibration_button));
        findViewById(R.id.calibrate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (calibrating) {

                    int next_calibration_phase = calibration_phase + 1;

                    if (next_calibration_phase > 4) { // Calibration completed

                        mTrainedEyesContainer.meanSamples();

                        R_upRight = mTrainedEyesContainer.getR_upRight();
                        Log.d(TAG1, "Point R_upRight " + R_upRight.toString());

                        L_upRight = mTrainedEyesContainer.getL_upRight();
                        Log.d(TAG1, "Point L_upRight " + L_upRight.toString());

                        R_upLeft = mTrainedEyesContainer.getR_upLeft();
                        Log.d(TAG1, "Point R_upLeft " + R_upLeft.toString());

                        L_upLeft = mTrainedEyesContainer.getL_upLeft();
                        Log.d(TAG1, "Point L_upLeft " + L_upLeft.toString());

                        R_downRight = mTrainedEyesContainer.getR_downRight();
                        L_downRight = mTrainedEyesContainer.getL_downRight();
                        R_downLeft = mTrainedEyesContainer.getR_downLeft();
                        L_downLeft = mTrainedEyesContainer.getL_downLeft();

                        calibration_phase = 0;
                        calibrated = true;

                        redefineButtonListener();

                        mGazeCalculator = new GazeCalculator(R_upRight, L_upRight, R_upLeft, L_upLeft, R_downRight, L_downRight, R_downLeft, L_downLeft);

                    }

                    if (!calibrated) {
                        Log.d(TAG, "Calibration phase: " + calibration_phase);
                        String toast_text = "Inizio acquisizione fase " + calibration_phase;
                        Toast t = Toast.makeText(MainActivity.this, toast_text, Toast.LENGTH_SHORT);
                        t.show();
                    }
                } else {

                    String toast_text = "Fine acquisizione fase " + calibration_phase;
                    Toast t = Toast.makeText(MainActivity.this, toast_text, Toast.LENGTH_SHORT);
                    t.show();
                    calibration_phase++;
                }
                calibrating = !calibrating;
            }
        });
    }*/



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
