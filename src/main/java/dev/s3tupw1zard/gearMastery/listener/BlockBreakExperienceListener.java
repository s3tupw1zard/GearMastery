package dev.s3tupw1zard.gearMastery.listener;

import dev.s3tupw1zard.gearMastery.config.ConfigurationService;
import dev.s3tupw1zard.gearMastery.level.ProgressionService;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/** The deliberately small vertical slice: configured mining XP for an actually broken block. */
public final class BlockBreakExperienceListener implements Listener {
    private static final String SOURCE_ID = "block_break";
    private final ConfigurationService configurations; private final ProgressionService progression;
    public BlockBreakExperienceListener(final ConfigurationService configurations, final ProgressionService progression) { this.configurations = configurations; this.progression = progression; }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (configurations.current().excludeCreative() && event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        final long xp = configurations.current().blockExperience().getOrDefault(event.getBlock().getType(), 0L);
        if (xp <= 0) return;
        final ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        progression.addExperience(event.getPlayer(), item, xp, SOURCE_ID, event);
    }
}
