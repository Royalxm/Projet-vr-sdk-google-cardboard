/*
 * Copyright 2014 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.vrtoolkit.cardboard.samples.treasurehunt;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.TimeUnit;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.KeyEvent;
import android.view.View;


public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    MediaPlayer mysound;
    private cube cube;

    SensorManager sensorManager;
    Sensor sensor;

    private float gero[] =  new float[3];

    private final String vss =
          "attribute vec2 vPosition;\n" +
                  "attribute vec2 vTexCoord;\n" +
                  "varying vec2 texCoord;\n" +
                  "  texCoord = vTexCoord;\n" +
                  "  gl_Position = vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
                  "}";

  private final String fss =
   " #ifdef GL_ES"+
    " precision mediump float;"+
            " #endif"+

            "  uniform float time;"+
            "  uniform vec2 resolution;"+

            "  void main( void ) {"+

            "     vec2 position = ( gl_FragCoord.xy / resolution.xy ) + 10.0 / 4.0;"+

            "     float color = 0.0;"+
            "     color += sin( position.x * cos( time / 15.0 ) * 80.0 ) + cos( position.y * cos( time / 15.0 ) * 10.0 );"+
    "    color += sin( position.y * sin( time / 10.0 ) * 40.0 ) + cos( position.x * sin( time / 25.0 ) * 40.0 );"+
            "     color += sin( position.x * sin( time / 5.0 ) * 10.0 ) + sin( position.y * sin( time / 35.0 ) * 80.0 );"+
            "     color *= sin( time / 10.0 ) * 0.5;"+

            "     gl_FragColor = vec4( vec3( color, color * 0.5, sin( color + time / 3.0 ) * 0.75 ), 1.0 );"+

            "  }";



  private int hProgram;

    private Boolean geros = false;
    private Boolean gero_up = false;



  private static final String TAG = "MainActivity";

  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 100.0f;

  private static final float CAMERA_Z = 0.01f;
  private static final float TIME_DELTA = 0.3f;

  private static final float YAW_LIMIT = 0.03f;
  private static final float PITCH_LIMIT = 0.12f;

  private static final int COORDS_PER_VERTEX = 3;

  // We keep the light always position just above the user.
  private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] { 0.0f, 2.0f, 0.0f, 1.0f };

  private final float[] lightPosInEyeSpace = new float[4];

  private FloatBuffer floorVertices;
  private FloatBuffer floorColors;
  private FloatBuffer floorNormals;

  private FloatBuffer floorskyVertices;
  private FloatBuffer floorskyColors;
  private FloatBuffer floorskyNormals;

  private FloatBuffer cubeVertices;
  private FloatBuffer cubeColors;
  private FloatBuffer cubeFoundColors;
  private FloatBuffer cubeNormals;

  private int cubeProgram;
  private int floorProgram;
  private int floorskyProgram;

  private int cubePositionParam;
  private int cubeNormalParam;
  private int cubeColorParam;
  private int cubeModelParam;
  private int cubeModelViewParam;
  private int cubeModelViewProjectionParam;
  private int cubeLightPosParam;

  private int floorPositionParam;
  private int floorNormalParam;
  private int floorColorParam;
  private int floorModelParam;
  private int floorModelViewParam;
  private int floorModelViewProjectionParam;
  private int floorLightPosParam;
    // tab

  private int floorskyPositionParam;
  private int floorskyNormalParam;
  private int floorskyColorParam;
  private int floorskyModelParam;
  private int floorskyModelViewParam;
  private int floorskyModelViewProjectionParam;
  private int floorskyLightPosParam;

  private float[][] modelCube =  new float[500][16];
  private float[] modelCube2;
  private float vitesse = 0.0f;
    int taillemap = 16;
  private float[] camera;
  private float[] view;
  private float[] headView;
  private float[] modelViewProjection;
  private float[] modelView;
  private float[][] modelsky;
  private float[] modelFloor;
 // int[] textures = new int[1];
 private float[] possition =  new float[16];
  private int score = 0;
  private float objectDistance = 12f;
  private float floorDepth = 1.0f;

  private Vibrator vibrator;
  private CardboardOverlayView overlayView;

  private int start = 0;
    private  float obj1 = -10.0f;
    private  float obj2 = -10.0f;
    private  float obj3 = -10.0f;


    private FloatBuffer textureBuffer;
    private double timeElapsed = 0, finalTime = 0;

    public SensorEventListener gyroListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) { }

        public void onSensorChanged(SensorEvent event) {
            gero[0] = event.values[0];
            gero[1] = event.values[1];
            gero[2] =  event.values[2];


        }
    };

    /**
   * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
   *
   * @param type The type of shader we will be creating.
   * @param resId The resource ID of the raw text file about to be turned into a shader.
   * @return The shader object handler.
   */
  private int loadGLShader(int type, int resId) {
    String code = readRawTextFile(resId);
    int shader = GLES20.glCreateShader(type);
    GLES20.glShaderSource(shader, code);
    GLES20.glCompileShader(shader);

    // Get the compilation status.
    final int[] compileStatus = new int[1];
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

    // If the compilation failed, delete the shader.
    if (compileStatus[0] == 0) {
      Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
      GLES20.glDeleteShader(shader);
      shader = 0;
    }

    if (shader == 0) {
      throw new RuntimeException("Error creating shader.");
    }

    return shader;
  }

  /**
   * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
   *
   * @param label Label to report in case of error.
   */
  private static void checkGLError(String label) {
    int error;
    while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
      Log.e(TAG, label + ": glError " + error);
      throw new RuntimeException(label + ": glError " + error);
    }
  }

  /**
   * Sets the view to our CardboardView and initializes the transformation matrices we will use
   * to render our scene.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {

     //sound
      mysound = MediaPlayer.create(this,R.raw.one);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.common_ui);
    CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
    cardboardView.setRestoreGLStateEnabled(false);
    cardboardView.setRenderer(this);
    setCardboardView(cardboardView);


    modelCube2 = new float[300];
    camera = new float[16];
    view = new float[16];
    modelViewProjection = new float[16];
    modelView = new float[16];
    modelFloor = new float[16];
    modelsky = new float[5][16];
    headView = new float[16];
    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


    overlayView = (CardboardOverlayView) findViewById(R.id.overlay);
    overlayView.show3DToast("Pull the magnet when you find an object.");
  }

  @Override
  public void onRendererShutdown() {
    Log.i(TAG, "onRendererShutdown");
  }

  @Override
  public void onSurfaceChanged(int width, int height) {
    Log.i(TAG, "onSurfaceChanged");
  }




  private static int loadShader ( String vss, String fss ) {
    int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
    GLES20.glShaderSource(vshader, vss);
    GLES20.glCompileShader(vshader);
    int[] compiled = new int[1];
    GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
    if (compiled[0] == 0) {
      Log.e("Shader", "Could not compile vshader");
      Log.v("Shader", "Could not compile vshader:"+GLES20.glGetShaderInfoLog(vshader));
      GLES20.glDeleteShader(vshader);
      vshader = 0;
    }

    int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
    GLES20.glShaderSource(fshader, fss);
    GLES20.glCompileShader(fshader);
    GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
    if (compiled[0] == 0) {
      Log.e("Shader", "Could not compile fshader");
      Log.v("Shader", "Could not compile fshader:"+GLES20.glGetShaderInfoLog(fshader));
      GLES20.glDeleteShader(fshader);
      fshader = 0;
    }

    int program = GLES20.glCreateProgram();
    GLES20.glAttachShader(program, vshader);
    GLES20.glAttachShader(program, fshader);
    GLES20.glLinkProgram(program);

    return program;
  }


    private float texture[] = {
            // Mapping coordinates for the vertices
            0.0f, 1.0f,		// top left		(V2)
            0.0f, 0.0f,		// bottom left	(V1)
            1.0f, 1.0f,		// top right	(V4)
            1.0f, 0.0f		// bottom right	(V3)
    };

    /** The texture pointer */
    private int[] textures = new int[1];

    public void loadGLTexture(GLES20 gl, Context context) {
        // loading texture
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.ic_launcher);

        // generate one texture pointer
        GLES20.glGenTextures(1, textures, 0);
        // ...and bind it to our array
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        // create nearest filtered texture
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // Use Android GLUtils to specify a two-dimensional texture image from our bitmap
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        // Clean up
        bitmap.recycle();
    }

    GL10 gl;
  /**
   * Creates the buffers we use to store information about the 3D world.
   *
   * <p>OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
   * Hence we use ByteBuffers.
   *
   * @param config The EGL configuration used when creating the surface.
   */
  @Override
  public void onSurfaceCreated(EGLConfig config) {
    Log.i(TAG, "onSurfaceCreated");


    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.
   // hProgram = loadShader ( vss, fss );
    ByteBuffer bbVertices = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_COORDS.length * 4);
    bbVertices.order(ByteOrder.nativeOrder());
    cubeVertices = bbVertices.asFloatBuffer();
    cubeVertices.put(WorldLayoutData.CUBE_COORDS);
    cubeVertices.position(0);

    ByteBuffer bbColors = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_COLORS.length * 4);
    bbColors.order(ByteOrder.nativeOrder());
    cubeColors = bbColors.asFloatBuffer();
    cubeColors.put(WorldLayoutData.CUBE_COLORS);
    cubeColors.position(0);

    ByteBuffer bbFoundColors = ByteBuffer.allocateDirect(
        WorldLayoutData.CUBE_FOUND_COLORS.length * 4);
    bbFoundColors.order(ByteOrder.nativeOrder());
    cubeFoundColors = bbFoundColors.asFloatBuffer();
    cubeFoundColors.put(WorldLayoutData.CUBE_FOUND_COLORS);
    cubeFoundColors.position(0);

     /* ByteBuffer bbtexture = ByteBuffer.allocateDirect(texture.length * 4);
      bbtexture.order(ByteOrder.nativeOrder());
      textureBuffer = bbtexture.asFloatBuffer();
      textureBuffer.put(texture);
      textureBuffer.position(0);*/

    ByteBuffer bbNormals = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_NORMALS.length * 4);
    bbNormals.order(ByteOrder.nativeOrder());
    cubeNormals = bbNormals.asFloatBuffer();
    cubeNormals.put(WorldLayoutData.CUBE_NORMALS);
    cubeNormals.position(0);




    // make a floor
    ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_COORDS.length * 4);
    bbFloorVertices.order(ByteOrder.nativeOrder());
    floorVertices = bbFloorVertices.asFloatBuffer();
    floorVertices.put(WorldLayoutData.FLOOR_COORDS);
    floorVertices.position(0);

    ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_NORMALS.length * 4);
    bbFloorNormals.order(ByteOrder.nativeOrder());
    floorNormals = bbFloorNormals.asFloatBuffer();
    floorNormals.put(WorldLayoutData.FLOOR_NORMALS);
    floorNormals.position(0);

    ByteBuffer bbFloorColors = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_COLORS.length * 4);
    bbFloorColors.order(ByteOrder.nativeOrder());
    floorColors = bbFloorColors.asFloatBuffer();
    floorColors.put(WorldLayoutData.FLOOR_COLORS);
    floorColors.position(0);

