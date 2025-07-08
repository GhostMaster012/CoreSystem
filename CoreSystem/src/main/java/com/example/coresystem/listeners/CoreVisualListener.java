package com.example.coresystem.listeners;

import com.example.coresystem.CoreSystem;
import com.example.coresystem.entity.CoreEntityManager;
import com.example.coresystem.events.CoreLevelUpEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CoreVisualListener implements Listener {

    private final CoreSystem plugin;
    private final CoreEntityManager coreEntityManager;

    public CoreVisualListener(CoreSystem plugin) {
        this.plugin = plugin;
        this.coreEntityManager = plugin.getCoreEntityManager();
    }

    @EventHandler
    public void onCoreLevelUp(CoreLevelUpEvent event) {
        Player player = event.getPlayer();
        int newLevel = event.getNewLevel();

        // Ensure the player is online, though they should be if they leveled up normally
        if (player != null && player.isOnline()) {
            coreEntityManager.updateCoreAppearance(player.getUniqueId(), newLevel);
        } else {
            // If player is somehow offline, the CoreEntityManager's loadExistingCoreEntities
            // or the PlayerJoinEvent logic should eventually update the appearance
            // or respawn the entity with the correct model.
            if(plugin.getConfigManager().isDebugMode()){
                plugin.getLogger().info("CoreLevelUpEvent for offline player " + event.getPlayer().getUniqueId() + " - appearance will update on next load/join.");
            }
        }
    }
}
