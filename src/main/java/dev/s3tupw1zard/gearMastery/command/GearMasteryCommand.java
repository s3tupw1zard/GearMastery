package dev.s3tupw1zard.gearMastery.command;

import dev.s3tupw1zard.gearMastery.config.ConfigurationService;
import dev.s3tupw1zard.gearMastery.data.GearItemData;
import dev.s3tupw1zard.gearMastery.level.ProgressionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public final class GearMasteryCommand implements CommandExecutor, TabCompleter {
    private final ProgressionService progression; private final ConfigurationService configurations;
    public GearMasteryCommand(final ProgressionService progression, final ConfigurationService configurations) { this.progression = progression; this.configurations = configurations; }
    @Override public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final String @NotNull [] args) {
        if (args.length == 0) return usage(sender);
        return switch (args[0].toLowerCase()) {
            case "status" -> status(sender);
            case "reload" -> reload(sender);
            case "xp" -> xp(sender, args);
            case "level" -> level(sender, args);
            default -> usage(sender);
        };
    }
    private boolean status(final CommandSender sender) {
        if (!sender.hasPermission("gearmastery.admin.status")) return denied(sender);
        final Optional<ItemStack> item = held(sender); if (item.isEmpty()) return true;
        progression.synchronize(item.get());
        final Optional<GearItemData> data = progression.data(item.get());
        if (data.isEmpty()) { sender.sendMessage("This held item has no GearMastery progression."); return true; }
        final GearItemData value = data.get(); sender.sendMessage("Gear ID: " + value.gearId()); sender.sendMessage("Profile: " + value.profileId());
        sender.sendMessage("Level: " + value.level() + " | XP: " + value.experience() + " | Lifetime XP: " + value.lifetimeExperience()); return true;
    }
    private boolean xp(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("gearmastery.admin.xp")) return denied(sender);
        if (args.length != 3 || !args[1].equalsIgnoreCase("add")) return usage(sender);
        try { final long amount = Long.parseLong(args[2]); if (amount <= 0) throw new NumberFormatException(); final Optional<ItemStack> item = held(sender); if (item.isEmpty()) return true;
            final Optional<GearItemData> result = progression.addExperience((Player) sender, item.get(), amount, "admin", sender); if (result.isEmpty()) sender.sendMessage("The item does not support admin XP or has no valid profile."); else sender.sendMessage("XP applied."); return true;
        } catch (final NumberFormatException ignored) { sender.sendMessage("XP must be a positive whole number."); return true; }
    }
    private boolean level(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("gearmastery.admin.level")) return denied(sender);
        if (args.length != 3 || !args[1].equalsIgnoreCase("set")) return usage(sender);
        try { final int target = Integer.parseInt(args[2]); final Optional<ItemStack> item = held(sender); if (item.isEmpty()) return true;
            progression.setLevel(item.get(), target).ifPresentOrElse(value -> sender.sendMessage("Level set to " + value.level() + "."), () -> sender.sendMessage("The held item has no matching profile.")); return true;
        } catch (final IllegalArgumentException ignored) { sender.sendMessage("Level must be within the configured bounds."); return true; }
    }
    private boolean reload(final CommandSender sender) { if (!sender.hasPermission("gearmastery.admin.reload")) return denied(sender); sender.sendMessage(configurations.reload() ? "GearMastery configuration reloaded." : "Reload failed; previous configuration remains active."); return true; }
    private Optional<ItemStack> held(final CommandSender sender) { if (sender instanceof Player player) return Optional.of(player.getInventory().getItemInMainHand()); sender.sendMessage("This command must be run by a player holding an item."); return Optional.empty(); }
    private boolean denied(final CommandSender sender) { sender.sendMessage("You do not have permission to do that."); return true; }
    private boolean usage(final CommandSender sender) { sender.sendMessage("Usage: /gearmastery <status|xp add <amount>|level set <level>|reload>"); return true; }
    @Override public List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String alias, final String @NotNull [] args) { if (args.length == 1) return List.of("status", "xp", "level", "reload"); if (args.length == 2 && args[0].equalsIgnoreCase("xp")) return List.of("add"); if (args.length == 2 && args[0].equalsIgnoreCase("level")) return List.of("set"); return List.of(); }
}
