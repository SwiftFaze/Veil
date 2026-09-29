package com.swiftfaze.veil.testing.uml;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UmlIrTest {

    private static final String POPUP = "ui.popup-toggle-listener";
    private static final String LISTENER = "game.game-listener";
    private static final List<List<String>> LEVELS = List.of(List.of("game", "mods"), List.of("ui"), List.of("main"));

    private static final JavaClasses IMPORTED = UmlExport.productionClasses();
    private static final UmlGraph GRAPH = UmlGraph.of(IMPORTED);
    private static final String IR = UmlIr.render(GRAPH, LEVELS);

    @Test
    void everyTopLevelClassAppearsOnceWithItsFullyQualifiedNameAsNs() {
        long topLevel = IMPORTED.stream()
                .filter(c -> c.getName().startsWith("com.swiftfaze.veil.") && c.getEnclosingClass().isEmpty())
                .filter(c -> !c.getName().endsWith("package-info"))
                .count();
        assertEquals(topLevel, GRAPH.classes().size());
        assertEquals(GRAPH.classes().size(), GRAPH.classes().stream().map(UmlGraph.Node::id).distinct().count());
        assertTrue(IR.contains("{:id :" + POPUP + " :name \"PopupToggleListener\" :ns \"com.swiftfaze.veil.ui."
                + "PopupToggleListener\" :level 1}"));
    }

    @Test
    void nestedAndAnonymousClassesAreFoldedIntoTheirOuterClass() {
        assertTrue(GRAPH.classes().stream().noneMatch(n -> n.fqcn().contains("$")));
        assertFalse(IR.contains("$"));
    }

    @Test
    void interfaceImplementationBecomesAnImplementsEdge() {
        assertTrue(GRAPH.edges().contains(new UmlGraph.Edge(POPUP, LISTENER, "implements")));
        assertTrue(IR.contains("{:from :" + POPUP + " :to :" + LISTENER + " :kind :implements}"));
    }

    @Test
    void anInterfaceIsMarkedWithTheInterfaceStereotype() {
        assertTrue(IR.contains(":ns \"com.swiftfaze.veil.game.GameListener\" :stereotype :interface :level 0}"));
    }

    @Test
    void aReferenceBecomesADependencyEdge() {
        assertTrue(GRAPH.edges().contains(new UmlGraph.Edge("main", "game.game-panel", "dependency")));
    }

    @Test
    void nothingOutsideVeilPackagesIsExported() {
        assertTrue(GRAPH.classes().stream().allMatch(n -> n.fqcn().startsWith("com.swiftfaze.veil.")));
        assertFalse(IR.contains("java."));
        assertFalse(IR.contains("javax."));
    }

    @Test
    void anInnerLayerDependingOnAnOuterOneIsMarkedViolating() {
        UmlGraph planted = new UmlGraph(List.of(), List.of(
                new UmlGraph.Edge("ui.a", "main", "dependency"),
                new UmlGraph.Edge("main", "ui.a", "dependency"),
                new UmlGraph.Edge("game.g", "ui.a", "dependency"),
                new UmlGraph.Edge("ui.a", "game.g", "implements"),
                new UmlGraph.Edge("ui.a", "ui.b", "dependency")));
        String ir = UmlIr.render(planted, LEVELS);
        assertTrue(ir.contains("{:from :ui.a :to :main :kind :dependency :violating true}"));
        assertTrue(ir.contains("{:from :game.g :to :ui.a :kind :dependency :violating true}"));
        assertTrue(ir.contains("{:from :main :to :ui.a :kind :dependency}"));
        assertTrue(ir.contains("{:from :ui.a :to :ui.b :kind :dependency}"));
        assertFalse(ir.contains("implements :violating"));
    }

    @Test
    void levelsAndOrderAreEmbeddedInTheIr() {
        assertTrue(IR.contains(" :levels [[:game :mods] [:ui] [:main]]"));
        assertTrue(IR.contains(" :order [:main :ui :game :mods]"));
        assertTrue(IR.startsWith("{:hierarchical true"));
    }

    @Test
    void kebabCasingSplitsCamelCaseWords() {
        assertEquals("popup-toggle-listener", UmlGraph.kebab("PopupToggleListener"));
        assertEquals("main", UmlGraph.kebab("Main"));
    }

    @Test
    void policyFileLevelsMirrorTheArchitectureLayers() throws Exception {
        String policy = java.nio.file.Files.readString(java.nio.file.Path.of("docs", "uml", "veil.policy.edn"));
        assertEquals(List.of(List.of("game", "world", "mods", "entities"), List.of("ui", "sandbox"),
                List.of("main")), UmlPolicy.levels(policy));
    }
}
