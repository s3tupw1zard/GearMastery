package dev.s3tupw1zard.gearMastery.stat;

import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import io.papermc.paper.datacomponent.DataComponentTypes;

/** Changes max damage natively while retaining the current relative remaining durability. */
public final class DurabilityStatHandler implements StatHandler {
    @Override public StatType type() { return StatType.DURABILITY; }
    @Override public void apply(final StatApplicationContext context) {
        final Integer currentMaximum = context.item().getData(DataComponentTypes.MAX_DAMAGE);
        if (currentMaximum == null || currentMaximum <= 0) return;
        final GearItemRepository repository = context.repository();
        final int baseline = repository.baselineMaxDamage(context.item()).orElseGet(() -> { repository.baselineMaxDamage(context.item(), currentMaximum); return currentMaximum; });
        final int newMaximum = DurabilityValue.toMaxDamage(StatValueCalculator.calculate(baseline, context.level(), context.rule()));
        final int currentDamage = context.item().getDataOrDefault(DataComponentTypes.DAMAGE, 0);
        context.item().setData(DataComponentTypes.MAX_DAMAGE, newMaximum);
        context.item().setData(DataComponentTypes.DAMAGE, DurabilityRescaler.rescaleDamage(currentMaximum, currentDamage, newMaximum));
    }
}
