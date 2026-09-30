package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.Camera;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared state across step definition classes within a single scenario.
 * Uses ThreadLocal to store scenario-scoped state without requiring dependency injection.
 */
public class SharedScenarioContext {
    private static final ThreadLocal<Camera> cameraHolder = new ThreadLocal<>();
    private static final ThreadLocal<DevConsoleModel> devConsoleModelHolder = new ThreadLocal<>();
    private static final ThreadLocal<DevConsolePanel> devConsolePanelHolder = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, String>> questLogSnapshotHolder = new ThreadLocal<>();

    public static Camera getCamera() {
        return cameraHolder.get();
    }

    public static void setCamera(Camera camera) {
        cameraHolder.set(camera);
    }

    public static DevConsoleModel getDevConsoleModel() {
        return devConsoleModelHolder.get();
    }

    public static void setDevConsoleModel(DevConsoleModel model) {
        devConsoleModelHolder.set(model);
    }

    public static DevConsolePanel getDevConsolePanel() {
        return devConsolePanelHolder.get();
    }

    public static void setDevConsolePanel(DevConsolePanel panel) {
        devConsolePanelHolder.set(panel);
    }

    public static Map<String, String> getQuestLogSnapshot() {
        return questLogSnapshotHolder.get();
    }

    public static void setQuestLogSnapshot(Map<String, String> snapshot) {
        questLogSnapshotHolder.set(snapshot);
    }

    public static void cleanup() {
        cameraHolder.remove();
        devConsoleModelHolder.remove();
        devConsolePanelHolder.remove();
        questLogSnapshotHolder.remove();
    }
}
