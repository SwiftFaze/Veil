package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.sandbox.KitchenSinkPreviewPanel;

/**
 * Shared state across step definition classes within a single scenario.
 * Uses ThreadLocal to store scenario-scoped state without requiring dependency injection.
 */
public class SharedScenarioContext {
    private static final ThreadLocal<Camera> cameraHolder = new ThreadLocal<>();
    private static final ThreadLocal<DevConsoleSteps> DEV_CONSOLE_STEPS_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<UiComponentFrameworkSteps> UI_STEPS_HOLDER =
            ThreadLocal.withInitial(UiComponentFrameworkSteps::new); // lazily built: Cucumber only instantiates step classes whose steps run
    private static final ThreadLocal<KitchenSinkPreviewPanel> PREVIEW_PANEL_HOLDER = new ThreadLocal<>();

    public static Camera getCamera() {
        return cameraHolder.get();
    }

    public static void setCamera(Camera camera) {
        cameraHolder.set(camera);
    }

    public static DevConsoleSteps getDevConsoleSteps() {
        return DEV_CONSOLE_STEPS_HOLDER.get();
    }

    public static void setDevConsoleSteps(DevConsoleSteps steps) {
        DEV_CONSOLE_STEPS_HOLDER.set(steps);
    }

    public static UiComponentFrameworkSteps getUiSteps() {
        return UI_STEPS_HOLDER.get();
    }

    public static void setUiSteps(UiComponentFrameworkSteps steps) {
        UI_STEPS_HOLDER.set(steps);
    }

    public static KitchenSinkPreviewPanel getKitchenSinkPreviewPanel() {
        return PREVIEW_PANEL_HOLDER.get();
    }

    public static void setKitchenSinkPreviewPanel(KitchenSinkPreviewPanel panel) {
        PREVIEW_PANEL_HOLDER.set(panel);
    }

    public static void cleanup() {
        cameraHolder.remove();
        DEV_CONSOLE_STEPS_HOLDER.remove();
        UI_STEPS_HOLDER.remove();
        PREVIEW_PANEL_HOLDER.remove();
    }
}
