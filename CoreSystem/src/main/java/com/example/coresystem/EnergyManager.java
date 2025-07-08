package com.example.coresystem;

import com.example.coresystem.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnergyManager {

    private final CoreSystem plugin;
    private BukkitTask passiveRegenTask;
    private final Map<UUID, Long> energizeCooldowns = new ConcurrentHashMap<>();
    private final DecimalFormat df = new DecimalFormat("#.#");


    public EnergyManager(CoreSystem plugin) {
        this.plugin = plugin;
        startPassiveRegeneration();
    }

    public void startPassiveRegeneration() {
        if (!plugin.getConfigManager().isPassiveEnergyRegenEnabled()) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Passive energy regeneration is disabled.");
            }
            return;
        }

        if (passiveRegenTask != null && !passiveRegenTask.isCancelled()) {
            passiveRegenTask.cancel();
        }

        long intervalTicks = plugin.getConfigManager().getPassiveEnergyRegenIntervalSeconds() * 20L;
        if (intervalTicks <= 0) {
             plugin.getLogger().warning("Passive energy regeneration interval is zero or negative, disabling feature.");
            return;
        }

        // Base regen amount from config
        double baseRegenAmount = plugin.getConfigManager().getPassiveEnergyRegenAmount();
        // Note: The actual regenAmount will be calculated per player inside the runnable,
        // including mutation boosts.

        if (baseRegenAmount <= 0 && plugin.getMutationManager().getPassiveEnergyRegenBoost(null) <=0 ) { // Check if any boost could make it positive
            plugin.getLogger().info("Passive energy regeneration amount (and potential boosts) is zero or negative, feature effectively disabled.");
        }


        passiveRegenTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
                    if (playerData.isCoreActive() && playerData.getEnergy() < playerData.getMaxEnergy()) {

                        double effectiveRegenAmount = baseRegenAmount + plugin.getMutationManager().getPassiveEnergyRegenBoost(playerData);
                        if (effectiveRegenAmount <= 0) continue; // Skip if no effective regen

                        double currentEnergy = playerData.getEnergy();
                        playerData.addEnergy(effectiveRegenAmount);

                        if (playerData.getEnergy() > currentEnergy && plugin.getConfigManager().isDebugMode()) {
                             // Optional: Send message only if a significant amount was regenerated or if it reached full
                             // ChatUtils.sendMessage(player, "&bYour Core passively regenerated " + df.format(effectiveRegenAmount) + " energy. Current: " + df.format(playerData.getEnergy()));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
        plugin.getLogger().info("Passive energy regeneration task started. Base Amount: " + baseRegenAmount + ", Interval: " + intervalTicks/20 + "s.");
    }

    public void stopPassiveRegeneration() {
        if (passiveRegenTask != null && !passiveRegenTask.isCancelled()) {
            passiveRegenTask.cancel();
            passiveRegenTask = null;
            plugin.getLogger().info("Passive energy regeneration task stopped.");
        }
    }

    public void tryEnergizeWithItem(Player player, ItemStack itemInHand) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (!playerData.isCoreActive()) {
            ChatUtils.sendErrorMessage(player, "You need an active Core to energize it.");
            return;
        }
        if (playerData.getEnergy() >= playerData.getMaxEnergy()) {
            ChatUtils.sendMessage(player, "&bYour Core energy is already full!");
            return;
        }

        long cooldownTime = plugin.getConfigManager().getCoreEnergizeCooldown() * 1000;
        if (cooldownTime > 0 && energizeCooldowns.containsKey(player.getUniqueId())) {
            long timeLeft = (energizeCooldowns.get(player.getUniqueId()) + cooldownTime) - System.currentTimeMillis();
            if (timeLeft > 0) {
                ChatUtils.sendErrorMessage(player, "You must wait " + (timeLeft / 1000) + " seconds before energizing again.");
                return;
            }
        }

        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            ChatUtils.sendErrorMessage(player, "You are not holding any item to energize your Core.");
            return;
        }

        Material itemType = itemInHand.getType();
        Map<Material, Integer> energyItems = plugin.getConfigManager().getEnergyItems();

        if (!energyItems.containsKey(itemType)) {
            ChatUtils.sendErrorMessage(player, "&c" + itemType.name().replace("_"," ").toLowerCase() + " cannot be used to energize your Core.");
            return;
        }

        int energyGained = energyItems.get(itemType);
        if (energyGained <= 0) {
            ChatUtils.sendErrorMessage(player, "&cThis item ("+itemType.name().toLowerCase()+") is configured to give no energy.");
            return;
        }

        itemInHand.setAmount(itemInHand.getAmount() - 1);
        if (itemInHand.getAmount() <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            player.getInventory().setItemInMainHand(itemInHand);
        }
        player.updateInventory();

        // double oldEnergy = playerData.getEnergy(); // Not strictly needed here
        playerData.addEnergy(energyGained);
        plugin.getDataManager().savePlayerData(playerData);

        ChatUtils.sendMessage(player, "&aYou energized your Core with &e" + itemType.name().replace("_"," ").toLowerCase() + "&a, gaining &b" + df.format(energyGained) + "&a energy!");
        ChatUtils.sendMessage(player, "&7Current Energy: &b" + df.format(playerData.getEnergy()) + "&7/&b" + df.format(playerData.getMaxEnergy()));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.8f);

        if (cooldownTime > 0) {
            energizeCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    public void reload() {
        stopPassiveRegeneration();
        startPassiveRegeneration();
        energizeCooldowns.clear();
    }
}