//Sky
    ByteBuffer bbFloorskyVertices = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_COORDS_SKY.length * 4);
    bbFloorskyVertices.order(ByteOrder.nativeOrder());
    floorskyVertices = bbFloorskyVertices.asFloatBuffer();
    floorskyVertices.put(WorldLayoutData.FLOOR_COORDS_SKY);
    floorskyVertices.position(0);

    ByteBuffer bbFloorskyNormals = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_NORMALS.length * 4);
    bbFloorskyNormals.order(ByteOrder.nativeOrder());
    floorskyNormals = bbFloorskyNormals.asFloatBuffer();
    floorskyNormals.put(WorldLayoutData.FLOOR_NORMALS);
    floorskyNormals.position(0);

    ByteBuffer bbFloorskyColors = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_COLORS_sky.length * 4);
    bbFloorskyColors.order(ByteOrder.nativeOrder());
    floorskyColors = bbFloorskyColors.asFloatBuffer();
    floorskyColors.put(WorldLayoutData.FLOOR_COLORS_sky);
    floorskyColors.position(0);


    int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
    int gridShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
    int passthroughShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

    cubeProgram = GLES20.glCreateProgram();
    GLES20.glAttachShader(cubeProgram, vertexShader);
    GLES20.glAttachShader(cubeProgram, passthroughShader);
    GLES20.glLinkProgram(cubeProgram);
    GLES20.glUseProgram(cubeProgram);

    checkGLError("Cube program");

    cubePositionParam = GLES20.glGetAttribLocation(cubeProgram, "a_Position");
    cubeNormalParam = GLES20.glGetAttribLocation(cubeProgram, "a_Normal");
    cubeColorParam = GLES20.glGetAttribLocation(cubeProgram, "a_Color");

    cubeModelParam = GLES20.glGetUniformLocation(cubeProgram, "u_Model");
    cubeModelViewParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVMatrix");
    cubeModelViewProjectionParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVP");
    cubeLightPosParam = GLES20.glGetUniformLocation(cubeProgram, "u_LightPos");

    GLES20.glEnableVertexAttribArray(cubePositionParam);
    GLES20.glEnableVertexAttribArray(cubeNormalParam);
    GLES20.glEnableVertexAttribArray(cubeColorParam);

    checkGLError("Cube program params");

    floorProgram = GLES20.glCreateProgram();
    GLES20.glAttachShader(floorProgram, vertexShader);
    GLES20.glAttachShader(floorProgram, gridShader);
    GLES20.glLinkProgram(floorProgram);
    GLES20.glUseProgram(floorProgram);

    checkGLError("Floor program");

    floorModelParam = GLES20.glGetUniformLocation(floorProgram, "u_Model");
    floorModelViewParam = GLES20.glGetUniformLocation(floorProgram, "u_MVMatrix");
    floorModelViewProjectionParam = GLES20.glGetUniformLocation(floorProgram, "u_MVP");
    floorLightPosParam = GLES20.glGetUniformLocation(floorProgram, "u_LightPos");

    floorPositionParam = GLES20.glGetAttribLocation(floorProgram, "a_Position");
    floorNormalParam = GLES20.glGetAttribLocation(floorProgram, "a_Normal");
    floorColorParam = GLES20.glGetAttribLocation(floorProgram, "a_Color");

    GLES20.glEnableVertexAttribArray(floorPositionParam);
    GLES20.glEnableVertexAttribArray(floorNormalParam);
    GLES20.glEnableVertexAttribArray(floorColorParam);

    checkGLError("Floor program params");

