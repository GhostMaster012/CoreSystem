package com.example.coresystem.commands;

import com.example.coresystem.CoreManager;
import com.example.coresystem.CoreSystem;
import com.example.coresystem.PlayerData;
import com.example.coresystem.entity.CoreEntityManager;
import com.example.coresystem.utils.ChatUtils;
import com.example.coresystem.utils.MessageManager; // Import MessageManager
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CoreAdminCommand implements CommandExecutor, TabCompleter {

    private final CoreSystem plugin;
    private final CoreManager coreManager;
    private final CoreEntityManager coreEntityManager;
    private final MessageManager mm; // Add MessageManager

    public CoreAdminCommand(CoreSystem plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
        this.coreEntityManager = plugin.getCoreEntityManager();
        this.mm = plugin.getMessageManager(); // Get MessageManager
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("coresystem.admin")) {
            mm.sendErrorMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        // Base permission coresystem.command.coreadmin is checked by plugin.yml for /coreadmin itself
        switch (subCommand) {
            case "damage":
                if (!sender.hasPermission("coresystem.admin.damage")) { mm.sendErrorMessage(sender, "no-permission"); return true; }
                handleDamage(sender, args);
                break;
            case "destroy":
                if (!sender.hasPermission("coresystem.admin.destroy")) { mm.sendErrorMessage(sender, "no-permission"); return true; }
                handleDestroy(sender, args);
                break;
            case "sethealth":
                if (!sender.hasPermission("coresystem.admin.sethealth")) { mm.sendErrorMessage(sender, "no-permission"); return true; }
                handleSetHealth(sender, args);
                break;
            case "gethealth":
                // No specific sub-permission for gethealth, base admin is enough.
                handleGetHealth(sender, args);
                break;
            case "setlevel":
                if (!sender.hasPermission("coresystem.admin.setlevel")) { mm.sendErrorMessage(sender, "no-permission"); return true; }
                handleSetLevel(sender, args);
                break;
            case "setxp":
                if (!sender.hasPermission("coresystem.admin.setxp")) { mm.sendErrorMessage(sender, "no-permission"); return true; }
                handleSetXp(sender, args);
                break;
            case "setenergy":
                if (!sender.hasPermission("coresystem.admin.setenergy")) { mm.sendErrorMessage(sender, "no-permission"); return true; }
                handleSetEnergy(sender, args);
                break;
            case "setarchetype":
                if (!sender.hasPermission("coresystem.admin.setarchetype")) { mm.sendErrorMessage(sender, "no-permission"); return true; }
                handleSetArchetype(sender, args);
                break;
            case "reload":
                if (!sender.hasPermission("coresystem.admin.reload")) { mm.sendErrorMessage(sender, "no-permission"); return true; }
                plugin.getConfigManager().reloadConfig();
                plugin.getRegionManager().updateProtectionRadius();
                plugin.getEnergyManager().reload();
                plugin.getCoreEntityManager().loadExistingCoreEntities();
                mm.sendMessage(sender, "reloaded");
                break;
            default:
                mm.sendErrorMessage(sender, "unknown-subcommand", ChatUtils.params("command", "coreadmin"));
                break;
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        mm.sendMessage(sender, "coreadmin.help-header");
        mm.sendMessage(sender, "coreadmin.help-damage");
        mm.sendMessage(sender, "coreadmin.help-destroy");
        mm.sendMessage(sender, "coreadmin.help-sethealth");
        mm.sendMessage(sender, "coreadmin.help-gethealth");
        mm.sendMessage(sender, "coreadmin.help-setlevel");
        mm.sendMessage(sender, "coreadmin.help-setxp");
        mm.sendMessage(sender, "coreadmin.help-setenergy");
        mm.sendMessage(sender, "coreadmin.help-setarchetype");
        mm.sendMessage(sender, "coreadmin.help-reload");
    }

    private PlayerData getPlayerDataForAdmin(CommandSender sender, String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName); // Deprecated but common for name->UUID
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            mm.sendErrorMessage(sender, "player-not-found", ChatUtils.params("player", playerName));
            return null;
        }
        PlayerData playerData = plugin.getDataManager().getPlayerData(offlinePlayer.getUniqueId());
        // Most admin commands require an active core. Some might not (e.g. see data for non-active core)
        // For now, let's assume most modification commands need an active core.
        if (!playerData.isCoreActive()){
             // Allow admin to modify their own data even if core not placed for some commands, but this check is general.
            // if (sender instanceof Player && ((Player)sender).getUniqueId().equals(offlinePlayer.getUniqueId())) { }
            mm.sendErrorMessage(sender, "coreadmin.no-active-core", ChatUtils.params("player", (offlinePlayer.getName() != null ? offlinePlayer.getName() : playerName)));
            return null;
        }
        return playerData;
    }


    private void handleDamage(CommandSender sender, String[] args) {
        if (args.length < 3) {
            mm.sendErrorMessage(sender, "coreadmin.damage.usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            mm.sendErrorMessage(sender, "player-not-found", ChatUtils.params("player", args[1]));
            return;
        }
        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                mm.sendErrorMessage(sender, "coreadmin.damage.amount-positive");
                return;
            }
            PlayerData targetData = plugin.getDataManager().getPlayerData(target.getUniqueId()); // Re-fetch to ensure it's for online player
            if (!targetData.isCoreActive()) {
                mm.sendErrorMessage(sender, "coreadmin.no-active-core", ChatUtils.params("player", target.getName()));
                return;
            }
            coreManager.damageCore(target, amount);
            mm.sendMessage(sender, "coreadmin.damage.success", ChatUtils.params("player", target.getName(), "amount", String.valueOf(amount)));
            // Target receives message from coreManager.damageCore
        } catch (NumberFormatException e) {
            mm.sendErrorMessage(sender, "coreadmin.sethealth.invalid-amount", ChatUtils.params("amount", args[2]));
        }
    }

    private void handleDestroy(CommandSender sender, String[] args) {
        if (args.length < 2) {
            mm.sendErrorMessage(sender, "coreadmin.destroy.usage");
            return;
        }

        String playerName = args[1];
        Player targetOnline = Bukkit.getPlayerExact(playerName);
        UUID targetUUID;
        String targetNameDisplay;

        if (targetOnline != null) {
            targetUUID = targetOnline.getUniqueId();
            targetNameDisplay = targetOnline.getName();
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(playerName);
            if(offlineTarget.hasPlayedBefore() || offlineTarget.isOnline()){
                 targetUUID = offlineTarget.getUniqueId();
                 targetNameDisplay = offlineTarget.getName() != null ? offlineTarget.getName() : playerName;
            } else {
                 mm.sendErrorMessage(sender, "player-not-found", ChatUtils.params("player", playerName));
                 return;
            }
        }

        PlayerData targetData = plugin.getDataManager().getPlayerData(targetUUID);
        if (!targetData.isCoreActive() || targetData.getCoreLocation() == null) {
            mm.sendErrorMessage(sender, "coreadmin.no-active-core", ChatUtils.params("player", targetNameDisplay));
            return;
        }

        if (targetOnline != null) {
            coreManager.destroyCore(targetOnline, targetData);
            mm.sendMessage(sender, "coreadmin.destroy.success", ChatUtils.params("player", targetNameDisplay));
        } else {
            targetData.createBackup();
            targetData.setCoreActive(false);
            targetData.setCurrentHealth(0);
            Location coreLoc = targetData.getCoreLocation();
            coreEntityManager.removeCoreEntity(targetUUID, true);
            plugin.getDataManager().savePlayerData(targetData);
            if (coreLoc != null && coreLoc.getWorld() != null) {
                plugin.getRegionManager().removeRegion(targetUUID, coreLoc.getWorld());
            } else {
                 plugin.getLogger().warning("Could not determine world for offline player " + targetNameDisplay + " core region removal.");
            }
            mm.sendMessage(sender, "coreadmin.destroy.offline-success", ChatUtils.params("player", targetNameDisplay));
             if (plugin.getConfigManager().isAnnounceCoreDestruction()) {
                // Use MessageManager to get the raw, colored message before replacing placeholders
                String rawMessage = plugin.getMessageManager().getRawMessage("destruction-announcement-message", "&c&lATTENTION! {player}''s Core has been destroyed!");
                String message = rawMessage.replace("{player}", targetNameDisplay);
                Bukkit.broadcastMessage(message);
            }
        }
    }


    private void handleSetHealth(CommandSender sender, String[] args) {
        if (args.length < 3) {
            mm.sendErrorMessage(sender, "coreadmin.sethealth.usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            mm.sendErrorMessage(sender, "player-not-found", ChatUtils.params("player", args[1]));
            return;
        }
        try {
            double amount = Double.parseDouble(args[2]);
            PlayerData targetData = plugin.getDataManager().getPlayerData(target.getUniqueId());
            if (!targetData.isCoreActive()) {
                mm.sendErrorMessage(sender, "coreadmin.no-active-core", ChatUtils.params("player", target.getName()));
                return;
            }
            targetData.setCurrentHealth(amount);
            plugin.getDataManager().savePlayerData(targetData);
            Map<String, String> params = ChatUtils.params(
                "player", target.getName(),
                "current_health", String.format("%.1f", targetData.getCurrentHealth()),
                "max_health", String.format("%.1f", targetData.getMaxHealth())
            );
            mm.sendMessage(sender, "coreadmin.sethealth.success", params);
            mm.sendMessage(target, "coreadmin.sethealth.notify-target", params);
        } catch (NumberFormatException e) {
            mm.sendErrorMessage(sender, "coreadmin.sethealth.invalid-amount", ChatUtils.params("amount", args[2]));
        }
    }

    private void handleGetHealth(CommandSender sender, String[] args) {
        if (args.length < 2) {
            mm.sendErrorMessage(sender, "coreadmin.gethealth.usage");
            return;
        }
        String playerName = args[1];
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(playerName);
         if(!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline()){
             mm.sendErrorMessage(sender, "player-not-found", ChatUtils.params("player", playerName));
             return;
         }

        PlayerData targetData = plugin.getDataManager().getPlayerData(offlineTarget.getUniqueId());
        if (!targetData.isCoreActive()) {
            mm.sendErrorMessage(sender, "coreadmin.no-active-core", ChatUtils.params("player", (offlineTarget.getName() != null ? offlineTarget.getName() : playerName)));
            return;
        }
        mm.sendMessage(sender, "coreadmin.gethealth.response", ChatUtils.params(
            "player", (offlineTarget.getName() != null ? offlineTarget.getName() : playerName),
            "current_health", String.format("%.1f", targetData.getCurrentHealth()),
            "max_health", String.format("%.1f", targetData.getMaxHealth())
        ));
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            mm.sendErrorMessage(sender, "coreadmin.setlevel.usage");
            return;
        }
        String playerName = args[1];
        PlayerData targetData = getPlayerDataForAdmin(sender, playerName);
        if (targetData == null) return;

        try {
            int level = Integer.parseInt(args[2]);
            if (level < 1 || level > plugin.getConfigManager().getMaxCoreLevel()) {
                mm.sendErrorMessage(sender, "coreadmin.setlevel.level-range", ChatUtils.params("max_level", String.valueOf(plugin.getConfigManager().getMaxCoreLevel())));
                return;
            }
            targetData.setLevel(level);
            targetData.setXp(com.example.coresystem.utils.ExperienceManager.getTotalXpForLevel(level));

            plugin.getCoreEntityManager().updateCoreAppearance(targetData.getPlayerUUID(), level);
            Player onlinePlayer = Bukkit.getPlayer(targetData.getPlayerUUID());
            if(onlinePlayer != null) plugin.getSkillManager().unlockSkillsForLevel(onlinePlayer, level);

            plugin.getDataManager().savePlayerData(targetData);
            mm.sendMessage(sender, "coreadmin.setlevel.success", ChatUtils.params("player", playerName, "level", String.valueOf(level)));
        } catch (NumberFormatException e) {
            mm.sendErrorMessage(sender, "coreadmin.setlevel.invalid-number", ChatUtils.params("level", args[2]));
        }
    }

    private void handleSetXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            mm.sendErrorMessage(sender, "coreadmin.setxp.usage");
            return;
        }
        String playerName = args[1];
        PlayerData targetData = getPlayerDataForAdmin(sender, playerName);
        if (targetData == null) return;

        try {
            double xp = Double.parseDouble(args[2]);
            if (xp < 0) {
                mm.sendErrorMessage(sender, "coreadmin.setxp.xp-negative");
                return;
            }
            targetData.setXp(xp);
            int oldLevel = targetData.getLevel();
            int newLevel = oldLevel;
            while(newLevel < plugin.getConfigManager().getMaxCoreLevel() && targetData.getXp() >= com.example.coresystem.utils.ExperienceManager.getTotalXpForLevel(newLevel + 1)){
                newLevel++;
            }
            while(newLevel > 1 && targetData.getXp() < com.example.coresystem.utils.ExperienceManager.getTotalXpForLevel(newLevel)){
                newLevel--;
            }
            if(newLevel != oldLevel){
                targetData.setLevel(newLevel);
                plugin.getCoreEntityManager().updateCoreAppearance(targetData.getPlayerUUID(), newLevel);
                 Player onlinePlayer = Bukkit.getPlayer(targetData.getPlayerUUID());
                 if(onlinePlayer != null) plugin.getSkillManager().unlockSkillsForLevel(onlinePlayer, newLevel);
            }

            plugin.getDataManager().savePlayerData(targetData);
            mm.sendMessage(sender, "coreadmin.setxp.success", ChatUtils.params("player", playerName, "xp", String.format("%.1f", xp), "level", String.valueOf(targetData.getLevel())));
        } catch (NumberFormatException e) {
            mm.sendErrorMessage(sender, "coreadmin.setxp.invalid-amount", ChatUtils.params("amount", args[2]));
        }
    }

    private void handleSetEnergy(CommandSender sender, String[] args) {
        if (args.length < 3) {
            mm.sendErrorMessage(sender, "coreadmin.setenergy.usage");
            return;
        }
        String playerName = args[1];
        PlayerData targetData = getPlayerDataForAdmin(sender, playerName);
        if (targetData == null) return;

        try {
            double energy = Double.parseDouble(args[2]);
            targetData.setEnergy(energy);
            plugin.getDataManager().savePlayerData(targetData);
            mm.sendMessage(sender, "coreadmin.setenergy.success", ChatUtils.params(
                "player", playerName,
                "current_energy", String.format("%.1f", targetData.getEnergy()),
                "max_energy", String.format("%.1f", targetData.getMaxEnergy())
            ));
        } catch (NumberFormatException e) {
            mm.sendErrorMessage(sender, "coreadmin.setenergy.invalid-amount", ChatUtils.params("amount", args[2]));
        }
    }

    private void handleSetArchetype(CommandSender sender, String[] args) {
        if (args.length < 3) {
            mm.sendErrorMessage(sender, "coreadmin.setarchetype.usage");
            mm.sendErrorMessage(sender, "coreadmin.setarchetype.list-available");
            return;
        }
        String playerName = args[1];
        PlayerData targetData = getPlayerDataForAdmin(sender, playerName);
         if (targetData == null && Bukkit.getOfflinePlayer(playerName).hasPlayedBefore()) {
            targetData = plugin.getDataManager().getPlayerData(Bukkit.getOfflinePlayer(playerName).getUniqueId());
        }
        if (targetData == null) {
             mm.sendErrorMessage(sender, "player-not-found", ChatUtils.params("player", playerName));
            return;
        }


        String archetypeId = args[2].toUpperCase();
        if (archetypeId.equalsIgnoreCase("NONE") || archetypeId.equalsIgnoreCase("NULL")) {
            targetData.setArchetype(null);
            targetData.setUnlockedSkills(new ArrayList<>());
            plugin.getDataManager().savePlayerData(targetData);
            mm.sendMessage(sender, "coreadmin.setarchetype.cleared", ChatUtils.params("player", playerName));
            return;
        }

        if (plugin.getConfigManager().getArchetype(archetypeId) == null) {
            mm.sendErrorMessage(sender, "coreadmin.setarchetype.invalid-id", ChatUtils.params("archetype_id", archetypeId));
            mm.sendErrorMessage(sender, "coreadmin.setarchetype.list-available");
            return;
        }
        targetData.setArchetype(archetypeId);
        Player onlinePlayer = Bukkit.getPlayer(targetData.getPlayerUUID());
        if(onlinePlayer != null && targetData.isCoreActive()){
            plugin.getSkillManager().unlockSkillsForLevel(onlinePlayer, targetData.getLevel());
        }
        plugin.getDataManager().savePlayerData(targetData);
        mm.sendMessage(sender, "coreadmin.setarchetype.success", ChatUtils.params("player", playerName, "archetype_id", archetypeId));
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("coresystem.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("damage", "destroy", "sethealth", "gethealth", "reload", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("damage") || args[0].equalsIgnoreCase("sethealth"))) {
            // These commands require online player for now
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("destroy") || args[0].equalsIgnoreCase("gethealth"))) {
             // These can work with offline players, suggest online first, then all known if more chars typed
            List<String> completions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            // Could add offline player name completion here if desired, but can be very long.
            return completions;
        }
        return new ArrayList<>();
    }
}
