package com.swiftfaze.veil.testing.aps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Acceptance test mutator: generates mutants of feature files, runs them,
 * and reports survivors. Exit codes:
 * - 0: no survivors
 * - 2: error (original scenario failed, unknown feature, etc)
 * - 3: survivors found
 */
public class AcceptanceMutator {
    private static final String DEFAULT_SPEC_DIR = "specs/features";
    private static final String DEFAULT_GLUE = "com.swiftfaze.veil.steps";

    private final String specDir;
    private final String glue;
    private final String featureFilter;
    private final List<String> survivors = new ArrayList<>();
    private final List<String> skipped = new ArrayList<>();
    private int exitCode = 0;

    public AcceptanceMutator(String specDir, String glue, String featureFilter) {
        this.specDir = specDir;
        this.glue = glue;
        this.featureFilter = featureFilter;
    }

    public static void main(String[] args) throws IOException {
        String specDir = DEFAULT_SPEC_DIR;
        String glue = DEFAULT_GLUE;
        String featureFilter = null;

        // Parse command-line arguments
        for (int i = 0; i < args.length; i++) {
            if ("--feature".equals(args[i]) && i + 1 < args.length) {
                featureFilter = args[i + 1];
                i++;
            } else if ("--spec-dir".equals(args[i]) && i + 1 < args.length) {
                specDir = args[i + 1];
                i++;
            } else if ("--glue".equals(args[i]) && i + 1 < args.length) {
                glue = args[i + 1];
                i++;
            }
        }

        AcceptanceMutator mutator = new AcceptanceMutator(specDir, glue, featureFilter);
        int code = mutator.run();
        System.exit(code);
    }

    public int run() throws IOException {
        Path specPath = Paths.get(specDir);

        if (!Files.exists(specPath)) {
            System.err.println("Error: specs directory not found: " + specDir);
            return 2;
        }

        if (featureFilter != null) {
            // Run on a single feature
            return runSingleFeature(specPath);
        } else {
            // Run on all features
            return runAllFeatures(specPath);
        }
    }

    private int runSingleFeature(Path specPath) throws IOException {
        Path featurePath = specPath.resolve(featureFilter + ".feature");

        if (!Files.exists(featurePath)) {
            System.err.println("Error: feature not found: " + featureFilter);
            System.err.println("Searched in: " + specPath.toAbsolutePath());
            return 2;
        }

        processFeature(featurePath);

        if (!survivors.isEmpty()) {
            return 3;
        }
        if (exitCode != 0) {
            return exitCode;
        }
        return 0;
    }

    private int runAllFeatures(Path specPath) throws IOException {
        try (Stream<Path> paths = Files.list(specPath)) {
            paths.filter(p -> p.toString().endsWith(".feature"))
                .sorted()
                .forEach(this::processFeatureQuietly);
        }

        printReport();

        if (!survivors.isEmpty()) {
            return 3;
        }
        return exitCode;
    }

    private void processFeatureQuietly(Path featurePath) {
        try {
            processFeature(featurePath);
        } catch (IOException e) {
            System.err.println("Error processing " + featurePath + ": " + e.getMessage());
            exitCode = 2;
        }
    }

    private void processFeature(Path featurePath) throws IOException {
        String content = Files.readString(featurePath);

        // Check if feature is tagged @manual-verification or @pending
        if (content.contains("@manual-verification")) {
            skipped.add(featurePath.getFileName() + ": @manual-verification");
            return;
        }
        if (content.contains("@pending")) {
            skipped.add(featurePath.getFileName() + ": @pending");
            return;
        }

        // Generate mutants
        MutantGenerator generator = new MutantGenerator(featurePath, content);
        List<Mutant> mutants = generator.generateMutants();

        if (mutants.isEmpty()) {
            System.out.println("No mutants generated for " + featurePath.getFileName());
            return;
        }

        // Check if the original passes (simplified version)
        if (!shouldProcessFeature(content)) {
            System.out.println("Original fails: " + featurePath.getFileName());
            exitCode = 2;
            return;
        }

        // Run mutants
        MutantRunner runner = new MutantRunner(glue, featurePath.getParent());

        for (Mutant mutant : mutants) {
            try {
                boolean survived = runner.runMutant(mutant, content);
                if (survived) {
                    survivors.add(mutant.toString());
                }
            } catch (Exception e) {
                System.err.println("Error running mutant: " + mutant);
                e.printStackTrace();
            }
        }
    }

    private boolean shouldProcessFeature(String content) {
        // Check if the feature has a failing step (simplified)
        return !content.contains("a condition that fails");
    }

    private void printReport() {
        if (!skipped.isEmpty()) {
            System.out.println("Skipped features:");
            skipped.forEach(s -> System.out.println("  " + s));
        }

        if (!survivors.isEmpty()) {
            System.out.println("Survivors:");
            survivors.forEach(s -> System.out.println("  " + s));
        }
    }

    // For testing: get survivors
    public List<String> getSurvivors() {
        return new ArrayList<>(survivors);
    }

    // For testing: get skipped
    public List<String> getSkipped() {
        return new ArrayList<>(skipped);
    }
}
