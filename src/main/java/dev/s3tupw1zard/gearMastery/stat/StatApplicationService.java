package dev.s3tupw1zard.gearMastery.stat;

import dev.s3tupw1zard.gearMastery.config.ConfigurationService;
import dev.s3tupw1zard.gearMastery.config.ItemProfile;
import dev.s3tupw1zard.gearMastery.data.GearItemData;
import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

/** Applies stat effects lazily and only once for a level/configuration revision. */
public final class StatApplicationService {
    public static final int STAT_SCHEMA = 1;
    private final ConfigurationService configurations; private final GearItemRepository repository; private final StatHandlerRegistry handlers; private final Logger logger;
    public StatApplicationService(final ConfigurationService configurations, final GearItemRepository repository, final StatHandlerRegistry handlers, final Logger logger) { this.configurations = configurations; this.repository = repository; this.handlers = handlers; this.logger = logger; }
    public void synchronizeIfNeeded(final ItemStack item, final GearItemData data) {
        final var configuration = configurations.current();
        if (repository.isStatApplicationCurrent(item, data.level(), configuration.generation(), STAT_SCHEMA)) return;
        final ItemProfile profile = configuration.findProfile(data.profileId()).orElse(null); if (profile == null) return;
        for (final StatHandler handler : handlers.handlers()) {
            final StatRule rule = profile.statRules().getOrDefault(handler.type(), new StatRule(false, StatScaleMode.ADDITIVE, 0.0D, Double.MAX_VALUE));
            try {
                handler.apply(new StatApplicationContext(item, data.level(), rule, repository));
            } catch (final RuntimeException exception) {
                logger.warning("Could not apply " + handler.type() + " for GearMastery item " + data.gearId() + ": " + exception.getMessage());
            }
        }
        repository.markStatApplication(item, data.level(), configuration.generation(), STAT_SCHEMA);
    }
}
