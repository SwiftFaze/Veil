package com.swiftfaze.veil.testing.quality;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * CRAP gate: Change Risk Anti-Pattern metric per method.
 * CRAP(m) = CC(m)^2 * (1 - cov(m))^3 + CC(m)
 * CC = cyclomatic complexity, cov = line coverage (0-1)
 */
public class CrapReport {
    private static final String JACOCO_REPORT = "target/site/jacoco/jacoco.xml";
    private static final String CRAP_OUTPUT = "target/crap/crap.txt";
    private static final String METRICS_OUTPUT = ".metrics/crap.edn";
    private static final double EPSILON = 0.0001;

    private final Path projectRoot;
    private final String mode; // "--gate" or "--report-only"
    private double maxCrap = 10.0;
    private final Map<String, Double> baseline = new HashMap<>();
    private final JacocoExcludesMatcher excludesMatcher;
    private final List<MethodScore> scores = new ArrayList<>();
    private int violations = 0;

    public CrapReport(String mode) throws Exception {
        this.projectRoot = Paths.get("").toAbsolutePath();
        this.mode = mode;
        this.excludesMatcher = new JacocoExcludesMatcher(projectRoot);
        loadQualityGatesProperties();
        loadBaseline();
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "--gate";
        CrapReport report = new CrapReport(mode);
        int exitCode = report.run();
        System.exit(exitCode);
    }

    public int run() throws Exception {
        Path jacocoFile = projectRoot.resolve(JACOCO_REPORT);
        if (!Files.exists(jacocoFile)) {
            System.err.println("FAIL: JaCoCo report not found: " + JACOCO_REPORT);
            return 2;
        }

        if (!excludesMatcher.isValid()) {
            System.err.println("FAIL: Could not find the exclusion list in pom.xml (jacoco-check execution)");
            return 2;
        }

        parseJacocoReport(jacocoFile);
        scores.sort((a, b) -> Double.compare(b.crap, a.crap));
        checkViolations();
        writeOutputs();

        if ("--gate".equals(mode) && violations > 0) {
            return 1;
        }
        return 0;
    }

    private void loadQualityGatesProperties() throws Exception {
        Path propsFile = projectRoot.resolve("quality-gates.properties");
        if (Files.exists(propsFile)) {
            Files.lines(propsFile)
                .filter(line -> line.startsWith("crap.max="))
                .findFirst()
                .ifPresent(line -> {
                    try {
                        maxCrap = Double.parseDouble(line.substring("crap.max=".length()).trim());
                    } catch (NumberFormatException ignored) {}
                });
        }
    }

