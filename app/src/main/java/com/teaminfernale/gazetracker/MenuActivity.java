package com.teaminfernale.gazetracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Created by the awesome Eugenio on 6/12/16.
 * Activity to choose the eye recognition algorithm.
 */
public class MenuActivity extends Activity {

    /**
     * Available algorithms: Java or C++.
     */
    public enum Algorithm {CPP, JAVA}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Assigns the corresponding layout
        setContentView(R.layout.menu_activity_layout);

        // Assign the listeners
        findViewById(R.id.cpp_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchCalibrationWithAlgorithm(Algorithm.CPP);
            }
        });

        findViewById(R.id.java_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchCalibrationWithAlgorithm(Algorithm.JAVA);
            }
        });
    }

    /**
     * Launches the calibration activity with the corresponding
     * algorithm as an extra inside the intent
     * @param algorithm the enum representing the chosen algorithm
     */
    private void launchCalibrationWithAlgorithm(Algorithm algorithm) {
        Intent launchMainIntent = new Intent(MenuActivity.this, CalibrationActivity.class);
        launchMainIntent.putExtra("algorithm", algorithm);
        MenuActivity.this.startActivity(launchMainIntent);
        //finish();
    }
}
