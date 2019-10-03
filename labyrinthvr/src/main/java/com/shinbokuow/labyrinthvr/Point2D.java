package com.shinbokuow.labyrinthvr;

public class Point2D {
    private double x, y;

    public Point2D(double _x, double _y) {
        x = _x;
        y = _y;
    }
    public double getX() { return x; }
    public double getY() { return y; }
    @Override
    public String toString() {
        return String.format("p(%f,%f)", x, y);
    }

    public Point2D inc(Point2D B) {
        return new Point2D(this.x + B.x, this.y + B.y);
    }

    public Point2D dec(Point2D B) {
        return new Point2D(this.x - B.x, this.y - B.y);
    }

    public Point2D scaMul(double p) {
        return new Point2D(this.x * p, this.y * p);
    }

    public double dot(Point2D B) {
        return this.x * B.x + this.y * B.y;
    }

    public double cross(Point2D B) {
        return this.x * B.y - this.y * B.x;
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public static double distance(Point2D A, Point2D B) {
        return Math.sqrt((A.x - B.x) * (A.x - B.x) + (A.y - B.y) * (A.y - B.y));
    }
}
