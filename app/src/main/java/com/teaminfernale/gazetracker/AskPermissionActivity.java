package com.teaminfernale.gazetracker;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;

/**
 * Created by Leonardo on 23/05/2016.
 */
public class AskPermissionActivity extends Activity {

    /**
     * Debug variable
     */
    private static final boolean DEBUG = false;

    private final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String TAG = "AskPermissionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ask_permission_activity);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        }
        else {
            launchActivity();
        }
    }

    /**
     * Shows the alert to request the camera permissions to the user
     */
    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(AskPermissionActivity.this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION){
            if (grantResults.length != 1 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                if (DEBUG) Log.i(TAG, "CAMERA permission has been DENIED.");
                findViewById(R.id.askPermissionsButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        requestCameraPermission();
                    }
                });
            }
            else {
                if (DEBUG)Log.i(TAG, "CAMERA permission has been GRANTED.");

                launchActivity();
            }
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Launches the next activity
     */
    private void launchActivity() {
        // Launch Calibration activity
           /* Intent launchMainIntent = new Intent(AskPermissionActivity.this, CalibrationActivity.class);*/
        Intent launchMainIntent = new Intent(AskPermissionActivity.this, MenuActivity.class);
        AskPermissionActivity.this.startActivity(launchMainIntent);
        finish();
    }

}