    private void loadBaseline() throws Exception {
        Path baselineFile = projectRoot.resolve("crap-baseline.txt");
        if (Files.exists(baselineFile)) {
            Files.lines(baselineFile)
                .filter(line -> !line.trim().startsWith("#") && line.contains("#"))
                .forEach(line -> {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            baseline.merge(parts[0], Double.parseDouble(parts[1]), Math::max);
                        } catch (NumberFormatException ignored) {}
                    }
                });
        }
    }

    private void parseJacocoReport(Path jacocoFile) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(jacocoFile.toFile());

        NodeList classes = doc.getElementsByTagName("class");
        for (int c = 0; c < classes.getLength(); c++) {
            Element classElem = (Element) classes.item(c);
            String className = classElem.getAttribute("name"); // "com/example/TestClass"
            String fqcn = className.replace("/", ".");
            String classFile = className + ".class";

            if (excludesMatcher.matches(classFile)) continue;

            NodeList methods = classElem.getElementsByTagName("method");
            for (int m = 0; m < methods.getLength(); m++) {
                Element methodElem = (Element) methods.item(m);
                String methodName = methodElem.getAttribute("name");
                parseMethod(methodElem, fqcn, methodName);
            }
        }
    }

    private void parseMethod(Element methodElem, String fqcn, String methodName) {
        int cc = 0;
        long lineMissed = 0, lineCovered = 0;

        NodeList counters = methodElem.getElementsByTagName("counter");
        for (int i = 0; i < counters.getLength(); i++) {
            Element counter = (Element) counters.item(i);
            String type = counter.getAttribute("type");
            int missed = Integer.parseInt(counter.getAttribute("missed"));
            int covered = Integer.parseInt(counter.getAttribute("covered"));

            if ("COMPLEXITY".equals(type)) {
                cc = missed + covered;
            } else if ("LINE".equals(type)) {
                lineMissed = missed;
                lineCovered = covered;
            }
        }

        long totalLines = lineMissed + lineCovered;
        double cov = totalLines == 0 ? 1.0 : (double) lineCovered / totalLines;
        double crap = cc == 0 ? 0 : cc * cc * Math.pow(1 - cov, 3) + cc;
        crap = Math.round(crap * 10.0) / 10.0;

        scores.add(new MethodScore(fqcn, methodName, cc, cov, crap));
    }

    private void checkViolations() {
        Set<String> reported = new HashSet<>();

        for (MethodScore score : scores) {
            String key = score.fqcn + "#" + score.methodName;
            reported.add(key);
            Double baselineScore = baseline.get(key);

            if (baselineScore != null) {
                if (score.crap > baselineScore + EPSILON) {
                    System.out.println("FAIL " + key + " (baselined " + baselineScore + ", now " + score.crap + ")");
                    violations++;
                }
            } else if (score.crap > maxCrap + EPSILON) {
                System.out.println("FAIL " + key + " (CRAP " + score.crap + " exceeds limit " + maxCrap + ")");
                violations++;
            }
        }

        // Check baseline entries
        for (String baselineKey : baseline.keySet()) {
            if (!reported.contains(baselineKey)) {
                System.out.println("FAIL " + baselineKey + " (baseline entry for non-existent method)");
                violations++;
            } else {
                MethodScore score = scores.stream()
                    .filter(s -> (s.fqcn + "#" + s.methodName).equals(baselineKey))
                    .findFirst().orElse(null);
                if (score != null && score.crap <= maxCrap + EPSILON) {
                    System.out.println("FAIL " + baselineKey + " (now within limit, remove baseline entry)");
                    violations++;
                }
            }
        }
    }

    private void writeOutputs() throws Exception {
        Files.createDirectories(projectRoot.resolve("target/crap"));
        Files.createDirectories(projectRoot.resolve(".metrics"));

        // Write text report
        List<String> lines = new ArrayList<>();
        lines.add("CRAP Report - Worst First");
        lines.add("========================================");
        lines.add("");

        for (MethodScore score : scores) {
            boolean baselined = baseline.containsKey(score.fqcn + "#" + score.methodName);
            String marker = baselined ? " (baselined)" : "";
            String status = score.crap > maxCrap ? "OVER" : "OK";
            lines.add(String.format("%s  %s#%s - CC:%d Cov:%.0f%% CRAP:%.1f%s",
                status, score.fqcn, score.methodName, score.cc,
                score.coverage * 100, score.crap, marker));
        }

        lines.add("");
        lines.add("========================================");
        lines.add("CRAP violations: " + violations);

        Files.write(projectRoot.resolve(CRAP_OUTPUT), lines);

        // Write EDN metrics
        List<String> edn = new ArrayList<>();
        edn.add("{:entries [");
        boolean first = true;
        for (MethodScore score : scores) {
            if (!first) edn.add("             ");
            first = false;
            int coveragePercent = Math.round((float)(score.coverage * 100));
            edn.add(String.format("             {:name \"%s\" :namespace \"%s\" :complexity %d :coverage %d :crap %.1f}",
                score.methodName, score.fqcn, score.cc, coveragePercent, score.crap));
        }
        edn.add("             ]}");
        Files.write(projectRoot.resolve(METRICS_OUTPUT), edn);
    }

    private static class MethodScore {
        final String fqcn;
        final String methodName;
        final int cc;
        final double coverage;
        final double crap;

        MethodScore(String fqcn, String methodName, int cc, double coverage, double crap) {
            this.fqcn = fqcn;
            this.methodName = methodName;
            this.cc = cc;
            this.coverage = coverage;
            this.crap = crap;
        }
    }
}
