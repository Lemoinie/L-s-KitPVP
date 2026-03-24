package com.test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.EnderPearl;
import com.test.Room.RoomStatus;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

public class PVPListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        if (title.equals(GUIManager.HUB_TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (clicked.getType() == Material.DIAMOND) {
                GUIManager.openCreateRoom(player);
            } else if (clicked.getType() == Material.IRON_SWORD) {
                // Join Room logic
                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
                    RoomManager manager = RoomManager.getInstance();
                    for (Room room : manager.getRooms()) {
                        if (displayName.contains(room.getType().getDisplayName())) {
                            room.addPlayer(player);
                            player.closeInventory();
                            return;
                        }
                    }
                }
            }
        } else if (title.equals(GUIManager.CREATE_TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            RoomType type = null;
            if (clicked.getType() == Material.WOODEN_SWORD) type = RoomType.ONE_V_ONE;
            else if (clicked.getType() == Material.STONE_SWORD) type = RoomType.TWO_V_TWO;
            else if (clicked.getType() == Material.GOLDEN_SWORD) type = RoomType.FOUR_V_FOUR;
            else if (clicked.getType() == Material.DIAMOND_SWORD) type = RoomType.CHAOS;

            if (type != null) {
                Room room = RoomManager.getInstance().createRoom(player.getUniqueId(), type);
                if (room != null) {
                    room.addPlayer(player);
                    player.sendMessage(Component.text("Room created successfully! Use /lpvp room to manage.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("You already have a room! Use /lpvp room to manage it.", NamedTextColor.RED));
                }
                player.closeInventory();
            }
        } else if (title.equals(GUIManager.MANAGE_TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            RoomManager manager = RoomManager.getInstance();
            Optional<Room> roomOpt = manager.getRoomByCreator(player.getUniqueId());
            if (roomOpt.isEmpty()) return;
            Room room = roomOpt.get();

            if (clicked.getType() == Material.LIME_DYE || clicked.getType() == Material.GRAY_DYE) {
                // Toggle setting
                String key = switch (event.getRawSlot()) {
                    case 10 -> "combat-log";
                    case 11 -> "block-breaking";
                    case 12 -> "block-placing";
                    case 13 -> "item-dropping";
                    case 14 -> "pearl-using";
                    case 15 -> "chat-disabled";
                    case 16 -> "command-disabled";
                    default -> null;
                };
                if (key != null) {
                    room.toggleSetting(key);
                    GUIManager.openRoomManagement(player);
                }
            } else if (clicked.getType() == Material.BLAZE_POWDER) {
                // Force Start
                if (room.getStatus() == RoomStatus.WAITING) {
                    if (room.getPlayers().size() >= 2) {
                        room.forceStart();
                        player.closeInventory();
                    } else {
                        player.sendMessage(Component.text("You need at least 2 players to start!", NamedTextColor.RED));
                    }
                }
            } else if (clicked.getType() == Material.PLAYER_HEAD) {
                // Kick player
                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String targetName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
                    Player target = Bukkit.getPlayer(targetName);
                    if (target != null) {
                        room.removePlayer(target);
                        target.sendMessage(Component.text("You have been kicked from the room by the creator.", NamedTextColor.RED));
                        GUIManager.openRoomManagement(player);
                    }
                }
            }
        } else if (title.equals(GUIManager.KIT_PICK_TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
                if (room.getStatus() == RoomStatus.KIT_PICK) {
                    ItemMeta meta = clicked.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        List<Component> lore = meta.lore();
                        if (lore != null && !lore.isEmpty()) {
                            Component lastLine = lore.get(lore.size() - 1);
                            String plain = PlainTextComponentSerializer.plainText().serialize(lastLine);
                            if (plain.startsWith("ID: ")) {
                                String kitId = plain.substring(4);
                                KitManager.getInstance().getKit(kitId).ifPresent(kit -> {
                                    room.applyKit(player, kit);
                                    player.closeInventory();
                                });
                            }
                        }
                    }
                }
            });
        }
    }

    @EventHandler
    public void onInventoryClickExploit(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
            if (room.getStatus() != RoomStatus.STARTED) return;

            Inventory clickedInv = event.getClickedInventory();
            if (clickedInv == null) return;

            // Prevent moving items into other inventories
            if (event.getView().getTopInventory() != event.getView().getBottomInventory()) {
                if (clickedInv == event.getView().getTopInventory()) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("You cannot interact with this inventory during a match!", NamedTextColor.RED));
                } else if (event.isShiftClick()) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("You cannot move items into other inventories during a match!", NamedTextColor.RED));
                }
            }
        });
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
            if (room.getStatus() != RoomStatus.STARTED) return;

            InventoryType type = event.getInventory().getType();
            if (type != InventoryType.CRAFTING && type != InventoryType.PLAYER) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot open this inventory during a match!", NamedTextColor.RED));
            }
        });
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
            if (room.getStatus() != RoomStatus.STARTED) return;

            Block block = event.getClickedBlock();
            if (block != null) {
                Material type = block.getType();
                // Check if it's a container or something we want to block
                if (type.name().contains("CHEST") || type.name().contains("SHULKER") || type == Material.HOPPER || 
                    type == Material.DISPENSER || type == Material.DROPPER || type == Material.BARREL) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("You cannot interact with containers during a match!", NamedTextColor.RED));
                }
            }
        });
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
            if (room.getStatus() != RoomStatus.STARTED) return;

            if (room.getSetting("chat-disabled")) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Chat is disabled during this match!", NamedTextColor.RED));
            }
        });
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
            if (room.getStatus() != RoomStatus.STARTED) return;

            if (room.getSetting("command-disabled")) {
                String cmd = event.getMessage().toLowerCase();
                // Allow /lpvp just in case
                if (!cmd.startsWith("/lpvp")) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("Commands are disabled during this match!", NamedTextColor.RED));
                }
            }
        });
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) return;

        RoomManager manager = RoomManager.getInstance();
        Optional<Room> damagerRoom = manager.getRoomByPlayer(damager.getUniqueId());
        Optional<Room> victimRoom = manager.getRoomByPlayer(victim.getUniqueId());

        if (damagerRoom.isPresent() && victimRoom.isPresent()) {
            Room room = damagerRoom.get();
            if (room.equals(victimRoom.get())) {
                if (room.getStatus() != RoomStatus.STARTED) {
                    event.setCancelled(true);
                    damager.sendMessage(Component.text("The match hasn't started yet!", NamedTextColor.RED));
                }
                // Both in same room and match started - allow damage
                return;
            } else {
                // Different rooms
                event.setCancelled(true);
                damager.sendMessage(Component.text("You can only damage players in your PVP match!", NamedTextColor.RED));
                return;
            }
        }

        if (damagerRoom.isPresent() && victimRoom.isEmpty()) {
            event.setCancelled(true);
            damager.sendMessage(Component.text("You cannot hit players outside of your PVP match!", NamedTextColor.RED));
            return;
        }

        if (damagerRoom.isEmpty() && victimRoom.isPresent()) {
            event.setCancelled(true);
            damager.sendMessage(Component.text("That player is currently in a PVP match!", NamedTextColor.RED));
            return;
        }

        // Both are NOT in a room - allow standard PVP
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
            if (room.isMatchStarted()) {
                if (!room.getSetting("combat-log")) {
                    room.eliminatePlayer(player);
                    Bukkit.broadcast(Component.text(player.getName() + " combat logged and was eliminated!", NamedTextColor.RED));
                }
            }
            room.removePlayer(player);
        });
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
            room.eliminatePlayer(player);
        });
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
            if (room.isMatchStarted()) {
                if (!room.getSetting("block-breaking")) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("Block breaking is disabled in this room!", NamedTextColor.RED));
                }
            }
        });
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
            if (room.isMatchStarted()) {
                if (!room.getSetting("block-placing")) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("Block placing is disabled in this room!", NamedTextColor.RED));
                }
            }
        });
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
            if (room.isMatchStarted()) {
                if (!room.getSetting("item-dropping")) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("Item dropping is disabled in this room!", NamedTextColor.RED));
                }
            }
        });
    }

    @EventHandler
    public void onPearlUse(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof EnderPearl pearl && pearl.getShooter() instanceof Player player) {
            RoomManager.getInstance().getRoomByPlayer(player.getUniqueId()).ifPresent(room -> {
                if (room.isMatchStarted()) {
                    if (!room.getSetting("pearl-using")) {
                        event.setCancelled(true);
                        player.sendMessage(Component.text("Ender pearls are disabled in this room!", NamedTextColor.RED));
                    }
                }
            });
        }
    }
}
