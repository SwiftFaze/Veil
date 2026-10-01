package com.swiftfaze.veil;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class AppVersionTest {

    @Test
    void readsVersionFromProperties() {
        String props = "version=0.5.0-beta.39\n";
        AppVersion version = new AppVersion(() -> new ByteArrayInputStream(props.getBytes()));
        assertEquals("v0.5.0-beta.39", version.getDisplayVersion());
    }

    @Test
    void returnsEmptyStringWhenResourceIsAbsent() {
        AppVersion version = new AppVersion(() -> null);
        assertEquals("", version.getDisplayVersion());
    }

    @Test
    void returnsEmptyStringWhenVersionKeyIsMissing() {
        String props = "other.key=value\n";
        AppVersion version = new AppVersion(() -> new ByteArrayInputStream(props.getBytes()));
        assertEquals("", version.getDisplayVersion());
    }

    @Test
    void returnsEmptyStringWhenVersionIsUnfiltered() {
        String props = "version=${project.version}\n";
        AppVersion version = new AppVersion(() -> new ByteArrayInputStream(props.getBytes()));
        assertEquals("", version.getDisplayVersion());
    }

    @Test
    void trimsWhitespaceFromVersion() {
        String props = "version=  0.5.0-beta.39  \n";
        AppVersion version = new AppVersion(() -> new ByteArrayInputStream(props.getBytes()));
        assertEquals("v0.5.0-beta.39", version.getDisplayVersion());
    }

    @Test
    void returnsEmptyStringWhenVersionIsEmpty() {
        String props = "version=\n";
        AppVersion version = new AppVersion(() -> new ByteArrayInputStream(props.getBytes()));
        assertEquals("", version.getDisplayVersion());
    }
}
