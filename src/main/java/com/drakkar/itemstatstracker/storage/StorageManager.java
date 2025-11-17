package com.drakkar.itemstatstracker.storage;

import com.drakkar.itemstatstracker.ItemStatsTracker;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Gerenciador de armazenamento para dados de itens.
 * Atualmente usa PersistentDataContainer (PDC) do Bukkit/Paper, que salva dados no NBT dos itens.
 * 
 * Estrutura preparada para suporte futuro a MySQL:
 * - Os dados ficam salvos diretamente nos itens via PDC (NBT)
 * - Não há banco de dados separado no momento
 * - Para MySQL futuro, seria necessário criar uma interface StorageAdapter
 *   e implementar persistência externa além do NBT
 */
public class StorageManager {
    
    private static StorageManager instance;
    private StorageType storageType;
    
    private StorageManager() {
        FileConfiguration config = ItemStatsTracker.getInstance().getConfig();
        // Por enquanto, sempre usa PDC (dados salvos no NBT dos itens)
        String storageTypeStr = config != null ? config.getString("storage.type", "PDC") : "PDC";
        try {
            this.storageType = StorageType.valueOf(storageTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.storageType = StorageType.PDC;
        }
        
        ItemStatsTracker.getInstance().getLogger().info("Sistema de armazenamento: " + this.storageType);
        ItemStatsTracker.getInstance().getLogger().info("Dados salvos em: NBT dos itens (PersistentDataContainer)");
    }
    
    public static StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }
    
    public StorageType getStorageType() {
        return storageType;
    }
    
    /**
     * Obtém informações sobre onde os dados estão sendo salvos.
     * 
     * @return String descritiva do local de armazenamento
     */
    public String getStorageLocation() {
        switch (storageType) {
            case PDC:
                return "Dados salvos nos próprios itens via PersistentDataContainer (NBT)";
            case MYSQL:
                // Implementação futura
                return "Dados salvos em MySQL (não implementado ainda)";
            default:
                return "Desconhecido";
        }
    }
    
    /**
     * Tipos de armazenamento suportados.
     */
    public enum StorageType {
        /**
         * PersistentDataContainer - dados salvos no NBT dos itens (atual).
         * Os dados ficam diretamente nos itens, não há banco de dados externo.
         */
        PDC,
        
        /**
         * MySQL - dados salvos em banco de dados (implementação futura).
         * Quando implementado, os dados dos itens serão salvos no MySQL
         * além de ou em vez do NBT.
         */
        MYSQL
    }
}

