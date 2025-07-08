package com.example.coresystem.api;

import com.example.coresystem.CoreManager;
import com.example.coresystem.CoreSystem;
import com.example.coresystem.DataManager;
import com.example.coresystem.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CoreSystemAPIImpl implements CoreSystemAPI {

    private final CoreSystem plugin;
    private final DataManager dataManager;
    private final CoreManager coreManager;

    public CoreSystemAPIImpl(CoreSystem plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.coreManager = plugin.getCoreManager();
    }

    @Override
    @Nullable
    public PlayerData getCoreData(@NotNull Player player) {
        return dataManager.getPlayerData(player.getUniqueId());
    }

    @Override
    @Nullable
    public PlayerData getCoreData(@NotNull UUID playerUUID) {
        return dataManager.getPlayerData(playerUUID);
    }

    @Override
    public boolean addCoreXP(@NotNull Player player, double amount, @NotNull String reason) {
        if (amount <= 0) return false;
        PlayerData playerData = dataManager.getPlayerData(player.getUniqueId());
        if (!playerData.isCoreActive()) return false;

        coreManager.addCoreXP(player, amount, reason); // This method already handles events and saving
        return true;
    }

    @Override
    public boolean addCoreXP(@NotNull UUID playerUUID, double amount, @NotNull String reason) {
        if (amount <= 0) return false;
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            return addCoreXP(player, amount, reason);
        } else {
            // Handling XP for offline players can be complex.
            // Option 1: Load data, modify, save. (Risk: concurrent modification if player logs in)
            // Option 2: Queue XP gain until next login.
            // Option 3: Disallow for offline players via API for simplicity. (Chosen for now)
            plugin.getLogger().warning("Attempted to add CoreXP to offline player " + playerUUID + " via API. This is not fully supported for offline players currently.");
            // If direct data modification is desired for offline:
            // PlayerData playerData = dataManager.getPlayerData(playerUUID);
            // if (playerData != null && playerData.isCoreActive()) {
            //    playerData.addXp(amount, reason); // This bypasses CoreGainXPEvent if player is offline
            //    dataManager.savePlayerData(playerData);
            //    return true;
            // }
            return false;
        }
    }

    @Override
    public boolean setCoreEnergy(@NotNull Player player, double amount) {
        PlayerData playerData = dataManager.getPlayerData(player.getUniqueId());
        if (!playerData.isCoreActive()) return false;
        playerData.setEnergy(amount);
        dataManager.savePlayerData(playerData);
        return true;
    }

    @Override
    public boolean setCoreEnergy(@NotNull UUID playerUUID, double amount) {
         PlayerData playerData = dataManager.getPlayerData(playerUUID); // Loads or gets from cache
         if (playerData != null) { // No direct isCoreActive check here, could be setting for an inactive core if needed
            playerData.setEnergy(amount);
            dataManager.savePlayerData(playerData);
            return true;
         }
         return false;
    }

    @Override
    public double getCoreEnergy(@NotNull Player player) {
        PlayerData pd = getCoreData(player);
        return pd != null ? pd.getEnergy() : 0.0;
    }

    @Override
    public double getCoreEnergy(@NotNull UUID playerUUID) {
        PlayerData pd = getCoreData(playerUUID);
        return pd != null ? pd.getEnergy() : 0.0;
    }

    @Override
    public double getMaxCoreEnergy(@NotNull Player player) {
        PlayerData pd = getCoreData(player);
        return pd != null ? pd.getMaxEnergy() : plugin.getConfigManager().getDefaultCoreMaxEnergy();
    }

    @Override
    public double getMaxCoreEnergy(@NotNull UUID playerUUID) {
        PlayerData pd = getCoreData(playerUUID);
        return pd != null ? pd.getMaxEnergy() : plugin.getConfigManager().getDefaultCoreMaxEnergy();
    }

    @Override
    public int getCoreLevel(@NotNull Player player) {
        PlayerData pd = getCoreData(player);
        return pd != null ? pd.getLevel() : 0;
    }

    @Override
    public int getCoreLevel(@NotNull UUID playerUUID) {
        PlayerData pd = getCoreData(playerUUID);
        return pd != null ? pd.getLevel() : 0;
    }

    @Override
    public boolean hasRebirthed(@NotNull Player player) {
        PlayerData pd = getCoreData(player);
        return pd != null && pd.getRebirthCount() > 0;
    }

    @Override
    public boolean hasRebirthed(@NotNull UUID playerUUID) {
        PlayerData pd = getCoreData(playerUUID);
        return pd != null && pd.getRebirthCount() > 0;
    }

    @Override
    public int getRebirthCount(@NotNull Player player) {
        PlayerData pd = getCoreData(player);
        return pd != null ? pd.getRebirthCount() : 0;
    }

    @Override
    public int getRebirthCount(@NotNull UUID playerUUID) {
        PlayerData pd = getCoreData(playerUUID);
        return pd != null ? pd.getRebirthCount() : 0;
    }

    @Override
    public boolean isCoreActive(@NotNull Player player) {
        PlayerData pd = getCoreData(player);
        return pd != null && pd.isCoreActive();
    }

    @Override
    public boolean isCoreActive(@NotNull UUID playerUUID) {
        PlayerData pd = getCoreData(playerUUID);
        return pd != null && pd.isCoreActive();
    }

    @Override
    @Nullable
    public Location getCoreLocation(@NotNull Player player) {
        PlayerData pd = getCoreData(player);
        return pd != null && pd.isCoreActive() ? pd.getCoreLocation() : null;
    }

    @Override
    @Nullable
    public Location getCoreLocation(@NotNull UUID playerUUID) {
        PlayerData pd = getCoreData(playerUUID);
        return pd != null && pd.isCoreActive() ? pd.getCoreLocation() : null;
    }
}
