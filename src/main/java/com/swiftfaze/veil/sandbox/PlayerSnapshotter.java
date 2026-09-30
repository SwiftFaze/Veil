package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.entities.player.Stats;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Manages named snapshots of a live player's position and editable Stats fields, using a
 * {@link Supplier} to always read the currently-running player (the same pattern as
 * {@link PlayerFieldMutator}).
 */
public class PlayerSnapshotter implements DevConsoleSnapshotter {

    private final Supplier<Player> playerSupplier;
    private final Map<String, Snapshot> snapshots = new HashMap<>();

    public PlayerSnapshotter(Supplier<Player> playerSupplier) {
        this.playerSupplier = playerSupplier;
    }

    @Override
    public String takeSnapshot(String name) {
        Player player = playerSupplier.get();
        Snapshot snapshot = new Snapshot(
                player.getX(),
                player.getY(),
                player.getPlayerInfo().getStats()
        );
        snapshots.put(name, snapshot);
        return "Snapshot " + name + " saved";
    }

    @Override
    public Optional<String> restoreSnapshot(String name) {
        Snapshot snapshot = snapshots.get(name);
        if (snapshot == null) {
            return Optional.empty();
        }
        Player player = playerSupplier.get();
        player.setPosition(snapshot.x, snapshot.y);
        Stats stats = player.getPlayerInfo().getStats();
        stats.setStrength(snapshot.strength);
        stats.setDexterity(snapshot.dexterity);
        stats.setConstitution(snapshot.constitution);
        stats.setIntelligence(snapshot.intelligence);
        stats.setWisdom(snapshot.wisdom);
        stats.setLuck(snapshot.luck);
        stats.setMaxHp(snapshot.maxHp);
        stats.setMaxMana(snapshot.maxMana);
        stats.setCurrentHp(snapshot.currentHp);
        stats.setCurrentMana(snapshot.currentMana);
        return Optional.of("Snapshot " + name + " restored");
    }

    /**
     * Immutable snapshot of player position and stats.
     */
    private static class Snapshot {
        final int x;
        final int y;
        final int strength;
        final int dexterity;
        final int constitution;
        final int intelligence;
        final int wisdom;
        final int luck;
        final int maxHp;
        final int maxMana;
        final int currentHp;
        final int currentMana;

        Snapshot(int x, int y, Stats stats) {
            this.x = x;
            this.y = y;
            this.strength = stats.getStrength();
            this.dexterity = stats.getDexterity();
            this.constitution = stats.getConstitution();
            this.intelligence = stats.getIntelligence();
            this.wisdom = stats.getWisdom();
            this.luck = stats.getLuck();
            this.maxHp = stats.getMaxHp();
            this.maxMana = stats.getMaxMana();
            this.currentHp = stats.getCurrentHp();
            this.currentMana = stats.getCurrentMana();
        }
    }
}
