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
        if (files == null) return;

        for (File file : files) {
            String id = file.getName().replace(".yml", "");
            if (id.equals("example_kit_structure")) continue;

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String displayName = config.getString("display-name", id);
            Material icon = Material.valueOf(config.getString("icon", "IRON_SWORD"));

            Map<String, Kit.KitItem> armor = new HashMap<>();
            ConfigurationSection armorSection = config.getConfigurationSection("armor");
            if (armorSection != null) {
                for (String slot : armorSection.getKeys(false)) {
                    armor.put(slot.toLowerCase(), parseKitItem(armorSection.getConfigurationSection(slot)));
                }
            }

            List<Kit.KitItem> items = new ArrayList<>();
            List<Map<?, ?>> itemMaps = config.getMapList("items");
            for (Map<?, ?> map : itemMaps) {
                items.add(parseKitItem(map));
            }

            kits.put(id, new Kit(id, displayName, icon, armor, items));
        }
    }

    private Kit.KitItem parseKitItem(ConfigurationSection section) {
        if (section == null) return null;
        Material mat = Material.valueOf(section.getString("material", "AIR"));
        int amount = section.getInt("amount", 1);
        String potionType = section.getString("potion-type");
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
        Material mat = Material.valueOf((String) map.get("material"));
        Object amountObj = map.get("amount");
        int amount = (amountObj instanceof Integer) ? (int) amountObj : 1;
        String potionType = (String) map.get("potion-type");
        Map<String, Integer> enchs = new HashMap<>();
        if (map.containsKey("enchantments")) {
            Map<?, ?> enchMap = (Map<?, ?>) map.get("enchantments");
            for (Map.Entry<?, ?> entry : enchMap.entrySet()) {
                enchs.put(((String) entry.getKey()).toLowerCase(), (Integer) entry.getValue());
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
