package com.swiftfaze.veil.world.object;

import java.awt.Color;

public final class Door implements InteractableObject {

    /** The glyph drawn while the door is open, and while it is closed. */
    public record Glyphs(char open, char closed) {
    }

    private final String id;
    private final Color color;
    private final Glyphs glyphs;
    private final boolean initiallyOpen;
    private boolean open;

    public Door(String id, Color color, Glyphs glyphs, boolean open) {
        this.id = id;
        this.color = color;
        this.glyphs = glyphs;
        this.initiallyOpen = open;
        this.open = open;
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public char getSymbol() {
        return open ? glyphs.open() : glyphs.closed();
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public boolean isWalkable() {
        return open;
    }

    @Override
    public InteractionResult interact(boolean tileOccupied) {
        if (open && tileOccupied) {
            return InteractionResult.REFUSED;
        }
        open = !open;
        return InteractionResult.TOGGLED;
    }

    @Override
    public Door newInstance() {
        return new Door(id, color, glyphs, initiallyOpen);
    }
}