//sky2

    floorskyProgram = GLES20.glCreateProgram();
    GLES20.glAttachShader(floorskyProgram, vertexShader);
    GLES20.glAttachShader(floorskyProgram, gridShader);
    GLES20.glLinkProgram(floorskyProgram);
    GLES20.glUseProgram(floorskyProgram);

    checkGLError("sky program");

    floorskyModelParam = GLES20.glGetUniformLocation(floorskyProgram, "u_Model");
    floorskyModelViewParam = GLES20.glGetUniformLocation(floorskyProgram, "u_MVMatrix");
    floorskyModelViewProjectionParam = GLES20.glGetUniformLocation(floorskyProgram, "u_MVP");
    floorskyLightPosParam = GLES20.glGetUniformLocation(floorskyProgram, "u_LightPos");

    floorskyPositionParam = GLES20.glGetAttribLocation(floorskyProgram, "a_Position");
    floorskyNormalParam = GLES20.glGetAttribLocation(floorskyProgram, "a_Normal");
    floorskyColorParam = GLES20.glGetAttribLocation(floorskyProgram, "a_Color");

    GLES20.glEnableVertexAttribArray(floorskyPositionParam);
    GLES20.glEnableVertexAttribArray(floorskyNormalParam);
    GLES20.glEnableVertexAttribArray(floorskyColorParam);

    checkGLError("sky program params");


    // Iniz map



     for(int i=0;i!=16;i++) {

         if(i%3 == 0)
         {
             possition[i] = -(float) (20 + (Math.random() * (150 - 20)));
             Matrix.setIdentityM(modelCube[i], 0);
             Matrix.translateM(modelCube[i], 0, 0, 0, possition[i]);
         }
   else if(i%3 == 1)
    {
        possition[i] = -(float) (20 + (Math.random() * (150 - 20)));
        Matrix.setIdentityM(modelCube[i], 0);
        Matrix.translateM(modelCube[i], 0, -5, 0, possition[i]);
    }
         else
    {
        possition[i] = -(float) (20 + (Math.random() * (150 - 20)));
        Matrix.setIdentityM(modelCube[i], 0);
        Matrix.translateM(modelCube[i], 0, 5, 0, possition[i]);
    }



      }

      possition[0] = obj1;
    Matrix.setIdentityM(modelCube[1], 0);
    Matrix.translateM(modelCube[1], 0, 0, 0, obj1);
      possition[1] = obj2;
    Matrix.setIdentityM(modelCube[2], 0);
    Matrix.translateM(modelCube[2], 0, -5, 0, obj2);
      possition[2] = obj2;
    Matrix.setIdentityM(modelCube[3], 0);
    Matrix.translateM(modelCube[3], 0, 5, 0, obj2);
    //texture :



      sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
      sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

    Matrix.setIdentityM(modelFloor, 0);
    Matrix.translateM(modelFloor, 0, 0, -floorDepth, 0); // Floor appears below user.

    checkGLError("onSurfaceCreated");


      Matrix.setIdentityM(modelsky[0], 0);
      Matrix.rotateM(modelsky[0], 0, 90, 1, 0, 0); // Floor appears below user.
      Matrix.translateM(modelsky[0], 0, 0, -10, -10); // Floor appears below user.

      Matrix.setIdentityM(modelsky[1], 0);
      Matrix.rotateM(modelsky[1], 0, 90, 1, 0, 0); // Floor appears below user.
      Matrix.translateM(modelsky[1], 0, 0, 10, -10); // Floor appears below user.


      Matrix.setIdentityM(modelsky[2], 0);
      Matrix.rotateM(modelsky[2], 0, 90, 0, 0, 1);
      Matrix.translateM(modelsky[2], 0, 8, 10, -(10+vitesse)); //up or ve

      Matrix.setIdentityM(modelsky[3], 0);
      Matrix.rotateM(modelsky[3], 0, 90, 0, 0, 1);
      Matrix.translateM(modelsky[3], 0, 8, -10, -(10+vitesse));

      Matrix.setIdentityM(modelsky[4], 0);
      Matrix.rotateM(modelsky[4], 0, 90, 0, 1, 0);
      Matrix.translateM(modelsky[4], 0, 0, (10-vitesse), -10);



      float angleCube = 0;
      checkGLError("onSurfaceCreated");
                  // Draw the cube (NEW)
  }


   private View.OnKeyListener key = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {


                    case KeyEvent.KEYCODE_DPAD_UP:
                        System.out.println("up key pressed");

                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        System.out.println("down key pressed");

                        return true;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        System.out.println("left key pressed");
                        Matrix.setIdentityM(camera, 0);
                        Matrix.translateM(camera, 0, 5, gee_up, vitesse);

                        return true;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        System.out.println("right key pressed");
                        Matrix.setIdentityM(camera, 0);
                        Matrix.translateM(camera, 0, -5, gee_up, vitesse);

                        return true;
                    case KeyEvent.KEYCODE_W:
                        System.out.println("up key pressed");

                        return true;
                    case KeyEvent.KEYCODE_S:
                        System.out.println("down key pressed");

                        return true;
                    case KeyEvent.KEYCODE_A:
                        System.out.println("left key pressed");

                        return true;
                    case KeyEvent.KEYCODE_L:
                        System.out.println("right key pressed");

                        return true;



                }
            }
            return false;
        }
    };









  /**
   * Converts a raw text file into a string.
   *
   * @param resId The resource ID of the raw text file about to be turned into a shader.
   * @return The context of the text file, or null in case of error.
   */
  private String readRawTextFile(int resId) {
    InputStream inputStream = getResources().openRawResource(resId);
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      reader.close();
      return sb.toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }


    float gee_up = -7.0f;
    int gee_cot = 0;

  /**
   * Prepares OpenGL ES before we draw a frame.
c   *
   * @param headTransform The head transformation in the new frame.
   */
  @Override
  public void onNewFrame(HeadTransform headTransform) {
    // Build the Model part of the ModelView matrix.
   // Matrix.rotateM(modelCube, 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);

    // Build the camera matrix and apply it to the ModelView.



     // onKey(key);

   // key.onKey();


      Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
     if(start == 1)
          vitesse += .1;



      Matrix.setIdentityM(modelsky[0], 0);
      Matrix.rotateM(modelsky[0], 0, 90, 1, 0, 0); // je fais 90 degre sur x
      Matrix.translateM(modelsky[0], 0, 0, -(10 + vitesse), -10);


      Matrix.setIdentityM(modelsky[1], 0);
      Matrix.rotateM(modelsky[1], 0, 90, 1, 0, 0);
      Matrix.translateM(modelsky[1], 0, 0, (10 - vitesse), -10);

      Matrix.setIdentityM(modelsky[2], 0);
      Matrix.rotateM(modelsky[2], 0, 90, 0, 0, 1);
      Matrix.translateM(modelsky[2], 0, 8, 10, -(10 + vitesse)); //up or ve

      Matrix.setIdentityM(modelsky[3], 0);
      Matrix.rotateM(modelsky[3], 0, 90, 0, 0, 1);
      Matrix.translateM(modelsky[3], 0, 8, -10, -(10 + vitesse));


      Matrix.setIdentityM(modelsky[4], 0);
      Matrix.rotateM(modelsky[4], 0, 90, 0, 1, 0);
      Matrix.translateM(modelsky[4], 0, vitesse, 10, -10);

      Matrix.setIdentityM(modelFloor, 0);
      Matrix.translateM(modelFloor, 0, 0, -floorDepth, -vitesse); // Floor appears below user.


      sensorManager.registerListener(gyroListener, sensor,
              SensorManager.SENSOR_DELAY_NORMAL);




      if (gero[2] > 3) {
          Log.e("Gyro z",Float.toString(gero[2]));// Ç¡

      }
      if (gero[1] > 3) {
          Log.e("Gyro y", Float.toString(gero[1]));// ^^
          gero_up = true;


      }
      if (gero[0] > 3) {
          Log.e("Gyro x", Float.toString(gero[0])); // <<

      }


      if (gero[2] < -3) {
          Log.e("Gyro- z",Float.toString(gero[2]));// /

      }
      if (gero[1] < -3) {
          Log.e("Gyro- y", Float.toString(gero[1])); // !(^^)

      }
      if (gero[0] < -3) {
          Log.e("Gyro- x", Float.toString(gero[0])); // >>

      }

      if (gero_up) {
          Matrix.setIdentityM(camera, 0);
          Matrix.translateM(camera, 0, 0, gee_up, vitesse);
         gee_up += 0.1;
          if(gee_up > 0)
          {
              gee_up = -7;
             gero_up = false;
          }
      }
      else
      {
          Matrix.setIdentityM(camera, 0);
          Matrix.translateM(camera, 0, 0, 0, vitesse);
      }

      for(int i = 0;i != taillemap;i++)
      {
       /*   if(i == 0)
          {
              Log.e("Gyrosddd- x", Float.toString(possition[i]));
              Log.e("Gyrfdsfo- x", Float.toString(vitesse));

          }*/
          if( -(possition[i]) < vitesse)
          {
              if(i%3 == 0)
              {
                  possition[i] = -(float) (vitesse+20 + (Math.random() * (vitesse+300 - vitesse+20)));
                  Matrix.setIdentityM(modelCube[i], 0);
                  Matrix.translateM(modelCube[i], 0, 0, 0, possition[i] + vitesse);
              }
              else if(i%3 == 1)
              {
                  possition[i] = -(float) (vitesse+20 + (Math.random() * (vitesse+300 - vitesse+20)));
                  Matrix.setIdentityM(modelCube[i], 0);
                  Matrix.translateM(modelCube[i], 0, -5, 0, possition[i] + vitesse);
              }
              else
              {
                  possition[i] = -(float) (vitesse+20 + (Math.random() * (vitesse+300 - vitesse+20)));
                  Matrix.setIdentityM(modelCube[i], 0);
                  Matrix.translateM(modelCube[i], 0, 5, 0, possition[i] + vitesse);
              }
          }

      }

   /*   KeyEvent event;
      if (KeyEvent.KEYCODE_DPAD_LEFT == event.getAction() ) {
          Matrix.setIdentityM(camera, 0);
          Matrix.translateM(camera, 0, 10, gee_up, vitesse);
      }
      else
          {
          //   Matrix.setIdentityM(camera, 0);
         //     Matrix.translateM(camera, 0, 0, 0, vitesse);
          }
/*
      KeyEvent event;
      if (event.getAction() == KeyEvent.ACTION_DOWN) {
          switch (keyCode) {


              case KeyEvent.KEYCODE_DPAD_UP:
                  System.out.println("up key pressed");

              case KeyEvent.KEYCODE_DPAD_DOWN:
                  System.out.println("down key pressed");

              case KeyEvent.KEYCODE_DPAD_LEFT:
                  System.out.println("left key pressed");

              case KeyEvent.KEYCODE_DPAD_RIGHT:
                  System.out.println("right key pressed");

              case KeyEvent.KEYCODE_W:
                  System.out.println("up key pressed");

              case KeyEvent.KEYCODE_S:
                  System.out.println("down key pressed");

              case KeyEvent.KEYCODE_A:
                  System.out.println("left key pressed");

              case KeyEvent.KEYCODE_L:
                  System.out.println("right key pressed");

          }
      }*/

/*

      if(start == 1) {
          for (int i = 0; i != 16; i++) {
              if(i%3 == 0)
              {
                  possition[i] = -(float) (20 + (Math.random() * (50 - 20)));
                  Matrix.setIdentityM(modelCube[i], 0);
                  Matrix.translateM(modelCube[i], 0, 0, 0, possition[i] + vitesse);
              }
              else if(i%3 == 1)
              {
                  possition[i] = -(float) (20 + (Math.random() * (50 - 20)));
                  Matrix.setIdentityM(modelCube[i], 0);
                  Matrix.translateM(modelCube[i], 0, -5, 0, possition[i] + vitesse);
              }
              else
              {
                  possition[i] = -(float) (20 + (Math.random() * (50 - 20)));
                  Matrix.setIdentityM(modelCube[i], 0);
                  Matrix.translateM(modelCube[i], 0, 5, 0, possition[i] + vitesse);
              }


          }
      }

      Matrix.setIdentityM(modelFloor, 0);
      Matrix.translateM(modelFloor, 0, 0, -floorDepth, -vitesse); // Floor appears below user.

/*
      Matrix.setIdentityM(modelsky, 0);
      Matrix.translateM(modelsky, 0, 0, 10, -vitesse); // Floor appears below user.
*/
      headTransform.getHeadView(headView, 0);

      checkGLError("onReadyToDraw");
      finalTime = mysound.getDuration();
      timeElapsed = mysound.getCurrentPosition();

      double timeRemaining = finalTime - timeElapsed;
  //  Log.e("time",String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes((long) timeElapsed), TimeUnit.MILLISECONDS.toSeconds((long) timeElapsed) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) timeElapsed))));


  }





  /**
   * Draws a frame for an eye.
   *
   * @param eye The eye to render. Includes all required transformations.
   */
  @Override
  public void onDrawEye(Eye eye) {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    checkGLError("colorParam");

    // Apply the eye transformation to the camera.
    Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

    // Set the position of the light
    Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

    // Build the ModelView and ModelViewProjection matrices
    // for calculating cube position and light.
    float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);




    for(int i = 0;i!=taillemap;i++) {
      Matrix.multiplyMM(modelView, 0, view, 0, modelCube[i], 0);
      Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);

      drawCube(i);

    }



    // Set modelView for the floor, so we draw floor in the correct location
    Matrix.multiplyMM(modelView, 0, view, 0, modelFloor, 0);
      Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
    drawFloor();


      for(int i = 0;i!=5;i++) {
    Matrix.multiplyMM(modelView, 0, view, 0, modelsky[i], 0);
      Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
    drawsky(i);
      }




  }




  @Override
  public void onFinishFrame(Viewport viewport) {
  }

  /**
   * Draw the cube.
   *
   * <p>We've set all of our transformation matrices. Now we simply pass them into the shader.
   */
  public void drawCube(int i) {
    GLES20.glUseProgram(cubeProgram);
    //  GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
    GLES20.glUniform3fv(cubeLightPosParam, 1, lightPosInEyeSpace, 0);

    // Set the Model in the shader, used to calculate lighting

      GLES20.glUniformMatrix4fv(cubeModelParam, 1, false, modelCube[i], 0);

    // Set the ModelView in the shader, used to calculate lighting
    GLES20.glUniformMatrix4fv(cubeModelViewParam, 1, false, modelView, 0);

    // Set the position of the cube
    GLES20.glVertexAttribPointer(cubePositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
            false, 0, cubeVertices);

    // Set the ModelViewProjection matrix in the shader.
    GLES20.glUniformMatrix4fv(cubeModelViewProjectionParam, 1, false, modelViewProjection, 0);

    // Set the normal positions of the cube, again for shading

      GLES20.glVertexAttribPointer(cubeNormalParam, 3, GLES20.GL_FLOAT, false, 0, cubeNormals);
      GLES20.glVertexAttribPointer(cubeColorParam, 4, GLES20.GL_FLOAT, false, 0,
              isLookingAtObject(i) ? cubeFoundColors : cubeColors);
      GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);


    checkGLError("Drawing cube");
  }


  public void drawFloor() {
    GLES20.glUseProgram(floorProgram);

    // Set ModelView, MVP, position, normals, and color.
    GLES20.glUniform3fv(floorLightPosParam, 1, lightPosInEyeSpace, 0);
    GLES20.glUniformMatrix4fv(floorModelParam, 1, false, modelFloor, 0);
    GLES20.glUniformMatrix4fv(floorModelViewParam, 1, false, modelView, 0);
    GLES20.glUniformMatrix4fv(floorModelViewProjectionParam, 1, false,
            modelViewProjection, 0);
    GLES20.glVertexAttribPointer(floorPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
            false, 0, floorVertices);
    GLES20.glVertexAttribPointer(floorNormalParam, 3, GLES20.GL_FLOAT, false, 0,
            floorNormals);
    GLES20.glVertexAttribPointer(floorColorParam, 4, GLES20.GL_FLOAT, false, 0, floorColors);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

    checkGLError("drawing floor");
  }

  public void drawsky(int i) {
    GLES20.glUseProgram(floorskyProgram);

    // Set ModelView, MVP, position, normals, and color.
    GLES20.glUniform3fv(floorskyLightPosParam, 1, lightPosInEyeSpace, 0);
    GLES20.glUniformMatrix4fv(floorskyModelParam, 1, false, modelsky[i], 0);
    GLES20.glUniformMatrix4fv(floorskyModelViewParam, 1, false, modelView, 0);
    GLES20.glUniformMatrix4fv(floorskyModelViewProjectionParam, 1, false,
            modelViewProjection, 0);
    GLES20.glVertexAttribPointer(floorskyPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
            false, 0, floorskyVertices);
    GLES20.glVertexAttribPointer(floorskyNormalParam, 3, GLES20.GL_FLOAT, false, 0,
            floorskyNormals);
    GLES20.glVertexAttribPointer(floorskyColorParam, 4, GLES20.GL_FLOAT, false, 0, floorskyColors);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

    checkGLError("drawing sky");
  }

  @Override
  public void onCardboardTrigger() {

      for (int i = 0; i != taillemap; i++) {
          Log.e("Vitesse",Float.toString(vitesse));
          Log.e("pos",Float.toString(possition[i]));
          Log.e("i", Integer.toString(i));
          if (possition[i] < -(vitesse + 11)) {



      Log.i(TAG, "onCardboardTrigger");

          if (isLookingAtObject(1)) {
          overlayView.show3DToast("CouCou");
          //    minim = new Minim(this);

              // this loads mysong.wav from the data folder
              //player = minim.loadFile("one.mp3");
            //  player.play();

          mysound.start();
          start = 1;


      }


          if (isLookingAtObject(i)) {

              overlayView.show3DToast("Score = " + score);

              hideObject(i);
          }
      }
      }
      // Always give user feedback.
      vibrator.vibrate(50);


  }

  private void hideObject(int i) {

      score++;

      if(i%3 == 0)
      {
          possition[i] = -(float) (vitesse+20 + (Math.random() * (vitesse+300 - vitesse+20)));
          Matrix.setIdentityM(modelCube[i], 0);
          Matrix.translateM(modelCube[i], 0, 0, 0, possition[i] + vitesse);

      }
      else if(i%3 == 1)
      {
          possition[i] = -(float) (vitesse+20 + (Math.random() * (vitesse+300 - vitesse+20)));
          Matrix.setIdentityM(modelCube[i], 0);
          Matrix.translateM(modelCube[i], 0, -5, 0, possition[i] + vitesse);
      }
      else
      {
          possition[i] = -(float) (vitesse+20 + (Math.random() * (vitesse+300 - vitesse+20)));
          Matrix.setIdentityM(modelCube[i], 0);
          Matrix.translateM(modelCube[i], 0, 5, 0, possition[i] + vitesse);
      }
  }





  private boolean isLookingAtObject(int i) {

          float[] initVec = {0, 0, 0, 1.0f};
          float[] objPositionVec = new float[4];

          // Convert object space to camera space. Use the headView from onNewFrame.

          Matrix.multiplyMM(modelView, 0, headView, 0, modelCube[i], 0);


          Matrix.multiplyMV(objPositionVec, 0, modelView, 0, initVec, 0);
          float pitch = (float) Math.atan2(objPositionVec[1], -objPositionVec[2]);
          float yaw = (float) Math.atan2(objPositionVec[0], -objPositionVec[2]);
      /*
if(i == 0) {
    Log.e("id", Float.toString(Math.abs(pitch)));
    Log.e("is", Float.toString(Math.abs(yaw)));
    Log.e("id", Float.toString(PITCH_LIMIT));
    Log.e("i", Float.toString(YAW_LIMIT));
}*/
          return Math.abs(pitch) < PITCH_LIMIT && Math.abs(yaw) < YAW_LIMIT;

/*

      if (possition[i]> -(vitesse + 15)){
          return true;
      }
      else
          return false;
      */

  }
}
