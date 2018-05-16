package com.google.ar.sceneform.samples.hellosceneform;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ux.TransformableNode;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.rendering.Renderer;

public class LocationMarkerCustom extends LocationMarker {

    public double longitude;
    public double latitude;
    public Anchor anchor;
    public AnchorNode anchorNode;
    public TransformableNode transformableNode;

    public Renderer renderer;
    private Runnable touchEvent;
    private int touchableSize;

    public LocationMarkerCustom(double longitude, double latitude, Renderer renderer) {
        super(longitude, latitude, renderer);
        this.longitude = longitude;
        this.latitude = latitude;
        this.renderer = renderer;
    }

    public void setOnTouchListener(Runnable touchEvent) {
        this.touchEvent = touchEvent;
    }

    public Runnable getTouchEvent() {
        return this.touchEvent;
    }

    public int getTouchableSize() {
        return this.touchableSize;
    }

    public void setTouchableSize(int touchableSize) {
        this.touchableSize = touchableSize;
    }
}
