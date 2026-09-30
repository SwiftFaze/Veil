package com.swiftfaze.veil.world;

import java.util.List;

/**
 * A dev-only scene containing every given tile,
 * arranged in a grid for preview/testing purposes. Used only by the
 * KitchenSinkProvider sandbox, not in the live game.
 */
public class KitchenSinkScene extends WorldScene {

    private static final int TILES_PER_ROW = 10;

    /**
     * Static factory (not a constructor call) so sandbox code can build the scene without
     * instantiating an engine class, which the composition-root ArchUnit rule forbids.
     */
    public static KitchenSinkScene of(List<Tile> tiles) {
        return new KitchenSinkScene(tiles);
    }

    private KitchenSinkScene(List<Tile> tiles) {
        super(calculateWidth(tiles), calculateHeight(tiles));
        fillWithTiles(tiles);
    }

    private void fillWithTiles(List<Tile> allTiles) {

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

    private static int calculateWidth(List<Tile> allTiles) {
        return Math.min(TILES_PER_ROW, Math.max(1, allTiles.size()));
    }

    private static int calculateHeight(List<Tile> allTiles) {
        if (allTiles.isEmpty()) {
            return 1;
        }
        return (allTiles.size() + TILES_PER_ROW - 1) / TILES_PER_ROW;
    }
}
