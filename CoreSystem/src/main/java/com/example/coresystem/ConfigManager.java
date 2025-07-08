package com.example.coresystem;

import com.example.coresystem.mutation.Mutation;
import com.example.coresystem.skill.Archetype;
import com.example.coresystem.skill.Skill;
import com.example.coresystem.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ConfigManager {

    private final CoreSystem plugin;
    private FileConfiguration config;
    private FileConfiguration experienceConfig;
    private FileConfiguration evolutionConfig;
    private FileConfiguration skillsConfig;
    private FileConfiguration mutationsConfig; // Added for mutations.yml

    // ... (all other fields)
    private String storageType;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private boolean mysqlUseSSL;
    private int defaultProtectionRadius;
    private double defaultCoreMaxHealth;
    private double defaultCoreMaxEnergy;
    private int maxCoreLevel;
    private boolean coreVulnerableByDefault;
    private boolean passiveEnergyRegenEnabled;
    private double passiveEnergyRegenAmount;
    private int passiveEnergyRegenIntervalSeconds;
    private Map<Material, Integer> energyItems;
    private boolean restorationEnabled;
    private double restorationVaultCost;
    private List<String> restorationItemCostStrings;
    private Map<Material, Integer> restorationItemCosts;
    private long restorationCooldown;
    private Material coreSeedMaterial;
    private String coreSeedName;
    private List<String> coreSeedLore;
    private int coreSeedCustomModelData;
    private long coreClaimCooldown;
    private long coreFeedCooldown;
    private long coreEnergizeCooldown;
    private boolean announceCoreDestruction;
    private String destructionAnnouncementMessage;
    private boolean debugMode;
    private Map<EntityType, Integer> mobKillXP;
    private int defaultMobKillXP;
    private int playerKillXP;
    private int defaultItemSacrificeXP;
    private List<CoreEvolutionState> evolutionStates;
    private Material defaultEvolutionMaterial;
    private CoreEvolutionState fallbackEvolutionState;
    private Map<String, Archetype> archetypes;
    private Map<String, Mutation> mutations; // Added for mutations


    public ConfigManager(CoreSystem plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        loadMainConfigValues();

        loadExperienceConfig();
        loadExperienceValues();

        loadEvolutionConfig();
        loadEvolutionValues();

        loadSkillsConfig();
        loadSkillSystemValues();

        loadMutationsConfig();
        loadMutationValues();

        loadWorldGuardSettings();
        loadCoreVulnerabilitySettings();
        loadActionBarSettings(); // Load Action Bar Settings
    }

    private void loadMainConfigValues() {
        // ... (same as before)
        storageType = config.getString("storage.type", "YAML").toUpperCase();
        mysqlHost = config.getString("storage.mysql.host", "localhost");
        mysqlPort = config.getInt("storage.mysql.port", 3306);
        mysqlDatabase = config.getString("storage.mysql.database", "coresystem");
        mysqlUsername = config.getString("storage.mysql.username", "user");
        mysqlPassword = config.getString("storage.mysql.password", "password");
        mysqlUseSSL = config.getBoolean("storage.mysql.useSSL", false);
        defaultProtectionRadius = config.getInt("default-protection-radius", 5);
        defaultCoreMaxHealth = config.getDouble("default-core-max-health", 100.0);
        defaultCoreMaxEnergy = config.getDouble("default-core-max-energy", 100.0);
        maxCoreLevel = config.getInt("max-core-level", 20);
        coreVulnerableByDefault = config.getBoolean("core-vulnerability.vulnerable-by-default", false);
        passiveEnergyRegenEnabled = config.getBoolean("energy-regeneration.passive-enabled", true);
        passiveEnergyRegenAmount = config.getDouble("energy-regeneration.passive-amount", 1.0);
        passiveEnergyRegenIntervalSeconds = config.getInt("energy-regeneration.passive-interval-seconds", 5);
        energyItems = new HashMap<>();
        ConfigurationSection energyItemsSection = config.getConfigurationSection("energy-items");
        if (energyItemsSection != null) {
            for (String key : energyItemsSection.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    int amount = energyItemsSection.getInt(key);
                    if (amount > 0) energyItems.put(mat, amount);
                } catch (IllegalArgumentException e) { plugin.getLogger().warning("Invalid material in energy-items: " + key); }
            }
        }
        restorationEnabled = config.getBoolean("core-restoration.enabled", true);
        restorationVaultCost = config.getDouble("core-restoration.vault-economy-cost", 1000.0);
        restorationItemCostStrings = config.getStringList("core-restoration.item-costs");
        parseRestorationItemCosts();
        restorationCooldown = config.getLong("core-restoration.cooldown", 600L);
        String seedMaterialName = config.getString("core-seed-item.material", "HEART_OF_THE_SEA");
        try { coreSeedMaterial = Material.valueOf(seedMaterialName.toUpperCase()); } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material for core-seed-item.material: " + seedMaterialName + ". Defaulting to HEART_OF_THE_SEA.");
            coreSeedMaterial = Material.HEART_OF_THE_SEA;
        }
        coreSeedName = config.getString("core-seed-item.name", "&d&lCore Seed");
        coreSeedLore = config.getStringList("core-seed-item.lore");
        coreSeedCustomModelData = config.getInt("core-seed-item.custom-model-data", -1);
        coreClaimCooldown = config.getLong("command-cooldowns.core-claim", 3600L);
        coreFeedCooldown = config.getLong("command-cooldowns.core-feed", 60L);
        coreEnergizeCooldown = config.getLong("command-cooldowns.core-energize", 30L);
        announceCoreDestruction = config.getBoolean("announce-core-destruction", true);
        destructionAnnouncementMessage = config.getString("destruction-announcement-message", "&c&lATTENTION! {player}''s Core has been destroyed!");
        debugMode = config.getBoolean("debug-mode", false);
        if(debugMode) plugin.getLogger().info("Debug mode enabled from config.yml.");
    }

    private void loadExperienceConfig() {
        // ... (same as before)
        File experienceFile = new File(plugin.getDataFolder(), "experience.yml");
        if (!experienceFile.exists()) plugin.saveResource("experience.yml", false);
        experienceConfig = YamlConfiguration.loadConfiguration(experienceFile);
        InputStream defaultConfigStream = plugin.getResource("experience.yml");
        if (defaultConfigStream != null) experienceConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream)));
    }

    private void loadExperienceValues() {
        // ... (same as before)
        mobKillXP = new HashMap<>();
        ConfigurationSection mobKillSection = experienceConfig.getConfigurationSection("mob-kills");
        if (mobKillSection != null) {
            for (String key : mobKillSection.getKeys(false)) {
                if (key.equalsIgnoreCase("DEFAULT")) continue;
                try { EntityType type = EntityType.valueOf(key.toUpperCase()); mobKillXP.put(type, mobKillSection.getInt(key)); }
                catch (IllegalArgumentException e) { plugin.getLogger().warning("Invalid EntityType in experience.yml: " + key); }
            }
        }
        defaultMobKillXP = experienceConfig.getInt("mob-kills.DEFAULT", 1);
        playerKillXP = experienceConfig.getInt("player-kill", 50);
        defaultItemSacrificeXP = experienceConfig.getInt("core-feed.DEFAULT_ITEM_SACRIFICE_XP", 5);
        if (debugMode) plugin.getLogger().info("Loaded " + mobKillXP.size() + " specific mob XP values. Default mob XP: " + defaultMobKillXP + ". Player kill XP: " + playerKillXP + ". Feed XP: " + defaultItemSacrificeXP);
    }

    private void loadEvolutionConfig() {
        // ... (same as before)
        File evoFile = new File(plugin.getDataFolder(), "evolution.yml");
        if (!evoFile.exists()) plugin.saveResource("evolution.yml", false);
        evolutionConfig = YamlConfiguration.loadConfiguration(evoFile);
        InputStream defaultConfigStream = plugin.getResource("evolution.yml");
        if (defaultConfigStream != null) evolutionConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream)));
    }

    private void loadEvolutionValues() {
        // ... (same as before)
        evolutionStates = new ArrayList<>();
        try { defaultEvolutionMaterial = Material.valueOf(evolutionConfig.getString("default_item_material", "PAPER").toUpperCase()); }
        catch (IllegalArgumentException e) { plugin.getLogger().warning("Invalid default_item_material in evolution.yml. Defaulting to PAPER."); defaultEvolutionMaterial = Material.PAPER; }
        List<Map<?, ?>> evolutionList = evolutionConfig.getMapList("evolutions");
        for (Map<?, ?> evoMap : evolutionList) {
            String levelRangeStr = (String) evoMap.get("level_range");
            String materialStr = (String) evoMap.getOrDefault("item_material", defaultEvolutionMaterial.name());
            int cmd = (Integer) evoMap.get("custom_model_data");
            String[] levels = levelRangeStr.split("-");
            if (levels.length != 2) { plugin.getLogger().warning("Invalid level_range format in evolution.yml: " + levelRangeStr); continue; }
            try {
                int minLvl = Integer.parseInt(levels[0]); int maxLvl = Integer.parseInt(levels[1]);
                Material mat = Material.valueOf(materialStr.toUpperCase());
                evolutionStates.add(new CoreEvolutionState(minLvl, maxLvl, mat, cmd));
            } catch (NumberFormatException e) { plugin.getLogger().warning("Invalid number in level_range in evolution.yml: " + levelRangeStr); }
            catch (IllegalArgumentException e) { plugin.getLogger().warning("Invalid material in evolution.yml: " + materialStr); }
        }
        evolutionStates.sort((s1, s2) -> Integer.compare(s1.getMinLevel(), s2.getMinLevel()));
        ConfigurationSection fallbackSection = evolutionConfig.getConfigurationSection("fallback_evolution");
        if (fallbackSection != null) {
            Material fallbackMat; try { fallbackMat = Material.valueOf(fallbackSection.getString("item_material", "STONE").toUpperCase()); } catch (IllegalArgumentException e) { fallbackMat = Material.STONE; }
            int fallbackCmd = fallbackSection.getInt("custom_model_data", 0);
            fallbackEvolutionState = new CoreEvolutionState(0, Integer.MAX_VALUE, fallbackMat, fallbackCmd);
        } else { fallbackEvolutionState = new CoreEvolutionState(0, Integer.MAX_VALUE, Material.STONE, 0); }
        if(debugMode) plugin.getLogger().info("Loaded " + evolutionStates.size() + " core evolution states.");
    }

    private void loadSkillsConfig() {
        // ... (same as before)
        File skillsFile = new File(plugin.getDataFolder(), "skills.yml");
        if (!skillsFile.exists()) plugin.saveResource("skills.yml", false);
        skillsConfig = YamlConfiguration.loadConfiguration(skillsFile);
        InputStream defaultConfigStream = plugin.getResource("skills.yml");
        if (defaultConfigStream != null) skillsConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream)));
    }

    private void loadSkillSystemValues() {
        // ... (same as before)
        archetypes = new HashMap<>();
        ConfigurationSection archetypesSection = skillsConfig.getConfigurationSection("archetypes");
        if (archetypesSection == null) { plugin.getLogger().warning("No 'archetypes' section found in skills.yml."); return; }
        for (String archetypeId : archetypesSection.getKeys(false)) {
            ConfigurationSection archSection = archetypesSection.getConfigurationSection(archetypeId); if (archSection == null) continue;
            String displayName = ChatUtils.color(archSection.getString("display_name", "&7Unnamed Archetype"));
            String description = ChatUtils.color(archSection.getString("description", "&8No description."));
            Material icon = Material.BARRIER; try { icon = Material.valueOf(archSection.getString("icon", "BARRIER").toUpperCase()); } catch (IllegalArgumentException e) { plugin.getLogger().warning("Invalid icon material for archetype " + archetypeId + ": " + archSection.getString("icon"));}
            Archetype archetype = new Archetype(archetypeId.toUpperCase(), displayName, description, icon);
            ConfigurationSection skillsSection = archSection.getConfigurationSection("skills");
            if (skillsSection != null) {
                for (String skillId : skillsSection.getKeys(false)) {
                    ConfigurationSection skillCfg = skillsSection.getConfigurationSection(skillId); if (skillCfg == null) continue;
                    String skillName = ChatUtils.color(skillCfg.getString("name", "&8Unnamed Skill"));
                    List<String> skillDesc = skillCfg.getStringList("description").stream().map(ChatUtils::color).collect(Collectors.toList());
                    if (skillDesc.isEmpty()) skillDesc.add(ChatUtils.color("&7No skill description."));
                    int reqLevel = skillCfg.getInt("required_level", 1); double energyCost = skillCfg.getDouble("energy_cost", 0);
                    int cooldown = skillCfg.getInt("cooldown", 0); List<String> prerequisites = skillCfg.getStringList("prerequisites");
                    String effectType = skillCfg.getString("type", "NONE");
                    Map<String, String> effectDetails = new HashMap<>(); ConfigurationSection detailsSection = skillCfg.getConfigurationSection("effect_details");
                    if (detailsSection != null) { for (String detailKey : detailsSection.getKeys(false)) { Object value = detailsSection.get(detailKey); if (value != null) effectDetails.put(detailKey, value.toString()); } }
                    Skill skill = new Skill(skillId, skillName, skillDesc, reqLevel, energyCost, cooldown, prerequisites, effectType, effectDetails);
                    archetype.addSkill(skill);
                }
            }
            archetypes.put(archetype.getId(), archetype);
        }
        if (debugMode) plugin.getLogger().info("Loaded " + archetypes.size() + " archetypes with their skills.");
    }

    private void loadMutationsConfig() {
        File mutationsFile = new File(plugin.getDataFolder(), "mutations.yml");
        if (!mutationsFile.exists()) {
            plugin.saveResource("mutations.yml", false);
        }
        mutationsConfig = YamlConfiguration.loadConfiguration(mutationsFile);
        InputStream defaultConfigStream = plugin.getResource("mutations.yml");
        if (defaultConfigStream != null) {
            mutationsConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream)));
        }
    }

    private void loadMutationValues() {
        mutations = new HashMap<>();
        ConfigurationSection mutationsSection = mutationsConfig.getConfigurationSection("mutations");
        if (mutationsSection == null) {
            plugin.getLogger().warning("No 'mutations' section found in mutations.yml.");
            return;
        }
        for (String mutationId : mutationsSection.getKeys(false)) {
            ConfigurationSection mutSection = mutationsSection.getConfigurationSection(mutationId);
            if (mutSection == null) continue;

            String name = ChatUtils.color(mutSection.getString("name", "&8Unknown Mutation"));
            String description = ChatUtils.color(mutSection.getString("description", "&7No description."));
            String type = mutSection.getString("type", "NONE").toUpperCase();

            Map<String, Object> effectDetails = new HashMap<>();
            ConfigurationSection detailsSection = mutSection.getConfigurationSection("effect_details");
            if (detailsSection != null) {
                for (String key : detailsSection.getKeys(false)) {
                    effectDetails.put(key, detailsSection.get(key)); // Store raw object
                }
            }
            Mutation mutation = new Mutation(mutationId.toUpperCase(), name, description, type, effectDetails);
            mutations.put(mutation.getId(), mutation);
        }
        if (debugMode) {
            plugin.getLogger().info("Loaded " + mutations.size() + " mutations.");
        }
    }


    private void parseRestorationItemCosts() {
        // ... (same as before)
        restorationItemCosts = new HashMap<>();
        if (restorationItemCostStrings == null) return;
        for (String itemString : restorationItemCostStrings) {
            String[] parts = itemString.split(":");
            if (parts.length == 2) {
                try {
                    Material material = Material.valueOf(parts[0].toUpperCase());
                    int amount = Integer.parseInt(parts[1]);
                    if (amount > 0) restorationItemCosts.put(material, amount);
                    else plugin.getLogger().warning("Invalid amount for restoration item cost: " + itemString);
                } catch (IllegalArgumentException e) { plugin.getLogger().warning("Invalid material for restoration item cost: " + parts[0]); }
            } else { plugin.getLogger().warning("Invalid format for restoration item cost: " + itemString + ". Expected MATERIAL_NAME:AMOUNT"); }
        }
    }

    // Getters
    public String getStorageType() { return storageType; }
    public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    public String getMysqlDatabase() { return mysqlDatabase; }
    public String getMysqlUsername() { return mysqlUsername; }
    public String getMysqlPassword() { return mysqlPassword; }
    public boolean isMysqlUseSSL() { return mysqlUseSSL; }

    public int getDefaultProtectionRadius() { return defaultProtectionRadius; }
    public double getDefaultCoreMaxHealth() { return defaultCoreMaxHealth; }
    public double getDefaultCoreMaxEnergy() { return defaultCoreMaxEnergy; }
    public int getMaxCoreLevel() { return maxCoreLevel; }
    public boolean isCoreVulnerableByDefault() { return coreVulnerableByDefault; }

    public boolean isPassiveEnergyRegenEnabled() { return passiveEnergyRegenEnabled; }
    public double getPassiveEnergyRegenAmount() { return passiveEnergyRegenAmount; }
    public int getPassiveEnergyRegenIntervalSeconds() { return passiveEnergyRegenIntervalSeconds; }
    public Map<Material, Integer> getEnergyItems() { return energyItems; }


    public boolean isRestorationEnabled() { return restorationEnabled; }
    public double getRestorationVaultCost() { return restorationVaultCost; }
    public Map<Material, Integer> getRestorationItemCosts() { return restorationItemCosts; }
    public long getRestorationCooldown() { return restorationCooldown; }

    public Material getCoreSeedMaterial() { return coreSeedMaterial; }
    public String getCoreSeedName() { return coreSeedName; }
    public List<String> getCoreSeedLore() {
        return coreSeedLore.stream().map(ChatUtils::color).collect(Collectors.toList());
    }
    public int getCoreSeedCustomModelData() { return coreSeedCustomModelData; }

    public long getCoreClaimCooldown() { return coreClaimCooldown; }
    public long getCoreFeedCooldown() { return coreFeedCooldown; }
    public long getCoreEnergizeCooldown() { return coreEnergizeCooldown; }
    public boolean isAnnounceCoreDestruction() { return announceCoreDestruction; }
    public String getDestructionAnnouncementMessage() { return destructionAnnouncementMessage; }
    public boolean isDebugMode() { return debugMode; }

    public int getXpForMobKill(EntityType type) {
        return mobKillXP.getOrDefault(type, defaultMobKillXP);
    }
    public int getPlayerKillXP() { return playerKillXP; }
    public int getDefaultItemSacrificeXP() { return defaultItemSacrificeXP; }

    public CoreEvolutionState getEvolutionStateForLevel(int level) {
        for (CoreEvolutionState state : evolutionStates) {
            if (state.matchesLevel(level)) {
                return state;
            }
        }
        plugin.getLogger().log(Level.WARNING, "No specific evolution state found for level " + level + ". Using fallback.");
        return fallbackEvolutionState;
    }

    public Map<String, Archetype> getArchetypes() {
        return Collections.unmodifiableMap(archetypes);
    }

    public Archetype getArchetype(String id) {
        return archetypes.get(id.toUpperCase());
    }

    public Skill getSkill(String skillId) {
        for (Archetype archetype : archetypes.values()) {
            Skill skill = archetype.getSkill(skillId);
            if (skill != null) {
                return skill;
            }
        }
        return null;
    }

    // Mutation Getters
    public Map<String, Mutation> getMutations() {
        return Collections.unmodifiableMap(mutations);
    }

    public Mutation getMutation(String id) {
        return mutations.get(id.toUpperCase());
    }

    // WorldGuard settings
    private boolean worldGuardIntegrationEnabled;
    private Map<String, String> worldGuardFlags;

    // Core Vulnerability settings
    private boolean coreVulnerableByDefaultSetting; // Renamed to avoid conflict with getter
    private boolean allowDamageAnytime;
    private boolean placeholderConditionAlwaysVulnerable;
    private List<String> coreDamageSources;

    // Action Bar settings
    private boolean actionBarEnabled;
    private long actionBarUpdateIntervalTicks;
    private String actionBarFormat;
    private String actionBarNoCooldownsText;


    private void loadWorldGuardSettings() {
        worldGuardIntegrationEnabled = config.getBoolean("worldguard.enabled", true);
        worldGuardFlags = new HashMap<>();
        ConfigurationSection wgFlagsSection = config.getConfigurationSection("worldguard.default-flags");
        if (wgFlagsSection != null) {
            for (String flagKey : wgFlagsSection.getKeys(false)) {
                worldGuardFlags.put(flagKey, wgFlagsSection.getString(flagKey, "DENY"));
            }
        } else {
            worldGuardFlags.put("block-break", "DENY");
            worldGuardFlags.put("block-place", "DENY");
            plugin.getLogger().info("WorldGuard default-flags section missing in config.yml, using basic defaults.");
        }
        if(debugMode && worldGuardIntegrationEnabled) plugin.getLogger().info("Loaded " + worldGuardFlags.size() + " WorldGuard default flags.");
    }

    private void loadCoreVulnerabilitySettings() {
        coreVulnerableByDefaultSetting = config.getBoolean("core-vulnerability.vulnerable-by-default", false);
        allowDamageAnytime = config.getBoolean("core-vulnerability.allow-damage-anytime", false);
        placeholderConditionAlwaysVulnerable = config.getBoolean("core-vulnerability.conditions.placeholder-condition-always-vulnerable", false);
        coreDamageSources = config.getStringList("core-vulnerability.damage-sources");
        // Convert to uppercase for easier checking
        coreDamageSources = coreDamageSources.stream().map(String::toUpperCase).collect(Collectors.toList());

        if (debugMode) {
            plugin.getLogger().info("Core Vulnerability: vulnerable-by-default=" + coreVulnerableByDefaultSetting +
                                     ", allow-damage-anytime=" + allowDamageAnytime +
                                     ", placeholder-always-vulnerable=" + placeholderConditionAlwaysVulnerable +
                                     ", damage-sources=" + coreDamageSources.toString());
        }
    }


    public boolean isWorldGuardIntegrationEnabled() { return worldGuardIntegrationEnabled; }
    public Map<String, String> getWorldGuardFlags() { return Collections.unmodifiableMap(worldGuardFlags); }

    public boolean isCoreVulnerableByDefault() { return coreVulnerableByDefaultSetting; } // Getter for the setting
    public boolean isAllowDamageAnytime() { return allowDamageAnytime; }
    public boolean isPlaceholderConditionAlwaysVulnerable() { return placeholderConditionAlwaysVulnerable; }
    public List<String> getCoreDamageSources() { return Collections.unmodifiableList(coreDamageSources); }

    public boolean isActionBarEnabled() { return actionBarEnabled; }
    public long getActionBarUpdateIntervalTicks() { return actionBarUpdateIntervalTicks; }
    public String getActionBarFormat() { return actionBarFormat; }
    public String getActionBarNoCooldownsText() { return actionBarNoCooldownsText; }
}
