package com.drakkar.itemstatstracker;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ItemStatsTracker extends JavaPlugin {

    private static ItemStatsTracker instance;
    private FileConfiguration levelEffectsConfig;
    private FileConfiguration enchantmentsConfig;
    private FileConfiguration reincarnadoConfig; // Novo
    private FileConfiguration itemMestreConfig;
    private FileConfiguration gemasConfig;
    private FileConfiguration acessoriosConfig;
    private final Set<UUID> ignoreArmorChangeEvent = new HashSet<>();
    
    // Sistema de itens temporizados
    private com.drakkar.itemstatstracker.timed.TimedItemManager timedItemManager;
    private com.drakkar.itemstatstracker.timed.TimedItemDBManager timedItemDBManager;
    private com.drakkar.itemstatstracker.timed.TimedItemExpirationTask timedItemExpirationTask;


    public static ItemStatsTracker getInstance() {
        return instance;
    }

    public FileConfiguration getLevelEffectsConfig() {
        return levelEffectsConfig;
    }

    public FileConfiguration getEnchantmentsConfig() {
        return this.enchantmentsConfig;
    }
    
    public FileConfiguration getReincarnadoConfig() { // Novo
        return this.reincarnadoConfig;
    }
    
    // Alias para compatibilidade
    @Deprecated
    public FileConfiguration getAscensionConfig() {
        return this.reincarnadoConfig;
    }

    public Set<UUID> getIgnoreArmorChangeEvent() {
        return ignoreArmorChangeEvent;
    }
    
    public FileConfiguration getItemMestreConfig() {
        return this.itemMestreConfig;
    }
    
    public FileConfiguration getGemasConfig() {
        return this.gemasConfig;
    }
    
    public FileConfiguration getAcessoriosConfig() {
        return this.acessoriosConfig;
    }

    private void loadReincarnadoConfig() { // Novo
        File reincarnadoFile = new File(getDataFolder(), "reincarnado.yml");
        if (!reincarnadoFile.exists()) {
            // Tenta carregar o arquivo antigo (ascension.yml) e renomear
            File ascensionFile = new File(getDataFolder(), "ascension.yml");
            if (ascensionFile.exists()) {
                ascensionFile.renameTo(reincarnadoFile);
            } else {
                saveResource("reincarnado.yml", false);
            }
        }
        this.reincarnadoConfig = YamlConfiguration.loadConfiguration(reincarnadoFile);
    }
    
    // Alias para compatibilidade
    @Deprecated
    private void loadAscensionConfig() {
        loadReincarnadoConfig();
    }

    private void loadItemMestreConfig() { // Novo
        File mestreFile = new File(getDataFolder(), "itemmestre.yml");
        if (!mestreFile.exists()) {
            saveResource("itemmestre.yml", false);
        }
        this.itemMestreConfig = YamlConfiguration.loadConfiguration(mestreFile);
    }

    @Override
    public void onEnable() {
        instance = this;

        // Salva e carrega os arquivos de configuração
        saveDefaultConfig();
        loadYamls();
        LanguageManager.loadMessages(this);

        // Inicializa o StatManager com a instância do plugin
        StatManager.init(this);
        StatListeners.loadEffectStatMapping(); // Carregar o mapeamento de efeitos para estatísticas
        
        // Inicializa o StorageManager para gerenciar armazenamento de dados
        com.drakkar.itemstatstracker.storage.StorageManager.getInstance();
        
        // Inicializa o sistema de itens temporizados
        if (getConfig().getBoolean("timed-items.enabled", true)) {
            try {
                timedItemDBManager = new com.drakkar.itemstatstracker.timed.TimedItemDBManager(this);
                timedItemDBManager.init();
                
                timedItemManager = new com.drakkar.itemstatstracker.timed.TimedItemManager(this, timedItemDBManager);
                
                timedItemExpirationTask = new com.drakkar.itemstatstracker.timed.TimedItemExpirationTask(this, timedItemManager);
                timedItemExpirationTask.start();
                
                getServer().getPluginManager().registerEvents(new com.drakkar.itemstatstracker.timed.TimedItemListener(this, timedItemManager), this);
                
                getLogger().info("Sistema de itens temporizados inicializado.");
            } catch (Exception e) {
                getLogger().warning("Erro ao inicializar sistema de itens temporizados: " + e.getMessage());
                e.printStackTrace();
            }
        }

        getServer().getPluginManager().registerEvents(new StatListeners(), this);
        getServer().getPluginManager().registerEvents(new GemaListener(), this);
        getServer().getPluginManager().registerEvents(new AcessorioListener(), this);

        // Registrar PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI hooks registrados.");
        } else {
            getLogger().warning("PlaceholderAPI não encontrado! Alguns placeholders podem não funcionar.");
        }

        getCommand("ist").setExecutor(new StatCommands(this));
        getCommand("ist").setTabCompleter(new StatCommands(this));
        
        // Registrar comando de acessórios
        getCommand("acessorios").setExecutor(new AcessorioCommand());
        getCommand("acessorios").setTabCompleter(new AcessorioCommand());
        
        // Registrar comando de itens temporizados
        if (timedItemManager != null) {
            com.drakkar.itemstatstracker.timed.TimedItemCommands timedCommands = new com.drakkar.itemstatstracker.timed.TimedItemCommands(this, timedItemManager);
            getCommand("timed").setExecutor(timedCommands);
            getCommand("timed").setTabCompleter(timedCommands);
        }
    }

    public void loadYamls() {
        saveDefaultConfig();
        reloadConfig();
        loadLevelEffectsConfig();
        loadEnchantmentsConfig();
        loadReincarnadoConfig(); // Novo
        loadItemMestreConfig();
        loadGemasConfig();
        loadAcessoriosConfig();
        LanguageManager.loadMessages(this);
    }

    private void loadLevelEffectsConfig() {
        File levelEffectsFile = new File(getDataFolder(), "level_effects.yml");
        if (!levelEffectsFile.exists()) {
            saveResource("level_effects.yml", false);
        }
        this.levelEffectsConfig = YamlConfiguration.loadConfiguration(levelEffectsFile);
    }

    private void loadEnchantmentsConfig() {
        File enchantmentsFile = new File(getDataFolder(), "enchantments.yml");
        if (!enchantmentsFile.exists()) {
            saveResource("enchantments.yml", false);
        }
        this.enchantmentsConfig = YamlConfiguration.loadConfiguration(enchantmentsFile);
    }
    
    private void loadGemasConfig() {
        File gemasFile = new File(getDataFolder(), "gemas.yml");
        if (!gemasFile.exists()) {
            saveResource("gemas.yml", false);
        }
        this.gemasConfig = YamlConfiguration.loadConfiguration(gemasFile);
    }
    
    private void loadAcessoriosConfig() {
        File acessoriosFile = new File(getDataFolder(), "acessorios.yml");
        if (!acessoriosFile.exists()) {
            saveResource("acessorios.yml", false);
        }
        this.acessoriosConfig = YamlConfiguration.loadConfiguration(acessoriosFile);
    }

    @Override
    public void onDisable() {
        // Parar tarefas de itens temporizados
        if (timedItemExpirationTask != null) {
            timedItemExpirationTask.stop();
        }
        
        // Fechar conexão do banco de dados de itens temporizados
        if (timedItemDBManager != null) {
            timedItemDBManager.close();
        }
        
        instance = null;
        getLogger().info("ItemStatsTracker has been disabled.");
    }
    
    /**
     * Retorna o gerenciador de itens temporizados (pode ser null se desabilitado)
     */
    public com.drakkar.itemstatstracker.timed.TimedItemManager getTimedItemManager() {
        return timedItemManager;
    }
}
