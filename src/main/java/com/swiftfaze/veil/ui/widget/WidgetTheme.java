package com.swiftfaze.veil.ui.widget;

import com.swiftfaze.veil.mods.WidgetColorTheme;

import javax.swing.JLabel;
import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;

public final class WidgetTheme {

    // Immutable record holding all 13 theme colors. The defaults below remain as fallback
    // values so any widget built without ModLoader ever running (e.g. a unit test) still
    // gets sane colors.
    private record Palette(
            Color selectedHighlight,
            Color selectedText,
            Color normalText,
            Color dimmedText,
            Color background,
            Color invalidHighlight,
            Color validHighlight,
            Color tableHeaderBackground,
            Color border,
            Color scrollbarThumb,
            Color accent,
            Color windowBorder,
            Color tableHeaderText
    ) {}

    private static final AtomicReference<Palette> palette = new AtomicReference<>(
            new Palette(
                    Color.LIGHT_GRAY,
                    Color.BLACK,
                    Color.WHITE,
                    Color.GRAY,
                    Color.BLACK,
                    Color.decode("#e05a4e"),
                    Color.decode("#6fcf7d"),
                    Color.decode("#1a1a1a"),
                    Color.LIGHT_GRAY,
                    Color.GRAY,
                    Color.decode("#eeb392"),
                    Color.WHITE,
                    Color.decode("#00c2c2")
            )
    );

    // Sandbox kitchen-sink preview colors. Constant (not part of the mod-loadable theme), so
    // applyTheme() leaves them alone; the tints are the valid/invalid highlights at alpha 80.
    public static final Color WALKABLE_TINT = new Color(111, 207, 125, 80);
    public static final Color UNWALKABLE_TINT = new Color(224, 90, 78, 80);
    public static final Color PREVIEW_MARKER = Color.decode("#ef481f");

    // Public static accessors for each color
    public static Color selectedHighlight() {
        return palette.get().selectedHighlight;
    }

    public static Color selectedText() {
        return palette.get().selectedText;
    }

    public static Color normalText() {
        return palette.get().normalText;
    }

    public static Color dimmedText() {
        return palette.get().dimmedText;
    }

    public static Color background() {
        return palette.get().background;
    }

    public static Color invalidHighlight() {
        return palette.get().invalidHighlight;
    }

    public static Color validHighlight() {
        return palette.get().validHighlight;
    }

    public static Color tableHeaderBackground() {
        return palette.get().tableHeaderBackground;
    }

    public static Color border() {
        return palette.get().border;
    }

    public static Color scrollbarThumb() {
        return palette.get().scrollbarThumb;
    }

    public static Color accent() {
        return palette.get().accent;
    }

    public static Color windowBorder() {
        return palette.get().windowBorder;
    }

    public static Color tableHeaderText() {
        return palette.get().tableHeaderText;
    }

    /**
     * Overwrites all 13 widget colors from a mod-loaded theme. Called once at startup
     * (see {@code Main.loadGame}) with whichever theme owns ID "core:default" — see
     * {@code WidgetColorTheme.REQUIRED_KEYS} for the key set this reads.
     */
    public static void applyTheme(WidgetColorTheme theme) {
        palette.set(new Palette(
                theme.color("SELECTED_HIGHLIGHT"),
                theme.color("SELECTED_TEXT"),
                theme.color("NORMAL_TEXT"),
                theme.color("DIMMED_TEXT"),
                theme.color("BACKGROUND"),
                theme.color("INVALID_HIGHLIGHT"),
                theme.color("VALID_HIGHLIGHT"),
                theme.color("TABLE_HEADER_BACKGROUND"),
                theme.color("BORDER"),
                theme.color("SCROLLBAR_THUMB"),
                theme.color("ACCENT"),
                theme.color("WINDOW_BORDER"),
                theme.color("TABLE_HEADER_TEXT")
        ));
    }

    /**
     * The one place every widget's "selected" look is defined: a filled background, not just
     * recolored text — applied identically by ListWidget, TableWidget, and RadioGroupWidget, so
     * the highlight convention can't quietly diverge between widgets. Requires the label be
     * opaque (set once, at label-creation time) for the background fill to actually paint.
     */
    public static void applySelection(JLabel label, boolean selected) {
        label.setForeground(selected ? selectedText() : normalText());
        label.setBackground(selected ? selectedHighlight() : background());
    }

    private WidgetTheme() {
    }
}