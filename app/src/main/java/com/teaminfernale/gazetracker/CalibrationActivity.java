package com.teaminfernale.gazetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.core.Point;

import java.util.StringTokenizer;

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

        if (calibrating && leftEye != null && rightEye != null) {
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

    // Launches recognition activity
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

        if (wantToSave) {
            SharedPreferences sp = getPreferences(MODE_PRIVATE);


            StringBuilder str_X = new StringBuilder();
            StringBuilder str_Y = new StringBuilder();

            for (Point aPointsArray : mTrainedEyesContainer.getPoints()) {
                str_X.append((int)aPointsArray.x).append(",");
                str_Y.append((int)aPointsArray.y).append(",");
            }
            sp.edit().putString("stringX", str_X.toString()).apply();
            sp.edit().putString("stringY", str_Y.toString()).apply();

            Log.i(TAG, "Calibration saved");
        }

        Intent launchMainIntent = new Intent(CalibrationActivity.this, RecognitionActivity.class);
        Point[] points = mTrainedEyesContainer.getPoints();
        Log.i(TAG,"Calibration points: "+points[0]+" "+points[1]+" "+points[2]+" "+points[3]+" "+points[4]+" "+points[5]+" "+points[6]+" "+points[7]);
        double[] pointsCoordinates = mTrainedEyesContainer.getPointsCoordinates();
        launchMainIntent.putExtra("trainedEyesContainer", pointsCoordinates);

        startActivity(launchMainIntent);
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


        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        final String savedStringX = prefs.getString("stringX", "");
        final String savedStringY = prefs.getString("stringY", "");

        if (savedStringX.length() > 0 && savedStringY.length() > 0) {
            findViewById(R.id.go_to_simulation_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    StringTokenizer stX = new StringTokenizer(savedStringX, ",");
                    StringTokenizer stY = new StringTokenizer(savedStringY, ",");

                    int[] savedListX = new int[8];
                    int[] savedListY = new int[8];

                    for (int i = 0; i < 8; i++) {
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
        }

        startCalibrationListener();
    }


    private void startCalibrationListener() {
        findViewById(R.id.calibrate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calibrating = true;
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();
    }
}
