package com.drakkar.itemstatstracker.timed;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TimedItemVisualUpdater implements Listener {

    private final TimedItemManager manager;

    public TimedItemVisualUpdater(TimedItemManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        updatePlayerItems(e.getPlayer());
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        updatePlayerItems(e.getPlayer());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p) {
            updatePlayerItems(p);
        }
    }

    private void updatePlayerItems(Player p) {
        ItemStack[] contents = p.getInventory().getContents();

        for (ItemStack stack : contents) {
            if (stack == null) continue;

            if (!manager.isTimedItem(stack)) continue;

            long remaining = manager.getRemainingTime(stack);
            if (remaining <= 0) continue;

            updateLore(stack, remaining);
        }
    }

    private void updateLore(ItemStack stack, long remaining) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        // Atualizar lore usando o método do manager
        long expireAt = System.currentTimeMillis() + (remaining * 1000L);
        lore = manager.updateOrAddTimeLore(lore, expireAt);

        meta.lore(lore);
        stack.setItemMeta(meta);
    }
}

