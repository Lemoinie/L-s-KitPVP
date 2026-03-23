package com.test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GUIManager {
    public static final Component HUB_TITLE = Component.text("LPVP Hub", NamedTextColor.BLUE);
    public static final Component CREATE_TITLE = Component.text("Create PVP Room", NamedTextColor.BLUE);
    public static final Component MANAGE_TITLE = Component.text("Room Management", NamedTextColor.BLUE);
    public static final Component KIT_PICK_TITLE = Component.text("Pick Your Kit", NamedTextColor.BLUE);

    public static void openHub(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, HUB_TITLE);

        // Create Room button
        ItemStack createRoom = createItem(Material.DIAMOND, Component.text("Create Room", NamedTextColor.GREEN), Component.text("Click to create a new room!"));
        inv.setItem(4, createRoom);

        // List Rooms
        List<Room> rooms = RoomManager.getInstance().getRooms();
        int slot = 9;
        for (Room room : rooms) {
            String creatorName = Bukkit.getOfflinePlayer(room.getCreatorUuid()).getName();
            if (creatorName == null) creatorName = "Unknown";
            
            Component status = room.isMatchStarted() ? Component.text("Started", NamedTextColor.RED) : Component.text("Waiting", NamedTextColor.GREEN);
            ItemStack roomItem = createItem(Material.IRON_SWORD, 
                Component.text(room.getType().getDisplayName() + " Room", NamedTextColor.YELLOW),
                Component.text("Creator: ", NamedTextColor.GRAY).append(Component.text(creatorName, NamedTextColor.GOLD)),
                Component.text("Players: " + room.getPlayers().size() + "/" + room.getType().getMaxPlayers(), NamedTextColor.GRAY),
                Component.text("Status: ", NamedTextColor.GRAY).append(status),
                Component.empty(),
                Component.text("Left-Click to Join!", NamedTextColor.YELLOW));
            inv.setItem(slot++, roomItem);
            if (slot >= 53) break;
        }

        player.openInventory(inv);
    }

    public static void openCreateRoom(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, CREATE_TITLE);

        inv.setItem(10, createItem(Material.WOODEN_SWORD, Component.text("1v1", NamedTextColor.GREEN), Component.text("Max Players: 2")));
        inv.setItem(12, createItem(Material.STONE_SWORD, Component.text("2v2", NamedTextColor.GREEN), Component.text("Max Players: 4")));
        inv.setItem(14, createItem(Material.GOLDEN_SWORD, Component.text("4v4", NamedTextColor.GREEN), Component.text("Max Players: 8")));
        inv.setItem(16, createItem(Material.DIAMOND_SWORD, Component.text("Chaos", NamedTextColor.RED), Component.text("Max Players: 16")));

        player.openInventory(inv);
    }

    public static void openRoomManagement(Player player) {
        Optional<Room> roomOpt = RoomManager.getInstance().getRoomByCreator(player.getUniqueId());
        if (roomOpt.isEmpty()) {
            player.sendMessage(Component.text("You don't have a room to manage!", NamedTextColor.RED));
            return;
        }
        Room room = roomOpt.get();
        Inventory inv = Bukkit.createInventory(null, 54, MANAGE_TITLE);

        // Settings (Left side)
        inv.setItem(10, createToggleItem("Combat Logging", "combat-log", room));
        inv.setItem(11, createToggleItem("Block Breaking", "block-breaking", room));
        inv.setItem(12, createToggleItem("Block Placing", "block-placing", room));
        inv.setItem(13, createToggleItem("Item Dropping", "item-dropping", room));
        inv.setItem(14, createToggleItem("Pearl Usage", "pearl-using", room));
        inv.setItem(15, createToggleItem("Chat Disabled", "chat-disabled", room));
        inv.setItem(16, createToggleItem("Command Disabled", "command-disabled", room));

        // Force Start (Bottom left)
        if (room.getStatus() == Room.RoomStatus.WAITING) {
            inv.setItem(45, createItem(Material.BLAZE_POWDER, 
                Component.text("Force Start", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("Click to skip waiting!", NamedTextColor.YELLOW)));
        }

        // Players (Right side)
        int slot = 13;
        for (UUID uuid : room.getPlayers()) {
            if (uuid.equals(player.getUniqueId())) continue;
            Player p = Bukkit.getPlayer(uuid);
            String name = (p != null) ? p.getName() : "Unknown";
            inv.setItem(slot, createItem(Material.PLAYER_HEAD, 
                Component.text(name, NamedTextColor.YELLOW),
                Component.text("Click to kick", NamedTextColor.RED)));
            slot++;
            if (slot % 9 == 0) slot += 4;
            if (slot >= 53) break;
        }

        player.openInventory(inv);
    }

    public static void openKitPick(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, KIT_PICK_TITLE);

        int slot = 10;
        for (Kit kit : KitManager.getInstance().getKits()) {
            ItemStack item = createItem(kit.icon(), 
                Component.text(kit.displayName(), NamedTextColor.GOLD),
                Component.empty(),
                Component.text("Click to select this kit!", NamedTextColor.YELLOW));
            
            // Add ID to item to identify it in listener (hidden in lore or just match display name)
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore();
                if (lore == null) lore = new ArrayList<>();
                lore.add(Component.text("ID: " + kit.id(), NamedTextColor.BLACK)); // Hidden-ish ID
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            inv.setItem(slot, item);
            slot += 2;
            if (slot > 16) break;
        }

        player.openInventory(inv);
    }

    private static ItemStack createToggleItem(String label, String key, Room room) {
        boolean enabled = room.getSetting(key);
        Material mat = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;
        return createItem(mat, Component.text(label + ": ", NamedTextColor.WHITE).append(Component.text(enabled ? "ON" : "OFF", color)),
            Component.text("Click to toggle", NamedTextColor.GRAY));
    }

    private static ItemStack createItem(Material material, Component name, Component... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            List<Component> lore = new ArrayList<>();
            for (Component line : loreLines) {
                lore.add(line);
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
