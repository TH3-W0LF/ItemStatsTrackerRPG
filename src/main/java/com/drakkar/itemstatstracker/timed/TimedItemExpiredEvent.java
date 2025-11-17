package com.drakkar.itemstatstracker.timed;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Evento disparado quando um item temporizado expira
 */
public class TimedItemExpiredEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final ItemStack item;
    private final InventoryHolder holder;
    
    public TimedItemExpiredEvent(ItemStack item, InventoryHolder holder) {
        this.item = item;
        this.holder = holder;
    }
    
    /**
     * Retorna o item que expirou
     */
    public ItemStack getItem() {
        return item;
    }
    
    /**
     * Retorna o holder do inventário onde o item estava (pode ser null para itens droppados)
     */
    public InventoryHolder getHolder() {
        return holder;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

