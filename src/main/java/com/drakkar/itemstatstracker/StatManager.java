package com.drakkar.itemstatstracker;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

public final class StatManager {

    private static ItemStatsTracker pluginInstance;

    private static NamespacedKey BLOCKS_BROKEN_KEY;
    private static NamespacedKey MOB_KILLS_KEY;
    private static NamespacedKey PLAYER_KILLS_KEY;
    private static NamespacedKey DAMAGE_DEALT_KEY;
    private static NamespacedKey ELYTRA_FLIGHT_TIME_KEY;
    private static NamespacedKey DAMAGE_TAKEN_KEY;
    private static NamespacedKey BOW_ARROWS_SHOT_KEY;
    private static NamespacedKey ORIGINAL_OWNER_KEY;
    private static NamespacedKey ACTIVE_ARROW_TYPE_KEY;
    private static NamespacedKey ARROW_UNLOCKS_KEY_PREFIX;
    private static NamespacedKey MILESTONE_ACHIEVED_KEY_PREFIX;
    private static NamespacedKey REINCARNADO_LEVEL_KEY;
    @Deprecated
    private static NamespacedKey ASCENSION_LEVEL_KEY; // Alias para compatibilidade
    private static NamespacedKey EFFECTS_KEY;
    private static NamespacedKey CUSTOM_EFFECTS_KEY;
    private static NamespacedKey DAMAGE_TAKEN_TOTAL_KEY; // Novo NamespacedKey para o dano total recebido
    // Novos NamespacedKeys para estatísticas granulares
    private static NamespacedKey ORES_BROKEN_KEY;
    private static NamespacedKey WOOD_CHOPPED_KEY;
    private static NamespacedKey FARM_HARVESTED_KEY;
    private static NamespacedKey DAMAGE_DEALT_PLAYER_KEY;
    private static NamespacedKey DAMAGE_DEALT_MOB_KEY;
    private static NamespacedKey DAMAGE_DEALT_UNDEAD_KEY;
    private static NamespacedKey DAMAGE_BLOCKED_KEY;
    private static NamespacedKey TRIDENT_THROWN_KEY;
    private static NamespacedKey TRIDENT_DAMAGE_KEY;
    private static NamespacedKey MACE_FALL_HEIGHT_KEY;
    private static NamespacedKey MACE_MAX_DAMAGE_KEY;
    private static NamespacedKey HOE_SOIL_TILLED_KEY;

