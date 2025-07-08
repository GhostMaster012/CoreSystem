package com.example.coresystem.listeners;

import com.example.coresystem.CoreSystem;
import com.example.coresystem.DataManager;
// import com.example.coresystem.PlayerData; // Not directly used here anymore for this logic
import com.example.coresystem.tutorial.TutorialManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final CoreSystem plugin;
    private final DataManager dataManager;
    private final TutorialManager tutorialManager;

    public PlayerConnectionListener(CoreSystem plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.tutorialManager = plugin.getTutorialManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        dataManager.loadAndCachePlayerData(player); // This also applies mutations

        // Start tutorial sequence if conditions are met (checked within TutorialManager)
        tutorialManager.startTutorialSequence(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dataManager.saveAndRemovePlayerData(player);
    }
}
