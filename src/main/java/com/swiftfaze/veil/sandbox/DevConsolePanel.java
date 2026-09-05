package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.PatternFieldWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Top-level dev-console shell: a results table of every registered
 * provider's individual entries, always shown on launch. Typing in the
 * search field (pinned to the bottom, Material-outlined-field styled)
 * filters the table by substring; Enter opens the selected result's detail
 * panel in place of the table; Escape returns to the table.
 */
public class DevConsolePanel extends JPanel {

    private static final String SEARCH_CARD = "search";
    private static final String PROVIDER_CARD = "provider";
    private static final Dimension DEFAULT_SIZE = new Dimension(820, 600);
    private static final String SEARCH_PLACEHOLDER = "Search by name, category, or mod...";
    private static final String TITLE = "Dev Console";

    private final DevConsoleModel model;
    private final CardLayout cardLayout;
    private final JPanel cards;
    private final PatternFieldWidget searchField;
    private final TableWidget<DevConsoleModel.SearchResult> resultsTable;
    private final JPanel providerContainer;

    public DevConsolePanel(DevConsoleModel model) {
        this.model = model;
        this.cardLayout = new CardLayout();
        this.cards = new JPanel(cardLayout);
        this.searchField = new PatternFieldWidget(".*", "Search");
        this.resultsTable = new TableWidget<>(
                List.of("Name", "Category", "Mod"),
                List.of(
                        result -> result.entry().name(),
                        result -> result.entry().category(),
                        result -> result.entry().namespace()
                )
        );
        this.providerContainer = new JPanel(new BorderLayout());
        providerContainer.setBackground(WidgetTheme.BACKGROUND);

        setBackground(WidgetTheme.BACKGROUND);
        setLayout(new BorderLayout());
        setFocusable(false);
        // Border lives on the whole panel (flush against the window's true edge) rather than
        // on providerContainer, so it shows on both the search and detail cards uniformly
        // instead of only around the opened detail view.
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WidgetTheme.WINDOW_BORDER, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        setPreferredSize(DEFAULT_SIZE);

        cards.add(buildSearchView(), SEARCH_CARD);
        cards.add(providerContainer, PROVIDER_CARD);
        add(cards, BorderLayout.CENTER);

        bindProviderBackKey();
        refreshResults();
        showSearchView();
    }

    @Override
    public boolean requestFocusInWindow() {
        return searchField.requestFocusInWindow();
    }

    public JTextField getSearchField() {
        return searchField.getTextField();
    }

    /**
     * Opens the currently selected result's detail panel, or does nothing if there are no
     * results. Public so it can be driven directly both by the Enter keybinding and by tests.
     */
    public void confirmSelection() {
        DevConsoleModel.SearchResult selected = resultsTable.getSelectedRow();
        if (selected != null) {
            showProvider(selected);
        }
    }

    /**
     * Returns from an open detail panel to the top-level results table. Public so it can be
     * driven directly both by the Escape keybinding and by tests.
     */
    public void showSearchView() {
        cardLayout.show(cards, SEARCH_CARD);
        searchField.requestFocusInWindow();
    }

    public boolean isProviderPanelShowing() {
        return providerContainer.isVisible();
    }

    private JPanel buildSearchView() {
        JPanel searchView = new JPanel();
        searchView.setBackground(WidgetTheme.BACKGROUND);
        searchView.setLayout(new BoxLayout(searchView, BoxLayout.Y_AXIS));
        // BoxLayout (not BorderLayout.CENTER) so the table keeps its own natural height instead
        // of being stretched to fill all remaining vertical space - stretching it dragged its
        // own top/left border down through the empty space below the actual rows.
        searchView.add(new HeaderWidget(TITLE));
        searchView.add(resultsTable);
        searchView.add(Box.createVerticalGlue());
        searchView.add(buildSearchField());
        return searchView;
    }

    private PatternFieldWidget buildSearchField() {
        searchField.setAlignmentX(LEFT_ALIGNMENT);
        searchField.setPlaceholder(SEARCH_PLACEHOLDER);
        searchField.setValidityColoringEnabled(false);
        searchField.setOnInputChanged(this::onSearchTextChanged);
        bindSearchFieldKeys();
        return searchField;
    }

    private void onSearchTextChanged(String text) {
        model.setSearchText(text);
        refreshResults();
    }

    private void bindSearchFieldKeys() {
        JTextField textField = searchField.getTextField();
        // Otherwise Tab is consumed as a focus-traversal key and moves focus off the search
        // field instead of reaching it as ordinary input - same pattern as the other
        // keyboard-navigated widgets (see e.g. InventoryPanel/CodexPanel's list components).
        textField.setFocusTraversalKeysEnabled(false);

        InputMap inputMap = textField.getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = textField.getActionMap();

        inputMap.put(Keybindings.MENU_UP, Keybindings.ACTION_MENU_UP);
        inputMap.put(Keybindings.MENU_DOWN, Keybindings.ACTION_MENU_DOWN);
        inputMap.put(Keybindings.MENU_CONFIRM, Keybindings.ACTION_MENU_CONFIRM);

        actionMap.put(Keybindings.ACTION_MENU_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resultsTable.moveUp();
            }
        });

        actionMap.put(Keybindings.ACTION_MENU_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resultsTable.moveDown();
            }
        });

        actionMap.put(Keybindings.ACTION_MENU_CONFIRM, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmSelection();
            }
        });
    }

    private void bindProviderBackKey() {
        InputMap inputMap = providerContainer.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = providerContainer.getActionMap();

        inputMap.put(Keybindings.MENU_CANCEL, Keybindings.ACTION_MENU_CANCEL);
        actionMap.put(Keybindings.ACTION_MENU_CANCEL, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSearchView();
            }
        });
    }

    private void showProvider(DevConsoleModel.SearchResult result) {
        providerContainer.removeAll();
        providerContainer.add(result.provider().createPanel(result.entry().name()), BorderLayout.CENTER);
        cardLayout.show(cards, PROVIDER_CARD);
        providerContainer.revalidate();
        providerContainer.repaint();
        providerContainer.getComponent(0).requestFocusInWindow();
    }

    private void refreshResults() {
        resultsTable.setRows(model.filteredResults());
    }
}
