package com.example.coresystem.api;

import com.example.coresystem.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable; // Para indicar que el resultado puede ser null
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Interface for the CoreSystem API, allowing other plugins to interact with CoreSystem functionalities.
 */
public interface CoreSystemAPI {

    /**
     * Retrieves the core data for the specified player.
     * Note: Modifying the returned PlayerData object directly is not recommended.
     * Use API methods for changes to ensure proper event handling and data consistency.
     *
     * @param player The player whose core data is to be retrieved.
     * @return PlayerData object for the player, or null if data cannot be retrieved.
     */
    @Nullable
    PlayerData getCoreData(@NotNull Player player);

    /**
     * Retrieves the core data for the specified player UUID.
     * Note: Modifying the returned PlayerData object directly is not recommended.
     * Use API methods for changes to ensure proper event handling and data consistency.
     *
     * @param playerUUID The UUID of the player whose core data is to be retrieved.
     * @return PlayerData object for the player, or null if data cannot be retrieved.
     */
    @Nullable
    PlayerData getCoreData(@NotNull UUID playerUUID);

    /**
     * Adds experience points to a player's core.
     * This will trigger a {@link com.example.coresystem.events.CoreGainXPEvent}.
     *
     * @param player The player to add XP to.
     * @param amount The amount of XP to add. Must be positive.
     * @param reason A string describing the reason for the XP gain (e.g., "MOB_KILL", "QUEST_REWARD").
     * @return true if XP was successfully processed (event not cancelled, player has active core), false otherwise.
     */
    boolean addCoreXP(@NotNull Player player, double amount, @NotNull String reason);

    /**
     * Adds experience points to a player's core by UUID.
     * This will trigger a {@link com.example.coresystem.events.CoreGainXPEvent} if the player is online.
     *
     * @param playerUUID The UUID of the player to add XP to.
     * @param amount The amount of XP to add. Must be positive.
     * @param reason A string describing the reason for the XP gain.
     * @return true if XP was successfully processed, false otherwise (e.g., player offline, no active core).
     */
    boolean addCoreXP(@NotNull UUID playerUUID, double amount, @NotNull String reason);

    /**
     * Sets the energy level for a player's core.
     * Energy will be capped between 0 and the player's max energy.
     *
     * @param player The player whose core energy is to be set.
     * @param amount The new energy amount.
     * @return true if energy was set (player has active core), false otherwise.
     */
    boolean setCoreEnergy(@NotNull Player player, double amount);

    /**
     * Sets the energy level for a player's core by UUID.
     * Energy will be capped between 0 and the player's max energy.
     * This method might only work effectively if the player is online or if data is modified directly for offline players.
     *
     * @param playerUUID The UUID of the player.
     * @param amount The new energy amount.
     * @return true if energy was set, false otherwise.
     */
    boolean setCoreEnergy(@NotNull UUID playerUUID, double amount);

    /**
     * Gets the current energy level of a player's core.
     *
     * @param player The player.
     * @return The current energy, or 0.0 if no core or player data.
     */
    double getCoreEnergy(@NotNull Player player);

    /**
     * Gets the current energy level of a player's core by UUID.
     *
     * @param playerUUID The UUID of the player.
     * @return The current energy, or 0.0 if no core or player data.
     */
    double getCoreEnergy(@NotNull UUID playerUUID);

    /**
     * Gets the maximum energy capacity of a player's core.
     *
     * @param player The player.
     * @return The maximum energy, or default max energy if no core or player data.
     */
    double getMaxCoreEnergy(@NotNull Player player);

    /**
     * Gets the maximum energy capacity of a player's core by UUID.
     *
     * @param playerUUID The UUID of the player.
     * @return The maximum energy, or default max energy if no core or player data.
     */
    double getMaxCoreEnergy(@NotNull UUID playerUUID);

    /**
     * Gets the current level of a player's core.
     *
     * @param player The player.
     * @return The core level, or 0 if no core or player data.
     */
    int getCoreLevel(@NotNull Player player);

    /**
     * Gets the current level of a player's core by UUID.
     *
     * @param playerUUID The UUID of the player.
     * @return The core level, or 0 if no core or player data.
     */
    int getCoreLevel(@NotNull UUID playerUUID);

    /**
     * Checks if the player's core has undergone at least one rebirth.
     *
     * @param player The player to check.
     * @return true if the player has a rebirth count greater than 0, false otherwise.
     */
    boolean hasRebirthed(@NotNull Player player);

    /**
     * Checks if the player's core has undergone at least one rebirth by UUID.
     *
     * @param playerUUID The UUID of the player to check.
     * @return true if the player has a rebirth count greater than 0, false otherwise.
     */
    boolean hasRebirthed(@NotNull UUID playerUUID);

     /**
     * Gets the rebirth count for a player's core.
     *
     * @param player The player.
     * @return The number of rebirths, or 0 if no core or player data.
     */
    int getRebirthCount(@NotNull Player player);

    /**
     * Gets the rebirth count for a player's core by UUID.
     *
     * @param playerUUID The UUID of the player.
     * @return The number of rebirths, or 0 if no core or player data.
     */
    int getRebirthCount(@NotNull UUID playerUUID);

    /**
     * Checks if a player's core is currently active (placed and not destroyed).
     *
     * @param player The player.
     * @return true if the core is active, false otherwise.
     */
    boolean isCoreActive(@NotNull Player player);

    /**
     * Checks if a player's core is currently active by UUID.
     *
     * @param playerUUID The UUID of the player.
     * @return true if the core is active, false otherwise.
     */
    boolean isCoreActive(@NotNull UUID playerUUID);

    /**
     * Gets the location of a player's active core.
     *
     * @param player The player.
     * @return The Location of the core, or null if not active or location not set.
     */
    @Nullable
    Location getCoreLocation(@NotNull Player player);

    /**
     * Gets the location of a player's active core by UUID.
     *
     * @param playerUUID The UUID of the player.
     * @return The Location of the core, or null if not active or location not set.
     */
    @Nullable
    Location getCoreLocation(@NotNull UUID playerUUID);
}
