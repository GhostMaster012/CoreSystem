package com.example.coresystem;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DataManager {

    private final CoreSystem plugin;
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private File playerDataFolder;

    public DataManager(CoreSystem plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!this.playerDataFolder.exists()) {
            if (!this.playerDataFolder.mkdirs()) {
                plugin.getLogger().severe("Could not create playerdata folder!");
            }
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }
        return loadPlayerDataFromFile(uuid);
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public void loadAndCachePlayerData(Player player) {
        PlayerData data = loadPlayerDataFromFile(player.getUniqueId());
        if (data.isCoreActive() && data.getCoreLocation() != null) {
            plugin.getRegionManager().loadRegionForPlayerData(data);
            plugin.getCoreEntityManager().ensureCoreEntityExists(player, data.getCoreLocation(), data.getLevel());
        }
        // Apply mutations after loading player data
        plugin.getMutationManager().applyAllMutations(player, data);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Loaded and cached PlayerData for " + player.getName());
        }
    }

    public void saveAndRemovePlayerData(Player player) {
        PlayerData data = playerDataCache.remove(player.getUniqueId());
        if (data != null) {
            savePlayerDataToFile(data);
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Saved and removed PlayerData for " + player.getName() + " from cache.");
            }
        }
    }

    private PlayerData loadPlayerDataFromFile(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }

        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        if (!playerFile.exists()) {
            PlayerData newPlayerData = new PlayerData(uuid);
            playerDataCache.put(uuid, newPlayerData);
            return newPlayerData;
        }

        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        boolean coreActive = playerConfig.getBoolean("core.active", false);
        Location coreLocation = null;
        if (playerConfig.isSet("core.location")) {
            String worldName = playerConfig.getString("core.location.world");
            double x = playerConfig.getDouble("core.location.x");
            double y = playerConfig.getDouble("core.location.y");
            double z = playerConfig.getDouble("core.location.z");
            float yaw = (float) playerConfig.getDouble("core.location.yaw", 0.0);
            float pitch = (float) playerConfig.getDouble("core.location.pitch", 0.0);
            if (worldName != null && Bukkit.getWorld(worldName) != null) {
                coreLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
            } else if (worldName != null) {
                plugin.getLogger().warning("World '" + worldName + "' for player " + uuid + " core location not found. Core location will be null.");
            }
        }

        int level = playerConfig.getInt("core.level", 1);
        double xp = playerConfig.getDouble("core.xp", 0.0);
        double maxEnergy = playerConfig.getDouble("core.maxEnergy", plugin.getConfigManager().getDefaultCoreMaxEnergy());
        double energy = playerConfig.getDouble("core.energy", maxEnergy);
        double maxHealth = playerConfig.getDouble("core.maxHealth", plugin.getConfigManager().getDefaultCoreMaxHealth());
        double currentHealth = playerConfig.getDouble("core.currentHealth", maxHealth);


        String archetype = playerConfig.getString("core.archetype");
        List<String> unlockedSkills = playerConfig.getStringList("core.unlocked_skills");
        List<String> activeMutations = playerConfig.getStringList("core.active_mutations"); // Load mutations
        int rebirthCount = playerConfig.getInt("core.rebirthCount", 0); // Load rebirth count


        Map<String, Object> coreBackup = null;
        if (playerConfig.isConfigurationSection("core.backup")) {
            ConfigurationSection backupSection = playerConfig.getConfigurationSection("core.backup");
            if (backupSection != null) {
                coreBackup = new HashMap<>(backupSection.getValues(true));
            }
        }

        double xpFromMobKills = playerConfig.getDouble("core.xp-stats.mob-kills", 0.0);
        double xpFromPlayerKills = playerConfig.getDouble("core.xp-stats.player-kills", 0.0);
        double xpFromFeed = playerConfig.getDouble("core.xp-stats.feed", 0.0);
        boolean tutorialCompleted = playerConfig.getBoolean("core.tutorialCompleted", false); // Load tutorialCompleted

        Map<String, Long> skillCooldowns = new HashMap<>();
        ConfigurationSection cooldownSection = playerConfig.getConfigurationSection("core.skill-cooldowns");
        if (cooldownSection != null) {
            for (String skillId : cooldownSection.getKeys(false)) {
                skillCooldowns.put(skillId, cooldownSection.getLong(skillId));
            }
        }

        PlayerData loadedData = new PlayerData(uuid, coreActive, coreLocation, level, xp, energy,
                                               maxEnergy, currentHealth, maxHealth, archetype, unlockedSkills,
                                               activeMutations, coreBackup, rebirthCount,
                                               xpFromMobKills, xpFromPlayerKills, xpFromFeed, skillCooldowns,
                                               tutorialCompleted); // Pass tutorialCompleted
        playerDataCache.put(uuid, loadedData);
        return loadedData;
    }

    public void savePlayerData(PlayerData playerData) {
        savePlayerDataToFile(playerData);
    }

    private void savePlayerDataToFile(PlayerData playerData) {
        if (playerData == null) return;

        File playerFile = new File(playerDataFolder, playerData.getPlayerUUID().toString() + ".yml");
        YamlConfiguration playerConfig = new YamlConfiguration();

        playerConfig.set("core.active", playerData.isCoreActive());
        if (playerData.getCoreLocation() != null) {
            playerConfig.set("core.location.world", playerData.getCoreLocation().getWorld().getName());
            playerConfig.set("core.location.x", playerData.getCoreLocation().getX());
            playerConfig.set("core.location.y", playerData.getCoreLocation().getY());
            playerConfig.set("core.location.z", playerData.getCoreLocation().getZ());
            playerConfig.set("core.location.yaw", playerData.getCoreLocation().getYaw());
            playerConfig.set("core.location.pitch", playerData.getCoreLocation().getPitch());
        } else {
            playerConfig.set("core.location", null);
        }
        playerConfig.set("core.level", playerData.getLevel());
        playerConfig.set("core.xp", playerData.getXp());
        playerConfig.set("core.energy", playerData.getEnergy());
        playerConfig.set("core.maxEnergy", playerData.getMaxEnergy());
        playerConfig.set("core.maxHealth", playerData.getMaxHealth());
        playerConfig.set("core.currentHealth", playerData.getCurrentHealth());
        playerConfig.set("core.archetype", playerData.getArchetype());
        playerConfig.set("core.unlocked_skills", playerData.getUnlockedSkills() != null ? playerData.getUnlockedSkills() : new ArrayList<>());
        playerConfig.set("core.active_mutations", playerData.getActiveMutations() != null ? playerData.getActiveMutations() : new ArrayList<>()); // Save mutations
        playerConfig.set("core.rebirthCount", playerData.getRebirthCount()); // Save rebirth count


        if (playerData.hasCoreBackup()) {
            playerConfig.set("core.backup", playerData.getCoreBackup());
        } else {
            playerConfig.set("core.backup", null);
        }

        playerConfig.set("core.xp-stats.mob-kills", playerData.getXpFromMobKills());
        playerConfig.set("core.xp-stats.player-kills", playerData.getXpFromPlayerKills());
        playerConfig.set("core.xp-stats.feed", playerData.getXpFromFeed());
        playerConfig.set("core.tutorialCompleted", playerData.isTutorialCompleted()); // Save tutorialCompleted

        Map<String, Long> cooldownsToSave = new HashMap<>(playerData.getSkillCooldowns());
        long currentTime = System.currentTimeMillis();
        cooldownsToSave.entrySet().removeIf(entry -> entry.getValue() < currentTime);
        playerConfig.set("core.skill-cooldowns", cooldownsToSave.isEmpty() ? null : cooldownsToSave);


        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data for " + playerData.getPlayerUUID(), e);
        }
    }

    public void saveAllCachedPlayerData() {
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Attempting to save data for " + playerDataCache.size() + " cached players...");
        }
        for (PlayerData pd : new ArrayList<>(playerDataCache.values())) {
            savePlayerDataToFile(pd);
        }
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Finished saving cached player data.");
        }
    }

    public Collection<PlayerData> getAllLoadedPlayerData() {
        return new ArrayList<>(playerDataCache.values());
    }
}
