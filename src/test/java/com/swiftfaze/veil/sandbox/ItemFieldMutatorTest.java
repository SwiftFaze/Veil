package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemFieldMutatorTest {

    private static Item ironSword() {
        return new Item(
                "core:iron_sword",
                "Iron Sword",
                new Item.ItemAttributes('/', "weapon", "hand", new Item.BaseDamage(4, 9), List.of())
        );
    }

    private ItemFieldMutator.ItemDamageUpdater createUpdater(Map<String, Item> itemsMap) {
        return info -> {
            Item current = itemsMap.get(info.itemId());
            Item.BaseDamage newDamage = new Item.BaseDamage(info.newMin(), info.newMax());
            Item updated = current.withBaseDamage(newDamage);
            itemsMap.put(info.itemId(), updated);
            return new DevConsoleMutationResult.Success(info.displayName(), info.newValue());
        };
    }

    @Test
    void setChangesMaxDamageAndReturnsSuccess() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, "maxdmg", "12");

        assertTrue(result instanceof DevConsoleMutationResult.Success);
        DevConsoleMutationResult.Success success = (DevConsoleMutationResult.Success) result;
        assertEquals("Base Damage (Max)", success.fieldName());
        assertEquals(12, success.newValue());
        assertEquals(12, itemsMap.get(item.getId()).getBaseDamage().max());
        assertEquals(4, itemsMap.get(item.getId()).getBaseDamage().min());
    }

    @Test
    void setChangesMinDamageAndReturnsSuccess() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, "mindmg", "0");

        assertTrue(result instanceof DevConsoleMutationResult.Success);
        DevConsoleMutationResult.Success success = (DevConsoleMutationResult.Success) result;
        assertEquals("Base Damage (Min)", success.fieldName());
        assertEquals(0, success.newValue());
        assertEquals(0, itemsMap.get(item.getId()).getBaseDamage().min());
        assertEquals(9, itemsMap.get(item.getId()).getBaseDamage().max());
    }

    @Test
    void addIncreasesMaxDamage() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.ADD, "maxdmg", "3");

        assertTrue(result instanceof DevConsoleMutationResult.Success);
        DevConsoleMutationResult.Success success = (DevConsoleMutationResult.Success) result;
        assertEquals(12, success.newValue());
        assertEquals(12, itemsMap.get(item.getId()).getBaseDamage().max());
    }

    @Test
    void subtractDecreasesMinDamage() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SUBTRACT, "mindmg", "1");

        assertTrue(result instanceof DevConsoleMutationResult.Success);
        DevConsoleMutationResult.Success success = (DevConsoleMutationResult.Success) result;
        assertEquals(3, success.newValue());
        assertEquals(3, itemsMap.get(item.getId()).getBaseDamage().min());
    }

    @Test
    void subtractClampsToZero() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SUBTRACT, "mindmg", "50");

        assertTrue(result instanceof DevConsoleMutationResult.Success);
        DevConsoleMutationResult.Success success = (DevConsoleMutationResult.Success) result;
        assertEquals(0, success.newValue());
        assertEquals(0, itemsMap.get(item.getId()).getBaseDamage().min());
    }

    @Test
    void defaultRestoresOriginalMaxDamage() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        // First, set a new value
        mutator.apply(DevConsoleMutationVerb.SET, "maxdmg", "20");
        assertEquals(20, itemsMap.get(item.getId()).getBaseDamage().max());

        // Then, reset to default
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, "maxdmg", "default");

        assertTrue(result instanceof DevConsoleMutationResult.Success);
        DevConsoleMutationResult.Success success = (DevConsoleMutationResult.Success) result;
        assertEquals(9, success.newValue());
        assertEquals(9, itemsMap.get(item.getId()).getBaseDamage().max());
    }

    @Test
    void rejectsUnknownField() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, "glyph", "1");

        assertTrue(result instanceof DevConsoleMutationResult.Failure);
        DevConsoleMutationResult.Failure failure = (DevConsoleMutationResult.Failure) result;
        assertEquals("glyph", failure.token());
    }

    @Test
    void rejectsNonNumericValue() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, "maxdmg", "lots");

        assertTrue(result instanceof DevConsoleMutationResult.Failure);
        DevConsoleMutationResult.Failure failure = (DevConsoleMutationResult.Failure) result;
        assertEquals("lots", failure.token());
    }

    @Test
    void rejectsNegativeAddMagnitude() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.ADD, "maxdmg", "-2");

        assertTrue(result instanceof DevConsoleMutationResult.Failure);
        DevConsoleMutationResult.Failure failure = (DevConsoleMutationResult.Failure) result;
        assertEquals("-2", failure.token());
    }

    @Test
    void rejectsMinGreaterThanMax() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, "mindmg", "10");

        assertTrue(result instanceof DevConsoleMutationResult.Failure);
        DevConsoleMutationResult.Failure failure = (DevConsoleMutationResult.Failure) result;
        assertEquals("10", failure.token());
        // Verify nothing changed
        assertEquals(4, itemsMap.get(item.getId()).getBaseDamage().min());
        assertEquals(9, itemsMap.get(item.getId()).getBaseDamage().max());
    }

    @Test
    void rejectsMaxLessThanMin() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, "maxdmg", "3");

        assertTrue(result instanceof DevConsoleMutationResult.Failure);
        DevConsoleMutationResult.Failure failure = (DevConsoleMutationResult.Failure) result;
        assertEquals("3", failure.token());
        // Verify nothing changed
        assertEquals(4, itemsMap.get(item.getId()).getBaseDamage().min());
        assertEquals(9, itemsMap.get(item.getId()).getBaseDamage().max());
    }

    @Test
    void rejectsDefaultOnAddVerb() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.ADD, "maxdmg", "default");

        assertTrue(result instanceof DevConsoleMutationResult.Failure);
    }

    @Test
    void editsAccumulateAcrossCommands() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        mutator.apply(DevConsoleMutationVerb.SET, "maxdmg", "20");
        assertEquals(20, itemsMap.get(item.getId()).getBaseDamage().max());

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.ADD, "maxdmg", "5");

        assertTrue(result instanceof DevConsoleMutationResult.Success);
        DevConsoleMutationResult.Success success = (DevConsoleMutationResult.Success) result;
        assertEquals(25, success.newValue());
        assertEquals(25, itemsMap.get(item.getId()).getBaseDamage().max());
    }

    @Test
    void fieldTokensIncludesMinAndMaxDamage() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        java.util.List<String> tokens = mutator.fieldTokens();

        assertEquals(2, tokens.size());
        assertTrue(tokens.contains("mindmg"));
        assertTrue(tokens.contains("maxdmg"));
    }

    @Test
    void hasClassDefaultReturnsTrueForBothFields() {
        Item item = ironSword();
        Map<String, Item> itemsMap = new HashMap<>();
        itemsMap.put(item.getId(), item);

        ItemFieldMutator mutator = new ItemFieldMutator(
                item.getId(),
                item.getBaseDamage(),
                createUpdater(itemsMap),
                id -> itemsMap.get(id)
        );

        assertTrue(mutator.hasClassDefault("mindmg"));
        assertTrue(mutator.hasClassDefault("maxdmg"));
    }
}
