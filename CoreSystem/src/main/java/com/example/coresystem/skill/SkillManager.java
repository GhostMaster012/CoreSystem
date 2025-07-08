package com.example.coresystem.skill;

import com.example.coresystem.ConfigManager;
import com.example.coresystem.CoreSystem;
import com.example.coresystem.PlayerData;
import com.example.coresystem.events.CoreSkillUseEvent;
import com.example.coresystem.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
// Import PotionEffect and PotionEffectType if you implement potion effects
// import org.bukkit.potion.PotionEffect;
// import org.bukkit.potion.PotionEffectType;

import java.text.DecimalFormat;

public class SkillManager {

    private final CoreSystem plugin;
    private final ConfigManager configManager;
    private final DecimalFormat df = new DecimalFormat("#.#");

    public SkillManager(CoreSystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void unlockSkillsForLevel(Player player, int newLevel) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (playerData.getArchetype() == null) {
            return; // No archetype chosen, no skills to unlock
        }

        Archetype archetype = configManager.getArchetype(playerData.getArchetype());
        if (archetype == null) {
            return;
        }

        boolean newSkillsUnlockedThisLevel = false;
        for (Skill skill : archetype.getSkills().values()) {
            if (playerData.getUnlockedSkills().contains(skill.getId())) {
                continue; // Already unlocked
            }

            if (newLevel >= skill.getRequiredLevel()) {
                // Check prerequisites
                boolean allPrerequisitesMet = true;
                if (skill.getPrerequisites() != null && !skill.getPrerequisites().isEmpty()) {
                    for (String prereqId : skill.getPrerequisites()) {
                        if (!playerData.getUnlockedSkills().contains(prereqId)) {
                            allPrerequisitesMet = false;
                            break;
                        }
                    }
                }

                if (allPrerequisitesMet) {
                    playerData.addSkill(skill.getId());
                    ChatUtils.sendMessage(player, "&aSkill Unlocked: " + skill.getName());
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
                    newSkillsUnlockedThisLevel = true;
                }
            }
        }
        if (newSkillsUnlockedThisLevel) {
            plugin.getDataManager().savePlayerData(playerData);
        }
    }

    public void tryActivateSkill(Player player, String skillId) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        Skill skill = configManager.getSkill(skillId); // This now searches across all archetypes

        if (skill == null) {
            ChatUtils.sendErrorMessage(player, "Skill '" + skillId + "' not found.");
            return;
        }

        // Check if skill belongs to player's archetype (important if skill IDs could be guessed/typed from other archetypes)
        Archetype playerArchetype = configManager.getArchetype(playerData.getArchetype());
        if (playerArchetype == null || playerArchetype.getSkill(skillId) == null) {
            ChatUtils.sendErrorMessage(player, "This skill does not belong to your current Core Archetype.");
            return;
        }


        if (!playerData.getUnlockedSkills().contains(skill.getId())) {
            ChatUtils.sendErrorMessage(player, "Skill " + skill.getName() + "&c is not unlocked yet.");
            return;
        }

        if (playerData.isSkillOnCooldown(skill.getId())) {
            long remainingMillis = playerData.getSkillCooldownRemainingMillis(skill.getId());
            ChatUtils.sendErrorMessage(player, skill.getName() + "&c is on cooldown for " + String.format("%.1f", remainingMillis / 1000.0) + "s.");
            return;
        }

        if (!playerData.consumeEnergy(skill.getEnergyCost())) {
            ChatUtils.sendErrorMessage(player, "&cNot enough energy for " + skill.getName() + "&c. Needs: " + df.format(skill.getEnergyCost()) + ", You have: " + df.format(playerData.getEnergy()) + ".");
            return;
        }

        CoreSkillUseEvent event = new CoreSkillUseEvent(player, skill);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // Refund energy if cancelled by another plugin
            playerData.addEnergy(skill.getEnergyCost());
            return;
        }

        // Apply skill effect (placeholder for now)
        ChatUtils.sendMessage(player, "&aUsed skill: " + skill.getName() + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1f);
        // Example: if (skill.getEffectType().equals("DAMAGE_BOOST")) { /* ... */ }

        playerData.setSkillOnCooldown(skill.getId(), skill.getCooldownSeconds());
        plugin.getDataManager().savePlayerData(playerData);
    }
}
