package com.example.coresystem.protection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ProtectedRegion {
    private final UUID ownerUUID;
    private final World world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final Location coreLocation; // Keep a reference to the exact core block

    public ProtectedRegion(UUID ownerUUID, Location coreLocation, int radius) {
        this.ownerUUID = ownerUUID;
        this.coreLocation = coreLocation.clone(); // Store a clone of the core's exact location
        this.world = coreLocation.getWorld();

        if (this.world == null) {
            throw new IllegalArgumentException("Core location must have a valid world.");
        }

        this.minX = coreLocation.getBlockX() - radius;
        this.minY = coreLocation.getBlockY() - radius; // Or 0 if you want full Y-axis protection from this point
        this.minZ = coreLocation.getBlockZ() - radius;
        this.maxX = coreLocation.getBlockX() + radius;
        this.maxY = coreLocation.getBlockY() + radius; // Or world max height
        this.maxZ = coreLocation.getBlockZ() + radius;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public World getWorld() {
        return world;
    }

    public Location getCoreLocation() {
        return coreLocation;
    }

    public boolean contains(Location location) {
        if (location == null || !this.world.equals(location.getWorld())) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    public boolean isOwner(Player player) {
        return player.getUniqueId().equals(ownerUUID);
    }

    // Optional: A method to check if a specific block is the core block itself
    public boolean isCoreBlock(Location location) {
        return coreLocation.getWorld().equals(location.getWorld()) &&
               coreLocation.getBlockX() == location.getBlockX() &&
               coreLocation.getBlockY() == location.getBlockY() &&
               coreLocation.getBlockZ() == location.getBlockZ();
    }

    @Override
    public String toString() {
        return "ProtectedRegion{" +
                "ownerUUID=" + ownerUUID +
                ", world=" + world.getName() +
                ", minX=" + minX +
                ", minY=" + minY +
                ", minZ=" + minZ +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                ", maxZ=" + maxZ +
                '}';
    }
}
