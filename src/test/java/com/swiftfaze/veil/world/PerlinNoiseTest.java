package com.swiftfaze.veil.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The expected values are the port's own output, pinned so any change to the noise math, the
 * seeded shuffle or the table indexing shows up: the terrain a seed gives is part of the product.
 */
class PerlinNoiseTest {

    private static final double TOLERANCE = 1e-12;
    private static final long ZERO_SEED = 0L;
    private static final long OTHER_SEED = 12_345L;

    @Test
    void seedZeroTerrainIsPinned() {
        PerlinNoise noise = new PerlinNoise(ZERO_SEED);

        assertEquals(0.06865822532492256, noise.noise(0.5, 0.5), TOLERANCE);
        assertEquals(-0.24277661816647017, noise.noise(37.25, 12.75), TOLERANCE);
        assertEquals(0.3559690290841851, noise.noise(5.0, 9.0), TOLERANCE);
        assertEquals(-0.09774075196938774, noise.noise(100.0, 100.0), TOLERANCE);
    }

    @Test
    void seedZeroTerrainIsPinnedBeyondTheTableAndForNegativeCoordinates() {
        PerlinNoise noise = new PerlinNoise(ZERO_SEED);

        assertEquals(-0.16138884096943962, noise.noise(-8.5, 260.3), TOLERANCE);
        assertEquals(0.28553060456699725, noise.noise(300.1, -2.0), TOLERANCE);
    }

    @Test
    void anotherSeedShufflesTheTableAndGivesItsOwnTerrain() {
        PerlinNoise noise = new PerlinNoise(OTHER_SEED);

        assertEquals(-0.09149471416934947, noise.noise(0.5, 0.5), TOLERANCE);
        assertEquals(-0.19430359858246113, noise.noise(37.25, 12.75), TOLERANCE);
        assertEquals(-0.7617595164835453, noise.noise(300.1, -2.0), TOLERANCE);
        assertEquals(-0.28109278377620195, noise.noise(100.0, 100.0), TOLERANCE);
        assertEquals(-0.24494237115534126, noise.noise(5.0, 9.0), TOLERANCE);
        assertNotEquals(new PerlinNoise(ZERO_SEED).noise(5.0, 9.0), noise.noise(5.0, 9.0));
    }

    @Test
    void theSameSeedGivesTheSameField() {
        assertEquals(new PerlinNoise(OTHER_SEED).noise(42.5, 17.5), new PerlinNoise(OTHER_SEED).noise(42.5, 17.5));
    }
}
