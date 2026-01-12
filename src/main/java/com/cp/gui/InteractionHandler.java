package com.cp.gui;

public class InteractionHandler {
    public float offsetX = 0;
    public float offsetY = 0;
    public float zoom = 1.0f;

    public void onDrag(double deltaX, double deltaY) {
        offsetX += (float) deltaX;
        offsetY += (float) deltaY;
    }

    public void onMouseScroll(double amount) {
        float zoomSpeed = 0.1f;
        if (amount > 0) zoom += zoomSpeed * zoom;
        else zoom -= zoomSpeed * zoom;
        zoom = Math.max(0.1f, Math.min(zoom, 5.0f));
    }
}
