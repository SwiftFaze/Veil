package com.swiftfaze.veil.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldPauseTest {

    @Test
    void startsUnpaused() {
        assertFalse(new WorldPause().isPaused());
    }

    @Test
    void holdingAScreenPausesTheWorld() {
        WorldPause pause = new WorldPause();

        pause.hold("inventory");

        assertTrue(pause.isPaused());
    }

    @Test
    void releasingTheOnlyHeldScreenResumesTheWorld() {
        WorldPause pause = new WorldPause();
        pause.hold("inventory");

        pause.release("inventory");

        assertFalse(pause.isPaused());
    }

    @Test
    void worldStaysPausedWhileAnotherScreenIsStillHeld() {
        WorldPause pause = new WorldPause();
        pause.hold("inventory");
        pause.hold("codex");

        pause.release("inventory");
        assertTrue(pause.isPaused());

        pause.release("codex");
        assertFalse(pause.isPaused());
    }

    @Test
    void releasingAScreenThatWasNeverHeldIsHarmless() {
        WorldPause pause = new WorldPause();
        pause.hold("pause");

        pause.release("codex");

        assertTrue(pause.isPaused());
    }

    @Test
    void clearResumesTheWorldWhateverIsHeld() {
        WorldPause pause = new WorldPause();
        pause.hold("inventory");
        pause.hold("pause");

        pause.clear();

        assertFalse(pause.isPaused());
    }
}
