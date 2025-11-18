package com.drakkar.itemstatstracker;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Arrow;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Trident;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.NamespacedKey;
import java.util.ArrayList;
import org.bukkit.Registry;
import java.util.Locale;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import java.util.Collection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class StatListeners implements Listener {

    private final Map<Player, Long> flyingPlayers = new ConcurrentHashMap<>();
    private final Map<Player, Map<PotionEffectType, PotionEffect>> activeEffects = new ConcurrentHashMap<>();
    private static Map<PotionEffectType, String> effectStatMapping = new ConcurrentHashMap<>();
    private final Map<UUID, java.util.Map<EquipmentSlot, Long>> lastArmorLoreUpdateMs = new ConcurrentHashMap<>();
    private final Map<UUID, java.util.Set<String>> activeSetStates = new ConcurrentHashMap<>();

    // Method to load the effect-stat mapping from config.yml
    public static void loadEffectStatMapping() {
        ItemStatsTracker plugin = ItemStatsTracker.getInstance();
        if (plugin == null) {
            return;
        }
        var config = plugin.getConfig();
        var mappingSection = config.getConfigurationSection("effect-stat-mapping");
        if (mappingSection != null) {
            for (String effectName : mappingSection.getKeys(false)) {
                // Usar NamespacedKey para resolver PotionEffectType
                NamespacedKey effectKey = NamespacedKey.fromString(effectName.toLowerCase(), plugin);
                PotionEffectType effectType = null;
                if (effectKey != null) {
                    effectType = Registry.POTION_EFFECT_TYPE.get(effectKey);
                }
                // Fallback para getByName se NamespacedKey não funcionar
                if (effectType == null) {
                    @SuppressWarnings("deprecation")
                    PotionEffectType deprecatedEffectType = PotionEffectType.getByName(effectName);
                    effectType = deprecatedEffectType;
                }
                
                if (effectType != null) {
                    String statType = mappingSection.getString(effectName);
                    if (statType != null && !statType.isEmpty()) {
                        effectStatMapping.put(effectType, statType);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Lógica de Bônus de Ascensão
        handleAscensionBlockBreakBonus(event, player, item);

        if (!isTrackable(item)) {
            return;
        }

        // Se o item for uma espada, não deve contar blocos quebrados
        if (item.getType().name().endsWith("_SWORD")) {
            return;
        }

        Material blockType = event.getBlock().getType();
        boolean statIncremented = false; // Flag to check if we need to update lore

        // Rastreamento granular para picaretas
        if (item.getType().name().endsWith("_PICKAXE")) {
            if (blockType.name().endsWith("_ORE")) {
                if (StatManager.isStatEnabled(item.getType(), "ORES_BROKEN")) {
                    StatManager.incrementStat(player, item, "ORES_BROKEN", 1);
                    checkAndApplyEnchantmentUpgrade(player, item, "ORES_BROKEN");
                    statIncremented = true;
                }
            } else if (Tag.MINEABLE_PICKAXE.isTagged(blockType)) {
                if (StatManager.isStatEnabled(item.getType(), "BLOCKS_BROKEN")) {
                    StatManager.incrementStat(player, item, "BLOCKS_BROKEN", 1);
                    checkAndApplyEnchantmentUpgrade(player, item, "BLOCKS_BROKEN");
                    statIncremented = true;
                }
            }
        }
        // Rastreamento granular para machados
        else if (item.getType().name().endsWith("_AXE")) {
            if (Tag.LOGS.isTagged(blockType) || Tag.PLANKS.isTagged(blockType) ||
                blockType.name().endsWith("_STEM") || blockType == Material.PUMPKIN || 
                blockType == Material.MELON) {
                if (StatManager.isStatEnabled(item.getType(), "WOOD_CHOPPED")) {
                    StatManager.incrementStat(player, item, "WOOD_CHOPPED", 1);
                    checkAndApplyEnchantmentUpgrade(player, item, "WOOD_CHOPPED");
                    statIncremented = true;
                }
            }
        }
        // Rastreamento granular para pás
        else if (item.getType().name().endsWith("_SHOVEL")) {
            if (Tag.MINEABLE_SHOVEL.isTagged(blockType)) {
                if (StatManager.isStatEnabled(item.getType(), "BLOCKS_BROKEN")) {
                    StatManager.incrementStat(player, item, "BLOCKS_BROKEN", 1);
                    checkAndApplyEnchantmentUpgrade(player, item, "BLOCKS_BROKEN");
                    statIncremented = true;
                }
            }
        }
        // Rastreamento para plantações (enxadas)
        else if (item.getType().name().endsWith("_HOE")) {
            if (blockType == Material.WHEAT || blockType == Material.CARROTS ||
                blockType == Material.POTATOES || blockType == Material.BEETROOTS ||
                blockType == Material.NETHER_WART) {
                if (StatManager.isStatEnabled(item.getType(), "FARM_HARVESTED")) {
                    StatManager.incrementStat(player, item, "FARM_HARVESTED", 1);
                    checkAndApplyEnchantmentUpgrade(player, item, "FARM_HARVESTED");
                    statIncremented = true;
                }
            }
        }

        if (statIncremented) {
            // Garantir que o item ainda é rastreável antes de definir dono
            if (isTrackable(item) && StatManager.getOriginalOwner(item) == null) {
                StatManager.setOriginalOwner(item, player.getName());
            }
            updateItemAndIgnoreEvent(player, item, EquipmentSlot.HAND);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isTrackable(item)) {
            return;
        }

        // Lógica de anulação/compensação: Attack % vs Protection %
        // Trata tanto Players quanto Villagers como alvos PvP
        if (event.getEntity() instanceof Player || event.getEntity() instanceof org.bukkit.entity.Villager) {
            double totalAttackBonus = 0.0;
            double totalResistanceBonus = 0.0;
            
            // Calcular bônus de ataque da arma do atacante
            if (item.hasItemMeta()) {
                PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                NamespacedKey attackKey = new NamespacedKey(ItemStatsTracker.getInstance(), "attack_bonus");
                if (pdc.has(attackKey, PersistentDataType.TAG_CONTAINER_ARRAY)) {
                    PersistentDataContainer[] bonuses = pdc.get(attackKey, PersistentDataType.TAG_CONTAINER_ARRAY);
                    if (bonuses != null) {
                        // Determinar tipo de alvo (PLAYER para jogadores e villagers)
                        String targetType = "PLAYER"; // Jogadores e villagers contam como PLAYER
                        for (PersistentDataContainer bonusContainer : bonuses) {
                            String bonusTargetType = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "target_type"), PersistentDataType.STRING);
                            Double percent = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE);
                            if (bonusTargetType != null && percent != null && bonusTargetType.equalsIgnoreCase(targetType)) {
                                totalAttackBonus += percent;
                            }
                        }
                    }
                }
            }
            
            // Aplicar resistência apenas se o defensor for um Player (villagers não têm armadura)
            if (event.getEntity() instanceof Player) {
                Player victim = (Player) event.getEntity();
                
                // Calcular bônus de resistência da armadura do defensor
                ItemStack[] armorContents = victim.getInventory().getArmorContents();
                for (ItemStack armorPiece : armorContents) {
                    if (armorPiece == null || armorPiece.getType() == Material.AIR || !armorPiece.hasItemMeta()) {
                        continue;
                    }
                    PersistentDataContainer pdc = armorPiece.getItemMeta().getPersistentDataContainer();
                    NamespacedKey resKey = new NamespacedKey(ItemStatsTracker.getInstance(), "resistance_bonus");
                    if (pdc.has(resKey, PersistentDataType.TAG_CONTAINER_ARRAY)) {
                        PersistentDataContainer[] bonuses = pdc.get(resKey, PersistentDataType.TAG_CONTAINER_ARRAY);
                        if (bonuses != null) {
                            // Determinar tipo de dano: PvP usa "PLAYER", projéteis usam "PROJECTILE", outros usam "ENTITY_ATTACK"
                            String damageCause = "ENTITY_ATTACK";
                            if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
                                damageCause = "PROJECTILE";
                            } else if (event.getDamager() instanceof Player || event.getDamager() instanceof org.bukkit.entity.Villager) {
                                // Ataques de jogadores e villagers usam "PLAYER" para resistência PvP
                                damageCause = "PLAYER";
                            }
                            for (PersistentDataContainer bonusContainer : bonuses) {
                                String bonusCause = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "cause"), PersistentDataType.STRING);
                                Double percent = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE);
                                if (bonusCause != null && percent != null) {
                                    String bonusCauseUpper = bonusCause.toUpperCase(Locale.ROOT);
                                    // Verificar se corresponde à causa do dano
                                    // PLAYER/PVP corresponde a ataques de jogadores
                                    // ENTITY_ATTACK corresponde a ataques de mobs
                                    if (bonusCauseUpper.equals("PLAYER") || bonusCauseUpper.equals("PVP")) {
                                        if (damageCause.equals("PLAYER")) {
                                            totalResistanceBonus += percent;
                                        }
                                    } else if (bonusCauseUpper.equals(damageCause.toUpperCase(Locale.ROOT))) {
                                        totalResistanceBonus += percent;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Aplicar compensação/anulação: Attack % aumenta dano, Resistance % reduz dano
            // A diferença final é aplicada ao dano
            double netBonus = totalAttackBonus - totalResistanceBonus;
            if (netBonus != 0.0) {
                double damageMultiplier = 1.0 + (netBonus / 100.0);
                // Limitar o multiplicador para evitar valores extremos
                damageMultiplier = Math.max(0.1, Math.min(2.0, damageMultiplier));
                event.setDamage(event.getDamage() * damageMultiplier);
            }
        }

        boolean statIncremented = false;

        // Rastreamento específico para tridentes
        if (item.getType() == Material.TRIDENT) {
            if (StatManager.isStatEnabled(item.getType(), "TRIDENT_DAMAGE")) {
                StatManager.incrementStat(player, item, "TRIDENT_DAMAGE", (int) event.getDamage());
                checkAndApplyEnchantmentUpgrade(player, item, "TRIDENT_DAMAGE");
                statIncremented = true;
            }
        }
        
        // Rastreamento específico para maces - maior dano aplicado
        if (item.getType() == Material.MACE) {
            if (StatManager.isStatEnabled(item.getType(), "MACE_MAX_DAMAGE")) {
                int currentMax = StatManager.getStat(item, "MACE_MAX_DAMAGE");
                int damage = (int) event.getDamage();
                if (damage > currentMax) {
                    StatManager.setStat(item, "MACE_MAX_DAMAGE", damage);
                    checkAndApplyEnchantmentUpgrade(player, item, "MACE_MAX_DAMAGE");
                    statIncremented = true;
                }
            }
        }
        
        // Incrementa a estatística geral de dano causado
        if (StatManager.isStatEnabled(item.getType(), "DAMAGE_DEALT")) {
            StatManager.incrementStat(player, item, "DAMAGE_DEALT", (int) event.getDamage());
            checkAndApplyEnchantmentUpgrade(player, item, "DAMAGE_DEALT");
            statIncremented = true;
        }

        // Rastreamento granular de dano
        // Jogadores e villagers contam como dano a players (PvP)
        if (event.getEntity() instanceof Player || event.getEntity() instanceof org.bukkit.entity.Villager) {
            if (StatManager.isStatEnabled(item.getType(), "DAMAGE_DEALT_PLAYER")) {
                StatManager.incrementStat(player, item, "DAMAGE_DEALT_PLAYER", (int) event.getDamage());
                checkAndApplyEnchantmentUpgrade(player, item, "DAMAGE_DEALT_PLAYER");
                statIncremented = true;
            }
        } else if (event.getEntity() instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) event.getEntity();
            if (victim.getType() == org.bukkit.entity.EntityType.ZOMBIE ||
                victim.getType() == org.bukkit.entity.EntityType.SKELETON ||
                victim.getType() == org.bukkit.entity.EntityType.WITHER_SKELETON ||
                victim.getType() == org.bukkit.entity.EntityType.STRAY ||
                victim.getType() == org.bukkit.entity.EntityType.HUSK ||
                victim.getType() == org.bukkit.entity.EntityType.DROWNED ||
                victim.getType() == org.bukkit.entity.EntityType.PHANTOM ||
                victim.getType() == org.bukkit.entity.EntityType.ZOMBIFIED_PIGLIN ||
                victim.getType() == org.bukkit.entity.EntityType.ZOGLIN ||
                victim.getType() == org.bukkit.entity.EntityType.GHAST) { // Adicione outros mortos-vivos conforme necessário
                if (StatManager.isStatEnabled(item.getType(), "DAMAGE_DEALT_UNDEAD")) {
                    StatManager.incrementStat(player, item, "DAMAGE_DEALT_UNDEAD", (int) event.getDamage());
                    checkAndApplyEnchantmentUpgrade(player, item, "DAMAGE_DEALT_UNDEAD");
                    statIncremented = true;
                }
            } else {
                if (StatManager.isStatEnabled(item.getType(), "DAMAGE_DEALT_MOB")) {
                    StatManager.incrementStat(player, item, "DAMAGE_DEALT_MOB", (int) event.getDamage());
                    checkAndApplyEnchantmentUpgrade(player, item, "DAMAGE_DEALT_MOB");
                    statIncremented = true;
                }
            }
        }

        if (statIncremented) {
            // Garantir que o item ainda é rastreável antes de definir dono
            if (isTrackable(item) && StatManager.getOriginalOwner(item) == null) {
                StatManager.setOriginalOwner(item, player.getName());
            }
            updateItemAndIgnoreEvent(player, item, EquipmentSlot.HAND);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        ItemStack item = killer.getInventory().getItemInMainHand();

        // Lógica de Bônus de Ascensão
        handleAscensionEntityDeathBonus(event, killer, item);

        if (!isTrackable(item)) {
            return;
        }

        // Ajuste aqui para usar as novas estatísticas granulares de dano causado
        // Jogadores e villagers contam como kills de players (PvP)
        String statType = null;
        if (event.getEntity() instanceof Player || event.getEntity() instanceof org.bukkit.entity.Villager) {
            statType = "PLAYER_KILLS";
        } else if (event.getEntity() instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) event.getEntity();
            if (victim.getType() == org.bukkit.entity.EntityType.ZOMBIE ||
                victim.getType() == org.bukkit.entity.EntityType.SKELETON ||
                victim.getType() == org.bukkit.entity.EntityType.WITHER_SKELETON ||
                victim.getType() == org.bukkit.entity.EntityType.STRAY ||
                victim.getType() == org.bukkit.entity.EntityType.HUSK ||
                victim.getType() == org.bukkit.entity.EntityType.DROWNED ||
                victim.getType() == org.bukkit.entity.EntityType.PHANTOM ||
                victim.getType() == org.bukkit.entity.EntityType.ZOMBIFIED_PIGLIN ||
                victim.getType() == org.bukkit.entity.EntityType.ZOGLIN ||
                victim.getType() == org.bukkit.entity.EntityType.GHAST) { // Adicione outros mortos-vivos conforme necessário
                // Não temos um stat específico para kills de mortos-vivos no momento, então usamos MOB_KILLS
                statType = "MOB_KILLS"; 
            } else {
                statType = "MOB_KILLS";
            }
        }

        if (statType == null || !StatManager.isStatEnabled(item.getType(), statType)) {
            return;
        }

        StatManager.incrementStat(killer, item, statType, 1);
        checkAndApplyEnchantmentUpgrade(killer, item, statType); // Chama para kills também
        updateItemAndIgnoreEvent(killer, item, EquipmentSlot.HAND);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        DamageCause cause = event.getCause();
        double totalReductionPercent = 0.0;

        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (int i = 0; i < armorContents.length; i++) {
            ItemStack armorPiece = armorContents[i];

            if (armorPiece == null || armorPiece.getType() == Material.AIR || !armorPiece.hasItemMeta()) {
                continue;
            }
            
            boolean statIncremented = false;
            // Incrementa estatísticas de dano recebido (para upgrades de encantamento)
                if (StatManager.isStatEnabled(armorPiece.getType(), "DAMAGE_TAKEN")) {
                    StatManager.incrementStat(player, armorPiece, "DAMAGE_TAKEN", (int) event.getDamage());
                checkAndApplyEnchantmentUpgrade(player, armorPiece, "DAMAGE_TAKEN");
                statIncremented = true;
            }
            if (StatManager.isStatEnabled(armorPiece.getType(), "DAMAGE_TAKEN_TOTAL")) {
                 StatManager.incrementDamageTaken(armorPiece, cause, (int) event.getDamage());
                 checkAndApplyEnchantmentUpgrade(player, armorPiece, "DAMAGE_TAKEN_TOTAL");
                 statIncremented = true;
            }

            // Atualiza a lore de forma segura e com rate limit
            if (statIncremented) {
                long now = System.currentTimeMillis();
                EquipmentSlot slot = EquipmentSlot.FEET;
                switch (i) {
                    case 1: slot = EquipmentSlot.LEGS; break;
                    case 2: slot = EquipmentSlot.CHEST; break;
                    case 3: slot = EquipmentSlot.HEAD; break;
                }
                java.util.Map<EquipmentSlot, Long> perSlot = lastArmorLoreUpdateMs.computeIfAbsent(player.getUniqueId(), k -> new java.util.concurrent.ConcurrentHashMap<>());
                long last = perSlot.getOrDefault(slot, 0L);
                if (now - last >= 500L) { // 0.5s por slot de armadura
                    updateItemAndIgnoreEvent(player, armorPiece, slot);
                    perSlot.put(slot, now);
                }
            }

            // Lógica para Bônus de Resistência
            PersistentDataContainer pdc = armorPiece.getItemMeta().getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(ItemStatsTracker.getInstance(), "resistance_bonus");
            if (pdc.has(key, PersistentDataType.TAG_CONTAINER_ARRAY)) {
                PersistentDataContainer[] bonuses = pdc.get(key, PersistentDataType.TAG_CONTAINER_ARRAY);
                if (bonuses != null) {
                    for (PersistentDataContainer bonusContainer : bonuses) {
                        String bonusCauseStr = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "cause"), PersistentDataType.STRING);
                        if (bonusCauseStr != null && bonusCauseStr.equalsIgnoreCase(cause.name())) {
                            Double percent = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE);
                            if (percent != null) {
                                totalReductionPercent += percent;
                            }
                        }
                    }
                }
            }
        }

        // Aplica a redução de dano somando todas as resistências de todas as peças
        if (totalReductionPercent > 0) {
            // Soma todas as resistências (cada peça contribui com sua resistência)
            // Cap para evitar 100% de redução
            double finalReduction = Math.min(totalReductionPercent, 95.0);
            double damageMultiplier = 1.0 - (finalReduction / 100.0);
            event.setDamage(event.getDamage() * damageMultiplier);
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack bow = event.getBow(); // Pode ser arco ou besta

        if (bow == null || !isTrackable(bow)) {
            return;
        }

        // Incrementa a estatística de flechas atiradas
        StatManager.incrementArrowsShot(bow, 1);
        updateItemAndIgnoreEvent(player, bow, EquipmentSlot.HAND);

        // Verifica o tipo de flecha ativa no PDC e modifica a flecha
        String activeArrowType = StatManager.getActiveArrowType(bow);
        if (activeArrowType != null && event.getProjectile() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getProjectile();

            switch (activeArrowType) {
                case "fire":
                    arrow.setFireTicks(200); // Flecha de fogo
                    break;
                case "explosive":
                    // Implementar lógica para flecha explosiva (ex: criar explosão ao pousar)
                    // Isso pode exigir um listener separado para o ProjectileHitEvent
                    break;
                // Adicione outros tipos de flecha aqui
            }
        }

        // A linha abaixo é importante para atualizar o item na mão do jogador com as novas estatísticas e lore
        // player.getInventory().setItemInMainHand(bow); // This line is now handled by updateItemAndIgnoreEvent
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        // Verificar e limpar dono de itens não rastreáveis quando o jogador segura o item
        if (newItem != null && newItem.getType() != Material.AIR) {
            // Se o item não é rastreável mas tem dono, limpar
            if (!isTrackable(newItem) && StatManager.getOriginalOwner(newItem) != null) {
                StatManager.removeOriginalOwner(newItem);
                // Limpar a lore removendo elementos do plugin
                LoreManager.updateLore(newItem);
                // Usar o sistema de ignore para evitar flickering
                final ItemStatsTracker plugin = ItemStatsTracker.getInstance();
                plugin.getIgnoreArmorChangeEvent().add(player.getUniqueId());
                // Atualizar o item no inventário
                player.getInventory().setItem(event.getNewSlot(), newItem);
                // Remover do ignore list após um delay
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getIgnoreArmorChangeEvent().remove(player.getUniqueId());
                }, 1L);
            }
        }

        // Remove efeitos do item antigo (se houver)
        if (oldItem != null) {
            removeEffects(player, oldItem);
        }

        // Aplica efeitos ao novo item (se houver)
        if (newItem != null) {
            applyEffects(player, newItem);
        }
    }

    @EventHandler
    public void onPlayerArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        if (ItemStatsTracker.getInstance().getIgnoreArmorChangeEvent().contains(player.getUniqueId())) {
            return;
        }

        // Seta dono na peça de armadura recém equipada, se ainda não tiver e se for rastreável
        ItemStack newItem = event.getNewItem();
        if (newItem != null && newItem.getType() != Material.AIR && isTrackable(newItem)) {
            if (StatManager.getOriginalOwner(newItem) == null) {
                StatManager.setOriginalOwner(newItem, player.getName());
                updateItemAndIgnoreEvent(player, newItem, findItemSlot(player, newItem));
            }
        }

        // A verificação é feita um tick depois para garantir que o inventário do jogador esteja totalmente atualizado.
        ItemStatsTracker.getInstance().getServer().getScheduler().runTask(ItemStatsTracker.getInstance(), () -> {
            checkAndApplySetBonus(event.getPlayer());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeSetBonuses(event.getPlayer());
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMoveForElytra(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack chestplate = player.getInventory().getChestplate();

        if (chestplate == null || chestplate.getType() != Material.ELYTRA) {
            // Se o jogador tinha elytra e não tem mais, finalizar voo
            if (flyingPlayers.containsKey(player)) {
                long startTime = flyingPlayers.remove(player);
                long flightDurationSeconds = (System.currentTimeMillis() - startTime) / 1000;
                if (flightDurationSeconds > 0) {
                    // Buscar elytra no inventário (pode ter sido desequipado)
                    ItemStack elytraInInventory = null;
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.ELYTRA && StatManager.itemDeveSerRastreado(item)) {
                            elytraInInventory = item;
                            break;
                        }
                    }
                    if (elytraInInventory != null && StatManager.isStatEnabled(elytraInInventory.getType(), "ELYTRA_FLIGHT_TIME")) {
                        StatManager.incrementStat(player, elytraInInventory, "ELYTRA_FLIGHT_TIME", (int) flightDurationSeconds);
                        checkAndApplyEnchantmentUpgrade(player, elytraInInventory, "ELYTRA_FLIGHT_TIME");
                    }
                }
            }
            return;
        }

        // Verificar se o jogador está gliding (planando com elytra)
        boolean isGliding = player.isGliding();
        boolean wasGliding = flyingPlayers.containsKey(player);

        if (isGliding && !wasGliding) {
            // Player started gliding
            flyingPlayers.put(player, System.currentTimeMillis());
        } else if (!isGliding && wasGliding) {
            // Player stopped gliding
            long startTime = flyingPlayers.remove(player);
            long flightDurationSeconds = (System.currentTimeMillis() - startTime) / 1000;

            if (flightDurationSeconds > 0) {
                if (!StatManager.isStatEnabled(chestplate.getType(), "ELYTRA_FLIGHT_TIME")) {
                    return;
                }
                StatManager.incrementStat(player, chestplate, "ELYTRA_FLIGHT_TIME", (int) flightDurationSeconds);
                checkAndApplyEnchantmentUpgrade(player, chestplate, "ELYTRA_FLIGHT_TIME");
                updateItemAndIgnoreEvent(player, chestplate, EquipmentSlot.CHEST);
            }
        }
    }

    private void updateItemAndIgnoreEvent(Player player, ItemStack item, EquipmentSlot slot) {
        final ItemStatsTracker plugin = ItemStatsTracker.getInstance();
        plugin.getIgnoreArmorChangeEvent().add(player.getUniqueId());
        
        // Só define dono se o item for rastreável
        if (isTrackable(item) && StatManager.getOriginalOwner(item) == null) {
            StatManager.setOriginalOwner(item, player.getName());
        }

        // Só atualiza lore se o item for rastreável
        if (isTrackable(item)) {
            LoreManager.updateLore(item);
        }
        
        // Atualiza o item no inventário do jogador
        if (slot != null) {
            switch (slot) {
                case HAND:
                    player.getInventory().setItemInMainHand(item);
                    break;
                case OFF_HAND:
                     player.getInventory().setItemInOffHand(item);
                    break;
                case HEAD:
                     player.getInventory().setHelmet(item);
                    break;
                case CHEST:
                     player.getInventory().setChestplate(item);
                    break;
                case LEGS:
                     player.getInventory().setLeggings(item);
                    break;
                case FEET:
                     player.getInventory().setBoots(item);
                    break;
            }
        }
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getIgnoreArmorChangeEvent().remove(player.getUniqueId());
            // Atualizar stats do jogador após equipar/desequipar armadura
            StatManager.atualizarStats(player);
        }, 1L);
    }

    private boolean isTrackable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // Usar o filtro de rastreamento do StatManager
        return StatManager.itemDeveSerRastreado(item);
    }

    private void applyEffects(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        ItemStatsTracker plugin = ItemStatsTracker.getInstance();
        if (plugin == null) {
            return;
        }

        // Remove a linha que declara itemTypeName se não for usada para evitar o aviso de lint.
        // String itemTypeName = item.getType().name(); 
        
        // Obtém o nível de ascensão do item
        int reincarnadoLevel = StatManager.getReincarnadoLevel(item);
        if (reincarnadoLevel <= 0) { // Só aplica efeitos se o item tiver nível de reincarnação
            return;
        }

        // Obtém a seção de efeitos por nível do level_effects.yml
        FileConfiguration levelEffectsFile = plugin.getLevelEffectsConfig();
        ConfigurationSection levelEffectsSection = levelEffectsFile.getConfigurationSection("LEVEL_EFFECTS");

        if (levelEffectsSection == null) {
            plugin.getLogger().warning("Seção 'LEVEL_EFFECTS' não encontrada no level_effects.yml!");
            return;
        }

        Map<PotionEffectType, PotionEffect> currentEffects = activeEffects.computeIfAbsent(player, k -> new ConcurrentHashMap<>());

        // Itera sobre todos os níveis configurados no level_effects.yml
        for (String levelKey : levelEffectsSection.getKeys(false)) {
            try {
                int configuredLevel = Integer.parseInt(levelKey);
                if (reincarnadoLevel >= configuredLevel) {
                    ConfigurationSection effectsAtLevel = levelEffectsSection.getConfigurationSection(levelKey);
                    if (effectsAtLevel == null) continue;

                    // Aplica efeitos de poção para este nível
                    ConfigurationSection potionEffectsSection = effectsAtLevel.getConfigurationSection("potion_effects");
                    if (potionEffectsSection != null) {
                        for (String effectName : potionEffectsSection.getKeys(false)) {
                            // Usar NamespacedKey para resolver PotionEffectType
                            NamespacedKey effectKey = NamespacedKey.fromString(effectName.toLowerCase(), plugin);
                            PotionEffectType effectType = null;
                            if (effectKey != null) {
                                effectType = Registry.POTION_EFFECT_TYPE.get(effectKey);
                            }
                            // Fallback para getByName se NamespacedKey não funcionar
                            if (effectType == null) {
                                @SuppressWarnings("deprecation")
                                PotionEffectType deprecatedEffectType = PotionEffectType.getByName(effectName);
                                effectType = deprecatedEffectType;
                            }
                            
            if (effectType == null) {
                                plugin.getLogger().warning("Efeito de poção inválido configurado em level_effects.yml: " + effectName + " no nível " + levelKey); // Corrigido
                continue;
            }
                            int amplifier = potionEffectsSection.getInt(effectName, 0);
                            int duration = PotionEffect.INFINITE_DURATION; // Efeitos de ascensão são geralmente permanentes
                            boolean ambient = true; // Geralmente ambient
                            boolean particles = false; // Geralmente sem partículas para ser menos intrusivo

                            if (effectType.equals(PotionEffectType.HASTE)) {
                                if (amplifier > 0) {
                                    item.addUnsafeEnchantment(Enchantment.EFFICIENCY, amplifier);
                                } else {
                                    item.removeEnchantment(Enchantment.EFFICIENCY);
                                }
                            } else if (amplifier > 0) {
                                PotionEffect newEffect = new PotionEffect(effectType, duration, amplifier, ambient, particles);
                                // Só adiciona se o jogador não tiver o efeito ou o efeito atual for mais fraco
                                if (!player.hasPotionEffect(effectType) || 
                                    java.util.Objects.requireNonNull(player.getPotionEffect(effectType)).getAmplifier() < amplifier) {
                                    player.addPotionEffect(newEffect);
                                    currentEffects.put(effectType, newEffect);
                                }
                            }
                        }
                    }

                    // Aplica atributos para este nível (implementação futura, apenas logando por enquanto)
                    ConfigurationSection attributesSection = effectsAtLevel.getConfigurationSection("attributes");
                    if (attributesSection != null) {
                        plugin.getLogger().info("Atributos configurados para o nível de ascensão " + levelKey + ": " + attributesSection.getKeys(false));
                        // TODO: Implementar a lógica para aplicar atributos aqui.
                    }
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Nível de ascensão inválido configurado em level_effects.yml: " + levelKey);
            }
        }
        updateItemAndIgnoreEvent(player, item, findItemSlot(player, item));
    }

    private EquipmentSlot findItemSlot(Player player, ItemStack item) {
        if (item == null) return null;
        if (item.equals(player.getInventory().getItemInMainHand())) return EquipmentSlot.HAND;
        if (item.equals(player.getInventory().getItemInOffHand())) return EquipmentSlot.OFF_HAND;
        if (item.equals(player.getInventory().getHelmet())) return EquipmentSlot.HEAD;
        if (item.equals(player.getInventory().getChestplate())) return EquipmentSlot.CHEST;
        if (item.equals(player.getInventory().getLeggings())) return EquipmentSlot.LEGS;
        if (item.equals(player.getInventory().getBoots())) return EquipmentSlot.FEET;
        return null;
    }

    private void removeEffects(Player player, ItemStack item) { // Adiciona o parâmetro ItemStack
        Map<PotionEffectType, PotionEffect> playerActiveEffects = activeEffects.get(player);
        if (playerActiveEffects == null || playerActiveEffects.isEmpty()) {
            return;
        }

        playerActiveEffects.keySet().retainAll(StatManager.getEffects(item).keySet().stream()
            .<PotionEffectType>map(s -> { // Explicitamente especifica o tipo para o map
                NamespacedKey effectKey = NamespacedKey.fromString(s.toLowerCase(), ItemStatsTracker.getInstance());
                if (effectKey != null) {
                    return Registry.POTION_EFFECT_TYPE.get(effectKey);
                }
                @SuppressWarnings("deprecation") // PotionEffectType.getByName() é obsoleto, mas necessário para nomes legados
                PotionEffectType legacyEffectType = PotionEffectType.getByName(s);
                return legacyEffectType;
            })
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet()));
    }

    private void handleAscensionBlockBreakBonus(BlockBreakEvent event, Player player, ItemStack item) {
        int reincarnadoLevel = StatManager.getReincarnadoLevel(item);
        if (reincarnadoLevel <= 0) {
            return;
        }
        
        FileConfiguration config = ItemStatsTracker.getInstance().getConfig();
        // Agora são multiplicadores diretos, não chances
        // Exemplo: 0.005 = 0.5% por nível, então nível 1000 = 500% de bônus
        double dropBonusPerLevel = config.getDouble("reincarnado.bonus-drop-percentage-per-level", 0.005);
        double expBonusPerLevel = config.getDouble("reincarnado.bonus-exp-percentage-per-level", 0.01);

        // Bônus de Drop - aplica como multiplicador
        if (dropBonusPerLevel > 0) {
            double totalDropMultiplier = 1.0 + (reincarnadoLevel * dropBonusPerLevel);
            Collection<ItemStack> drops = event.getBlock().getDrops(item);
            if (!drops.isEmpty()) {
                // Calcula quantos drops extras dar baseado no multiplicador
                // Exemplo: multiplicador 6.0 (500% bônus) = 5x drops extras
                int extraDropsMultiplier = (int) Math.floor(totalDropMultiplier - 1.0);
                if (extraDropsMultiplier > 0) {
                    for (int i = 0; i < extraDropsMultiplier; i++) {
                        for (ItemStack drop : drops) {
                            if (drop != null && drop.getType() != Material.AIR) {
                                ItemStack clonedDrop = drop.clone();
                                // Para plantas, garante que o drop seja válido
                                if (clonedDrop.getAmount() > 0) {
                                    event.getBlock().getWorld().dropItemNaturally(
                                        event.getBlock().getLocation().add(0.5, 0.5, 0.5), 
                                        clonedDrop
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bônus de Experiência - aplica como multiplicador
        if (expBonusPerLevel > 0 && event.getExpToDrop() > 0) {
            double totalExpMultiplier = 1.0 + (reincarnadoLevel * expBonusPerLevel);
            int newExp = (int) Math.round(event.getExpToDrop() * totalExpMultiplier);
            event.setExpToDrop(newExp);
        }
    }

    private void handleAscensionEntityDeathBonus(EntityDeathEvent event, Player player, ItemStack item) {
        int reincarnadoLevel = StatManager.getReincarnadoLevel(item);
        if (reincarnadoLevel <= 0) {
            return;
        }

        FileConfiguration config = ItemStatsTracker.getInstance().getConfig();
        // Agora são multiplicadores diretos, não chances
        double dropBonusPerLevel = config.getDouble("reincarnado.bonus-drop-percentage-per-level", 0.005);
        double expBonusPerLevel = config.getDouble("reincarnado.bonus-exp-percentage-per-level", 0.01);

        // Bônus de Drop - aplica como multiplicador
        if (dropBonusPerLevel > 0 && !event.getDrops().isEmpty()) {
            double totalDropMultiplier = 1.0 + (reincarnadoLevel * dropBonusPerLevel);
            List<ItemStack> originalDrops = new ArrayList<>(event.getDrops());
            // Calcula quantos conjuntos extras de drops dar baseado no multiplicador
            int extraDropsMultiplier = (int) Math.floor(totalDropMultiplier - 1.0);
            for (int i = 0; i < extraDropsMultiplier; i++) {
                for (ItemStack drop : originalDrops) {
                    if (drop != null && drop.getType() != Material.AIR) {
                        event.getDrops().add(drop.clone());
                    }
                }
            }
        }

        // Bônus de Experiência - aplica como multiplicador
        if (expBonusPerLevel > 0 && event.getDroppedExp() > 0) {
            double totalExpMultiplier = 1.0 + (reincarnadoLevel * expBonusPerLevel);
            int newExp = (int) Math.round(event.getDroppedExp() * totalExpMultiplier);
            event.setDroppedExp(newExp);
        }
    }

    private void checkAndApplyEnchantmentUpgrade(Player player, ItemStack item, String statTypeJustIncremented) {
        ItemStatsTracker plugin = ItemStatsTracker.getInstance();
        if (plugin == null) return;

        // Obter a categoria do item (SWORDS, PICKAXES, ARMOR_PIECES, default)
        String itemCategory = StatManager.getItemCategory(item.getType());
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection enchantUpgradesSection = config.getConfigurationSection("enchantment-upgrades");

        if (enchantUpgradesSection == null) return;

        // Tenta obter os upgrades específicos para a categoria do item, caso contrário, usa o padrão
        ConfigurationSection categoryUpgrades = enchantUpgradesSection.getConfigurationSection(itemCategory);
        if (categoryUpgrades == null) {
            categoryUpgrades = enchantUpgradesSection.getConfigurationSection("default");
        }

        if (categoryUpgrades == null) return; // Sem upgrades definidos para esta categoria ou padrão

        // Itera sobre os encantamentos configurados para upgrade na categoria
        FileConfiguration enchantmentsConfig = plugin.getEnchantmentsConfig();
        for (String enchantNameKey : categoryUpgrades.getKeys(false)) { // SHARPNESS, SMITE, AE:LIFESTEAL, BP:ENCHANT
            ConfigurationSection enchantConfig = categoryUpgrades.getConfigurationSection(enchantNameKey);
            if (enchantConfig == null) continue;
            
            boolean isCustomEnchant = enchantNameKey.contains(":"); // Formato: "AE:ENCHANT" ou "BP:ENCHANT"
            int currentLevel = 0;
            int maxLevel = 1;
            
            if (isCustomEnchant) {
                // Encantamento mágico/customizado (AE, BP, etc.)
                String normalizedKey = StatManager.normalizeCustomEffect(enchantNameKey);
                List<String> customEffects = StatManager.getCustomEffects(item);
                
                // Buscar o encantamento mágico no item
                for (String effect : customEffects) {
                    String normalizedEffect = StatManager.normalizeCustomEffect(effect);
                    // Verificar se é o mesmo encantamento (com ou sem nível)
                    if (normalizedEffect.equals(normalizedKey) || normalizedEffect.startsWith(normalizedKey + ":")) {
                        // Extrair o nível do formato "PLUGIN:ENCHANT:LEVEL" ou "PLUGIN:ENCHANT"
                        if (normalizedEffect.equals(normalizedKey)) {
                            // Sem nível especificado, assume nível 1
                            currentLevel = 1;
                        } else {
                            // Tem nível especificado
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
                
                // Para encantamentos mágicos, permitir upgrade mesmo se ainda não está no item (nível 0 -> 1)
                // Mas verificar se há critérios configurados antes de permitir
                maxLevel = enchantConfig.getInt("max-level", 10);
                
                // Se o encantamento não está no item E não há critérios configurados, pular
                if (currentLevel == 0) {
                    List<?> tempCriteriaList = enchantConfig.getList("criteria");
                    if (tempCriteriaList == null || tempCriteriaList.isEmpty()) {
                        continue; // Sem critérios, não pode ganhar o encantamento
                    }
                }
            } else {
                // Encantamento vanilla
                @SuppressWarnings("deprecation")
                Enchantment finalEnchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantNameKey.toLowerCase(Locale.ROOT)));

                if (finalEnchantment == null) {
                    plugin.getLogger().warning("Encantamento inválido ou não encontrado no config.yml: '" + enchantNameKey + "'. Verifique se o nome está correto e corresponde a um encantamento vanilla ou use o formato 'PLUGIN:ENCHANT' para encantamentos mágicos.");
                    continue;
                }

                currentLevel = item.getEnchantmentLevel(finalEnchantment);
                maxLevel = enchantConfig.getInt("max-level", currentLevel);
            }

            if (currentLevel >= maxLevel) {
                continue; // Já está no nível máximo
            }

            List<?> criteriaList = enchantConfig.getList("criteria");
            if (criteriaList == null || criteriaList.isEmpty()) {
                continue;
            }

            boolean isStatRelevantForThisEnchantment = false;
            List<StatManager.ReincarnadoCriterion> upgradeCriteria = new ArrayList<>();
            for (Object criterionObj : criteriaList) {
                if (criterionObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> criterionMap = (Map<String, Object>) criterionObj;
                    String statType = (String) criterionMap.get("stat-type");
                    Object reqValueObj = criterionMap.get("required-value-per-level");
                    String displayName = (String) criterionMap.get("display-name");

                    if (statType != null && reqValueObj instanceof Integer) {
                        int requiredValuePerLevel = (Integer) reqValueObj;
                        upgradeCriteria.add(new StatManager.ReincarnadoCriterion(statType, requiredValuePerLevel, displayName));
                        if (statType.equals(statTypeJustIncremented)) {
                            isStatRelevantForThisEnchantment = true;
                        }
                    }
                }
            }

            // Otimização: Se a estatística que mudou não é usada por este encantamento, pule-o.
            if (!isStatRelevantForThisEnchantment) {
                continue;
            }

            // Agora, verifique TODOS os critérios
            boolean allCriteriaMet = true;
            for (StatManager.ReincarnadoCriterion currentCriterion : upgradeCriteria) {
                int currentStatValue = StatManager.getStat(item, currentCriterion.getStatType());
                int requiredValueForNextLevel = currentCriterion.getRequiredValuePerLevel() * (currentLevel + 1);

                if (currentStatValue < requiredValueForNextLevel) {
                    allCriteriaMet = false;
                    break; // Um critério não foi atendido, não há necessidade de verificar os outros
                }
            }

            if (allCriteriaMet) {
                int newLevel = currentLevel + 1;
                
                if (isCustomEnchant) {
                    // Aplicar upgrade em encantamento mágico/customizado
                    String normalizedKey = StatManager.normalizeCustomEffect(enchantNameKey);
                    List<String> customEffects = StatManager.getCustomEffects(item);
                    
                    // Remover todas as versões antigas do encantamento (com qualquer nível)
                    // Precisamos remover usando o formato original, não o normalizado
                    List<String> toRemove = new ArrayList<>();
                    for (String effect : customEffects) {
                        String normalizedEffect = StatManager.normalizeCustomEffect(effect);
                        if (normalizedEffect.startsWith(normalizedKey + ":") || normalizedEffect.equals(normalizedKey)) {
                            toRemove.add(effect); // Usar o formato original para remover
                        }
                    }
                    
                    // Remover todas as versões antigas
                    for (String effectToRemove : toRemove) {
                        StatManager.removeCustomEffect(item, effectToRemove);
                    }
                    
                    // Adicionar o encantamento com o novo nível
                    String newEffect = normalizedKey + ":" + newLevel;
                    StatManager.addCustomEffect(item, newEffect);
                    
                    // Nota: Os encantamentos mágicos são salvos no PDC do item via addCustomEffect
                    // Se você precisar aplicar via plugins externos (AE, BP), isso deve ser feito
                    // manualmente ou através de eventos do próprio plugin externo
                } else {
                    // Aplicar upgrade em encantamento vanilla
                    @SuppressWarnings("deprecation")
                    Enchantment finalEnchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantNameKey.toLowerCase(Locale.ROOT)));
                    if (finalEnchantment != null) {
                        item.addUnsafeEnchantment(finalEnchantment, newLevel);
                    }
                }

                updateItemAndIgnoreEvent(player, item, findItemSlot(player, item));
                
                // Obter nome de exibição e formato do encantamento (igual à lore)
                String displayName;
                String loreFormat = null;
                
                if (isCustomEnchant) {
                    // Buscar no CUSTOM_EFFECTS do enchantments.yml
                    ConfigurationSection customEffectsSection = enchantmentsConfig != null 
                        ? enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS") : null;
                    String normalizedKey = StatManager.normalizeCustomEffect(enchantNameKey);
                    ConfigurationSection effectConfig = customEffectsSection != null 
                        ? customEffectsSection.getConfigurationSection(normalizedKey) : null;
                    if (effectConfig != null) {
                        displayName = effectConfig.getString("display-name", enchantNameKey);
                        loreFormat = effectConfig.getString("lore-format");
                    } else {
                        displayName = enchantNameKey;
                    }
                } else {
                    String enchantConfigPath = "ENCHANTMENTS." + enchantNameKey.toUpperCase(Locale.ROOT);
                    if (enchantmentsConfig != null) {
                        displayName = enchantmentsConfig.getString(enchantConfigPath + ".display-name", enchantNameKey);
                        loreFormat = enchantmentsConfig.getString(enchantConfigPath + ".lore-format");
                    } else {
                        displayName = enchantNameKey;
                    }
                }
                
                // Criar mensagem formatada usando o mesmo estilo da lore
                net.kyori.adventure.text.Component message;
                
                if (loreFormat != null && !loreFormat.isEmpty()) {
                    // Usar o formato da lore para o encantamento
                    String romanLevel = StatManager.toRoman(newLevel);
                    String formattedEnchant = loreFormat.replace("%display_name%", displayName).replace("%roman_level%", romanLevel);
                    
                    // Criar mensagem com gradiente dourado para "Seu item evoluiu!"
                    net.kyori.adventure.text.Component enchantComponent = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize(formattedEnchant);
                    
                    message = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<gradient:#ffd700:#ffed4e>Seu item evoluiu! </gradient>")
                        .append(enchantComponent)
                        .append(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize("<gradient:#ffd700:#ffed4e> agora é nível </gradient>"))
                        .append(net.kyori.adventure.text.Component.text(romanLevel, net.kyori.adventure.text.format.TextColor.color(255, 255, 255)))
                        .append(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize("<gradient:#ffd700:#ffed4e>.</gradient>"));
                } else {
                    // Fallback para formato simples se não houver lore-format
                    String romanLevel = StatManager.toRoman(newLevel);
                    message = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<gradient:#ffd700:#ffed4e>Seu item evoluiu! </gradient>")
                        .append(net.kyori.adventure.text.Component.text(displayName, net.kyori.adventure.text.format.TextColor.color(255, 255, 255)))
                        .append(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize("<gradient:#ffd700:#ffed4e> agora é nível </gradient>"))
                        .append(net.kyori.adventure.text.Component.text(romanLevel, net.kyori.adventure.text.format.TextColor.color(255, 255, 255)))
                        .append(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize("<gradient:#ffd700:#ffed4e>.</gradient>"));
                }
                
                player.sendMessage(message);
            }
        }
    }

    private void checkAndApplySetBonus(Player player) {
        ItemStatsTracker plugin = ItemStatsTracker.getInstance();
        ConfigurationSection armorSetsSection = plugin.getConfig().getConfigurationSection("armor-sets");
        if (armorSetsSection == null) return;

        ItemStack[] armorContents = player.getInventory().getArmorContents();

        java.util.Set<String> playerState = activeSetStates.computeIfAbsent(player.getUniqueId(), k -> java.util.concurrent.ConcurrentHashMap.newKeySet());

        for (String setId : armorSetsSection.getKeys(false)) {
            ConfigurationSection armorSet = armorSetsSection.getConfigurationSection(setId);
            if (armorSet == null) continue;

            ConfigurationSection pieces = armorSet.getConfigurationSection("pieces");
            if (pieces == null) continue;

            int equippedCount = 0;
            for (ItemStack armorPiece : armorContents) {
                if (armorPiece != null && armorPiece.getType() != Material.AIR) {
                    String pieceName = armorPiece.getType().name();
                    if (pieceName.equals(pieces.getString("HELMET")) ||
                        pieceName.equals(pieces.getString("CHESTPLATE")) ||
                        pieceName.equals(pieces.getString("LEGGINGS")) ||
                        pieceName.equals(pieces.getString("BOOTS"))) {
                        equippedCount++;
                    }
                }
            }

            ConfigurationSection setBonusSection = armorSet.getConfigurationSection("set-bonus");
            if (setBonusSection == null) continue;

            for (String tierKey : setBonusSection.getKeys(false)) { // Itera sobre "2_pieces", "4_pieces"
                int requiredPieces;
                try {
                    requiredPieces = Integer.parseInt(tierKey.split("_")[0]);
                } catch (NumberFormatException e) { continue; }

                boolean shouldBeActive = equippedCount >= requiredPieces;
                String stateKey = setId + "|" + tierKey;
                boolean isRecordedActive = playerState.contains(stateKey);
                ConfigurationSection tierBonusSection = setBonusSection.getConfigurationSection(tierKey);
                if (tierBonusSection == null) continue;

                // Só aplica/avisa quando o estado muda
                if (shouldBeActive != isRecordedActive) {
                    handleAttributeBonuses(player, setId, tierKey, tierBonusSection, shouldBeActive, requiredPieces);
                    handlePotionEffectBonuses(player, setId, tierKey, tierBonusSection, shouldBeActive, requiredPieces);
                    if (shouldBeActive) {
                        playerState.add(stateKey);
                    } else {
                        playerState.remove(stateKey);
                    }
                }
            }
        }
    }
    
    private void handleAttributeBonuses(Player player, String setId, String tierKey, ConfigurationSection tierBonusSection, boolean shouldBeActive, int requiredPieces) {
        ConfigurationSection attributesSection = tierBonusSection.getConfigurationSection("attributes");
        if (attributesSection == null) return;
        
        for (String attributeName : attributesSection.getKeys(false)) {
            try {
                Attribute attribute = Attribute.valueOf(attributeName.toUpperCase());
                UUID modifierUUID = UUID.nameUUIDFromBytes(("itemstatstracker." + setId + "." + tierKey + "." + attributeName).getBytes());
                org.bukkit.attribute.AttributeInstance playerAttribute = player.getAttribute(attribute);
                if (playerAttribute == null) continue;

                boolean isCurrentlyActive = playerAttribute.getModifiers().stream().anyMatch(m -> m.getUniqueId().equals(modifierUUID));
                ConfigurationSection attributeConfig = attributesSection.getConfigurationSection(attributeName);
                if (attributeConfig == null) continue;
                String bonusDisplayName = attributeConfig.getString("display-name", attributeName);

                String stageKey = requiredPieces >= 4 ? "setbonus.stage.full" : "setbonus.stage.partial";
                String stageTemplate = LanguageManager.getRawString(stageKey);
                if (stageTemplate == null || stageTemplate.startsWith("§cMissing")) {
                    stageTemplate = requiredPieces >= 4 ? "Conjunto Completo" : "<pieces> Peças";
                }
                String stageResolved = stageTemplate.replace("<pieces>", String.valueOf(requiredPieces));
                var stageComponent = MiniMessage.miniMessage().deserialize(stageResolved);
                var bonusComponent = MiniMessage.miniMessage().deserialize(bonusDisplayName);

                if (shouldBeActive && !isCurrentlyActive) {
                    double value = attributeConfig.getDouble("value");
                    AttributeModifier modifier = new AttributeModifier(modifierUUID, "itemstatstracker." + setId, value, AttributeModifier.Operation.ADD_NUMBER);
                    playerAttribute.addModifier(modifier);
                    player.sendMessage(LanguageManager.getMessage("setbonus.activated", 
                        Placeholder.unparsed("pieces", String.valueOf(requiredPieces)),
                        Placeholder.component("stage", stageComponent),
                        Placeholder.component("bonus_name", bonusComponent)));
                } else if (!shouldBeActive && isCurrentlyActive) {
                    playerAttribute.getModifiers().stream()
                        .filter(m -> m.getUniqueId().equals(modifierUUID))
                        .findFirst()
                        .ifPresent(playerAttribute::removeModifier);
                    player.sendMessage(LanguageManager.getMessage("setbonus.deactivated", 
                        Placeholder.unparsed("pieces", String.valueOf(requiredPieces)),
                        Placeholder.component("stage", stageComponent),
                        Placeholder.component("bonus_name", bonusComponent)));
                }
            } catch (Exception e) {
                ItemStatsTracker.getInstance().getLogger().warning("Erro ao processar atributo de bônus de conjunto: " + e.getMessage());
            }
        }
    }

    private void handlePotionEffectBonuses(Player player, String setId, String tierKey, ConfigurationSection tierBonusSection, boolean shouldBeActive, int requiredPieces) {
        ConfigurationSection potionsSection = tierBonusSection.getConfigurationSection("potion_effects");
        if (potionsSection == null) return;

        for (String effectName : potionsSection.getKeys(false)) {
            try {
                PotionEffectType effectType = PotionEffectType.getByName(effectName.toUpperCase());
                if (effectType == null) continue;

                ConfigurationSection effectConfig = potionsSection.getConfigurationSection(effectName);
                if (effectConfig == null) continue;
                String bonusDisplayName = effectConfig.getString("display-name", effectName);
                int amplifier = effectConfig.getInt("amplifier", 0);

                boolean isCurrentlyActive = player.getActivePotionEffects().stream()
                    .anyMatch(e -> e.getType().equals(effectType) && e.getAmplifier() == amplifier);

                String stageKey = requiredPieces >= 4 ? "setbonus.stage.full" : "setbonus.stage.partial";
                String stageTemplate = LanguageManager.getRawString(stageKey);
                if (stageTemplate == null || stageTemplate.startsWith("§cMissing")) {
                    stageTemplate = requiredPieces >= 4 ? "Conjunto Completo" : "<pieces> Peças";
                }
                String stageResolved = stageTemplate.replace("<pieces>", String.valueOf(requiredPieces));
                var stageComponent = MiniMessage.miniMessage().deserialize(stageResolved);
                var bonusComponent = MiniMessage.miniMessage().deserialize(bonusDisplayName);

                if (shouldBeActive && !isCurrentlyActive) {
                    player.addPotionEffect(new PotionEffect(effectType, PotionEffect.INFINITE_DURATION, amplifier, true, false));
                    player.sendMessage(LanguageManager.getMessage("setbonus.activated", 
                        Placeholder.unparsed("pieces", String.valueOf(requiredPieces)),
                        Placeholder.component("stage", stageComponent),
                        Placeholder.component("bonus_name", bonusComponent)));
                } else if (!shouldBeActive && isCurrentlyActive) {
                    player.removePotionEffect(effectType);
                    player.sendMessage(LanguageManager.getMessage("setbonus.deactivated", 
                        Placeholder.unparsed("pieces", String.valueOf(requiredPieces)),
                        Placeholder.component("stage", stageComponent),
                        Placeholder.component("bonus_name", bonusComponent)));
                }
            } catch (Exception e) {
                ItemStatsTracker.getInstance().getLogger().warning("Erro ao processar efeito de poção de bônus de conjunto: " + e.getMessage());
            }
        }
    }

    private void removeSetBonuses(Player player) {
        ItemStatsTracker plugin = ItemStatsTracker.getInstance();
        ConfigurationSection armorSetsSection = plugin.getConfig().getConfigurationSection("armor-sets");
        if (armorSetsSection == null) return;

        for (String setId : armorSetsSection.getKeys(false)) {
            ConfigurationSection armorSet = armorSetsSection.getConfigurationSection(setId);
            if (armorSet == null) continue;

            ConfigurationSection setBonusSection = armorSet.getConfigurationSection("set-bonus");
            if (setBonusSection == null) continue;
            
            for (String tierKey : setBonusSection.getKeys(false)) {
                ConfigurationSection tierBonusSection = setBonusSection.getConfigurationSection(tierKey);
                if (tierBonusSection == null) continue;

                // Remove Atributos
                ConfigurationSection attributesSection = tierBonusSection.getConfigurationSection("attributes");
                if (attributesSection != null) {
                    for (String attributeName : attributesSection.getKeys(false)) {
                        try {
                            Attribute attribute = Attribute.valueOf(attributeName.toUpperCase());
                            UUID modifierUUID = UUID.nameUUIDFromBytes(("itemstatstracker." + setId + "." + tierKey + "." + attributeName).getBytes());
                            
                            org.bukkit.attribute.AttributeInstance playerAttribute = player.getAttribute(attribute);
                            if (playerAttribute != null) {
                                playerAttribute.getModifiers().stream()
                                    .filter(m -> m.getUniqueId().equals(modifierUUID))
                                    .findFirst()
                                    .ifPresent(playerAttribute::removeModifier);
                            }
                        } catch (IllegalArgumentException e) {
                            // Silencioso
                        }
                    }
                }
                
                // Remove Efeitos de Poção
                ConfigurationSection potionsSection = tierBonusSection.getConfigurationSection("potion_effects");
                if (potionsSection != null) {
                    for (String effectName : potionsSection.getKeys(false)) {
                        PotionEffectType effectType = PotionEffectType.getByName(effectName.toUpperCase());
                        if (effectType != null && player.hasPotionEffect(effectType)) {
                            player.removePotionEffect(effectType);
                        }
                    }
                }
            }
        }
    }
    
    private String getArmorSetId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        ItemStatsTracker plugin = ItemStatsTracker.getInstance();
        if (plugin == null) {
            return null;
        }

        ConfigurationSection armorSetsSection = plugin.getConfig().getConfigurationSection("armor-sets");
        if (armorSetsSection == null) {
            return null;
        }

        // Itera sobre todos os conjuntos configurados
        for (String setId : armorSetsSection.getKeys(false)) {
            ConfigurationSection armorSet = armorSetsSection.getConfigurationSection(setId);
            if (armorSet == null) continue;

            ConfigurationSection pieces = armorSet.getConfigurationSection("pieces");
            if (pieces == null) continue;

            String helmetType = pieces.getString("HELMET");
            if (helmetType != null && item.getType().name().equals(helmetType)) return setId;

            String chestplateType = pieces.getString("CHESTPLATE");
            if (chestplateType != null && item.getType().name().equals(chestplateType)) return setId;

            String leggingsType = pieces.getString("LEGGINGS");
            if (leggingsType != null && item.getType().name().equals(leggingsType)) return setId;

            String bootsType = pieces.getString("BOOTS");
            if (bootsType != null && item.getType().name().equals(bootsType)) return setId;
        }
        return null;
    }
    
    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.isCancelled() || !(event.getEntity().getShooter() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity().getShooter();
        
        // Rastreamento para tridentes lançados
        if (event.getEntity() instanceof Trident) {
            Trident trident = (Trident) event.getEntity();
            ItemStack tridentItem = trident.getItem();
            if (tridentItem != null && isTrackable(tridentItem)) {
                if (StatManager.isStatEnabled(tridentItem.getType(), "TRIDENT_THROWN")) {
                    // Verificar se o tridente ainda está na mão do jogador
                    // Quando um tridente é lançado, ele é removido da mão
                    // Se ainda estiver na mão, atualizar; caso contrário, o tridente será
                    // atualizado quando retornar (com Loyalty) ou quando causar dano
                    ItemStack handItem = player.getInventory().getItemInMainHand();
                    
                    if (handItem != null && handItem.getType() == Material.TRIDENT && 
                        handItem.isSimilar(tridentItem)) {
                        // O tridente ainda está na mão, atualizar
                        StatManager.incrementStat(player, handItem, "TRIDENT_THROWN", 1);
                        checkAndApplyEnchantmentUpgrade(player, handItem, "TRIDENT_THROWN");
                        // Garantir que o item ainda é rastreável antes de definir dono
                        if (isTrackable(handItem) && StatManager.getOriginalOwner(handItem) == null) {
                            StatManager.setOriginalOwner(handItem, player.getName());
                        }
                        updateItemAndIgnoreEvent(player, handItem, EquipmentSlot.HAND);
                    }
                    // Se o tridente não está mais na mão, não fazer nada aqui
                    // O tridente será atualizado quando retornar ao inventário ou causar dano
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!isTrackable(item) || !item.getType().name().endsWith("_HOE")) {
            return;
        }
        
        // Rastreamento para enxadas arando terra
        Material blockType = event.getClickedBlock().getType();
        if (blockType == Material.DIRT || blockType == Material.GRASS_BLOCK || 
            blockType == Material.DIRT_PATH || blockType == Material.COARSE_DIRT ||
            blockType == Material.PODZOL || blockType == Material.MYCELIUM) {
            if (StatManager.isStatEnabled(item.getType(), "HOE_SOIL_TILLED")) {
                StatManager.incrementStat(player, item, "HOE_SOIL_TILLED", 1);
                checkAndApplyEnchantmentUpgrade(player, item, "HOE_SOIL_TILLED");
                // Garantir que o item ainda é rastreável antes de definir dono
                if (isTrackable(item) && StatManager.getOriginalOwner(item) == null) {
                    StatManager.setOriginalOwner(item, player.getName());
                }
                updateItemAndIgnoreEvent(player, item, EquipmentSlot.HAND);
            }
        }
    }
    
    // Mapa para rastrear dano original antes do bloqueio (para escudos)
    private final Map<UUID, Double> playerOriginalDamage = new ConcurrentHashMap<>();
    
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onEntityDamageBeforeShield(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Armazenar dano original antes do bloqueio (na prioridade mais baixa, antes do escudo processar)
        if (player.isBlocking()) {
            playerOriginalDamage.put(player.getUniqueId(), event.getDamage());
        }
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onEntityDamageWithShield(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Verificar se o jogador estava bloqueando com escudo (na prioridade mais alta, depois do escudo processar)
        Double originalDamage = playerOriginalDamage.remove(player.getUniqueId());
        if (originalDamage != null && originalDamage > 0) {
            ItemStack shield = player.getInventory().getItemInOffHand();
            if (shield != null && shield.getType() == Material.SHIELD && isTrackable(shield)) {
                // Calcular dano bloqueado (diferença entre dano original e dano final)
                double finalDamage = event.getFinalDamage();
                double blockedDamage = Math.max(0, originalDamage - finalDamage);
                
                if (blockedDamage > 0 && StatManager.isStatEnabled(shield.getType(), "DAMAGE_BLOCKED")) {
                    StatManager.incrementStat(player, shield, "DAMAGE_BLOCKED", (int) blockedDamage);
                    checkAndApplyEnchantmentUpgrade(player, shield, "DAMAGE_BLOCKED");
                    // Garantir que o item ainda é rastreável antes de definir dono
                    if (isTrackable(shield) && StatManager.getOriginalOwner(shield) == null) {
                        StatManager.setOriginalOwner(shield, player.getName());
                    }
                    updateItemAndIgnoreEvent(player, shield, EquipmentSlot.OFF_HAND);
                }
            }
        }
    }
    
    // Mapa para rastrear altura inicial de queda dos jogadores (para maces)
    private final Map<UUID, Double> playerFallStartHeights = new ConcurrentHashMap<>();
    
    @EventHandler
    public void onPlayerFallDamage(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        if (event.getCause() != DamageCause.FALL) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item != null && item.getType() == Material.MACE && isTrackable(item)) {
            Double fallStartHeight = playerFallStartHeights.remove(player.getUniqueId());
            if (fallStartHeight != null && StatManager.isStatEnabled(item.getType(), "MACE_FALL_HEIGHT")) {
                // Calcular altura de queda: diferença entre altura inicial e altura final
                // O dano de queda no Minecraft é calculado como: (altura - 3) * 1, então altura ≈ dano + 3
                double currentY = player.getLocation().getY();
                // Usar o dano do evento para calcular a altura de queda mais precisamente
                double fallDamage = event.getDamage();
                // Fórmula aproximada: altura de queda ≈ (dano / 0.5) + 3 (para dano de queda padrão)
                int fallHeight = (int) Math.max(0, fallStartHeight - currentY);
                // Se o cálculo pela diferença de altura for menor, usar o cálculo pelo dano
                int fallHeightByDamage = (int) Math.max(0, (fallDamage / 0.5) + 3);
                fallHeight = Math.max(fallHeight, fallHeightByDamage);
                
                int currentMax = StatManager.getStat(item, "MACE_FALL_HEIGHT");
                if (fallHeight > currentMax) {
                    StatManager.setStat(item, "MACE_FALL_HEIGHT", fallHeight);
                    checkAndApplyEnchantmentUpgrade(player, item, "MACE_FALL_HEIGHT");
                    // Garantir que o item ainda é rastreável antes de definir dono
                    if (isTrackable(item) && StatManager.getOriginalOwner(item) == null) {
                        StatManager.setOriginalOwner(item, player.getName());
                    }
                    updateItemAndIgnoreEvent(player, item, EquipmentSlot.HAND);
                }
            }
        }
    }
    
    // Rastrear altura inicial de queda dos jogadores quando começam a cair
    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        
        Player player = event.getPlayer();
        double velocityY = player.getVelocity().getY();
        
        // Se o jogador está caindo (velocidade Y negativa) e ainda não está rastreando
        if (velocityY < -0.1 && !playerFallStartHeights.containsKey(player.getUniqueId())) {
            double y = event.getFrom().getY();
            playerFallStartHeights.put(player.getUniqueId(), y);
        } else if (velocityY >= 0 || (event.getTo().getBlock().getType().isSolid() && event.getTo().getY() <= event.getFrom().getY())) {
            // Se o jogador parou de cair ou tocou o chão, remover do rastreamento
            playerFallStartHeights.remove(player.getUniqueId());
        }
    }
}
