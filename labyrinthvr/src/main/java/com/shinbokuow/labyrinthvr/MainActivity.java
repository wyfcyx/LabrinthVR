package com.shinbokuow.labyrinthvr;

import android.graphics.Point;
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

    private static final int N = 15;
    private static final float Z_NEAR = 0.01f;
    private static final float Z_FAR = (float)(2 * N + 1);

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
    private static final float SPEED_PER_MS = 1.5f / 1000;

    private SegmentTriggerSystem triggerSystem;

    private int[][] map;
    private static final float AIR_WALL_DISTANCE = .15f;

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
        triggerSystem = new SegmentTriggerSystem();

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

        sceneReset();

    }
    private void sceneReset() {
        doorRect = new ArrayList<>();
        wallRect = new ArrayList<>();
        floorRect = new ArrayList<>();
        ceilingRect = new ArrayList<>();
        triggerSystem.reset();

        isMoving = false;
        lastTime = -1;

        cameraPosition[0] = 1.5f;
        cameraPosition[1] = 1.7f;
        cameraPosition[2] = 1.5f;
        cameraDirection[0] = 0.0f;
        cameraDirection[1] = 0.0f;
        cameraDirection[2] = 1.0f;

        map = new LabGenerator().generate(N);
        for (int z = 0; z < 2 * N + 1; ++z) {
            for (int x = 0; x < 2 * N + 1; ++x) {
                Log.i("Main", "z="+String.valueOf(z)+",x="+String.valueOf(x)+",(z,x)="+String.valueOf(map[z][x]));
                if (map[z][x] == 1) {
                    // a wall
                    float[] wall = null;
                    Point2D direction = null;
                    Point2D p1 = null, p2 = null;
                    if (x - 1 >= 0 && map[z][x - 1] == 0) {
                        // a right wall
                        for (int y = 0; y < 2; ++y) {
                            wall = new float[]{
                                    (float) x, 1.0f + y, (float) z,
                                    (float) x, 1.0f + y, (float) (z + 1),
                                    (float) x, 0.0f + y, (float) (z + 1),
                                    (float) x, 0.0f + y, (float) z
                            };
                            addRect(wallRect, wall);
                        }
                        direction = new Point2D(-1.0f, 0.0f);
                        p1 = new Point2D((float) x, (float) z);
                        p2 = new Point2D((float) x, (float) (z + 1));
                        addBoundTrigger(
                                direction,
                                p1, new Point2D(0.0f, -1.0f),
                                p2, new Point2D(0.0f, 1.0f)
                        );
                    }
                    if (x + 1 < 2 * N + 1 && map[z][x + 1] == 0) {
                        // a left wall
                        for (int y = 0; y < 2; ++y) {
                            wall = new float[]{
                                    (float) (x + 1), 1.0f + y, (float) z,
                                    (float) (x + 1), 1.0f + y, (float) (z + 1),
                                    (float) (x + 1), 0.0f + y, (float) (z + 1),
                                    (float) (x + 1), 0.0f + y, (float) z
                            };
                            addRect(wallRect, wall);
                        }
                        direction = new Point2D(1.0f, 0.0f);
                        p1 = new Point2D((float) (x + 1), (float) z);
                        p2 = new Point2D((float) (x + 1), (float) (z + 1));
                        addBoundTrigger(
                                direction,
                                p1, new Point2D(0.0f, -1.0f),
                                p2, new Point2D(0.0f, 1.0f)
                        );
                    }
                    if (z - 1 >= 0 && map[z - 1][x] == 0) {
                        // a down wall
                        for (int y = 0; y < 2; ++y) {
                            wall = new float[]{
                                    (float) x, 1.0f + y, (float) z,
                                    (float) (x + 1), 1.0f + y, (float) z,
                                    (float) (x + 1), 0.0f + y, (float) z,
                                    (float) x, 0.0f + y, (float) z
                            };
                            if (x == 2 * N - 1 && z == 2 * N && y == 1)
                                addRect(doorRect, wall);
                            else
                                addRect(wallRect, wall);
                        }
                        direction = new Point2D(0.0f, -1.0f);
                        p1 = new Point2D((float) x, (float) z);
                        p2 = new Point2D((float) (x + 1), (float) z);
                        if (x == 2 * N - 1 && z == 2 * N) {
                            addExitTrigger(
                                    direction,
                                    p1, new Point2D(-1.0f, 0.0f),
                                    p2, new Point2D(1.0f, 0.0f)
                            );
                        }
                        else {
                            addBoundTrigger(
                                    direction,
                                    p1, new Point2D(-1.0f, 0.0f),
                                    p2, new Point2D(1.0f, 0.0f)
                            );
                        }

                    }
                    if (z + 1 < 2 * N + 1 && map[z + 1][x] == 0) {
                        // a up wall
                        for (int y = 0; y < 2; ++y) {
                            wall = new float[]{
                                    (float) (x + 1), 1.0f + y, (float) (z + 1),
                                    (float) x, 1.0f + y, (float) (z + 1),
                                    (float) x, 0.0f + y, (float) (z + 1),
                                    (float) (x + 1), 0.0f + y, (float) (z + 1)
                            };
                            addRect(wallRect, wall);
                        }
                        direction = new Point2D(0.0f, 1.0f);
                        p1 = new Point2D((float) x, (float) (z + 1));
                        p2 = new Point2D((float) (x + 1), (float) (z + 1));
                        //addBoundTrigger(direction, p1, p2);
                        addBoundTrigger(
                                direction,
                                p1, new Point2D(-1.0f, 0.0f),
                                p2, new Point2D(1.0f, 0.0f)
                        );

                    }
                }
                else {
                    // a floor
                    float[] floor = new float[] {
                            (float)x, 0.0f, (float)z,
                            (float)(x + 1), 0.0f, (float)z,
                            (float)(x + 1), 0.0f, (float)(z + 1),
                            (float)x, 0.0f, (float)(z + 1)
                    };
                    floorRect.add(
                            new MeshedRectangle(
                                    floor, 0,
                                    objectPositionParam,
                                    objectUVParam
                            )
                    );

                }
            }
        }
    }
    private void addRect(ArrayList<MeshedRectangle> list, float[] wall) {
        list.add(
                new MeshedRectangle(
                        wall, 0,
                        objectPositionParam,
                        objectUVParam
                )
        );
    }
    private void addBoundTrigger(Point2D direction, Point2D p1, Point2D p1Out, Point2D p2, Point2D p2Out) {
        p1 = p1.inc(direction.inc(p1Out).scaMul(AIR_WALL_DISTANCE));
        p2 = p2.inc(direction.inc(p2Out).scaMul(AIR_WALL_DISTANCE));
        triggerSystem.addSegmentTrigger(
                new SegmentTrigger(
                        new Segment2D(p1, p2),
                        direction,
                        SegmentTrigger.TriggerType.BOUND_TRIGGER
                )
        );
    }
    private void addExitTrigger(Point2D direction, Point2D p1, Point2D p1Out, Point2D p2, Point2D p2Out) {
        p1 = p1.inc(direction.inc(p1Out).scaMul(AIR_WALL_DISTANCE));
        p2 = p2.inc(direction.inc(p2Out).scaMul(AIR_WALL_DISTANCE));
        triggerSystem.addSegmentTrigger(
                new SegmentTrigger(
                        new Segment2D(p1, p2),
                        direction,
                        SegmentTrigger.TriggerType.EXIT_TRIGGER
                )
        );
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
                headForwardDirection[2] * headForwardDirection[2]
        );
        float xStep = -headForwardDirection[0] / r;
        float zStep = -headForwardDirection[2] / r;
        if (isMoving && lastTime != -1) {
            float nextx = cameraPosition[0] + SPEED_PER_MS * (currentTime - lastTime) * xStep;
            float nextz = cameraPosition[2] + SPEED_PER_MS * (currentTime - lastTime) * zStep;
            // handling collides through triggerSystem
            long triggerCost = System.currentTimeMillis();
            SegmentTrigger trigger = triggerSystem.getTrigger(
                    new Segment2D(
                            new Point2D(cameraPosition[0], cameraPosition[2]),
                            new Point2D(nextx, nextz)
                    )
            );
            Log.i("Main", "triggerCost = " + String.valueOf(System.currentTimeMillis() - triggerCost));
            if (trigger != null) {
                if (trigger.getTriggerType() == SegmentTrigger.TriggerType.BOUND_TRIGGER) {
                    Log.i("Main", "BOUND_TRIGGER");
                    Point2D intersection = trigger.getIntersection();
                    cameraPosition[0] = (float)intersection.getX();
                    cameraPosition[2] = (float)intersection.getY();
                }
                else if (trigger.getTriggerType() == SegmentTrigger.TriggerType.EXIT_TRIGGER) {
                    Log.i("Main", "EXIT_TRIGGER");
                    sceneReset();
                }
            }
            else {
                Log.i("Main", "NO_TRIGGER");
                // no collide found
                cameraPosition[0] = nextx;
                cameraPosition[2] = nextz;
            }
        }


        Log.i(
                "Main",
                "updated position:" +
                        " x = " + String.valueOf(cameraPosition[0]) +
                        " y = " + String.valueOf(cameraPosition[1]) +
                        " z = " + String.valueOf(cameraPosition[2])
        );
        Log.i("Main", "FrameTime = " + String.valueOf(currentTime - lastTime) + "ms");
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
        doorTexture.bind();
        for (MeshedRectangle door: doorRect) {
            door.draw();
        }
        floorTexture.bind();
        for (MeshedRectangle floor: floorRect) {
            floor.draw();
        }
        wallTexture.bind();
        for (MeshedRectangle wall: wallRect) {
            wall.draw();
        }
        Utility.checkGlError("testRect.draw()");
        //Log.i("Main", "discarded = " + String.valueOf(discarded));
        //Log.i("Main", "total = " + String.valueOf(total));
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
