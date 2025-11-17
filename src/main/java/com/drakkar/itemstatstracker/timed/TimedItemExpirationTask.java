package com.drakkar.itemstatstracker.timed;

import com.drakkar.itemstatstracker.ItemStatsTracker;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * Tarefa de expiração de itens temporizados
 * Atualiza lore e remove itens expirados em intervalos configuráveis
 */
public class TimedItemExpirationTask {
    
    private final ItemStatsTracker plugin;
    private final TimedItemManager itemManager;
    
    private BukkitTask updateTask;
    private BukkitTask containerScanTask;
    
    public TimedItemExpirationTask(ItemStatsTracker plugin, TimedItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }
    
    /**
     * Inicia as tarefas de atualização
     */
    public void start() {
        int ticks = plugin.getConfig().getInt("timed-items.scheduling.update_interval_ticks", 20);
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::runUpdate, ticks, ticks);
        
        int intervalSeconds = plugin.getConfig().getInt("timed-items.scheduling.container_scan_interval_seconds", 60);
        long intervalTicks = intervalSeconds * 20L;
        containerScanTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::scanLoadedContainers, intervalTicks, intervalTicks);
        
        plugin.getLogger().info("Tarefas de expiração de itens temporizados iniciadas.");
    }
    
    /**
     * Para as tarefas de atualização
     */
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        
        if (containerScanTask != null) {
            containerScanTask.cancel();
            containerScanTask = null;
        }
        
        plugin.getLogger().info("Tarefas de expiração de itens temporizados paradas.");
    }
    
    /**
     * Executado periodicamente para atualizar inventários e itens droppados
     * Roda na thread principal do Minecraft
     */
    private void runUpdate() {
        // 1) Inventários e ender chests dos jogadores online
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack it = inv.getItem(i);
                if (it != null && it.getType() != org.bukkit.Material.AIR) {
                    try {
                        itemManager.checkAndExpireItemStack(it, player, i);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erro ao verificar item temporizado no inventário de " + player.getName() + ": " + e.getMessage());
                    }
                }
            }
            
            // Ender chest
            Inventory ender = player.getEnderChest();
            for (int i = 0; i < ender.getSize(); i++) {
                ItemStack it = ender.getItem(i);
                if (it != null && it.getType() != org.bukkit.Material.AIR) {
                    try {
                        itemManager.checkAndExpireItemStack(it, ender.getHolder(), i);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Erro ao verificar item temporizado no ender chest de " + player.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        // 2) Itens droppados no mundo (chunks carregados)
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
                    if (entity instanceof Item) {
                        Item itemEntity = (Item) entity;
                        try {
                            itemManager.checkAndExpireEntityItem(itemEntity);
                        } catch (Exception e) {
                            // Ignorar erros de itens que podem ter sido removidos
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Escaneia containers carregados (baús, fornos, etc)
     * Coleta chunks de forma assíncrona (apenas referências) e processa na thread principal
     */
    private void scanLoadedContainers() {
        // Coletar chunks carregados de forma assíncrona (apenas referências, sem acessar blocos)
        // getLoadedChunks() retorna uma cópia, então é relativamente seguro
        java.util.List<Chunk> chunksToProcess = new java.util.ArrayList<>();
        
        try {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                Chunk[] loadedChunks = world.getLoadedChunks();
                if (loadedChunks != null) {
                    for (Chunk chunk : loadedChunks) {
                        // Apenas armazenar referência do chunk (sem acessar blocos ainda)
                        chunksToProcess.add(chunk);
                    }
                }
            }
        } catch (Exception e) {
            // Se falhar na coleta, tentar processar o que foi coletado
            plugin.getLogger().warning("Erro ao coletar chunks para scan de containers: " + e.getMessage());
        }
        
        // Processar cada chunk na thread principal (onde é seguro acessar blocos/tile entities)
        for (Chunk chunk : chunksToProcess) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    // Verificar se chunk ainda está carregado antes de processar
                    if (chunk != null && chunk.isLoaded()) {
                        scanContainersInChunk(chunk);
                    }
                } catch (Exception e) {
                    // Ignorar erros (chunk pode ter sido descarregado ou modificado)
                }
            });
        }
    }
    
    /**
     * Escaneia containers em um chunk específico
     * DEVE rodar na thread principal do Minecraft
     */
    private void scanContainersInChunk(Chunk chunk) {
        if (!chunk.isLoaded()) {
            return;
        }
        
        // Obter tile entities do chunk (só funciona na thread principal)
        org.bukkit.block.BlockState[] tileEntities = chunk.getTileEntities();
        
        for (org.bukkit.block.BlockState block : tileEntities) {
            if (block instanceof InventoryHolder) {
                Inventory inv = ((InventoryHolder) block).getInventory();
                
                try {
                    for (int i = 0; i < inv.getSize(); i++) {
                        ItemStack it = inv.getItem(i);
                        if (it != null && it.getType() != org.bukkit.Material.AIR) {
                            itemManager.checkAndExpireItemStack(it, (InventoryHolder) block, i);
                        }
                    }
                } catch (Exception e) {
                    // Ignorar erros (container pode ter sido removido ou modificado)
                }
            }
        }
    }
}

