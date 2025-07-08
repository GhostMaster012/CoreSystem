package com.example.coresystem;

import com.example.coresystem.commands.CoreAdminCommand;
import com.example.coresystem.commands.CoreCommand;
import com.example.coresystem.entity.CoreEntityManager;
import com.example.coresystem.expansion.CoreSystemExpansion;
import com.example.coresystem.listeners.CoreVisualListener;
import com.example.coresystem.listeners.ExperienceListener;
import com.example.coresystem.listeners.GuiListener;
import com.example.coresystem.listeners.PlayerConnectionListener;
import com.example.coresystem.listeners.ProtectionListener;
import com.example.coresystem.listeners.SkillListener;
import com.example.coresystem.mutation.MutationManager; // Import MutationManager
import com.example.coresystem.protection.RegionManager;
import com.example.coresystem.skill.SkillManager;
import com.example.coresystem.tutorial.TutorialManager;
import com.example.coresystem.display.ActionBarManager;
import com.example.coresystem.utils.MessageManager; // Import MessageManager
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoreSystem extends JavaPlugin {

    private static CoreSystem instance;
    private DataManager dataManager;
    private ConfigManager configManager;
    private CoreItemManager coreItemManager;
    private RegionManager regionManager;
    private CoreManager coreManager;
    private CoreEntityManager coreEntityManager;
    private EnergyManager energyManager;
    private SkillManager skillManager;
    private MutationManager mutationManager;
    private TutorialManager tutorialManager;
    private ActionBarManager actionBarManager;
    private MessageManager messageManager; // Added MessageManager

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("CoreSystem is enabling...");

        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        coreItemManager = new CoreItemManager(this);
        regionManager = new RegionManager(this);
        coreEntityManager = new CoreEntityManager(this);
        skillManager = new SkillManager(this);
        mutationManager = new MutationManager(this);
        coreManager = new CoreManager(this);
        energyManager = new EnergyManager(this);
        tutorialManager = new TutorialManager(this);
        actionBarManager = new ActionBarManager(this);
        messageManager = new MessageManager(this); // Initialize MessageManager

        // Register API
        CoreSystemAPI api = new CoreSystemAPIImpl(this);
        getServer().getServicesManager().register(CoreSystemAPI.class, api, this, org.bukkit.plugin.ServicePriority.Normal);
        getLogger().info("CoreSystemAPI registered successfully.");

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ExperienceListener(this), this);
        getServer().getPluginManager().registerEvents(new CoreVisualListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new SkillListener(this), this);
        getServer().getPluginManager().registerEvents(new CoreDamageListener(this), this); // Register CoreDamageListener


        // Register Commands
        CoreCommand coreCommandExecutor = new CoreCommand(this);
        getCommand("core").setExecutor(coreCommandExecutor);
        getCommand("core").setTabCompleter(coreCommandExecutor);

        CoreAdminCommand coreAdminCommandExecutor = new CoreAdminCommand(this);
        getCommand("coreadmin").setExecutor(coreAdminCommandExecutor);
        getCommand("coreadmin").setTabCompleter(coreAdminCommandExecutor);

        // PlaceholderAPI Integration
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CoreSystemExpansion(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI and registered CoreSystem placeholders.");
        } else {
            getLogger().info("PlaceholderAPI not found, CoreSystem placeholders will not be available.");
        }

        Bukkit.getOnlinePlayers().forEach(player -> {
            dataManager.loadAndCachePlayerData(player); // This will also call applyAllMutations via PlayerConnectionListener
        });

        getLogger().info("CoreSystem has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CoreSystem is disabling...");
        if (energyManager != null) {
            energyManager.stopPassiveRegeneration();
        }
        if (actionBarManager != null) { // Stop action bar task
            actionBarManager.stopActionBarTask();
        }
        if (coreEntityManager != null) {
            coreEntityManager.removeAllCoreEntities();
        }

        if (dataManager != null) {
            dataManager.saveAllCachedPlayerData();
        }

        getLogger().info("CoreSystem has been disabled!");
        instance = null;
    }

    public static CoreSystem getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CoreItemManager getCoreItemManager() {
        return coreItemManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public CoreManager getCoreManager() {
        return coreManager;
    }

    public CoreEntityManager getCoreEntityManager() {
        return coreEntityManager;
    }

    public EnergyManager getEnergyManager() {
        return energyManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public TutorialManager getTutorialManager() {
        return mutationManager;
    }

    public ActionBarManager getActionBarManager() {
        return actionBarManager;
    }

    public MessageManager getMessageManager() { // Getter for MessageManager
        return messageManager;
    }
}
