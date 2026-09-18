package dev.s3tupw1zard.gearMastery.listener;

import dev.s3tupw1zard.gearMastery.level.ProgressionService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/** Event-driven, bounded synchronization for items entering or being used by a player. */
public final class RuntimeStatSyncListener implements Listener {
    private final JavaPlugin plugin;
    private final ProgressionService progression;
    public RuntimeStatSyncListener(final JavaPlugin plugin, final ProgressionService progression) { this.plugin = plugin; this.progression = progression; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) { progression.synchronizePlayer(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldItem(final PlayerItemHeldEvent event) { later(event.getPlayer(), () -> progression.synchronize(event.getPlayer().getInventory().getItem(event.getNewSlot()))); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(final PlayerSwapHandItemsEvent event) {
        later(event.getPlayer(), () -> { progression.synchronize(event.getPlayer().getInventory().getItemInMainHand()); progression.synchronize(event.getPlayer().getInventory().getItemInOffHand()); });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) later(player, () -> {
            synchronize(event.getCursor());
            if (event.getClickedInventory() != null && event.getSlot() >= 0) synchronize(event.getClickedInventory().getItem(event.getSlot()));
            if (event.isShiftClick()) progression.synchronizePlayer(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) later(player, () -> event.getNewItems().keySet().forEach(slot -> synchronize(event.getView().getItem(slot))));
    }

    private void later(final Player player, final Runnable action) { plugin.getServer().getScheduler().runTask(plugin, action); }

    private void synchronize(final ItemStack item) { if (item != null) progression.synchronize(item); }
}
