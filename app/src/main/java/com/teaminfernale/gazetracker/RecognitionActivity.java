package com.teaminfernale.gazetracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.core.Point;

/**
 * Created by Leonardo on 31/05/2016.
 */
public class RecognitionActivity extends MainActivity {

    private  TrainedEyesContainer mTrainedEyesContainer;
    private static final String TAG = "RecognitionActivity";
    private int imageID = 0;

    @Override
    protected void setLayout() {
        super.setModeRecognition();
        setContentView(R.layout.recognition_activity_layout);
      /*  ((ImageView) findViewById(R.id.left_eye)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.right_eye)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.top_left_image)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.top_right_image)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.down_left_image)).setImageResource(R.drawable.lena1);
        ((ImageView) findViewById(R.id.down_right_image)).setImageResource(R.drawable.lena1);
        */
    }

    @Override
    protected void onEyeFound(Point leftEye, Point rightEye, Bitmap le, Bitmap re) {

        //to show the eyes for debug
        ((ImageView) findViewById(R.id.left_eye)).setImageBitmap(le);
        ((ImageView) findViewById(R.id.right_eye)).setImageBitmap(re);

        final Point finalLMatchedEye = leftEye;
        final Point finalRMatchedEye = rightEye;

        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

        Runnable changePicture = new Runnable() {
            @Override
            public void run() {
                if (finalLMatchedEye != null && finalRMatchedEye != null) {
                    String result = "You are watching ";
                    ImageView imageView = null;
                    switch (mTrainedEyesContainer.computeCorner(finalLMatchedEye, finalRMatchedEye)) {
                        case UP_LEFT:
                            Log.i(TAG, result + "up left");
                            imageView = (ImageView) findViewById(R.id.top_left_image);
                            break;
                        case UP_RIGHT:
                            Log.i(TAG, result + "up right");
                            imageView = (ImageView) findViewById(R.id.top_right_image);
                            break;
                        case DOWN_LEFT:
                            Log.i(TAG, result + "down left");
                            imageView = (ImageView) findViewById(R.id.down_left_image);
                            break;
                        case DOWN_RIGHT:
                            Log.i(TAG, result + "down right");
                            imageView = (ImageView) findViewById(R.id.down_right_image);
                            break;
                        default:
                            Log.i(TAG, "somewhere I don't know");
                            break;
                    }

                    if (imageID != 0) {
                        ((ImageView)findViewById(imageID)).setImageResource(R.drawable.lena1);
                    }
                    imageID = imageView.getId();
                    imageView.setImageResource(R.drawable.arianna);

                }
            }
        };

        mainHandler.post(changePicture);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        Point[] points = (Point[]) i.getSerializableExtra("trainedEyesContainer");
        mTrainedEyesContainer = new TrainedEyesContainer(points);
        Log.i(TAG, "Trained container created");
        //SETTARE LAYOUT

        //INIZIALIZZARE mGazeCalculator!!!!
    }

    /*PER RICARICARE I PUNTI DELLA CALIBRATION (onCreate)
    SharedPreferences prefs = getPreferences(MODE_PRIVATE);
    String savedStringX = prefs.getString("stringX", "");
    String savedStringY = prefs.getString("stringY", "");

    if (savedStringX.length() > 0 && savedStringY.length() > 0) {

        StringTokenizer stX = new StringTokenizer(savedStringX, ",");
        StringTokenizer stY = new StringTokenizer(savedStringY, ",");

        int[] savedListX = new int[8];
        int[] savedListY = new int[8];
        String toast_text = "Calibration loaded";
        Toast.makeText(MainActivity.this, toast_text, Toast.LENGTH_SHORT).show();

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

        calibrated = true;

        mGazeCalculator = new GazeCalculator(R_upRight, L_upRight, R_upLeft, L_upLeft, R_downRight, L_downRight, R_downLeft, L_downLeft);

    }*/
}
