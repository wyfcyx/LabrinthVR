package com.shinbokuow.labyrinthvr;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.google.vr.ndk.base.Properties;
import com.google.vr.ndk.base.Properties.PropertyType;
import com.google.vr.ndk.base.Value;
import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;

public class MainActivity extends GvrActivity implements GvrView.StereoRenderer {

    private GvrAudioEngine gvrAudioEngine;

    private Properties gvrProperties;

    private ArrayList<MeshedRectangle> wallRect;
    private ArrayList<MeshedRectangle> floorRect;
    private ArrayList<MeshedRectangle> doorRect;
    private ArrayList<MeshedRectangle> ceilingRect;

    private Texture wallTexture;
    private Texture floorTexture;
    private Texture doorTexture;

    private static final String VERTEX_SHADER_CODE_PATH =
            new String("default_vs.glsl");
    private static final String FRAGMENT_SHADER_CODE_PATH =
            new String("default_fs.glsl");

    private int objectProgram;
    private int objectPositionParam;
    private int objectUVParam;
    private int objectModelViewProjectionParam;

    private static final float Z_NEAR = 0.01f;
    private static final float Z_FAR = 10.0f;

    private float[] camera;
    private float[] view;
    private float[] modelViewProjection;
    private float[] cameraPosition;
    private float[] cameraDirection;
    private float[] headForwardDirection;
    //private float[] model;
    //private float[] modelView;

    private boolean isMoving;
    private long lastTime;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initialGvrView();

