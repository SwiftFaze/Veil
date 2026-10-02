package com.swiftfaze.veil.entities.buildings;

import com.swiftfaze.veil.world.Tile;
import java.util.List;

public class Building {

    private int worldX;
    private int worldY;

    private final List<List<Tile>> blueprint;

    /** @param blueprint rows of tiles, top row first; copied, so later changes to the argument don't leak in */
    public Building(List<List<Tile>> blueprint) {
        this.blueprint = blueprint.stream().map(List::copyOf).toList();
    }

    /** Rows of tiles, top row first: {@code getBlueprint().get(y).get(x)}. Unmodifiable. */
    public List<List<Tile>> getBlueprint() {
        return blueprint;
    }

    public int getWorldX() {
        return worldX;
    }

    public void setWorldX(int worldX) {
        this.worldX = worldX;
    }

    public int getWorldY() {
        return worldY;
    }

    public void setWorldY(int worldY) {
        this.worldY = worldY;
    }
}