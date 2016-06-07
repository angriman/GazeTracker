package com.teaminfernale.gazetracker;

import org.opencv.core.Point;

import java.util.ArrayList;

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

    //Set methods

    public void setR_upRight(Point r_upRight) {
        R_upRight = r_upRight;
    }

    public void setL_upRight(Point l_upRight) {
        L_upRight = l_upRight;
    }

    public void setR_upLeft(Point r_upLeft) {
        R_upLeft = r_upLeft;
    }

    public void setL_upLeft(Point l_upLeft) {
        L_upLeft = l_upLeft;
    }

    public void setR_downRight(Point r_downRight) {
        R_downRight = r_downRight;
    }

    public void setL_downRight(Point l_downRight) {
        L_downRight = l_downRight;
    }

    public void setR_downLeft(Point r_downLeft) {
        R_downLeft = r_downLeft;
    }

    public void setL_downLeft(Point l_downLeft) {
        L_downLeft = l_downLeft;
    }



    //Get methods

    public Point getR_upRight() {
        return R_upRight;
    }

    public Point getL_upRight() {
        return L_upRight;
    }

    public Point getR_upLeft() {
        return R_upLeft;
    }

    public Point getL_upLeft() {
        return L_upLeft;
    }

    public Point getR_downRight() {
        return R_downRight;
    }

    public Point getL_downRight() {
        return L_downRight;
    }

    public Point getR_downLeft() {
        return R_downLeft;
    }

    public Point getL_downLeft() {
        return L_downLeft;
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

        Point[] list = new Point[arr.size()];

        int xTot = 0;
        int yTot = 0;
        for (int i = 0; i < arr.size(); i++){
            xTot += arr.get(i).x;
            yTot += arr.get(i).y;
            list[i] = arr.get(i);
        }


        Point[] sortedlist = sorter.sort(list);


        xTot = xTot / arr.size();
        yTot = yTot / arr.size();

        return new Point(xTot, yTot);
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


        if (min_LR == 0) return ScreenRegion.UP_LEFT;
        if (min_LR == 1) return ScreenRegion.UP_RIGHT;
        if (min_LR == 2) return ScreenRegion.DOWN_RIGHT;
        return ScreenRegion.DOWN_LEFT;
    }

    private int minIndex(double a, double b, double c, double d){
        double[] minimum = {a, b, c, d};
        double min = a;
        int minIn = 0;
        for (int i = 1; i < 4; i++) {
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