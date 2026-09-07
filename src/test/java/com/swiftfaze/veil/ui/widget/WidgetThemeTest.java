package com.swiftfaze.veil.ui.widget;

import com.swiftfaze.veil.mods.WidgetColorTheme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * WidgetTheme's fields are mutable statics shared across the whole test JVM (see
 * WidgetTheme.applyTheme's javadoc), so this test snapshots and restores them around every
 * case to avoid leaking a mutation into other tests running in the same fork.
 */
class WidgetThemeTest {

    private Map<String, Color> originalColors;

    @BeforeEach
    void snapshotOriginalColors() {
        originalColors = currentColors();
    }

    @AfterEach
    void restoreOriginalColors() {
        WidgetTheme.applyTheme(new WidgetColorTheme("test:snapshot", originalColors));
    }

    @ParameterizedTest
    @MethodSource("keysAndExpectedColors")
    void applyThemeSetsTheStaticFieldForItsKey(String key, Color expected) {
        WidgetTheme.applyTheme(new WidgetColorTheme("test:theme", fullColorMap()));

        assertEquals(expected, widgetThemeColor(key));
    }

    private static Stream<Arguments> keysAndExpectedColors() {
        return fullColorMap().entrySet().stream()
                .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
    }

    private static Map<String, Color> fullColorMap() {
        Map<String, Color> colors = new LinkedHashMap<>();
        colors.put("SELECTED_HIGHLIGHT", new Color(10, 20, 30));
        colors.put("SELECTED_TEXT", new Color(11, 21, 31));
        colors.put("NORMAL_TEXT", new Color(12, 22, 32));
        colors.put("DIMMED_TEXT", new Color(13, 23, 33));
        colors.put("BACKGROUND", new Color(14, 24, 34));
        colors.put("INVALID_HIGHLIGHT", new Color(15, 25, 35));
        colors.put("VALID_HIGHLIGHT", new Color(16, 26, 36));
        colors.put("TABLE_HEADER_BACKGROUND", new Color(17, 27, 37));
        colors.put("BORDER", new Color(18, 28, 38));
        colors.put("SCROLLBAR_THUMB", new Color(19, 29, 39));
        colors.put("ACCENT", new Color(20, 30, 40));
        colors.put("TABLE_HEADER_TEXT", new Color(21, 31, 41));
        return colors;
    }

    private Color widgetThemeColor(String key) {
        return switch (key) {
            case "SELECTED_HIGHLIGHT" -> WidgetTheme.SELECTED_HIGHLIGHT;
            case "SELECTED_TEXT" -> WidgetTheme.SELECTED_TEXT;
            case "NORMAL_TEXT" -> WidgetTheme.NORMAL_TEXT;
            case "DIMMED_TEXT" -> WidgetTheme.DIMMED_TEXT;
            case "BACKGROUND" -> WidgetTheme.BACKGROUND;
            case "INVALID_HIGHLIGHT" -> WidgetTheme.INVALID_HIGHLIGHT;
            case "VALID_HIGHLIGHT" -> WidgetTheme.VALID_HIGHLIGHT;
            case "TABLE_HEADER_BACKGROUND" -> WidgetTheme.TABLE_HEADER_BACKGROUND;
            case "BORDER" -> WidgetTheme.BORDER;
            case "SCROLLBAR_THUMB" -> WidgetTheme.SCROLLBAR_THUMB;
            case "ACCENT" -> WidgetTheme.ACCENT;
            case "TABLE_HEADER_TEXT" -> WidgetTheme.TABLE_HEADER_TEXT;
            default -> throw new IllegalArgumentException("Unknown WidgetTheme color key: " + key);
        };
    }

    private Map<String, Color> currentColors() {
        Map<String, Color> colors = new LinkedHashMap<>();
        colors.put("SELECTED_HIGHLIGHT", WidgetTheme.SELECTED_HIGHLIGHT);
        colors.put("SELECTED_TEXT", WidgetTheme.SELECTED_TEXT);
        colors.put("NORMAL_TEXT", WidgetTheme.NORMAL_TEXT);
        colors.put("DIMMED_TEXT", WidgetTheme.DIMMED_TEXT);
        colors.put("BACKGROUND", WidgetTheme.BACKGROUND);
        colors.put("INVALID_HIGHLIGHT", WidgetTheme.INVALID_HIGHLIGHT);
        colors.put("VALID_HIGHLIGHT", WidgetTheme.VALID_HIGHLIGHT);
        colors.put("TABLE_HEADER_BACKGROUND", WidgetTheme.TABLE_HEADER_BACKGROUND);
        colors.put("BORDER", WidgetTheme.BORDER);
        colors.put("SCROLLBAR_THUMB", WidgetTheme.SCROLLBAR_THUMB);
        colors.put("ACCENT", WidgetTheme.ACCENT);
        colors.put("TABLE_HEADER_TEXT", WidgetTheme.TABLE_HEADER_TEXT);
        return colors;
    }
}
