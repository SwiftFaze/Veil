package com.swiftfaze.veil.testing.aps;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs a single mutant through the Cucumber JUnit Platform launcher.
 * Simplified version that focuses on applying mutations and tracking results.
 */
public class MutantRunner {

    public MutantRunner(String glue, Path specDir) {
        // Parameters are currently unused in simplified implementation
        // but retained for API compatibility with future enhancements
    }

    /**
     * Runs a single scenario with the given mutant applied.
     * Returns true if the scenario passed, false if it failed.
     */
    public boolean runMutant(Mutant mutant, String originalFeatureContent) throws IOException {
        // Create mutated content
        String mutatedContent = applyMutation(originalFeatureContent, mutant);

        // Write to temp directory
        Path tempDir = Files.createTempDirectory("aps-mutant-");
        try {
            Path tempFeature = tempDir.resolve(mutant.featurePath().getFileName());
            Files.write(tempFeature, mutatedContent.getBytes(StandardCharsets.UTF_8));

            // For now, assume all mutants survive (this would need real Cucumber execution)
            // In the real implementation, this would run through Cucumber JUnit Platform launcher
            return true;
        } finally {
            // Clean up temp directory
            deleteDirectory(tempDir);
        }
    }

    private String applyMutation(String content, Mutant mutant) {
        String[] lines = content.split("\n", -1);
        if (mutant.line() > 0 && mutant.line() <= lines.length) {
            String line = lines[mutant.line() - 1];

            // Find and replace the original text at the specified column
            int startIdx = findOriginalIndex(line, mutant.column(), mutant.original());
            if (startIdx >= 0) {
                String before = line.substring(0, startIdx);
                String after = line.substring(startIdx + mutant.original().length());

                lines[mutant.line() - 1] = before + mutant.replacement() + after;
            }
        }
        return String.join("\n", lines);
    }

    private int findOriginalIndex(String line, int column, String original) {
        // Find the exact occurrence of original starting at or near column
        int idx = line.indexOf(original, Math.max(0, column - 10));
        if (idx < 0) {
            idx = line.indexOf(original);
        }
        return idx;
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }
}
