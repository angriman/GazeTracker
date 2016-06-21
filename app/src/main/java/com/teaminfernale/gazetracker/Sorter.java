package com.teaminfernale.gazetracker;

import org.opencv.core.Point;

/**
 * Class used to sort the eyes-point array
 */
public class Sorter {

    /**
     * Return the median value given an array list of int
     * @param list Array of numbers to find find the median of
     */
    public int getMedian(int[] list) {
        if (list.length > 0) {
            if (isOdd(list.length)) {
                return list[(list.length + 1) / 2];
            }
            int result = 0;
            result = (list[(list.length) / 2] + list[(list.length) / 2 + 1]) / 2;
            return result;
        }
        return 0;
    }


    /**
     * True if the number is odd, false otherwise
     * @param i number to check
     */
    private boolean isOdd(int i) {
        return i % 2 == 0;
    }


    /**
     * Sort an array of points.
     * @param list Array of points to sort
     */
    public Point[] sort(Point[] list) {
        if (list.length == 1) {
            return list;
        }

        Point[] firstList = new Point[list.length/2];
        Point[] secondList = new Point[list.length - firstList.length];

        System.arraycopy(list, 0, firstList, 0, firstList.length);
        System.arraycopy(list, firstList.length, secondList, 0, secondList.length);

        firstList = sort(firstList);
        secondList = sort(secondList);

        return merge(firstList, secondList);
    }


    /**
     * Ausiliar method for merge sort. Returns a single array of the points in the two list merged.
     * @param list1 first array of points to merge
     * @param list2 second array of points to merge
     */
    private Point[] merge(Point[] list1, Point[] list2) {
        int i = 0;
        int j = 0;
        int count = 0;
        Point[] result = new Point[list1.length + list2.length];

        while (count < result.length) {

            if (i == list1.length) {
                result[count] = list2[j];
                j++;
                count++;
            }
            else if (j == list2.length) {
                result[count] = list1[i];
                i++;
                count++;
            }
            else {
                switch (comparePoints(list1[i], list2[j])) {
                    case -1:
                        result[count] = list1[i];
                        i++;
                        count++;
                        break;
                    case 0:
                        result[count] = list1[i];
                        i++;
                        count++;
                        break;
                    case 1:
                        result[count] = list2[j];
                        j++;
                        count++;
                        break;
                }
            }
        }

        return result;
    }


    /**
     * Ausiliar method for merge sort. Returns 1 if p1 > p2, 0 if p1 = p2, -1 if p1 < p2.
     * Checks the x values first, then the y ones.
     * @param p1 first array of points to merge
     * @param p2 second array of points to merge
     */
    private int comparePoints(Point p1, Point p2) {
        if (p1.x > p2.x) {
            return 1;
        }
        else if (p1.x == p2.x) {
            if (p1.y > p2.y) {
                return 1;
            }
            else if (p1.y == p2.y) {
                return 0;
            }
            else {
                return -1;
            }
        }
        else {
            return -1;
        }
    }
}