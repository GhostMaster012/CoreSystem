package com.example.coresystem.commands;

import com.example.coresystem.CoreItemManager;
import com.example.coresystem.CoreSystem;
import com.example.coresystem.CoreManager;
import com.example.coresystem.DataManager;
import com.example.coresystem.EnergyManager;
import com.example.coresystem.PlayerData;
import com.example.coresystem.entity.CoreEntityManager;
import com.example.coresystem.gui.CoreGui;
import com.example.coresystem.mutation.Mutation;
import com.example.coresystem.mutation.MutationManager;
import com.example.coresystem.protection.RegionManager;
import com.example.coresystem.skill.Archetype;
import com.example.coresystem.skill.Skill;
import com.example.coresystem.skill.SkillManager;
import com.example.coresystem.utils.ChatUtils; // Will be mostly unused, direct calls to MessageManager
import com.example.coresystem.utils.MessageManager; // Import MessageManager
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CoreCommand implements CommandExecutor, TabCompleter {

    private final CoreSystem plugin;
    private final CoreManager coreManager;
    private final CoreEntityManager coreEntityManager;
    private final EnergyManager energyManager;
    private final SkillManager skillManager;
    private final MutationManager mutationManager;
    private final CoreGui coreGui;
    private final MessageManager mm; // Store MessageManager instance
    private final Map<UUID, Long> claimCooldowns = new HashMap<>();
    private final Map<UUID, Long> feedCooldowns = new HashMap<>();

    public CoreCommand(CoreSystem plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
        this.coreEntityManager = plugin.getCoreEntityManager();
        this.energyManager = plugin.getEnergyManager();
        this.skillManager = plugin.getSkillManager();
        this.mutationManager = plugin.getMutationManager();
        this.coreGui = new CoreGui(plugin);
        this.mm = plugin.getMessageManager(); // Get MessageManager
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            mm.sendErrorMessage(sender, "player-only-command");
            return true;
        }

        Player player = (Player) sender;
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (args.length == 0) {
            coreGui.openMainCoreGui(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gui":
            case "menu":
                if (!player.hasPermission("coresystem.command.core")) {
                    mm.sendErrorMessage(player, "no-permission"); return true;
                }
                coreGui.openMainCoreGui(player);
                break;
            case "claim":
                if (!player.hasPermission("coresystem.core.claim")) {
                    mm.sendErrorMessage(player, "no-permission"); return true;
                }
                handleClaim(player, playerData);
                break;
            case "place":
                if (!player.hasPermission("coresystem.core.place")) {
                    mm.sendErrorMessage(player, "no-permission"); return true;
                }
                handlePlace(player, playerData);
                break;
            case "restore":
                if (!player.hasPermission("coresystem.core.restore")) {
                    mm.sendErrorMessage(player, "no-permission"); return true;
                }
                handleRestore(player);
                break;
            case "feed":
                if (!player.hasPermission("coresystem.core.feed")) {
                    mm.sendErrorMessage(player, "no-permission"); return true;
                }
                handleFeed(player, playerData);
                break;
            case "energize":
                if (!player.hasPermission("coresystem.core.energize")) {
                    mm.sendErrorMessage(player, "no-permission"); return true;
                }
                handleEnergize(player);
                break;
            case "archetype":
                if (!player.hasPermission("coresystem.core.archetype.choose")) {
                    mm.sendErrorMessage(player, "no-permission"); return true;
                }
                handleArchetype(player, playerData, args);
                break;
            case "skill":
                if (!player.hasPermission("coresystem.core.skill.use")) {
                    mm.sendErrorMessage(player, "no-permission"); return true;
                }
                handleSkill(player, playerData, args);
                break;
            case "rebirth":
                if (!player.hasPermission("coresystem.core.rebirth")) {
                    mm.sendErrorMessage(player, "no-permission"); return true;
                }
                handleRebirth(player);
                break;
            case "history":
                if (!player.hasPermission("coresystem.core.history")) {
                    mm.sendErrorMessage(player, "no-permission"); return true;
                }
                handleHistory(player, playerData);
                break;
            case "help":
                 if (!player.hasPermission("coresystem.command.core")) { // Same as GUI for base help
                    mm.sendErrorMessage(player, "no-permission"); return true;
                }
                showHelp(player);
                break;
            default:
                mm.sendErrorMessage(player, "unknown-subcommand", ChatUtils.params("command", label));
                break;
        }
        return true;
    }

    private void showHelp(Player player) {
        mm.sendMessage(player, "core.help-header");
        mm.sendMessage(player, "core.help-claim");
        mm.sendMessage(player, "core.help-place");
        mm.sendMessage(player, "core.help-restore");
        mm.sendMessage(player, "core.help-feed");
        mm.sendMessage(player, "core.help-energize");
        mm.sendMessage(player, "core.help-archetype");
        mm.sendMessage(player, "core.help-skill");
        mm.sendMessage(player, "core.help-rebirth");
        mm.sendMessage(player, "core.help-history");
        mm.sendMessage(player, "core.help-gui");
        mm.sendMessage(player, "core.help-help");
        if (player.hasPermission("coresystem.admin")) {
            mm.sendMessage(player, "core.help-admin-refer");
        }
    }


    private void handleClaim(Player player, PlayerData playerData) {
        if (playerData.isCoreActive()) {
            mm.sendErrorMessage(player, "core.claim.already-active");
            return;
        }
        CoreItemManager itemManager = plugin.getCoreItemManager();
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (itemManager.isCoreSeedItem(item)) {
                mm.sendErrorMessage(player, "core.claim.has-seed");
                return;
            }
        }
        long cooldownTime = plugin.getConfigManager().getCoreClaimCooldown() * 1000;
        if (claimCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (claimCooldowns.get(player.getUniqueId()) + cooldownTime) - System.currentTimeMillis();
            if (timeLeft > 0) {
                mm.sendErrorMessage(player, "core.claim.cooldown", ChatUtils.params("time", String.valueOf(timeLeft / 1000)));
                return;
            }
        }
        ItemStack coreSeed = itemManager.createCoreSeedItem();
        HashMap<Integer, ItemStack> result = inventory.addItem(coreSeed);
        if (!result.isEmpty()) {
            mm.sendErrorMessage(player, "core.claim.inventory-full");
        } else {
            mm.sendMessage(player, "core.claim.success", ChatUtils.params("seed_name", plugin.getConfigManager().getCoreSeedName()));
            claimCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    private void handlePlace(Player player, PlayerData playerData) {
        if (playerData.isCoreActive()) {
            mm.sendErrorMessage(player, "core.place.already-active");
            return;
        }
        CoreItemManager itemManager = plugin.getCoreItemManager();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (!itemManager.isCoreSeedItem(itemInHand)) {
            mm.sendErrorMessage(player, "core.place.no-seed");
            return;
        }
        Location exactPlayerLocation = player.getLocation();
        Block targetBlock = exactPlayerLocation.getBlock();
        Location corePlacementLocation = targetBlock.getLocation().clone();
        RegionManager regionManager = plugin.getRegionManager();
        if (regionManager.isLocationProtected(corePlacementLocation)) {
            mm.sendErrorMessage(player, "core.place.protected-area");
            return;
        }
        if (!targetBlock.isEmpty() && !targetBlock.isLiquid() && targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.CAVE_AIR && targetBlock.getType() != Material.VOID_AIR && !targetBlock.isPassable()) {
            corePlacementLocation = exactPlayerLocation.clone().add(0, 1, 0).getBlock().getLocation();
            if (regionManager.isLocationProtected(corePlacementLocation)) {
                mm.sendErrorMessage(player, "core.place.protected-area");
                return;
            }
            Block checkBlock = corePlacementLocation.getBlock();
            if (!checkBlock.isEmpty() && !checkBlock.isLiquid() && checkBlock.getType() != Material.AIR && checkBlock.getType() != Material.CAVE_AIR && checkBlock.getType() != Material.VOID_AIR && !checkBlock.isPassable()) {
                mm.sendErrorMessage(player, "core.place.obstructed");
                return;
            }
        }
        itemInHand.setAmount(itemInHand.getAmount() - 1);
        playerData.setCoreActive(true);
        playerData.setCoreLocation(corePlacementLocation.clone());
        playerData.setLevel(1);
        playerData.setXp(0);
        playerData.setMaxHealth(plugin.getConfigManager().getDefaultCoreMaxHealth());
        playerData.setCurrentHealth(playerData.getMaxHealth());
        playerData.setMaxEnergy(plugin.getConfigManager().getDefaultCoreMaxEnergy());
        playerData.setEnergy(playerData.getMaxEnergy());
        if (playerData.getArchetype() == null && playerData.getLevel() >= 1) {
            String availableArchetypes = plugin.getConfigManager().getArchetypes().values().stream()
                .map(Archetype::getDisplayName)
                .collect(Collectors.joining(ChatUtils.color("&f, "))); // Use ChatUtils.color for the comma for consistency
            mm.sendMessage(player, "core.place.choose-archetype");
            mm.sendMessage(player, "core.place.available-archetypes", ChatUtils.params("archetypes_list", availableArchetypes));
        }
        plugin.getDataManager().savePlayerData(playerData);
        regionManager.addRegion(player.getUniqueId(), corePlacementLocation, player);
        coreEntityManager.spawnCoreEntity(player, corePlacementLocation, playerData.getLevel());
        mm.sendMessage(player, "core.place.success");
        mm.sendMessage(player, "core.place.now-protected");
    }

    private void handleRestore(Player player) {
        Location exactPlayerLocation = player.getLocation();
        Block targetBlock = exactPlayerLocation.getBlock();
        Location coreRestoreLocation = targetBlock.getLocation().clone();
        RegionManager regionManager = plugin.getRegionManager();
        if (regionManager.isLocationProtected(coreRestoreLocation)) {
            mm.sendErrorMessage(player, "core.restore.protected-area");
            return;
        }
        if (!targetBlock.isEmpty() && !targetBlock.isLiquid() && targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.CAVE_AIR && targetBlock.getType() != Material.VOID_AIR && !targetBlock.isPassable()) {
            coreRestoreLocation = exactPlayerLocation.clone().add(0, 1, 0).getBlock().getLocation();
            if (regionManager.isLocationProtected(coreRestoreLocation)) {
                mm.sendErrorMessage(player, "core.restore.protected-area");
                return;
            }
            Block checkBlock = coreRestoreLocation.getBlock();
            if (!checkBlock.isEmpty() && !checkBlock.isLiquid() && checkBlock.getType() != Material.AIR && checkBlock.getType() != Material.CAVE_AIR && checkBlock.getType() != Material.VOID_AIR && !checkBlock.isPassable()) {
                mm.sendErrorMessage(player, "core.restore.obstructed");
                return;
            }
        }
        coreManager.attemptRestoreCore(player, coreRestoreLocation); // CoreManager handles messages for this process
    }

    private void handleFeed(Player player, PlayerData playerData) {
        if (!playerData.isCoreActive()) {
            mm.sendErrorMessage(player, "core.feed.needs-active-core");
            return;
        }
        if (playerData.getLevel() >= plugin.getConfigManager().getMaxCoreLevel()) {
            mm.sendErrorMessage(player, "core.feed.max-level");
            return;
        }
        long cooldownTime = plugin.getConfigManager().getCoreFeedCooldown() * 1000;
        if (feedCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (feedCooldowns.get(player.getUniqueId()) + cooldownTime) - System.currentTimeMillis();
            if (timeLeft > 0) {
                mm.sendErrorMessage(player, "core.feed.cooldown", ChatUtils.params("time", String.valueOf(timeLeft / 1000)));
                return;
            }
        }
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            mm.sendErrorMessage(player, "core.feed.no-item");
            return;
        }
        int xpAmount = plugin.getConfigManager().getDefaultItemSacrificeXP();
        if (xpAmount <= 0) {
            mm.sendErrorMessage(player, "core.feed.no-xp-configured");
            return;
        }
        itemInHand.setAmount(itemInHand.getAmount() - 1);
        if (itemInHand.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(itemInHand);
        }
        player.updateInventory();
        coreManager.addCoreXP(player, xpAmount, "CORE_FEED"); // CoreManager now handles success message for XP gain
        feedCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void handleEnergize(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        energyManager.tryEnergizeWithItem(player, itemInHand); // EnergyManager handles its own messages
    }

    private void handleArchetype(Player player, PlayerData playerData, String[] args) {
        if (!playerData.isCoreActive()) {
            mm.sendErrorMessage(player, "core.archetype.needs-active-core");
            return;
        }
        if (playerData.getArchetype() != null) {
            mm.sendErrorMessage(player, "core.archetype.already-chosen", ChatUtils.params("archetype_name", playerData.getArchetype()));
            return;
        }
        if (args.length < 2) {
            mm.sendMessage(player, "core.archetype.usage");
            String availableArchetypes = plugin.getConfigManager().getArchetypes().values().stream()
                .map(Archetype::getDisplayName) // Using display name for user-friendliness
                .collect(Collectors.joining(ChatUtils.color("&f, ")));
            mm.sendMessage(player, "core.archetype.list-available", ChatUtils.params("archetypes_list", availableArchetypes));
            return;
        }
        String chosenArchetypeId = args[1].toUpperCase();
        Archetype chosenArchetype = plugin.getConfigManager().getArchetype(chosenArchetypeId);
        if (chosenArchetype == null) {
            mm.sendErrorMessage(player, "core.archetype.invalid-name");
            return;
        }
        playerData.setArchetype(chosenArchetype.getId());
        plugin.getDataManager().savePlayerData(playerData);
        mm.sendMessage(player, "core.archetype.success", ChatUtils.params("archetype_display_name", chosenArchetype.getDisplayName()));
        mm.sendMessage(player, "core.archetype.description", ChatUtils.params("description", chosenArchetype.getDescription()));
        skillManager.unlockSkillsForLevel(player, playerData.getLevel());
    }

    private void handleSkill(Player player, PlayerData playerData, String[] args) {
        if (!playerData.isCoreActive()) {
            mm.sendErrorMessage(player, "core.skill.needs-active-core");
            return;
        }
        if (playerData.getArchetype() == null) {
            mm.sendErrorMessage(player, "core.skill.no-archetype");
            return;
        }
        if (args.length < 2) {
            mm.sendMessage(player, "core.skill.usage");
            // TODO: List available skills for the player's archetype and level in messages.yml
            return;
        }
        String skillId = args[1];
        skillManager.tryActivateSkill(player, skillId); // SkillManager handles its own messages
    }

    private void handleRebirth(Player player) {
        mutationManager.attemptRebirth(player); // MutationManager handles its own messages
    }

    private void handleHistory(Player player, PlayerData playerData) {
        mm.sendMessage(player, "core.history.header");
        mm.sendMessage(player, "core.history.rebirths", ChatUtils.params("count", String.valueOf(playerData.getRebirthCount())));
        if (playerData.getActiveMutations().isEmpty()) {
            mm.sendMessage(player, "core.history.no-mutations");
        } else {
            mm.sendMessage(player, "core.history.mutations-header");
            for (String mutationId : playerData.getActiveMutations()) {
                Mutation mutation = plugin.getConfigManager().getMutation(mutationId);
                if (mutation != null) {
                    mm.sendMessage(player, "core.history.mutation-entry",
                        ChatUtils.params("mutation_name", mutation.getName(), "mutation_description", mutation.getDescription()));
                } else {
                    mm.sendMessage(player, "core.history.unknown-mutation", ChatUtils.params("mutation_id", mutationId));
                }
            }
        }
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("claim", "place", "restore", "feed", "energize", "archetype", "skill", "rebirth", "history", "gui", "menu", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("archetype")) {
                return plugin.getConfigManager().getArchetypes().keySet().stream()
                        .map(String::toLowerCase)
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("skill")) {
                Player player = (Player) sender;
                PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
                if (playerData.getArchetype() != null) {
                    Archetype archetype = plugin.getConfigManager().getArchetype(playerData.getArchetype());
                    if (archetype != null) {
                        return archetype.getSkills().values().stream()
                                .filter(skill -> playerData.getUnlockedSkills().contains(skill.getId()))
                                .map(Skill::getId)
                                .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                }
            }
        }
        return new ArrayList<>();
    }
}
