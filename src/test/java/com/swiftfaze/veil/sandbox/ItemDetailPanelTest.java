package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.ui.widget.TableWidget;
import org.junit.jupiter.api.Test;

import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDetailPanelTest {

    @Test
    void displaysItemNameInHeader() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        Item ironSword = mods.getItem("core:iron_sword");

        ItemDetailPanel panel = new ItemDetailPanel(ironSword);

        assertEquals("Iron Sword", panel.title());
    }

    @Test
    void exposesAtLeastOneDetailTable() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        Item ironSword = mods.getItem("core:iron_sword");

        ItemDetailPanel panel = new ItemDetailPanel(ironSword);

        assertTrue(panel.tableCount() >= 1);
    }

    @Test
    void menuDownNavigatesToNextRow() {
        ItemDetailPanel panel = ironSwordPanel();
        TableWidget<List<String>> table = panel.table(0);
        table.moveToStart();
        List<String> firstRow = table.getSelectedRow();

        press(panel, Keybindings.ACTION_MENU_DOWN);

        assertNotEquals(firstRow, table.getSelectedRow(), "Row should have changed after pressing down");
    }

    @Test
    void menuUpReturnsToThePreviousRow() {
        ItemDetailPanel panel = ironSwordPanel();
        TableWidget<List<String>> table = panel.table(0);
        table.moveToStart();
        List<String> firstRow = table.getSelectedRow();
        press(panel, Keybindings.ACTION_MENU_DOWN);

        press(panel, Keybindings.ACTION_MENU_UP);

        assertEquals(firstRow, table.getSelectedRow());
    }

    @Test
    void mapsMenuKeysToTheNavigationActionsWhenFocused() {
        ItemDetailPanel panel = ironSwordPanel();

        InputMap inputMap = panel.getInputMap(JComponent.WHEN_FOCUSED);

        assertEquals(Keybindings.ACTION_MENU_UP, inputMap.get(Keybindings.MENU_UP));
        assertEquals(Keybindings.ACTION_MENU_DOWN, inputMap.get(Keybindings.MENU_DOWN));
    }

    @Test
    void isFocusableSoKeyboardNavigationCanReachIt() {
        assertTrue(ironSwordPanel().isFocusable());
    }

    @Test
    void stacksHeaderAndDetailsVertically() {
        assertInstanceOf(BoxLayout.class, ironSwordPanel().getLayout());
    }

    @Test
    void usesTheThemeBackground() {
        assertEquals(WidgetTheme.BACKGROUND, ironSwordPanel().getBackground());
    }

    private static void press(ItemDetailPanel panel, String actionName) {
        panel.getActionMap().get(actionName)
                .actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, null));
    }

    private static ItemDetailPanel ironSwordPanel() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        return new ItemDetailPanel(mods.getItem("core:iron_sword"));
    }
}
