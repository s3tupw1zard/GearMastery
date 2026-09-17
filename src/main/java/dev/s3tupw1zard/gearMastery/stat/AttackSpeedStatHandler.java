package dev.s3tupw1zard.gearMastery.stat;

import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;

/** Scales the effective positive player attack speed, not the signed item adjustment. */
public final class AttackSpeedStatHandler implements StatHandler {
    static final double VANILLA_PLAYER_BASE_ATTACK_SPEED = 4.0D;
    @Override public StatType type() { return StatType.ATTACK_SPEED; }
    @Override public void apply(final StatApplicationContext context) {
        final ItemAttributeModifiers existing = context.item().getData(DataComponentTypes.ATTRIBUTE_MODIFIERS); if (existing == null && !context.rule().enabled()) return;
        final java.util.List<ItemAttributeModifiers.Entry> entries = existing == null ? java.util.List.of() : existing.modifiers();
        final GearItemRepository repository = context.repository();
        final var target = AttributeSlotMatcher.targetSlot(context.item()); if (target.isEmpty()) { removeOwned(existing, context.item()); return; }
        final EquipmentSlot slot = target.get(); final NamespacedKey modifierKey = GearModifierKeys.current(StatType.ATTACK_SPEED, slot);
        final var storedBaseline = repository.baselineAttackSpeedSlot(context.item()).filter(slot.name()::equals).flatMap(ignored -> repository.baselineAttackSpeedEffective(context.item()));
        final double effectiveBaseline = storedBaseline.orElseGet(() -> {
            final double captured = effectiveBaseline(entries, modifierKey, GearModifierKeys.legacy(StatType.ATTACK_SPEED), slot); return captured;
        });
        final double bonus = context.rule().enabled() ? AttributeModifierValue.requireFinite(effectiveBonus(effectiveBaseline, context.level(), context.rule()), StatType.ATTACK_SPEED) : 0.0D;
        final ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes();
        entries.stream().filter(entry -> !GearModifierKeys.owns(entry.modifier().getKey(), StatType.ATTACK_SPEED))
            .forEach(entry -> builder.addModifier(entry.attribute(), entry.modifier(), entry.getGroup(), entry.display()));
        if (bonus != 0.0D) builder.addModifier(Attribute.ATTACK_SPEED,
            new AttributeModifier(modifierKey, bonus, AttributeModifier.Operation.ADD_NUMBER, AttributeSlotMatcher.targetGroup(slot)));
        if (storedBaseline.isEmpty()) { repository.baselineAttackSpeedEffective(context.item(), effectiveBaseline); repository.baselineAttackSpeedSlot(context.item(), slot.name()); }
        context.item().setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }
    private static void removeOwned(final ItemAttributeModifiers existing, final org.bukkit.inventory.ItemStack item) { final ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes(); existing.modifiers().stream().filter(entry -> !GearModifierKeys.owns(entry.modifier().getKey(), StatType.ATTACK_SPEED)).forEach(entry -> builder.addModifier(entry.attribute(), entry.modifier(), entry.getGroup(), entry.display())); item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build()); }
    static double effectiveBaseline(final java.util.List<ItemAttributeModifiers.Entry> modifiers, final NamespacedKey ownKey, final NamespacedKey legacyKey, final EquipmentSlot targetSlot) {
        return VANILLA_PLAYER_BASE_ATTACK_SPEED + modifiers.stream().filter(entry -> entry.attribute().equals(Attribute.ATTACK_SPEED))
            .filter(entry -> !GearModifierKeys.owns(entry.modifier().getKey(), StatType.ATTACK_SPEED)).filter(entry -> entry.modifier().getOperation() == AttributeModifier.Operation.ADD_NUMBER)
            .filter(entry -> AttributeSlotMatcher.appliesTo(entry.getGroup(), targetSlot))
            .mapToDouble(entry -> entry.modifier().getAmount()).sum();
    }
    static double effectiveBonus(final double effectiveBaseline, final int level, final StatRule rule) {
        return StatValueCalculator.calculate(effectiveBaseline, level, rule) - effectiveBaseline;
    }
}
