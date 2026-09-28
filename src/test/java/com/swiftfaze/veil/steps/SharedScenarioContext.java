package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.Camera;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.world.WorldScene;

/**
 * Shared state across step definition classes within a single scenario.
 * Uses ThreadLocal to store scenario-scoped state without requiring dependency injection.
 */
public class SharedScenarioContext {
    private static final ThreadLocal<Camera> cameraHolder = new ThreadLocal<>();
    private static final ThreadLocal<GamePanel> gamePanelHolder = new ThreadLocal<>();
    private static final ThreadLocal<WorldScene> worldSceneHolder = new ThreadLocal<>();
    private static final ThreadLocal<GameEventLog> eventLogHolder = new ThreadLocal<>();

    public static Camera getCamera() {
        return cameraHolder.get();
    }

    public static void setCamera(Camera camera) {
        cameraHolder.set(camera);
    }

    public static GamePanel getGamePanel() {
        return gamePanelHolder.get();
    }

    public static void setGamePanel(GamePanel gamePanel) {
        gamePanelHolder.set(gamePanel);
    }

    public static WorldScene getWorldScene() {
        return worldSceneHolder.get();
    }

    public static void setWorldScene(WorldScene worldScene) {
        worldSceneHolder.set(worldScene);
    }

    public static GameEventLog getGameEventLog() {
        return eventLogHolder.get();
    }

    public static void setGameEventLog(GameEventLog eventLog) {
        eventLogHolder.set(eventLog);
    }

    public static void cleanup() {
        cameraHolder.remove();
        gamePanelHolder.remove();
        worldSceneHolder.remove();
        eventLogHolder.remove();
    }
}
