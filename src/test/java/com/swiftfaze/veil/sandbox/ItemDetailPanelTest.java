package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.ui.widget.TableWidget;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ItemDetailPanelTest {

    @Test
    void displaysItemNameInHeader() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        Item ironSword = mods.getItem("core:iron_sword");

        ItemDetailPanel panel = new ItemDetailPanel(ironSword);

        assertEquals("Iron Sword", panel.getHeader().getTitle());
    }

    @Test
    void hasAccessibleDetailsPane() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        Item ironSword = mods.getItem("core:iron_sword");

        ItemDetailPanel panel = new ItemDetailPanel(ironSword);

        assertNotNull(panel.getDetailsPane());
    }

    @Test
    void menuDownNavigatesToNextRow() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        Item ironSword = mods.getItem("core:iron_sword");
        ItemDetailPanel panel = new ItemDetailPanel(ironSword);

        TableWidget<List<String>> table = panel.getDetailsPane().getTable(0);
        assertNotNull(table, "First table should exist");

        table.moveToStart();
        List<String> firstRow = table.getSelectedRow();
        assertNotNull(firstRow, "First row should exist");

        Action downAction = panel.getActionMap().get(Keybindings.ACTION_MENU_DOWN);
        assertNotNull(downAction, "Down action should be bound");
        downAction.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, null));

        List<String> secondRow = table.getSelectedRow();
        assertNotNull(secondRow, "Second row should exist after moving down");
        assertNotEquals(firstRow, secondRow, "Row should have changed after pressing down");
    }
}
