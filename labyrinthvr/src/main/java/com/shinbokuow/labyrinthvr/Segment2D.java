package com.shinbokuow.labyrinthvr;

import android.util.Log;

public class Segment2D {
    private Point2D p, v;
    private static final double EPSILON = 1e-6;

    private static boolean intersect;
    private static Point2D intersection;
    private static String TAG = "Segment2D";

    public Segment2D(Point2D a, Point2D b) {
        p = a;
        v = b.dec(a);
    }
    public Segment2D(double x1, double y1, double x2, double y2) {
        p = new Point2D(x1, y1);
        v = new Point2D(x2 - x1, y2 - y1);
    }

    @Override
    public String toString() {
        String str = String.format(
                "p=(%f,%f),v=(%f,%f)",
                p.getX(), p.getY(),
                v.getX(), v.getY()
        );
        return str;
    }

    public Point2D getStartPoint() {
        return p;
    }
    public Point2D getDirection() {
        return v;
    }

    public static int dcmp(double d) {
        if (Math.abs(d) < EPSILON)
            return 0;
        else if (d < 0)
            return -1;
        else
            return 1;
    }
    public boolean onSegment(Point2D a) {
        Point2D va = a.dec(p);
        if (dcmp(va.cross(v)) == 0 && dcmp(va.dot(v)) >= 0 && dcmp(va.length() - v.length()) <= 0)
            return true;
        return false;
    }
    public static boolean getIntersect() {
        return intersect;
    }
    public static Point2D getIntersection() {
        return intersection;
    }
    public static void getSegmentIntersection(Segment2D A, Segment2D B) {
        //Log.i(TAG, "getSegmentIntersectionBegin");
        // assume that at most one intersection
        intersect = false;
        // check parallel
        if (dcmp(A.v.cross(B.v)) == 0) {
            //Log.i(TAG, "parallel");
            //Log.i(TAG, "getSegmentIntersectionEnd");
            return;
        }
        // find intersection
        double stepA = B.p.dec(A.p).cross(B.v) / A.v.cross(B.v);
        //Log.i(TAG, "stepA=" + String.valueOf(stepA));
        intersection = A.p.inc(A.v.scaMul(stepA));
        //Log.i(TAG, "intersection=" + intersection.toString());
        if (stepA >= -EPSILON && stepA <= 1 + EPSILON && B.onSegment(intersection))
            intersect = true;
        /*
        if (intersect) {
            Log.i(TAG, "passed B test");
        }
        else {
            Log.i(TAG, "B test failed");
        }
        Log.i(TAG, "getSegmentIntersectionEnd");
        */
    }
}
