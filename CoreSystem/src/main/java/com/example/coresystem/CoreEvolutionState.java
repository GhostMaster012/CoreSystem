package com.example.coresystem;

import org.bukkit.Material;

public class CoreEvolutionState {
    private final int minLevel;
    private final int maxLevel;
    private final Material itemMaterial;
    private final int customModelData;
    // Optional: String displayName;
    // Optional: List<String> lore;

    public CoreEvolutionState(int minLevel, int maxLevel, Material itemMaterial, int customModelData) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.itemMaterial = itemMaterial;
        this.customModelData = customModelData;
    }

    public boolean matchesLevel(int level) {
        return level >= minLevel && level <= maxLevel;
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    // --- Optional getters for display name and lore if added ---
    // public String getDisplayName() { return displayName; }
    // public List<String> getLore() { return lore; }

    @Override
    public String toString() {
        return "CoreEvolutionState{" +
                "minLevel=" + minLevel +
                ", maxLevel=" + maxLevel +
                ", itemMaterial=" + itemMaterial +
                ", customModelData=" + customModelData +
                '}';
    }
}
