package com.swiftfaze.veil.testing.uml;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Converts PIT's {@code target/pit-reports/mutations.xml} into the per-class snapshots
 * uml-viewer's overlay reads from {@code .metrics/mutate/}: one {@code <fqcn>.edn} of
 * {@code {:namespace fqcn :forms [{:id "defn/<method>" :killed :survived :uncovered :sites}]}}.
 * The overlay keys a class by {@code :namespace} (= the class's {@code :ns}, its FQCN) and a
 * member by the {@code defn/} name (= the method name crap.edn uses), so the match is exact;
 * overloads share one entry, as they do in crap.edn. Nested classes fold into their outer class.
 */
public final class PitMutationMetrics {

    static final Path MUTATIONS_XML = Path.of("target", "pit-reports", "mutations.xml");
    static final Path OUTPUT_DIR = Path.of(".metrics", "mutate");

    private PitMutationMetrics() {
    }

    record Tally(int killed, int survived, int uncovered) {
        Tally plus(Tally other) {
            return new Tally(killed + other.killed, survived + other.survived, uncovered + other.uncovered);
        }

        int sites() {
            return killed + survived + uncovered;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(convert(Path.of("").toAbsolutePath()));
    }

    /** Converts if the PIT XML exists under {@code root}; returns a one-line message either way. */
    static String convert(Path root) throws Exception {
        Path xml = root.resolve(MUTATIONS_XML);
        if (!Files.isRegularFile(xml)) {
            return "Mutation report missing (" + MUTATIONS_XML + "): skipping mutation colouring."
                    + " Run: mvn org.pitest:pitest-maven:mutationCoverage";
        }
        Map<String, Map<String, Tally>> byClass;
        try (InputStream in = Files.newInputStream(xml)) {
            byClass = tally(in);
        }
        Path out = root.resolve(OUTPUT_DIR);
        clearSnapshots(out);
        for (Map.Entry<String, Map<String, Tally>> entry : byClass.entrySet()) {
            Files.writeString(out.resolve(entry.getKey() + ".edn"), snapshot(entry.getKey(), entry.getValue()));
        }
        return "Wrote " + byClass.size() + " mutation snapshots to " + OUTPUT_DIR;
    }

    private static void clearSnapshots(Path dir) throws IOException {
        Files.createDirectories(dir);
        try (Stream<Path> files = Files.list(dir)) {
            for (Path old : files.filter(p -> p.toString().endsWith(".edn")).toList()) {
                Files.delete(old);
            }
        }
    }

    /** {@code fqcn -> method -> tally}, folding nested classes into the outer class. */
    static Map<String, Map<String, Tally>> tally(InputStream xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        NodeList mutations = factory.newDocumentBuilder().parse(xml).getElementsByTagName("mutation");
        Map<String, Map<String, Tally>> byClass = new TreeMap<>();
        for (int i = 0; i < mutations.getLength(); i++) {
            Element mutation = (Element) mutations.item(i);
            Tally tally = outcome(mutation.getAttribute("status"));
            if (tally != null) {
                String owner = outerClass(text(mutation, "mutatedClass"));
                byClass.computeIfAbsent(owner, k -> new TreeMap<>())
                        .merge(text(mutation, "mutatedMethod"), tally, Tally::plus);
            }
        }
        return byClass;
    }

    private static Tally outcome(String status) {
        return switch (status) {
            case "KILLED", "TIMED_OUT", "MEMORY_ERROR" -> new Tally(1, 0, 0);
            case "SURVIVED" -> new Tally(0, 1, 0);
            case "NO_COVERAGE" -> new Tally(0, 0, 1);
            default -> null;
        };
    }

    private static String outerClass(String className) {
        int nested = className.indexOf('$');
        return nested < 0 ? className : className.substring(0, nested);
    }

    private static String text(Element parent, String tag) {
        return parent.getElementsByTagName(tag).item(0).getTextContent();
    }

    static String snapshot(String fqcn, Map<String, Tally> methods) {
        List<String> forms = new ArrayList<>();
        methods.forEach((method, t) -> forms.add("{:id \"defn/" + method + "\" :killed " + t.killed()
                + " :survived " + t.survived() + " :uncovered " + t.uncovered() + " :sites " + t.sites() + "}"));
        return "{:namespace \"" + fqcn + "\"\n :forms [" + String.join("\n         ", forms) + "]}\n";
    }
}
