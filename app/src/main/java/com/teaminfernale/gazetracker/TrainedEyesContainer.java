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

    /**
     * Debug variable
     */
    private static final boolean DEBUG = false;

    private static final String TAG = "TrainedEyesContainer";

    /**
     * Clear way to indicate one of the four region of the screen
     */
    public enum ScreenRegion {UP_RIGHT, UP_LEFT, DOWN_RIGHT, DOWN_LEFT}

    /**
     * For both left and right eye here we store every
     * sample that is a point that represents the center
     * of the pupil
     */
    private ArrayList<Point> R_upRight_a = new ArrayList<>();
    private ArrayList<Point> L_upRight_a = new ArrayList<>();

    private ArrayList<Point> R_upLeft_a = new ArrayList<>();
    private ArrayList<Point> L_upLeft_a = new ArrayList<>();

    private ArrayList<Point> R_downRight_a = new ArrayList<>();
    private ArrayList<Point> L_downRight_a = new ArrayList<>();

    private ArrayList<Point> R_downLeft_a = new ArrayList<>();
    private ArrayList<Point> L_downLeft_a = new ArrayList<>();

    /**
     * For both left and right eye here we store the
     * center of each of the four screen regions calculated
     * at the end of the calibration using the samples
     */
    private Point R_upRight;
    private Point L_upRight;

    private Point R_upLeft;
    private Point L_upLeft;

    private Point R_downRight;
    private Point L_downRight;

    private Point R_downLeft;
    private Point L_downLeft;

    /**
     * Instance of the Sorter class used to make
     * operations with arrays
     */
    private Sorter sorter = new Sorter();

    /**
     * Thresholds values used for recognition
     */
    // If left pupil y coordinate is greater than this the user is probably watching down
    private int upperLeftThreshold = 0;
    // If right pupil y coordinate is greater than this the user is probably watching down
    private int upperRightThreshold = 0;
    // If left pupil x coordinate is greater than this the user is probably watching right
    private int leftLeftThreshold = 0;
    // If right pupil x coordinate is greater than this the user is probably watching right
    private int leftRightThreshold = 0;


    public TrainedEyesContainer() {}

    /**
     * Constructor used after calibration, it instantiates a new instance
     * using the data collected and computed by a previous instance of this class
     * @param coordinates Used to pass the data of the eight points which
     *                    represent the center of each part of the screen
     *                    for both eyes
     * @param thresholds vector that contains the four thresholds
     */
    public TrainedEyesContainer(double[] coordinates, int[] thresholds) {
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

        if (thresholds.length == 4) {
            upperLeftThreshold = thresholds[0];
            upperRightThreshold = thresholds[1];
            leftLeftThreshold = thresholds[2];
            leftRightThreshold = thresholds[3];
        }
    }


    /**
     * Computes the simple mean of the points contained in the eight
     * lists of points and stores the results in the eight points
     * declared as class variables
     */
    public void meanSamples() {
        R_upRight = meanPointArrayList(R_upRight_a);
        L_upRight = meanPointArrayList(L_upRight_a);

        R_upLeft = meanPointArrayList(R_upLeft_a);
        L_upLeft = meanPointArrayList(L_upLeft_a);

        R_downRight = meanPointArrayList(R_downRight_a);
        L_downRight = meanPointArrayList(L_downRight_a);

        R_downLeft = meanPointArrayList(R_downLeft_a);
        L_downLeft = meanPointArrayList(L_downLeft_a);

        computeThresholds();
    }

    /**
     * Computes the four thresholds using
     * the eight points declared as class variables
     */
    private void computeThresholds() {
        if (DEBUG){
            Log.i(TAG, "Results: R_upRight = (" + R_upRight.x + "," + R_upRight.y + ")");
            Log.i(TAG, "Results: L_upRight = (" + L_upRight.x + "," + L_upRight.y + ")");
            Log.i(TAG, "Results: R_upLeft = (" + R_upLeft.x + "," + R_upLeft.y + ")");
            Log.i(TAG, "Results: L_upLeft = (" + L_upLeft.x + "," + L_upLeft.y + ")");
            Log.i(TAG, "Results: R_downRight = (" + R_downRight.x + "," + R_downRight.y + ")");
            Log.i(TAG, "Results: L_downRight = (" + L_downRight.x + "," + L_downRight.y + ")");
            Log.i(TAG, "Results: R_downLeft = (" + R_downLeft.x + "," + R_downLeft.y + ")");
            Log.i(TAG, "Results: L_downLeft = (" + L_downLeft.x + "," + L_downLeft.y + ")");
        }

        upperLeftThreshold = (int)((L_upLeft.y + L_upRight.y) / 2 + (L_downLeft.y + L_downRight.y) / 2) / 2;
        upperRightThreshold = (int)((R_upLeft.y + R_upLeft.y) / 2 + (R_downLeft.y + R_downRight.y) / 2 )/2;

        leftLeftThreshold = (int)((L_upLeft.x + L_downLeft.x) / 2 + (L_upRight.x + L_downRight.x) / 2) / 2;
        leftRightThreshold = (int)((R_upLeft.x + R_downLeft.x) / 2 + (R_upRight.x + R_downRight.x) / 2) / 2;

        if (DEBUG) {
            Log.i(TAG, "Thresholds = upperLeftThreshold: " + upperLeftThreshold + "\n leftLefThresholdt: " + leftLeftThreshold);
            Log.i(TAG, "Thresholds = upperRightThreshold: " + upperRightThreshold + "\n upperRightThreshold: " + leftRightThreshold);
        }

    }

    /**
     * Returns eight points declared
     * as class variables
     */
    public Point[] getPoints() {
        return new Point[]{R_upRight, L_upRight, R_upLeft, L_upLeft, R_downRight, L_downRight, R_downLeft, L_downLeft};
    }

    /**
     * Returns the eight points coordinates as a unique double array.
     * It is used to pass data to another instance of this class
     * because the Point class is not serializable.
     */
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

    /**
     * Returns in a single array the values of the four thresholds
     */
    public int[] getThresholds() {
        return new int[]{upperLeftThreshold, upperRightThreshold, leftLeftThreshold, leftLeftThreshold};
    }

    /**
     * Calculates and returns a point obtained as the median of
     * the list of points passed as input.
     * @param arr list of points used to compute the median point
     */
    private Point meanPointArrayList(ArrayList<Point> arr){

        //If there are no points returns a default point (0,0)
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

        if(DEBUG){
            Log.i(TAG, "X array: " + Arrays.toString(listX));
            Log.i(TAG, "Y array: " + Arrays.toString(listY));
        }

        int medianX = sorter.getMedian(listX);
        int medianY = sorter.getMedian(listY);

        if(DEBUG){
            Log.i(TAG, "Median x = " + medianX);
            Log.i(TAG, "Median y = " + medianY);
        }

        return new Point(medianX, medianY);
    }


    /**
     * Stores a new sample inside the corresponding list
     * @param eye represents the left (0) or the right (1) eye
     * @param position represent which of the four position the sample is about
     *                 (0 ~ Up-Left; 1 ~ Up-Right; 2 ~ Down-Right; 3 ~ Down-Left)
     * @param center represents the center of the pupil
     * */
    public void addSample(int eye, int position, Point center) {

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

    /**
     * Computes which area of the screen is being watched by the user.
     * It computes the mean distance between the current center of the pupil
     * and the centers computed after the calibration.
     * @param p_left the current center of the left pupil
     * @param p_right the current center of the right pupil
     * */
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

    /**
     * Computes which area of the screen is being watched by the user.
     * It uses a different approach based on the four thresholds: it
     * discriminates between up and down and between left and right.
     * Then it combines the results and returns the area.
     * @param p_left the current center of the left pupil
     * @param p_right the current center of the right pupil
     */
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

    /**
     * Discriminates if the user is watching the upper or the lower
     * part of the screen.
     * @param leftEyeY the Y coordinate of the center of the pupil of the left eye
     * @param rightEyeY the Y coordinate of the center of the pupil of the right eye
     */
    private boolean isUp(int leftEyeY, int rightEyeY) {
        if(DEBUG) Log.i(TAG, "upperLeftThreshold = " + upperLeftThreshold);
        int leftUpperDistance = leftEyeY - upperLeftThreshold;
        int rightUpperDistance = rightEyeY - upperRightThreshold;
        if(DEBUG){
            Log.i(TAG, "left upper distance = " + leftEyeY + " - " + upperLeftThreshold);
            Log.i(TAG, "right upper distance = " + rightEyeY + " - " + upperRightThreshold);
        }
        return leftUpperDistance + rightUpperDistance < 0;
    }

    /**
     * Discriminates if the user is watching the left or the right
     * part of the screen.
     * @param leftEyeX the X coordinate of the center of the pupil of the left eye
     * @param rightEyeX the X coordinate of the center of the pupil of the right eye
     */
    private boolean isLeft(int leftEyeX, int rightEyeX) {
        int leftLeftDistance = leftEyeX - leftLeftThreshold;
        int leftRightDistance = rightEyeX - leftRightThreshold;

        return leftLeftDistance + leftRightDistance < 0;
    }

    /**
     * Helper method that returns the index of the minimum value
     */
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

    /**
     * Returns the distance between two points.
     * @param pRegion first point
     * @param p second point
     */
    private double distance(Point pRegion, Point p) {

        if (pRegion != null && p != null) {
            return Math.sqrt(((pRegion.x - p.x) * (pRegion.x - p.x)) + ((pRegion.y - p.y) * (pRegion.y - p.y)));
        }
        return Double.MAX_VALUE;
    }

}
