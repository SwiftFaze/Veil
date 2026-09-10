package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.Camera;

/**
 * Shared state across step definition classes within a single scenario.
 * Uses ThreadLocal to store scenario-scoped state without requiring dependency injection.
 */
public class SharedScenarioContext {
    private static final ThreadLocal<Camera> cameraHolder = new ThreadLocal<>();

    public static Camera getCamera() {
        return cameraHolder.get();
    }

    public static void setCamera(Camera camera) {
        cameraHolder.set(camera);
    }

    public static void cleanup() {
        cameraHolder.remove();
    }
}
