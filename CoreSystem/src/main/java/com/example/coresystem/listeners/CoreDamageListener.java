package com.example.coresystem.listeners;

import com.example.coresystem.CoreManager;
import com.example.coresystem.CoreSystem;
import com.example.coresystem.PlayerData;
import com.example.coresystem.entity.CoreEntityManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class CoreDamageListener implements Listener {

    private final CoreSystem plugin;
    private final CoreManager coreManager;
    private final CoreEntityManager coreEntityManager;
    private final NamespacedKey coreEntityKey;

    public CoreDamageListener(CoreSystem plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
        this.coreEntityManager = plugin.getCoreEntityManager();
        this.coreEntityKey = new NamespacedKey(plugin, "core_entity_owner_uuid");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCoreDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof ArmorStand)) {
            return;
        }

        ArmorStand armorStand = (ArmorStand) event.getEntity();
        String ownerUUIDString = armorStand.getPersistentDataContainer().get(coreEntityKey, PersistentDataType.STRING);

        if (ownerUUIDString == null) {
            return; // Not a core entity
        }

        // Always cancel direct damage to the ArmorStand; CoreManager will handle health reduction.
        event.setCancelled(true);

        UUID ownerUUID = UUID.fromString(ownerUUIDString);
        Player ownerOnline = plugin.getServer().getPlayer(ownerUUID);
        PlayerData coreOwnerData = plugin.getDataManager().getPlayerData(ownerUUID);

        if (coreOwnerData == null || !coreOwnerData.isCoreActive()) {
            return; // Core is not active or data missing
        }

        Player attacker = null;
        Entity damagerEntity = null;

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent byEntityEvent = (EntityDamageByEntityEvent) event;
            damagerEntity = byEntityEvent.getDamager();
            if (damagerEntity instanceof Player) {
                attacker = (Player) damagerEntity;
            } else if (damagerEntity instanceof Projectile) {
                Projectile projectile = (Projectile) damagerEntity;
                if (projectile.getShooter() instanceof Player) {
                    attacker = (Player) projectile.getShooter();
                }
            }
        }

        // If attacker is the owner, they cannot damage their own core this way
        if (attacker != null && attacker.getUniqueId().equals(ownerUUID)) {
            if (plugin.getConfigManager().isDebugMode()) {
                 // plugin.getLogger().info("Core owner " + attacker.getName() + " attempted to damage their own core. Denied.");
            }
            return;
        }

        if (coreManager.isCoreVulnerable(ownerUUID, attacker, damagerEntity, event.getCause())) {
            double damageAmount = event.getFinalDamage();
            // Let CoreManager handle the damage application, which includes PlayerData updates & destruction logic
            if (ownerOnline != null && ownerOnline.isOnline()) { // damageCore expects online player for messaging
                coreManager.damageCore(ownerOnline, damageAmount);
            } else {
                // Handle damage for offline player's core (no direct messaging to owner)
                coreManager.damageCoreOffline(ownerUUID, coreOwnerData, damageAmount);
            }
        } else {
            if (plugin.getConfigManager().isDebugMode() && attacker != null) {
                plugin.getLogger().info("Core of " + ownerUUID + " is not vulnerable to attack from " + attacker.getName() + " by " + event.getCause());
            }
        }
    }
}
