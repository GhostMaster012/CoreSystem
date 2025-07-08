package com.example.coresystem.skill;

import org.bukkit.Material; // For icon later
import java.util.List;
import java.util.Map;

public class Skill {
    private final String id;
    private final String name;
    private final List<String> description;
    private final int requiredLevel;
    private final double energyCost;
    private final int cooldownSeconds; // Cooldown in seconds
    private final List<String> prerequisites; // List of skill IDs
    // Placeholder for actual effect implementation
    private final String effectType; // e.g., "DAMAGE_BOOST", "POTION_EFFECT", "MOVEMENT"
    private final Map<String, String> effectDetails; // e.g., {"potion":"SPEED:0:10", "value":"5"}

    public Skill(String id, String name, List<String> description, int requiredLevel,
                 double energyCost, int cooldownSeconds, List<String> prerequisites,
                 String effectType, Map<String, String> effectDetails) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requiredLevel = requiredLevel;
        this.energyCost = energyCost;
        this.cooldownSeconds = cooldownSeconds;
        this.prerequisites = prerequisites;
        this.effectType = effectType;
        this.effectDetails = effectDetails;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getDescription() {
        return description;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public double getEnergyCost() {
        return energyCost;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public long getCooldownMillis() {
        return cooldownSeconds * 1000L;
    }

    public List<String> getPrerequisites() {
        return prerequisites;
    }

    public String getEffectType() {
        return effectType;
    }

    public Map<String, String> getEffectDetails() {
        return effectDetails;
    }

    // Convenience method to get a specific detail
    public String getEffectDetail(String key) {
        return effectDetails.get(key);
    }
}
