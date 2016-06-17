package com.teaminfernale.gazetracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.core.Point;

import static com.teaminfernale.gazetracker.MenuActivity.Algorithm;
import static com.teaminfernale.gazetracker.MenuActivity.Algorithm.JAVA;

/**
 * Created by the awesome Leonardo on 31/05/2016.
 */
public class CalibrationActivity extends MainActivity {

    /**
     * Number of samples for each eye to collect during the calibration
     */
    private static final int mSamplePerEye = 15;

    /**
     * Number of samples per single eye collected until now. Note that
     * new samples are recorded in couples simultaneously for both eyes. */
    private static int currentEyeSamples = 0;

    /**
     * If the calibration phase has started
     */
    private boolean calibrating = false;

    /**
     * Regions of the screen that can be observed by the user
     */
    public enum SRegion {UP_RIGHT, UP_LEFT, DOWN_RIGHT, DOWN_LEFT, NONE}

    /**
     * Region currently observed by the user (used during the calibration phase
     */
    SRegion currentRegion = SRegion.UP_LEFT;

    /**
     * Instance of the class used to store samples
     */
    TrainedEyesContainer mTrainedEyesContainer = new TrainedEyesContainer();

    /**
     * Currently used algorithm for eye recognition
     */
    private Algorithm mAlgorithm;

    /**
     * SeekBar used to switch eye recognition model if using the Java algorithm
     */
    private SeekBar methodSeekBar;

    /**
     * TextView that indicates the algorithm chosen with the SeekBar
     */
    private TextView mValue;

    /**
     * Number of dots at the end of the text of the TextView at the center of the screen.
     * This variable is used to correctly update the dots.
     */
    private int locatingEyesTextViewStatus = 0;

    /**
     * Blue color of the enabled "Start calibration button" text
     */
    private static final String mBlueColorString = "#FF1C8AD9";

    /**
     * SeekBar methods names
     */
    private static final String TMSQDIFF = "TM_SQDIFF";
    private static final String TMSQDIFFNORMED = "TM_SQDIFF_NORMED";
    private static final String TMCCOEFF = "TM_CCOEFF";
    private static final String TMCCOEFFNORMED = "TM_CCOEFF_NORMED";
    private static final String TMCCORR = "TM_CCORR";
    private static final String TMCCORRNORMED = "TM_CCORR_NORMED";

    /**
     * Debug tags
     */
    private static final String TAG = "CalibrationActivity";
    private static final String TAG3 = "CalibActivity_lifeCycle";




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

        // Calibration is taking place and both eyes are not null.
        // Saving them into the container.
        if (calibrating && leftEye != null && rightEye != null) {
            Log.i(TAG, "Left eye = (" + leftEye.x + "," + leftEye.y +")");
            // On the base of which region is being watched, saves the pair of samples
            // in the container.
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
     * the calibration has been completed
     */
    private void launchRecognitionActivity() {
        super.closeCamera();
        Log.i(TAG, "Creating new activity");

        Intent launchMainIntent = new Intent(CalibrationActivity.this, RecognitionActivity.class);
        launchMainIntent.putExtra("algorithm", mAlgorithm);
        Point[] points = mTrainedEyesContainer.getPoints();
        Log.i(TAG,"Calibration points: "+points[0]+" "+points[1]+" "+points[2]+" "+points[3]+" "+points[4]+" "+points[5]+" "+points[6]+" "+points[7]);
        double[] pointsCoordinates = mTrainedEyesContainer.getPointsCoordinates();
        int[] thresholds = mTrainedEyesContainer.getThresholds();
        launchMainIntent.putExtra("trainedEyesContainer", pointsCoordinates);
        launchMainIntent.putExtra("tresholdsEyesContainer", thresholds);
        startActivity(launchMainIntent);
        finish();
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

        // Gets which algorithm is used from the parent Activity
        mAlgorithm = super.getAlgorithm();
        methodSeekBar = (SeekBar) findViewById(R.id.methodSeekBar);
        mValue = (TextView) findViewById(R.id.method);

        ((ImageView) findViewById(R.id.top_left_image)).setImageResource(R.drawable.lena1);

        // When the calibration button is pressed the calibration starts, the button and the
        // SeekBar disappear.
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
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Implemented but not used method
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Implemented but not used method
                }

                // When the progress changes it updates the algorithm and updates
                // the corresponding TextView
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    setMethod(progress);
                    updateMethodLabelText(progress);
                }
            });
        }
        else {
            // C++ method: SeekBar not needed
            methodSeekBar.setVisibility(View.INVISIBLE);
            mValue.setVisibility(View.INVISIBLE);
        }

        // Sets the last method as the default selected by the SeekBar
        int initialMethod = 5;
        setMethod(initialMethod);
        updateMethodLabelText(initialMethod);

        // Start a recursive thread to animate the TextView at the center of the screen
        startTexViewUpdate();
    }

    /**
     * Creates a recursive thread that changes the text
     * at the center of the screen every time it is called
     * adding 0, 1, 2, and 3 dots
     */
    private void startTexViewUpdate() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!eyesFound()) {
                    long millisecondsDelay = 250;
                    mainHandler.postDelayed(this, millisecondsDelay);

                    String tail = "";
                    switch (locatingEyesTextViewStatus) {
                        case 1:
                            tail += ".";
                            break;
                        case 2:
                            tail += "..";
                            break;
                        case 3:
                            tail += "...";
                            break;
                        default:
                            break;
                    }

                    String newText = getResources().getString(R.string.locating_text_view) + tail;
                    TextView textView = (TextView) findViewById(R.id.status_text_view);
                    if (textView != null) {
                        textView.setText(newText);
                    }

                    locatingEyesTextViewStatus = (locatingEyesTextViewStatus + 1) % 4;
                }
            }
        };

        mainHandler.postDelayed(runnable, 0);
        threadList.add(runnable);
    }

    /**
     * Enables the "start calibration" button and changes the text
     * of the string in the center of the screen
     */
    @Override
    protected void updateUI() {
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                Button startCalibrationButton = (Button)findViewById(R.id.calibrate_button);
                if (startCalibrationButton != null) {
                    startCalibrationButton.setTextColor(Color.parseColor(mBlueColorString));
                    startCalibrationButton.setEnabled(true);
                    ((TextView) findViewById(R.id.status_text_view)).setText(getResources().getString(R.string.ready_text_view));
                }
            }
        };
        mainHandler.post(myRunnable);
        threadList.add(myRunnable);
    }

    /**
     * Updates the text of the text view above the seek bar
     */
    private void updateMethodLabelText(int newMethod) {
        switch (newMethod) {
            case 0:
                mValue.setText(TMSQDIFF);
                break;
            case 1:
                mValue.setText(TMSQDIFFNORMED);
                break;
            case 2:
                mValue.setText(TMCCOEFF);
                break;
            case 3:
                mValue.setText(TMCCOEFFNORMED);
                break;
            case 4:
                mValue.setText(TMCCORR);
                break;
            case 5:
                mValue.setText(TMCCORRNORMED);
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
