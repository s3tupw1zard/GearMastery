package dev.s3tupw1zard.gearMastery.api;

import dev.s3tupw1zard.gearMastery.data.GearItemData;
import dev.s3tupw1zard.gearMastery.stat.StatType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public interface GearMasteryApi {
    boolean isLevelable(ItemStack item);
    Optional<GearItemData> data(ItemStack item);
    Optional<GearItemData> initialize(ItemStack item);
    Optional<GearItemData> addExperience(Player player, ItemStack item, long amount, String sourceId, Object cause);
    Optional<GearItemData> setLevel(ItemStack item, int level);
    Optional<Double> statValue(ItemStack item, StatType type, double baseValue);
}
