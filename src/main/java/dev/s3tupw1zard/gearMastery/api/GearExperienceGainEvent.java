package dev.s3tupw1zard.gearMastery.api;

import dev.s3tupw1zard.gearMastery.xp.ExperienceContext;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class GearExperienceGainEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ExperienceContext context; private long amount; private boolean cancelled;
    public GearExperienceGainEvent(final ExperienceContext context, final long amount) { this.context = context; this.amount = amount; }
    public ExperienceContext context() { return context; } public long amount() { return amount; }
    public void setAmount(final long amount) { if (amount < 0) throw new IllegalArgumentException("XP cannot be negative"); this.amount = amount; }
    @Override public boolean isCancelled() { return cancelled; } @Override public void setCancelled(final boolean cancelled) { this.cancelled = cancelled; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; } public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
