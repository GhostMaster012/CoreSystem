package com.example.coresystem.events;

import com.example.coresystem.mutation.Mutation;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CoreRebirthEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final int previousRebirthCount;
    private final int newRebirthCount;
    private final List<Mutation> mutationsAwarded; // Could be one or more if design changes

    public CoreRebirthEvent(Player player, int previousRebirthCount, int newRebirthCount, List<Mutation> mutationsAwarded) {
        this.player = player;
        this.previousRebirthCount = previousRebirthCount;
        this.newRebirthCount = newRebirthCount;
        this.mutationsAwarded = mutationsAwarded;
    }

    public Player getPlayer() {
        return player;
    }

    public int getPreviousRebirthCount() {
        return previousRebirthCount;
    }

    public int getNewRebirthCount() {
        return newRebirthCount;
    }

    public List<Mutation> getMutationsAwarded() {
        return mutationsAwarded;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
