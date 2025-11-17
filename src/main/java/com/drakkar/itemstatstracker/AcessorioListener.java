package com.drakkar.itemstatstracker;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.minimessage.MiniMessage;

public final class AcessorioListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Verificar se é a GUI de acessórios
        if (!(event.getInventory().getHolder() instanceof AcessorioGUI)) {
            return;
        }

        AcessorioGUI gui = (AcessorioGUI) event.getInventory().getHolder();

        // Verificar se o jogador é o dono da GUI
        if (!gui.getPlayer().equals(player)) {
            return;
        }

        // Cancelar o evento padrão
        event.setCancelled(true);

        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Verificar se é um slot de acessório
        String slotType = gui.getSlotType(slot);
        if (slotType == null || slotType.isEmpty()) {
            // Não é um slot de acessório, permitir apenas se não estiver tentando colocar algo
            return;
        }

        // Se está clicando com item no cursor para equipar
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            // Verificar se o item é válido para aquele slot
            if (AcessorioManager.isAcessorioValido(cursorItem, slotType)) {
                // Equipar o acessório
                boolean sucesso = AcessorioManager.equiparAcessorio(player, cursorItem, slotType);
                if (sucesso) {
                    // Remover 1 item do cursor
                    if (cursorItem.getAmount() > 1) {
                        cursorItem.setAmount(cursorItem.getAmount() - 1);
                    } else {
                        cursorItem.setType(Material.AIR);
                    }
                    player.setItemOnCursor(cursorItem);

                    // Tocar som
                    AcessorioManager.tocarSomEquipar(player);

                    // Atualizar a GUI
                    gui.updateInventory();

                    // Mensagem de sucesso
                    player.sendMessage(MiniMessage.miniMessage()
                        .deserialize("<green>Acessório equipado com sucesso!</green>"));
                } else {
                    player.sendMessage(MiniMessage.miniMessage()
                        .deserialize("<red>Este item não pode ser equipado neste slot.</red>"));
                }
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<red>Este item não é válido para o slot de " + slotType + ".</red>"));
            }
            return;
        }

        // Se está clicando para desequipar
        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            // Verificar se há um acessório equipado neste slot
            ItemStack acessorioEquipado = AcessorioManager.getAcessorioEquipado(player, slotType);
            if (acessorioEquipado != null && acessorioEquipado.getType() != Material.AIR) {
                // Desequipar o acessório
                ItemStack acessorioRemovido = AcessorioManager.desequiparAcessorio(player, slotType);
                if (acessorioRemovido != null) {
                    // Colocar o acessório no cursor do jogador
                    player.setItemOnCursor(acessorioRemovido);

                    // Tocar som
                    AcessorioManager.tocarSomDesequipar(player);

                    // Atualizar a GUI
                    gui.updateInventory();

                    // Mensagem de sucesso
                    player.sendMessage(MiniMessage.miniMessage()
                        .deserialize("<green>Acessório desequipado com sucesso!</green>"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // Verificar se é a GUI de acessórios
        if (!(event.getInventory().getHolder() instanceof AcessorioGUI)) {
            return;
        }

        // Se o jogador tinha um item no cursor, colocá-lo no inventário
        ItemStack cursorItem = player.getItemOnCursor();
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            // Tentar adicionar ao inventário
            java.util.HashMap<Integer, ItemStack> excess = player.getInventory().addItem(cursorItem);
            if (excess.isEmpty()) {
                // Item adicionado com sucesso, limpar cursor
                player.setItemOnCursor(null);
            } else {
                // Inventário cheio, manter no cursor
                player.setItemOnCursor(excess.get(0));
            }
        }
    }
}

