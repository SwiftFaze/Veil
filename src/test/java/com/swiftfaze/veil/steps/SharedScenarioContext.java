package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.Camera;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import java.util.Map;

/**
 * Shared state across step definition classes within a single scenario.
 * Uses ThreadLocal to store scenario-scoped state without requiring dependency injection.
 */
public class SharedScenarioContext {
    private static final ThreadLocal<Camera> CAMERA = new ThreadLocal<>();
    private static final ThreadLocal<DevConsoleModel> DEV_CONSOLE_MODEL = new ThreadLocal<>();
    private static final ThreadLocal<DevConsolePanel> DEV_CONSOLE_PANEL = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, String>> QUEST_LOG_SNAPSHOT = new ThreadLocal<>();

    public static Camera getCamera() {
        return CAMERA.get();
    }

    public static void setCamera(Camera camera) {
        CAMERA.set(camera);
    }

    public static DevConsoleModel getDevConsoleModel() {
        return DEV_CONSOLE_MODEL.get();
    }

    public static void setDevConsoleModel(DevConsoleModel model) {
        DEV_CONSOLE_MODEL.set(model);
    }

    public static DevConsolePanel getDevConsolePanel() {
        return DEV_CONSOLE_PANEL.get();
    }

    public static void setDevConsolePanel(DevConsolePanel panel) {
        DEV_CONSOLE_PANEL.set(panel);
    }

    public static Map<String, String> getQuestLogSnapshot() {
        return QUEST_LOG_SNAPSHOT.get();
    }

    public static void setQuestLogSnapshot(Map<String, String> snapshot) {
        QUEST_LOG_SNAPSHOT.set(snapshot);
    }

    public static void cleanup() {
        CAMERA.remove();
        DEV_CONSOLE_MODEL.remove();
        DEV_CONSOLE_PANEL.remove();
        QUEST_LOG_SNAPSHOT.remove();
    }
}
