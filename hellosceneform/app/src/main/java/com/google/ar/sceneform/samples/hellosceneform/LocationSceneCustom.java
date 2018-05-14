package com.google.ar.sceneform.samples.hellosceneform;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.ArrayList;
import java.util.Iterator;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.sensor.DeviceLocation;
import uk.co.appoly.arcorelocation.sensor.DeviceOrientation;
import uk.co.appoly.arcorelocation.utils.LocationUtils;

public class LocationSceneCustom extends LocationScene {
    private static final int ANCHOR_REFRESH_INTERVAL = 8000;
    public static Context mContext;
    public static Activity mActivity;
    private final float[] mAnchorMatrix = new float[16];
    public ArrayList<LocationMarker> mLocationMarkers = new ArrayList();
    public DeviceLocation deviceLocation;
    public DeviceOrientation deviceOrientation;
    private int distanceLimit = 50;
    private int bearingAdjustment = 0;
    private String TAG = "LocationScene";
    private boolean anchorsNeedRefresh = true;
    private Handler mHandler = new Handler();
    Runnable anchorRefreshTask = new Runnable() {
        public void run() {
            LocationSceneCustom.this.anchorsNeedRefresh = true;
            LocationSceneCustom.this.mHandler.postDelayed(LocationSceneCustom.this.anchorRefreshTask, 8000L);
        }
    };
    private Session mSession;
    private ArFragment arFragment;

    public LocationSceneCustom(Context mContext, Activity mActivity, ArFragment arFragment) {
        super(mContext, mActivity, arFragment.getArSceneView().getSession());

        this.arFragment = arFragment;
        this.startCalculationTask();
        this.deviceLocation = new DeviceLocation();
        this.deviceOrientation = new DeviceOrientation();
    }

