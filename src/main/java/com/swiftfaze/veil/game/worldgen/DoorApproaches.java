package com.swiftfaze.veil.game.worldgen;

import com.swiftfaze.veil.world.CoreTiles;

import com.swiftfaze.veil.entities.buildings.Building;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The world tiles directly outside a building's doors. A door on an edge row or column
 * contributes the tile just beyond that edge; a corner door contributes both.
 */
final class DoorApproaches {

    private DoorApproaches() {
    }

    static List<Point> outsideDoorsOf(Building building) {
        List<Point> approaches = new ArrayList<>();
        for (int y = 0; y < building.getHeight(); y++) {
            for (int x = 0; x < building.getWidth(); x++) {
                if (CoreTiles.DOOR.equals(building.getBlueprint().get(y).get(x).getId())) {
                    addOutsideEdges(building, x, y, approaches);
                }
            }
        }
        return approaches;
    }

    private static void addOutsideEdges(Building building, int x, int y, Collection<Point> approaches) {
        int worldX = building.getWorldX() + x;
        int worldY = building.getWorldY() + y;
        if (y == 0) {
            approaches.add(new Point(worldX, worldY - 1));
        }
        if (y == building.getHeight() - 1) {
            approaches.add(new Point(worldX, worldY + 1));
        }
        if (x == 0) {
            approaches.add(new Point(worldX - 1, worldY));
        }
        if (x == building.getWidth() - 1) {
            approaches.add(new Point(worldX + 1, worldY));
        }
    }
}
