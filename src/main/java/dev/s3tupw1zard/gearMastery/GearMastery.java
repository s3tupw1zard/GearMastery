package dev.s3tupw1zard.gearMastery;

import dev.s3tupw1zard.gearMastery.api.GearMasteryApi;
import dev.s3tupw1zard.gearMastery.command.GearMasteryCommand;
import dev.s3tupw1zard.gearMastery.config.ConfigurationService;
import dev.s3tupw1zard.gearMastery.config.ConfigMigrationService;
import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import dev.s3tupw1zard.gearMastery.level.ProgressionService;
import dev.s3tupw1zard.gearMastery.listener.BlockBreakExperienceListener;
import dev.s3tupw1zard.gearMastery.stat.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class GearMastery extends JavaPlugin {

    private ConfigurationService configurations;

    @Override
    public void onEnable() {
        saveDefaultResources();
        if (!new ConfigMigrationService(this).migrateInstalledConfigs()) {
            getLogger().severe("GearMastery could not migrate its configuration and will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        configurations = new ConfigurationService(this);
        if (!configurations.reload()) {
            getLogger().severe("GearMastery could not load a valid configuration and will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        final GearItemRepository repository = new GearItemRepository(this);
        final StatHandlerRegistry handlers = new StatHandlerRegistry();
        handlers.register(new DurabilityStatHandler()); handlers.register(new MiningSpeedStatHandler());
        handlers.register(new NativeAttributeStatHandler(StatType.ATTACK_DAMAGE, org.bukkit.attribute.Attribute.ATTACK_DAMAGE));
        handlers.register(new AttackSpeedStatHandler());
        handlers.register(new NativeAttributeStatHandler(StatType.KNOCKBACK, org.bukkit.attribute.Attribute.ATTACK_KNOCKBACK));
        handlers.register(new NativeAttributeStatHandler(StatType.ARMOR, org.bukkit.attribute.Attribute.ARMOR));
        handlers.register(new NativeAttributeStatHandler(StatType.ARMOR_TOUGHNESS, org.bukkit.attribute.Attribute.ARMOR_TOUGHNESS));
        handlers.register(new NativeAttributeStatHandler(StatType.KNOCKBACK_RESISTANCE, org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE));
        final ProgressionService progression = new ProgressionService(configurations, repository, new StatApplicationService(configurations, repository, handlers, getLogger()));
        getServer().getServicesManager().register(GearMasteryApi.class, progression, this, org.bukkit.plugin.ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(new BlockBreakExperienceListener(configurations, progression), this);
        final GearMasteryCommand command = new GearMasteryCommand(progression, configurations);
        Objects.requireNonNull(getCommand("gearmastery"), "Missing gearmastery command").setExecutor(command);
        Objects.requireNonNull(getCommand("gearmastery")).setTabCompleter(command);
        getLogger().info("GearMastery core foundation enabled.");
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
    }

    private void saveDefaultResources() {
        saveDefaultConfig();
        for (final String resource : new String[] {"leveling.yml", "items.yml", "stats.yml", "xp/blocks.yml", "messages.yml"}) {
            saveResource(resource, false);
        }
    }
}
