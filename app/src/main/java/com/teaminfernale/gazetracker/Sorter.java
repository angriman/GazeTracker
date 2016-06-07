package com.teaminfernale.gazetracker;

import org.opencv.core.Point;

/**
 * Created by the awesome Eugenio on 6/7/16.
 */
public class Sorter {

    public Point getMedian(Point[] list) {
        if (list.length > 0) {
            if (isOdd(list.length)) {
                return list[(list.length + 1) / 2];
            }
            Point result = new Point();
            result.x = (list[(list.length) / 2].x + list[(list.length) / 2 + 1].x) / 2;
            result.y = (list[(list.length) / 2].y + list[(list.length) / 2 + 1].y) / 2;
            return result;
        }
        return new Point();
    }

    private boolean isOdd(int i) {
        return i % 2 == 0;
    }


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
