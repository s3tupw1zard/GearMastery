package dev.s3tupw1zard.gearMastery.level;

import dev.s3tupw1zard.gearMastery.api.*;
import dev.s3tupw1zard.gearMastery.config.*;
import dev.s3tupw1zard.gearMastery.data.GearItemData;
import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import dev.s3tupw1zard.gearMastery.stat.*;
import dev.s3tupw1zard.gearMastery.xp.ExperienceContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/** Centralizes initialization, XP mutation, level-up events, and persistence. */
public final class ProgressionService implements GearMasteryApi {
    private final ConfigurationService configurations; private final GearItemRepository repository;
    public ProgressionService(final ConfigurationService configurations, final GearItemRepository repository) { this.configurations = configurations; this.repository = repository; }
    @Override public boolean isLevelable(final ItemStack item) { return configurations.current().profileFor(item.getType()).isPresent(); }
    @Override public Optional<GearItemData> data(final ItemStack item) { return repository.read(item); }
    @Override public Optional<GearItemData> initialize(final ItemStack item) {
        final Optional<GearItemData> existing = repository.read(item); if (existing.isPresent()) return existing;
        return configurations.current().profileFor(item.getType()).map(profile -> { final GearItemData data = new GearItemData(GearItemRepository.CURRENT_SCHEMA, UUID.randomUUID(), profile.id(), 0, 0, 0); repository.write(item, data); return data; });
    }
    @Override public Optional<GearItemData> addExperience(final Player player, final ItemStack item, final long amount, final String sourceId, final Object cause) {
        if (amount <= 0) return repository.read(item);
        final GearConfiguration configuration = configurations.current();
        final Optional<GearItemData> initialized = initialize(item); if (initialized.isEmpty()) return Optional.empty();
        final ItemProfile profile = configuration.findProfile(initialized.get().profileId()).orElse(null);
        if (profile == null || (!sourceId.equals("admin") && !profile.supportsSource(sourceId))) return Optional.empty();
        final GearExperienceGainEvent gain = new GearExperienceGainEvent(new ExperienceContext(player, item, sourceId, cause), amount);
        Bukkit.getPluginManager().callEvent(gain); if (gain.isCancelled() || gain.amount() == 0) return initialized;
        GearItemData state = initialized.get(); long xp = safeAdd(state.experience(), gain.amount()); long lifetime = safeAdd(state.lifetimeExperience(), gain.amount());
        final LevelingCurve curve = configuration.curveFor(profile);
        while (state.level() < configuration.maxLevel() && xp >= curve.experienceForNextLevel(state.level())) {
            xp -= curve.experienceForNextLevel(state.level()); final GearItemData before = state;
            state = new GearItemData(state.schemaVersion(), state.gearId(), state.profileId(), state.level() + 1, xp, lifetime);
            repository.write(item, state); Bukkit.getPluginManager().callEvent(new GearLevelUpEvent(player, item, before, state));
        }
        state = new GearItemData(state.schemaVersion(), state.gearId(), state.profileId(), state.level(), xp, lifetime); repository.write(item, state); return Optional.of(state);
    }
    @Override public Optional<GearItemData> setLevel(final ItemStack item, final int level) {
        if (level < 0 || level > configurations.current().maxLevel()) throw new IllegalArgumentException("Level is outside configured bounds");
        return initialize(item).map(data -> { final GearItemData changed = new GearItemData(data.schemaVersion(), data.gearId(), data.profileId(), level, 0, data.lifetimeExperience()); repository.write(item, changed); return changed; });
    }
    @Override public Optional<Double> statValue(final ItemStack item, final StatType type, final double baseValue) {
        return repository.read(item).flatMap(data -> configurations.current().findProfile(data.profileId()).map(profile -> {
            final StatRule rule = profile.statRules().get(type); return rule == null ? baseValue : StatValueCalculator.calculate(baseValue, data.level(), rule);
        }));
    }
    private static long safeAdd(final long left, final long right) { return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right; }
}
