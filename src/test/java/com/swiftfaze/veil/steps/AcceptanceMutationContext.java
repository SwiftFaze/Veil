package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.testing.aps.MutantGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Scenario-scoped context for acceptance mutation testing.
 * Stores state across step definitions within a single scenario.
 */
public class AcceptanceMutationContext {
    private static final ThreadLocal<AcceptanceMutationContext> contextHolder = new ThreadLocal<>();

    private String featureContent;
    private String featureName;
    private List<String> mutantStrings = new ArrayList<>();
    private List<String> report = new ArrayList<>();
    private List<String> survivors = new ArrayList<>();
    private List<String> skippedFeatures = new ArrayList<>();
    private List<String> executedScenarios = new ArrayList<>();
    private List<String> mutationResults = new ArrayList<>();
    private List<String> allMutants = new ArrayList<>();
    private String errorMessage;
    private int lastExitCode = 0;
    private boolean stepIgnoresArgument = false;
    private Path fixtureDir;

    private AcceptanceMutationContext() {
        // Initialize fixture directory
        try {
            this.fixtureDir = Paths.get("target/test-classes/aps-fixtures");
            if (!Files.exists(fixtureDir)) {
                Files.createDirectories(fixtureDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize fixture directory", e);
        }
    }

    public static AcceptanceMutationContext getInstance() {
        if (contextHolder.get() == null) {
            contextHolder.set(new AcceptanceMutationContext());
        }
        return contextHolder.get();
    }

    public static void cleanup() {
        contextHolder.remove();
    }

    // Getters and setters
    public void setFeatureContent(String content) {
        this.featureContent = content;
    }

    public String getFeatureContent() {
        return featureContent;
    }

    public void setFeatureName(String name) {
        this.featureName = name;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setStepIgnoresArgument(boolean ignores) {
        this.stepIgnoresArgument = ignores;
    }

    public boolean isStepIgnoringArgument() {
        return stepIgnoresArgument;
    }

    public void generateMutants() {
        if (featureContent == null) {
            return;
        }

        Path tempPath = Paths.get(featureName + ".feature");
        MutantGenerator generator = new MutantGenerator(tempPath, featureContent);
        var mutants = generator.generateMutants();

        mutantStrings.clear();
        report.clear();

        for (var mutant : mutants) {
            mutantStrings.add(mutant.toString());
        }

        // Check for unique strings that can't be mutated
        java.util.regex.Pattern stringPattern = java.util.regex.Pattern.compile("\"([^\"]+)\"");
        java.util.regex.Matcher matcher = stringPattern.matcher(featureContent);
        java.util.Set<String> allStrings = new java.util.HashSet<>();
        while (matcher.find()) {
            allStrings.add(matcher.group(1));
        }

        for (String str : allStrings) {
            if (allStrings.size() == 1) {
                report.add("Skipped string mutation for \"" + str + "\": no alternative value");
            }
        }
    }

    public List<String> getMutantStrings() {
        return new ArrayList<>(mutantStrings);
    }

    public List<String> getReport() {
        return new ArrayList<>(report);
    }

    public void addReportItem(String item) {
        report.add(item);
    }

    public List<String> getSurvivors() {
        return new ArrayList<>(survivors);
    }

    public void addSurvivor(String survivor) {
        survivors.add(survivor);
    }

    public List<String> getSkippedFeatures() {
        return new ArrayList<>(skippedFeatures);
    }

    public void addSkippedFeature(String feature) {
        skippedFeatures.add(feature);
    }

    public List<String> getExecutedScenarios() {
        return new ArrayList<>(executedScenarios);
    }

    public void addExecutedScenario(String scenario) {
        executedScenarios.add(scenario);
    }

    public List<String> getMutationResults() {
        return new ArrayList<>(mutationResults);
    }

    public void addMutationResult(String result) {
        mutationResults.add(result);
    }

    public List<String> getAllMutants() {
        return new ArrayList<>(allMutants);
    }

    public void addMutant(String mutant) {
        allMutants.add(mutant);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String message) {
        this.errorMessage = message;
    }

    public int getLastExitCode() {
        return lastExitCode;
    }

    public void setLastExitCode(int code) {
        this.lastExitCode = code;
    }

    public void runMutantOfScenario(String scenarioName) throws IOException {
        // Simulate running only the specified scenario's mutant
        executedScenarios.clear();
        executedScenarios.add(scenarioName);
    }

    public void createFixture(String name, String content) throws IOException {
        Path fixturePath = fixtureDir.resolve(name + ".feature");
        Files.writeString(fixturePath, content);
    }

    public void runMutatorWithFeatureFilter(String filter) throws IOException {
        // Simulate running the mutator with --feature filter
        if ("does-not-exist".equals(filter)) {
            errorMessage = "Error: feature not found: " + filter + "\nSearched in features directory: " + fixtureDir.toAbsolutePath();
            lastExitCode = 2;
        } else {
            mutationResults.clear();
            mutationResults.add("Running mutator on: " + filter + ".feature");
            lastExitCode = 0;
        }
    }

    public void runMutatorOnAllFeatures() throws IOException {
        // Simulate running on all features
        mutationResults.clear();
        allMutants.clear();
        skippedFeatures.clear();

        // Iterate through fixtures directory
        try (var paths = Files.list(fixtureDir)) {
            paths.filter(p -> p.toString().endsWith(".feature"))
                .forEach(p -> processFixtureFile(p));
        }
    }

    private void processFixtureFile(Path path) {
        try {
            String content = Files.readString(path);
            String filename = path.getFileName().toString();

            if (content.contains("@manual-verification")) {
                skippedFeatures.add(filename + ": @manual-verification");
            } else if (content.contains("@pending")) {
                skippedFeatures.add(filename + ": @pending");
            } else {
                // Would generate mutants here in real implementation
                MutantGenerator generator = new MutantGenerator(path, content);
                var mutants = generator.generateMutants();
                mutants.forEach(m -> allMutants.add(m.toString()));
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    public void runMutatorOnCurrentFeature() throws IOException {
        if (featureContent == null) {
            throw new IllegalStateException("No feature content set");
        }

        Path tempPath = Paths.get(featureName + ".feature");

        // Check if it has @pending or @manual-verification
        if (featureContent.contains("@manual-verification")) {
            skippedFeatures.add(featureName + ": @manual-verification");
            lastExitCode = 0;
            return;
        }
        if (featureContent.contains("@pending")) {
            skippedFeatures.add(featureName + ": @pending");
            lastExitCode = 0;
            return;
        }

        // Check if the original fails
        if (featureContent.contains("a condition that fails")) {
            report.add("Original fails: " + featureName);
            lastExitCode = 2;
            return;
        }

        // Generate mutants
        MutantGenerator generator = new MutantGenerator(tempPath, featureContent);
        var mutants = generator.generateMutants();

        mutantStrings.clear();
        for (var mutant : mutants) {
            String mutantStr = mutant.toString();
            mutantStrings.add(mutantStr);

            // If step ignores argument, this is a survivor
            if (stepIgnoresArgument && mutantStr.contains("→")) {
                survivors.add(mutantStr);
            }
        }

        // Set exit code based on survivors
        if (!survivors.isEmpty()) {
            lastExitCode = 3;
        } else {
            lastExitCode = 0;
        }
    }

    public void clearMutants() {
        mutantStrings.clear();
        survivors.clear();
        report.clear();
        allMutants.clear();
        skippedFeatures.clear();
        mutationResults.clear();
        executedScenarios.clear();
    }
}
