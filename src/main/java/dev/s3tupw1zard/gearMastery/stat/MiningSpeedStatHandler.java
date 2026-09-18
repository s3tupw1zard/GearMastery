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
        final java.util.List<ToolRuleBaseline.Snapshot> prior = snapshots(repository.toolRuleSnapshots(context.item()).orElse(""));
        final java.util.List<ToolRuleBaseline.Snapshot> applied = new java.util.ArrayList<>();
        final java.util.Set<ToolRuleBaseline.Snapshot> consumed = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        final java.util.List<ToolRuleBaseline.Snapshot> exact = reserveExact(current.rules(), prior, consumed);
        final float defaultSpeed = ToolRuleBaseline.select(current.defaultMiningSpeed(), repository.baselineToolDefaultSpeed(context.item()).orElse(null), repository.expectedToolDefaultSpeed(context.item()).orElse(null));
        final float scaledDefault = ToolSpeedValue.requireValid(StatValueCalculator.calculate(defaultSpeed, context.level(), context.rule()), "default mining speed");
        final Tool.Builder builder = Tool.tool().defaultMiningSpeed(scaledDefault).damagePerBlock(current.damagePerBlock()).canDestroyBlocksInCreative(current.canDestroyBlocksInCreative());
        for (int index = 0; index < current.rules().size(); index++) { final Tool.Rule rule = current.rules().get(index);
            final String identity = identity(rule); final ToolRuleBaseline.Snapshot priorRule = exact.get(index);
            final ToolRuleBaseline.Snapshot structural = priorRule == null ? ToolRuleBaseline.structuralMatch(rule.speed(), prior.stream().filter(snapshot -> !consumed.contains(snapshot)).toList()) : null;
            if (structural != null) consumed.add(structural);
            final Float baseline = baseline(rule.speed(), priorRule, structural);
            final Float scaled = baseline == null ? null : ToolSpeedValue.requireValid(StatValueCalculator.calculate(baseline, context.level(), context.rule()), "rule " + identity);
            if (scaled != null) applied.add(new ToolRuleBaseline.Snapshot(identity, baseline, scaled));
            builder.addRule(Tool.rule(rule.blocks(), scaled, rule.correctForDrops()));
        }
        // Commit only after every rule and the rebuilt component have passed validation.
        final Tool rebuilt = builder.build();
        repository.baselineToolDefaultSpeed(context.item(), defaultSpeed);
        repository.expectedToolDefaultSpeed(context.item(), scaledDefault);
        repository.toolRuleSnapshots(context.item(), serialize(applied));
        context.item().setData(DataComponentTypes.TOOL, rebuilt);
    }
    static Float baseline(final Float visible, final ToolRuleBaseline.Snapshot exact, final ToolRuleBaseline.Snapshot structural) {
        if (exact != null) return ToolRuleBaseline.select(visible, exact.baselineSpeed(), exact.expectedSpeed());
        if (structural != null) return Float.valueOf(structural.baselineSpeed());
        return visible;
    }
    static String identity(final Tool.Rule rule) {
        final String blocks = rule.blocks().values().stream().map(key -> key.key().asString()).sorted().collect(java.util.stream.Collectors.joining(","));
        return blocks + '|' + rule.correctForDrops();
    }
    static java.util.List<ToolRuleBaseline.Snapshot> snapshots(final String encoded) {
        final java.util.List<ToolRuleBaseline.Snapshot> result = new java.util.ArrayList<>();
        for (final String entry : encoded.split(";")) { final String[] parts = entry.split("\\|", 3); if (parts.length == 3) try { final String identity = new String(java.util.Base64.getUrlDecoder().decode(parts[0]), java.nio.charset.StandardCharsets.UTF_8); result.add(new ToolRuleBaseline.Snapshot(identity, Float.parseFloat(parts[1]), Float.parseFloat(parts[2]))); } catch (final IllegalArgumentException ignored) { } }
        return result;
    }
    static String serialize(final java.util.List<ToolRuleBaseline.Snapshot> snapshots) { return snapshots.stream().map(snapshot -> java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(snapshot.identity().getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "|" + snapshot.baselineSpeed() + "|" + snapshot.expectedSpeed()).collect(java.util.stream.Collectors.joining(";")); }
    static ToolRuleBaseline.Snapshot match(final String identity, final Float visible, final java.util.List<ToolRuleBaseline.Snapshot> prior, final java.util.Set<ToolRuleBaseline.Snapshot> consumed) {
        final java.util.List<ToolRuleBaseline.Snapshot> exact = prior.stream().filter(snapshot -> !consumed.contains(snapshot) && snapshot.identity().equals(identity)).toList();
        final java.util.List<ToolRuleBaseline.Snapshot> expected = exact.stream().filter(snapshot -> java.util.Objects.equals(snapshot.expectedSpeed(), visible)).toList();
        // Equal snapshots are occurrence-equivalent: consume the first original occurrence deterministically.
        final ToolRuleBaseline.Snapshot match = !expected.isEmpty() ? expected.getFirst() : exact.size() == 1 ? exact.getFirst() : null;
        if (match != null) consumed.add(match);
        return match;
    }
    private static java.util.List<ToolRuleBaseline.Snapshot> reserveExact(final java.util.List<Tool.Rule> rules, final java.util.List<ToolRuleBaseline.Snapshot> prior, final java.util.Set<ToolRuleBaseline.Snapshot> consumed) {
        final java.util.List<ToolRuleBaseline.Snapshot> result = new java.util.ArrayList<>();
        for (final Tool.Rule rule : rules) result.add(match(identity(rule), rule.speed(), prior, consumed));
        return result;
    }
}