    /**
     * Verifica se um item deve ser rastreado pelo plugin baseado no filtro de rastreamento.
     * Se o filtro estiver desabilitado, todos os itens são rastreados.
     * Se o filtro estiver habilitado, apenas itens com as tags NBT/PDC requeridas são rastreados.
     * 
     * @param item O item a ser verificado
     * @return true se o item deve ser rastreado, false caso contrário
     */
    public static boolean itemDeveSerRastreado(ItemStack item) {
        if (pluginInstance == null || item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        FileConfiguration config = pluginInstance.getConfig();
        if (config == null) {
            return true; // Se não há config, rastreia tudo (comportamento padrão)
        }
        
        // Se o filtro estiver desabilitado, rastreia tudo
        if (!config.getBoolean("tracking-filter.enable", false)) {
            return true;
        }
        
        // Verificar se o item tem ItemMeta e PDC
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false; // Sem meta, não pode ter tags NBT/PDC
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        List<String> requiredTags = config.getStringList("tracking-filter.required-tags");
        
        // Se não há tags requeridas configuradas, não rastreia nada (por segurança)
        if (requiredTags == null || requiredTags.isEmpty()) {
            return false;
        }
        
        // Verificar se o item possui QUALQUER uma das tags requeridas
        for (String tagKey : requiredTags) {
            if (tagKey == null || tagKey.trim().isEmpty()) {
                continue;
            }
            
            // Parsear a tag (formato: "namespace:key")
            String[] parts = tagKey.split(":", 2);
            if (parts.length != 2) {
                continue; // Tag inválida, pular
            }
            
            try {
                NamespacedKey key = new NamespacedKey(parts[0], parts[1]);
                
                // Verificar se o PDC possui a chave (testando vários tipos comuns)
                if (pdc.has(key, PersistentDataType.STRING) ||
                    pdc.has(key, PersistentDataType.INTEGER) ||
                    pdc.has(key, PersistentDataType.DOUBLE) ||
                    pdc.has(key, PersistentDataType.BYTE) ||
                    pdc.has(key, PersistentDataType.BOOLEAN) ||
                    pdc.has(key, PersistentDataType.LONG) ||
                    pdc.has(key, PersistentDataType.FLOAT) ||
                    pdc.has(key, PersistentDataType.SHORT)) {
                    return true; // Item possui pelo menos uma tag requerida
                }
            } catch (IllegalArgumentException e) {
                // NamespacedKey inválido, pular
                continue;
            }
        }
        
        // Nenhuma tag requerida foi encontrada
        return false;
    }

    public static void init(ItemStatsTracker plugin) {
        pluginInstance = plugin;
        BLOCKS_BROKEN_KEY = new NamespacedKey(plugin, "blocks_broken");
        MOB_KILLS_KEY = new NamespacedKey(plugin, "mob_kills");
        PLAYER_KILLS_KEY = new NamespacedKey(plugin, "player_kills");
        DAMAGE_DEALT_KEY = new NamespacedKey(plugin, "damage_dealt");
        ELYTRA_FLIGHT_TIME_KEY = new NamespacedKey(plugin, "elytra_flight_time");
        DAMAGE_TAKEN_KEY = new NamespacedKey(plugin, "damage_taken");
        BOW_ARROWS_SHOT_KEY = new NamespacedKey(plugin, "bow_arrows_shot");
        ORIGINAL_OWNER_KEY = new NamespacedKey(plugin, "original_owner");
        ACTIVE_ARROW_TYPE_KEY = new NamespacedKey(plugin, "active_arrow_type");
        ARROW_UNLOCKS_KEY_PREFIX = new NamespacedKey(plugin, "arrow_unlocks_");
        MILESTONE_ACHIEVED_KEY_PREFIX = new NamespacedKey(plugin, "milestone_achieved_");
        REINCARNADO_LEVEL_KEY = new NamespacedKey(plugin, "reincarnado_level");
        ASCENSION_LEVEL_KEY = REINCARNADO_LEVEL_KEY; // Alias para compatibilidade
        EFFECTS_KEY = new NamespacedKey(plugin, "item_effects");
        CUSTOM_EFFECTS_KEY = new NamespacedKey(plugin, "custom_item_effects");
        DAMAGE_TAKEN_TOTAL_KEY = new NamespacedKey(plugin, "damage_taken_total"); // Inicializa o novo NamespacedKey
        // Inicializa os novos NamespacedKeys
        ORES_BROKEN_KEY = new NamespacedKey(plugin, "ores_broken");
        WOOD_CHOPPED_KEY = new NamespacedKey(plugin, "wood_chopped");
        FARM_HARVESTED_KEY = new NamespacedKey(plugin, "farm_harvested");
        DAMAGE_DEALT_PLAYER_KEY = new NamespacedKey(plugin, "damage_dealt_player");
        DAMAGE_DEALT_MOB_KEY = new NamespacedKey(plugin, "damage_dealt_mob");
        DAMAGE_DEALT_UNDEAD_KEY = new NamespacedKey(plugin, "damage_dealt_undead");
        DAMAGE_BLOCKED_KEY = new NamespacedKey(plugin, "damage_blocked");
        TRIDENT_THROWN_KEY = new NamespacedKey(plugin, "trident_thrown");
        TRIDENT_DAMAGE_KEY = new NamespacedKey(plugin, "trident_damage");
        MACE_FALL_HEIGHT_KEY = new NamespacedKey(plugin, "mace_fall_height");
        MACE_MAX_DAMAGE_KEY = new NamespacedKey(plugin, "mace_max_damage");
        HOE_SOIL_TILLED_KEY = new NamespacedKey(plugin, "hoe_soil_tilled");
    }

    private static final List<String> REGISTERED_STATS = List.of(
            "BLOCKS_BROKEN", "MOB_KILLS", "PLAYER_KILLS", "DAMAGE_DEALT", "ELYTRA_FLIGHT_TIME", 
            "DAMAGE_TAKEN", "DAMAGE_TAKEN_TOTAL", "BOW_ARROWS_SHOT", 
            // Adiciona as novas estatísticas granulares
            "ORES_BROKEN", "WOOD_CHOPPED", "FARM_HARVESTED", 
            "DAMAGE_DEALT_PLAYER", "DAMAGE_DEALT_MOB", "DAMAGE_DEALT_UNDEAD",
            // Novas estatísticas
            "DAMAGE_BLOCKED", "TRIDENT_THROWN", "TRIDENT_DAMAGE", 
            "MACE_FALL_HEIGHT", "MACE_MAX_DAMAGE", "HOE_SOIL_TILLED"
    );

    public static boolean isStatEnabled(Material itemType, String statType) {
        Objects.requireNonNull(itemType, "itemType");
        Objects.requireNonNull(statType, "statType");

        if (pluginInstance == null) {
            return false;
        }
        
        // Get the configuration section for the item type.
        // If no specific configuration is found for this item type, all stats are considered enabled by default.
        return pluginInstance.getConfig()
                .getBoolean("item-stat-settings." + itemType.name() + "." + statType, true);
    }

    public static ItemStack setEffect(ItemStack item, PotionEffectType effectType, int amplifier) {
        if (item == null || effectType == null) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        Map<String, Integer> effects = getEffects(item);
        if (amplifier <= 0) {
            effects.remove(effectType.getKey().getKey());
        } else {
            effects.put(effectType.getKey().getKey(), amplifier);
        }

        container.set(EFFECTS_KEY, PersistentDataType.STRING, serializeEffects(effects));
        item.setItemMeta(meta);
        return item;
    }

    public static Map<String, Integer> getEffects(ItemStack item) {
        if (item == null) {
            return new HashMap<>();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return new HashMap<>();
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String serialized = container.getOrDefault(EFFECTS_KEY, PersistentDataType.STRING, "");
        return deserializeEffects(serialized);
    }

    private static String serializeEffects(Map<String, Integer> effects) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : effects.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
        }
        return sb.toString();
    }

    private static Map<String, Integer> deserializeEffects(String serialized) {
        Map<String, Integer> effects = new HashMap<>();
        if (serialized == null || serialized.isEmpty()) {
            return effects;
        }
        String[] parts = serialized.split(";");
        for (String part : parts) {
            String[] subParts = part.split(":");
            if (subParts.length == 2) {
                try {
                    String effectName = subParts[0];
                    int amplifier = Integer.parseInt(subParts[1]);
                    effects.put(effectName, amplifier);
                } catch (NumberFormatException ignored) {
                    // Ignore malformed entries
                }
            }
        }
        return effects;
    }

