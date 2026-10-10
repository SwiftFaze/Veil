package com.swiftfaze.veil.game.worldgen;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.world.CoreTiles;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;

import java.awt.Rectangle;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Shared builders for the world-generation unit tests: the real core mods and small hand-made scenes. */
final class WorldgenFixtures {

    static final ModRegistry MODS = ModLoader.load(Paths.get("mods"));

    private WorldgenFixtures() {
    }

    static Tile tile(String id) {
        return MODS.getTile(id);
    }

    /** A scene whose every tile is {@code id}. */
    static WorldScene sceneOf(int width, int height, String id) {
        WorldScene scene = new WorldScene(width, height) {
        };
        scene.fillRegion(new Rectangle(0, 0, width, height), tile(id));
        return scene;
    }

    static void setTile(WorldScene scene, int x, int y, String id) {
        scene.fillRegion(new Rectangle(x, y, 1, 1), tile(id));
    }

    /** A building whose blueprint rows are given as strings: {@code D} door, {@code S} stone, anything else wood. */
    static Building building(int worldX, int worldY, String... rows) {
        List<List<Tile>> blueprint = new ArrayList<>();
        for (String row : rows) {
            List<Tile> tiles = new ArrayList<>();
            for (int column = 0; column < row.length(); column++) {
                tiles.add(tile(switch (row.charAt(column)) {
                    case 'D' -> CoreTiles.DOOR;
                    case 'S' -> CoreTiles.STONE;
                    default -> CoreTiles.WOOD;
                }));
            }
            blueprint.add(tiles);
        }
        Building building = new Building(blueprint);
        building.setWorldX(worldX);
        building.setWorldY(worldY);
        return building;
    }
}
