package com.example.coresystem;

import com.example.coresystem.entity.CoreEntityManager;
import com.example.coresystem.events.CoreGainXPEvent;
import com.example.coresystem.events.CoreLevelUpEvent;
import com.example.coresystem.mutation.MutationManager;
import com.example.coresystem.utils.ChatUtils;
import com.example.coresystem.utils.ExperienceManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CoreManager {

    private final CoreSystem plugin;
    private final CoreEntityManager coreEntityManager;
    private final MutationManager mutationManager; // Added
    private Economy vaultEconomy = null;
    private final Map<UUID, Long> restoreCooldowns = new HashMap<>();
    private final DecimalFormat df = new DecimalFormat("#.#");


    public CoreManager(CoreSystem plugin) {
        this.plugin = plugin;
        this.coreEntityManager = plugin.getCoreEntityManager();
        this.mutationManager = plugin.getMutationManager();
        setupEconomy();
    }

    // --- Vulnerability Logic ---
    public boolean isCoreVulnerable(UUID coreOwnerUUID, @Nullable Player attacker, @Nullable Entity directDamager, EntityDamageEvent.DamageCause cause) {
        ConfigManager config = plugin.getConfigManager();
        PlayerData coreOwnerData = plugin.getDataManager().getPlayerData(coreOwnerUUID);

        if (!coreOwnerData.isCoreActive()) return false;

        if (config.isAllowDamageAnytime()) return true; // Overrides all other conditions

        // Check damage sources
        List<String> allowedSources = config.getCoreDamageSources();
        if (!allowedSources.isEmpty()) {
            boolean sourceAllowed = false;
            String causeName = cause.name().toUpperCase();
            if (allowedSources.contains(causeName)) {
                sourceAllowed = true;
            } else if (attacker != null && allowedSources.contains("PLAYER")) {
                sourceAllowed = true;
            } else if (directDamager != null) {
                // Check for specific entity types like TNT, CREEPER, or generic MOB
                String damagerTypeName = directDamager.getType().name().toUpperCase();
                if (allowedSources.contains(damagerTypeName)) sourceAllowed = true;
                else if (directDamager instanceof LivingEntity && !(directDamager instanceof Player) && allowedSources.contains("MOB")) sourceAllowed = true;
                else if (directDamager.getType() == EntityType.PRIMED_TNT && allowedSources.contains("TNT")) sourceAllowed = true;
                else if (directDamager.getType() == EntityType.CREEPER && allowedSources.contains("CREEPER")) sourceAllowed = true;
                // Add more specific source checks if needed (e.g. FIREBALL for Ghast/Blaze)
            }
            if (!sourceAllowed) {
                if (config.isDebugMode() && attacker != null) plugin.getLogger().info("Damage source " + cause + (directDamager != null ? "/"+directDamager.getType() : "") +" not allowed for core of " + coreOwnerUUID);
                return false;
            }
        }


        // Default vulnerability state
        boolean isVulnerable = config.isCoreVulnerableByDefault();

        // Check conditions
        // For now, only placeholder condition
        if (config.isPlaceholderConditionAlwaysVulnerable()) {
            if(config.isCoreVulnerableByDefault()){
                 // If default is vulnerable, this condition doesn't make it invulnerable unless explicitly designed to.
                 // Assuming placeholderConditionAlwaysVulnerable=true means it IS vulnerable IF default is false.
            } else {
                isVulnerable = true; // If default is invulnerable, this condition makes it vulnerable.
            }
        }

        // TODO: Implement Faction War check
        // if (config.isFactionWarConditionEnabled()) {
        //     Player owner = Bukkit.getPlayer(coreOwnerUUID);
        //     if (owner != null && factionsIntegration.isInFactionWar(owner)) {
        //         if(config.isCoreVulnerableByDefault()){ /* remains vulnerable */ }
        //         else { isVulnerable = true; }
        //     } else {
        //          if(config.isCoreVulnerableByDefault()){ isVulnerable = false; /* protected if not in war */ }
        //          else { /* remains invulnerable */ }
        //     }
        // }

        return isVulnerable;
    }

    public void damageCoreOffline(UUID coreOwnerUUID, PlayerData coreOwnerData, double amount) {
        if (!coreOwnerData.isCoreActive() || coreOwnerData.getCoreLocation() == null) {
            return;
        }
        coreOwnerData.takeDamage(amount);
        // No direct messaging to offline player here

        if (coreOwnerData.getCurrentHealth() <= 0) {
            // Simplified destruction for offline - no "Player owner" instance
            plugin.getLogger().info("Core of offline player " + coreOwnerUUID + " is being destroyed.");
            coreOwnerData.createBackup();
            coreOwnerData.setCoreActive(false);
            // coreOwnerData.setCurrentHealth(0) already done by takeDamage

            coreEntityManager.removeCoreEntity(coreOwnerUUID, true);

            if(coreOwnerData.getCoreLocation() != null && coreOwnerData.getCoreLocation().getWorld() != null){
                 plugin.getRegionManager().removeRegion(coreOwnerUUID, coreOwnerData.getCoreLocation().getWorld());
            }

            if (plugin.getConfigManager().isAnnounceCoreDestruction()) {
                OfflinePlayer offlineOwner = Bukkit.getOfflinePlayer(coreOwnerUUID);
                String ownerName = offlineOwner.getName() != null ? offlineOwner.getName() : coreOwnerUUID.toString();
                String message = ChatUtils.color(plugin.getConfigManager().getDestructionAnnouncementMessage().replace("{player}", ownerName));
                Bukkit.broadcastMessage(message);
            }
        }
        plugin.getDataManager().savePlayerData(coreOwnerData);
    }


    // --- Existing Methods ---

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found, economy features for core restoration will be disabled.");
            return;
        }
        var rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().info("No economy plugin found by Vault, economy features for core restoration will be disabled.");
            return;
        }
        vaultEconomy = rsp.getProvider();
        if (vaultEconomy != null) {
            plugin.getLogger().info("Successfully hooked into Vault and found economy provider: " + vaultEconomy.getName());
        } else {
             plugin.getLogger().warning("Vault was found, but no economy provider was registered with it.");
        }
    }

    public void damageCore(Player targetPlayer, double amount) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(targetPlayer.getUniqueId());
        if (!playerData.isCoreActive() || playerData.getCoreLocation() == null) {
            return;
        }
        playerData.takeDamage(amount);
        ChatUtils.sendMessage(targetPlayer, "&cYour Core has taken " + df.format(amount) + " damage! Current health: " + df.format(playerData.getCurrentHealth()) + "/" + df.format(playerData.getMaxHealth()));
        if (playerData.getCurrentHealth() <= 0) {
            destroyCore(targetPlayer, playerData);
        } else {
            plugin.getDataManager().savePlayerData(playerData);
        }
    }

    public void destroyCore(Player owner, PlayerData playerData) {
        if (!playerData.isCoreActive() || playerData.getCoreLocation() == null) return;
        playerData.createBackup();
        playerData.setCoreActive(false);
        playerData.setCurrentHealth(0);

        coreEntityManager.removeCoreEntity(owner.getUniqueId(), true);

        // Pass the world from the core's location for WG region removal
        if (playerData.getCoreLocation() != null && playerData.getCoreLocation().getWorld() != null) {
            plugin.getRegionManager().removeRegion(owner.getUniqueId(), playerData.getCoreLocation().getWorld());
        } else {
            // Fallback or error if world is not available, though it should be if core was active
            plugin.getLogger().warning("Could not determine world for WorldGuard region removal for player: " + owner.getUniqueId());
        }
        plugin.getDataManager().savePlayerData(playerData);

        ChatUtils.sendMessage(owner, "&c&lYour Core has been destroyed! Its essence is saved. You can restore it using /core restore.");
        if (plugin.getConfigManager().isAnnounceCoreDestruction()) {
            String message = ChatUtils.color(plugin.getConfigManager().getDestructionAnnouncementMessage().replace("{player}", owner.getName()));
            Bukkit.broadcastMessage(message);
        }
        // TODO: Fire CoreDestroyEvent
    }

    public void attemptRestoreCore(Player player, Location newLocation) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        ConfigManager config = plugin.getConfigManager();
        if (playerData.isCoreActive()) {
            ChatUtils.sendErrorMessage(player, "You already have an active Core.");
            return;
        }
        if (!playerData.hasCoreBackup()) {
            ChatUtils.sendErrorMessage(player, "No Core backup found to restore.");
            return;
        }
        if (!config.isRestorationEnabled()) {
            ChatUtils.sendErrorMessage(player, "Core restoration is currently disabled.");
            return;
        }
        long cooldownSeconds = config.getRestorationCooldown();
        if (cooldownSeconds > 0 && restoreCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (restoreCooldowns.get(player.getUniqueId()) + (cooldownSeconds * 1000)) - System.currentTimeMillis();
            if (timeLeft > 0) {
                ChatUtils.sendErrorMessage(player, "You must wait " + (timeLeft / 1000) + " seconds before restoring your Core again.");
                return;
            }
        }
        double econCost = config.getRestorationVaultCost();
        if (econCost > 0) {
            if (vaultEconomy == null) {
                ChatUtils.sendErrorMessage(player, "Economy system not available. Please contact an admin.");
                return;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
            if (vaultEconomy.getBalance(offlinePlayer) < econCost) {
                ChatUtils.sendErrorMessage(player, "You need " + vaultEconomy.format(econCost) + " to restore your Core.");
                return;
            }
        }
        Map<Material, Integer> itemCosts = config.getRestorationItemCosts();
        if (itemCosts != null && !itemCosts.isEmpty()) {
            PlayerInventory inventory = player.getInventory();
            for (Map.Entry<Material, Integer> entry : itemCosts.entrySet()) {
                if (!inventory.containsAtLeast(new ItemStack(entry.getKey()), entry.getValue())) {
                    ChatUtils.sendErrorMessage(player, "You need " + entry.getValue() + "x " + entry.getKey().name().replace("_", " ").toLowerCase() + " to restore.");
                    return;
                }
            }
            for (Map.Entry<Material, Integer> entry : itemCosts.entrySet()) {
                inventory.removeItem(new ItemStack(entry.getKey(), entry.getValue()));
            }
        }
        if (econCost > 0 && vaultEconomy != null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
            EconomyResponse r = vaultEconomy.withdrawPlayer(offlinePlayer, econCost);
            if (!r.transactionSuccess()) {
                ChatUtils.sendErrorMessage(player, "Failed to withdraw money: " + r.errorMessage);
                return;
            }
        }
        if (playerData.restoreFromBackup()) {
            playerData.setCoreActive(true);
            playerData.setCoreLocation(newLocation.clone());

            mutationManager.applyAllMutations(player, playerData); // Apply mutations after stats are restored from backup

            coreEntityManager.spawnCoreEntity(player, newLocation, playerData.getLevel());

            plugin.getRegionManager().addRegion(player.getUniqueId(), newLocation);
            plugin.getDataManager().savePlayerData(playerData);
            ChatUtils.sendMessage(player, "&aYour Core has been successfully restored!");
            restoreCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            // TODO: Fire CoreRestoreEvent
        } else {
            ChatUtils.sendErrorMessage(player, "An unexpected error occurred while restoring from backup.");
        }
    }

    public void addCoreXP(Player player, double amount, String reason) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (!playerData.isCoreActive() || playerData.getLevel() >= plugin.getConfigManager().getMaxCoreLevel()) {
            if(playerData.getLevel() >= plugin.getConfigManager().getMaxCoreLevel()){
                 if(plugin.getConfigManager().isDebugMode() && amount > 0 && reason != null && !reason.equalsIgnoreCase("ADMIN_SET")) {
                 }
            }
            return;
        }

        double finalAmount = amount * mutationManager.getXPMultiplierFromMutations(playerData); // Apply XP Multiplier

        CoreGainXPEvent xpEvent = new CoreGainXPEvent(player, finalAmount, reason);
        Bukkit.getPluginManager().callEvent(xpEvent);

        if (xpEvent.isCancelled() || xpEvent.getAmount() <= 0) {
            return;
        }

        double amountAfterEvent = xpEvent.getAmount();
        playerData.addXp(amountAfterEvent, reason);

        ChatUtils.sendMessage(player, "&7Gained &b" + df.format(amountAfterEvent) + " &7Core XP from &e" + reason.replace("_", " ") + "&7.");

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
        if (player.getWorld().getGameRuleValue(org.bukkit.GameRule.SEND_COMMAND_FEEDBACK)) {
             player.getWorld().spawnParticle(Particle.COMPOSTER, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.01);
        }

        checkForLevelUp(player, playerData);
        plugin.getDataManager().savePlayerData(playerData);
    }

    private void checkForLevelUp(Player player, PlayerData playerData) {
        int currentLevel = playerData.getLevel();
        if (currentLevel >= plugin.getConfigManager().getMaxCoreLevel()) {
            return;
        }

        double xpNeededForNextLevelTotal = ExperienceManager.getTotalXpForLevel(currentLevel + 1);

        if (playerData.getXp() >= xpNeededForNextLevelTotal) {
            int oldLevelForEvent = currentLevel;
            int newLevel = currentLevel;
            while(playerData.getXp() >= ExperienceManager.getTotalXpForLevel(newLevel + 1) && newLevel < plugin.getConfigManager().getMaxCoreLevel()){
                newLevel++;
            }

            if (newLevel > oldLevelForEvent) {
                playerData.setLevel(newLevel);

                coreEntityManager.updateCoreAppearance(player.getUniqueId(), newLevel);

                CoreLevelUpEvent levelUpEvent = new CoreLevelUpEvent(player, oldLevelForEvent, newLevel);
                Bukkit.getPluginManager().callEvent(levelUpEvent);

                ChatUtils.sendMessage(player, "&a&lCORE LEVEL UP! Your Core is now Level &e" + newLevel + "&a&l!");
                if (playerData.getCoreLocation() != null && playerData.getCoreLocation().getWorld() != null) {
                    playerData.getCoreLocation().getWorld().spawnParticle(Particle.TOTEM, playerData.getCoreLocation().clone().add(0.5, 1, 0.5), 50, 0.5, 0.5, 0.5, 0.1);
                    playerData.getCoreLocation().getWorld().playSound(playerData.getCoreLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            }
        }
    }

    public Economy getVaultEconomy() {
        return vaultEconomy;
    }
}
