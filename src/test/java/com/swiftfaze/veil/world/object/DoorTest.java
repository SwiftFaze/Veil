package com.swiftfaze.veil.world.object;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoorTest {

    private static Door door(boolean open) {
        return new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), open);
    }

    @Test
    void aClosedDoorOpensWhenInteractedWith() {
        Door door = door(false);

        assertEquals(InteractionResult.TOGGLED, door.interact(false));

        assertTrue(door.isOpen());
        assertTrue(door.isWalkable());
        assertEquals('/', door.getSymbol());
    }

    @Test
    void anOpenDoorClosesWhenNothingStandsInIt() {
        Door door = door(true);

        assertEquals(InteractionResult.TOGGLED, door.interact(false));

        assertFalse(door.isOpen());
        assertFalse(door.isWalkable());
        assertEquals('+', door.getSymbol());
    }

    @Test
    void anOpenDoorRefusesToCloseOnAnOccupant() {
        Door door = door(true);

        assertEquals(InteractionResult.REFUSED, door.interact(true));

        assertTrue(door.isOpen());
    }

    @Test
    void newInstancesStartInTheDefinitionsStateAndDoNotShareState() {
        Door template = door(false);
        Door placed = (Door) template.newInstance();
        placed.interact(false);

        assertTrue(placed.isOpen());
        assertFalse(((Door) template.newInstance()).isOpen());
    }
}
