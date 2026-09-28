package com.swiftfaze.veil.testing.quality;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * The CRAP gate: scores every method in JaCoCo's report and fails when one exceeds
 * {@code crap.max} (quality-gates.properties) without a covering crap-baseline.txt entry.
 * Bound to {@code verify} after {@code jacoco:report}; see specs/features/crap-gate.feature.
 *
 * <p>Writes {@code target/crap/crap.txt} (worst first) and {@code .metrics/crap.edn} (the
 * UML viewer's metrics shape). Exit codes: 0 pass, 1 gate violations, 2 could not run.
 *
 * <p>Usage: {@code CrapReport [--gate | --report-only]}; {@code --report-only} writes the
 * same outputs but never fails, for check-clean.sh to judge afterwards. From {@code main},
 * a non-zero exit code is thrown as an exception, which fails the Maven build.
 */
public final class CrapReport {

    static final String JACOCO_XML = "target/site/jacoco/jacoco.xml";
    static final String CRAP_TXT = "target/crap/crap.txt";
    static final String CRAP_EDN = ".metrics/crap.edn";
    static final String PROPERTIES = "quality-gates.properties";
    static final String BASELINE = "crap-baseline.txt";

    static final int PASS = 0;
    static final int VIOLATIONS = 1;
    static final int CANNOT_RUN = 2;

    /** Exit code plus the lines printed for the human; the violations are the FAIL lines. */
    public record Result(int exitCode, List<String> messages) {
    }

    private CrapReport() {
    }

    public static void main(String[] args) {
        boolean reportOnly = args.length > 0 && "--report-only".equals(args[0]);
        Result result = run(Paths.get("").toAbsolutePath(), reportOnly);
        result.messages().forEach(System.out::println);
        if (result.exitCode() != PASS) {
            // exec:java runs inside Maven's JVM, where System.exit would kill Maven itself;
            // an exception from main fails the build cleanly instead.
            throw new IllegalStateException("CRAP gate failed with exit code " + result.exitCode());
        }
    }

    public static Result run(Path root, boolean reportOnly) {
        try {
            return runChecked(root, reportOnly);
        } catch (Exception e) {
            return new Result(CANNOT_RUN, List.of("CRAP gate could not run: " + e));
        }
    }

    private static Result runChecked(Path root, boolean reportOnly) throws Exception {
        Path jacocoXml = root.resolve(JACOCO_XML);
        if (!Files.exists(jacocoXml)) {
            return new Result(CANNOT_RUN, List.of("CRAP gate could not run: JaCoCo report not found: " + JACOCO_XML));
        }
        Optional<JacocoExcludes> excludes = JacocoExcludes.fromPom(root.resolve("pom.xml"));
        if (excludes.isEmpty()) {
            return new Result(CANNOT_RUN, List.of("CRAP gate could not run: could not find the exclusion list"
                    + " (the jacoco-check execution's <excludes>) in pom.xml"));
        }
        double max = loadMax(root.resolve(PROPERTIES));
        Map<String, Double> baseline = CrapBaseline.load(root.resolve(BASELINE));
        List<MethodCoverage> methods = JacocoReportParser.parse(jacocoXml).stream()
                .filter(m -> !excludes.get().matches(m.className()))
                .toList();
        CrapGate gate = new CrapGate(max, baseline);
        List<String> failures = gate.violations(methods);
        CrapOutputs.write(root, methods, gate, failures);
        return new Result(failures.isEmpty() || reportOnly ? PASS : VIOLATIONS, summary(failures));
    }

    private static List<String> summary(List<String> failures) {
        List<String> lines = new ArrayList<>(failures);
        lines.add("CRAP violations: " + failures.size() + " (report: " + CRAP_TXT + ")");
        return lines;
    }

    private static double loadMax(Path propertiesFile) throws IOException {
        if (!Files.exists(propertiesFile)) {
            throw new IOException(PROPERTIES + " not found");
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(propertiesFile)) {
            properties.load(in);
        }
        String max = properties.getProperty("crap.max");
        if (max == null) {
            throw new IOException("crap.max is not set in " + PROPERTIES);
        }
        return Double.parseDouble(max.trim());
    }
}
