package com.teaminfernale.gazetracker;

import org.opencv.core.Point;

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

}