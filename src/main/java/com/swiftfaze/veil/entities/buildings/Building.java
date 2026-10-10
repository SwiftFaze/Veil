package com.swiftfaze.veil.entities.buildings;

import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.object.InteractableObject;
import java.util.List;

public class Building {

    private int worldX;
    private int worldY;

    private final List<List<Tile>> blueprint;
    private final List<ObjectPlacement> objects;

    /** @param blueprint rows of tiles, top row first; copied, so later changes to the argument don't leak in */
    public Building(List<List<Tile>> blueprint) {
        this(blueprint, List.of());
    }

    /**
     * @param blueprint rows of tiles, top row first; copied
     * @param objects object templates to place when this building is placed in a scene
     */
    public Building(List<List<Tile>> blueprint, List<ObjectPlacement> objects) {
        this.blueprint = blueprint.stream().map(List::copyOf).toList();
        this.objects = List.copyOf(objects);
    }

    /** An object template placed at a position relative to the building's top-left tile. */
    public record ObjectPlacement(int x, int y, InteractableObject object) {
    }

    /** Rows of tiles, top row first: {@code getBlueprint().get(y).get(x)}. Unmodifiable. */
    public List<List<Tile>> getBlueprint() {
        return blueprint;
    }

    /** Object templates this building places, relative to its top-left tile. Unmodifiable. */
    public List<ObjectPlacement> getObjects() {
        return objects;
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