    // Métodos para Efeitos Customizados (de outros plugins)
    public static ItemStack addCustomEffect(ItemStack item, String customEffectString) {
        if (item == null || customEffectString == null || customEffectString.isEmpty()) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        List<String> customEffects = getCustomEffects(item);
        String normalizedEffect = normalizeCustomEffect(customEffectString);
        if (normalizedEffect.isEmpty()) {
            return item;
        }

        // Verifica se já existe um efeito similar (mesmo baseKey, mesmo nível ou sem nível)
        // Isso previne duplicatas como "AE:LIFESTEAL:5" e "AE:LIFESTEAL:5" ou variações
        boolean alreadyExists = false;
        String[] normalizedParts = normalizedEffect.split(":");
        String baseKey = normalizedParts.length > 1 ? normalizedParts[0] + ":" + normalizedParts[1] : normalizedEffect;
        
        for (String existing : customEffects) {
            String normalizedExisting = normalizeCustomEffect(existing);
            if (normalizedExisting.equals(normalizedEffect)) {
                alreadyExists = true;
                break;
            }
            // Também verifica se é o mesmo encantamento base (mesmo plugin:enchant)
            String[] existingParts = normalizedExisting.split(":");
            if (existingParts.length > 1 && normalizedParts.length > 1) {
                String existingBaseKey = existingParts[0] + ":" + existingParts[1];
                if (existingBaseKey.equals(baseKey)) {
                    // Se já existe o mesmo encantamento base, substitui pelo novo (mantém o nível mais alto)
                    customEffects.remove(existing);
                    break;
                }
            }
        }

        if (!alreadyExists) {
            customEffects.add(normalizedEffect);
            container.set(CUSTOM_EFFECTS_KEY, PersistentDataType.STRING, serializeCustomEffects(customEffects));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static List<String> getCustomEffects(ItemStack item) {
        if (item == null) {
            return new ArrayList<>();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return new ArrayList<>();
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String serialized = container.getOrDefault(CUSTOM_EFFECTS_KEY, PersistentDataType.STRING, "");
        return deserializeCustomEffects(serialized);
    }

    public static ItemStack removeCustomEffect(ItemStack item, String customEffectString) {
        if (item == null || customEffectString == null || customEffectString.isEmpty()) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        List<String> customEffects = getCustomEffects(item);
        String normalizedEffect = normalizeCustomEffect(customEffectString);
        if (customEffects.remove(normalizedEffect)) {
            container.set(CUSTOM_EFFECTS_KEY, PersistentDataType.STRING, serializeCustomEffects(customEffects));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String serializeCustomEffects(List<String> customEffects) {
        if (customEffects == null || customEffects.isEmpty()) {
            return "";
        }
        List<String> normalized = new ArrayList<>();
        for (String effect : customEffects) {
            String normalizedEffect = normalizeCustomEffect(effect);
            if (!normalizedEffect.isEmpty() && !normalized.contains(normalizedEffect)) {
                normalized.add(normalizedEffect);
            }
        }
        return String.join(";", normalized);
    }

    private static List<String> deserializeCustomEffects(String serialized) {
        List<String> normalized = new ArrayList<>();
        if (serialized == null || serialized.isEmpty()) {
            return normalized;
        }
        String[] parts = serialized.split(";");
        for (String part : parts) {
            String normalizedEffect = normalizeCustomEffect(part);
            if (!normalizedEffect.isEmpty() && !normalized.contains(normalizedEffect)) {
                normalized.add(normalizedEffect);
            }
        }
        return normalized;
    }

    public static String normalizeCustomEffect(String effect) {
        if (effect == null) {
            return "";
        }
        // Normaliza o efeito: remove espaços, converte para maiúsculas, substitui / por :
        String normalized = effect.replace("/", ":").toUpperCase(Locale.ROOT).trim();
        // Remove espaços extras entre os componentes
        normalized = normalized.replaceAll("\\s+", "");
        // Garante que não há duplicatas de dois pontos
        normalized = normalized.replaceAll(":+", ":");
        return normalized;
    }

    public static ItemStack setStat(ItemStack item, String statType, int value) {
        if (item == null || statType == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = resolveStatKey(statType);
        if (key == null) {
            return item;
        }
        if (value <= 0) {
            container.remove(key);
        } else {
            container.set(key, PersistentDataType.INTEGER, value);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static int getStat(ItemStack item, String statType) {
        if (item == null || statType == null) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = resolveStatKey(statType);
        if (key == null) {
            return 0;
        }
        return container.getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    public static ItemStack incrementStat(Player player, ItemStack item, String statType, int amount) {
        if (item == null || statType == null || amount == 0) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        int current = getStat(item, statType);
        setStat(item, statType, current + amount);

        // Se for um stat que contribui para DAMAGE_TAKEN_TOTAL, incrementa também o total
        if (statType.equals("DAMAGE_TAKEN")) {
            int currentTotalDamageTaken = getStat(item, DAMAGE_TAKEN_TOTAL_KEY.getKey());
            setStat(item, DAMAGE_TAKEN_TOTAL_KEY.getKey(), currentTotalDamageTaken + amount);
        }
        return item;
    }

    public static ItemStack setOriginalOwner(ItemStack item, String owner) {
        if (item == null || owner == null || owner.isEmpty()) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(ORIGINAL_OWNER_KEY, PersistentDataType.STRING, owner);
        item.setItemMeta(meta);
        return item;
    }

    public static String getOriginalOwner(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(ORIGINAL_OWNER_KEY, PersistentDataType.STRING);
    }

    public static ItemStack removeOriginalOwner(ItemStack item) {
        if (item == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.remove(ORIGINAL_OWNER_KEY);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack incrementDamageTaken(ItemStack item, org.bukkit.event.entity.EntityDamageEvent.DamageCause cause, int amount) {
        if (item == null || cause == null || amount <= 0) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(pluginInstance, "damage_taken_" + cause.name().toLowerCase());
        int current = container.getOrDefault(key, PersistentDataType.INTEGER, 0);
        container.set(key, PersistentDataType.INTEGER, current + amount);
        item.setItemMeta(meta);
        return item;
    }

    public static int getDamageTaken(ItemStack item, org.bukkit.event.entity.EntityDamageEvent.DamageCause cause) {
        if (item == null || cause == null) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(pluginInstance, "damage_taken_" + cause.name().toLowerCase());
        return container.getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    public static ItemStack incrementArrowsShot(ItemStack item, int amount) {
        if (item == null || amount <= 0) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        int current = getStat(item, BOW_ARROWS_SHOT_KEY.getKey());
        setStat(item, BOW_ARROWS_SHOT_KEY.getKey(), current + amount);
        return item;
    }

    public static ItemStack unlockArrowType(ItemStack item, String arrowType) {
        if (item == null || arrowType == null || arrowType.isEmpty()) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(pluginInstance, ARROW_UNLOCKS_KEY_PREFIX.getKey() + arrowType.toLowerCase());
        container.set(key, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isArrowUnlocked(ItemStack item, String arrowType) {
        if (item == null || arrowType == null || arrowType.isEmpty()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(pluginInstance, ARROW_UNLOCKS_KEY_PREFIX.getKey() + arrowType.toLowerCase());
        return container.has(key, PersistentDataType.BOOLEAN) && container.get(key, PersistentDataType.BOOLEAN);
    }

    public static ItemStack setActiveArrowType(ItemStack item, String arrowType) {
        if (item == null || arrowType == null || arrowType.isEmpty()) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(ACTIVE_ARROW_TYPE_KEY, PersistentDataType.STRING, arrowType.toLowerCase());
        item.setItemMeta(meta);
        return item;
    }

    public static String getActiveArrowType(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(ACTIVE_ARROW_TYPE_KEY, PersistentDataType.STRING);
    }

    // Métodos para Reincarnação
    public static int getReincarnadoLevel(ItemStack item) {
        if (item == null) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.getOrDefault(REINCARNADO_LEVEL_KEY, PersistentDataType.INTEGER, 0);
    }
    
    // Alias para compatibilidade
    @Deprecated
    public static int getAscensionLevel(ItemStack item) {
        return getReincarnadoLevel(item);
    }

    public static ItemStack setReincarnadoLevel(ItemStack item, int level) {
        if (item == null) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (level <= 0) {
            container.remove(REINCARNADO_LEVEL_KEY);
            container.remove(ASCENSION_LEVEL_KEY); // Remove também a chave antiga
        } else {
            container.set(REINCARNADO_LEVEL_KEY, PersistentDataType.INTEGER, level);
            container.set(ASCENSION_LEVEL_KEY, PersistentDataType.INTEGER, level); // Mantém compatibilidade
        }
        item.setItemMeta(meta);
        return item;
    }
    
    // Alias para compatibilidade
    @Deprecated
    public static ItemStack setAscensionLevel(ItemStack item, int level) {
        return setReincarnadoLevel(item, level);
    }

    public static boolean hasMilestoneAchieved(ItemStack item, String milestoneKey) {
        if (item == null || milestoneKey == null || milestoneKey.isEmpty()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(pluginInstance, MILESTONE_ACHIEVED_KEY_PREFIX.getKey() + milestoneKey);
        return container.has(key, PersistentDataType.BOOLEAN) && container.get(key, PersistentDataType.BOOLEAN);
    }

    public static ItemStack setMilestoneAchieved(ItemStack item, String milestoneKey, boolean achieved) {
        if (item == null || milestoneKey == null || milestoneKey.isEmpty()) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(pluginInstance, MILESTONE_ACHIEVED_KEY_PREFIX.getKey() + milestoneKey);
        if (achieved) {
            container.set(key, PersistentDataType.BOOLEAN, true);
        } else {
            container.remove(key);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static List<String> getRegisteredStats() {
        return REGISTERED_STATS;
    }

    // Novo método para obter o progresso de reincarnação de um item
    public static Map<String, Double> getReincarnadoProgress(ItemStack item) {
        Map<String, Double> progressMap = new HashMap<>();
        if (item == null || item.getType() == Material.AIR) {
            return progressMap;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return progressMap;
        }

        // Obtém o nível de reincarnação atual do item
        int currentReincarnadoLevel = getReincarnadoLevel(item);
        
        // Obtém os critérios de reincarnação para o tipo de item
        List<ReincarnadoCriterion> criteria = getReincarnadoCriteria(item);
        if (criteria.isEmpty()) {
            return progressMap; // Sem critérios, sem progressão de reincarnação.
        }

        for (ReincarnadoCriterion criterion : criteria) {
            int currentStatValue = 0;
            // Obtém o valor atual da estatística para o critério
            if (criterion.getStatType().equals("DAMAGE_TAKEN")) {
                // Para DAMAGE_TAKEN, precisamos somar o dano de todas as causas ou ter um total
                // Por enquanto, vamos usar um stat total para DAMAGE_TAKEN se existir.
                currentStatValue = getStat(item, criterion.getStatType()); // Será o DAMAGE_TAKEN_TOTAL
            } else {
                currentStatValue = getStat(item, criterion.getStatType());
            }
            
            double requiredValueForNextLevel = criterion.getRequiredValuePerLevel() * (currentReincarnadoLevel + 1);
            if (requiredValueForNextLevel <= 0) { // Evita divisão por zero
                progressMap.put(criterion.getStatType(), 1.0); // Se não há requisito, considera 100% completo
                continue;
            }

            double progress = Math.min(1.0, currentStatValue / requiredValueForNextLevel);
            progressMap.put(criterion.getStatType(), progress);
        }

        return progressMap;
    }

    // Novo método para obter os critérios de reincarnação do config.yml para um item
    public static List<ReincarnadoCriterion> getReincarnadoCriteria(ItemStack item) {
        List<ReincarnadoCriterion> criteria = new ArrayList<>();
        ItemStatsTracker plugin = ItemStatsTracker.getInstance();
        if (plugin == null || item == null) {
            return criteria;
        }

        FileConfiguration reincarnadoConfig = plugin.getReincarnadoConfig();
        if (reincarnadoConfig == null) {
            plugin.getLogger().warning("O arquivo reincarnado.yml não está carregado.");
            return criteria;
        }

        String itemCategory = getItemCategory(item.getType());
        ConfigurationSection criteriaSection = reincarnadoConfig.getConfigurationSection("reincarnado-criteria");
        if (criteriaSection == null) {
            return criteria;
        }
        
        List<Map<?, ?>> criteriaMapList = criteriaSection.getMapList(itemCategory);
        
        // Se a categoria específica não for encontrada ou estiver vazia, tenta usar a padrão "default"
        if (criteriaMapList == null || criteriaMapList.isEmpty()) {
            criteriaMapList = criteriaSection.getMapList("default");
        }

        if (criteriaMapList == null) {
            return criteria; // Nenhum critério encontrado
        }
        
        // Detecta ItemsAdder ID se disponível
        String itemsAdderId = detectItemsAdderId(item); // ex: "fiendskullset:fiendskull_sword"

        for (Map<?, ?> criterionMap : criteriaMapList) {
            if (criterionMap.get("stat-type") instanceof String statType &&
                criterionMap.get("required-value-per-level") instanceof Integer requiredValue) {
                
                Object displayNameKeyObj = criterionMap.get("display-name-key");
                String displayNameKey = (displayNameKeyObj instanceof String) ? (String) displayNameKeyObj : "stats." + statType.toLowerCase(Locale.ROOT);
                String displayName = LanguageManager.getRawString(displayNameKey);
                
                // Filtros opcionais
                boolean allowed = true;
                Object allowedItemsObj = criterionMap.get("allowed-items");
                if (allowedItemsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> allowedMap = (Map<String, Object>) allowedItemsObj;
                    // Materiais
                    if (allowedMap.containsKey("materials")) {
                        Object mats = allowedMap.get("materials");
                        if (mats instanceof List<?> matsList && !matsList.isEmpty()) {
                            boolean anyMatch = matsList.stream().filter(String.class::isInstance)
                                    .map(String.class::cast)
                                    .map(s -> s.toUpperCase(Locale.ROOT))
                                    .anyMatch(matName -> item.getType().name().equals(matName));
                            allowed = allowed && anyMatch;
                        }
                    }
                    // ItemsAdder
                    if (allowedMap.containsKey("itemsadder")) {
                        Object ia = allowedMap.get("itemsadder");
                        if (ia instanceof List<?> idList && !idList.isEmpty()) {
                            boolean anyIa = idList.stream().filter(String.class::isInstance)
                                    .map(String.class::cast)
                                    .anyMatch(id -> id.equalsIgnoreCase(itemsAdderId));
                            allowed = allowed && anyIa;
                        }
                    }
                }
                
                if (!allowed) {
                    continue; // este critério não se aplica a este item
                }
                
                if (!statType.isEmpty() && requiredValue > 0) {
                    criteria.add(new ReincarnadoCriterion(statType, requiredValue, displayName));
                }
            }
        }
        
        return criteria;
    }
    
    // Alias para compatibilidade
    @Deprecated
    public static List<AscensionCriterion> getAscensionCriteria(ItemStack item) {
        List<ReincarnadoCriterion> reincarnadoCriteria = getReincarnadoCriteria(item);
        List<AscensionCriterion> ascensionCriteria = new ArrayList<>();
        for (ReincarnadoCriterion rc : reincarnadoCriteria) {
            ascensionCriteria.add(new AscensionCriterion(rc.getStatType(), rc.getRequiredValuePerLevel(), rc.getDisplayName()));
        }
        return ascensionCriteria;
    }
    
    @Deprecated
    public static Map<String, Double> getAscensionProgress(ItemStack item) {
        return getReincarnadoProgress(item);
    }
    
    @Deprecated
    public static class AscensionCriterion {
        private final String statType;
        private final int requiredValuePerLevel;
        private final String displayName;

        public AscensionCriterion(String statType, int requiredValuePerLevel, String displayName) {
            this.statType = statType;
            this.requiredValuePerLevel = requiredValuePerLevel;
            this.displayName = displayName;
        }

        public String getStatType() {
            return statType;
        }

        public int getRequiredValuePerLevel() {
            return requiredValuePerLevel;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Tenta obter o ID namespaced do ItemsAdder (ex.: "namespace:id").
    private static String detectItemsAdderId(ItemStack item) {
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            java.lang.reflect.Method byItemStack = customStackClass.getMethod("byItemStack", ItemStack.class);
            Object customStack = byItemStack.invoke(null, item);
            if (customStack != null) {
                java.lang.reflect.Method getId = customStackClass.getMethod("getNamespacedID");
                Object id = getId.invoke(customStack);
                if (id instanceof String) {
                    return (String) id; // namespace:id
                }
            }
        } catch (Throwable ignored) {
            // ItemsAdder não instalado ou API mudou
        }
        return null;
    }

    // Novo método para determinar a categoria de um item
    public static String getItemCategory(Material type) {
        String typeName = type.name().toUpperCase(Locale.ROOT);
        if (typeName.endsWith("_SWORD")) {
            return "SWORDS";
        } else if (typeName.endsWith("_PICKAXE")) {
            return "PICKAXES";
        } else if (typeName.endsWith("_AXE")) {
            return "AXES";
        } else if (typeName.endsWith("_SHOVEL")) {
            return "SHOVELS";
        } else if (typeName.endsWith("_HOE")) {
            return "HOES";
        } else if (type == Material.BOW || type == Material.CROSSBOW) {
            return "BOWS";
        } else if (type == Material.TRIDENT) {
            return "TRIDENT";
        } else if (type == Material.MACE) {
            return "MACE";
        } else if (type == Material.SHIELD) {
            return "SHIELD";
        } else if (type == Material.ELYTRA) {
            return "CHESTPLATE"; // Elytra usa categoria CHESTPLATE para upgrades
        } else if (typeName.endsWith("_CHESTPLATE")) {
            return "CHESTPLATE";
        } else if (typeName.endsWith("_HELMET") || typeName.endsWith("_LEGGINGS") || typeName.endsWith("_BOOTS")) {
            return "ARMOR_PIECES";
        } else {
            return "default";
        }
    }

    // Classe auxiliar para representar um critério de reincarnação
    public static class ReincarnadoCriterion {
        private final String statType;
        private final int requiredValuePerLevel;
        private final String displayName;

        public ReincarnadoCriterion(String statType, int requiredValuePerLevel, String displayName) {
            this.statType = statType;
            this.requiredValuePerLevel = requiredValuePerLevel;
            this.displayName = displayName;
        }

        public String getStatType() {
            return statType;
        }

        public int getRequiredValuePerLevel() {
            return requiredValuePerLevel;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Nova classe interna para armazenar os dados de progresso do upgrade de encantamento
    public static class EnchantmentUpgradeProgress {
        private final double progress;
        private final int currentValue;
        private final int requiredValue;
        private final String displayName;

        public EnchantmentUpgradeProgress(double progress, int currentValue, int requiredValue, String displayName) {
            this.progress = progress;
            this.currentValue = currentValue;
            this.requiredValue = requiredValue;
            this.displayName = displayName;
        }

        public double getProgress() { return progress; }
        public int getCurrentValue() { return currentValue; }
        public int getRequiredValue() { return requiredValue; }
        public String getDisplayName() { return displayName; }
    }

    // Novo método para obter o progresso do upgrade de um encantamento
    public static List<EnchantmentUpgradeProgress> getEnchantmentUpgradeProgress(ItemStack item, Enchantment enchantment) {
        List<EnchantmentUpgradeProgress> progressList = new ArrayList<>();
        if (pluginInstance == null || item == null || enchantment == null) {
            return progressList;
        }

        String itemCategory = getItemCategory(item.getType());
        FileConfiguration config = pluginInstance.getConfig();
        ConfigurationSection enchantUpgradesSection = config.getConfigurationSection("enchantment-upgrades");

        if (enchantUpgradesSection == null) {
            return progressList;
        }

        String enchantNameKey = enchantment.getKey().getKey().toUpperCase(Locale.ROOT);
        ConfigurationSection enchantConfig = enchantUpgradesSection.getConfigurationSection(itemCategory + "." + enchantNameKey);

        if (enchantConfig == null) {
            enchantConfig = enchantUpgradesSection.getConfigurationSection("default." + enchantNameKey);
        }
        
        if (enchantConfig == null) {
            return progressList;
        }

        int currentLevel = item.getEnchantmentLevel(enchantment);
        int maxLevel = enchantConfig.getInt("max-level", currentLevel);

        if (currentLevel >= maxLevel) {
            return progressList; // Já está no nível máximo ou acima
        }

        List<?> criteriaList = enchantConfig.getList("criteria");
        if (criteriaList == null) return progressList;
        
        for (Object criterionObj : criteriaList) {
            if (criterionObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> criterionMap = (Map<String, Object>) criterionObj;
                String statType = (String) criterionMap.get("stat-type");
                int requiredValuePerLevel = (int) criterionMap.get("required-value-per-level");
                
                Object displayNameKeyObj = criterionMap.get("display-name-key");
                String displayNameKey = (displayNameKeyObj instanceof String) ? (String) displayNameKeyObj : "stats." + statType.toLowerCase(Locale.ROOT);
                String displayName = LanguageManager.getRawString(displayNameKey);

                int currentStatValue = getStat(item, statType);
                int requiredValueForNextLevel = requiredValuePerLevel * (currentLevel + 1);
                
                double progress = 0.0;
                if (requiredValueForNextLevel > 0) {
                    progress = Math.min(1.0, (double) currentStatValue / requiredValueForNextLevel);
                }

                progressList.add(new EnchantmentUpgradeProgress(progress, currentStatValue, requiredValueForNextLevel, displayName));
            }
        }
        
        return progressList;
    }

    // Sobrecarga para encantamentos mágicos/customizados (aceita String ao invés de Enchantment)
    public static List<EnchantmentUpgradeProgress> getEnchantmentUpgradeProgress(ItemStack item, String enchantKey) {
        List<EnchantmentUpgradeProgress> progressList = new ArrayList<>();
        if (pluginInstance == null || item == null || enchantKey == null || enchantKey.isEmpty()) {
            return progressList;
        }

        String itemCategory = getItemCategory(item.getType());
        FileConfiguration config = pluginInstance.getConfig();
        ConfigurationSection enchantUpgradesSection = config.getConfigurationSection("enchantment-upgrades");

        if (enchantUpgradesSection == null) {
            return progressList;
        }

        ConfigurationSection enchantConfig = enchantUpgradesSection.getConfigurationSection(itemCategory + "." + enchantKey);

        if (enchantConfig == null) {
            enchantConfig = enchantUpgradesSection.getConfigurationSection("default." + enchantKey);
        }
        
        if (enchantConfig == null) {
            return progressList;
        }

        // Obter nível atual do encantamento mágico do PDC
        int currentLevel = 0;
        String normalizedKey = normalizeCustomEffect(enchantKey);
        List<String> customEffects = getCustomEffects(item);
        
        for (String effect : customEffects) {
            String normalizedEffect = normalizeCustomEffect(effect);
            if (normalizedEffect.equals(normalizedKey) || normalizedEffect.startsWith(normalizedKey + ":")) {
                // Extrair o nível do formato "PLUGIN:ENCHANT:LEVEL" ou "PLUGIN:ENCHANT"
                if (normalizedEffect.equals(normalizedKey)) {
                    currentLevel = 1; // Sem nível especificado, assume nível 1
                } else {
                    String[] parts = normalizedEffect.split(":");
                    if (parts.length >= 3) {
                        try {
                            currentLevel = Integer.parseInt(parts[parts.length - 1]);
                        } catch (NumberFormatException e) {
                            currentLevel = 1; // Se não conseguir parsear, assume nível 1
                        }
                    } else {
                        currentLevel = 1; // Se não tem nível especificado, assume nível 1
                    }
                }
                break;
            }
        }

        int maxLevel = enchantConfig.getInt("max-level", currentLevel);

        if (currentLevel >= maxLevel) {
            return progressList; // Já está no nível máximo ou acima
        }

        List<?> criteriaList = enchantConfig.getList("criteria");
        if (criteriaList == null) return progressList;
        
        for (Object criterionObj : criteriaList) {
            if (criterionObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> criterionMap = (Map<String, Object>) criterionObj;
                String statType = (String) criterionMap.get("stat-type");
                int requiredValuePerLevel = (int) criterionMap.get("required-value-per-level");
                
                Object displayNameKeyObj = criterionMap.get("display-name-key");
                String displayNameKey = (displayNameKeyObj instanceof String) ? (String) displayNameKeyObj : "stats." + statType.toLowerCase(Locale.ROOT);
                String displayName = LanguageManager.getRawString(displayNameKey);

                int currentStatValue = getStat(item, statType);
                int requiredValueForNextLevel = requiredValuePerLevel * (currentLevel + 1);
                
                double progress = 0.0;
                if (requiredValueForNextLevel > 0) {
                    progress = Math.min(1.0, (double) currentStatValue / requiredValueForNextLevel);
                }

                progressList.add(new EnchantmentUpgradeProgress(progress, currentStatValue, requiredValueForNextLevel, displayName));
            }
        }
        
        return progressList;
    }

    private static NamespacedKey resolveStatKey(String statType) {
        if (statType == null) {
            return null;
        }

        return switch (statType.toUpperCase(Locale.ROOT)) {
            case "BLOCKS_BROKEN" -> BLOCKS_BROKEN_KEY;
            case "MOB_KILLS" -> MOB_KILLS_KEY;
            case "PLAYER_KILLS" -> PLAYER_KILLS_KEY;
            case "DAMAGE_DEALT" -> DAMAGE_DEALT_KEY;
            case "ELYTRA_FLIGHT_TIME" -> ELYTRA_FLIGHT_TIME_KEY;
            case "DAMAGE_TAKEN" -> DAMAGE_TAKEN_KEY;
            case "DAMAGE_TAKEN_TOTAL" -> DAMAGE_TAKEN_TOTAL_KEY; // Adicionado
            case "BOW_ARROWS_SHOT" -> BOW_ARROWS_SHOT_KEY;
            // Adiciona os novos casos para as estatísticas granulares
            case "ORES_BROKEN" -> ORES_BROKEN_KEY;
            case "WOOD_CHOPPED" -> WOOD_CHOPPED_KEY;
            case "FARM_HARVESTED" -> FARM_HARVESTED_KEY;
            case "DAMAGE_DEALT_PLAYER" -> DAMAGE_DEALT_PLAYER_KEY;
            case "DAMAGE_DEALT_MOB" -> DAMAGE_DEALT_MOB_KEY;
            case "DAMAGE_DEALT_UNDEAD" -> DAMAGE_DEALT_UNDEAD_KEY;
            case "DAMAGE_BLOCKED" -> DAMAGE_BLOCKED_KEY;
            case "TRIDENT_THROWN" -> TRIDENT_THROWN_KEY;
            case "TRIDENT_DAMAGE" -> TRIDENT_DAMAGE_KEY;
            case "MACE_FALL_HEIGHT" -> MACE_FALL_HEIGHT_KEY;
            case "MACE_MAX_DAMAGE" -> MACE_MAX_DAMAGE_KEY;
            case "HOE_SOIL_TILLED" -> HOE_SOIL_TILLED_KEY;
            default -> null;
        };
    }

    public static String toRoman(int num) {
        // Basic Roman numeral conversion for levels up to 10 (enough for potion effects)
        switch (num) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(num);
        }
    }

    public static String createProgressBar(double progress, int barLength) {
        int progressChars = (int) (progress * barLength);
        StringBuilder bar = new StringBuilder();
        bar.append("<#00F66A>");
        for (int i = 0; i < progressChars; i++) {
            bar.append("█");
        }
        bar.append("</#00F66A><gray>");
        for (int i = 0; i < barLength - progressChars; i++) {
            bar.append("█");
        }
        bar.append("</gray>");
        return bar.toString();
    }

    /**
     * Atualiza os stats do jogador, aplicando bônus de itens equipados, gemas e acessórios.
     * Este método deve ser chamado sempre que o jogador:
     * - Equipa/desequipa armadura/item (evento vanilla)
     * - "Equipa"/"desequipa" um acessório (evento da GUI)
     * - Soca/remove uma gema (evento do sistema de gemas)
     * 
     * @param player O jogador cujos stats devem ser atualizados
     */
    public static void atualizarStats(org.bukkit.entity.Player player) {
        if (player == null || pluginInstance == null) {
            return;
        }

        // UUID único para os modificadores deste plugin
        java.util.UUID modifierUUID = java.util.UUID.nameUUIDFromBytes(
            ("itemstatstracker." + player.getUniqueId().toString()).getBytes());

        // Limpar todos os modificadores antigos do plugin
        clearPluginModifiers(player, modifierUUID);

        // Map para armazenar o total de bônus de cada atributo
        java.util.Map<org.bukkit.attribute.Attribute, Double> totalBonuses = new java.util.HashMap<>();

        // 1. Processar itens equipados (armadura + mãos)
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        
        // Armaduras
        ItemStack helmet = inventory.getHelmet();
        ItemStack chestplate = inventory.getChestplate();
        ItemStack leggings = inventory.getLeggings();
        ItemStack boots = inventory.getBoots();
        
        processItemStats(player, helmet, totalBonuses);
        processItemStats(player, chestplate, totalBonuses);
        processItemStats(player, leggings, totalBonuses);
        processItemStats(player, boots, totalBonuses);
        
        // Mãos
        ItemStack mainHand = inventory.getItemInMainHand();
        ItemStack offHand = inventory.getItemInOffHand();
        
        processItemStats(player, mainHand, totalBonuses);
        processItemStats(player, offHand, totalBonuses);

        // 2. Processar acessórios equipados
        FileConfiguration acessoriosConfig = ItemStatsTracker.getInstance().getAcessoriosConfig();
        if (acessoriosConfig != null) {
            ConfigurationSection itemTypesSection = acessoriosConfig.getConfigurationSection("item-types");
            if (itemTypesSection != null) {
                for (String slotType : itemTypesSection.getKeys(false)) {
                    ItemStack acessorio = AcessorioManager.getAcessorioEquipado(player, slotType);
                    if (acessorio != null && acessorio.getType() != Material.AIR) {
                        processItemStats(player, acessorio, totalBonuses);
                    }
                }
            }
        }

        // 3. Aplicar os bônus totais ao jogador
        for (java.util.Map.Entry<org.bukkit.attribute.Attribute, Double> entry : totalBonuses.entrySet()) {
            org.bukkit.attribute.Attribute attribute = entry.getKey();
            double totalBonus = entry.getValue();
            
            if (totalBonus != 0.0) {
                org.bukkit.attribute.AttributeInstance attributeInstance = player.getAttribute(attribute);
                if (attributeInstance != null) {
                    org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                        modifierUUID,
                        "itemstatstracker.total",
                        totalBonus,
                        org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
                    );
                    attributeInstance.addModifier(modifier);
                }
            }
        }
    }

    /**
     * Processa os stats de um item (bônus base + gemas socadas).
     * 
     * @param player O jogador
     * @param item O item a ser processado
     * @param totalBonuses Map para acumular os bônus
     */
    private static void processItemStats(org.bukkit.entity.Player player, ItemStack item, 
                                         java.util.Map<org.bukkit.attribute.Attribute, Double> totalBonuses) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Verificar se o item é rastreável (passa no filtro)
        if (!itemDeveSerRastreado(item)) {
            return;
        }

        // TODO: Aplicar stats base do item (se houver configuração)
        // Por enquanto, apenas processamos gemas

        // Processar gemas socadas no item
        java.util.List<String> gemas = GemaManager.getGemasSocadas(item);
        for (String gemaId : gemas) {
            ConfigurationSection gemaStats = GemaManager.getGemaStats(gemaId);
            if (gemaStats == null) {
                continue;
            }

            // Aplicar stats da gema
            for (String statKey : gemaStats.getKeys(false)) {
                double value = gemaStats.getDouble(statKey, 0.0);
                if (value == 0.0) {
                    continue;
                }

                // Mapear stat para Attribute
                org.bukkit.attribute.Attribute attribute = mapStatToAttribute(statKey);
                if (attribute != null) {
                    totalBonuses.put(attribute, totalBonuses.getOrDefault(attribute, 0.0) + value);
                }
            }
        }
    }

    /**
     * Mapeia um nome de stat para um Attribute do Minecraft.
     * 
     * @param statKey O nome do stat (ex: "ATTACK_DAMAGE", "MAX_HEALTH")
     * @return O Attribute correspondente, ou null se não houver correspondência
     */
    private static org.bukkit.attribute.Attribute mapStatToAttribute(String statKey) {
        try {
            switch (statKey.toUpperCase(Locale.ROOT)) {
                case "ATTACK_DAMAGE":
                    return org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE;
                case "MAX_HEALTH":
                    return org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH;
                case "ARMOR":
                    return org.bukkit.attribute.Attribute.GENERIC_ARMOR;
                case "ARMOR_TOUGHNESS":
                    return org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS;
                case "KNOCKBACK_RESISTANCE":
                    return org.bukkit.attribute.Attribute.GENERIC_KNOCKBACK_RESISTANCE;
                case "MOVEMENT_SPEED":
                    return org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED;
                case "ATTACK_SPEED":
                    return org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED;
                case "LUCK":
                    return org.bukkit.attribute.Attribute.GENERIC_LUCK;
                default:
                    return null;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Remove todos os modificadores do plugin de um jogador.
     * 
     * @param player O jogador
     * @param modifierUUID O UUID dos modificadores
     */
    private static void clearPluginModifiers(org.bukkit.entity.Player player, java.util.UUID modifierUUID) {
        for (org.bukkit.attribute.Attribute attribute : org.bukkit.attribute.Attribute.values()) {
            org.bukkit.attribute.AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null) {
                attributeInstance.getModifiers().stream()
                    .filter(modifier -> modifier.getUniqueId().equals(modifierUUID))
                    .forEach(attributeInstance::removeModifier);
            }
        }
    }

    /**
     * Formata um número inteiro com separadores de milhar e formato compacto opcional para bilhões.
     * 
     * @param value O valor numérico a ser formatado
     * @param useCompactFormat Se true, usa formato compacto (1B) para números >= 1 bilhão
     * @return String formatada (ex: 10.000, 100.000, 1.000.000.000 ou 1B)
     */
    public static String formatNumber(long value, boolean useCompactFormat) {
        if (value < 10000) {
            return String.valueOf(value);
        }
        
        // Se >= 1 bilhão e compacto está habilitado, usar formato curto
        if (useCompactFormat && value >= 1_000_000_000L) {
            long billions = value / 1_000_000_000L;
            long remainder = value % 1_000_000_000L;
            if (remainder == 0) {
                return billions + "B";
            }
            // Para valores com resto, usar formato longo mesmo com compacto habilitado
            // ou podemos usar formato misto (ex: 1.5B), mas por enquanto vamos para longo
        }
        
        // Formato longo com separadores de milhar
        String numberStr = String.valueOf(value);
        StringBuilder formatted = new StringBuilder();
        int length = numberStr.length();
        
        for (int i = 0; i < length; i++) {
            if (i > 0 && (length - i) % 3 == 0) {
                formatted.append('.');
            }
            formatted.append(numberStr.charAt(i));
        }
        
        return formatted.toString();
    }
    
    /**
     * Formata um número inteiro usando a configuração do plugin.
     * 
     * @param value O valor numérico a ser formatado
     * @return String formatada
     */
    public static String formatNumber(long value) {
        if (pluginInstance == null) {
            return formatNumber(value, false);
        }
        FileConfiguration config = pluginInstance.getConfig();
        boolean useCompact = config != null && config.getBoolean("number-format.use-compact-for-billions", false);
        return formatNumber(value, useCompact);
    }
}
