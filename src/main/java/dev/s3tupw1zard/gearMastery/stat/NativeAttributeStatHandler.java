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
        final var targetSlot = AttributeSlotMatcher.targetSlot(context.item().getType().name());
        final var modifierKey = GearModifierKeys.current(type, targetSlot); final var legacyKey = GearModifierKeys.legacy(type);
        final var storedBaseline = repository.baseline(context.item(), type);
        final double baseline = storedBaseline.orElseGet(() -> {
            final double captured = existing.modifiers().stream().filter(entry -> entry.attribute().equals(attribute))
                .filter(entry -> !entry.modifier().getKey().equals(modifierKey) && !entry.modifier().getKey().equals(legacyKey))
                .filter(entry -> entry.modifier().getOperation() == AttributeModifier.Operation.ADD_NUMBER)
                .filter(entry -> AttributeSlotMatcher.appliesTo(entry.getGroup(), targetSlot))
                .mapToDouble(entry -> entry.modifier().getAmount()).sum(); return captured;
        });
        final double delta = context.rule().enabled() ? AttributeModifierValue.requireFinite(StatDeltaCalculator.delta(baseline, context.level(), context.rule()), type) : 0.0D;
        final ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes();
        existing.modifiers().stream().filter(entry -> !entry.modifier().getKey().equals(modifierKey) && !entry.modifier().getKey().equals(legacyKey))
            .forEach(entry -> builder.addModifier(entry.attribute(), entry.modifier(), entry.getGroup(), entry.display()));
        if (delta != 0.0D) builder.addModifier(attribute, new AttributeModifier(modifierKey, delta, AttributeModifier.Operation.ADD_NUMBER, AttributeSlotMatcher.targetGroup(context.item().getType().name())));
        if (storedBaseline.isEmpty()) repository.baseline(context.item(), type, baseline);
        context.item().setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }
}
