package com.swiftfaze.veil.ui.widget;

import com.swiftfaze.veil.mods.WidgetColorTheme;

import javax.swing.JLabel;
import java.awt.Color;

public final class WidgetTheme {

    // Mutable (not `final`) so applyTheme() can repopulate them from a mod-loaded
    // WidgetColorTheme at startup; the hardcoded values below remain as defaults so any
    // widget built without ModLoader ever running (e.g. a unit test) still gets sane colors.
    public static Color SELECTED_HIGHLIGHT = Color.LIGHT_GRAY;
    public static Color SELECTED_TEXT = Color.BLACK;
    public static Color NORMAL_TEXT = Color.WHITE;
    public static Color DIMMED_TEXT = Color.GRAY;
    public static Color BACKGROUND = Color.BLACK;
    public static Color INVALID_HIGHLIGHT = Color.decode("#e05a4e");
    public static Color VALID_HIGHLIGHT = Color.decode("#6fcf7d");
    public static Color TABLE_HEADER_BACKGROUND = Color.decode("#1a1a1a");
    public static Color BORDER = Color.LIGHT_GRAY;
    public static Color SCROLLBAR_THUMB = Color.GRAY;
    public static Color ACCENT = Color.decode("#eeb392");
    public static Color WINDOW_BORDER = Color.WHITE;

    /**
     * Overwrites all 12 widget colors from a mod-loaded theme. Called once at startup
     * (see {@code Main.loadGame}) with whichever theme owns ID "core:default" — see
     * {@code WidgetColorTheme.REQUIRED_KEYS} for the key set this reads.
     */
    public static void applyTheme(WidgetColorTheme theme) {
        SELECTED_HIGHLIGHT = theme.color("SELECTED_HIGHLIGHT");
        SELECTED_TEXT = theme.color("SELECTED_TEXT");
        NORMAL_TEXT = theme.color("NORMAL_TEXT");
        DIMMED_TEXT = theme.color("DIMMED_TEXT");
        BACKGROUND = theme.color("BACKGROUND");
        INVALID_HIGHLIGHT = theme.color("INVALID_HIGHLIGHT");
        VALID_HIGHLIGHT = theme.color("VALID_HIGHLIGHT");
        TABLE_HEADER_BACKGROUND = theme.color("TABLE_HEADER_BACKGROUND");
        BORDER = theme.color("BORDER");
        SCROLLBAR_THUMB = theme.color("SCROLLBAR_THUMB");
        ACCENT = theme.color("ACCENT");
        WINDOW_BORDER = theme.color("WINDOW_BORDER");
    }

    /**
     * The one place every widget's "selected" look is defined: a filled background, not just
     * recolored text — applied identically by ListWidget, TableWidget, and RadioGroupWidget, so
     * the highlight convention can't quietly diverge between widgets. Requires the label be
     * opaque (set once, at label-creation time) for the background fill to actually paint.
     */
    public static void applySelection(JLabel label, boolean selected) {
        label.setForeground(selected ? SELECTED_TEXT : NORMAL_TEXT);
        label.setBackground(selected ? SELECTED_HIGHLIGHT : BACKGROUND);
    }

    private WidgetTheme() {
    }
}
