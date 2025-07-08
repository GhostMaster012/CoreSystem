package com.example.coresystem.display;

import com.example.coresystem.ConfigManager;
import com.example.coresystem.CoreSystem;
import com.example.coresystem.PlayerData;
import com.example.coresystem.skill.Skill;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.Map;

public class ActionBarManager {

    private final CoreSystem plugin;
    private final ConfigManager configManager;
    private BukkitTask actionBarTask;
    private final DecimalFormat df = new DecimalFormat("#.#");

    public ActionBarManager(CoreSystem plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        startActionBarTask();
    }

    public void startActionBarTask() {
        if (!configManager.isActionBarEnabled()) {
            if (configManager.isDebugMode()) {
                plugin.getLogger().info("Action Bar display is disabled in config.");
            }
            return;
        }

        if (actionBarTask != null && !actionBarTask.isCancelled()) {
            actionBarTask.cancel();
        }

        long interval = configManager.getActionBarUpdateIntervalTicks();
        if (interval <= 0) interval = 20L; // Default to 1 second if invalid

        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
                    if (playerData.isCoreActive()) { // Only show for players with active cores
                        sendActionBar(player, playerData);
                    } else {
                        // Optionally clear action bar if core becomes inactive
                        // player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, interval); // Run async as it involves PAPI and string manipulation
         plugin.getLogger().info("Action Bar display task started.");
    }

    private void sendActionBar(Player player, PlayerData playerData) {
        String format = configManager.getActionBarFormat();
        String cooldownsText = getActiveCooldownsText(playerData);

        // Replace our custom internal placeholder first
        format = format.replace("%coresystem_active_cooldowns%", cooldownsText);

        // Then, if PAPI is enabled, parse its placeholders
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            format = PlaceholderAPI.setPlaceholders(player, format);
        } else {
            // Manual replacements if PAPI is not available
            format = format.replace("%coresystem_energy%", df.format(playerData.getEnergy()))
                           .replace("%coresystem_max_energy%", df.format(playerData.getMaxEnergy()));
            // Add other manual PAPI placeholder replacements if needed
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(format));
    }

    private String getActiveCooldownsText(PlayerData playerData) {
        long longestCooldown = 0;
        String skillWithLongestCooldown = null;

        for (Map.Entry<String, Long> entry : playerData.getSkillCooldowns().entrySet()) {
            if (playerData.isSkillOnCooldown(entry.getKey())) {
                long remaining = playerData.getSkillCooldownRemainingMillis(entry.getKey());
                if (remaining > longestCooldown) {
                    longestCooldown = remaining;
                    Skill skill = plugin.getConfigManager().getSkill(entry.getKey());
                    skillWithLongestCooldown = (skill != null) ? skill.getName() : entry.getKey();
                }
            }
        }

        if (skillWithLongestCooldown != null) {
            // Using Name from Skill object now, which should be colored.
            return skillWithLongestCooldown + "&c: " + df.format(longestCooldown / 1000.0) + "s";
        } else {
            return configManager.getActionBarNoCooldownsText();
        }
    }

    public void stopActionBarTask() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) {
            actionBarTask.cancel();
            actionBarTask = null;
            plugin.getLogger().info("Action Bar display task stopped.");
        }
    }

    public void reload() {
        stopActionBarTask();
        startActionBarTask();
    }
}
