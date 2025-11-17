package com.drakkar.itemstatstracker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AcessorioGUI implements InventoryHolder {

    private final Inventory inventory;
    private final Player player;
    private final Map<Integer, String> slotTypes = new HashMap<>(); // Slot index -> Slot type

    public AcessorioGUI(Player player) {
        this.player = player;
        
        FileConfiguration config = ItemStatsTracker.getInstance().getAcessoriosConfig();
        if (config == null) {
            this.inventory = Bukkit.createInventory(this, 27, "Seus Acessórios");
            return;
        }

        ConfigurationSection guiSection = config.getConfigurationSection("gui");
        if (guiSection == null) {
            this.inventory = Bukkit.createInventory(this, 27, "Seus Acessórios");
            return;
        }

        String title = guiSection.getString("title", "Seus Acessórios");
        int rows = guiSection.getInt("rows", 3);
        int size = rows * 9;

        this.inventory = Bukkit.createInventory(this, size, title);

        // Configurar slots e placeholders
        ConfigurationSection slotsSection = guiSection.getConfigurationSection("slots");
        if (slotsSection != null) {
            for (String slotKey : slotsSection.getKeys(false)) {
                try {
                    int slotIndex = Integer.parseInt(slotKey);
                    ConfigurationSection slotConfig = slotsSection.getConfigurationSection(slotKey);
                    if (slotConfig == null) {
                        continue;
                    }

                    String slotType = slotConfig.getString("type");
                    if (slotType == null || slotType.isEmpty()) {
                        continue;
                    }

                    slotTypes.put(slotIndex, slotType);

                    // Criar item placeholder
                    String placeholderMaterial = slotConfig.getString("placeholder-item", "GRAY_STAINED_GLASS_PANE");
                    Material material;
                    try {
                        material = Material.valueOf(placeholderMaterial.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        material = Material.GRAY_STAINED_GLASS_PANE;
                    }

                    ItemStack placeholder = new ItemStack(material);
                    ItemMeta meta = placeholder.getItemMeta();
                    if (meta != null) {
                        String placeholderName = slotConfig.getString("placeholder-name", "§7Slot de " + slotType);
                        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', placeholderName));
                        
                        List<String> lore = slotConfig.getStringList("lore");
                        if (!lore.isEmpty()) {
                            lore.replaceAll(line -> org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                            meta.setLore(lore);
                        }
                        placeholder.setItemMeta(meta);
                    }

                    // Verificar se há acessório equipado neste slot
                    ItemStack acessorioEquipado = AcessorioManager.getAcessorioEquipado(player, slotType);
                    if (acessorioEquipado != null && acessorioEquipado.getType() != Material.AIR) {
                        // Colocar o acessório equipado
                        inventory.setItem(slotIndex, acessorioEquipado);
                    } else {
                        // Colocar o placeholder
                        inventory.setItem(slotIndex, placeholder);
                    }
                } catch (NumberFormatException e) {
                    // Slot key inválido, pular
                    continue;
                }
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Obtém o tipo de slot em um índice específico.
     * 
     * @param slotIndex O índice do slot
     * @return O tipo do slot, ou null se não for um slot de acessório
     */
    public String getSlotType(int slotIndex) {
        return slotTypes.get(slotIndex);
    }

    /**
     * Atualiza o inventário com os acessórios equipados.
     */
    public void updateInventory() {
        FileConfiguration config = ItemStatsTracker.getInstance().getAcessoriosConfig();
        if (config == null) {
            return;
        }

        ConfigurationSection guiSection = config.getConfigurationSection("gui");
        if (guiSection == null) {
            return;
        }

        ConfigurationSection slotsSection = guiSection.getConfigurationSection("slots");
        if (slotsSection == null) {
            return;
        }

        for (Map.Entry<Integer, String> entry : slotTypes.entrySet()) {
            int slotIndex = entry.getKey();
            String slotType = entry.getValue();

            ConfigurationSection slotConfig = slotsSection.getConfigurationSection(String.valueOf(slotIndex));
            if (slotConfig == null) {
                continue;
            }

            ItemStack acessorioEquipado = AcessorioManager.getAcessorioEquipado(player, slotType);
            if (acessorioEquipado != null && acessorioEquipado.getType() != Material.AIR) {
                // Colocar o acessório equipado
                inventory.setItem(slotIndex, acessorioEquipado);
            } else {
                // Criar item placeholder
                String placeholderMaterial = slotConfig.getString("placeholder-item", "GRAY_STAINED_GLASS_PANE");
                Material material;
                try {
                    material = Material.valueOf(placeholderMaterial.toUpperCase());
                } catch (IllegalArgumentException e) {
                    material = Material.GRAY_STAINED_GLASS_PANE;
                }

                ItemStack placeholder = new ItemStack(material);
                ItemMeta meta = placeholder.getItemMeta();
                if (meta != null) {
                    String placeholderName = slotConfig.getString("placeholder-name", "§7Slot de " + slotType);
                    meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', placeholderName));
                    
                    List<String> lore = slotConfig.getStringList("lore");
                    if (!lore.isEmpty()) {
                        lore.replaceAll(line -> org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                        meta.setLore(lore);
                    }
                    placeholder.setItemMeta(meta);
                }

                inventory.setItem(slotIndex, placeholder);
            }
        }
    }

    /**
     * Obtém o jogador dono desta GUI.
     * 
     * @return O jogador
     */
    public Player getPlayer() {
        return player;
    }
}

