package com.teaminfernale.gazetracker;

import android.util.Log;

import org.opencv.core.Point;

import java.util.ArrayList;

/**
 * Created by Leonardo on 24/05/2016.
 * Class used to store the first 8 points (4 for left eye and 4 for right eye) acquired during the
 * initial 4-points acquisition.
 */
public class TrainedEyesContainer {
    private Point R_upRight;
    private Point L_upRight;

    private Point R_upLeft;
    private Point L_upLeft;

    private Point R_downRight;
    private Point L_downRight;

    private Point R_downLeft;
    private Point L_downLeft;



    private ArrayList<Point> R_upRight_a = new ArrayList<>();
    private ArrayList<Point> L_upRight_a = new ArrayList<>();

    private ArrayList<Point> R_upLeft_a = new ArrayList<>();
    private ArrayList<Point> L_upLeft_a = new ArrayList<>();

    private ArrayList<Point> R_downRight_a = new ArrayList<>();
    private ArrayList<Point> L_downRight_a = new ArrayList<>();

    private ArrayList<Point> R_downLeft_a = new ArrayList<>();
    private ArrayList<Point> L_downLeft_a = new ArrayList<>();

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

    public void meanSamples(){//simple mean
        R_upRight = meanPointArrayList(R_upRight_a);
        L_upRight = meanPointArrayList(L_upRight_a);

        R_upLeft = meanPointArrayList(R_upLeft_a);
        L_upLeft = meanPointArrayList(L_upLeft_a);

        R_downRight = meanPointArrayList(R_downRight_a);
        L_downRight = meanPointArrayList(L_downRight_a);

        R_downLeft = meanPointArrayList(R_downLeft_a);
        L_downLeft = meanPointArrayList(L_downLeft_a);
    }

    private Point meanPointArrayList(ArrayList<Point> arr){
        //If I have no points returns a default point (0,0)
        if (arr.size()== 0) return new Point(0, 0);

        int xTot = 0;
        int yTot = 0;
        for (int i = 0; i<arr.size(); i++){
            xTot += arr.get(i).x;
            yTot += arr.get(i).y;
        }
        xTot = xTot/arr.size();
        yTot = yTot/arr.size();

        return new Point(xTot, yTot);
    }

    // 0 for left, 1 for right
    public boolean addSample(int eye, int position, Point center) {

        if (center.x != 0 && center.y != 0) {
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
            } else { // right eye
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
            return true;
        }
        return false;
    }

}