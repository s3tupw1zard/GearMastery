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
    private final ConfigurationService configurations; private final GearItemRepository repository; private final StatApplicationService stats;
    public ProgressionService(final ConfigurationService configurations, final GearItemRepository repository, final StatApplicationService stats) { this.configurations = configurations; this.repository = repository; this.stats = stats; }
    @Override public boolean isLevelable(final ItemStack item) { return configurations.current().profileFor(item.getType()).isPresent(); }
    @Override public Optional<GearItemData> data(final ItemStack item) { return repository.read(item); }
    @Override public Optional<GearItemData> initialize(final ItemStack item) {
        final Optional<GearItemData> existing = repository.read(item); if (existing.isPresent()) return existing;
        return configurations.current().profileFor(item.getType()).map(profile -> { final GearItemData data = newItemData(profile); repository.write(item, data); stats.synchronizeIfNeeded(item, data); return data; });
    }
    @Override public Optional<GearItemData> addExperience(final Player player, final ItemStack item, final long amount, final String sourceId, final Object cause) {
        if (amount <= 0) return repository.read(item);
        final GearConfiguration configuration = configurations.current();
        final Optional<ExperienceAdmission> admission = ExperienceAdmission.resolve(configuration, repository.read(item), item.getType(), sourceId);
        if (admission.isEmpty()) return Optional.empty();
        final GearExperienceGainEvent gain = new GearExperienceGainEvent(new ExperienceContext(player, item, sourceId, cause), amount);
        Bukkit.getPluginManager().callEvent(gain); if (!ExperienceAdmission.shouldMutate(gain.isCancelled(), gain.amount())) return admission.get().existingData();
        GearItemData state = admission.get().existingData().orElseGet(() -> newItemData(admission.get().profile()));
        if (admission.get().existingData().isEmpty()) repository.write(item, state);
        long xp = safeAdd(state.experience(), gain.amount()); long lifetime = safeAdd(state.lifetimeExperience(), gain.amount());
        final LevelingCurve curve = configuration.curveFor(admission.get().profile());
        while (state.level() < configuration.maxLevel() && xp >= curve.experienceForNextLevel(state.level())) {
            xp -= curve.experienceForNextLevel(state.level()); final GearItemData before = state;
            state = new GearItemData(state.schemaVersion(), state.gearId(), state.profileId(), state.level() + 1, xp, lifetime);
            repository.write(item, state); Bukkit.getPluginManager().callEvent(new GearLevelUpEvent(player, item, before, state));
        }
        state = new GearItemData(state.schemaVersion(), state.gearId(), state.profileId(), state.level(), xp, lifetime); repository.write(item, state); stats.synchronizeIfNeeded(item, state); return Optional.of(state);
    }
    @Override public Optional<GearItemData> setLevel(final ItemStack item, final int level) {
        if (level < 0 || level > configurations.current().maxLevel()) throw new IllegalArgumentException("Level is outside configured bounds");
        return initialize(item).map(data -> { final GearItemData changed = new GearItemData(data.schemaVersion(), data.gearId(), data.profileId(), level, 0, data.lifetimeExperience()); repository.write(item, changed); stats.synchronizeIfNeeded(item, changed); return changed; });
    }
    @Override public Optional<Double> statValue(final ItemStack item, final StatType type, final double baseValue) {
        return repository.read(item).flatMap(data -> configurations.current().findProfile(data.profileId()).map(profile -> {
            final StatRule rule = profile.statRules().get(type); return rule == null ? baseValue : StatValueCalculator.calculate(baseValue, data.level(), rule);
        }));
    }
    public void synchronize(final ItemStack item) { repository.read(item).ifPresent(data -> stats.synchronizeIfNeeded(item, data)); }
    private static GearItemData newItemData(final ItemProfile profile) { return new GearItemData(GearItemRepository.CURRENT_SCHEMA, UUID.randomUUID(), profile.id(), 0, 0, 0); }
    private static long safeAdd(final long left, final long right) { return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right; }
}
