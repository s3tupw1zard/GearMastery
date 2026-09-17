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
        final float defaultSpeed = ToolRuleBaseline.select(current.defaultMiningSpeed(), repository.baselineToolDefaultSpeed(context.item()).orElse(null), repository.expectedToolDefaultSpeed(context.item()).orElse(null));
        repository.baselineToolDefaultSpeed(context.item(), defaultSpeed);
        final float scaledDefault = (float) StatValueCalculator.calculate(defaultSpeed, context.level(), context.rule());
        repository.expectedToolDefaultSpeed(context.item(), scaledDefault);
        final Tool.Builder builder = Tool.tool().defaultMiningSpeed(scaledDefault).damagePerBlock(current.damagePerBlock()).canDestroyBlocksInCreative(current.canDestroyBlocksInCreative());
        for (final Tool.Rule rule : current.rules()) {
            final String identity = identity(rule); final Float baseline = ToolRuleBaseline.select(rule.speed(), repository.baselineToolRuleSpeed(context.item(), identity).orElse(null), repository.expectedToolRuleSpeed(context.item(), identity).orElse(null));
            if (baseline != null) repository.baselineToolRuleSpeed(context.item(), identity, baseline);
            final Float scaled = baseline == null ? null : (float) StatValueCalculator.calculate(baseline, context.level(), context.rule());
            if (scaled != null) repository.expectedToolRuleSpeed(context.item(), identity, scaled);
            builder.addRule(Tool.rule(rule.blocks(), scaled, rule.correctForDrops()));
        }
        context.item().setData(DataComponentTypes.TOOL, builder.build());
    }
    static String identity(final Tool.Rule rule) {
        final String blocks = rule.blocks().values().stream().map(key -> key.key().asString()).sorted().collect(java.util.stream.Collectors.joining(","));
        return blocks + '|' + rule.correctForDrops();
    }
}
