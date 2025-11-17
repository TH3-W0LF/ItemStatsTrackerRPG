package com.drakkar.itemstatstracker;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class GemaManager {

    private static final NamespacedKey GEMA_SLOTS_TOTAL_KEY = new NamespacedKey(ItemStatsTracker.getInstance(), "gema_slots_total");
    private static final String GEMA_SOCKET_PREFIX = "istats:gema_socket_";

    private GemaManager() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Verifica se um item é uma gema válida (definida em gemas.yml).
     * 
     * @param item O item a ser verificado
     * @return true se for uma gema válida, false caso contrário
     */
    public static boolean isGemaValida(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }

        FileConfiguration gemasConfig = ItemStatsTracker.getInstance().getGemasConfig();
        if (gemasConfig == null) {
            return false;
        }

        ConfigurationSection gemasSection = gemasConfig.getConfigurationSection("gemas");
        if (gemasSection == null) {
            return false;
        }

        // Verificar se o item tem alguma tag NBT/PDC que corresponde a uma gema configurada
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // Verificar todas as gemas configuradas
        for (String gemaId : gemasSection.getKeys(false)) {
            // Parsear o ID da gema (formato: "namespace:key")
            String[] parts = gemaId.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            try {
                NamespacedKey key = new NamespacedKey(parts[0], parts[1]);
                
                // Verificar se o PDC possui a chave (testando vários tipos comuns)
                if (pdc.has(key, PersistentDataType.STRING) ||
                    pdc.has(key, PersistentDataType.INTEGER) ||
                    pdc.has(key, PersistentDataType.DOUBLE) ||
                    pdc.has(key, PersistentDataType.BYTE) ||
                    pdc.has(key, PersistentDataType.BOOLEAN)) {
                    return true; // Item é uma gema válida
                }
            } catch (IllegalArgumentException e) {
                continue;
            }
        }

        // Método alternativo: verificar pelo nome do item ou material específico
        // Se necessário, pode ser expandido aqui

        return false;
    }

    /**
     * Obtém o ID da gema de um item.
     * 
     * @param item O item gema
     * @return O ID da gema ou null se não for uma gema válida
     */
    public static String getGemaId(ItemStack item) {
        if (!isGemaValida(item)) {
            return null;
        }

        FileConfiguration gemasConfig = ItemStatsTracker.getInstance().getGemasConfig();
        if (gemasConfig == null) {
            return null;
        }

        ConfigurationSection gemasSection = gemasConfig.getConfigurationSection("gemas");
        if (gemasSection == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Verificar todas as gemas configuradas
        for (String gemaId : gemasSection.getKeys(false)) {
            String[] parts = gemaId.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            try {
                NamespacedKey key = new NamespacedKey(parts[0], parts[1]);
                
                if (pdc.has(key, PersistentDataType.STRING) ||
                    pdc.has(key, PersistentDataType.INTEGER) ||
                    pdc.has(key, PersistentDataType.DOUBLE) ||
                    pdc.has(key, PersistentDataType.BYTE) ||
                    pdc.has(key, PersistentDataType.BOOLEAN)) {
                    return gemaId; // Retornar o ID da gema
                }
            } catch (IllegalArgumentException e) {
                continue;
            }
        }

        return null;
    }

    /**
     * Define o total de slots de gema de um item.
     * 
     * @param item O item
     * @param totalSlots O número total de slots (deve ser >= 0)
     * @return O item modificado
     */
    public static ItemStack setTotalSlots(ItemStack item, int totalSlots) {
        if (item == null || item.getType() == Material.AIR || totalSlots < 0) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        if (totalSlots == 0) {
            // Remover todos os slots
            pdc.remove(GEMA_SLOTS_TOTAL_KEY);
            // Remover todos os slots individuais
            for (int i = 1; i <= 10; i++) { // Limpar até 10 slots por segurança
                NamespacedKey slotKey = new NamespacedKey(ItemStatsTracker.getInstance(), GEMA_SOCKET_PREFIX + i);
                pdc.remove(slotKey);
            }
        } else {
            pdc.set(GEMA_SLOTS_TOTAL_KEY, PersistentDataType.INTEGER, totalSlots);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Obtém o total de slots de gema de um item.
     * 
     * @param item O item
     * @return O número total de slots, ou 0 se não houver slots configurados
     */
    public static int getTotalSlots(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(GEMA_SLOTS_TOTAL_KEY, PersistentDataType.INTEGER, 0);
    }

    /**
     * Soca uma gema em um item no slot especificado.
     * 
     * @param item O item onde a gema será socada
     * @param gemaId O ID da gema (ex: "itemsadder:gema_forca_t1")
     * @param slotIndex O índice do slot (1-based)
     * @return true se a gema foi socada com sucesso, false caso contrário
     */
    public static boolean socarGema(ItemStack item, String gemaId, int slotIndex) {
        if (item == null || item.getType() == Material.AIR || gemaId == null || gemaId.isEmpty()) {
            return false;
        }

        int totalSlots = getTotalSlots(item);
        if (slotIndex < 1 || slotIndex > totalSlots) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey slotKey = new NamespacedKey(ItemStatsTracker.getInstance(), GEMA_SOCKET_PREFIX + slotIndex);
        
        pdc.set(slotKey, PersistentDataType.STRING, gemaId);
        item.setItemMeta(meta);

        // Atualizar a lore do item para mostrar a gema
        LoreManager.updateLore(item);

        return true;
    }

    /**
     * Remove uma gema de um slot específico.
     * 
     * @param item O item
     * @param slotIndex O índice do slot (1-based)
     * @return O ID da gema removida, ou null se não havia gema no slot
     */
    public static String removerGema(ItemStack item, int slotIndex) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        int totalSlots = getTotalSlots(item);
        if (slotIndex < 1 || slotIndex > totalSlots) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey slotKey = new NamespacedKey(ItemStatsTracker.getInstance(), GEMA_SOCKET_PREFIX + slotIndex);
        
        String gemaId = pdc.get(slotKey, PersistentDataType.STRING);
        if (gemaId == null || gemaId.isEmpty() || "EMPTY".equals(gemaId)) {
            return null;
        }

        pdc.set(slotKey, PersistentDataType.STRING, "EMPTY");
        item.setItemMeta(meta);

        // Atualizar a lore do item
        LoreManager.updateLore(item);

        return gemaId;
    }

    /**
     * Obtém o ID da gema em um slot específico.
     * 
     * @param item O item
     * @param slotIndex O índice do slot (1-based)
     * @return O ID da gema ou "EMPTY" se o slot estiver vazio
     */
    public static String getGemaNoSlot(ItemStack item, int slotIndex) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return "EMPTY";
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return "EMPTY";
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey slotKey = new NamespacedKey(ItemStatsTracker.getInstance(), GEMA_SOCKET_PREFIX + slotIndex);
        
        String gemaId = pdc.get(slotKey, PersistentDataType.STRING);
        return (gemaId == null || gemaId.isEmpty()) ? "EMPTY" : gemaId;
    }

    /**
     * Encontra o primeiro slot vazio em um item.
     * 
     * @param item O item
     * @return O índice do primeiro slot vazio (1-based), ou -1 se não houver slots vazios
     */
    public static int encontrarSlotVazio(ItemStack item) {
        int totalSlots = getTotalSlots(item);
        if (totalSlots <= 0) {
            return -1;
        }

        for (int i = 1; i <= totalSlots; i++) {
            String gemaId = getGemaNoSlot(item, i);
            if ("EMPTY".equals(gemaId)) {
                return i;
            }
        }

        return -1; // Nenhum slot vazio
    }

    /**
     * Remove todas as gemas de um item.
     * 
     * @param item O item
     * @return O item modificado
     */
    public static ItemStack limparTodasGemas(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }

        int totalSlots = getTotalSlots(item);
        for (int i = 1; i <= totalSlots; i++) {
            removerGema(item, i);
        }

        return item;
    }

    /**
     * Obtém todas as gemas socadas em um item.
     * 
     * @param item O item
     * @return Lista de IDs de gemas socadas (não inclui "EMPTY")
     */
    public static List<String> getGemasSocadas(ItemStack item) {
        List<String> gemas = new ArrayList<>();
        
        if (item == null || item.getType() == Material.AIR) {
            return gemas;
        }

        int totalSlots = getTotalSlots(item);
        for (int i = 1; i <= totalSlots; i++) {
            String gemaId = getGemaNoSlot(item, i);
            if (gemaId != null && !gemaId.isEmpty() && !"EMPTY".equals(gemaId)) {
                gemas.add(gemaId);
            }
        }

        return gemas;
    }

    /**
     * Obtém os stats de uma gema do arquivo gemas.yml.
     * 
     * @param gemaId O ID da gema
     * @return Map com os stats da gema, ou null se a gema não for encontrada
     */
    public static ConfigurationSection getGemaStats(String gemaId) {
        FileConfiguration gemasConfig = ItemStatsTracker.getInstance().getGemasConfig();
        if (gemasConfig == null) {
            return null;
        }

        ConfigurationSection gemasSection = gemasConfig.getConfigurationSection("gemas");
        if (gemasSection == null) {
            return null;
        }

        ConfigurationSection gemaSection = gemasSection.getConfigurationSection(gemaId);
        if (gemaSection == null) {
            return null;
        }

        return gemaSection.getConfigurationSection("stats");
    }

    /**
     * Toca o som de socar gema.
     * 
     * @param player O jogador
     */
    public static void tocarSomSocar(org.bukkit.entity.Player player) {
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        }
    }
}

