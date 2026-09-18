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
    private final ConfigurationService configurations; private final GearItemRepository repository; private final StatApplicationService stats; private final ExperienceGainEventDispatcher experienceEvents;
    public ProgressionService(final ConfigurationService configurations, final GearItemRepository repository, final StatApplicationService stats) { this(configurations, repository, stats, event -> Bukkit.getPluginManager().callEvent(event)); }
    ProgressionService(final ConfigurationService configurations, final GearItemRepository repository, final StatApplicationService stats, final ExperienceGainEventDispatcher experienceEvents) { this.configurations = configurations; this.repository = repository; this.stats = stats; this.experienceEvents = experienceEvents; }
    @Override public boolean isLevelable(final ItemStack item) { return configurations.current().profileFor(item.getType()).isPresent(); }
    @Override public Optional<GearItemData> data(final ItemStack item) { return repository.read(item); }
    @Override public Optional<GearItemData> initialize(final ItemStack item) {
        final Optional<GearItemData> existing = repository.read(item); if (existing.isPresent()) { stats.synchronizeIfNeeded(item, existing.get()); return existing; }
        return configurations.current().profileFor(item.getType()).map(profile -> { final GearItemData data = newItemData(profile); repository.write(item, data); stats.synchronizeIfNeeded(item, data); return data; });
    }
    @Override public Optional<GearItemData> addExperience(final Player player, final ItemStack item, final long amount, final String sourceId, final Object cause) {
        if (amount <= 0) return repository.read(item);
        final GearConfiguration configuration = configurations.current();
        final Optional<GearItemData> existingData = repository.read(item);
        final Optional<ExperienceAdmission> admission = ExperienceAdmission.resolve(configuration, existingData, item.getType(), sourceId);
        if (admission.isEmpty()) { existingData.ifPresent(data -> stats.synchronizeIfNeeded(item, data, configuration)); return Optional.empty(); }
        final GearExperienceGainEvent gain = new GearExperienceGainEvent(new ExperienceContext(player, item, sourceId, cause), amount);
        experienceEvents.call(gain);
        final Optional<GearItemData> postEventData = repository.read(item);
        if (!ExperienceAdmission.shouldMutate(gain.isCancelled(), gain.amount())) return postEventData;
        GearItemData state = mutationBase(postEventData, admission.get().profile());
        if (postEventData.isEmpty()) repository.write(item, state);
        long xp = safeAdd(state.experience(), gain.amount()); long lifetime = safeAdd(state.lifetimeExperience(), gain.amount());
        final LevelingCurve curve = configuration.curveFor(configuration.findProfile(state.profileId()).orElse(admission.get().profile()));
        while (state.level() < configuration.maxLevel() && xp >= curve.experienceForNextLevel(state.level())) {
            xp -= curve.experienceForNextLevel(state.level()); final GearItemData before = state;
            state = new GearItemData(state.schemaVersion(), state.gearId(), state.profileId(), state.level() + 1, xp, lifetime);
            repository.write(item, state); Bukkit.getPluginManager().callEvent(new GearLevelUpEvent(player, item, before, state));
            // Level-up callbacks are allowed to use the public API. Never let the outer
            // transaction write its stale local state over a callback's newer mutation.
            final Optional<GearItemData> callbackState = repository.read(item);
            if (callbackState.isEmpty()) return Optional.empty();
            state = callbackState.get();
            xp = state.experience();
            lifetime = state.lifetimeExperience();
        }
        state = new GearItemData(state.schemaVersion(), state.gearId(), state.profileId(), state.level(), xp, lifetime); repository.write(item, state); stats.synchronizeIfNeeded(item, state, configuration); return Optional.of(state);
    }
    @Override public Optional<GearItemData> setLevel(final ItemStack item, final int level) {
        if (level < 0 || level > configurations.current().maxLevel()) throw new IllegalArgumentException("Level is outside configured bounds");
        return initialize(item).map(data -> { final GearItemData changed = new GearItemData(data.schemaVersion(), data.gearId(), data.profileId(), level, 0, data.lifetimeExperience()); repository.write(item, changed); stats.synchronizeIfNeeded(item, changed); return changed; });
    }
    @Override public Optional<Double> statValue(final ItemStack item, final StatType type, final double baseValue) {
        final GearConfiguration configuration = configurations.current();
        return repository.read(item).flatMap(data -> configuration.findProfile(data.profileId()).flatMap(profile -> calculateStatValue(baseValue, data, type, profile, configuration)));
    }
    public void synchronize(final ItemStack item) { repository.read(item).ifPresent(data -> stats.synchronizeIfNeeded(item, data)); }
    public void synchronizePlayer(final Player player) {
        for (final ItemStack item : player.getInventory().getContents()) synchronize(item);
        for (final ItemStack item : player.getInventory().getArmorContents()) synchronize(item);
        synchronize(player.getInventory().getItemInOffHand());
    }
    private static GearItemData newItemData(final ItemProfile profile) { return new GearItemData(GearItemRepository.CURRENT_SCHEMA, UUID.randomUUID(), profile.id(), 0, 0, 0); }
    static GearItemData mutationBase(final Optional<GearItemData> postEventData, final ItemProfile admittedProfile) { return postEventData.orElseGet(() -> newItemData(admittedProfile)); }
    static Optional<Double> calculateStatValue(final double baseValue, final GearItemData data, final StatType type, final ItemProfile profile, final GearConfiguration configuration) {
        final StatRule rule = profile.statRules().get(type);
        return Optional.of(rule == null ? baseValue : StatValueCalculator.calculate(baseValue, EffectiveStatLevel.of(data.level(), configuration.maxLevel()), rule));
    }
    private static long safeAdd(final long left, final long right) { return right > Long.MAX_VALUE - left ? Long.MAX_VALUE : left + right; }

    @FunctionalInterface interface ExperienceGainEventDispatcher { void call(GearExperienceGainEvent event); }
}
