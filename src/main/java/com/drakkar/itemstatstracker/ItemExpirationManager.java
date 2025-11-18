package com.drakkar.itemstatstracker;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.PacketType;

public class ItemExpirationManager implements Listener {

    private final Plugin plugin;
    private final ProtocolManager protocolManager;

    // Mapeia items por UUID => timestamp de expiração
    private final Map<UUID, Long> expirations = new ConcurrentHashMap<>();

    // JSON GSON para salvar e carregar do banco
    private final Gson gson = new GsonBuilder().create();
    private final Type mapType = new TypeToken<Map<UUID, Long>>() {}.getType();
    
    // Adventure API
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    public ItemExpirationManager(Plugin plugin) {
        this.plugin = plugin;

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            this.protocolManager = ProtocolLibrary.getProtocolManager();
            setupProtocolLibListener();
            Bukkit.getConsoleSender().sendMessage("§a[ItemStats] ProtocolLib detectado — atualização visual otimizada!");
        } else {
            this.protocolManager = null;
            Bukkit.getConsoleSender().sendMessage("§e[ItemStats] ProtocolLib não detectado — usando fallback.");
        }

        startExpirationCheck();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** =========================================================
     *     CARREGA E SALVA NO BANCO (usar do jeito que você quer)
     *  ========================================================= */
    public String serializeData() {
        return gson.toJson(expirations, mapType);
    }

    public void loadFromDatabase(String json) {
        if (json == null || json.trim().isEmpty()) return;
        Map<UUID, Long> loaded = gson.fromJson(json, mapType);
        expirations.clear();
        expirations.putAll(loaded);
    }

    /** =========================================================
     *            REGISTRA PROTOCOLLIB (ATUALIZAÇÃO VISUAL)
     *  ========================================================= */
    private void setupProtocolLibListener() {
        // Interceptar SET_SLOT (atualização de slot individual)
        protocolManager.addPacketListener(new PacketAdapter(
            plugin, 
            PacketType.Play.Server.SET_SLOT
        ) {
            @Override
            public void onPacketSending(PacketEvent e) {
                try {
                    ItemStack item = e.getPacket().getItemModifier().read(0);
                    if (item == null || item.getType() == Material.AIR) return;

                    UUID id = getItemUUID(item);
                    if (id == null) return;

                    if (expirations.containsKey(id)) {
                        long left = expirations.get(id) - System.currentTimeMillis();
                        if (left > 0) {
                            // Clonar o item para evitar modificar a referência original
                            ItemStack cloned = item.clone();
                            applyLore(cloned, left);
                            e.getPacket().getItemModifier().write(0, cloned);
                        }
                    }
                } catch (Exception ex) {
                    // Ignorar erros silenciosamente
                }
            }
        });

        // Interceptar WINDOW_ITEMS (atualização de inventário completo)
        protocolManager.addPacketListener(new PacketAdapter(
            plugin,
            PacketType.Play.Server.WINDOW_ITEMS
        ) {
            @Override
            public void onPacketSending(PacketEvent e) {
                try {
                    List<ItemStack> items = e.getPacket().getItemListModifier().read(0);
                    if (items == null || items.isEmpty()) return;

                    boolean modified = false;
                    for (int i = 0; i < items.size(); i++) {
                        ItemStack item = items.get(i);
                        if (item == null || item.getType() == Material.AIR) continue;

                        UUID id = getItemUUID(item);
                        if (id == null) continue;

                        if (expirations.containsKey(id)) {
                            long left = expirations.get(id) - System.currentTimeMillis();
                            if (left > 0) {
                                // Clonar o item para evitar modificar a referência original
                                ItemStack cloned = item.clone();
                                applyLore(cloned, left);
                                items.set(i, cloned);
                                modified = true;
                            }
                        }
                    }

                    if (modified) {
                        e.getPacket().getItemListModifier().write(0, items);
                    }
                } catch (Exception ex) {
                    // Ignorar erros silenciosamente
                }
            }
        });
    }

    /** =========================================================
     *         EVENTOS — FALLBACK QUANDO NÃO HÁ PROTOCOLLIB
     *  ========================================================= */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        // Com ProtocolLib, não precisamos atualizar manualmente - ele cuida automaticamente
        // Apenas atualizar se ProtocolLib não estiver disponível
        if (protocolManager != null) return;

