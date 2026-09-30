package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;

import java.nio.file.Paths;
import java.util.List;

/**
 * A dev-only scene containing every tile from ModRegistry.getAllTiles(),
 * arranged in a grid for preview/testing purposes. Used only by the
 * KitchenSinkProvider sandbox, not in the live game.
 */
public class KitchenSinkScene extends WorldScene {

    private static final int TILES_PER_ROW = 10;

    public KitchenSinkScene() {
        super(calculateWidth(), calculateHeight());
        fillWithTiles();
    }

    private void fillWithTiles() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        List<Tile> allTiles = mods.getAllTiles();

        int x = 0;
        int y = 0;
        for (Tile tile : allTiles) {
            if (x >= TILES_PER_ROW) {
                x = 0;
                y++;
            }
            if (y < getHeight() && x < getWidth()) {
                setTile(x, y, tile);
            }
            x++;
        }
    }

    private void setTile(int x, int y, Tile tile) {
        // Access the protected tiles array through a helper
        // Since WorldScene doesn't provide a setter, we'll use fillRegion
        // with a 1x1 rectangle
        java.awt.Rectangle region = new java.awt.Rectangle(x, y, 1, 1);
        fillRegion(region, tile);
    }

    private static int calculateWidth() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        List<Tile> allTiles = mods.getAllTiles();
        return Math.min(TILES_PER_ROW, Math.max(1, allTiles.size()));
    }

    private static int calculateHeight() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        List<Tile> allTiles = mods.getAllTiles();
        if (allTiles.isEmpty()) {
            return 1;
        }
        return (allTiles.size() + TILES_PER_ROW - 1) / TILES_PER_ROW;
    }
}
