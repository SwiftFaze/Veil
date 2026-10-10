package com.swiftfaze.veil.world;

import com.swiftfaze.veil.world.object.Door;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSceneObjectLayerTest {

    private static final Tile FLOOR = new Tile("test:floor", '.', Color.GRAY, true);

    private static WorldScene floorScene() {
        WorldScene scene = new WorldScene(3, 3) {
        };
        scene.fillAll(FLOOR);
        return scene;
    }

    @Test
    void aClosedDoorBlocksItsTileAndAnOpenDoorDoesNot() {
        WorldScene scene = floorScene();
        Door door = new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false);
        scene.placeObject(1, 1, door);

        assertFalse(scene.isWalkable(1, 1));

        door.interact(false);

        assertTrue(scene.isWalkable(1, 1));
    }

    @Test
    void theGridShowsTheObjectGlyphOverTheTile() {
        WorldScene scene = floorScene();
        scene.placeObject(1, 1, new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false));

        List<String> rows = scene.renderToGrid(new Viewport(0, 0, 3, 3), List.of());

        assertEquals("...", rows.get(0));
        assertEquals(".+.", rows.get(1));
        assertEquals("...", rows.get(2));
    }

    @Test
    void aTileWithNoObjectIsUnchanged() {
        WorldScene scene = floorScene();

        assertEquals(FLOOR, scene.getTile(0, 0));
        assertEquals(null, scene.getObject(0, 0));
    }
}
