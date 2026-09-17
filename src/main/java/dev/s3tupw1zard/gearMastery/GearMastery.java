package dev.s3tupw1zard.gearMastery;

import dev.s3tupw1zard.gearMastery.api.GearMasteryApi;
import dev.s3tupw1zard.gearMastery.command.GearMasteryCommand;
import dev.s3tupw1zard.gearMastery.config.ConfigurationService;
import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import dev.s3tupw1zard.gearMastery.level.ProgressionService;
import dev.s3tupw1zard.gearMastery.listener.BlockBreakExperienceListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class GearMastery extends JavaPlugin {

    private ConfigurationService configurations;

    @Override
    public void onEnable() {
        saveDefaultResources();
        configurations = new ConfigurationService(this);
        if (!configurations.reload()) {
            getLogger().severe("GearMastery could not load a valid configuration and will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        final ProgressionService progression = new ProgressionService(configurations, new GearItemRepository(this));
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
