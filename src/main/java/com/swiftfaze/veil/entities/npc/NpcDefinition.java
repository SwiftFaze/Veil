package com.swiftfaze.veil.entities.npc;

import org.jspecify.annotations.Nullable;

import java.awt.Color;

/**
 * An NPC as declared in a mod's {@code npcs/*.json}. Immutable registry content; each
 * world spawn creates its own {@link NonPlayerCharacter} from it.
 * {@code patrol} is null for a stationary NPC.
 */
public record NpcDefinition(String id, String name, char symbol, Color color, TileCoord spawn,
                            @Nullable Patrol patrol) {
}
