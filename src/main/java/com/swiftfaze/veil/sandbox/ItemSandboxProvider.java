package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.mods.ModLoader;

import javax.swing.JComponent;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Exposes every mod-loaded item as its own searchable dev-console entry,
 * reusing the Inspectable / DetailTable output already used by InventoryPanel
 * and CodexPanel. Supports live editing of base damage fields via
 * {@link #fieldMutator(String)}, which swaps in a new immutable Item with the
 * edited damage values into an id-keyed map. The original mod-loaded BaseDamage
 * values are cached separately to support the "default" keyword restoration.
 */
public class ItemSandboxProvider implements DevConsoleProvider {

    private static final String CATEGORY = "Items";

    private final Map<String, Item> itemsById;
    private final Map<String, Item.BaseDamage> originalBaseDamageById;

    public ItemSandboxProvider() {
        this(ModLoader.load(Paths.get("mods")).getAllItems());
    }

    public ItemSandboxProvider(Collection<Item> items) {
        this.itemsById = new LinkedHashMap<>();
        this.originalBaseDamageById = new LinkedHashMap<>();
        for (Item item : items) {
            this.itemsById.put(item.getId(), item);
            this.originalBaseDamageById.put(item.getId(), item.getBaseDamage());
        }
    }

    @Override
    public List<DevConsoleEntry> entries() {
        return itemsById.values().stream()
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

    @Override
    public Optional<DevConsoleFieldMutator> fieldMutator(String id) {
        Item item = findItemById(id);
        Item.BaseDamage originalBaseDamage = originalBaseDamageById.getOrDefault(id, item.getBaseDamage());
        return Optional.of(new ItemFieldMutator(
                id,
                originalBaseDamage,
                this::updateItemDamage,
                lookupId -> findItemById(lookupId)
        ));
    }

    private DevConsoleMutationResult updateItemDamage(ItemFieldMutator.DamageUpdateInfo info) {
        Item current = findItemById(info.itemId());
        Item updated = current.withBaseDamage(info.newMin(), info.newMax());
        itemsById.put(info.itemId(), updated);
        return new DevConsoleMutationResult.Success(info.displayName(), info.newValue());
    }

    private Item findItemById(String id) {
        return itemsById.values().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown item id: " + id));
    }

    private static String namespaceOf(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(0, colon) : id;
    }
}
