package com.teaminfernale.gazetracker;

import android.util.Log;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Leonardo on 24/05/2016.
 * Class used to store the first 8 points (4 for left eye and 4 for right eye) acquired during the
 * initial 4-points acquisition.
 */
public class TrainedEyesContainer {

    public enum ScreenRegion {UP_RIGHT, UP_LEFT, DOWN_RIGHT, DOWN_LEFT}

    private Point R_upRight;
    private Point L_upRight;

    private Point R_upLeft;
    private Point L_upLeft;

    private Point R_downRight;
    private Point L_downRight;

    private Point R_downLeft;
    private Point L_downLeft;

    private static final String TAG = "TrainedEyesContainer";

    private ArrayList<Point> R_upRight_a = new ArrayList<>();
    private ArrayList<Point> L_upRight_a = new ArrayList<>();

    private ArrayList<Point> R_upLeft_a = new ArrayList<>();
    private ArrayList<Point> L_upLeft_a = new ArrayList<>();

    private ArrayList<Point> R_downRight_a = new ArrayList<>();
    private ArrayList<Point> L_downRight_a = new ArrayList<>();

    private ArrayList<Point> R_downLeft_a = new ArrayList<>();
    private ArrayList<Point> L_downLeft_a = new ArrayList<>();

    private Sorter sorter = new Sorter();

    private int upperLeftTreshold = 0;
    private int upperRightTreshold = 0;
    private int leftLeftTreshold = 0;
    private int leftRightTreshold = 0;


    //Constructors
    public TrainedEyesContainer() {}

    public TrainedEyesContainer(Point R_upRight, Point L_upRight, Point R_upLeft, Point L_upLeft, Point R_downRight, Point L_downRight, Point R_downLeft, Point L_downLeft) {
        this.R_upRight = R_upRight;
        this.L_upRight = L_upRight;

        this.R_upLeft = R_upLeft;
        this.L_upLeft = L_upLeft;

        this.R_downRight = R_downRight;
        this.L_downRight = L_downRight;

        this.R_downLeft = R_downLeft;
        this.L_downLeft = L_downLeft;
    }

    public TrainedEyesContainer(Point[] points) {
        if (points.length == 8) {
            this.R_upRight = points[0];
            this.L_upRight = points[1];

            this.R_upLeft = points[2];
            this.L_upLeft = points[3];

            this.R_downRight = points[4];
            this.L_downRight = points[5];

            this.R_downLeft = points[6];
            this.L_downLeft = points[7];
        }
    }

    public TrainedEyesContainer(double[] coordinates){
        if (coordinates.length == 16) {
            this.R_upRight = new Point(coordinates[0], coordinates[1]);
            this.L_upRight = new Point(coordinates[2], coordinates[3]);

            this.R_upLeft = new Point(coordinates[4], coordinates[5]);
            this.L_upLeft = new Point(coordinates[6], coordinates[7]);

            this.R_downRight = new Point(coordinates[8], coordinates[9]);
            this.L_downRight = new Point(coordinates[10], coordinates[11]);

            this.R_downLeft = new Point(coordinates[12], coordinates[13]);
            this.L_downLeft = new Point(coordinates[14], coordinates[15]);
        }
    }

    public void meanSamples(){ //simple mean
        R_upRight = meanPointArrayList(R_upRight_a);
        L_upRight = meanPointArrayList(L_upRight_a);

        R_upLeft = meanPointArrayList(R_upLeft_a);
        L_upLeft = meanPointArrayList(L_upLeft_a);

        R_downRight = meanPointArrayList(R_downRight_a);
        L_downRight = meanPointArrayList(L_downRight_a);

        R_downLeft = meanPointArrayList(R_downLeft_a);
        L_downLeft = meanPointArrayList(L_downLeft_a);

        computeTresholds();
    }

    private void computeTresholds() {
        Log.i(TAG, "Results: R_upRight = " + R_upRight.x + "," + R_upRight.y + ")");
        Log.i(TAG, "Results: L_upRight = " + L_upRight.x + "," + L_upRight.y + ")");
        Log.i(TAG, "Results: R_upLeft = " + R_upLeft.x + "," + R_upLeft.y + ")");
        Log.i(TAG, "Results: L_upLeft = " + L_upLeft.x + "," + L_upLeft.y + ")");
        Log.i(TAG, "Results: R_downRight = " + R_downRight.x + "," + R_downRight.y + ")");
        Log.i(TAG, "Results: L_downRight = " + L_downRight.x + "," + L_downRight.y + ")");
        Log.i(TAG, "Results: R_downLeft = " + R_downLeft.x + "," + R_downLeft.y + ")");
        Log.i(TAG, "Results: L_downLeft = " + L_downLeft.x + "," + L_downLeft.y + ")");
        upperLeftTreshold = (int)((L_upLeft.y + L_upRight.y) / 2 + (L_downLeft.y + L_downRight.y) / 2) / 2;
        upperRightTreshold = (int)((R_upLeft.y + R_upLeft.y) / 2 + (R_downLeft.y + R_downRight.y) / 2 )/2;

        leftLeftTreshold = (int)((L_upLeft.x + L_downLeft.x) / 2 + (L_upRight.y + L_downRight.y) / 2) / 2;
        leftRightTreshold = (int)((R_upLeft.x + R_downLeft.x) / 2 + (R_upRight.y + R_downRight.y) / 2) / 2;

        Log.i(TAG, "Tresholds = upperLeftTreshold: " + upperLeftTreshold + "\n leftLefTresholdt: " + leftLeftTreshold);
        Log.i(TAG, "Tresholds = upperRightTreshold: " + upperRightTreshold + "\n upperRightTreshold: " + leftRightTreshold);

    }

