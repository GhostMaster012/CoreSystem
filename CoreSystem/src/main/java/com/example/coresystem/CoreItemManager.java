package com.example.coresystem;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class CoreItemManager {

    private final CoreSystem plugin;
    public final NamespacedKey coreSeedKey;

    public CoreItemManager(CoreSystem plugin) {
        this.plugin = plugin;
        this.coreSeedKey = new NamespacedKey(plugin, "core_seed_item");
    }

    public ItemStack createCoreSeedItem() {
        ConfigManager configManager = plugin.getConfigManager();

        Material material = configManager.getCoreSeedMaterial();
        String name = configManager.getCoreSeedName();
        List<String> lore = configManager.getCoreSeedLore();
        int customModelData = configManager.getCoreSeedCustomModelData();

        ItemStack coreSeed = new ItemStack(material, 1);
        ItemMeta meta = coreSeed.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name); // Name is already color-translated by ConfigManager
            meta.setLore(lore); // Lore is already color-translated by ConfigManager

            if (customModelData != -1) {
                meta.setCustomModelData(customModelData);
            }

            // Add NBT tag to identify this as a special item
            meta.getPersistentDataContainer().set(coreSeedKey, PersistentDataType.BYTE, (byte) 1);
            coreSeed.setItemMeta(meta);
        }
        return coreSeed;
    }

    public boolean isCoreSeedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(coreSeedKey, PersistentDataType.BYTE);
    }
}
