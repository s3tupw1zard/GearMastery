package dev.s3tupw1zard.gearMastery.stat;

import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.EquipmentSlot;

/** Scales the effective positive player attack speed, not the signed item adjustment. */
public final class AttackSpeedStatHandler implements StatHandler {
    static final double VANILLA_PLAYER_BASE_ATTACK_SPEED = 4.0D;
    private final NamespacedKey modifierKey = new NamespacedKey("gearmastery", "attack_speed");
    @Override public StatType type() { return StatType.ATTACK_SPEED; }
    @Override public void apply(final StatApplicationContext context) {
        final ItemAttributeModifiers existing = context.item().getData(DataComponentTypes.ATTRIBUTE_MODIFIERS); if (existing == null) return;
        final GearItemRepository repository = context.repository();
        final double effectiveBaseline = repository.baselineAttackSpeedEffective(context.item()).orElseGet(() -> {
            final double captured = effectiveBaseline(existing, modifierKey, EquipmentSlot.HAND); repository.baselineAttackSpeedEffective(context.item(), captured); return captured;
        });
        final ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes();
        existing.modifiers().stream().filter(entry -> !entry.modifier().getKey().equals(modifierKey))
            .forEach(entry -> builder.addModifier(entry.attribute(), entry.modifier(), entry.getGroup(), entry.display()));
        if (context.rule().enabled()) {
            final double bonus = effectiveBonus(effectiveBaseline, context.level(), context.rule());
            if (bonus != 0.0D) builder.addModifier(Attribute.ATTACK_SPEED,
                new AttributeModifier(modifierKey, bonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        }
        context.item().setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }
    static double effectiveBaseline(final ItemAttributeModifiers modifiers, final NamespacedKey ownKey, final EquipmentSlot targetSlot) {
        return VANILLA_PLAYER_BASE_ATTACK_SPEED + modifiers.modifiers().stream().filter(entry -> entry.attribute().equals(Attribute.ATTACK_SPEED))
            .filter(entry -> !entry.modifier().getKey().equals(ownKey)).filter(entry -> entry.modifier().getOperation() == AttributeModifier.Operation.ADD_NUMBER)
            .filter(entry -> AttributeSlotMatcher.appliesTo(entry.getGroup(), targetSlot))
            .mapToDouble(entry -> entry.modifier().getAmount()).sum();
    }
    static double effectiveBonus(final double effectiveBaseline, final int level, final StatRule rule) {
        return StatValueCalculator.calculate(effectiveBaseline, level, rule) - effectiveBaseline;
    }
}