    public Point[] getPoints() {
        return new Point[]{R_upRight, L_upRight, R_upLeft, L_upLeft, R_downRight, L_downRight, R_downLeft, L_downLeft};
    }

    public double[] getPointsCoordinates() {
        return new double[]{R_upRight.x, R_upRight.y,
                L_upRight.x, L_upRight.y,
                R_upLeft.x, R_upLeft.y,
                L_upLeft.x, L_upLeft.y,
                R_downRight.x, R_downRight.y,
                L_downRight.x, L_downRight.y,
                R_downLeft.x, R_downLeft.y,
                L_downLeft.x, L_downLeft.y};
    }

    private Point meanPointArrayList(ArrayList<Point> arr){
        //If I have no points returns a default point (0,0)
        if (arr.size() == 0) {
            return new Point(0, 0);
        }

        int[] listX = new int[arr.size()];
        int[] listY = new int[arr.size()];

        for (int i = 0; i < arr.size(); ++i) {
            listX[i] = (int)arr.get(i).x;
            listY[i] = (int)arr.get(i).y;
        }

        Arrays.sort(listX);
        Arrays.sort(listY);
        Log.i(TAG, "X array: "+Arrays.toString(listX));
        Log.i(TAG, "Y array: "+Arrays.toString(listY));
        int medianX = sorter.getMedian(listX);
        int medianY = sorter.getMedian(listY);
        Log.i(TAG, "Median x = " + medianX);
        Log.i(TAG, "Median y = " + medianY);

        return new Point(medianX, medianY);
    }


    // 0 for left, 1 for right
    public void addSample(int eye, int position, Point center) {

        //Log.i(TAG, "Added sample: eye" + eye + " position " + position + " Center = (" + center.x + "," + center.y + ")");
        if (eye == 0) { // left eye
            switch (position) {
                case 0://up left
                    L_upLeft_a.add(center);
                    break;
                case 1://up right
                    L_upRight_a.add(center);
                    break;
                case 2://down right
                    L_downRight_a.add(center);
                    break;
                case 3://down left
                    L_downLeft_a.add(center);
                    break;
            }
        }
        else { // right eye
            switch (position) {
                case 0://up left
                    R_upLeft_a.add(center);
                    break;
                case 1://up right
                    R_upRight_a.add(center);
                    break;
                case 2://down right
                    R_downRight_a.add(center);
                    break;
                case 3://down left
                    R_downLeft_a.add(center);
                    break;
            }
        }
    }


    public ScreenRegion computeCorner(Point p_left, Point p_right) {

        int min_LR = minIndex(
                distance(L_upLeft, p_left) + distance(R_upLeft,p_right),
                distance(L_upRight,p_left) + distance(R_upRight, p_right),
                distance(L_downRight, p_left) + distance(R_downRight, p_right),
                distance(L_downLeft, p_left) + distance(R_downLeft, p_right));



        switch (min_LR) {
            case 0:
                return ScreenRegion.UP_LEFT;
            case 1:
                return ScreenRegion.UP_RIGHT;
            case 2:
                return ScreenRegion.DOWN_RIGHT;
            default:
                return ScreenRegion.DOWN_LEFT;
        }
    }

    public ScreenRegion computeCorner2(Point p_left, Point p_right) {

        if (isUp((int)p_left.y, (int)p_right.y)) {
            if (isLeft((int)p_left.x, (int)p_right.x)) {
                return ScreenRegion.UP_LEFT;
            }
            return ScreenRegion.UP_RIGHT;
        }
        if (isLeft((int)p_left.x, (int)p_right.x)) {
            return ScreenRegion.DOWN_LEFT;
        }
        return ScreenRegion.DOWN_RIGHT;
    }

    private boolean isUp(int leftEyeY, int rightEyeY) {
        int leftUpperDistance = leftEyeY - upperLeftTreshold;
        int rightUpperDistance = rightEyeY - upperRightTreshold;

        return leftUpperDistance + rightUpperDistance > 0;
    }

    private boolean isLeft(int leftEyeX, int rightEyeX) {
        int leftLeftDistance = leftEyeX - leftLeftTreshold;
        int leftRightDistance = rightEyeX - leftRightTreshold;

        return leftLeftDistance + leftRightDistance > 0;
    }

    private int minIndex(double a, double b, double c, double d){
        double[] minimum = {a, b, c, d};
        double min = a;
        int minIn = 0;
        for (int i = 1; i < 4; ++i) {
            if (minimum[i] < min) {
                min = minimum[i];
                minIn = i;
            }
        }
        return  minIn;
    }

    private double distance(Point pRegion, Point p){

        if (pRegion != null && p != null) {
            return Math.sqrt(((pRegion.x - p.x) * (pRegion.x - p.x)) + ((pRegion.y - p.y) * (pRegion.y - p.y)));
        }
        return 1000;
    }

}