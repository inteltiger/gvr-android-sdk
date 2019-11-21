/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
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

package com.google.vr.sdk.samples.video360;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import android.view.ViewGroup;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import com.google.vr.sdk.samples.video360.rendering.SceneRenderer;
import javax.microedition.khronos.egl.EGLConfig;

/**
 * GVR Activity demonstrating a 360 video player.
 *
 * The default intent for this Activity will load a 360 placeholder panorama. For more options on
 * how to load other media using a custom Intent, see {@link MediaLoader}.
 */
public class VrVideoActivity extends GvrActivity {
  private static final String TAG = "VrVideoActivity";

  private GvrView gvrView;
  private Renderer renderer;

  // Given an intent with a media file and format, this will load the file and generate the mesh.
  private MediaLoader mediaLoader;

  /**
   * Configures the VR system.
   *
   * @param savedInstanceState unused in this sample but it could be used to track video position
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mediaLoader = new MediaLoader(this);

    gvrView = new GvrView(this);
    // Since the videos have fewer pixels per degree than the phones, reducing the render target
    // scaling factor reduces the work required to render the scene. This factor can be adjusted at
    // runtime depending on the resolution of the loaded video.
    // You can use Eye.getViewport() in the overridden onDrawEye() method to determine the current
    // render target size in pixels.
    gvrView.setRenderTargetScale(.5f);

    // Standard GvrView configuration
    renderer = new Renderer(gvrView);
    gvrView.setEGLConfigChooser(
        8, 8, 8, 8,  // RGBA bits.
        16,  // Depth bits.
        0);  // Stencil bits.
    gvrView.setRenderer(renderer);
    setContentView(gvrView);

    checkPermissionAndInitialize();
  }

  /**
   * Normal apps don't need this. However, since we use adb to interact with this sample, we
   * want any new adb Intents to be routed to the existing Activity rather than launching a new
   * Activity.
   */
  @Override
  protected void onNewIntent(Intent intent) {
    // Save the new Intent which may contain a new Uri. Then tear down & recreate this Activity to
    // load that Uri.
    setIntent(intent);
    recreate();
  }

  /** Initializes the Activity only if the permission has been granted. */
  private void checkPermissionAndInitialize() {
    if (ContextCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED) {
      mediaLoader.handleIntent(getIntent());
    } else {
      Log.e(TAG, "No read sdcard permission");
    }
  }


  @Override
  protected void onResume() {
    super.onResume();

    mediaLoader.resume();
  }

  @Override
  protected void onPause() {
    mediaLoader.pause();

    super.onPause();
  }

  @Override
  protected void onDestroy() {
    mediaLoader.destroy();
    super.onDestroy();
  }

  /**
   * Standard GVR renderer. Most of the real work is done by {@link SceneRenderer}.
   */
  private class Renderer implements GvrView.StereoRenderer {
    private static final float Z_NEAR = .1f;
    private static final float Z_FAR = 100;

    // Used by ControllerEventListener to manipulate the scene.
    public final SceneRenderer scene;

    private final float[] viewProjectionMatrix = new float[16];

    /**
     * Creates the Renderer and configures the VR exit button.
     *
     * @param parent Any View that is already attached to the Window. The uiView will secretly be
     *     attached to this View in order to properly handle UI events.
     */
    @MainThread
    public Renderer(ViewGroup parent) {
      scene = SceneRenderer.createForVR();
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {}

    @Override
    public void onDrawEye(Eye eye) {
      Matrix.multiplyMM(
          viewProjectionMatrix, 0, eye.getPerspective(Z_NEAR, Z_FAR), 0, eye.getEyeView(), 0);
      scene.glDrawFrame(viewProjectionMatrix, eye.getType());
    }

    @Override
    public void onFinishFrame(Viewport viewport) {}

    @Override
    public void onSurfaceCreated(EGLConfig config) {
      scene.glInit();
      mediaLoader.onGlSceneReady(scene);
    }

    @Override
    public void onSurfaceChanged(int width, int height) { }

    @Override
    public void onRendererShutdown() {
      scene.glShutdown();
    }
  }

}
