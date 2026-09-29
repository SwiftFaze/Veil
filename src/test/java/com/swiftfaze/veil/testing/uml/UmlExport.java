package com.swiftfaze.veil.testing.uml;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes {@code target/uml/veil.edn}, the diagram uml-viewer opens, from Veil's compiled
 * production classes, then converts PIT's mutation report if there is one. Run from the
 * repo root: {@code bb export} in {@code tools/uml}.
 */
public final class UmlExport {

    static final Path POLICY = Path.of("docs", "uml", "veil.policy.edn");
    static final Path OUTPUT = Path.of("target", "uml", "veil.edn");

    private UmlExport() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        UmlGraph graph = UmlGraph.of(productionClasses());
        write(root, graph);
        System.out.println("Wrote " + OUTPUT + ": " + graph.classes().size() + " classes, "
                + graph.edges().size() + " edges");
        System.out.println(PitMutationMetrics.convert(root));
    }

    static JavaClasses productionClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(UmlGraph.ROOT_PACKAGE);
    }

    static void write(Path root, UmlGraph graph) throws IOException {
        String policy = Files.readString(root.resolve(POLICY));
        Path out = root.resolve(OUTPUT);
        Files.createDirectories(out.getParent());
        Files.writeString(out, UmlIr.render(graph, UmlPolicy.levels(policy)));
    }
}
