package com.swiftfaze.veil.testing.aps;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates mutants from a feature file by identifying and mutating literals.
 * Mutations include:
 * - Integers: +1 and -1
 * - Quoted strings: swapped for another value from the same file
 */
public class MutantGenerator {
    private static final Pattern INTEGER_PATTERN = Pattern.compile("\\b\\d+\\b");
    private static final Pattern STRING_PATTERN = Pattern.compile("\"([^\"]+)\"");

    private final Path featurePath;
    private final String featureContent;

    public MutantGenerator(Path featurePath, String featureContent) {
        this.featurePath = featurePath;
        this.featureContent = featureContent;
    }

    public List<Mutant> generateMutants() {
        List<Mutant> mutants = new ArrayList<>();

        String[] lines = featureContent.split("\n", -1);
        Map<String, String> stringReplacements = buildStringReplacements();

        String currentScenario = "Scenario";
        boolean inExamples = false;
        int exampleRowCount = 0;
        boolean seenHeaderRow = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.trim().startsWith("Scenario")) {
                currentScenario = line.replaceAll(".*Scenario.*:\\s+", "").trim();
                inExamples = false;
                exampleRowCount = 0;
                seenHeaderRow = false;
            }

            if (line.trim().startsWith("Examples:")) {
                inExamples = true;
                exampleRowCount = 0;
                seenHeaderRow = false;
                continue;
            }

            // Skip non-step lines
            if (line.trim().isEmpty() || line.trim().startsWith("#") || line.trim().startsWith("Feature:")) {
                continue;
            }

            String label = currentScenario;

            // Handle Examples table rows
            if (inExamples && line.trim().startsWith("|")) {
                if (!seenHeaderRow) {
                    // This is the header row, skip mutation
                    seenHeaderRow = true;
                    continue;
                }

                exampleRowCount++;
                label = currentScenario + " (row " + exampleRowCount + ")";
            }

            // Mutate integers in this line
            mutateIntegersInLine(line, i + 1, label, mutants);

            // Mutate strings in this line
            mutateStringsInLine(line, i + 1, label, stringReplacements, mutants);
        }

        return mutants;
    }

    private void mutateIntegersInLine(String line, int lineNum, String scenarioName, List<Mutant> mutants) {
        Matcher matcher = INTEGER_PATTERN.matcher(line);
        while (matcher.find()) {
            String original = matcher.group();
            try {
                int value = Integer.parseInt(original);

                // Generate +1 mutation
                mutants.add(new Mutant(
                    featurePath,
                    lineNum,
                    matcher.start(),
                    original,
                    String.valueOf(value + 1),
                    scenarioName
                ));

                // Generate -1 mutation
                mutants.add(new Mutant(
                    featurePath,
                    lineNum,
                    matcher.start(),
                    original,
                    String.valueOf(value - 1),
                    scenarioName
                ));
            } catch (NumberFormatException e) {
                // Skip non-parseable integers
            }
        }
    }

    private void mutateStringsInLine(String line, int lineNum, String scenarioName,
                                     Map<String, String> stringReplacements, List<Mutant> mutants) {
        Matcher matcher = STRING_PATTERN.matcher(line);
        while (matcher.find()) {
            String original = matcher.group(1);  // content without quotes
            String quotedOriginal = matcher.group();  // with quotes
            String replacement = stringReplacements.get(original);

            if (replacement != null) {
                mutants.add(new Mutant(
                    featurePath,
                    lineNum,
                    matcher.start(),
                    quotedOriginal,
                    "\"" + replacement + "\"",
                    scenarioName
                ));
            }
        }
    }

    private Map<String, String> buildStringReplacements() {
        Set<String> uniqueStrings = new HashSet<>();
        List<String> stringList = new ArrayList<>();

        Matcher matcher = STRING_PATTERN.matcher(featureContent);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (uniqueStrings.add(value)) {
                stringList.add(value);
            }
        }

        // For each string, find another value to swap it with
        Map<String, String> replacements = new HashMap<>();

        for (String value : stringList) {
            // Only mutate if there's another value in the file
            for (String other : stringList) {
                if (!value.equals(other)) {
                    replacements.put(value, other);
                    break;
                }
            }
        }

        return replacements;
    }
}
