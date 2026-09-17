package dev.s3tupw1zard.gearMastery.stat;

import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;

/** Owns exactly one key-based additive modifier while preserving all foreign entries. */
public final class NativeAttributeStatHandler implements StatHandler {
    private final StatType type; private final Attribute attribute; private final NamespacedKey modifierKey;
    public NativeAttributeStatHandler(final StatType type, final Attribute attribute) { this.type = type; this.attribute = attribute; this.modifierKey = new NamespacedKey("gearmastery", type.name().toLowerCase(java.util.Locale.ROOT)); }
    @Override public StatType type() { return type; }
    @Override public void apply(final StatApplicationContext context) {
        final GearItemRepository repository = context.repository();
        final ItemAttributeModifiers existing = context.item().getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (existing == null) return;
        final double baseline = repository.baseline(context.item(), type).orElseGet(() -> {
            final double captured = existing.modifiers().stream().filter(entry -> entry.attribute().equals(attribute))
                .filter(entry -> !entry.modifier().getKey().equals(modifierKey))
                .filter(entry -> entry.modifier().getOperation() == AttributeModifier.Operation.ADD_NUMBER)
                .mapToDouble(entry -> entry.modifier().getAmount()).sum();
            repository.baseline(context.item(), type, captured); return captured;
        });
        final ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes();
        existing.modifiers().stream().filter(entry -> !entry.modifier().getKey().equals(modifierKey))
            .forEach(entry -> builder.addModifier(entry.attribute(), entry.modifier(), entry.getGroup(), entry.display()));
        if (context.rule().enabled()) {
            final double delta = StatDeltaCalculator.delta(baseline, context.level(), context.rule());
            if (delta != 0.0D) builder.addModifier(attribute, new AttributeModifier(modifierKey, delta, AttributeModifier.Operation.ADD_NUMBER, slotGroup(context.item().getType().name())));
        }
        context.item().setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }
    private static EquipmentSlotGroup slotGroup(final String materialName) {
        if (materialName.endsWith("_HELMET")) return EquipmentSlotGroup.HEAD;
        if (materialName.endsWith("_CHESTPLATE")) return EquipmentSlotGroup.CHEST;
        if (materialName.endsWith("_LEGGINGS")) return EquipmentSlotGroup.LEGS;
        if (materialName.endsWith("_BOOTS")) return EquipmentSlotGroup.FEET;
        return EquipmentSlotGroup.MAINHAND;
    }
}
