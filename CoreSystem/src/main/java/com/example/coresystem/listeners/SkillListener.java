package com.example.coresystem.listeners;

import com.example.coresystem.CoreSystem;
import com.example.coresystem.events.CoreLevelUpEvent;
import com.example.coresystem.skill.SkillManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SkillListener implements Listener {

    private final CoreSystem plugin;
    private final SkillManager skillManager;

    public SkillListener(CoreSystem plugin) {
        this.plugin = plugin;
        this.skillManager = plugin.getSkillManager();
    }

    @EventHandler
    public void onCoreLevelUp(CoreLevelUpEvent event) {
        Player player = event.getPlayer();
        int newLevel = event.getNewLevel();
        skillManager.unlockSkillsForLevel(player, newLevel);
    }
}
