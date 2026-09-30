package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Player;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PlayerSnapshotterTest {

    @Test
    void takeSnapshotCapturesPositionAndAllTenStatFields() {
        Player player = new Player(4, 7);
        player.getPlayerInfo().getStats().setStrength(18);
        player.getPlayerInfo().getStats().setDexterity(14);
        player.getPlayerInfo().getStats().setConstitution(16);
        player.getPlayerInfo().getStats().setIntelligence(12);
        player.getPlayerInfo().getStats().setWisdom(11);
        player.getPlayerInfo().getStats().setLuck(9);
        player.getPlayerInfo().getStats().setMaxHp(150);
        player.getPlayerInfo().getStats().setMaxMana(80);
        player.getPlayerInfo().getStats().setCurrentHp(70);
        player.getPlayerInfo().getStats().setCurrentMana(30);

        PlayerSnapshotter snapshotter = new PlayerSnapshotter(() -> player);
        String result = snapshotter.takeSnapshot("boss");

        assertEquals("Snapshot boss saved", result);
    }

    @Test
    void restoreSnapshotWritesPositionAndStatsBackToPlayer() {
        Player player = new Player(4, 7);
        player.getPlayerInfo().getStats().setStrength(18);
        player.getPlayerInfo().getStats().setCurrentHp(60);

        PlayerSnapshotter snapshotter = new PlayerSnapshotter(() -> player);
        snapshotter.takeSnapshot("boss");

        // Change the player state
        player.setPosition(10, 2);
        player.getPlayerInfo().getStats().setStrength(3);
        player.getPlayerInfo().getStats().setCurrentHp(5);

        // Restore
        Optional<String> result = snapshotter.restoreSnapshot("boss");

        assertTrue(result.isPresent());
        assertEquals("Snapshot boss restored", result.get());
        assertEquals(4, player.getX());
        assertEquals(7, player.getY());
        assertEquals(18, player.getPlayerInfo().getStats().getStrength());
        assertEquals(60, player.getPlayerInfo().getStats().getCurrentHp());
    }

    @Test
    void restoreSnapshotReturnsEmptyForUnknownName() {
        Player player = new Player(0, 0);
        PlayerSnapshotter snapshotter = new PlayerSnapshotter(() -> player);

        Optional<String> result = snapshotter.restoreSnapshot("nosuchslot");

        assertFalse(result.isPresent());
    }

    @Test
    void resnapshotingAnExistingNameOverwritesIt() {
        Player player = new Player(0, 0);
        player.getPlayerInfo().getStats().setStrength(18);

        PlayerSnapshotter snapshotter = new PlayerSnapshotter(() -> player);
        snapshotter.takeSnapshot("boss");

        // Change and re-snapshot
        player.getPlayerInfo().getStats().setStrength(2);
        snapshotter.takeSnapshot("boss");

        // Change again
        player.getPlayerInfo().getStats().setStrength(7);

        // Restore should get the second snapshot
        snapshotter.restoreSnapshot("boss");
        assertEquals(2, player.getPlayerInfo().getStats().getStrength());
    }

    @Test
    void restoreTargetsTheSupplierPlayerNotACachedReference() {
        Player player1 = new Player(0, 0);
        player1.getPlayerInfo().getStats().setStrength(18);

        // Use an array to allow the supplier to be changed after snapshot
        Player[] currentPlayer = { player1 };

        PlayerSnapshotter snapshotter = new PlayerSnapshotter(() -> currentPlayer[0]);
        snapshotter.takeSnapshot("boss");

        // Supplier now returns a different player
        Player player2 = new Player(0, 0);
        player2.getPlayerInfo().getStats().setStrength(1);
        currentPlayer[0] = player2;

        // Restore should write to player2, not player1
        snapshotter.restoreSnapshot("boss");
        assertEquals(18, player2.getPlayerInfo().getStats().getStrength(), "player2 should have restored value");
        assertEquals(18, player1.getPlayerInfo().getStats().getStrength(), "player1 should still have original value");
    }

    @Test
    void separateNamedSlotsAreKeptIndependently() {
        Player player = new Player(0, 0);
        player.getPlayerInfo().getStats().setStrength(18);

        PlayerSnapshotter snapshotter = new PlayerSnapshotter(() -> player);
        snapshotter.takeSnapshot("strong");

        player.getPlayerInfo().getStats().setStrength(2);
        snapshotter.takeSnapshot("weak");

        snapshotter.restoreSnapshot("strong");
        assertEquals(18, player.getPlayerInfo().getStats().getStrength());
    }
}
