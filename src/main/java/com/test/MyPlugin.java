package com.test;

import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        KitManager.getInstance().loadKits();
        getCommand("lpvp").setExecutor(new CommandLpvp());
        getServer().getPluginManager().registerEvents(new PVPListener(), this);
        getLogger().info("L-s-KitPVP has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("L-s-KitPVP has been disabled!");
    }

    public org.bukkit.Location getPVPSpawn() {
        if (!getConfig().contains("pvp-spawn")) {
            return getServer().getWorlds().get(0).getSpawnLocation();
        }
        return getConfig().getLocation("pvp-spawn");
    }

    public void setPVPSpawn(org.bukkit.Location location) {
        getConfig().set("pvp-spawn", location);
        saveConfig();
    }
}
