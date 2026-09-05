package com.swiftfaze.veil.sandbox;

/**
 * One individually-searchable item a {@link DevConsoleProvider} contributes -
 * e.g. a single player class, not the "Classes" provider itself.
 *
 * @param namespace the owning mod's id (e.g. "core")
 * @param category  the provider's category label (e.g. "Classes")
 * @param name      the display name used both for the results table and to
 *                  look the entry back up via {@link DevConsoleProvider#createPanel(String)}
 */
public record DevConsoleEntry(String namespace, String category, String name) {
}
