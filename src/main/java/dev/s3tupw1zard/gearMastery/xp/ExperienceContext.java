package dev.s3tupw1zard.gearMastery.xp;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Context intentionally remains extensible as future sources need different payloads. */
public record ExperienceContext(Player player, ItemStack item, String sourceId, Object cause) { }
