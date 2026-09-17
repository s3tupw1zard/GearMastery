package dev.s3tupw1zard.gearMastery.api;

import dev.s3tupw1zard.gearMastery.data.GearItemData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Fired after each persisted level increase. */
public final class GearLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player; private final ItemStack item; private final GearItemData previous; private final GearItemData current;
    public GearLevelUpEvent(final Player player, final ItemStack item, final GearItemData previous, final GearItemData current) { this.player = player; this.item = item; this.previous = previous; this.current = current; }
    public Player player() { return player; } public ItemStack item() { return item; } public GearItemData previous() { return previous; } public GearItemData current() { return current; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; } public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
