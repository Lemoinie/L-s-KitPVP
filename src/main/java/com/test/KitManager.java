package com.test;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

import java.util.*;

public class KitManager {
    private static KitManager instance;
    private final Map<String, Kit> kits = new HashMap<>();

    private KitManager() {}

    public static KitManager getInstance() {
        if (instance == null) instance = new KitManager();
        return instance;
    }

    public void loadKits() {
        kits.clear();
        MyPlugin plugin = MyPlugin.getPlugin(MyPlugin.class);
        File kitsFolder = new File(plugin.getDataFolder(), "kits");
        if (!kitsFolder.exists()) {
            kitsFolder.mkdirs();
            // Create default kits if they don't exist
            saveDefaultKits(kitsFolder);
        }

        File[] files = kitsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            plugin.getLogger().warning("No kit files found in the kits folder!");
            return;
        }

        List<String> loadedNames = new ArrayList<>();
        for (File file : files) {
            String id = file.getName().replace(".yml", "");
            if (id.equals("example_kit_structure")) continue;

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String displayName = config.getString("display-name", id);
                String iconStr = config.getString("icon", "IRON_SWORD").toUpperCase();
                Material icon = Material.matchMaterial(iconStr);
                if (icon == null) icon = Material.IRON_SWORD;

                Map<String, Kit.KitItem> armor = new HashMap<>();
                ConfigurationSection armorSection = config.getConfigurationSection("armor");
                if (armorSection != null) {
                    for (String slot : armorSection.getKeys(false)) {
                        Kit.KitItem kitItem = parseKitItem(armorSection.getConfigurationSection(slot));
                        if (kitItem != null) {
                            armor.put(slot.toLowerCase(), kitItem);
                        }
                    }
                }

                List<Kit.KitItem> items = new ArrayList<>();
                List<Map<?, ?>> itemMaps = config.getMapList("items");
                for (Map<?, ?> map : itemMaps) {
                    Kit.KitItem kitItem = parseKitItem(map);
                    if (kitItem != null) {
                        items.add(kitItem);
                    }
                }

                kits.put(id, new Kit(id, displayName, icon, armor, items));
                loadedNames.add(id);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load kit " + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        plugin.getLogger().info("Successfully loaded " + loadedNames.size() + " kits: " + String.join(", ", loadedNames));
    }

    private Kit.KitItem parseKitItem(ConfigurationSection section) {
        if (section == null) return null;
        String matStr = section.getString("material", "AIR").toUpperCase();
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.AIR;
        
        int amount = section.getInt("amount", 1);
        String potionType = section.getString("potion-type");
        if (potionType != null) potionType = potionType.toUpperCase();
        
        Map<String, Integer> enchs = new HashMap<>();
        ConfigurationSection enchSection = section.getConfigurationSection("enchantments");
        if (enchSection != null) {
            for (String key : enchSection.getKeys(false)) {
                enchs.put(key.toLowerCase(), enchSection.getInt(key));
            }
        }
        return new Kit.KitItem(mat, amount, enchs, potionType);
    }

    private Kit.KitItem parseKitItem(Map<?, ?> map) {
        if (map == null) return null;
        String matStr = (String) map.get("material");
        if (matStr == null) return null;
        
        Material mat = Material.matchMaterial(matStr.toUpperCase());
        if (mat == null) mat = Material.AIR;
        
        Object amountObj = map.get("amount");
        int amount = (amountObj instanceof Number num) ? num.intValue() : 1;
        
        String potionType = (String) map.get("potion-type");
        if (potionType != null) potionType = potionType.toUpperCase();
        
        Map<String, Integer> enchs = new HashMap<>();
        if (map.containsKey("enchantments")) {
            Object enchObj = map.get("enchantments");
            if (enchObj instanceof Map<?, ?> enchMap) {
                for (Map.Entry<?, ?> entry : enchMap.entrySet()) {
                    String key = entry.getKey().toString().toLowerCase();
                    if (entry.getValue() instanceof Number level) {
                        enchs.put(key, level.intValue());
                    }
                }
            }
        }
        return new Kit.KitItem(mat, amount, enchs, potionType);
    }

    private void saveDefaultKits(File folder) {
        MyPlugin plugin = MyPlugin.getPlugin(MyPlugin.class);
        String[] defaults = {"iron.yml", "diamond.yml", "example_kit_structure.yml"};
        for (String def : defaults) {
            File file = new File(folder, def);
            if (!file.exists()) {
                plugin.saveResource("kits/" + def, false);
            }
        }
    }

    public Collection<Kit> getKits() {
        return kits.values();
    }

    public Optional<Kit> getKit(String id) {
        return Optional.ofNullable(kits.get(id));
    }
}
