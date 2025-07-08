package com.example.coresystem.skill;

import org.bukkit.Material;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Archetype {
    private final String id; // e.g., AGGRESSIVE
    private final String displayName;
    private final String description;
    private final Material icon;
    private final Map<String, Skill> skills; // Skill ID -> Skill object

    public Archetype(String id, String displayName, String description, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.skills = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public Map<String, Skill> getSkills() {
        return skills;
    }

    public void addSkill(Skill skill) {
        this.skills.put(skill.getId(), skill);
    }

    public Skill getSkill(String skillId) {
        return this.skills.get(skillId);
    }
}
