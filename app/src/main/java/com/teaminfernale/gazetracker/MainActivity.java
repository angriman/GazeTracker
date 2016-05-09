package com.teaminfernale.gazetracker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.opencv.core.Core;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.textView)).setText("" + getMessage());

    }

    static {
        System.loadLibrary("main-jni");
    }

    public native int getMessage();

}
