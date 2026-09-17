package dev.s3tupw1zard.gearMastery.stat;

import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Tool;

/** Rebuilds the effective tool component while changing only captured speed values. */
public final class MiningSpeedStatHandler implements StatHandler {
    @Override public StatType type() { return StatType.MINING_SPEED; }
    @Override public void apply(final StatApplicationContext context) {
        final Tool current = context.item().getData(DataComponentTypes.TOOL); if (current == null) return;
        final GearItemRepository repository = context.repository();
        final float defaultSpeed = repository.baselineToolDefaultSpeed(context.item()).orElseGet(() -> { repository.baselineToolDefaultSpeed(context.item(), current.defaultMiningSpeed()); repository.baselineToolRuleCount(context.item(), current.rules().size()); for (int i = 0; i < current.rules().size(); i++) { final Float speed = current.rules().get(i).speed(); if (speed != null) repository.baselineToolRuleSpeed(context.item(), i, speed); } return current.defaultMiningSpeed(); });
        final Tool.Builder builder = Tool.tool().defaultMiningSpeed((float) StatValueCalculator.calculate(defaultSpeed, context.level(), context.rule())).damagePerBlock(current.damagePerBlock()).canDestroyBlocksInCreative(current.canDestroyBlocksInCreative());
        for (int i = 0; i < current.rules().size(); i++) { final Tool.Rule rule = current.rules().get(i); final Float baseline = repository.baselineToolRuleSpeed(context.item(), i).orElse(rule.speed()); final Float scaled = baseline == null ? null : (float) StatValueCalculator.calculate(baseline, context.level(), context.rule()); builder.addRule(Tool.rule(rule.blocks(), scaled, rule.correctForDrops())); }
        context.item().setData(DataComponentTypes.TOOL, builder.build());
    }
}
