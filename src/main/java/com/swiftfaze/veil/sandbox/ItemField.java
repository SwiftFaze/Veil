package com.swiftfaze.veil.sandbox;

import java.util.Arrays;
import java.util.Optional;

/**
 * The two {@link com.swiftfaze.veil.entities.items.Item.BaseDamage} fields addressable by the dev
 * console's set/add/subtract verbs. Other fields (glyph, type, slot, effects) are deliberately
 * not here - {@link ItemFieldMutator#apply} rejects any other token as an unknown field.
 */
enum ItemField {
    MIN_DAMAGE("mindmg", "Base Damage (Min)", 0, true),
    MAX_DAMAGE("maxdmg", "Base Damage (Max)", 0, true);

    private final String token;
    private final String displayName;
    private final int floor;
    private final boolean hasClassDefault;

    ItemField(String token, String displayName, int floor, boolean hasClassDefault) {
        this.token = token;
        this.displayName = displayName;
        this.floor = floor;
        this.hasClassDefault = hasClassDefault;
    }

    static Optional<ItemField> fromToken(String token) {
        return Arrays.stream(values()).filter(field -> field.token.equals(token)).findFirst();
    }

    String displayName() {
        return displayName;
    }

    String token() {
        return token;
    }

    int floor() {
        return floor;
    }

    boolean hasClassDefault() {
        return hasClassDefault;
    }
}
