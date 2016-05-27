package com.teaminfernale.gazetracker;

import org.opencv.core.Point;


/**
 * Created by elisabetta on 5/27/16.
 */
public class GazeCalculator {

    public enum ScreenRegion {UP_RIGHT, UP_LEFT, DOWN_RIGHT, DOWN_LEFT}


    private Point R_upRight;
    private Point L_upRight;

    private Point R_upLeft;
    private Point L_upLeft;

    private Point R_downRight;
    private Point L_downRight;

    private Point R_downLeft;
    private Point L_downLeft;

    public GazeCalculator(Point R_upRight, Point L_upRight, Point R_upLeft, Point L_upLeft, Point R_downRight, Point L_downRight, Point R_downLeft, Point L_downLeft) {
        this.R_upRight = R_upRight;
        this.L_upRight = L_upRight;

        this.R_upLeft = R_upLeft;
        this.L_upLeft = L_upLeft;

        this.R_downRight = R_downRight;
        this.L_downRight = L_downRight;

        this.R_upRight = R_upRight;
        this.R_upRight = R_upRight;

        this.R_downLeft = R_downLeft;
        this.L_downLeft = L_downLeft;
    }

    public int computeCorner(Point p_left, Point p_right) {

        if ((int)p_left.x <= ((int)R_upRight.x + 5) && (int)p_left.y >= (int)R_upRight.y - 5)
            return 0;




        return 0;
    }
}
