/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.hellosceneform;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Plane.Type;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.AnnotationRenderer;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity {
  private static final String TAG = HelloSceneformActivity.class.getSimpleName();

  private ArFragment arFragment;
  private ModelRenderable starbucksRenderable;
  private LocationSceneCustom locationScene;
  private Session arSession;

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_ux);

    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);



    // When you build a Renderable, Sceneform loads its resources in the background while returning
    // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
    // Starbucks Renderable
      ModelRenderable.builder()
              .setSource(this, R.raw.starbucks)
              .build()
              .thenAccept(renderable -> starbucksRenderable = renderable)
              .exceptionally(
                      throwable -> {
                          Toast toast =
                                  Toast.makeText(this, "Unable to load starbucks renderable", Toast.LENGTH_LONG);
                          toast.setGravity(Gravity.CENTER, 0, 0);
                          toast.show();
                          return null;
                      });

    arFragment.setOnTapArPlaneListener(
        (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
          if (starbucksRenderable == null) {
            return;
          }

          if (plane.getType() != Type.HORIZONTAL_UPWARD_FACING) {
            return;
          }

          Log.d(TAG, "ARSession: " + arFragment.getArSceneView().getSession());



            // Create the Anchor.
          Anchor anchor = hitResult.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());
          Log.d(TAG, "ARSession Anchors: " + arSession.getAllAnchors().size());


            // Create the transformable andy and add it to the anchor.
          TransformableNode starbucks = new TransformableNode(arFragment.getTransformationSystem());
          starbucks.setParent(anchorNode);
          starbucks.setRenderable(starbucksRenderable);
          starbucks.select();
        });

      // onResume to initialze the Session
      arFragment.onResume();
      Frame frame = null;
      try {
          frame = arFragment.getArSceneView().getSession().update();
      } catch (CameraNotAvailableException e) {
          e.printStackTrace();
      }
      Log.d(TAG, "ARSession [FRAME]: " + frame);

      // Using Custom LocationScene
      locationScene = new LocationSceneCustom(this, this, arFragment);

      // Annotation at Buckingham Palace
      locationScene.mLocationMarkers.add(
              new LocationMarker(
                      33.780201,
                      -84.388684,
                      new AnnotationRenderer("Buckingham Palace")));

      // Calling draw which calls refreshAnchorsIfRequired on the frame
      locationScene.draw(frame);

      Log.d(TAG, "ARSession INITIALIZED: " + arFragment.getArSceneView().getSession());
      Log.d(TAG, "ARSession Anchors: " + arFragment.getArSceneView().getSession().getAllAnchors().size());

  }

  @Override
    protected void onResume() {
      super.onResume();
      if (ARLocationPermissionHelper.hasPermission(this)) {

          if (locationScene != null) {
              locationScene.resume();
          }
      } else {
          ARLocationPermissionHelper.requestPermission(this);
      }
  }

  @Override
    public void onPause() {
      super.onPause();
      if (locationScene != null) {
          locationScene.pause();
      }
  }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                ARLocationPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

}
