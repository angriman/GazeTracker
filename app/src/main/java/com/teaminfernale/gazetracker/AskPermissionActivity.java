package com.teaminfernale.gazetracker;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;

/**
 * Created by Leonardo on 23/05/2016.
 */
public class AskPermissionActivity extends Activity {
    private final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String TAG = "AskPermissionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ask_permission_activity);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestCameraPermission();
        else {

            // Launch Calibration or Recognition activity
            Intent launchMainIntent = new Intent(AskPermissionActivity.this, CalibrationActivity.class);
            AskPermissionActivity.this.startActivity(launchMainIntent);
            finish();
        }
    }


    private void requestCameraPermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {
            //SPIEGA A COSA TI SERVONO I PERMESSI (MOSTRALO ALL'UTENTE)
        }
        ActivityCompat.requestPermissions(AskPermissionActivity.this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION){
            if (grantResults.length != 1 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "CAMERA permission has been DENIED.");
                // Handle lack of permission here
                //Snackbar.make(mLayout, R.string.permission_not_granted,
                 //       Snackbar.LENGTH_SHORT).show();
            }
            else {
                Log.i(TAG, "CAMERA permission has been GRANTED.");
                Intent launchMainIntent = new Intent(AskPermissionActivity.this, MainActivity.class);
                AskPermissionActivity.this.startActivity(launchMainIntent);
                finish();

                // You can now access the camera
                //Snackbar.make(mLayout, R.string.permission_available_camera,
                 //       Snackbar.LENGTH_SHORT).show();
            }
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
