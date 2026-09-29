package com.swiftfaze.veil.testing.uml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PitMutationMetricsTest {

    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mutations>
              <mutation detected='true' status='KILLED'><sourceFile>Player.java</sourceFile>
                <mutatedClass>com.swiftfaze.veil.entities.player.Player</mutatedClass>
                <mutatedMethod>move</mutatedMethod></mutation>
              <mutation detected='false' status='SURVIVED'><sourceFile>Player.java</sourceFile>
                <mutatedClass>com.swiftfaze.veil.entities.player.Player</mutatedClass>
                <mutatedMethod>move</mutatedMethod></mutation>
              <mutation detected='false' status='NO_COVERAGE'><sourceFile>Player.java</sourceFile>
                <mutatedClass>com.swiftfaze.veil.entities.player.Player$1</mutatedClass>
                <mutatedMethod>heal</mutatedMethod></mutation>
              <mutation detected='true' status='TIMED_OUT'><sourceFile>Main.java</sourceFile>
                <mutatedClass>com.swiftfaze.veil.Main</mutatedClass>
                <mutatedMethod>&lt;init&gt;</mutatedMethod></mutation>
              <mutation detected='false' status='NON_VIABLE'><sourceFile>Main.java</sourceFile>
                <mutatedClass>com.swiftfaze.veil.Main</mutatedClass>
                <mutatedMethod>main</mutatedMethod></mutation>
            </mutations>
            """;

    @TempDir
    Path root;

    @Test
    void aMissingReportSkipsWithAMessageAndWritesNothing() throws Exception {
        String message = PitMutationMetrics.convert(root);
        assertTrue(message.contains("Mutation report missing"));
        assertFalse(Files.exists(root.resolve(PitMutationMetrics.OUTPUT_DIR)));
    }

    @Test
    void outcomesAreTalliedPerMethodAndNestedClassesFoldIntoTheirOuterClass() throws Exception {
        var byClass = PitMutationMetrics.tally(new java.io.ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)));
        var player = byClass.get("com.swiftfaze.veil.entities.player.Player");
        assertEquals(new PitMutationMetrics.Tally(1, 1, 0), player.get("move"));
        assertEquals(new PitMutationMetrics.Tally(0, 0, 1), player.get("heal"));
        assertEquals(new PitMutationMetrics.Tally(1, 0, 0), byClass.get("com.swiftfaze.veil.Main").get("<init>"));
        assertFalse(byClass.get("com.swiftfaze.veil.Main").containsKey("main"));
    }

    @Test
    void oneSnapshotPerClassIsWrittenInTheOverlaysShape() throws Exception {
        Path xml = root.resolve(PitMutationMetrics.MUTATIONS_XML);
        Files.createDirectories(xml.getParent());
        Files.writeString(xml, XML);
        Path stale = root.resolve(PitMutationMetrics.OUTPUT_DIR).resolve("old.edn");
        Files.createDirectories(stale.getParent());
        Files.writeString(stale, "{}");

        assertEquals("Wrote 2 mutation snapshots to " + PitMutationMetrics.OUTPUT_DIR, PitMutationMetrics.convert(root));

        assertFalse(Files.exists(stale));
        String player = Files.readString(root.resolve(PitMutationMetrics.OUTPUT_DIR)
                .resolve("com.swiftfaze.veil.entities.player.Player.edn"));
        assertEquals("""
                {:namespace "com.swiftfaze.veil.entities.player.Player"
                 :forms [{:id "defn/heal" :killed 0 :survived 0 :uncovered 1 :sites 1}
                         {:id "defn/move" :killed 1 :survived 1 :uncovered 0 :sites 2}]}
                """, player);
    }
}
