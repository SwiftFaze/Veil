package com.swiftfaze.veil.world;

import org.jspecify.annotations.Nullable;
import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.render.DrawableAsciiEntity;
import com.swiftfaze.veil.entities.buildings.Building;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

public abstract class WorldScene implements DrawableAsciiEntity {
    private final Tile[][] tiles;
    private final int width;
    private final int height;
    private static final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 15);

    protected WorldScene(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
    }

    public void fillAll(Tile type) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = type;
            }
        }
    }

    public void fillRegion(Rectangle region, Tile type) {
        for (int x = region.x; x < region.x + region.width; x++) {
            for (int y = region.y; y < region.y + region.height; y++) {
                if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
                    tiles[x][y] = type;
                }
            }
        }
    }

    public void placeBuilding(Building building) {
        List<List<Tile>> blueprint = building.getBlueprint();

        for (int y = 0; y < blueprint.size(); y++) {
            List<Tile> row = blueprint.get(y);
            for (int x = 0; x < row.size(); x++) {
                tiles[building.getWorldX() + x][building.getWorldY() + y] = row.get(x);
            }
        }
    }

    public void createBorder(int width, int height, Tile type) {
        fillRegion(new Rectangle(0, 0, width, 1), type);
        fillRegion(new Rectangle(0, 0, 1, height), type);
        fillRegion(new Rectangle(0, height - 1, width, 1), type);
        fillRegion(new Rectangle(width - 1, 0, 1, height), type);
    }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }
        Tile type = tiles[x][y];
        return type != null && type.isWalkable();
    }

    public @Nullable Tile getTile(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return tiles[x][y];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public char getSymbol() {
        return ' ';
    }

    @Override
    public Color getColor() {
        return Color.WHITE;
    }

    @Override
    public void render(Graphics2D g2d, int tileWidth, int tileHeight, Camera camera) {
        renderWorld(g2d, tileWidth, tileHeight, camera);
    }

    public void renderWorld(Graphics2D g2d, int tileWidth, int tileHeight, Camera camera) {
        g2d.setFont(font);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Tile type = tiles[x][y];

                if (type == null) {
                    continue;
                }

                int screenX = (x - camera.getX()) * tileWidth;
                int screenY = (y - camera.getY()) * tileHeight + tileHeight;

                g2d.setColor(type.getColor());
                g2d.drawString(
                        String.valueOf(type.getSymbol()),
                        screenX,
                        screenY
                );
            }
        }
    }

    /** The visible glyphs as one string per viewport row, top row first. */
    public List<String> renderToGrid(Viewport viewport, List<? extends PositionedGlyph> entities) {
        char[][] grid = new char[viewport.height()][viewport.width()];
        fillTileLayer(grid, viewport);
        overlayEntities(grid, viewport, entities);
        return Arrays.stream(grid).map(String::new).toList();
    }

    private void fillTileLayer(char[][] grid, Viewport viewport) {
        for (int row = 0; row < viewport.height(); row++) {
            for (int col = 0; col < viewport.width(); col++) {
                Tile tile = getTile(viewport.cameraX() + col, viewport.cameraY() + row);
                grid[row][col] = tile != null ? tile.getSymbol() : ' ';
            }
        }
    }

    private static void overlayEntities(char[][] grid, Viewport viewport, List<? extends PositionedGlyph> entities) {
        for (PositionedGlyph entity : entities) {
            int col = entity.getX() - viewport.cameraX();
            int row = entity.getY() - viewport.cameraY();
            if (isWithinViewport(row, col, viewport)) {
                grid[row][col] = entity.getSymbol();
            }
        }
    }

    private static boolean isWithinViewport(int row, int col, Viewport viewport) {
        return row >= 0 && row < viewport.height() && col >= 0 && col < viewport.width();
    }
}