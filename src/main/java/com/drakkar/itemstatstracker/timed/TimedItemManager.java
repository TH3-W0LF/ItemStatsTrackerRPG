package com.drakkar.itemstatstracker.timed;

import com.drakkar.itemstatstracker.ItemStatsTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de itens temporizados
 * Responsável por marcar itens com tempo de expiração e atualizar lore
 */
public class TimedItemManager {
    
    private final ItemStatsTracker plugin;
    private final TimedItemDBManager db;
    
    // Keys para PersistentDataContainer
    private final NamespacedKey keyExpireAt;
    private final NamespacedKey keyExpireId;
    private final NamespacedKey keyTimedTag;
    
    // Cache em memória para acesso rápido: expireId -> expireAt
    private final Map<String, Long> cache = new ConcurrentHashMap<>();
    
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    public TimedItemManager(ItemStatsTracker plugin, TimedItemDBManager db) {
        this.plugin = plugin;
        this.db = db;
        this.keyExpireAt = new NamespacedKey(plugin, "timed_expire_at");
        this.keyExpireId = new NamespacedKey(plugin, "timed_expire_id");
        this.keyTimedTag = new NamespacedKey(plugin, "timed_tag");
    }
    
    /**
     * Adiciona timer a um item existente (sem dar o item novamente)
     * Útil para adicionar timer a itens que o jogador já está segurando
     */
    public ItemStack addTimerToItem(Player player, ItemStack item, long seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("seconds must be > 0");
        }
        
