package com.test;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Room {
    public enum RoomStatus { WAITING, KIT_PICK, STARTED }
    
    private final UUID id;
    private final UUID creatorUuid;
    private final RoomType type;
    private RoomStatus status = RoomStatus.WAITING;
    private final Set<UUID> players = new HashSet<>();
    private final Set<UUID> standingPlayers = new HashSet<>();
    private final Map<UUID, Location> originalLocations = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<String, Boolean> settings = new HashMap<>();
    private BukkitTask countdownTask;
    private int countdownSeconds = 30;

    public Room(UUID creatorUuid, RoomType type) {
        this.id = UUID.randomUUID();
        this.creatorUuid = creatorUuid;
        this.type = type;
        loadDefaultSettings();
    }

    private void loadDefaultSettings() {
        MyPlugin plugin = MyPlugin.getPlugin(MyPlugin.class);
        settings.put("combat-log", plugin.getConfig().getBoolean("combat-log", false));
        settings.put("block-breaking", plugin.getConfig().getBoolean("block-breaking", true));
        settings.put("block-placing", plugin.getConfig().getBoolean("block-placing", true));
        settings.put("item-dropping", plugin.getConfig().getBoolean("item-dropping", false));
        settings.put("pearl-using", plugin.getConfig().getBoolean("pearl-using", true));
        settings.put("chat-disabled", plugin.getConfig().getBoolean("chat-disabled", false));
        settings.put("command-disabled", plugin.getConfig().getBoolean("command-disabled", false));
    }

    public UUID getCreatorUuid() {
        return creatorUuid;
    }

    public boolean getSetting(String key) {
        return settings.getOrDefault(key, true);
    }

    public void toggleSetting(String key) {
        settings.put(key, !getSetting(key));
    }

    public UUID getId() {
        return id;
    }

    public RoomType getType() {
        return type;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public boolean isMatchStarted() {
        return status == RoomStatus.STARTED;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void addPlayer(Player player) {
        if (status != RoomStatus.WAITING) {
            player.sendMessage(Component.text("This match has already started!", NamedTextColor.RED));
            return;
        }
        if (players.size() >= type.getMaxPlayers()) {
            player.sendMessage(Component.text("This room is full!", NamedTextColor.RED));
            return;
        }
        players.add(player.getUniqueId());
        broadcast(Component.text(player.getName() + " joined the room (" + players.size() + "/" + type.getMaxPlayers() + ")", NamedTextColor.YELLOW));
        
        if (players.size() >= 2) {
            startCountdown();
        }
    }

    public void removePlayer(Player player) {
        if (players.remove(player.getUniqueId())) {
            broadcast(Component.text(player.getName() + " left the room.", NamedTextColor.YELLOW));
            
            if (status == RoomStatus.STARTED || status == RoomStatus.KIT_PICK) {
                eliminatePlayer(player);
                restoreInventory(player);
            }

            if (status == RoomStatus.WAITING) {
                if (players.size() < 2) {
                    stopCountdown();
                } else {
                    startCountdown(); // Reset countdown
                }
            }
            checkWinner();
        }

        if (players.isEmpty()) {
            RoomManager.getInstance().removeRoom(this);
        }
    }

    public boolean containsPlayer(UUID uuid) {
        return players.contains(uuid);
    }

    public void eliminatePlayer(Player player) {
        if (status == RoomStatus.WAITING) return;
        standingPlayers.remove(player.getUniqueId());
        broadcast(Component.text(player.getName() + " has been eliminated!", NamedTextColor.RED));
        checkWinner();
    }

    private void checkWinner() {
        if (status != RoomStatus.STARTED && status != RoomStatus.KIT_PICK) return;
        if (standingPlayers.size() <= 1) {
            endMatch();
        }
    }

    private void stopCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    public void forceStart() {
        if (status == RoomStatus.WAITING && players.size() >= 2) {
            startKitPickPhase();
        }
    }

    private void startCountdown() {
        stopCountdown();
        countdownSeconds = 30;
        countdownTask = Bukkit.getScheduler().runTaskTimer(MyPlugin.getPlugin(MyPlugin.class), () -> {
            if (countdownSeconds <= 0) {
                if (status == RoomStatus.WAITING) {
                    startKitPickPhase();
                } else if (status == RoomStatus.KIT_PICK) {
                    startMatch();
                }
                return;
            }

            if (countdownSeconds <= 5 || countdownSeconds % 10 == 0) {
                String phase = (status == RoomStatus.WAITING) ? "Teleporting" : "Match Start";
                broadcast(Component.text(phase + " in " + countdownSeconds + " seconds...", NamedTextColor.GOLD));
            }
            countdownSeconds--;
        }, 0, 20);
    }

    private void startKitPickPhase() {
        stopCountdown();
        status = RoomStatus.KIT_PICK;
        Location pvpSpawn = MyPlugin.getPlugin(MyPlugin.class).getPVPSpawn();

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                originalLocations.put(uuid, player.getLocation());
                saveInventory(player);
                player.teleport(pvpSpawn);
                GUIManager.openKitPick(player);
            }
        }

        broadcast(Component.text("Teleported to PVP Arena! Pick your kits. Match starts in 30 seconds.", NamedTextColor.GREEN));
        startCountdown(); // Start Kit Pick countdown
    }

    private void startMatch() {
        stopCountdown();
        status = RoomStatus.STARTED;
        standingPlayers.addAll(players);
        broadcast(Component.text("The match has started! Good luck!", NamedTextColor.RED, TextDecoration.BOLD));
    }

    private void endMatch() {
        stopCountdown();
        if (!standingPlayers.isEmpty()) {
            UUID winnerUuid = standingPlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerUuid);
            if (winner != null) {
                broadcast(Component.text("Winner: " + winner.getName() + "!", NamedTextColor.GOLD, TextDecoration.BOLD));
            }
        } else {
            broadcast(Component.text("The match ended with no winner.", NamedTextColor.YELLOW));
        }

        // Teleport back and restore inventories
        for (UUID uuid : new HashSet<>(players)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Location loc = originalLocations.get(uuid);
                if (loc != null) player.teleport(loc);
                restoreInventory(player);
            }
        }

        players.clear();
        standingPlayers.clear();
        originalLocations.clear();
        savedInventories.clear();
        savedArmor.clear();
        RoomManager.getInstance().removeRoom(this);
    }

    private void saveInventory(Player player) {
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
        savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());
        player.getInventory().clear();
    }

    private void restoreInventory(Player player) {
        if (savedInventories.containsKey(player.getUniqueId())) {
            player.getInventory().clear();
            player.getInventory().setContents(savedInventories.remove(player.getUniqueId()));
            player.getInventory().setArmorContents(savedArmor.remove(player.getUniqueId()));
        }
    }

    public void applyKit(Player player, Kit kit) {
        player.getInventory().clear();
        
        // Armor
        player.getInventory().setHelmet(createKitItem(kit.armor().get("helmet")));
        player.getInventory().setChestplate(createKitItem(kit.armor().get("chestplate")));
        player.getInventory().setLeggings(createKitItem(kit.armor().get("leggings")));
        player.getInventory().setBoots(createKitItem(kit.armor().get("boots")));

        // Items
        for (Kit.KitItem kitItem : kit.items()) {
            player.getInventory().addItem(createKitItem(kitItem));
        }
        
        player.sendMessage(Component.text("You picked the " + kit.displayName() + "!", NamedTextColor.GREEN));
    }

    private ItemStack createKitItem(Kit.KitItem kitItem) {
        if (kitItem == null || kitItem.material() == null || kitItem.material() == Material.AIR) return null;
        ItemStack item = new ItemStack(kitItem.material(), kitItem.amount());
        
        // Potion Support
        if (kitItem.potionType() != null && item.getItemMeta() instanceof PotionMeta potionMeta) {
            try {
                PotionType type = PotionType.valueOf(kitItem.potionType().toUpperCase());
                potionMeta.setBasePotionType(type);
                item.setItemMeta(potionMeta);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Invalid potion type: " + kitItem.potionType());
            }
        }

        for (Map.Entry<String, Integer> entry : kitItem.enchantments().entrySet()) {
            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(entry.getKey()));
            if (ench != null) {
                item.addUnsafeEnchantment(ench, entry.getValue());
            }
        }
        return item;
    }

    private void broadcast(Component message) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(message);
            }
        }
    }
}
