package com.example.coresystem.mutation;

import com.example.coresystem.ConfigManager;
import com.example.coresystem.CoreSystem;
import com.example.coresystem.PlayerData;
import com.example.coresystem.events.CoreRebirthEvent;
import com.example.coresystem.skill.SkillManager;
import com.example.coresystem.utils.ChatUtils;
import com.example.coresystem.utils.ExperienceManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MutationManager {

    private final CoreSystem plugin;
    private final ConfigManager configManager;
    private final SkillManager skillManager;


    public MutationManager(CoreSystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.skillManager = plugin.getSkillManager();
    }

    public void attemptRebirth(Player player) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (!playerData.isCoreActive()) {
            ChatUtils.sendErrorMessage(player, "You need an active Core to perform a rebirth.");
            return;
        }

        if (playerData.getLevel() < configManager.getMaxCoreLevel()) {
            ChatUtils.sendErrorMessage(player, "Your Core must be at the maximum level (" + configManager.getMaxCoreLevel() + ") to rebirth.");
            return;
        }

        // Determine which mutation to award
        // For simplicity, award them in the order they appear in mutations.yml,
        // if the player doesn't have them yet.
        List<Mutation> availableMutations = new ArrayList<>(configManager.getMutations().values());
        Mutation mutationToAward = null;

        // Sort mutations by ID to ensure consistent order if needed, though config order is usually fine.
        // availableMutations.sort(Comparator.comparing(Mutation::getId));

        List<String> currentMutationIds = playerData.getActiveMutations();
        for (Mutation potentialMutation : availableMutations) {
            if (!currentMutationIds.contains(potentialMutation.getId())) {
                mutationToAward = potentialMutation;
                break;
            }
        }

        if (mutationToAward == null) {
            ChatUtils.sendMessage(player, "&eYou have already acquired all available mutations!");
            // Optionally, still allow rebirth for other benefits or a "dummy" prestige point.
            // For now, if no new mutation, don't allow rebirth.
            return;
        }

        int oldRebirthCount = playerData.getRebirthCount();

        // --- Perform Rebirth ---
        playerData.incrementRebirthCount();
        playerData.addMutation(mutationToAward.getId());

        // Reset core stats
        playerData.setLevel(1);
        playerData.setXp(0); // Reset total XP

        // Reset base stats before applying mutations for the new state
        playerData.setMaxHealth(configManager.getDefaultCoreMaxHealth());
        playerData.setMaxEnergy(configManager.getDefaultCoreMaxEnergy());
        playerData.setCurrentHealth(playerData.getMaxHealth());
        playerData.setEnergy(playerData.getMaxEnergy());

        // Clear unlocked skills (they will be re-unlocked based on new level 1 and archetype)
        playerData.setUnlockedSkills(new ArrayList<>());
        // Archetype is kept.

        // Apply all active mutations (including the new one) to the reset stats
        applyAllMutations(player, playerData);

        // Re-unlock skills for level 1 with the chosen archetype
        if(playerData.getArchetype() != null) {
            skillManager.unlockSkillsForLevel(player, 1); // Unlock level 1 skills
        }


        // Update visual representation
        plugin.getCoreEntityManager().updateCoreAppearance(player.getUniqueId(), 1);

        plugin.getDataManager().savePlayerData(playerData);

        // Announce
        ChatUtils.sendMessage(player, "&a&lCORE REBIRTH! Your Core has been reborn!");
        ChatUtils.sendMessage(player, "&7You are now at Rebirth: &e" + playerData.getRebirthCount());
        ChatUtils.sendMessage(player, "&7Level reset to 1. XP reset to 0.");
        ChatUtils.sendMessage(player, "&7New Mutation Unlocked: " + mutationToAward.getName());
        ChatUtils.sendMessage(player, "&7  " + mutationToAward.getDescription());
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.2f);


        CoreRebirthEvent event = new CoreRebirthEvent(player, oldRebirthCount, playerData.getRebirthCount(), Collections.singletonList(mutationToAward));
        Bukkit.getPluginManager().callEvent(event);
    }

    public void applyAllMutations(Player player, PlayerData playerData) {
        // Reset stats to base before applying all mutations to avoid stacking issues on multiple calls
        // This is crucial if this method is called outside of rebirth (e.g., on player data load)
        double baseMaxHealth = configManager.getDefaultCoreMaxHealth();
        double baseMaxEnergy = configManager.getDefaultCoreMaxEnergy();
        // Other base stats if they exist (e.g., base_xp_multiplier = 1.0)

        // Temporary variables to accumulate bonuses before setting them
        double finalMaxHealth = baseMaxHealth;
        double finalMaxEnergy = baseMaxEnergy;
        double currentXpMultiplier = 1.0; // Start with 1.0, add percentage bonuses
        double currentPassiveEnergyRegenBonus = 0.0;


        for (String mutationId : playerData.getActiveMutations()) {
            Mutation mutation = configManager.getMutation(mutationId);
            if (mutation == null) continue;

            switch (mutation.getType().toUpperCase()) {
                case "CORE_MAX_HEALTH_ADD":
                    finalMaxHealth += mutation.getEffectDetailDouble("amount", 0.0);
                    break;
                case "CORE_MAX_ENERGY_ADD":
                    finalMaxEnergy += mutation.getEffectDetailDouble("amount", 0.0);
                    break;
                case "CORE_XP_MULTIPLIER":
                    // Multipliers are typically additive to a base of 1.0
                    // e.g. two 0.05 multipliers result in 1.0 + 0.05 + 0.05 = 1.10x
                    currentXpMultiplier += mutation.getEffectDetailDouble("multiplier", 0.0);
                    break;
                case "PASSIVE_ENERGY_REGEN_BOOST_ADD":
                    currentPassiveEnergyRegenBonus += mutation.getEffectDetailDouble("amount_increase", 0.0);
                    break;
                // Add more cases for other mutation types
            }
        }

        // Set the final calculated stats
        playerData.setMaxHealth(finalMaxHealth);
        playerData.setCurrentHealth(Math.min(playerData.getCurrentHealth(), finalMaxHealth)); // Cap current health if max decreased

        playerData.setMaxEnergy(finalMaxEnergy);
        playerData.setEnergy(Math.min(playerData.getEnergy(), finalMaxEnergy)); // Cap current energy

        // Store effective multipliers or direct bonuses if needed by other systems
        // For example, EnergyManager might query for the passive regen bonus.
        // Or, PlayerData could store these effective_multipliers if frequently accessed.
        // For now, the effect of XP multiplier and regen boost will be handled by CoreManager/EnergyManager directly
        // by querying these mutations when an event occurs.

        if (plugin.getConfigManager().isDebugMode() && player != null && player.isOnline()) {
             ChatUtils.sendMessage(player, "&d[Debug] Mutations applied. MaxHealth: " + df.format(playerData.getMaxHealth()) + ", MaxEnergy: " + df.format(playerData.getMaxEnergy()));
        }
    }

    // This method will be called by CoreManager when calculating XP gain
    public double getXPMultiplierFromMutations(PlayerData playerData) {
        double totalMultiplier = 1.0; // Start with base 100%
        for (String mutationId : playerData.getActiveMutations()) {
            Mutation mutation = configManager.getMutation(mutationId);
            if (mutation != null && "CORE_XP_MULTIPLIER".equals(mutation.getType().toUpperCase())) {
                totalMultiplier += mutation.getEffectDetailDouble("multiplier", 0.0);
            }
        }
        return Math.max(0, totalMultiplier); // Ensure multiplier isn't negative
    }

    // This method will be called by EnergyManager when calculating passive regen
    public double getPassiveEnergyRegenBoost(PlayerData playerData) {
        double totalBoost = 0.0;
        for (String mutationId : playerData.getActiveMutations()) {
            Mutation mutation = configManager.getMutation(mutationId);
            if (mutation != null && "PASSIVE_ENERGY_REGEN_BOOST_ADD".equals(mutation.getType().toUpperCase())) {
                totalBoost += mutation.getEffectDetailDouble("amount_increase", 0.0);
            }
        }
        return totalBoost;
    }
}
