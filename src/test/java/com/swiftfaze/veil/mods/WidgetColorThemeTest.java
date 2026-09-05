package com.swiftfaze.veil.mods;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetColorThemeTest {

    @Test
    void requiredKeysHasExactlyTheTwelveWidgetThemeColorNames() {
        assertEquals(12, WidgetColorTheme.REQUIRED_KEYS.size());
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("SELECTED_HIGHLIGHT"));
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("SELECTED_TEXT"));
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("NORMAL_TEXT"));
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("DIMMED_TEXT"));
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("BACKGROUND"));
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("INVALID_HIGHLIGHT"));
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("VALID_HIGHLIGHT"));
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("TABLE_HEADER_BACKGROUND"));
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("BORDER"));
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("SCROLLBAR_THUMB"));
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("ACCENT"));
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains("WINDOW_BORDER"));
    }

    @Test
    void colorReturnsTheValueRegisteredForItsKey() {
        Map<String, Color> colors = new LinkedHashMap<>();
        colors.put("BACKGROUND", new Color(1, 2, 3));

        WidgetColorTheme theme = new WidgetColorTheme("test:theme", colors);

        assertEquals("test:theme", theme.id());
        assertEquals(new Color(1, 2, 3), theme.color("BACKGROUND"));
    }

    @Test
    void colorReturnsNullForAnUnregisteredKey() {
        WidgetColorTheme theme = new WidgetColorTheme("test:theme", Map.of());

        assertNull(theme.color("BACKGROUND"));
    }

    @Test
    void constructorCopiesTheSuppliedMapSoLaterMutationDoesNotLeakIn() {
        Map<String, Color> colors = new LinkedHashMap<>();
        colors.put("BACKGROUND", new Color(1, 2, 3));
        WidgetColorTheme theme = new WidgetColorTheme("test:theme", colors);

        colors.put("BACKGROUND", new Color(9, 9, 9));

        assertEquals(new Color(1, 2, 3), theme.color("BACKGROUND"));
    }
}
