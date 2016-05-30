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

        /**
        if (((int)p_left.x <= ((int)R_upRight.x + 5) && (int)p_left.y >= (int)R_upRight.y - 5) || ((int)p_right.x <= ((int)R_upRight.x + 5) && (int)p_left.y >= (int)R_upRight.y - 5))
            return ScreenRegion.UP_RIGHT;

        if (((int)p_left.x >= ((int)R_upLeft.x - 5) && (int)p_left.y >= (int)R_upLeft.y - 5) || ((int)p_right.x >= ((int)R_upLeft.x - 5) && (int)p_left.y >= (int)R_upLeft.y - 5))
            return ScreenRegion.UP_LEFT;

        if (((int)p_left.x <= ((int)R_downRight.x + 5) && (int)p_left.y <= (int)R_downRight.y + 5) || ((int)p_right.x <= ((int)R_downRight.x + 5) && (int)p_left.y >= (int)R_downRight.y + 5))
            return ScreenRegion.DOWN_RIGHT;

        if (((int)p_left.x >= ((int)R_downLeft.x - 5) && (int)p_left.y <= (int)R_downLeft.y + 5) || ((int)p_right.x >= ((int)R_downLeft.x - 5) && (int)p_left.y <= (int)R_downLeft.y + 5))
            return ScreenRegion.UP_LEFT;

        return null;
         */
    }

    private int minIndex(double a, double b, double c, double d){

        if ((a <= b) && (a <=c) && (a <=d))
            return 0;
        else if ((b <= a) && (b <= c) && (b <= d))
                return 1;
        else if ((c <= a) && (c <=b) && (c <=d))
                return 2;
        else return 3;

    }

    private double distance(Point pRegion, Point p){

        return Math.sqrt((double)((pRegion.x-p.x)*(pRegion.x-p.x)) + (double)((pRegion.y-p.y)*(pRegion.y-p.y)));

    }

}