        gvrAudioEngine = new GvrAudioEngine(
                this,
                GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY
        );

        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        cameraPosition = new float[3];
        cameraDirection = new float[3];
        headForwardDirection = new float[3];
        //model = new float[16];
        //modelView = new float[16];
    }

    public void initialGvrView() {
        setContentView(R.layout.activity_main);

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(true);

        gvrView.enableCardboardTriggerEmulation();

        if (gvrView.setAsyncReprojectionEnabled(true)) {
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }

        setGvrView(gvrView);
        gvrProperties = gvrView.getGvrApi().getCurrentProperties();
    }
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onRendererShutdown() {}

    @Override
    public void onSurfaceChanged(int width, int height) {}

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        Log.i("TAG", loadCode(VERTEX_SHADER_CODE_PATH));
        Log.i("TAG", loadCode(FRAGMENT_SHADER_CODE_PATH));
        objectProgram = Utility.compileProgram(
                loadCode(VERTEX_SHADER_CODE_PATH),
                loadCode(FRAGMENT_SHADER_CODE_PATH)
        );
        Log.i("TAG", "objectProgram = " + String.valueOf(objectProgram));

        objectPositionParam = GLES20.glGetAttribLocation(
                objectProgram,
                "a_Position"
        );
        Log.i("TAG", "objectPositionParam = " + String.valueOf(objectPositionParam));

        objectUVParam = GLES20.glGetAttribLocation(
                objectProgram,
                "a_UV"
        );
        Log.i("TAG", "objectUVParam = " + String.valueOf(objectUVParam));

        objectModelViewProjectionParam = GLES20.glGetUniformLocation(
                objectProgram,
                "u_MVP"
        );
        Log.i("TAG", "objectMVP = " + String.valueOf(objectModelViewProjectionParam));

        cameraPosition[0] = 0.5f;
        cameraPosition[1] = 0.7f;
        cameraPosition[2] = 1.5f;
        cameraDirection[0] = 0.0f;
        cameraDirection[1] = 0.0f;
        cameraDirection[2] = 1.0f;

        float[] testRectCoord = new float[]{
            /* TopRight */ 1.0f, 1.0f, -5.0f,
            /* TopLeft */ -1.0f, 1.0f, -5.0f,
            /* BottomLeft */ -1.0f, -1.0f, -5.0f,
            /* BottomRight */ 1.0f, -1.0f, -5.0f
        };

        doorRect = new ArrayList<>();
        wallRect = new ArrayList<>();
        floorRect = new ArrayList<>();
        ceilingRect = new ArrayList<>();
        float[] door = new float[] {
                1.0f, 1.0f, 3.0f,
                0.0f, 1.0f, 3.0f,
                0.0f, 0.0f, 3.0f,
                1.0f, 0.0f, 3.0f
        };
        doorRect.add(
                new MeshedRectangle(
                        door, 0,
                        objectPositionParam,
                        objectUVParam
                )
        );
        for (int z = 1; z <= 3; ++z) {
            float[] floor = new float[] {
                    1.0f, 0.0f, (float)z,
                    0.0f, 0.0f, (float)z,
                    0.0f, 0.0f, (float)(z - 1),
                    1.0f, 0.0f, (float)(z - 1)
            };
            floorRect.add(
                    new MeshedRectangle(
                            floor, 0,
                            objectPositionParam,
                            objectUVParam
                    )
            );
            float[] leftWall = new float[] {
                    0.0f, 1.0f, (float)z,
                    0.0f, 1.0f, (float)(z - 1),
                    0.0f, 0.0f, (float)(z - 1),
                    0.0f, 0.0f, (float)z
            };
            wallRect.add(
                    new MeshedRectangle(
                            leftWall, 0,
                            objectPositionParam,
                            objectUVParam
                    )
            );
            /* x 1,1,1,1 y 1,1,0,0 z z-1,z,z,z-1 */
            float[] rightWall = new float[] {
                    1.0f, 1.0f, (float)(z - 1),
                    1.0f, 1.0f, (float)z,
                    1.0f, 0.0f, (float)z,
                    1.0f, 0.0f, (float)(z - 1)
            };
            wallRect.add(
                    new MeshedRectangle(
                            rightWall, 0,
                            objectPositionParam,
                            objectUVParam
                    )
            );
        }

        try {
            wallTexture = new Texture(
                    this,
                    "wall.jpg"
            );
            floorTexture = new Texture(
                    this,
                    "floor.jpg"
            );
            doorTexture = new Texture(
                    this,
                    "door.jpg"
            );
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        isMoving = false;
        lastTime = -1;
    }

    private String loadCode(String codePath) {
        String code = "";
        String line = "";
        try {
            InputStream input = getAssets().open(codePath);
            InputStreamReader reader = new InputStreamReader(input);
            BufferedReader bufReader = new BufferedReader(reader);
            while (true) {
                line = bufReader.readLine();
                if (line == null)
                    break;
                code = code + "\n" + line;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return code;
    }
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        long currentTime = System.currentTimeMillis();

        headTransform.getForwardVector(
                headForwardDirection, 0
        );
        float r = (float)Math.sqrt(
                headForwardDirection[0] * headForwardDirection[0] +
                headForwardDirection[1] * headForwardDirection[1]
        );
        float xStep = -headForwardDirection[0] / r;
        float zStep = -headForwardDirection[2] / r;
        if (isMoving && lastTime != -1) {
            cameraPosition[0] += 0.5f / 1000 * (currentTime - lastTime) * xStep;
            cameraPosition[2] += 0.5f / 1000 * (currentTime - lastTime) * zStep;
        }
        lastTime = currentTime;
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        Matrix.setLookAtM(
                camera, 0,
                cameraPosition[0], cameraPosition[1], cameraPosition[2],
                cameraPosition[0] + cameraDirection[0],
                cameraPosition[1] + cameraDirection[1],
                cameraPosition[2] + cameraDirection[2],
                0.0f, 1.0f, 0.0f
        );
        Matrix.multiplyMM(
                view, 0,
                eye.getEyeView(), 0,
                camera, 0
        );
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        Matrix.multiplyMM(
                modelViewProjection, 0,
                perspective, 0,
                view, 0
        );

        GLES20.glUseProgram(objectProgram);
        Utility.checkGlError("glUseProgram");

        GLES20.glUniformMatrix4fv(
                objectModelViewProjectionParam,
                1,
                false,
                modelViewProjection, 0
        );
        Utility.checkGlError("glUniformMatrix4fv");
        for (MeshedRectangle door: doorRect) {
            doorTexture.bind();
            door.draw();
        }
        for (MeshedRectangle floor: floorRect) {
            floorTexture.bind();
            floor.draw();
        }
        for (MeshedRectangle wall: wallRect) {
            wallTexture.bind();
            wall.draw();
        }
        Utility.checkGlError("testRect.draw()");
    }

    @Override
    public void onFinishFrame(Viewport viewport) {}


    @Override
    public void onCardboardTrigger() {
        isMoving = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_UP:
                Log.i("ACTION", "released...");
                isMoving = false;
                break;
        }
        return true;
    }
}
