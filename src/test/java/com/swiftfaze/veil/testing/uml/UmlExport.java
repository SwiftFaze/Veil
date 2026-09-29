package com.swiftfaze.veil.testing.uml;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

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

    public static void main(String[] arguments) {
        run(Path.of("").toAbsolutePath(), System.out);
    }

    static void run(Path root, PrintStream out) {
        UmlGraph graph = UmlGraph.fromClasses(productionClasses());
        write(root, graph);
        out.println("Wrote " + OUTPUT + ": " + graph.classes().size() + " classes, "
                + graph.edges().size() + " edges");
        out.println(PitMutationMetrics.convert(root));
    }

    static Collection<JavaClass> productionClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(UmlGraph.ROOT_PACKAGE);
    }

    static void write(Path root, UmlGraph graph) {
        try {
            String policy = Files.readString(root.resolve(POLICY));
            Path target = root.resolve(OUTPUT);
            Files.createDirectories(target.getParent());
            Files.writeString(target, UmlIr.render(graph, UmlPolicy.levels(policy)));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + OUTPUT, e);
        }
    }
}
