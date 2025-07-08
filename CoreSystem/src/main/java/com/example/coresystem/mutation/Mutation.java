package com.example.coresystem.mutation;

import org.bukkit.Material;
import java.util.Map;

public class Mutation {
    private final String id;
    private final String name;
    private final String description;
    private final String type; // e.g., "CORE_MAX_HEALTH_ADD", "CORE_XP_MULTIPLIER"
    private final Map<String, Object> effectDetails; // Using Object for flexibility (e.g., double, string)
    // private final Material icon; // Optional for GUI display

    public Mutation(String id, String name, String description, String type, Map<String, Object> effectDetails) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.effectDetails = effectDetails;
        // this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getEffectDetails() {
        return effectDetails;
    }

    @SuppressWarnings("unchecked") // Use with caution, ensure type safety at usage
    public <T> T getEffectDetail(String key, T defaultValue) {
        return (T) effectDetails.getOrDefault(key, defaultValue);
    }

    public double getEffectDetailDouble(String key, double defaultValue) {
        Object value = effectDetails.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value != null ? Double.parseDouble(value.toString()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
     public int getEffectDetailInt(String key, int defaultValue) {
        Object value = effectDetails.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    // public Material getIcon() { return icon; }
}
