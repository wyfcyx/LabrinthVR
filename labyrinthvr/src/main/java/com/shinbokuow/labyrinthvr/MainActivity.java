package com.shinbokuow.labyrinthvr;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;

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

import javax.microedition.khronos.egl.EGLConfig;

public class MainActivity extends GvrActivity implements GvrView.StereoRenderer {

    private GvrAudioEngine gvrAudioEngine;

    private Properties gvrProperties;

    private MeshedRectangle testRect;
    private Texture wallTexture;

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

        float[] testRectCoord = new float[]{
            /* TopRight */ 1.0f, 1.0f, -5.0f,
            /* TopLeft */ -1.0f, 1.0f, -5.0f,
            /* BottomLeft */ -1.0f, -1.0f, -5.0f,
            /* BottomRight */ 1.0f, -1.0f, -5.0f
        };

        testRect = new MeshedRectangle(
                testRectCoord, 0,
                objectPositionParam,
                objectUVParam
        );


        try {
            wallTexture = new Texture(
                    this,
                    "wall.jpg"
            );
        }
        catch (IOException e) {
            e.printStackTrace();
        }

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
    public void onNewFrame(HeadTransform headTransform) {}

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        Matrix.setLookAtM(
                camera, 0,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, -1.0f,
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

        wallTexture.bind();
        testRect.draw();
        Utility.checkGlError("testRect.draw()");
    }

    @Override
    public void onFinishFrame(Viewport viewport) {}
}
