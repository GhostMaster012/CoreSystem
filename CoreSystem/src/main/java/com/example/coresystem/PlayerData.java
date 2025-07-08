package com.example.coresystem;

import org.bukkit.Location;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    private UUID playerUUID;
    private boolean coreActive;
    private Location coreLocation;
    private int level;
    private double xp;
    private double energy;
    private double maxEnergy;
    private double currentHealth;
    private double maxHealth;
    private String archetype;
    private List<String> unlockedSkills;
    private List<String> activeMutations;
    private Map<String, Object> coreBackup;
    private int rebirthCount; // Added rebirthCount

    private double xpFromMobKills;
    private double xpFromPlayerKills;
    private double xpFromFeed;

    private Map<String, Long> skillCooldowns;
    private boolean tutorialCompleted; // Added for tutorial tracking

    public PlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.coreActive = false;
        this.coreLocation = null;
        this.level = 1;
        this.xp = 0;
        this.maxEnergy = CoreSystem.getInstance() != null ? CoreSystem.getInstance().getConfigManager().getDefaultCoreMaxEnergy() : 100.0;
        this.energy = this.maxEnergy;
        this.maxHealth = CoreSystem.getInstance() != null ? CoreSystem.getInstance().getConfigManager().getDefaultCoreMaxHealth() : 100.0;
        this.currentHealth = this.maxHealth;
        this.archetype = null;
        this.unlockedSkills = new ArrayList<>();
        this.activeMutations = new ArrayList<>();
        this.coreBackup = null;
        this.rebirthCount = 0; // Default to 0 rebirths
        this.xpFromMobKills = 0;
        this.xpFromPlayerKills = 0;
        this.xpFromFeed = 0;
        this.skillCooldowns = new ConcurrentHashMap<>();
        this.tutorialCompleted = false; // Default for new players
    }

    public PlayerData(UUID playerUUID, boolean coreActive, Location coreLocation, int level, double xp,
                      double energy, double maxEnergy, double currentHealth, double maxHealth, String archetype,
                      List<String> unlockedSkills, List<String> activeMutations, Map<String, Object> coreBackup,
                      int rebirthCount,
                      double xpFromMobKills, double xpFromPlayerKills, double xpFromFeed, Map<String, Long> skillCooldowns,
                      boolean tutorialCompleted) { // Added tutorialCompleted
        this.playerUUID = playerUUID;
        this.coreActive = coreActive;
        this.coreLocation = coreLocation;
        this.level = level;
        this.xp = xp;
        this.energy = energy;
        this.maxEnergy = maxEnergy;
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.archetype = archetype;
        this.unlockedSkills = unlockedSkills != null ? unlockedSkills : new ArrayList<>();
        this.activeMutations = activeMutations != null ? activeMutations : new ArrayList<>();
        this.coreBackup = coreBackup;
        this.rebirthCount = rebirthCount; // Load rebirthCount
        this.xpFromMobKills = xpFromMobKills;
        this.xpFromPlayerKills = xpFromPlayerKills;
        this.xpFromFeed = xpFromFeed;
        this.skillCooldowns = skillCooldowns != null ? new ConcurrentHashMap<>(skillCooldowns) : new ConcurrentHashMap<>();
        this.tutorialCompleted = tutorialCompleted; // Load tutorialCompleted
    }

    // Getters
    public UUID getPlayerUUID() { return playerUUID; }
    public boolean isCoreActive() { return coreActive; }
    public Location getCoreLocation() { return coreLocation; }
    public int getLevel() { return level; }
    public double getXp() { return xp; }
    public double getEnergy() { return energy; }
    public double getMaxEnergy() { return maxEnergy; }
    public double getCurrentHealth() { return currentHealth; }
    public double getMaxHealth() { return maxHealth; }
    public String getArchetype() { return archetype; }
    public List<String> getUnlockedSkills() { return unlockedSkills; }
    public List<String> getActiveMutations() { return activeMutations; }
    public Map<String, Object> getCoreBackup() { return coreBackup; }
    public boolean hasCoreBackup() { return coreBackup != null && !coreBackup.isEmpty(); }
    public int getRebirthCount() { return rebirthCount; } // Getter for rebirthCount
    public double getXpFromMobKills() { return xpFromMobKills; }
    public double getXpFromPlayerKills() { return xpFromPlayerKills; }
    public double getXpFromFeed() { return xpFromFeed; }
    public Map<String, Long> getSkillCooldowns() { return skillCooldowns; }
    public boolean isTutorialCompleted() { return tutorialCompleted; } // Getter for tutorialCompleted


    // Setters
    public void setCoreActive(boolean coreActive) { this.coreActive = coreActive; }
    public void setCoreLocation(Location coreLocation) { this.coreLocation = coreLocation; }
    public void setLevel(int level) { this.level = level; }
    public void setXp(double xp) { this.xp = xp; }
    public void setEnergy(double energy) { this.energy = Math.max(0, Math.min(energy, this.maxEnergy));}
    public void setMaxEnergy(double maxEnergy) { this.maxEnergy = Math.max(0, maxEnergy); }
    public void setCurrentHealth(double currentHealth) { this.currentHealth = Math.max(0, Math.min(currentHealth, this.maxHealth)); }
    public void setMaxHealth(double maxHealth) { this.maxHealth = Math.max(1, maxHealth); }
    public void setArchetype(String archetype) { this.archetype = archetype; }
    public void setUnlockedSkills(List<String> unlockedSkills) { this.unlockedSkills = unlockedSkills; }
    public void setActiveMutations(List<String> activeMutations) { this.activeMutations = activeMutations; }
    public void setCoreBackup(Map<String, Object> coreBackup) { this.coreBackup = coreBackup; }
    public void setRebirthCount(int rebirthCount) { this.rebirthCount = rebirthCount; } // Setter for rebirthCount
    public void incrementRebirthCount() { this.rebirthCount++; }
    public void setXpFromMobKills(double xpFromMobKills) { this.xpFromMobKills = xpFromMobKills; }
    public void setXpFromPlayerKills(double xpFromPlayerKills) { this.xpFromPlayerKills = xpFromPlayerKills; }
    public void setXpFromFeed(double xpFromFeed) { this.xpFromFeed = xpFromFeed; }
    public void setSkillCooldowns(Map<String, Long> skillCooldowns) { this.skillCooldowns = new ConcurrentHashMap<>(skillCooldowns); }
    public void setTutorialCompleted(boolean tutorialCompleted) { this.tutorialCompleted = tutorialCompleted; } // Setter for tutorialCompleted


    // Convenience methods
    public void addXp(double amount, String reason) {
        this.xp += amount;
        if (reason != null) {
            if (reason.startsWith("MOB_KILL")) this.xpFromMobKills += amount;
            else if (reason.equals("PLAYER_KILL")) this.xpFromPlayerKills += amount;
            else if (reason.equals("CORE_FEED")) this.xpFromFeed += amount;
        }
    }

    public boolean consumeEnergy(double amount) {
        if (this.energy >= amount) {
            this.energy -= amount;
            return true;
        }
        return false;
    }

    public void addEnergy(double amount) {
        this.energy = Math.min(this.maxEnergy, this.energy + amount);
    }

    public void addSkill(String skillId) {
        if (!this.unlockedSkills.contains(skillId)) {
            this.unlockedSkills.add(skillId);
        }
    }

    public void addMutation(String mutationId) {
        if (!this.activeMutations.contains(mutationId)) {
            this.activeMutations.add(mutationId);
        }
    }

    public void takeDamage(double amount) {
        if (!coreActive) return;
        this.currentHealth = Math.max(0, this.currentHealth - amount);
    }

    public void heal(double amount) {
        this.currentHealth = Math.min(this.maxHealth, this.currentHealth + amount);
    }

    public void setSkillOnCooldown(String skillId, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return;
        this.skillCooldowns.put(skillId, System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }

    public boolean isSkillOnCooldown(String skillId) {
        if (!skillCooldowns.containsKey(skillId)) {
            return false;
        }
        return System.currentTimeMillis() < skillCooldowns.get(skillId);
    }

    public long getSkillCooldownRemainingMillis(String skillId) {
        if (!isSkillOnCooldown(skillId)) {
            return 0;
        }
        return skillCooldowns.get(skillId) - System.currentTimeMillis();
    }


    public void createBackup() {
        this.coreBackup = new HashMap<>();
        this.coreBackup.put("level", this.level);
        this.coreBackup.put("xp", this.xp);
        this.coreBackup.put("maxHealth", this.maxHealth);
        this.coreBackup.put("energy", this.energy);
        this.coreBackup.put("maxEnergy", this.maxEnergy);
        this.coreBackup.put("archetype", this.archetype);
        this.coreBackup.put("unlocked_skills", new ArrayList<>(this.unlockedSkills)); // Save currently unlocked skills
        this.coreBackup.put("active_mutations", new ArrayList<>(this.activeMutations)); // Save current mutations
        this.coreBackup.put("rebirthCount", this.rebirthCount); // Save rebirth count
        this.coreBackup.put("xpFromMobKills", this.xpFromMobKills);
        this.coreBackup.put("xpFromPlayerKills", this.xpFromPlayerKills);
        this.coreBackup.put("xpFromFeed", this.xpFromFeed);
        this.coreBackup.put("tutorialCompleted", this.tutorialCompleted); // Backup tutorial status
    }

    @SuppressWarnings("unchecked")
    public boolean restoreFromBackup() {
        if (!hasCoreBackup()) {
            return false;
        }
        this.level = (int) coreBackup.getOrDefault("level", 1);
        this.xp = (double) coreBackup.getOrDefault("xp", 0.0);
        this.maxHealth = (double) coreBackup.getOrDefault("maxHealth", CoreSystem.getInstance().getConfigManager().getDefaultCoreMaxHealth());
        this.currentHealth = this.maxHealth;
        this.maxEnergy = (double) coreBackup.getOrDefault("maxEnergy", CoreSystem.getInstance().getConfigManager().getDefaultCoreMaxEnergy());
        this.energy = (double) coreBackup.getOrDefault("energy", this.maxEnergy);
        this.archetype = (String) coreBackup.get("archetype");
        this.unlockedSkills = (List<String>) coreBackup.getOrDefault("unlocked_skills", new ArrayList<>());
        // Restore active mutations - these are permanent and should persist through destruction/restoration
        this.activeMutations = (List<String>) coreBackup.getOrDefault("active_mutations", new ArrayList<>());
        this.rebirthCount = (int) coreBackup.getOrDefault("rebirthCount", 0);
        this.xpFromMobKills = (double) coreBackup.getOrDefault("xpFromMobKills", 0.0);
        this.xpFromPlayerKills = (double) coreBackup.getOrDefault("xpFromPlayerKills", 0.0);
        this.xpFromFeed = (double) coreBackup.getOrDefault("xpFromFeed", 0.0);
        this.tutorialCompleted = (boolean) coreBackup.getOrDefault("tutorialCompleted", false); // Restore tutorial status

        this.skillCooldowns.clear();
        this.coreBackup = null;
        return true;
    }
}
