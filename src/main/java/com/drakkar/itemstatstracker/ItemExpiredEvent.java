package com.drakkar.itemstatstracker;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.UUID;

public class ItemExpiredEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final UUID itemId;

    public ItemExpiredEvent(UUID itemId) {
        this.itemId = itemId;
    }

    public UUID getItemId() {
        return itemId;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

