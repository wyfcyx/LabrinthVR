package com.shinbokuow.labyrinthvr;

public class MeshedRectangle {
    private float[] coordinates;
    /* x, y, z of TopRight, TopLeft, BottomLeft, BottomRight */
    public MeshedRectangle(float[] _coordinates, int offset) {
        coordinates = new float[12];
        for (int i = 0; i < 12; ++i)
            coordinates[i] = _coordinates[offset + i];
    }
}
