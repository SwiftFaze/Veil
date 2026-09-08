package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Stats;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/**
 * The ten {@link Stats} fields addressable by the dev console's set/add/subtract verbs, keyed by
 * the abbreviation token issue #170's clarification pass settled on. Class and the derived Attack
 * Power/Defense rows are deliberately not here - {@link PlayerFieldMutator#apply} rejects any
 * other token as an unknown field.
 */
enum PlayerField {
    STRENGTH("str", "Strength", new Spec(0, true, Stats::getStrength, Stats::setStrength)),
    DEXTERITY("dex", "Dexterity", new Spec(0, true, Stats::getDexterity, Stats::setDexterity)),
    CONSTITUTION("con", "Constitution", new Spec(0, true, Stats::getConstitution, Stats::setConstitution)),
    INTELLIGENCE("int", "Intelligence", new Spec(0, true, Stats::getIntelligence, Stats::setIntelligence)),
    WISDOM("wis", "Wisdom", new Spec(0, true, Stats::getWisdom, Stats::setWisdom)),
    LUCK("luck", "Luck", new Spec(0, true, Stats::getLuck, Stats::setLuck)),
    MAX_HP("maxhp", "Max HP", new Spec(1, true, Stats::getMaxHp, Stats::setMaxHp)),
    MAX_MANA("maxmana", "Max Mana", new Spec(1, true, Stats::getMaxMana, Stats::setMaxMana)),
    CURRENT_HP("hp", "Current HP", new Spec(0, false, Stats::getCurrentHp, Stats::setCurrentHp)),
    CURRENT_MANA("mana", "Current Mana", new Spec(0, false, Stats::getCurrentMana, Stats::setCurrentMana));

    private final String token;
    private final String displayName;
    private final Spec spec;

    PlayerField(String token, String displayName, Spec spec) {
        this.token = token;
        this.displayName = displayName;
        this.spec = spec;
    }

    static Optional<PlayerField> fromToken(String token) {
        return Arrays.stream(values()).filter(field -> field.token.equals(token)).findFirst();
    }

    String displayName() {
        return displayName;
    }

    String token() {
        return token;
    }

    int floor() {
        return spec.floor();
    }

    boolean hasClassDefault() {
        return spec.hasClassDefault();
    }

    int get(Stats stats) {
        return spec.getter().applyAsInt(stats);
    }

    void set(Stats stats, int value) {
        spec.setter().accept(stats, value);
    }

    private record Spec(int floor, boolean hasClassDefault, ToIntFunction<Stats> getter, ObjIntConsumer<Stats> setter) {
    }
}
