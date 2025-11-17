package com.drakkar.itemstatstracker;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Base64;
import java.util.Locale;

public final class AcessorioManager {

    private static final String ACESSORIO_PREFIX = "acessorio_";

    private AcessorioManager() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Verifica se um item é um acessório válido para um tipo de slot específico.
     * 
     * @param item O item a ser verificado
     * @param slotType O tipo de slot (ex: "ANEL", "COLAR", "ASA")
     * @return true se o item é válido para aquele slot, false caso contrário
     */
    public static boolean isAcessorioValido(ItemStack item, String slotType) {
        if (item == null || item.getType() == Material.AIR || slotType == null || slotType.isEmpty()) {
            return false;
        }

        FileConfiguration acessoriosConfig = ItemStatsTracker.getInstance().getAcessoriosConfig();
        if (acessoriosConfig == null) {
            return false;
        }

        ConfigurationSection itemTypesSection = acessoriosConfig.getConfigurationSection("item-types");
        if (itemTypesSection == null) {
            return false;
        }

        // Obter lista de IDs válidos para este tipo de slot
        java.util.List<String> validIds = itemTypesSection.getStringList(slotType);

        // Se não há IDs configurados, retornar false
        if (validIds == null || validIds.isEmpty()) {
            return false;
        }

        // Verificar se o item possui alguma das tags NBT/PDC válidas
        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (String itemId : validIds) {
            if (itemId == null || itemId.isEmpty()) {
                continue;
            }

            // Parsear o ID do item (formato: "namespace:key")
            String[] parts = itemId.split(":", 2);
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
                    return true; // Item é um acessório válido para este slot
                }
            } catch (IllegalArgumentException e) {
                continue;
            }
        }

        return false;
    }

    /**
     * Equipa um acessório em um jogador.
     * 
     * @param player O jogador
     * @param item O item acessório a ser equipado
     * @param slotType O tipo de slot (ex: "ANEL", "COLAR", "ASA")
     * @return true se o acessório foi equipado com sucesso, false caso contrário
     */
    public static boolean equiparAcessorio(Player player, ItemStack item, String slotType) {
        if (player == null || item == null || item.getType() == Material.AIR || slotType == null || slotType.isEmpty()) {
            return false;
        }

        // Verificar se o item é válido para aquele slot
        if (!isAcessorioValido(item, slotType)) {
            return false;
        }

        // Serializar o item
        String serialized = serializeItemStack(item);
        if (serialized == null || serialized.isEmpty()) {
            return false;
        }

        // Salvar no PDC do jogador
        NamespacedKey key = new NamespacedKey(ItemStatsTracker.getInstance(), ACESSORIO_PREFIX + slotType.toUpperCase(Locale.ROOT));
        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, serialized);

        // Atualizar stats do jogador
        StatManager.atualizarStats(player);

        return true;
    }

    /**
     * Desequipa um acessório de um jogador.
     * 
     * @param player O jogador
     * @param slotType O tipo de slot (ex: "ANEL", "COLAR", "ASA")
     * @return O ItemStack do acessório desequipado, ou null se não havia acessório equipado
     */
    public static ItemStack desequiparAcessorio(Player player, String slotType) {
        if (player == null || slotType == null || slotType.isEmpty()) {
            return null;
        }

        NamespacedKey key = new NamespacedKey(ItemStatsTracker.getInstance(), ACESSORIO_PREFIX + slotType.toUpperCase(Locale.ROOT));
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        String serialized = pdc.get(key, PersistentDataType.STRING);
        if (serialized == null || serialized.isEmpty()) {
            return null;
        }

        // Desserializar o item
        ItemStack item = deserializeItemStack(serialized);
        if (item == null) {
            return null;
        }

        // Remover do PDC do jogador
        pdc.remove(key);

        // Atualizar stats do jogador
        StatManager.atualizarStats(player);

        return item;
    }

    /**
     * Obtém o acessório equipado em um slot específico.
     * 
     * @param player O jogador
     * @param slotType O tipo de slot (ex: "ANEL", "COLAR", "ASA")
     * @return O ItemStack do acessório equipado, ou null se não houver acessório equipado
     */
    public static ItemStack getAcessorioEquipado(Player player, String slotType) {
        if (player == null || slotType == null || slotType.isEmpty()) {
            return null;
        }

        NamespacedKey key = new NamespacedKey(ItemStatsTracker.getInstance(), ACESSORIO_PREFIX + slotType.toUpperCase(Locale.ROOT));
        PersistentDataContainer pdc = player.getPersistentDataContainer();

        String serialized = pdc.get(key, PersistentDataType.STRING);
        if (serialized == null || serialized.isEmpty()) {
            return null;
        }

        return deserializeItemStack(serialized);
    }

    /**
     * Serializa um ItemStack para String (Base64).
     * Usa o método serializeAsBytes() do Paper/Bukkit.
     * 
     * @param item O ItemStack a ser serializado
     * @return A string serializada, ou null se houver erro
     */
    private static String serializeItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        try {
            byte[] itemData = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(itemData);
        } catch (Exception e) {
            ItemStatsTracker.getInstance().getLogger().warning("Erro ao serializar ItemStack: " + e.getMessage());
            return null;
        }
    }

    /**
     * Desserializa um ItemStack de String (Base64).
     * Usa o método ItemStack.deserializeBytes() do Paper/Bukkit.
     * 
     * @param serialized A string serializada
     * @return O ItemStack desserializado, ou null se houver erro
     */
    private static ItemStack deserializeItemStack(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return null;
        }

        try {
            byte[] itemData = Base64.getDecoder().decode(serialized);
            return ItemStack.deserializeBytes(itemData);
        } catch (Exception e) {
            ItemStatsTracker.getInstance().getLogger().warning("Erro ao desserializar ItemStack: " + e.getMessage());
            return null;
        }
    }

    /**
     * Toca o som de equipar acessório.
     * 
     * @param player O jogador
     */
    public static void tocarSomEquipar(Player player) {
        if (player != null && player.isOnline()) {
            FileConfiguration config = ItemStatsTracker.getInstance().getAcessoriosConfig();
            if (config != null) {
                String soundName = config.getString("settings.equip-sound", "BLOCK_ANVIL_USE");
                try {
                    org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
                }
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Toca o som de desequipar acessório.
     * 
     * @param player O jogador
     */
    public static void tocarSomDesequipar(Player player) {
        if (player != null && player.isOnline()) {
            FileConfiguration config = ItemStatsTracker.getInstance().getAcessoriosConfig();
            if (config != null) {
                String soundName = config.getString("settings.unequip-sound", "BLOCK_NOTE_BLOCK_BASS");
                try {
                    org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
                    player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException e) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                }
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            }
        }
    }
}

