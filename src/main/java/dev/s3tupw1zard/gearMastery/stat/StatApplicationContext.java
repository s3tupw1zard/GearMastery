package dev.s3tupw1zard.gearMastery.stat;

import dev.s3tupw1zard.gearMastery.item.GearItemRepository;
import org.bukkit.inventory.ItemStack;

/** Context passed to a single native stat handler. */
public record StatApplicationContext(ItemStack item, int level, StatRule rule, GearItemRepository repository) { }
