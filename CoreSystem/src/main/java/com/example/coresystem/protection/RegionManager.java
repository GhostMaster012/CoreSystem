package com.example.coresystem.protection;

import com.example.coresystem.CoreSystem;
import com.example.coresystem.PlayerData;
import com.example.coresystem.integration.WorldGuardIntegration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.World; // Added import

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
// import java.util.logging.Level; // Not used directly after changes

public class RegionManager {

    private final CoreSystem plugin;
    private final WorldGuardIntegration wgIntegration;
    // Key: Owner UUID, Value: ProtectedRegion (built-in system)
    private final ConcurrentHashMap<UUID, ProtectedRegion> internalProtectedRegions = new ConcurrentHashMap<>();
    private int protectionRadius;
    private boolean useWorldGuard;

    public RegionManager(CoreSystem plugin) {
        this.plugin = plugin;
        this.wgIntegration = new WorldGuardIntegration(plugin); // Initialize WG integration
        this.protectionRadius = plugin.getConfigManager().getDefaultProtectionRadius();
        this.useWorldGuard = plugin.getConfigManager().isWorldGuardIntegrationEnabled() && wgIntegration.isEnabled();

        if (useWorldGuard) {
            plugin.getLogger().info("RegionManager: Using WorldGuard for protection.");
        } else {
            plugin.getLogger().info("RegionManager: Using built-in protection system. Radius: " + protectionRadius);
        }
        // loadActiveCoreRegions(); // Initial loading might be complex with WG, handle on player join/core place
    }

    // public void addRegion(Player player, Location coreLocation) { // Kept for potential direct use
    //    addRegion(player.getUniqueId(), coreLocation, player);
    // }

