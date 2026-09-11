package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.component.Inspectable;
import com.swiftfaze.veil.ui.widget.TableWidget;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * A shared details-pane widget that renders however many DetailTables an
 * Inspectable returns and routes Up/Down/Left/Right keyboard focus across them.
 * Replaces the hand-duplicated focus-routing logic in InventoryPanel and CodexPanel.
 * Package placement: com.swiftfaze.veil.ui (not .widget) — see ModuleDependencyTest
 * for ArchUnit constraints.
 */
public class DetailsPaneWidget extends JPanel {

    private final JPanel detailsPanel;
    private final JScrollPane detailsScrollPane;
    private final List<TableWidget<List<String>>> tables = new ArrayList<>();
    private int focusedIndex = -1;

    public DetailsPaneWidget() {
        detailsPanel = ListDetailLayoutUtility.buildDetailsPanel();
        detailsScrollPane = ListDetailLayoutUtility.buildScrollPane(detailsPanel);
        setOpaque(false);
        setLayout(new java.awt.BorderLayout());
        add(detailsScrollPane, java.awt.BorderLayout.CENTER);
        setFocusTraversalKeysEnabled(false);
    }

    public void showEntry(Inspectable entry) {
        detailsPanel.removeAll();
        tables.clear();
        focusedIndex = -1;

        if (entry == null) {
            detailsPanel.add(buildPlaceholderLabel("(no item selected)"));
        } else {
            addDetailTables(entry.getDetailTables());
        }

        detailsPanel.revalidate();
        detailsPanel.repaint();
        detailsScrollPane.getViewport().setViewPosition(new Point(0, 0));
    }

    @Override
    public boolean hasFocus() {
        return focusedIndex >= 0;
    }

    public void focusFirstTable() {
        if (tables.isEmpty()) {
            return;
        }
        focusedIndex = 0;
        applyHighlight();
        tables.get(0).moveToStart();
    }

    public void clearFocus() {
        focusedIndex = -1;
        applyHighlight();
    }

    public void moveUp() {
        if (!hasFocus()) {
            return;
        }
        if (tables.get(focusedIndex).isAtFirstRow() && focusedIndex > 0) {
            focusedIndex--;
            applyHighlight();
            tables.get(focusedIndex).moveToEnd();
        } else {
            tables.get(focusedIndex).moveUp();
        }
    }

    public void moveDown() {
        if (!hasFocus()) {
            return;
        }
        if (tables.get(focusedIndex).isAtLastRow() && focusedIndex < tables.size() - 1) {
            focusedIndex++;
            applyHighlight();
            tables.get(focusedIndex).moveToStart();
        } else {
            tables.get(focusedIndex).moveDown();
        }
    }

    public boolean isTableFocused(int index) {
        return focusedIndex == index;
    }

    public TableWidget<List<String>> getTable(int index) {
        return index < 0 || index >= tables.size() ? null : tables.get(index);
    }

    public int getTableCount() {
        return tables.size();
    }

    public boolean isShowingPlaceholder() {
        return tables.isEmpty() && detailsPanel.getComponentCount() > 0;
    }

    private void addDetailTables(List<DetailTable> detailTables) {
        for (DetailTable detail : detailTables) {
            if (!detail.label().isBlank()) {
                detailsPanel.add(ListDetailLayoutUtility.makeSectionLabel(detail.label()));
            }
            TableWidget<List<String>> table = TableWidget.ofRows(detail.columnHeaders(), detail.rows());
            ListDetailLayoutUtility.configureDetailsTable(table);
            table.setFocusTraversalKeysEnabled(false);
            tables.add(table);
            detailsPanel.add(table);
        }
    }

    private void applyHighlight() {
        for (int i = 0; i < tables.size(); i++) {
            tables.get(i).setSelectable(i == focusedIndex);
        }
    }

    private JLabel buildPlaceholderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(com.swiftfaze.veil.ui.widget.WidgetTheme.NORMAL_TEXT);
        label.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 16));
        label.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        return label;
    }
}
