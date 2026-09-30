package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.Camera;
import com.swiftfaze.veil.entities.quests.Quest;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import java.util.List;

/**
 * Shared state across step definition classes within a single scenario.
 * Uses ThreadLocal to store scenario-scoped state without requiring dependency injection.
 */
public class SharedScenarioContext {
    private static final ThreadLocal<Camera> cameraHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> currentQuestIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<DevConsoleModel> devConsoleModelHolder = new ThreadLocal<>();
    private static final ThreadLocal<List<Quest>> loadedQuestsHolder = new ThreadLocal<>();

    public static Camera getCamera() {
        return cameraHolder.get();
    }

    public static void setCamera(Camera camera) {
        cameraHolder.set(camera);
    }

    public static String getCurrentQuestId() {
        return currentQuestIdHolder.get();
    }

    public static void setCurrentQuestId(String questId) {
        currentQuestIdHolder.set(questId);
    }

    public static DevConsoleModel getDevConsoleModel() {
        return devConsoleModelHolder.get();
    }

    public static void setDevConsoleModel(DevConsoleModel model) {
        devConsoleModelHolder.set(model);
    }

    public static List<Quest> getLoadedQuests() {
        return loadedQuestsHolder.get();
    }

    public static void setLoadedQuests(List<Quest> quests) {
        loadedQuestsHolder.set(quests);
    }

    public static void cleanup() {
        cameraHolder.remove();
        currentQuestIdHolder.remove();
        devConsoleModelHolder.remove();
        loadedQuestsHolder.remove();
    }
}
