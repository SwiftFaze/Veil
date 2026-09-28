package com.swiftfaze.veil.testing.quality;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * crap-baseline.txt: methods allowed over {@code crap.max}, each capped at its recorded
 * score. One {@code <dotted.FQCN>#<method> <score>} per line; {@code #} starts a comment
 * line. Overloads share a key, so a repeated key keeps the worse score.
 */
final class CrapBaseline {

    private CrapBaseline() {
    }

    static Map<String, Double> load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return Collections.emptyMap();
        }
        Map<String, Double> entries = new LinkedHashMap<>();
        for (String raw : Files.readAllLines(file)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Malformed crap-baseline.txt line: " + raw);
            }
            entries.merge(parts[0], Double.parseDouble(parts[1]), Math::max);
        }
        return entries;
    }
}