        updateInventory(e.getPlayer().getInventory());
        updateInventory(e.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // Com ProtocolLib, não precisamos atualizar manualmente
        if (protocolManager != null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateInventory(e.getWhoClicked().getInventory());
            updateInventory(e.getInventory());
        }, 1);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        // Com ProtocolLib, não precisamos atualizar manualmente
        if (protocolManager != null) return;
        
        updateInventory(e.getPlayer().getInventory());
    }

    private void updateInventory(org.bukkit.inventory.Inventory inv) {
        if (inv == null) return;

        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            UUID id = getItemUUID(item);
            if (id == null || !expirations.containsKey(id)) continue;

            long left = expirations.get(id) - System.currentTimeMillis();
            if (left > 0) applyLore(item, left);
        }
    }

    /** =========================================================
     *                   SISTEMA DE EXPIRAÇÃO GERAL
     *  ========================================================= */
    private void startExpirationCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                Set<UUID> expired = Sets.newHashSet();

                for (UUID id : expirations.keySet()) {
                    long expireAt = expirations.get(id);
                    if (expireAt <= now) expired.add(id);
                }

                if (!expired.isEmpty()) {
                    for (UUID id : expired) {
                        expirations.remove(id);
                        removeExpiredItems(id);
                        Bukkit.getPluginManager().callEvent(new ItemExpiredEvent(id));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // a cada 1s
    }

    private void removeExpiredItems(UUID id) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeFromInventory(p.getInventory(), id);
            removeFromInventory(p.getEnderChest(), id);
        }

        removeDropsFromWorld(id);
    }

    private void removeFromInventory(org.bukkit.inventory.Inventory inv, UUID id) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;

            if (id.equals(getItemUUID(item))) {
                inv.setItem(i, null);
            }
        }
    }

    private void removeDropsFromWorld(UUID id) {
        Bukkit.getWorlds().forEach(world -> world.getEntities().forEach(e -> {
            if (e instanceof org.bukkit.entity.Item itemEntity) {
                ItemStack item = itemEntity.getItemStack();
                if (id.equals(getItemUUID(item))) {
                    itemEntity.remove();
                }
            }
        }));
    }

    /** =========================================================
     *               REGISTRO E MANIPULAÇÃO DE UUID DO ITEM
     *  ========================================================= */
    public void registerExpiration(ItemStack item, long durationMs) {
        UUID id = getOrCreateUUID(item);

        expirations.put(id, System.currentTimeMillis() + durationMs);
        applyLore(item, durationMs);
    }

    private UUID getOrCreateUUID(ItemStack item) {
        UUID id = getItemUUID(item);
        if (id != null) return id;

        id = UUID.randomUUID();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) return id;
        }
        
        meta.getPersistentDataContainer().set(
            NamespacedKeys.ITEM_UUID,
            NamespacedKeys.UUID_TYPE,
            id.toString()
        );
        item.setItemMeta(meta);
        return id;
    }

    private UUID getItemUUID(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String raw = meta.getPersistentDataContainer().get(
                NamespacedKeys.ITEM_UUID,
                NamespacedKeys.UUID_TYPE
        );

        try {
            return raw == null ? null : UUID.fromString(raw);
        } catch (Exception e) {
            return null;
        }
    }

    /** =========================================================
     *                     LORE DINÂMICO (tempo)
     *  ========================================================= */
    private void applyLore(ItemStack item, long msLeft) {
        long sec = msLeft / 1000;
        long min = sec / 60;
        long hrs = min / 60;

        String formatted;
        if (hrs > 0) formatted = hrs + "h " + (min % 60) + "m";
        else if (min > 0) formatted = min + "m " + (sec % 60) + "s";
        else formatted = sec + "s";

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) return;
        }
        
        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        // Criar componente com MiniMessage
        Component timeComponent = MINI_MESSAGE.deserialize("<red>Expira em: <white>" + formatted + "</white></red>");

        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            String loreLine = PLAIN_TEXT.serialize(lore.get(i));
            if (loreLine.contains("Expira em") || loreLine.contains("Expira em:")) {
                lore.set(i, timeComponent);
                found = true;
                break;
            }
        }

        if (!found) {
            lore.add(timeComponent);
        }

        meta.lore(lore);
        item.setItemMeta(meta);
    }
}

