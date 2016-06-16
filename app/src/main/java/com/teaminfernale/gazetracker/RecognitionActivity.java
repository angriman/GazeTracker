package com.teaminfernale.gazetracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.teaminfernale.gazetracker.MenuActivity.Algorithm;

import org.opencv.core.Point;

/**
 * Created by the awesome Leonardo on 31/05/2016.
 */

public class RecognitionActivity extends MainActivity {

    private  TrainedEyesContainer mTrainedEyesContainer;
    private static final String TAG = "RecognitionActivity";
    private int imageID = 0;
    private boolean simulationStarted = false;
    private static final String TAG4 = "RecogActivity_lifeCycle";
    private enum RecognitionMetric {MEDIAN, THRESHOLD};
    private RecognitionMetric metric;


    /**
     * Sets the corresponding layout and initializes the images of the ImageViews
     */
    @Override
    protected void setLayout() {
        super.setModeRecognition();
        setContentView(R.layout.recognition_activity_layout);
        ((ImageView) findViewById(R.id.rec_left_eye)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.rec_right_eye)).setImageResource(R.drawable.lena1);
        initializeCornerImages();
    }

    /**
     * Sets the images at the corners of the screen their default image
     */
    private void initializeCornerImages() {
        ((ImageView) findViewById(R.id.rec_top_left_image)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.rec_top_right_image)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.rec_down_left_image)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.rec_down_right_image)).setImageResource(R.drawable.lena1);
    }

    @Override
    protected void updateUI() {}

    /**
     * Called each time the parent activity matches the eyes of the user
     * @param rightEye Coordinates of the center of the right eye
     * @param leftEye Coordinates of the center of the left eye
     * @param re Image containing the right eye
     * @param le Image containing the left eye
     */
    @Override
    protected void onEyeFound(Point leftEye, Point rightEye, Bitmap le, Bitmap re) {

        // To show the eyes for debug
        ((ImageView) findViewById(R.id.rec_left_eye)).setImageBitmap(le);
        ((ImageView) findViewById(R.id.rec_right_eye)).setImageBitmap(re);

        if (simulationStarted) {
            Log.i("CalibrationActivity", "Left eye = (" + leftEye.x + "," + leftEye.y +")");

            final Point finalLMatchedEye = leftEye;
            final Point finalRMatchedEye = rightEye;

            Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

            Runnable changePicture = new Runnable() {
                @Override
                public void run() {
                    if (finalLMatchedEye != null && finalRMatchedEye != null) {
                        String result = "You are watching ";
                        ImageView imageView = null;
                        TrainedEyesContainer.ScreenRegion choosenRegion = null;
                        if (metric==RecognitionMetric.MEDIAN)
                            choosenRegion = mTrainedEyesContainer.computeCorner(finalLMatchedEye, finalRMatchedEye);
                        else
                            choosenRegion = mTrainedEyesContainer.computeCorner2(finalLMatchedEye, finalRMatchedEye);
                        switch (choosenRegion) {
                            case UP_LEFT:
                                Log.i(TAG, result + "up left");
                                imageView = (ImageView) findViewById(R.id.rec_top_left_image);
                                break;
                            case UP_RIGHT:
                                Log.i(TAG, result + "up right");
                                imageView = (ImageView) findViewById(R.id.rec_top_right_image);
                                break;
                            case DOWN_LEFT:
                                Log.i(TAG, result + "down left");
                                imageView = (ImageView) findViewById(R.id.rec_down_left_image);
                                break;
                            case DOWN_RIGHT:
                                Log.i(TAG, result + "down right");
                                imageView = (ImageView) findViewById(R.id.rec_down_right_image);
                                break;
                            default:
                                Log.i(TAG, "somewhere I don't know");
                                break;
                        }

                        if (imageID != 0) {
                            ((ImageView) findViewById(imageID)).setImageResource(R.drawable.lena1);
                        }
                        imageID = imageView.getId();
                        imageView.setImageResource(R.drawable.arianna);

                    }
                }
            };

            mainHandler.post(changePicture);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG4, "RecogActivity onCreate() called");
        metric = RecognitionMetric.THRESHOLD;

        // Restore the simulation button state from the savedInstanceState
        if (savedInstanceState != null) {
            String buttonValue = savedInstanceState.getString("button");
            if (buttonValue != null)
                ((Button) findViewById(R.id.simulation_button)).setText(buttonValue);
            boolean previousState = savedInstanceState.getBoolean("buttonState");
            simulationStarted = previousState;
        }

        Intent intent = getIntent();
        double[] pointsCoordinates = intent.getDoubleArrayExtra("trainedEyesContainer");
        int[] tresholds = intent.getIntArrayExtra("tresholdsEyesContainer");
        setAlgorithm((Algorithm) intent.getSerializableExtra("algorithm"));
        mTrainedEyesContainer = new TrainedEyesContainer(pointsCoordinates, tresholds);

        findViewById(R.id.simulation_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulationStarted = !simulationStarted;
                Button b = (Button) findViewById(R.id.simulation_button);
                if (simulationStarted) {
                    b.setText(getResources().getString(R.string.stop_simulation_button));
                } else {
                    b.setText(getResources().getString(R.string.start_simulation_button));
                    initializeCornerImages();
                }
            }
        });

        findViewById(R.id.goto_menu_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent backToMenuIntent = new Intent(RecognitionActivity.this, MenuActivity.class);
                startActivity(backToMenuIntent);
                finish();
            }
        });


        findViewById(R.id.recognition_method_switch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Switch s = (Switch) findViewById(R.id.recognition_method_switch);
                if (metric == RecognitionMetric.MEDIAN) {
                    metric = RecognitionMetric.THRESHOLD;
                    s.setText(R.string.rec_method_switch_tresh);
                } else {
                    metric = RecognitionMetric.MEDIAN;
                    s.setText(R.string.rec_method_switch_med);
                }
            }
        });


    }

    // Save the instance state
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        String button = ((Button)findViewById(R.id.simulation_button)).getText().toString();
        savedInstanceState.putString("button", button);
        savedInstanceState.putBoolean("buttonState", simulationStarted);
        super.onSaveInstanceState(savedInstanceState);
    }

}
