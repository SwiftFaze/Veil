package com.swiftfaze.veil.sandbox;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldRegeneratorTest {

    @Test
    void noGameDrawsFreshSeedsFromItsSource() {
        WorldRegenerator regenerator = WorldRegenerator.noGame(() -> 777L);

        assertEquals(777L, regenerator.freshSeed());
    }

    @Test
    void noGameReportsThatNoGameIsRunning() {
        WorldRegenerator regenerator = WorldRegenerator.noGame(() -> 1L);

        assertEquals(Optional.of(WorldRegenerator.NO_GAME_MESSAGE), regenerator.regenerate(12345L));
    }
}
