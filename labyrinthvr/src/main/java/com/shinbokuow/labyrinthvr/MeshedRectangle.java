package com.shinbokuow.labyrinthvr;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class MeshedRectangle {
    private int objectPositionParam;
    private int objectUVParam;
    private FloatBuffer vertexBuffer;
    private FloatBuffer uvBuffer;
    private ShortBuffer indiceBuffer;
    private float[] coordinates;
    /* x, y, z of TopRight, TopLeft, BottomLeft, BottomRight */
    public MeshedRectangle(float[] _coordinates, int offset, int _objectPositionParam, int _objectUVParam) {
        coordinates = new float[12];
        for (int i = 0; i < 12; ++i)
            coordinates[i] = _coordinates[offset + i];
        objectPositionParam = _objectPositionParam;
        objectUVParam = _objectUVParam;

        ByteBuffer byteVertex = ByteBuffer.allocateDirect(coordinates.length * 4);
        byteVertex.order(ByteOrder.nativeOrder());
        vertexBuffer = byteVertex.asFloatBuffer();
        vertexBuffer.put(coordinates);
        vertexBuffer.position(0);

        float[] uv = new float[]{
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f
        };
        ByteBuffer byteUV = ByteBuffer.allocateDirect(uv.length * 4);
        byteUV.order(ByteOrder.nativeOrder());
        uvBuffer = byteUV.asFloatBuffer();
        uvBuffer.put(uv);
        uvBuffer.position(0);

        short[] indices = new short[]{
                0, 1, 2,
                2, 3, 0
        };
        ByteBuffer byteIndice = ByteBuffer.allocateDirect(indices.length * 2);
        byteIndice.order(ByteOrder.nativeOrder());
        indiceBuffer = byteIndice.asShortBuffer();
        indiceBuffer.put(indices);
        indiceBuffer.position(0);
    }
    public void draw() {
        GLES20.glEnableVertexAttribArray(objectPositionParam);
        GLES20.glVertexAttribPointer(
                objectPositionParam,
                3,
                GLES20.GL_FLOAT,
                false,
                0,
                vertexBuffer
        );

        GLES20.glEnableVertexAttribArray(objectUVParam);
        GLES20.glVertexAttribPointer(
                objectUVParam,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                uvBuffer
        );

        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,
                indiceBuffer.limit(),
                GLES20.GL_UNSIGNED_SHORT,
                indiceBuffer
        );

    }
    public boolean isVisible(float[] modelViewProjection) {
        // if four vertexes are all not visible,
        // then this rectangle will be discarded
        boolean canSee = false;
        for (int v = 0; v < 4; ++v) {
            float[] coor = new float[] {
                  coordinates[3 * v],
                  coordinates[3 * v + 1],
                  coordinates[3 * v + 2],
                  1.0f
            };
            Matrix.multiplyMV(
                    coor, 0,
                    modelViewProjection, 0,
                    coor, 0
            );
            if (Math.abs(coor[0]) < coor[3] && Math.abs(coor[1]) < coor[3] && Math.abs(coor[2]) < coor[3]) {
                canSee = true;
                break;
            }
        }
        return canSee;
    }
}
