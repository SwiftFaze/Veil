package com.swiftfaze.veil.world;

import com.swiftfaze.veil.Camera;
import com.swiftfaze.veil.entities.buildings.Building;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorldSceneTest {

    private static final Tile WALL = new Tile("test:wall", '#', Color.GRAY, false);
    private static final Tile GRASS = new Tile("test:grass", ',', Color.GREEN, true);
    private static final Tile STONE = new Tile("test:stone", '%', Color.DARK_GRAY, false);
    private static final Tile WOOD = new Tile("test:wood", '+', Color.ORANGE, true);
    private static final Tile DOOR = new Tile("test:door", '/', Color.YELLOW, true);
    private static final Tile WATER = new Tile("test:water", '~', Color.BLUE, false);

    private WorldScene sceneOf(int width, int height) {
        return new WorldScene(width, height) {
        };
    }

    @Test
    void createBorderFillsAllFourEdgesButNotTheInterior() {
        WorldScene scene = sceneOf(5, 5);

        scene.createBorder(5, 5, WALL);

        assertEquals(WALL, scene.getTile(0, 0));
        assertEquals(WALL, scene.getTile(4, 0));
        assertEquals(WALL, scene.getTile(0, 4));
        assertEquals(WALL, scene.getTile(4, 4));
        assertEquals(WALL, scene.getTile(2, 0));
        assertEquals(WALL, scene.getTile(0, 2));
        assertNull(scene.getTile(2, 2));
    }

    @Test
    void isWalkableReturnsFalseOutOfBounds() {
        WorldScene scene = sceneOf(5, 5);

        assertFalse(scene.isWalkable(-1, 0));
        assertFalse(scene.isWalkable(0, -1));
        assertFalse(scene.isWalkable(5, 0));
        assertFalse(scene.isWalkable(0, 5));
    }

    @Test
    void getTileReturnsNullOutOfBounds() {
        WorldScene scene = sceneOf(5, 5);

        assertNull(scene.getTile(-1, 0));
        assertNull(scene.getTile(0, 5));
    }

    @Test
    void getTileReturnsActualTileInBounds() {
        WorldScene scene = sceneOf(5, 5);
        scene.fillRegion(new Rectangle(2, 2, 1, 1), STONE);

        assertEquals(STONE, scene.getTile(2, 2));
    }

    @Test
    void getWidthAndHeightReturnConstructorValues() {
        WorldScene scene = sceneOf(7, 9);

        assertEquals(7, scene.getWidth());
        assertEquals(9, scene.getHeight());
    }

    @Test
    void positionSymbolAndColorAreFixedDefaults() {
        WorldScene scene = sceneOf(5, 5);

        assertEquals(0, scene.getX());
        assertEquals(0, scene.getY());
        assertEquals(' ', scene.getSymbol());
        assertEquals(Color.WHITE, scene.getColor());
    }

    @Test
    void renderDrawsNonEmptyTilesWithoutThrowing() {
        WorldScene scene = sceneOf(5, 5);
        scene.fillRegion(new Rectangle(1, 1, 1, 1), GRASS);
        Graphics2D g2d = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();

        assertDoesNotThrow(() -> scene.render(g2d, 15, 15, new Camera(0, 0)));
    }

    @Test
    void renderWorldSkipsEmptyTilesWithoutThrowing() {
        WorldScene scene = sceneOf(5, 5);
        Graphics2D g2d = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();

        assertDoesNotThrow(() -> scene.renderWorld(g2d, 15, 15, new Camera(0, 0)));
    }

    @Test
    void fillAllSetsEveryTileToTheGivenType() {
        WorldScene scene = sceneOf(4, 3);

        scene.fillAll(GRASS);

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 3; y++) {
                assertEquals(GRASS, scene.getTile(x, y));
            }
        }
    }

    @Test
    void placeBuildingStampsBlueprintAtWorldOffsetWithoutTransposingAxes() {
        WorldScene scene = sceneOf(10, 10);
        Tile[][] blueprint = {
                {WALL, WOOD},
                {STONE, DOOR}
        };
        Building building = new Building(blueprint);
        building.setWorldX(3);
        building.setWorldY(4);

        scene.placeBuilding(building);

        assertEquals(WALL, scene.getTile(3, 4));
        assertEquals(WOOD, scene.getTile(4, 4));
        assertEquals(STONE, scene.getTile(3, 5));
        assertEquals(DOOR, scene.getTile(4, 5));
    }

    @Test
    void placeBuildingOverwritesTilesAlreadyInTheScene() {
        WorldScene scene = sceneOf(10, 10);
        scene.fillRegion(new Rectangle(6, 6, 1, 1), WATER);
        Building building = new Building(new Tile[][]{{WALL}});
        building.setWorldX(6);
        building.setWorldY(6);

        scene.placeBuilding(building);

        assertEquals(WALL, scene.getTile(6, 6));
    }

    @Test
    void placeBuildingWithEmptyBlueprintLeavesTheSceneUnchanged() {
        WorldScene scene = sceneOf(10, 10);
        scene.fillAll(GRASS);
        Building building = new Building(new Tile[0][0]);
        building.setWorldX(2);
        building.setWorldY(2);

        assertDoesNotThrow(() -> scene.placeBuilding(building));

        assertEquals(GRASS, scene.getTile(2, 2));
    }
}
