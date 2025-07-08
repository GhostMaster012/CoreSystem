package com.example.coresystem.listeners;

import com.example.coresystem.CoreManager;
import com.example.coresystem.CoreSystem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class ExperienceListener implements Listener {

    private final CoreSystem plugin;
    private final CoreManager coreManager;

    public ExperienceListener(CoreSystem plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity killed = event.getEntity();
        Player killer = killed.getKiller(); // This gets the player who dealt the last hit

        if (killer == null) {
            return; // Mob died by other means (fall, lava, etc.) or was not killed by a player
        }

        // Player killed another Player
        if (killed.getType() == EntityType.PLAYER) {
            // Ensure it's not the same player (e.g. suicide by commands or weird scenarios)
            if (killer.getUniqueId().equals(killed.getUniqueId())) {
                return;
            }
            int xpAmount = plugin.getConfigManager().getPlayerKillXP();
            if (xpAmount > 0) {
                coreManager.addCoreXP(killer, xpAmount, "PLAYER_KILL");
            }
        }
        // Player killed a Mob
        else {
            // Check if the entity type is one of the "monster" types or any other type you want to give XP for
            // For simplicity, we are giving XP for any mob type listed in experience.yml or the default value.
            // You might want to add more checks here if certain entities should not grant XP (e.g., ArmorStands, Paintings)
            // EntityType.isAlive() or EntityType.isSpawnable() could be useful checks if needed.
            // For now, we rely on experience.yml defining relevant mobs or a DEFAULT.

            EntityType killedType = killed.getType();
            int xpAmount = plugin.getConfigManager().getXpForMobKill(killedType);

            if (xpAmount > 0) {
                coreManager.addCoreXP(killer, xpAmount, "MOB_KILL:" + killedType.name());
            }
        }
    }
}
