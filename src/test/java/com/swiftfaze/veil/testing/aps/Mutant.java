package com.swiftfaze.veil.testing.aps;

import java.nio.file.Path;

/**
 * Represents a single mutation of a feature file.
 * Contains the location, original text, and replacement text.
 */
public record Mutant(
        Path featurePath,
        int line,
        int column,
        String original,
        String replacement,
        String scenarioName
) {
    @Override
    public String toString() {
        String msg = String.format("%s:%d  %s → %s", featurePath.getFileName(), line, original, replacement);
        if (scenarioName != null && !scenarioName.isEmpty()) {
            msg += " [" + scenarioName + "]";
        }
        return msg;
    }
}
