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
        final java.util.Map<String, ToolRuleBaseline.Snapshot> prior = snapshots(repository.toolRuleSnapshots(context.item()).orElse(""));
        final java.util.List<ToolRuleBaseline.Snapshot> applied = new java.util.ArrayList<>();
        final float defaultSpeed = ToolRuleBaseline.select(current.defaultMiningSpeed(), repository.baselineToolDefaultSpeed(context.item()).orElse(null), repository.expectedToolDefaultSpeed(context.item()).orElse(null));
        repository.baselineToolDefaultSpeed(context.item(), defaultSpeed);
        final float scaledDefault = (float) StatValueCalculator.calculate(defaultSpeed, context.level(), context.rule());
        repository.expectedToolDefaultSpeed(context.item(), scaledDefault);
        final Tool.Builder builder = Tool.tool().defaultMiningSpeed(scaledDefault).damagePerBlock(current.damagePerBlock()).canDestroyBlocksInCreative(current.canDestroyBlocksInCreative());
        for (final Tool.Rule rule : current.rules()) {
            final String identity = identity(rule); final ToolRuleBaseline.Snapshot priorRule = prior.get(identity);
            final Float baseline = priorRule != null ? ToolRuleBaseline.select(rule.speed(), priorRule.baselineSpeed(), priorRule.expectedSpeed()) : ToolRuleBaseline.selectStructural(rule.speed(), prior.values().stream().toList());
            if (baseline != null) repository.baselineToolRuleSpeed(context.item(), identity, baseline);
            final Float scaled = baseline == null ? null : (float) StatValueCalculator.calculate(baseline, context.level(), context.rule());
            if (scaled != null) { repository.expectedToolRuleSpeed(context.item(), identity, scaled); applied.add(new ToolRuleBaseline.Snapshot(identity, baseline, scaled)); }
            builder.addRule(Tool.rule(rule.blocks(), scaled, rule.correctForDrops()));
        }
        repository.toolRuleSnapshots(context.item(), serialize(applied));
        context.item().setData(DataComponentTypes.TOOL, builder.build());
    }
    static String identity(final Tool.Rule rule) {
        final String blocks = rule.blocks().values().stream().map(key -> key.key().asString()).sorted().collect(java.util.stream.Collectors.joining(","));
        return blocks + '|' + rule.correctForDrops();
    }
    private static java.util.Map<String, ToolRuleBaseline.Snapshot> snapshots(final String encoded) {
        final java.util.Map<String, ToolRuleBaseline.Snapshot> result = new java.util.HashMap<>();
        for (final String entry : encoded.split(";")) { final String[] parts = entry.split("\\|", 3); if (parts.length == 3) try { final String identity = new String(java.util.Base64.getUrlDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8); result.put(identity, new ToolRuleBaseline.Snapshot(identity, Float.parseFloat(parts[1]), Float.parseFloat(parts[2]))); } catch (final IllegalArgumentException ignored) { } }
        return result;
    }
    private static String serialize(final java.util.List<ToolRuleBaseline.Snapshot> snapshots) { return snapshots.stream().map(snapshot -> java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(snapshot.identity().getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "|" + snapshot.baselineSpeed() + "|" + snapshot.expectedSpeed()).collect(java.util.stream.Collectors.joining(";")); }
}
