package com.test;

import org.bukkit.Material;
import java.util.List;
import java.util.Map;

public record Kit(
    String id,
    String displayName,
    Material icon,
    Map<String, KitItem> armor,
    List<KitItem> items
) {
    public record KitItem(Material material, int amount, Map<String, Integer> enchantments, String potionType) {}
}
