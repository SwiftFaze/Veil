package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.DetailsPaneWidget;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * An item's full detail panel via Inspectable, showing field table plus effects
 * when present. Reuses DetailsPaneWidget (shared with InventoryPanel and CodexPanel)
 * and follows ClassDetailPanel's layout shape.
 */
public final class ItemDetailPanel extends JPanel {

    private final String title;
    private final DetailsPaneWidget detailsPane;

    public ItemDetailPanel(Item item) {
        this.title = item.getName();
        this.detailsPane = new DetailsPaneWidget();
        this.detailsPane.showEntry(item);
        this.detailsPane.focusFirstTable();

        setBackground(WidgetTheme.BACKGROUND);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setFocusable(true);

        add(new HeaderWidget(title));
        add(detailsPane);

        bindKeys();
    }

    public String title() {
        return title;
    }

    public int tableCount() {
        return detailsPane.getTableCount();
    }

    public TableWidget<List<String>> table(int index) {
        return detailsPane.getTable(index);
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