        long expireAt = System.currentTimeMillis() + (seconds * 1000L);
        String id = UUID.randomUUID().toString();
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) {
                plugin.getLogger().warning("Não foi possível criar ItemMeta para " + item.getType());
                return item;
            }
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyExpireAt, PersistentDataType.LONG, expireAt);
        pdc.set(keyExpireId, PersistentDataType.STRING, id);
        pdc.set(keyTimedTag, PersistentDataType.BYTE, (byte) 1);
        
        // Atualizar lore com tempo inicial
        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        lore = updateOrAddTimeLore(lore, expireAt);
        meta.lore(lore);
        
        item.setItemMeta(meta);
        
        // Registrar no banco de dados
        db.insertTimedItem(id, player.getUniqueId(), item.getType().name(), item.getAmount(), expireAt, System.currentTimeMillis());
        
        cache.put(id, expireAt);
        
        return item;
    }
    
    /**
     * Dá um item temporizado para o jogador e registra no banco de dados
     */
    public ItemStack giveTimedItem(Player player, ItemStack item, long seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("seconds must be > 0");
        }
        
        long expireAt = System.currentTimeMillis() + (seconds * 1000L);
        String id = UUID.randomUUID().toString();
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) {
                plugin.getLogger().warning("Não foi possível criar ItemMeta para " + item.getType());
                return item;
            }
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyExpireAt, PersistentDataType.LONG, expireAt);
        pdc.set(keyExpireId, PersistentDataType.STRING, id);
        pdc.set(keyTimedTag, PersistentDataType.BYTE, (byte) 1);
        
        // Atualizar lore com tempo inicial
        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        lore = updateOrAddTimeLore(lore, expireAt);
        meta.lore(lore);
        
        item.setItemMeta(meta);
        
        // Registrar no banco de dados
        db.insertTimedItem(id, player.getUniqueId(), item.getType().name(), item.getAmount(), expireAt, System.currentTimeMillis());
        
        // Adicionar ao inventário do jogador
        HashMap<Integer, ItemStack> couldnt = player.getInventory().addItem(item);
        
        // Se não couber no inventário, dropar no chão
        couldnt.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
        
        String giveMsgRaw = plugin.getConfig().getString("timed-items.messages.give_msg", 
                "<gradient:#00ff88:#00ffaa>✓</gradient> <gradient:#ff8c42:#ffd700>Você recebeu um item temporizado por <white>%seconds% segundos</white>.</gradient>");
        String giveMsg = giveMsgRaw.replace("%seconds%", String.valueOf(seconds));
        player.sendMessage(MINI_MESSAGE.deserialize(giveMsg));
        
        cache.put(id, expireAt);
        
        return item;
    }
    
    /**
     * Atualiza ou adiciona a linha de tempo na lore com estilo bonito
     */
    public List<Component> updateOrAddTimeLore(List<Component> lore, long expireAt) {
        if (lore == null) {
            lore = new ArrayList<>();
        }
        
        String timeLine = TimeUtil.formatRemaining(expireAt);
        
        // Usar gradiente bonito igual ao resto do plugin
        // Gradiente laranja/dourado para indicar tempo limitado
        String formattedTime = "<gradient:#ff8c42:#ffd700>[Expira em] <white>" + timeLine + "</white></gradient>";
        Component timeComponent = MINI_MESSAGE.deserialize(formattedTime);
        
        // Procurar linha existente que contenha "Expira em" ou "[Expira em]"
        boolean updated = false;
        for (int i = 0; i < lore.size(); i++) {
            String loreLine = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lore.get(i));
            if (loreLine.contains("[Expira em]") || loreLine.contains("Expira em") || loreLine.contains("Expira em:")) {
                lore.set(i, timeComponent);
                updated = true;
                break;
            }
        }
        
        if (!updated) {
            // Adicionar no final ou em posição específica
            int position = plugin.getConfig().getInt("timed-items.lore.position", -1);
            if (position >= 0 && position < lore.size()) {
                lore.add(position, timeComponent);
            } else {
                lore.add(timeComponent);
            }
        }
        
        return lore;
    }
    
    /**
     * Verifica e expira um ItemStack se necessário
     * @return true se o item expirou e foi removido
     */
    public boolean checkAndExpireItemStack(ItemStack stack, InventoryHolder holder, int slot) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(keyTimedTag, PersistentDataType.BYTE)) {
            return false;
        }
        
        if (!pdc.has(keyExpireAt, PersistentDataType.LONG)) {
            return false;
        }
        
        long expireAt = pdc.get(keyExpireAt, PersistentDataType.LONG);
        long now = System.currentTimeMillis();
        
        if (expireAt <= now) {
            // Item expirado - remover
            String id = null;
            if (pdc.has(keyExpireId, PersistentDataType.STRING)) {
                id = pdc.get(keyExpireId, PersistentDataType.STRING);
            }
            
            // Remover do inventário
            if (holder instanceof Player) {
                Player p = (Player) holder;
                p.getInventory().setItem(slot, null);
                String expiredMsgRaw = plugin.getConfig().getString("timed-items.messages.expired_msg", 
                        "<gradient:#ff4d4d:#ff6b6b>⚠</gradient> <gradient:#ff4d4d:#ff8c42>Um item expirou e foi removido.</gradient>");
                p.sendMessage(MINI_MESSAGE.deserialize(expiredMsgRaw));
            } else if (holder instanceof BlockState && holder instanceof InventoryHolder) {
                Inventory inv = ((InventoryHolder) holder).getInventory();
                if (inv != null) {
                    inv.setItem(slot, null);
                }
            } else if (holder != null) {
                Inventory inv = holder.getInventory();
                if (inv != null) {
                    inv.setItem(slot, null);
                }
            }
            
            // Remover do banco de dados
            if (id != null) {
                db.deleteById(id);
                cache.remove(id);
            }
            
            // Disparar evento
            ItemStack expiredStack = stack.clone();
            Bukkit.getScheduler().runTask(plugin, () -> {
                TimedItemExpiredEvent event = new TimedItemExpiredEvent(expiredStack, holder);
                Bukkit.getPluginManager().callEvent(event);
            });
            
            return true;
        } else {
            // Item não expirado - NÃO atualizar nada aqui para evitar flicker
            // A atualização visual da lore será feita pelo TimedItemVisualUpdater quando necessário
            // (onJoin, onHeld, onInventoryOpen)
            // O PDC já contém os dados corretos, não precisa ser reescrito
            
            return false;
        }
    }
    
    /**
     * Verifica e expira um item droppado no mundo
     */
    public boolean checkAndExpireEntityItem(Item entityItem) {
        if (entityItem == null || !entityItem.getItemStack().hasItemMeta()) {
            return false;
        }
        
        ItemStack stack = entityItem.getItemStack();
        ItemMeta meta = stack.getItemMeta();
        
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(keyTimedTag, PersistentDataType.BYTE)) {
            return false;
        }
        
        if (!pdc.has(keyExpireAt, PersistentDataType.LONG)) {
            return false;
        }
        
        long expireAt = pdc.get(keyExpireAt, PersistentDataType.LONG);
        long now = System.currentTimeMillis();
        
        if (expireAt <= now) {
            // Item expirado - remover
            String id = null;
            if (pdc.has(keyExpireId, PersistentDataType.STRING)) {
                id = pdc.get(keyExpireId, PersistentDataType.STRING);
            }
            
            entityItem.remove();
            
            // Remover do banco de dados
            if (id != null) {
                db.deleteById(id);
                cache.remove(id);
            }
            
            // Disparar evento
            ItemStack expiredStack = stack.clone();
            Bukkit.getScheduler().runTask(plugin, () -> {
                TimedItemExpiredEvent event = new TimedItemExpiredEvent(expiredStack, null);
                Bukkit.getPluginManager().callEvent(event);
            });
            
            return true;
        } else {
            // Item não expirado - NÃO atualizar lore aqui para evitar flicker
            // A atualização visual da lore será feita pelo TimedItemVisualUpdater quando necessário
            // Para itens droppados, não precisamos atualizar constantemente
            // O PDC já contém os dados corretos
            
            return false;
        }
    }
    
    /**
     * Verifica se um item é temporizado
     */
    public boolean isTimedItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(keyTimedTag, PersistentDataType.BYTE);
    }
    
    /**
     * Obtém o tempo restante em segundos de um item temporizado
     * @return tempo restante em segundos, ou -1 se não for um item temporizado ou já expirado
     */
    public long getRemainingTime(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return -1;
        }
        
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return -1;
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(keyTimedTag, PersistentDataType.BYTE)) {
            return -1;
        }
        
        if (!pdc.has(keyExpireAt, PersistentDataType.LONG)) {
            return -1;
        }
        
        long expireAt = pdc.get(keyExpireAt, PersistentDataType.LONG);
        long now = System.currentTimeMillis();
        long remaining = expireAt - now;
        
        if (remaining <= 0) {
            return -1;
        }
        
        return remaining / 1000L; // Converter para segundos
    }
    
    // Getters para as keys
    public NamespacedKey getKeyExpireAt() {
        return keyExpireAt;
    }
    
    public NamespacedKey getKeyExpireId() {
        return keyExpireId;
    }
    
    public NamespacedKey getKeyTimedTag() {
        return keyTimedTag;
    }
}

