package com.example.coresystem.expansion;

import com.example.coresystem.CoreSystem;
import com.example.coresystem.PlayerData;
import com.example.coresystem.utils.ExperienceManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class CoreSystemExpansion extends PlaceholderExpansion {

    private final CoreSystem plugin;
    private final DecimalFormat df = new DecimalFormat("#.#");

    public CoreSystemExpansion(CoreSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "coresystem";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return "No Data";
        }

        switch (params.toLowerCase()) {
            case "level":
                return String.valueOf(playerData.getLevel());
            case "xp":
                double currentLevelXp = ExperienceManager.getCurrentLevelProgressXp(playerData.getXp(), playerData.getLevel());
                return df.format(currentLevelXp);
            case "xp_required":
                double requiredForNext = ExperienceManager.getRequiredXpForLevel(playerData.getLevel());
                return df.format(requiredForNext);
            case "xp_total":
                return df.format(playerData.getXp());
            case "energy":
                return df.format(playerData.getEnergy());
            case "max_energy": // New placeholder
                return df.format(playerData.getMaxEnergy());
            case "type":
                return playerData.getArchetype() != null ? playerData.getArchetype() : "None";
            case "status":
                if (playerData.isCoreActive()) {
                    return "Active";
                } else if (playerData.hasCoreBackup()) {
                    return "Destroyed (Backup Available)";
                } else {
                    return "Inactive";
                }
            case "health":
                return df.format(playerData.getCurrentHealth());
            case "max_health":
                return df.format(playerData.getMaxHealth());
            case "health_bar":
                return createHealthBar(playerData.getCurrentHealth(), playerData.getMaxHealth());
            case "owner_name":
                return player.getName() != null ? player.getName() : "Unknown";
            default:
                return null;
        }
    }

    private String createHealthBar(double current, double max, int barLength, char symbolFull, char symbolEmpty, String colorFull, String colorEmpty) {
        if (max <= 0) return "";
        double percentage = current / max;
        int fullChars = (int) (percentage * barLength);
        int emptyChars = barLength - fullChars;

        StringBuilder bar = new StringBuilder();
        bar.append(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', colorFull));
        for (int i = 0; i < fullChars; i++) {
            bar.append(symbolFull);
        }
        bar.append(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', colorEmpty));
        for (int i = 0; i < emptyChars; i++) {
            bar.append(symbolEmpty);
        }
        return bar.toString();
    }

    private String createHealthBar(double current, double max) {
        return createHealthBar(current, max, 10, '\u258B', '\u258B', "&c", "&7");
    }
}
