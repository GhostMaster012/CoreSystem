package com.example.coresystem.entity;

import com.example.coresystem.CoreEvolutionState;
import com.example.coresystem.CoreSystem;
import com.example.coresystem.PlayerData;
// import com.example.coresystem.utils.ChatUtils; // Not used here currently
import org.bukkit.Bukkit;
import org.bukkit.Location;
// import org.bukkit.Material; // Not used here currently
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CoreEntityManager {

    private final CoreSystem plugin;
    private final NamespacedKey coreEntityKey;
    private final ConcurrentHashMap<UUID, UUID> activeCoreEntities = new ConcurrentHashMap<>(); // Player UUID -> ArmorStand Entity UUID

    public CoreEntityManager(CoreSystem plugin) {
        this.plugin = plugin;
        this.coreEntityKey = new NamespacedKey(plugin, "core_entity_owner_uuid");
        loadExistingCoreEntities();
    }

    public void spawnCoreEntity(Player player, Location location, int coreLevel) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Attempted to spawn core entity for " + player.getName() + " at null location or world.");
            return;
        }

        removeCoreEntity(player.getUniqueId(), false);

        // Spawn slightly offset to be in the center of the block visually, and on top of the block.
        Location spawnLocation = location.clone().add(0.5, 0, 0.5); // Center of the block, at Y=0 of block
        spawnLocation.setYaw(player.getLocation().getYaw()); // Face the same direction as player (optional)


        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(spawnLocation, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setCollidable(false);
        armorStand.setMarker(true);
        // armorStand.setSmall(true); // Make this configurable or based on model later
        armorStand.getPersistentDataContainer().set(coreEntityKey, PersistentDataType.STRING, player.getUniqueId().toString());

        activeCoreEntities.put(player.getUniqueId(), armorStand.getUniqueId());
        updateCoreAppearance(player.getUniqueId(), coreLevel);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Spawned core entity for " + player.getName() + " at " + spawnLocation);
        }
    }

    public void updateCoreAppearance(UUID playerUUID, int newLevel) {
        UUID armorStandUUID = activeCoreEntities.get(playerUUID);
        ArmorStand armorStand = null;

        if (armorStandUUID != null) {
            Entity entity = Bukkit.getEntity(armorStandUUID);
            if (entity instanceof ArmorStand) {
                armorStand = (ArmorStand) entity;
            } else {
                 activeCoreEntities.remove(playerUUID); // Clean up cache if UUID points to wrong/no entity
            }
        }

        // If not found in cache or entity is invalid, try to find it in the world
        if (armorStand == null) {
            armorStand = findCoreEntityByOwner(playerUUID);
            if (armorStand != null) {
                activeCoreEntities.put(playerUUID, armorStand.getUniqueId());
            }
        }

        // If still not found, and player is online, attempt to respawn.
        if (armorStand == null) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().warning("No active core entity found for player " + playerUUID + " to update appearance.");
            }
            Player onlinePlayer = Bukkit.getPlayer(playerUUID);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                PlayerData pd = plugin.getDataManager().getPlayerData(playerUUID);
                if (pd.isCoreActive() && pd.getCoreLocation() != null) {
                    plugin.getLogger().info("Re-spawning missing core entity for " + onlinePlayer.getName() + " during updateCoreAppearance.");
                    spawnCoreEntity(onlinePlayer, pd.getCoreLocation(), newLevel);
                }
            }
            return;
        }

        // At this point, armorStand should be valid
        CoreEvolutionState state = plugin.getConfigManager().getEvolutionStateForLevel(newLevel);
        ItemStack headItem = new ItemStack(state.getItemMaterial());
        ItemMeta meta = headItem.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(state.getCustomModelData());
            headItem.setItemMeta(meta);
        }
        armorStand.getEquipment().setHelmet(headItem);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Updated core appearance for player " + playerUUID + " to level " + newLevel + " (CMD: " + state.getCustomModelData() + ")");
        }
    }

    public void removeCoreEntity(UUID playerUUID, boolean fromDestruction) {
        UUID armorStandUUID = activeCoreEntities.remove(playerUUID);
        Entity entity = null;
        if (armorStandUUID != null) {
            entity = Bukkit.getEntity(armorStandUUID);
        } else {
            ArmorStand foundStand = findCoreEntityByOwner(playerUUID);
            if (foundStand != null) {
                entity = foundStand;
            }
        }

        if (entity instanceof ArmorStand) {
            entity.remove();
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Removed core entity for player " + playerUUID);
            }
        } else if (armorStandUUID != null && plugin.getConfigManager().isDebugMode()) {
             plugin.getLogger().warning("Core entity UUID " + armorStandUUID + " for player " + playerUUID + " (to be removed) was not an ArmorStand or not found.");
        }
    }

    public void removeAllCoreEntities() {
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Attempting to remove all (" + activeCoreEntities.size() + ") active core entities...");
        }
        for (UUID armorStandUUID : new ArrayList<>(activeCoreEntities.values())) { // Iterate copy to allow removal
            Entity entity = Bukkit.getEntity(armorStandUUID);
            if (entity instanceof ArmorStand) {
                entity.remove();
            }
        }
        activeCoreEntities.clear();
         if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("All visual core entities removed.");
        }
    }

    public ArmorStand findCoreEntityByOwner(UUID ownerUUID) {
        // Check online player's world first for optimization if player is online
        Player player = Bukkit.getPlayer(ownerUUID);
        if (player != null && player.isOnline()) {
            for (Entity entity : player.getWorld().getEntitiesByClass(ArmorStand.class)) {
                 String ownerUUIDString = entity.getPersistentDataContainer().get(coreEntityKey, PersistentDataType.STRING);
                if (ownerUUIDString != null && ownerUUIDString.equals(ownerUUID.toString())) {
                    return (ArmorStand) entity;
                }
            }
        }
        // Fallback to all worlds if not found in player's current world or player is offline
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(ArmorStand.class)) {
                String ownerUUIDString = entity.getPersistentDataContainer().get(coreEntityKey, PersistentDataType.STRING);
                if (ownerUUIDString != null && ownerUUIDString.equals(ownerUUID.toString())) {
                    return (ArmorStand) entity;
                }
            }
        }
        return null;
    }

    public void loadExistingCoreEntities() {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Scanning for existing core entities on server load/reload...");
                int repopulatedCount = 0;
                int orphanedRemovedCount = 0;

                for (World world : Bukkit.getWorlds()) {
                    for (ArmorStand armorStand : world.getEntitiesByClass(ArmorStand.class)) {
                        String ownerUUIDString = armorStand.getPersistentDataContainer().get(coreEntityKey, PersistentDataType.STRING);
                        if (ownerUUIDString != null) {
                            UUID ownerUUID;
                            try {
                                ownerUUID = UUID.fromString(ownerUUIDString);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Found core entity with invalid owner UUID tag: " + ownerUUIDString + ". Removing.");
                                armorStand.remove();
                                orphanedRemovedCount++;
                                continue;
                            }

                            PlayerData pd = plugin.getDataManager().getPlayerData(ownerUUID); // Loads if not cached
                            if (pd.isCoreActive() && pd.getCoreLocation() != null) {
                                // Check if location matches roughly (ArmorStand might be slightly offset)
                                if (armorStand.getLocation().getWorld().equals(pd.getCoreLocation().getWorld()) &&
                                    armorStand.getLocation().distanceSquared(pd.getCoreLocation().clone().add(0.5,0,0.5)) < 2.0) { // Allow some tolerance

                                    activeCoreEntities.put(ownerUUID, armorStand.getUniqueId());
                                    updateCoreAppearance(ownerUUID, pd.getLevel());
                                    repopulatedCount++;
                                } else {
                                     plugin.getLogger().warning("Found core entity for " + ownerUUID + " at wrong location ("+armorStand.getLocation()+") vs PlayerData ("+pd.getCoreLocation()+"). Removing old, will respawn if player logs in.");
                                     armorStand.remove();
                                     orphanedRemovedCount++;
                                }
                            } else {
                                plugin.getLogger().warning("Found orphaned core entity for " + ownerUUID + " (PlayerData says inactive). Removing.");
                                armorStand.remove();
                                orphanedRemovedCount++;
                            }
                        }
                    }
                }
                plugin.getLogger().info("Finished scanning. Repopulated/verified " + repopulatedCount + " core entities. Removed " + orphanedRemovedCount + " orphaned entities.");
            }
        }.runTaskLater(plugin, 40L); // Increased delay to allow worlds and other plugins to load
    }

    // Called by DataManager when player data is loaded (e.g., on join)
    public void ensureCoreEntityExists(Player player, Location coreLocation, int coreLevel) {
        if (coreLocation == null || player == null) return;

        UUID playerUUID = player.getUniqueId();
        if (activeCoreEntities.containsKey(playerUUID)) {
            Entity existing = Bukkit.getEntity(activeCoreEntities.get(playerUUID));
            if (existing instanceof ArmorStand && existing.isValid()) {
                // It exists and is valid, just update appearance
                updateCoreAppearance(playerUUID, coreLevel);
                return;
            } else {
                // Cache had an invalid/dead entity
                activeCoreEntities.remove(playerUUID);
            }
        }

        // Try to find it in the world if not in cache or cache was invalid
        ArmorStand armorStand = findCoreEntityByOwner(playerUUID);
        if (armorStand != null) {
            activeCoreEntities.put(playerUUID, armorStand.getUniqueId());
            updateCoreAppearance(playerUUID, coreLevel);
        } else {
            // Not in cache, not in world, must spawn it.
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Core entity for " + player.getName() + " not found on data load. Spawning new one.");
            }
            spawnCoreEntity(player, coreLocation, coreLevel);
        }
    }

    public UUID getCoreEntityUUID(UUID playerUUID) {
        return activeCoreEntities.get(playerUUID);
    }
}
