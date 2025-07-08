package com.example.coresystem.tutorial;

import com.example.coresystem.CoreSystem;
import com.example.coresystem.PlayerData;
import com.example.coresystem.utils.MessageManager; // Import MessageManager
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

// import java.util.Arrays; // Not needed if messages are from messages.yml
// import java.util.List; // Not needed if messages are from messages.yml

public class TutorialManager {

    private final CoreSystem plugin;
    private final MessageManager mm;

    // Messages will be loaded from messages.yml via MessageManager
    // private final List<String> tutorialMessages = Arrays.asList(...);

    public TutorialManager(CoreSystem plugin) {
        this.plugin = plugin;
        this.mm = plugin.getMessageManager();
    }

    public void startTutorialSequence(Player player) {
        PlayerData playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (playerData.isTutorialCompleted() && !plugin.getConfigManager().isDebugMode()) {
            return;
        }
        if (playerData.isCoreActive() && !plugin.getConfigManager().isDebugMode()){
            return;
        }

        // Get tutorial messages from messages.yml
        // This assumes a structure like:
        // tutorial:
        //   - "message1"
        //   - "message2"
        // Or, individual keys: tutorial.step1, tutorial.step2 etc.
        // For simplicity, let's use a list from a single key if possible, or multiple keys.
        // We'll use individual keys for more flexibility with placeholders if needed later.

        final String[] messageKeys = {
            "tutorial.welcome", "tutorial.what-is-core", "tutorial.claim-seed",
            "tutorial.place-core", "tutorial.evolution", "tutorial.feed-core",
            "tutorial.kill-for-xp", "tutorial.gui-info", "tutorial.good-luck"
        };

        new BukkitRunnable() {
            int messageIndex = 0;
            long delay = 60L; // Initial delay: 3 seconds

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                if (messageIndex < messageKeys.length) {
                    mm.sendMessage(player, messageKeys[messageIndex]); // MessageManager handles prefix and color
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, (float) (1.0 + Math.random() * 0.2));
                    messageIndex++;
                    delay = 100L + (long)(Math.random() * 60L);

                    if (messageIndex < messageKeys.length) {
                        // Schedule next part of the task
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (!player.isOnline() || messageIndex >= messageKeys.length) {
                                    this.cancel();
                                    checkAndCompleteTutorial(player, playerData);
                                    return;
                                }
                                mm.sendMessage(player, messageKeys[messageIndex]);
                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, (float) (1.0 + Math.random() * 0.2));
                                messageIndex++;
                                if (messageIndex >= messageKeys.length) {
                                    checkAndCompleteTutorial(player, playerData);
                                }
                            }
                        }.runTaskLater(plugin, delay);
                        // Cancel this outer task as the next one is scheduled
                        this.cancel();
                    } else {
                        checkAndCompleteTutorial(player, playerData);
                        this.cancel();
                    }
                } else {
                     checkAndCompleteTutorial(player, playerData);
                    this.cancel();
                }
            }
        }.runTaskLater(plugin, delay);
    }

    private void checkAndCompleteTutorial(Player player, PlayerData playerData){
        if(!playerData.isTutorialCompleted()){
            playerData.setTutorialCompleted(true);
            plugin.getDataManager().savePlayerData(playerData);
            if(plugin.getConfigManager().isDebugMode()) {
                // This message should also come from messages.yml if we are thorough
                mm.sendMessage(player, "tutorial.debug-complete");
            }
        }
    }
}
