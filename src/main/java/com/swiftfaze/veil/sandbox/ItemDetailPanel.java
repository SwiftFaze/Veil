package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.DetailsPaneWidget;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;

/**
 * An item's full detail panel via Inspectable, showing field table plus effects
 * when present. Reuses DetailsPaneWidget (shared with InventoryPanel and CodexPanel)
 * and follows ClassDetailPanel's layout shape.
 */
public class ItemDetailPanel extends JPanel {

    private final HeaderWidget header;
    private final DetailsPaneWidget detailsPane;

    public ItemDetailPanel(Item item) {
        this.header = new HeaderWidget(item.getName());
        this.detailsPane = new DetailsPaneWidget();
        this.detailsPane.showEntry(item);
        this.detailsPane.focusFirstTable();

        setBackground(WidgetTheme.BACKGROUND);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setFocusable(true);

        add(header);
        add(detailsPane);

        bindKeys();
    }

    public HeaderWidget getHeader() {
        return header;
    }

    public DetailsPaneWidget getDetailsPane() {
        return detailsPane;
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MENU_UP, Keybindings.ACTION_MENU_UP);
        inputMap.put(Keybindings.MENU_DOWN, Keybindings.ACTION_MENU_DOWN);

        actionMap.put(Keybindings.ACTION_MENU_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                detailsPane.moveUp();
            }
        });

        actionMap.put(Keybindings.ACTION_MENU_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                detailsPane.moveDown();
            }
        });
    }
}
