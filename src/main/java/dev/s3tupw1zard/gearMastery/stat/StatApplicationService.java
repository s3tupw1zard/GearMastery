package dev.s3tupw1zard.gearMastery.stat;

import dev.s3tupw1zard.gearMastery.config.ConfigurationService;
import dev.s3tupw1zard.gearMastery.config.ItemProfile;
import dev.s3tupw1zard.gearMastery.data.GearItemData;
import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;
import java.util.logging.Logger;

/** Applies stat effects lazily and only once for a level/configuration revision. */
public final class StatApplicationService {
    public static final int STAT_SCHEMA = 2;
    private final ConfigurationService configurations; private final GearItemRepository repository; private final StatHandlerRegistry handlers; private final Logger logger;
    public StatApplicationService(final ConfigurationService configurations, final GearItemRepository repository, final StatHandlerRegistry handlers, final Logger logger) { this.configurations = configurations; this.repository = repository; this.handlers = handlers; this.logger = logger; }
    public void synchronizeIfNeeded(final ItemStack item, final GearItemData data) {
        final var configuration = configurations.current();
        if (repository.isStatApplicationCurrent(item, data.level(), configuration.statRevision(), STAT_SCHEMA)) return;
        // A retired profile keeps its progression data, but must not retain effects from its old configuration.
        final ItemProfile profile = configuration.findProfile(data.profileId()).orElseGet(StatApplicationService::disabledProfile);
        if (applyHandlers(handlers.handlers(), profile, item, data.level(), repository,
            (handler, exception) -> logger.warning("Could not apply " + handler.type() + " for GearMastery item " + data.gearId() + ": " + exception.getMessage()))) {
            repository.markStatApplication(item, data.level(), configuration.statRevision(), STAT_SCHEMA);
        }
    }
    private static ItemProfile disabledProfile() { return new ItemProfile("_missing_profile", java.util.Set.of(), "", java.util.Set.of(), java.util.Map.of()); }
    static boolean applyHandlers(final Iterable<StatHandler> handlers, final ItemProfile profile, final ItemStack item, final int level,
                                 final GearItemRepository repository, final BiConsumer<StatHandler, RuntimeException> failures) {
        boolean successful = true;
        for (final StatHandler handler : handlers) {
            final StatRule rule = profile.statRules().getOrDefault(handler.type(), new StatRule(false, StatScaleMode.ADDITIVE, 0.0D, Double.MAX_VALUE));
            try {
                handler.apply(new StatApplicationContext(item, level, rule, repository));
            } catch (final RuntimeException exception) {
                successful = false;
                failures.accept(handler, exception);
            }
        }
        return successful;
    }
}
