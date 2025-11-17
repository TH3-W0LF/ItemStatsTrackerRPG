package com.drakkar.itemstatstracker.timed;

import com.drakkar.itemstatstracker.ItemStatsTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener para eventos relacionados a itens temporizados
 */
public class TimedItemListener implements Listener {
    
    private final TimedItemManager manager;
    
    public TimedItemListener(ItemStatsTracker plugin, TimedItemManager manager) {
        this.manager = manager;
    }
    
    /**
     * Quando um jogador abre um container, atualiza a lore imediatamente
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        org.bukkit.inventory.Inventory inv = event.getInventory();
        org.bukkit.inventory.InventoryHolder holder = inv.getHolder();
        
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() != org.bukkit.Material.AIR) {
                try {
                    manager.checkAndExpireItemStack(it, holder, i);
                } catch (Exception e) {
                    // Ignorar erros
                }
            }
        }
    }
    
    /**
     * Quando um jogador pega um item do chão
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        org.bukkit.entity.Item itemEntity = event.getItem();
        // Verificar antes de adicionar ao inventário
        manager.checkAndExpireEntityItem(itemEntity);
    }
    
    /**
     * Quando um jogador interage com um item, atualiza a lore
     * (opcional - ajuda a manter a lore atualizada)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item != null && manager.isTimedItem(item)) {
            // Forçar atualização da lore ao interagir
            // O ExpirationTask já cuida disso, mas isso garante atualização imediata
            try {
                long expireAt = item.getItemMeta().getPersistentDataContainer()
                        .get(manager.getKeyExpireAt(), org.bukkit.persistence.PersistentDataType.LONG);
                
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    java.util.List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? meta.lore() : new java.util.ArrayList<>();
                    lore = manager.updateOrAddTimeLore(lore, expireAt);
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
            } catch (Exception e) {
                // Ignorar erros
            }
        }
    }
}

