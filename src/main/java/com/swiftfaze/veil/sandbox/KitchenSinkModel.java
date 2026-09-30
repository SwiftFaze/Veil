package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.world.WorldScene;

/**
 * Model for the kitchen-sink preview: marker position, overlay state, and
 * the underlying scene. Kept separate so tests can assert on the model
 * without depending on pixel rendering.
 */
public class KitchenSinkModel {
    private final WorldScene scene;
    private int markerX;
    private int markerY;
    private boolean overlayOn;

    public KitchenSinkModel(WorldScene scene) {
        this.scene = scene;
        initializeMarkerPosition();
        this.overlayOn = false;
    }

    private void initializeMarkerPosition() {
        // Find the first walkable tile to start on
        for (int y = 0; y < scene.getHeight(); y++) {
            for (int x = 0; x < scene.getWidth(); x++) {
                if (scene.isWalkable(x, y)) {
                    this.markerX = x;
                    this.markerY = y;
                    return;
                }
            }
        }
        // Fallback if no walkable tile found (shouldn't happen)
        this.markerX = 0;
        this.markerY = 0;
    }

    public void moveMarker(int dx, int dy) {
        int newX = markerX + dx;
        int newY = markerY + dy;
        if (scene.isWalkable(newX, newY)) {
            markerX = newX;
            markerY = newY;
        }
    }

    /**
     * Whether the scene holds at least one tile. An empty registry still yields a 1x1 scene
     * (WorldScene can't be 0x0), so size alone can't tell.
     */
    public boolean hasTiles() {
        for (int y = 0; y < scene.getHeight(); y++) {
            for (int x = 0; x < scene.getWidth(); x++) {
                if (scene.getTile(x, y) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public void toggleOverlay() {
        overlayOn = !overlayOn;
    }

    public WorldScene getScene() {
        return scene;
    }

    public int getMarkerX() {
        return markerX;
    }

    public int getMarkerY() {
        return markerY;
    }

    public boolean isOverlayOn() {
        return overlayOn;
    }
}
