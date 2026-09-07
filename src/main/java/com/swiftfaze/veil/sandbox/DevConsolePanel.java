package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.ListDetailLayoutUtility;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.PatternFieldWidget;
import com.swiftfaze.veil.ui.widget.TranscriptWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

/**
 * Top-level dev-console shell: an append-only log transcript of every typed command's output,
 * always shown on launch. Typing `search <term>` and pressing Enter filters entries and
 * prints a numbered result table into the transcript; typing `edit <namespace:id>` and
 * pressing Enter opens that entry's detail panel in place of the transcript. `set`/`add`/
 * `subtract` mutate a field and write a transcript line. Escape returns from a detail panel to
 * the transcript.
 */
public class DevConsolePanel extends JPanel {

    private static final String SEARCH_CARD = "search";
    private static final String PROVIDER_CARD = "provider";
    private static final Dimension DEFAULT_SIZE = new Dimension(820, 600);
    private static final String COMMAND_PLACEHOLDER = "search <term> | edit <namespace:id> | set/add/subtract <entry> <field> <value>";
    private static final String TITLE = "Dev Console";
    // ui-styling.md's "component gap" - the fixed spacing between two sibling components.
    private static final int COMPONENT_GAP_PX = 8;

    private final CardLayout cardLayout;
    private final JPanel cards;
    private final PatternFieldWidget commandField;
    private final TranscriptWidget transcript;
    private final DevConsoleCommandRunner commandRunner;
    private final JPanel providerContainer;

    public DevConsolePanel(DevConsoleModel model) {
        this.cardLayout = new CardLayout();
        this.cards = new JPanel(cardLayout);
        this.commandField = new PatternFieldWidget(".*", "Command");
        this.transcript = new TranscriptWidget();
        this.commandRunner = new DevConsoleCommandRunner(model, transcript, this::showProvider);
        this.providerContainer = new JPanel(new BorderLayout());
        providerContainer.setBackground(WidgetTheme.BACKGROUND);

        setBackground(WidgetTheme.BACKGROUND);
        setLayout(new BorderLayout());
        setFocusable(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WidgetTheme.WINDOW_BORDER, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        setPreferredSize(DEFAULT_SIZE);

        cards.add(buildSearchView(), SEARCH_CARD);
        cards.add(providerContainer, PROVIDER_CARD);
        add(cards, BorderLayout.CENTER);

        bindProviderBackKey();
        showSearchView();
    }

    @Override
    public boolean requestFocusInWindow() {
        return commandField.requestFocusInWindow();
    }

    public JTextField getSearchField() {
        return commandField.getTextField();
    }

    public TranscriptWidget getTranscript() {
        return transcript;
    }

    /**
     * Parses and executes whatever is currently typed in the command field, then clears it -
     * public so it can be driven directly both by the Enter keybinding and by tests.
     */
    public void runCommand() {
        String line = commandField.getInput();
        if (!line.isBlank()) {
            commandRunner.run(line);
        }
        commandField.getTextField().setText("");
    }

    /**
     * Returns from an open detail panel to the top-level transcript view. Public so it can be
     * driven directly both by the Escape keybinding and by tests.
     */
    public void showSearchView() {
        cardLayout.show(cards, SEARCH_CARD);
        commandField.requestFocusInWindow();
    }

    public boolean isProviderPanelShowing() {
        return providerContainer.isVisible();
    }

    /**
     * The currently-opened provider's own panel, or {@code null} if none is open.
     */
    public Component getOpenedProviderPanel() {
        return providerContainer.getComponentCount() > 0 ? providerContainer.getComponent(0) : null;
    }

    private JPanel buildSearchView() {
        JPanel searchView = new JPanel(new BorderLayout());
        searchView.setBackground(WidgetTheme.BACKGROUND);
        searchView.add(new HeaderWidget(TITLE), BorderLayout.NORTH);
        searchView.add(buildTranscriptScrollPane(), BorderLayout.CENTER);
        searchView.add(buildCommandField(), BorderLayout.SOUTH);
        return searchView;
    }

    private JScrollPane buildTranscriptScrollPane() {
        JScrollPane scrollPane = ListDetailLayoutUtility.buildScrollPane(transcript);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(COMPONENT_GAP_PX, 0, COMPONENT_GAP_PX, 0));
        return scrollPane;
    }

    private PatternFieldWidget buildCommandField() {
        commandField.setAlignmentX(LEFT_ALIGNMENT);
        commandField.setPlaceholder(COMMAND_PLACEHOLDER);
        commandField.setValidityColoringEnabled(false);
        bindSearchFieldKeys();
        return commandField;
    }

    private void bindSearchFieldKeys() {
        JTextField textField = commandField.getTextField();
        textField.setFocusTraversalKeysEnabled(false);

        InputMap inputMap = textField.getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = textField.getActionMap();

        inputMap.put(Keybindings.MENU_CONFIRM, Keybindings.ACTION_MENU_CONFIRM);
        actionMap.put(Keybindings.ACTION_MENU_CONFIRM, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runCommand();
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
        providerContainer.add(result.provider().createPanel(result.entry().id()), BorderLayout.CENTER);
        cardLayout.show(cards, PROVIDER_CARD);
        providerContainer.revalidate();
        providerContainer.repaint();
        providerContainer.getComponent(0).requestFocusInWindow();
    }
}
