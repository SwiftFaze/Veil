package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.Camera;
import com.swiftfaze.veil.sandbox.KitchenSinkPreviewPanel;

/**
 * Shared state across step definition classes within a single scenario.
 * Uses ThreadLocal to store scenario-scoped state without requiring dependency injection.
 */
public class SharedScenarioContext {
    private static final ThreadLocal<Camera> cameraHolder = new ThreadLocal<>();
    private static final ThreadLocal<DevConsoleSteps> devConsoleStepsHolder = new ThreadLocal<>();
    private static final ThreadLocal<UiComponentFrameworkSteps> uiStepsHolder = new ThreadLocal<>();
    private static final ThreadLocal<KitchenSinkPreviewPanel> kitchenSinkPreviewPanelHolder = new ThreadLocal<>();

    public static Camera getCamera() {
        return cameraHolder.get();
    }

    public static void setCamera(Camera camera) {
        cameraHolder.set(camera);
    }

    public static DevConsoleSteps getDevConsoleSteps() {
        return devConsoleStepsHolder.get();
    }

    public static void setDevConsoleSteps(DevConsoleSteps steps) {
        devConsoleStepsHolder.set(steps);
    }

    public static UiComponentFrameworkSteps getUiSteps() {
        return uiStepsHolder.get();
    }

    public static void setUiSteps(UiComponentFrameworkSteps steps) {
        uiStepsHolder.set(steps);
    }

    public static KitchenSinkPreviewPanel getKitchenSinkPreviewPanel() {
        return kitchenSinkPreviewPanelHolder.get();
    }

    public static void setKitchenSinkPreviewPanel(KitchenSinkPreviewPanel panel) {
        kitchenSinkPreviewPanelHolder.set(panel);
    }

    public static void cleanup() {
        cameraHolder.remove();
        devConsoleStepsHolder.remove();
        uiStepsHolder.remove();
        kitchenSinkPreviewPanelHolder.remove();
    }
}
