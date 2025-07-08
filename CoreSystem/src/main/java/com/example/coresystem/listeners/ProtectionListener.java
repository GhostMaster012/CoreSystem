package com.example.coresystem.listeners;

import com.example.coresystem.CoreSystem;
import com.example.coresystem.entity.CoreEntityManager;
import com.example.coresystem.protection.ProtectedRegion;
import com.example.coresystem.protection.RegionManager;
import com.example.coresystem.utils.ChatUtils;
import org.bukkit.Location;
// import org.bukkit.Material; // Not needed for core block check anymore
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ProtectionListener implements Listener {

    private final CoreSystem plugin;
    private final RegionManager regionManager;
    private final CoreEntityManager coreEntityManager;
    private final NamespacedKey coreEntityKey;


    public ProtectionListener(CoreSystem plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.coreEntityManager = plugin.getCoreEntityManager();
        this.coreEntityKey = new NamespacedKey(plugin, "core_entity_owner_uuid"); // Same key as in CoreEntityManager
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        ProtectedRegion region = regionManager.getRegionAt(location);

        if (region != null) {
            // If the broken block IS the core's base location (where ArmorStand stands)
            // This is a more critical check.
            if (region.getCoreLocation().getBlockX() == location.getBlockX() &&
                region.getCoreLocation().getBlockY() == location.getBlockY() &&
                region.getCoreLocation().getBlockZ() == location.getBlockZ()) {

                ChatUtils.sendErrorMessage(player, "You cannot break the block your Core is on! Manage your core via commands.");
                event.setCancelled(true);
                return;
            }

            if (region.isOwner(player)) {
                return; // Owner can modify their region (except the core's base block)
            }

            ChatUtils.sendErrorMessage(player, "You cannot break blocks in this protected Core region.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Location location = block.getLocation();

        ProtectedRegion region = regionManager.getRegionAt(location);

        if (region != null) {
            if (region.isOwner(player)) {
                 // Prevent placing blocks that would suffocate/obstruct the ArmorStand directly
                if (location.equals(region.getCoreLocation()) ||
                    location.equals(region.getCoreLocation().clone().add(0,1,0))) { // Check block above core base
                     // More precise checking might be needed if armor stand is small and not exactly at 0,0,0 of its block
                    ChatUtils.sendErrorMessage(player, "You cannot place a block directly on/in your Core entity.");
                    event.setCancelled(true);
                    return;
                }
                return;
            }
            ChatUtils.sendErrorMessage(player, "You cannot place blocks in this protected Core region.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> blocksToRemove = event.blockList();
        blocksToRemove.removeIf(block -> {
            ProtectedRegion region = regionManager.getRegionAt(block.getLocation());
            if (region != null) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Explosion prevented block destruction at " + block.getLocation() + " in region of " + region.getOwnerUUID());
                }
                return true;
            }
            return false;
        });
    }

    // Protect ArmorStand Core from manipulation
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand armorStand = event.getRightClicked();
        if (armorStand.getPersistentDataContainer().has(coreEntityKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
            // Optionally send a message to the player
            // Player player = event.getPlayer();
            // ChatUtils.sendErrorMessage(player, "You cannot interact with this Core entity.");
        }
    }

    // Protect ArmorStand Core from damage
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand) {
            ArmorStand armorStand = (ArmorStand) event.getEntity();
            if (armorStand.getPersistentDataContainer().has(coreEntityKey, PersistentDataType.STRING)) {
                event.setCancelled(true);
            }
        }
    }
}
