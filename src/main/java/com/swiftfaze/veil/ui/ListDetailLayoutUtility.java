package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.TerminalScrollBarUI;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;

/**
 * Shared utilities for list/detail split-pane layouts (Codex, Inventory).
 * Extracts common UI building patterns to eliminate duplication between panels
 * implementing the same list-detail contract.
 */
public final class ListDetailLayoutUtility {
    private ListDetailLayoutUtility() {}

    /**
     * Builds a scrollpane with terminal-like scrollbar and transparent viewport.
     */
    public static JScrollPane buildScrollPane(JComponent view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUI(new TerminalScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        return scrollPane;
    }

    /**
     * Builds a two-column body pane (list on left, detail on right).
     * Reserves unbounded height so the pane expands to fill available vertical space.
     */
    public static JPanel buildBody(JComponent left, JComponent right) {
        JPanel body = new JPanel(new GridLayout(1, 2, 20, 0));
        body.setBackground(WidgetTheme.BACKGROUND);
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        body.add(left);
        body.add(right);
        return body;
    }

    /**
     * Builds a styled section label with the given text.
     */
    public static JLabel makeSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(WidgetTheme.NORMAL_TEXT);
        label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        return label;
    }

    /**
     * Builds the styled details panel with standard borders and layout (shared by Codex and Inventory).
     */
    public static JPanel buildDetailsPanel() {
        Border detailsDivider = BorderFactory.createMatteBorder(0, 2, 0, 0, WidgetTheme.BORDER);
        return buildDetailsPanel(BorderFactory.createCompoundBorder(detailsDivider, detailsPadding()));
    }

    /**
     * Builds a details panel with the same padding but no list divider, for a details pane
     * shown on its own (the dev-console sandbox panels) where there is no list to divide it from.
     */
    public static JPanel buildStandaloneDetailsPanel() {
        return buildDetailsPanel(detailsPadding());
    }

    private static Border detailsPadding() {
        return BorderFactory.createEmptyBorder(4, 10, 0, 0);
    }

    private static JPanel buildDetailsPanel(Border border) {
        JPanel detailsPanel = new JPanel();
        detailsPanel.setBackground(WidgetTheme.BACKGROUND);
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(border);
        return detailsPanel;
    }

    /**
     * Configures a table widget with standard detail-pane styling (non-wrapping, non-selectable,
     * full width). Height is capped at the table's own rows: left uncapped, BoxLayout stretches
     * the table into the pane's spare height and its left border runs on below the last row.
     */
    public static <T> void configureDetailsTable(TableWidget<T> table) {
        table.setWrapAround(false);
        table.setSelectable(false);
        table.setAlignmentX(Component.LEFT_ALIGNMENT);
        table.setMaximumSize(new Dimension(Integer.MAX_VALUE, table.getPreferredSize().height));
    }
}
