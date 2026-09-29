package com.swiftfaze.veil.testing.qa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QaProcedureTest {
    @TempDir
    Path dir;

    @Test
    void loadsKeysAndExpectedEvents() throws IOException {
        Files.writeString(dir.resolve("p.keys"), "# go\nRIGHT\n");
        Files.writeString(dir.resolve("p.json"),
                "{\"script\":\"p.keys\",\"expect\":[{\"type\":\"PlayerMoved\",\"x\":6,\"y\":5}]}");

        QaProcedure procedure = QaProcedure.load(dir, "p");

        assertEquals(List.of(KeyEvent.VK_RIGHT), procedure.keys());
        assertEquals(1, procedure.expected().size());
        assertEquals(6, procedure.expected().get(0).get("x").getAsInt());
    }

    @Test
    void missingJsonNamesTheJsonFile() throws IOException {
        Files.writeString(dir.resolve("foo.keys"), "RIGHT\n");

        QaException e = assertThrows(QaException.class, () -> QaProcedure.load(dir, "foo"));

        assertTrue(e.getMessage().contains("foo.json"), e.getMessage());
    }

    @Test
    void missingKeysNamesTheKeysFile() throws IOException {
        Files.writeString(dir.resolve("foo.json"), "{\"expect\":[]}");

        QaException e = assertThrows(QaException.class, () -> QaProcedure.load(dir, "foo"));

        assertTrue(e.getMessage().contains("foo.keys"), e.getMessage());
    }

    @Test
    void anUnknownKeyFailsAtLoadTimeNamingFileAndLine() throws IOException {
        Files.writeString(dir.resolve("foo.keys"), "RIGHT\nNOT_A_KEY\n");
        Files.writeString(dir.resolve("foo.json"), "{\"expect\":[]}");

        QaException e = assertThrows(QaException.class, () -> QaProcedure.load(dir, "foo"));

        assertTrue(e.getMessage().contains("foo.keys:2"), e.getMessage());
        assertTrue(e.getMessage().contains("NOT_A_KEY"), e.getMessage());
    }

    @Test
    void aJsonFileWithoutExpectFails() throws IOException {
        Files.writeString(dir.resolve("foo.keys"), "RIGHT\n");
        Files.writeString(dir.resolve("foo.json"), "{}");

        assertThrows(QaException.class, () -> QaProcedure.load(dir, "foo"));
    }
}
