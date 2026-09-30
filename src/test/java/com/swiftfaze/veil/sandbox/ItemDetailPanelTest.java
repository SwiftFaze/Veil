package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
