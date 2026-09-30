package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.mods.ModLoader;

import javax.swing.JComponent;
import java.nio.file.Paths;
import java.util.List;

/**
 * Exposes every mod-loaded item as its own searchable dev-console entry,
 * reusing the Inspectable / DetailTable output already used by InventoryPanel
 * and CodexPanel.
 */
public class ItemSandboxProvider implements DevConsoleProvider {

    private static final String CATEGORY = "Items";

    private final List<Item> items;

    public ItemSandboxProvider() {
        this(ModLoader.load(Paths.get("mods")).getAllItems());
    }

    public ItemSandboxProvider(List<Item> items) {
        this.items = items;
    }

    @Override
    public List<DevConsoleEntry> entries() {
        return items.stream()
                .map(item -> new DevConsoleEntry(
                        namespaceOf(item.getId()),
                        item.getId(),
                        CATEGORY,
                        item.getName()))
                .toList();
    }

    @Override
    public JComponent createPanel(String id) {
        Item item = findItemById(id);
        return new ItemDetailPanel(item);
    }

    private Item findItemById(String id) {
        return items.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown item id: " + id));
    }

    private static String namespaceOf(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(0, colon) : id;
    }
}