    public void draw(Frame frame) {
        this.refreshAnchorsIfRequired(frame);
        this.drawMarkers(frame);
        Iterator var2 = this.mLocationMarkers.iterator();

        while(var2.hasNext()) {
            LocationMarker lm = (LocationMarker)var2.next();
            if (lm.anchor == null) {
                this.anchorsNeedRefresh = true;
            }
        }

        if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
            this.anchorsNeedRefresh = true;
        }

    }

    public void drawMarkers(Frame frame) {
        Iterator var2 = this.mLocationMarkers.iterator();

        while(var2.hasNext()) {
            LocationMarker locationMarker = (LocationMarker)var2.next();

            try {
                float[] translation = new float[3];
                float[] rotation = new float[4];
                locationMarker.anchor.getPose().getTranslation(translation, 0);
                frame.getCamera().getPose().getRotationQuaternion(rotation, 0);
                Pose rotatedPose = new Pose(translation, rotation);
                rotatedPose.toMatrix(this.mAnchorMatrix, 0);
                int markerDistance = (int)Math.ceil(LocationUtils.distance(locationMarker.latitude, this.deviceLocation.currentBestLocation.getLatitude(), locationMarker.longitude, this.deviceLocation.currentBestLocation.getLongitude(), 0.0D, 0.0D));
                int renderDistance = markerDistance;
                if (markerDistance > this.distanceLimit) {
                    renderDistance = this.distanceLimit;
                }

                float[] projectionMatrix = new float[16];
                frame.getCamera().getProjectionMatrix(projectionMatrix, 0, 0.1F, 100.0F);
                float[] viewMatrix = new float[16];
                frame.getCamera().getViewMatrix(viewMatrix, 0);
                float scale = 0.3F * (float)renderDistance;
                if (markerDistance > 3000) {
                    scale *= 0.75F;
                }

                float lightIntensity = frame.getLightEstimate().getPixelIntensity();
                locationMarker.renderer.updateModelMatrix(this.mAnchorMatrix, scale);
                locationMarker.renderer.draw(viewMatrix, projectionMatrix, lightIntensity);
            } catch (Exception var13) {
                var13.printStackTrace();
            }
        }

    }

    public void refreshAnchorsIfRequired(Frame frame) {
        Log.d(TAG, "ARSession [Anchors Refresh]");
        if (this.anchorsNeedRefresh) {
            this.anchorsNeedRefresh = false;
            Log.d(TAG, "ARSession [Anchors Refresh True]");
            Log.d(TAG, "ARSession [mLocationMarkers]: " + this.mLocationMarkers.size());
            Log.d(TAG, "ARSession [SESSION]: " + arFragment.getArSceneView().getSession());

            for(int i = 0; i < this.mLocationMarkers.size(); ++i) {
                try {
                    int markerDistance = (int)Math.round(LocationUtils.distance(((LocationMarker)this.mLocationMarkers.get(i)).latitude, this.deviceLocation.currentBestLocation.getLatitude(), ((LocationMarker)this.mLocationMarkers.get(i)).longitude, this.deviceLocation.currentBestLocation.getLongitude(), 0.0D, 0.0D));
                    float markerBearing = this.deviceOrientation.currentDegree + (float)LocationUtils.bearing(this.deviceLocation.currentBestLocation.getLatitude(), this.deviceLocation.currentBestLocation.getLongitude(), ((LocationMarker)this.mLocationMarkers.get(i)).latitude, ((LocationMarker)this.mLocationMarkers.get(i)).longitude);
                    markerBearing += (float)this.bearingAdjustment;
                    markerBearing %= 360.0F;
                    double rotation = Math.floor((double)markerBearing);
                    if (this.deviceOrientation.pitch > -25.0F) {
                        rotation = rotation * 3.141592653589793D / 180.0D;
                    }

                    int renderDistance = markerDistance;
                    if (markerDistance > this.distanceLimit) {
                        renderDistance = this.distanceLimit;
                    }

                    double heightAdjustment = (double)Math.round((double)renderDistance * Math.tan(Math.toRadians((double)this.deviceOrientation.pitch)));
                    int cappedRealDistance = markerDistance > 500 ? 500 : markerDistance;
                    if (renderDistance != markerDistance) {
                        heightAdjustment += (double)(0.01F * (float)(cappedRealDistance - renderDistance));
                    }

                    float x = 0.0F;
                    float z = (float)(-renderDistance);
                    float zRotated = (float)((double)z * Math.cos(rotation) - (double)x * Math.sin(rotation));
                    float xRotated = (float)(-((double)z * Math.sin(rotation) + (double)x * Math.cos(rotation)));
                    float y = frame.getCamera().getDisplayOrientedPose().ty();
                    Anchor newAnchor = arFragment.getArSceneView().getSession().createAnchor(frame.getCamera().getPose().compose(Pose.makeTranslation(xRotated, y + (float)heightAdjustment, zRotated)));
                    AnchorNode anchorNode = new AnchorNode(newAnchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                    Log.d(TAG, "ARSession Anchors [LOCATIONSCENE]: " + arFragment.getArSceneView().getSession().getAllAnchors().size());

                    ((LocationMarker)this.mLocationMarkers.get(i)).anchor = newAnchor;
                    ((LocationMarker)this.mLocationMarkers.get(i)).renderer.createOnGlThread(mContext, markerDistance);
                } catch (Exception var17) {
                    var17.printStackTrace();
                }
            }
        }

    }

    public int getBearingAdjustment() {
        return this.bearingAdjustment;
    }

    public void setBearingAdjustment(int i) {
        this.bearingAdjustment = i;
        this.anchorsNeedRefresh = true;
    }

    public void resume() {
        this.deviceOrientation.resume();
    }

    public void pause() {
        this.deviceOrientation.pause();
    }

    void startCalculationTask() {
        this.anchorRefreshTask.run();
    }

    void stopCalculationTask() {
        this.mHandler.removeCallbacks(this.anchorRefreshTask);
    }
}
