package com.drakkar.itemstatstracker;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public final class GemaListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Verificar se está clicando em um inventário (não craft, anvil, etc.)
        if (event.getClickedInventory() == null) {
            return;
        }

        // Verificar se está clicando em um slot do inventário
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Se não há cursor item ou não é uma gema válida, ignorar
        if (cursorItem == null || cursorItem.getType() == Material.AIR) {
            return;
        }

        // Verificar se o item no cursor é uma gema válida
        if (!GemaManager.isGemaValida(cursorItem)) {
            return;
        }

        // Se não há item clicado ou não é um item rastreável, ignorar
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Verificar se o item clicado é rastreável
        if (!StatManager.itemDeveSerRastreado(clickedItem)) {
            return;
        }

        // Permitir apenas clicks de esquerda e shift+esquerda
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.SHIFT_LEFT) {
            return;
        }

        // Cancelar o evento padrão
        event.setCancelled(true);

        // Obter o ID da gema
        String gemaId = GemaManager.getGemaId(cursorItem);
        if (gemaId == null || gemaId.isEmpty()) {
            return;
        }

        // Verificar se o item tem slots de gema
        int totalSlots = GemaManager.getTotalSlots(clickedItem);
        if (totalSlots <= 0) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<red>Este item não possui slots de gema configurados.</red>"));
            return;
        }

        // Encontrar o primeiro slot vazio
        int slotVazio = GemaManager.encontrarSlotVazio(clickedItem);
        if (slotVazio < 0) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<red>Este item não possui slots de gema disponíveis.</red>"));
            return;
        }

        // Socar a gema
        boolean sucesso = GemaManager.socarGema(clickedItem, gemaId, slotVazio);
        if (!sucesso) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<red>Erro ao socar a gema. Tente novamente.</red>"));
            return;
        }

        // Remover 1 gema do cursor
        if (cursorItem.getAmount() > 1) {
            cursorItem.setAmount(cursorItem.getAmount() - 1);
        } else {
            cursorItem.setType(Material.AIR);
        }
        player.setItemOnCursor(cursorItem);

        // Atualizar a lore do item ANTES de atualizar no inventário
        LoreManager.updateLore(clickedItem);

        // Usar o sistema de ignore para evitar flickering
        final ItemStatsTracker plugin = ItemStatsTracker.getInstance();
        plugin.getIgnoreArmorChangeEvent().add(player.getUniqueId());

        // Atualizar o item no inventário
        event.setCurrentItem(clickedItem);

        // Remover do ignore list após um delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getIgnoreArmorChangeEvent().remove(player.getUniqueId());
            // Atualizar stats do jogador (caso o item esteja equipado)
            StatManager.atualizarStats(player);
        }, 1L);

        // Tocar som
        GemaManager.tocarSomSocar(player);

        // Mensagem de sucesso
        player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
            .deserialize("<green>Gema socada com sucesso no slot " + slotVazio + "!</green>"));
    }
}

