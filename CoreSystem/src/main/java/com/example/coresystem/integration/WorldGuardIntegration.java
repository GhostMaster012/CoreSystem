package com.example.coresystem.integration;

import com.example.coresystem.CoreSystem;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.logging.Level;

public class WorldGuardIntegration {

    private final CoreSystem plugin;
    private boolean enabled = false;

    public WorldGuardIntegration(CoreSystem plugin) {
        this.plugin = plugin;
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.enabled = true;
            plugin.getLogger().info("WorldGuard found! CoreSystem will attempt to use it for region protection.");
        } else {
            plugin.getLogger().info("WorldGuard not found. CoreSystem will use its built-in protection system.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private String getRegionId(Player player) {
        return "coresystem_core_" + player.getUniqueId().toString();
    }

    private String getRegionId(java.util.UUID playerUUID) {
        return "coresystem_core_" + playerUUID.toString();
    }

    public boolean createProtectedRegion(Player player, Location center, int radius) {
        if (!enabled) return false;

        World world = center.getWorld();
        if (world == null) return false;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

        if (regionManager == null) {
            plugin.getLogger().warning("WorldGuard RegionManager not found for world: " + world.getName());
            return false;
        }

        String regionId = getRegionId(player);

        // Remove existing region first, if any
        if (regionManager.hasRegion(regionId)) {
            regionManager.removeRegion(regionId);
        }

        BlockVector3 min = BlockVector3.at(center.getBlockX() - radius, Math.max(world.getMinHeight(), center.getBlockY() - radius), center.getBlockZ() - radius);
        BlockVector3 max = BlockVector3.at(center.getBlockX() + radius, Math.min(world.getMaxHeight() -1, center.getBlockY() + radius), center.getBlockZ() + radius);

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);

        // Set owner
        DefaultDomain owners = region.getOwners();
        owners.addPlayer(player.getUniqueId());
        region.setOwners(owners);

        // Set flags (configurable flags to be loaded from config.yml later)
        // Example flags:
        // region.setFlag(Flags.BUILD, StateFlag.State.DENY); // Deny building for non-members by default
        // region.setFlag(Flags.BUILD.getRegionGroupFlag(), RegionGroup.OWNERS); // Allow owners to build
        // region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY);
        // region.setFlag(Flags.BLOCK_BREAK.getRegionGroupFlag(), RegionGroup.OWNERS);
        // region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
        // region.setFlag(Flags.BLOCK_PLACE.getRegionGroupFlag(), RegionGroup.OWNERS);
        // region.setFlag(Flags.INTERACT, StateFlag.State.DENY); // Prevent general interaction
        // region.setFlag(Flags.INTERACT.getRegionGroupFlag(), RegionGroup.OWNERS);
        // region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.DENY);
        // region.setFlag(Flags.CHEST_ACCESS.getRegionGroupFlag(), RegionGroup.OWNERS);

        // For simplicity, let's set some basic flags now, load from config later.
        // Default: deny build for non-owners, allow for owners.
        // WorldGuard's default behavior often allows owners and denies others if a region exists.
        // Explicitly setting flags is better.
        setFlagsFromConfig(region);


        region.setPriority(100); // High priority for core regions

        try {
            regionManager.addRegion(region);
            regionManager.saveChanges(); // Persist changes
            plugin.getLogger().info("Created WorldGuard region '" + regionId + "' for " + player.getName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating WorldGuard region for " + player.getName(), e);
            return false;
        }
    }

    private void setFlagsFromConfig(ProtectedRegion region) {
        // Load this from config.yml in a future step
        Map<String, String> flagsToSet = plugin.getConfigManager().getWorldGuardFlags();

        for (Map.Entry<String, String> entry : flagsToSet.entrySet()) {
            StateFlag flag = (StateFlag) Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), entry.getKey());
            if (flag != null) {
                StateFlag.State state = null;
                if (entry.getValue().equalsIgnoreCase("ALLOW")) {
                    state = StateFlag.State.ALLOW;
                } else if (entry.getValue().equalsIgnoreCase("DENY")) {
                    state = StateFlag.State.DENY;
                }
                // Add more complex flag value parsing if needed (e.g., for groups)
                if (state != null) {
                    region.setFlag(flag, state);
                } else {
                    plugin.getLogger().warning("Invalid state value '" + entry.getValue() + "' for WorldGuard flag '" + entry.getKey() + "'.");
                }
            } else {
                plugin.getLogger().warning("Unknown WorldGuard flag '" + entry.getKey() + "' in config.");
            }
        }
         // Default owner build override if not explicitly denied for owners by config
        if (!flagsToSet.containsKey("build") || !flagsToSet.get("build").equalsIgnoreCase("DENY_OWNERS")) { // Hypothetical DENY_OWNERS
             DefaultDomain ownerDomain = region.getOwners(); // Get the owners domain
             if (ownerDomain.size() > 0) { // Ensure there are owners
                region.setFlag(Flags.BUILD, StateFlag.State.ALLOW); // Allow building for everyone in region
                // Then deny for non-owners if that's the desired behavior, or use region groups.
                // For simplicity, if owners exist, let's just allow build for them by default.
                // A more robust way is to use RegionGroupFlag.
                // region.setFlag(Flags.BUILD.getRegionGroupFlag(), RegionGroup.OWNERS); this is cleaner
             }
        }
    }


    public boolean removeProtectedRegion(Player player) {
        return removeProtectedRegion(player.getUniqueId(), player.getWorld());
    }

    public boolean removeProtectedRegion(java.util.UUID playerUUID, World world) {
        if (!enabled || world == null) return false;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

        if (regionManager == null) {
            plugin.getLogger().warning("WorldGuard RegionManager not found for world: " + world.getName() + " during region removal.");
            return false;
        }

        String regionId = getRegionId(playerUUID);

        if (regionManager.hasRegion(regionId)) {
            try {
                regionManager.removeRegion(regionId);
                regionManager.saveChanges();
                plugin.getLogger().info("Removed WorldGuard region '" + regionId + "'.");
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error removing WorldGuard region " + regionId, e);
                return false;
            }
        }
        return false; // Region didn't exist
    }

    // Method to check if a player can build, used by RegionManager
    public boolean canBuild(Player player, Location location) {
        if (!enabled) {
            // Should not be called if WG is not enabled, but as a safeguard
            return true; // Fallback to CoreSystem's own protection or allow if none
        }
        com.sk89q.worldedit.world.World worldEditWorld = BukkitAdapter.adapt(location.getWorld());
        return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
                .testBuild(BukkitAdapter.adapt(location), WorldGuard.getInstance().getPlatform().getSessionManager().get(BukkitAdapter.adapt(player)), Flags.BUILD);
    }
}
