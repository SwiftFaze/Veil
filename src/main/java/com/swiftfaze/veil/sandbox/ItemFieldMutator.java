package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

/**
 * Applies the dev-console command bar's set/add/subtract verbs to an item's base damage fields,
 * reusing the same field floors/clamping rules as {@link PlayerFieldMutator}. The mutator holds
 * references to the item ID, the original mod-loaded BaseDamage (for `default` restoration), and
 * a function to replace the item with new damage values (delegated to the provider to avoid
 * instantiating engine entities from the sandbox package).
 */
public class ItemFieldMutator implements DevConsoleFieldMutator {

    private static final String DEFAULT_VALUE_TOKEN = "default";

    private final String itemId;
    private final Item.BaseDamage originalBaseDamage;
    private final ItemDamageUpdater damageUpdater;
    private final Function<String, Item> itemFinder;

    /**
     * Information about a damage update operation.
     */
    public record DamageUpdateInfo(String itemId, int newMin, int newMax, String displayName, int newValue) {
    }

    /**
     * Callback to update an item's damage and handle the replacement.
     * The provider is responsible for constructing BaseDamage and the updated Item.
     */
    public interface ItemDamageUpdater {
        DevConsoleMutationResult updateDamage(DamageUpdateInfo info);
    }

    public ItemFieldMutator(String itemId, Item.BaseDamage originalBaseDamage,
                            ItemDamageUpdater damageUpdater, Function<String, Item> itemFinder) {
        this.itemId = itemId;
        this.originalBaseDamage = originalBaseDamage;
        this.damageUpdater = damageUpdater;
        this.itemFinder = itemFinder;
    }

    @Override
    public DevConsoleMutationResult apply(DevConsoleMutationVerb verb, String fieldToken, String rawValue) {
        Optional<ItemField> field = ItemField.fromToken(fieldToken);
        if (field.isEmpty()) {
            return new DevConsoleMutationResult.Failure(fieldToken);
        }

        Item currentItem = itemFinder.apply(itemId);
        Item.BaseDamage currentDamage = currentItem.getBaseDamage();
        OptionalInt resolvedValue = resolveValue(field.get(), verb, rawValue, currentDamage);
        if (resolvedValue.isEmpty()) {
            return new DevConsoleMutationResult.Failure(rawValue);
        }

        int newValue = applyClamped(field.get(), currentDamage, resolvedValue.getAsInt());
        int newMin = field.get() == ItemField.MIN_DAMAGE ? newValue : currentDamage.min();
        int newMax = field.get() == ItemField.MAX_DAMAGE ? newValue : currentDamage.max();

        // Validate that min is not > max after the update
        if (newMin > newMax) {
            return new DevConsoleMutationResult.Failure(rawValue);
        }

        // Tell the provider to replace the item
        DamageUpdateInfo info = new DamageUpdateInfo(itemId, newMin, newMax, field.get().displayName(), newValue);
        return damageUpdater.updateDamage(info);
    }

    private OptionalInt resolveValue(ItemField field, DevConsoleMutationVerb verb, String rawValue, Item.BaseDamage currentDamage) {
        if (DEFAULT_VALUE_TOKEN.equals(rawValue)) {
            return verb == DevConsoleMutationVerb.SET ? defaultValue(field) : OptionalInt.empty();
        }
        OptionalInt parsed = parseInt(rawValue);
        if (parsed.isEmpty() || (verb != DevConsoleMutationVerb.SET && parsed.getAsInt() < 0)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(combine(field, verb, parsed.getAsInt(), currentDamage));
    }

    private int combine(ItemField field, DevConsoleMutationVerb verb, int value, Item.BaseDamage currentDamage) {
        int current = switch (field) {
            case MIN_DAMAGE -> currentDamage.min();
            case MAX_DAMAGE -> currentDamage.max();
        };
        return switch (verb) {
            case SET -> value;
            case ADD -> current + value;
            case SUBTRACT -> current - value;
        };
    }

    private OptionalInt defaultValue(ItemField field) {
        if (!field.hasClassDefault()) {
            return OptionalInt.empty();
        }
        int value = switch (field) {
            case MIN_DAMAGE -> originalBaseDamage.min();
            case MAX_DAMAGE -> originalBaseDamage.max();
        };
        return OptionalInt.of(value);
    }

    private int applyClamped(ItemField field, Item.BaseDamage unused, int rawNewValue) {
        return Math.max(field.floor(), rawNewValue);
    }

    private OptionalInt parseInt(String rawValue) {
        try {
            return OptionalInt.of(Integer.parseInt(rawValue));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    @Override
    public List<String> fieldTokens() {
        return List.of(ItemField.MIN_DAMAGE.token(), ItemField.MAX_DAMAGE.token());
    }

    @Override
    public boolean hasClassDefault(String fieldToken) {
        return ItemField.fromToken(fieldToken).map(ItemField::hasClassDefault).orElse(false);
    }
}
