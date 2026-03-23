package com.test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandLpvp implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reload(sender);
                return true;
            }
            sender.sendMessage("This command is for players only!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /lpvp [hub|reload|room|set]", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "hub" -> GUIManager.openHub(player);
            case "room" -> GUIManager.openRoomManagement(player);
            case "reload" -> reload(player);
            case "set" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("spawn")) {
                    MyPlugin.getPlugin(MyPlugin.class).setPVPSpawn(player.getLocation());
                    player.sendMessage(Component.text("PVP spawn point has been set to your current location!", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Usage: /lpvp set spawn", NamedTextColor.RED));
                }
            }
            default -> player.sendMessage(Component.text("Unknown subcommand!", NamedTextColor.RED));
        }

        return true;
    }

    private void reload(CommandSender sender) {
        MyPlugin plugin = MyPlugin.getPlugin(MyPlugin.class);
        plugin.reloadConfig();
        sender.sendMessage(Component.text("L-s-KitPVP reloaded successfully!", NamedTextColor.GREEN));
    }
}