    public void addRegion(UUID ownerUUID, Location coreLocation, Player playerInstance) { // playerInstance can be null if offline
        if (coreLocation == null || coreLocation.getWorld() == null) {
            plugin.getLogger().warning("Attempted to add a protected region with invalid location for owner: " + ownerUUID);
            return;
        }

        if (useWorldGuard) {
            if (playerInstance != null && playerInstance.isOnline()) { // WG region creation needs an online player context for ownership
                wgIntegration.createProtectedRegion(playerInstance, coreLocation, protectionRadius);
            } else {
                 plugin.getLogger().warning("Cannot create WorldGuard region for offline player " + ownerUUID + ". Region will be created on next login if core is still active.");
                 // Optionally, queue this or handle on player join.
                 // For now, internal protection might be used as a temporary fallback if player is offline during this call,
                 // but that adds complexity. Better to ensure player is online or handle on join.
                 // If we must support offline region creation and WG needs Player, this is an issue.
                 // WG addPlayer to domain can use UUID, so it's possible.
            }
        } else {
            ProtectedRegion region = new ProtectedRegion(ownerUUID, coreLocation, protectionRadius);
            internalProtectedRegions.put(ownerUUID, region);
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Built-in protected region added for " + ownerUUID + " at " + coreLocation);
            }
        }
    }

    public void removeRegion(UUID ownerUUID, World world) { // World is needed for WG
        if (useWorldGuard) {
            wgIntegration.removeProtectedRegion(ownerUUID, world);
        } else {
            ProtectedRegion removedRegion = internalProtectedRegions.remove(ownerUUID);
            if (removedRegion != null && plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Built-in protected region removed for " + ownerUUID);
            }
        }
    }

    // This might be complex if WG is used, as WG regions are not directly stored here.
    // public ProtectedRegion getRegion(UUID ownerUUID) {
    //     if (useWorldGuard) {
    //         // TODO: Query WorldGuard for the region if needed, or return a wrapper.
    //         // This method might become less relevant if all checks go through canBuild/isLocationProtected.
    //         return null;
    //     }
    //     return internalProtectedRegions.get(ownerUUID);
    // }

    // This method is primarily for the internal system.
    private ProtectedRegion getInternalRegionAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        for (ProtectedRegion region : internalProtectedRegions.values()) {
            if (!region.getWorld().equals(location.getWorld())) continue;
            if (region.contains(location)) {
                return region;
            }
        }
        return null;
    }

    public boolean isLocationProtected(Location location) {
        if (useWorldGuard) {
            // A simple check could be to see if ANY region protects it,
            // but more specifically we care if CoreSystem's region protects it.
            // WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(BukkitAdapter.adapt(location)).size() > 0;
            // This doesn't tell us if it's OUR core region.
            // For now, if WG is on, we assume WG handles all protection checks via its events.
            // If we need to check specifically for OUR core regions in WG, it's more complex.
            // The canBuild method is a better check for WG context.
            return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(BukkitAdapter.adapt(location)).size() > 0;

        }
        return getInternalRegionAt(location) != null;
    }

    public boolean canBuild(Player player, Location location) {
        if (useWorldGuard) {
            return wgIntegration.canBuild(player, location);
        } else {
            ProtectedRegion region = getInternalRegionAt(location);
            if (region == null) {
                return true;
            }
            return region.isOwner(player);
        }
    }

    public void loadRegionForPlayerData(PlayerData playerData, Player playerInstance) { // playerInstance can be null
        if (playerData.isCoreActive() && playerData.getCoreLocation() != null) {
            if (useWorldGuard) {
                // WG regions are persistent. We just need to ensure our understanding is sync.
                // For now, no specific action on load if WG is used, as WG handles persistence.
                // We might want to verify the region exists and has correct owner/flags.
                if (playerInstance != null && plugin.getConfigManager().isDebugMode()) {
                     plugin.getLogger().info("WorldGuard is active. Region for " + playerData.getPlayerUUID() + " should be managed by WorldGuard.");
                }
            } else {
                if (!internalProtectedRegions.containsKey(playerData.getPlayerUUID())) {
                    addRegion(playerData.getPlayerUUID(), playerData.getCoreLocation(), playerInstance); // playerInstance is null here if offline
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("Loaded built-in protected region for " + playerData.getPlayerUUID() + " from PlayerData.");
                    }
                }
            }
        }
    }

    public Collection<ProtectedRegion> getAllInternalRegions(){ // Renamed for clarity
        return internalProtectedRegions.values();
    }

    public void updateProtectionRadius() { // This primarily affects the internal system
        int newRadius = plugin.getConfigManager().getDefaultProtectionRadius();
        boolean newUseWorldGuard = plugin.getConfigManager().isWorldGuardIntegrationEnabled() && wgIntegration.isEnabled();

        if (newRadius != this.protectionRadius || newUseWorldGuard != this.useWorldGuard) {
            plugin.getLogger().info("Protection settings changed. Radius: " + newRadius + ", UseWorldGuard: " + newUseWorldGuard + ". Re-evaluating regions...");
            this.protectionRadius = newRadius;
            this.useWorldGuard = newUseWorldGuard;

            // This is complex. If switching from WG to internal or vice-versa, or radius change with WG,
            // existing regions need to be removed from one system and added to another, or resized.
            // For simplicity now, this method might just update the radius for future internal regions.
            // A full re-initialization would iterate all active PlayerData.

            // If using internal system, re-create internal regions
            if (!this.useWorldGuard) {
                ConcurrentHashMap<UUID, ProtectedRegion> oldRegions = new ConcurrentHashMap<>(internalProtectedRegions);
                internalProtectedRegions.clear();
                for (ProtectedRegion oldRegion : oldRegions.values()) {
                    // We need player instance for WG, but not for internal.
                    // This assumes that if we are here, it's because useWorldGuard is false.
                    addRegion(oldRegion.getOwnerUUID(), oldRegion.getCoreLocation(), Bukkit.getPlayer(oldRegion.getOwnerUUID()));
                }
                 plugin.getLogger().info("Internal protected regions have been updated with the new radius if applicable.");
            } else {
                 plugin.getLogger().info("WorldGuard is active. Radius changes for WG regions would need manual adjustment or a more complex update logic.");
                 // For WG, if radius changed, we'd ideally remove and re-create WG regions for ALL active cores.
            }
        }
    }

    public boolean isUsingWorldGuard() {
        return this.useWorldGuard;
    }
}
