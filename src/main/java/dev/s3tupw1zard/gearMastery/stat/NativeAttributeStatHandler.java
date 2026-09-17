package dev.s3tupw1zard.gearMastery.stat;

import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

/** Owns exactly one key-based additive modifier while preserving all foreign entries. */
public final class NativeAttributeStatHandler implements StatHandler {
    private final StatType type; private final Attribute attribute;
    public NativeAttributeStatHandler(final StatType type, final Attribute attribute) { this.type = type; this.attribute = attribute; }
    @Override public StatType type() { return type; }
    @Override public void apply(final StatApplicationContext context) {
        final GearItemRepository repository = context.repository();
        final ItemAttributeModifiers existing = context.item().getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (existing == null) return;
        final var targetSlot = AttributeSlotMatcher.targetSlot(context.item());
        if (targetSlot.isEmpty()) { removeOwned(existing, context.item(), type); return; }
        final var slot = targetSlot.get();
        final var modifierKey = GearModifierKeys.current(type, slot);
        final var storedBaseline = repository.baselineSlot(context.item(), type).filter(slot.name()::equals).flatMap(ignored -> repository.baseline(context.item(), type));
        final double baseline = storedBaseline.orElseGet(() -> {
            final double captured = existing.modifiers().stream().filter(entry -> entry.attribute().equals(attribute))
                .filter(entry -> !GearModifierKeys.owns(entry.modifier().getKey(), type))
                .filter(entry -> entry.modifier().getOperation() == AttributeModifier.Operation.ADD_NUMBER)
                .filter(entry -> AttributeSlotMatcher.appliesTo(entry.getGroup(), slot))
                .mapToDouble(entry -> entry.modifier().getAmount()).sum(); return captured;
        });
        final double delta = context.rule().enabled() ? AttributeModifierValue.requireFinite(StatDeltaCalculator.delta(baseline, context.level(), context.rule()), type) : 0.0D;
        final ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes();
        existing.modifiers().stream().filter(entry -> !GearModifierKeys.owns(entry.modifier().getKey(), type))
            .forEach(entry -> builder.addModifier(entry.attribute(), entry.modifier(), entry.getGroup(), entry.display()));
        if (delta != 0.0D) builder.addModifier(attribute, new AttributeModifier(modifierKey, delta, AttributeModifier.Operation.ADD_NUMBER, AttributeSlotMatcher.targetGroup(slot)));
        if (storedBaseline.isEmpty()) { repository.baseline(context.item(), type, baseline); repository.baselineSlot(context.item(), type, slot.name()); }
        context.item().setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }
    private static void removeOwned(final ItemAttributeModifiers existing, final org.bukkit.inventory.ItemStack item, final StatType type) {
        final ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes();
        existing.modifiers().stream().filter(entry -> !GearModifierKeys.owns(entry.modifier().getKey(), type)).forEach(entry -> builder.addModifier(entry.attribute(), entry.modifier(), entry.getGroup(), entry.display()));
        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }
}
