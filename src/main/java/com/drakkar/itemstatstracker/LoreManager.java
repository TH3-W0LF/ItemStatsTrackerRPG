package com.drakkar.itemstatstracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class LoreManager {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String LORE_MARKER_PREFIX = "ISTATS:";
    private static final String EFFECT_LORE_MARKER_PREFIX = "IEFFECTS:";
    private static final String ARMOR_SET_LORE_MARKER_PREFIX = "IARMORSET:";
    
    // Ícones para encantamentos - lista variada para deixar bonito
    private static final String[] ENCHANTMENT_ICONS = {
        "❤", "✦", "✿", "✷", "✘", "☯", "✂", "💀", "⚔", "👑", "🪓", "🎃", "👻", "🍬", "🦇", "🧡", "🌐", "❄", "⛊", "⚜", "💠", "🛡", "✈", "💎", "🎯", "⌛", "🔱", "🧟"
    };

    private LoreManager() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * Retorna um ícone baseado no nome do encantamento
     * Usa hash determinístico para sempre retornar o mesmo ícone para o mesmo encantamento
     */
    private static String getEnchantmentIcon(String enchantName) {
        if (enchantName == null || enchantName.isEmpty()) {
            return ENCHANTMENT_ICONS[0];
        }
        int hash = Math.abs(enchantName.hashCode());
        return ENCHANTMENT_ICONS[hash % ENCHANTMENT_ICONS.length];
    }

    public static String capitalizeFirstLetter(String original) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1).toLowerCase();
    }
    
    /**
     * Retorna a cor temática para um tipo de alvo de ataque.
     * Cores seguem padrões temáticos para diferentes tipos de alvos.
     */
    private static String getAttackColor(String targetType) {
        if (targetType == null) {
            return "<gradient:#ff6b6b:#ff8787>";
        }
        String targetUpper = targetType.toUpperCase(Locale.ROOT);
        
        // Jogadores: Vermelho/Laranja (PvP)
        if (targetUpper.contains("PLAYER")) {
            return "<gradient:#ff4d4d:#ff8c42>";
        }
        
        // Mobs: Verde/Lima (PvE)
        if (targetUpper.contains("MOB")) {
            return "<gradient:#7bff00:#a8ff42>";
        }
        
        // Mortos-vivos: Roxo/Violeta (Undead)
        if (targetUpper.contains("UNDEAD")) {
            return "<gradient:#9d50bb:#6e48aa>";
        }
        
        // Padrão: Vermelho suave
        return "<gradient:#ff6b6b:#ff8787>";
    }
    
    /**
     * Retorna a cor temática para um tipo de bônus de ferramenta.
     * Cores seguem padrões temáticos para diferentes tipos de bônus.
     */
    private static String getToolColor(String bonusType) {
        if (bonusType == null) {
            return "<gradient:#4facfe:#00f2fe>";
        }
        String bonusUpper = bonusType.toUpperCase(Locale.ROOT);
        
        // Velocidade de mineração: Azul/Ciano
        if (bonusUpper.contains("MINING_SPEED") || bonusUpper.contains("SPEED")) {
            return "<gradient:#4facfe:#00f2fe>";
        }
        
        // Chance de drop: Dourado/Amarelo
        if (bonusUpper.contains("DROP") || bonusUpper.contains("LOOT")) {
            return "<gradient:#f09819:#edde5d>";
        }
        
        // Experiência: Verde/Lima
        if (bonusUpper.contains("EXP") || bonusUpper.contains("EXPERIENCE") || bonusUpper.contains("XP")) {
            return "<gradient:#7bff00:#a8ff42>";
        }
        
        // Durabilidade: Cinza/Azul claro
        if (bonusUpper.contains("DURABILITY") || bonusUpper.contains("DURATION")) {
            return "<gradient:#c0c0c0:#e0e0e0>";
        }
        
        // Padrão: Azul claro
        return "<gradient:#4facfe:#00f2fe>";
    }
    
    /**
     * Retorna a cor temática para um tipo de dano/resistência.
     * Cores seguem padrões temáticos:
     * - Fogo: Vermelho e Laranja
     * - Água/Gelo: Azul claro e Azul escuro
     * - Explosão: Roxo
     * - Outros: Sequência de cores harmoniosas
     */
    private static String getResistanceColor(String cause) {
        if (cause == null) {
            return "<gradient:#c0c0c0:#e0e0e0>";
        }
        String causeUpper = cause.toUpperCase(Locale.ROOT);
        
        // Fogo e Lava
        if (causeUpper.contains("FIRE") || causeUpper.contains("LAVA") || causeUpper.contains("BURN") || causeUpper.contains("HOT")) {
            return "<gradient:#ff4d4d:#ff8c42>";
        }
        
        // Água, Gelo e Afogamento
        if (causeUpper.contains("WATER") || causeUpper.contains("DROWN") || causeUpper.contains("ICE") || causeUpper.contains("FREEZE") || causeUpper.contains("FROST")) {
            return "<gradient:#4facfe:#00f2fe>";
        }
        
        // Explosão
        if (causeUpper.contains("EXPLOSION") || causeUpper.contains("EXPLODE") || causeUpper.contains("BLAST")) {
            return "<gradient:#9d50bb:#6e48aa>";
        }
        
        // Queda
        if (causeUpper.contains("FALL") || causeUpper.contains("VOID")) {
            return "<gradient:#667eea:#764ba2>";
        }
        
        // Projéteis
        if (causeUpper.contains("PROJECTILE") || causeUpper.contains("ARROW")) {
            return "<gradient:#f09819:#edde5d>";
        }
        
        // Magia
        if (causeUpper.contains("MAGIC") || causeUpper.contains("SPELL") || causeUpper.contains("POISON") || causeUpper.contains("WITHER")) {
            return "<gradient:#7bff00:#a8ff42>";
        }
        
        // Ataques de entidades
        if (causeUpper.contains("ENTITY") || causeUpper.contains("ATTACK") || causeUpper.contains("CONTACT")) {
            return "<gradient:#ff6b6b:#ff8787>";
        }
        
        // Sufixo
        if (causeUpper.contains("SUFFOCATION") || causeUpper.contains("CRAMMING")) {
            return "<gradient:#4a4a4a:#6a6a6a>";
        }
        
        // Raio
        if (causeUpper.contains("LIGHTNING") || causeUpper.contains("THUNDER")) {
            return "<gradient:#ffd700:#ffed4e>";
        }
        
        // Padrão: Cinza elegante
        return "<gradient:#c0c0c0:#e0e0e0>";
    }

    private static void addBlankLineIfNecessary(List<Component> lore) {
        if (!lore.isEmpty()) {
            Component lastLine = lore.get(lore.size() - 1);
            if (!MINI_MESSAGE.serialize(lastLine).trim().isEmpty() &&
                (lastLine.insertion() == null || !lastLine.insertion().startsWith(LORE_MARKER_PREFIX + "BLANK_SEPARATOR"))) {
                lore.add(MINI_MESSAGE.deserialize(" ").insertion(LORE_MARKER_PREFIX + "BLANK_SEPARATOR:" + System.nanoTime()));
            }
        }
    }

    private static Component formatCustomEffect(String effectEntry, FileConfiguration enchantmentsConfig) {
        if (effectEntry == null || effectEntry.isEmpty()) {
            return MINI_MESSAGE.deserialize("<gray>• Efeito Desconhecido</gray>");
        }

        try {
        String normalizedWithLevel = effectEntry.replace("/", ":").toUpperCase(Locale.ROOT);
        String[] parts = normalizedWithLevel.split(":");
        String baseKey = normalizedWithLevel;
        int level = 0;
            
            // Extrai o nível se estiver no final
        if (parts.length > 1) {
            String last = parts[parts.length - 1];
            if (last.matches("\\d+")) {
                level = Integer.parseInt(last);
                baseKey = String.join(":", Arrays.copyOf(parts, parts.length - 1));
            }
        }

        ConfigurationSection customEffectsSection = enchantmentsConfig != null ? enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS") : null;
        ConfigurationSection effectConfig = customEffectsSection != null ? customEffectsSection.getConfigurationSection(baseKey) : null;

        String displayName;
        String loreFormat = null;
            
        if (effectConfig != null) {
            displayName = effectConfig.getString("display-name", baseKey);
            loreFormat = effectConfig.getString("lore-format");
        } else {
                // Fallback para encantamentos não listados - formata o nome de forma legível
                displayName = formatEnchantmentName(baseKey);
        }

        if (loreFormat == null || loreFormat.isEmpty()) {
                // Formato padrão para encantamentos não configurados
                // Usa cores diferentes baseado no prefixo (AE = roxo)
                // Obter ícone variado baseado no nome do efeito
                String icon = getEnchantmentIcon(baseKey);
                if (baseKey.startsWith("AE:")) {
                    loreFormat = "<gradient:#9d50bb:#6e48aa>" + icon + " %display_name% %roman_level%</gradient>";
                } else {
            loreFormat = "<gray>" + icon + " %display_name% %roman_level%</gray>";
                }
        } else {
            // Se já tem formato configurado, também substituir ícones
            String icon = getEnchantmentIcon(baseKey);
            // Substituir o primeiro ✦ ou • encontrado pelo ícone variado
            loreFormat = loreFormat.replaceFirst("✦|•", icon);
        }

        if (displayName == null || displayName.isEmpty()) {
                displayName = formatEnchantmentName(baseKey);
        }

        // Para encantamentos de nível único (level 1), não mostrar o "I"
        String romanLevel = (level > 1) ? StatManager.toRoman(level) : "";
        loreFormat = loreFormat.replace("%display_name%", displayName);
        loreFormat = loreFormat.replace("%roman_level%", romanLevel);
        loreFormat = loreFormat.replace("%level%", String.valueOf(level));
        loreFormat = loreFormat.replaceAll("\\s{2,}", " ").trim();

        if (loreFormat.isEmpty()) {
            loreFormat = "<gray>• " + displayName + (romanLevel.isEmpty() ? "" : " " + romanLevel) + "</gray>";
        }

        return MINI_MESSAGE.deserialize(loreFormat);
        } catch (Exception e) {
            // Em caso de erro, retorna um formato seguro
            return MINI_MESSAGE.deserialize("<gray>• " + effectEntry + "</gray>");
        }
    }
    
    /**
     * Formata o nome de um encantamento de forma legível quando não está na lista
     */
    private static String formatEnchantmentName(String baseKey) {
        if (baseKey == null || baseKey.isEmpty()) {
            return "Desconhecido";
        }
        
        // Remove prefixos comuns (AE:, etc.)
        String name = baseKey;
        if (name.contains(":")) {
            String[] parts = name.split(":");
            if (parts.length > 1) {
                // Pega a última parte (o nome do encantamento)
                name = parts[parts.length - 1];
            }
        }
        
        // Converte de SNAKE_CASE para Title Case
        name = name.replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) formatted.append(" ");
            if (!words[i].isEmpty()) {
                formatted.append(words[i].substring(0, 1).toUpperCase(Locale.ROOT));
                if (words[i].length() > 1) {
                    formatted.append(words[i].substring(1).toLowerCase(Locale.ROOT));
                }
            }
        }
        
        return formatted.toString();
    }

    public static Component formatCustomEffectDisplay(String effectEntry) {
        FileConfiguration enchantmentsConfig = ItemStatsTracker.getInstance().getEnchantmentsConfig();
        return formatCustomEffect(effectEntry, enchantmentsConfig);
    }

    @SuppressWarnings("deprecation")
    public static ItemStack updateLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        
        // Verificar se o item é rastreável antes de processar a lore
        // Se não for rastreável, remover dono e NBTs do plugin se existirem
        if (!StatManager.itemDeveSerRastreado(item)) {
            // Limpar dono se o item não é mais rastreável
            boolean hadOwner = StatManager.getOriginalOwner(item) != null;
            if (hadOwner) {
                StatManager.removeOriginalOwner(item);
            }
            
            // Se tinha dono ou outras tags do plugin, limpar a lore também
            if (hadOwner) {
                // Limpar a lore atual e reaplicar apenas a lore original do item
                List<Component> currentLore = meta.lore();
                if (currentLore != null) {
                    // Remover todas as linhas que contêm marcadores do plugin
                    List<Component> cleanedLore = new ArrayList<>();
                    for (Component line : currentLore) {
                        String lineText = LegacyComponentSerializer.legacySection().serialize(line);
                        // Não incluir linhas com marcadores do plugin
                        if (!lineText.contains(LORE_MARKER_PREFIX) && !lineText.contains("IOwner:")) {
                            cleanedLore.add(line);
                        }
                    }
                    meta.lore(cleanedLore.isEmpty() ? null : cleanedLore);
                    item.setItemMeta(meta);
                }
            }
            
            // Não atualizar lore de itens não rastreáveis
            return item;
        }

        // IMPORTANTE: Aplicar ItemFlags ANTES de ler a lore para evitar duplicidade
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        // Verificar se HIDE_ATTRIBUTES está habilitado no config
        boolean hideAttributes = ItemStatsTracker.getInstance().getConfig().getBoolean("hide-attributes.enabled", true);
        if (hideAttributes) {
            // Preservar modificadores de atributos padrão do item antes de adicionar HIDE_ATTRIBUTES
            // Isso é necessário para que a anvil funcione corretamente
            // A anvil precisa que os modificadores de atributos padrão existam no ItemMeta
            if (meta.getAttributeModifiers() == null || meta.getAttributeModifiers().isEmpty()) {
                // Se não há modificadores, adicionar os padrão do tipo de item para cada slot
                final ItemMeta finalMeta = meta; // Variável final para usar no lambda
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (item.getType().getDefaultAttributeModifiers(slot) != null) {
                        item.getType().getDefaultAttributeModifiers(slot).forEach((attribute, mod) -> {
                            try {
                                finalMeta.addAttributeModifier(attribute, mod);
                            } catch (Exception e) {
                                // Ignorar erros ao adicionar modificadores duplicados
                            }
                        });
                    }
                }
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        } else {
            meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        
        // NÃO remover os AttributeModifiers - isso quebra a anvil
        // meta.setAttributeModifiers(null); // REMOVIDO - causava problema na anvil
        item.setItemMeta(meta); // Aplicar imediatamente para que o HIDE_ENCHANTS tenha efeito
        meta = item.getItemMeta(); // Reler o meta após aplicar os flags

        // Coletar encantamentos do AE e vanilla antes de processar a lore
        List<String> customEffects = StatManager.getCustomEffects(item);
        Set<String> aeEnchantNames = new HashSet<>();
        Set<String> vanillaEnchantNames = new HashSet<>();
        FileConfiguration enchantmentsConfig = ItemStatsTracker.getInstance().getEnchantmentsConfig();
        
        // Coletar encantamentos vanilla do item
        if (meta.hasEnchants()) {
            for (Enchantment enchant : meta.getEnchants().keySet()) {
                String enchantKey = enchant.getKey().getKey().toUpperCase(Locale.ROOT);
                if (enchantmentsConfig != null) {
                    String displayName = enchantmentsConfig.getString("ENCHANTMENTS." + enchantKey + ".display-name");
                    if (displayName != null) {
                        vanillaEnchantNames.add(displayName.toLowerCase(Locale.ROOT));
                    }
                }
                // Adicionar também o nome em inglês para capturar duplicatas
                String englishName = capitalizeFirstLetter(enchant.getKey().getKey().replace("_", " "));
                vanillaEnchantNames.add(englishName.toLowerCase(Locale.ROOT));
                vanillaEnchantNames.add(enchant.getKey().getKey().toLowerCase(Locale.ROOT));
            }
        }
        
        // Coletar encantamentos AE dos customEffects
        if (!customEffects.isEmpty() && enchantmentsConfig != null) {
            for (String effectEntry : customEffects) {
                String normalized = effectEntry.replace("/", ":").toUpperCase(Locale.ROOT);
                String[] parts = normalized.split(":");
                if (parts.length > 0 && "AE".equals(parts[0]) && parts.length > 1) {
                    String enchantKey = parts[1];
                    String baseKey = "AE:" + enchantKey;
                    ConfigurationSection effectConfig = enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS." + baseKey);
                    if (effectConfig != null) {
                        String displayName = effectConfig.getString("display-name", enchantKey);
                        aeEnchantNames.add(displayName.toLowerCase(Locale.ROOT));
                        aeEnchantNames.add(enchantKey.toLowerCase(Locale.ROOT));
                    } else {
                        // Mesmo sem config, adiciona para filtrar duplicatas
                        aeEnchantNames.add(enchantKey.toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        
        // Também coletar todos os encantamentos AE conhecidos do config para filtrar duplicatas
        // Isso ajuda quando um encantamento AE foi adicionado mas ainda não está na lista de customEffects
        if (enchantmentsConfig != null) {
            ConfigurationSection customEffectsSection = enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS");
            if (customEffectsSection != null) {
                for (String key : customEffectsSection.getKeys(false)) {
                    if (key.startsWith("AE:")) {
                        ConfigurationSection effectConfig = customEffectsSection.getConfigurationSection(key);
                        if (effectConfig != null) {
                            String displayName = effectConfig.getString("display-name");
                            if (displayName != null) {
                                aeEnchantNames.add(displayName.toLowerCase(Locale.ROOT));
                            }
                            String enchantKey = key.substring(3); // Remove "AE:"
                            aeEnchantNames.add(enchantKey.toLowerCase(Locale.ROOT));
                        }
                    }
                }
            }
        }
        
        List<Component> existingLore = meta.lore();
        List<Component> cleanedLore = new ArrayList<>();
        if (existingLore != null) {
            for (Component line : existingLore) {
                if (line == null) continue;

                String insertion = line.insertion();
                String plainText = PlainTextComponentSerializer.plainText().serialize(line).trim();
                boolean isPluginGenerated = insertion != null && (
                    insertion.startsWith(LORE_MARKER_PREFIX) ||
                    insertion.startsWith(EFFECT_LORE_MARKER_PREFIX) ||
                    insertion.startsWith(ARMOR_SET_LORE_MARKER_PREFIX) ||
                    insertion.startsWith("IOwner:")
                );
                
                if (isPluginGenerated) {
                    continue;
                }

                if (plainText.isEmpty()) {
                    continue;
                }

                if (isLikelyPluginLoreLine(plainText)) {
                    continue;
                }
                
                String plainTextLower = plainText.toLowerCase(Locale.ROOT);
                String textWithoutFormatting = plainTextLower.replaceAll("§[0-9a-fk-or]", "").trim();
                
                // Filtrar encantamentos vanilla que aparecem no topo do item
                if (!vanillaEnchantNames.isEmpty()) {
                    boolean isVanillaEnchant = false;
                    for (String vanillaName : vanillaEnchantNames) {
                        String normalizedLine = textWithoutFormatting.replaceAll("[^a-z0-9]", "");
                        String normalizedName = vanillaName.replaceAll("[^a-z0-9]", "");
                        
                        if (normalizedLine.contains(normalizedName)) {
                            String escapedName = vanillaName.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
                            if (textWithoutFormatting.matches(".*" + escapedName + ".*[ivxlcdm1-9].*") ||
                                textWithoutFormatting.matches(".*" + escapedName + "\\s+[ivxlcdm]+.*") ||
                                textWithoutFormatting.matches(".*" + escapedName + "\\s+\\d+.*") ||
                                textWithoutFormatting.startsWith(vanillaName + " ") ||
                                textWithoutFormatting.equals(vanillaName) ||
                                normalizedLine.startsWith(normalizedName) ||
                                textWithoutFormatting.endsWith(" " + vanillaName)) {
                                isVanillaEnchant = true;
                                break;
                            }
                        }
                    }
                    
                    if (isVanillaEnchant) {
                        continue;
                    }
                }
                
                // Filtrar encantamentos do AE que aparecem no topo do item
                if (!aeEnchantNames.isEmpty()) {
                    boolean isAeEnchant = false;
                    
                    for (String aeName : aeEnchantNames) {
                        String normalizedLine = textWithoutFormatting.replaceAll("[^a-z0-9]", "");
                        String normalizedName = aeName.replaceAll("[^a-z0-9]", "");
                        
                        if (normalizedLine.contains(normalizedName)) {
                            String escapedName = aeName.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
                            if (textWithoutFormatting.matches(".*" + escapedName + ".*[ivxlcdm1-9].*") ||
                                textWithoutFormatting.matches(".*" + escapedName + "\\s+[ivxlcdm]+.*") ||
                                textWithoutFormatting.matches(".*" + escapedName + "\\s+\\d+.*") ||
                                textWithoutFormatting.startsWith(aeName + " ") ||
                                textWithoutFormatting.equals(aeName) ||
                                normalizedLine.startsWith(normalizedName) ||
                                textWithoutFormatting.endsWith(" " + aeName) ||
                                normalizedLine.contains(normalizedName)) {
                                isAeEnchant = true;
                                break;
                            }
                        }
                    }
                    
                    if (isAeEnchant) {
                        continue;
                    }
                }

                if (!MINI_MESSAGE.serialize(line).trim().isEmpty()) {
                    cleanedLore.add(line);
                }
            }
        }

        FileConfiguration config = ItemStatsTracker.getInstance().getConfig();
        Set<Enchantment> processedEnchantments = new HashSet<>();
        List<Component> enchantmentProgressLore = new ArrayList<>();
        
        // Lista para encantamentos vanilla
        List<Component> vanillaEnchantments = new ArrayList<>();

        String itemCategory = StatManager.getItemCategory(item.getType());
        ConfigurationSection categoryUpgrades = config.getConfigurationSection("enchantment-upgrades." + itemCategory);
        if (categoryUpgrades == null) {
            categoryUpgrades = config.getConfigurationSection("enchantment-upgrades.default");
        }

        if (categoryUpgrades != null) {
            for (String enchantKey : categoryUpgrades.getKeys(false)) {
                // Verificar se é um encantamento mágico (tem ":") ou vanilla
                boolean isCustomEnchant = enchantKey.contains(":");
                Enchantment enchantment = null;
                int currentLevel = 0;
                
                if (isCustomEnchant) {
                    // Encantamento mágico/customizado (AE:ENCHANT, etc.)
                    // Obter nível atual do PDC
                    String normalizedKey = StatManager.normalizeCustomEffect(enchantKey);
                    List<String> itemCustomEffects = StatManager.getCustomEffects(item);
                    
                    for (String effect : itemCustomEffects) {
                        String normalizedEffect = StatManager.normalizeCustomEffect(effect);
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
                } else {
                    // Encantamento vanilla
                    try {
                        Enchantment vanillaEnchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey.toLowerCase(Locale.ROOT)));
                        enchantment = vanillaEnchant;
                        if (enchantment != null) {
                            processedEnchantments.add(enchantment);
                            currentLevel = item.getEnchantmentLevel(enchantment);
                        }
                    } catch (IllegalArgumentException e) {
                        // Chave inválida, pular
                        continue;
                    }
                }
                
                // Se é vanilla e não tem enchantment, pular
                if (!isCustomEnchant && enchantment == null) {
                    continue;
                }

                int maxLevel = categoryUpgrades.getInt(enchantKey + ".max-level", currentLevel);

                // Para encantamentos vanilla, mostrar na seção de efeitos
                if (!isCustomEnchant && currentLevel > 0 && enchantment != null) {
                    String enchantName = capitalizeFirstLetter(enchantment.getKey().getKey().replace("_", " "));
                    String loreFormat = enchantmentsConfig != null ? enchantmentsConfig.getString("ENCHANTMENTS." + enchantKey.toUpperCase(Locale.ROOT) + ".lore-format") : null;
                    Component enchantComponent;
                    if (loreFormat != null && !loreFormat.isEmpty() && enchantmentsConfig != null) {
                        String displayName = enchantmentsConfig.getString("ENCHANTMENTS." + enchantKey.toUpperCase(Locale.ROOT) + ".display-name", enchantName);
                        // Para encantamentos de nível único (level 1), não mostrar o "I"
                        String romanLevel = (currentLevel > 1) ? StatManager.toRoman(currentLevel) : "";
                        String formattedEnchant = loreFormat.replace("%roman_level%", romanLevel).replace("%display_name%", displayName);
                        enchantComponent = MINI_MESSAGE.deserialize(formattedEnchant).insertion(LORE_MARKER_PREFIX + "ENCHANTMENT:" + enchantment.getKey().getKey());
                    } else {
                        // Para encantamentos de nível único (level 1), não mostrar o "I"
                        String romanLevel = (currentLevel > 1) ? StatManager.toRoman(currentLevel) : "";
                        String levelText = romanLevel.isEmpty() ? "" : " " + romanLevel;
                        enchantComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(String.format("&b%s%s", enchantName, levelText)).insertion(LORE_MARKER_PREFIX + "ENCHANTMENT:" + enchantment.getKey().getKey());
                    }
                    vanillaEnchantments.add(enchantComponent);
                }

                // Mostrar progresso se o encantamento ainda não atingiu o nível máximo
                // OU se o encantamento ainda não está no item (currentLevel == 0) mas está configurado
                if (currentLevel < maxLevel) {
                    List<StatManager.EnchantmentUpgradeProgress> progressList;
                    if (isCustomEnchant) {
                        progressList = StatManager.getEnchantmentUpgradeProgress(item, enchantKey);
                    } else {
                        progressList = StatManager.getEnchantmentUpgradeProgress(item, enchantment);
                    }
                    
                    if (!progressList.isEmpty()) {
                        double totalProgress = progressList.stream().mapToDouble(StatManager.EnchantmentUpgradeProgress::getProgress).sum();
                        double averageProgress = totalProgress / progressList.size();
                        
                        // Usar a mesma formatação visual do encantamento
                        Component formattedEnchantName;
                        String displayName = null;
                        
                        if (isCustomEnchant) {
                            // Buscar no CUSTOM_EFFECTS do enchantments.yml
                            ConfigurationSection customEffectsSection = enchantmentsConfig != null 
                                ? enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS") : null;
                            String normalizedKey = StatManager.normalizeCustomEffect(enchantKey);
                            ConfigurationSection effectConfig = customEffectsSection != null 
                                ? customEffectsSection.getConfigurationSection(normalizedKey) : null;
                            displayName = effectConfig != null 
                                ? effectConfig.getString("display-name", enchantKey)
                                : enchantKey;
                        } else {
                            if (enchantmentsConfig != null) {
                                displayName = enchantmentsConfig.getString("ENCHANTMENTS." + enchantKey.toUpperCase(Locale.ROOT) + ".display-name");
                            }
                            if ((displayName == null || displayName.isEmpty()) && enchantment != null) {
                                displayName = capitalizeFirstLetter(enchantment.getKey().getKey().replace("_", " "));
                            }
                        }
                        
                        if (enchantmentsConfig != null) {
                            String loreFormat;
                            if (isCustomEnchant) {
                                ConfigurationSection customEffectsSection = enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS");
                                String normalizedKey = StatManager.normalizeCustomEffect(enchantKey);
                                ConfigurationSection effectConfig = customEffectsSection != null 
                                    ? customEffectsSection.getConfigurationSection(normalizedKey) : null;
                                loreFormat = effectConfig != null 
                                    ? effectConfig.getString("lore-format") : null;
                            } else {
                                loreFormat = enchantment != null ? enchantmentsConfig.getString("ENCHANTMENTS." + enchantKey.toUpperCase(Locale.ROOT) + ".lore-format") : null;
                            }
                            
                            if (loreFormat != null && !loreFormat.isEmpty()) {
                                // Remover o nível romano do formato e aplicar apenas ao nome
                                String nameOnlyFormat = loreFormat.replace("%display_name%", displayName);
                                nameOnlyFormat = nameOnlyFormat.replace("%roman_level%", "").trim();
                                nameOnlyFormat = nameOnlyFormat.replaceAll("\\s+", " ").replace(": :", ":").replace(" :", ":").trim();
                                nameOnlyFormat = nameOnlyFormat.replaceAll("[:\\s]+$", "").trim();
                                formattedEnchantName = MINI_MESSAGE.deserialize(nameOnlyFormat);
                            } else {
                                formattedEnchantName = LegacyComponentSerializer.legacyAmpersand().deserialize("&b" + displayName);
                            }
                        } else {
                            formattedEnchantName = LegacyComponentSerializer.legacyAmpersand().deserialize("&b" + displayName);
                        }
                        
                        String progressBar = StatManager.createProgressBar(averageProgress, 15);
                        
                        // Criar mensagem customizada com o nome formatado
                        String progressKey = isCustomEnchant ? enchantKey : (enchantment != null ? enchantment.getKey().getKey() : enchantKey);
                        Component progressMessage = formattedEnchantName
                            .append(Component.text(": ", NamedTextColor.GRAY))
                            .append(MiniMessage.miniMessage().deserialize(progressBar))
                            .append(Component.text(" " + String.format("%.0f", averageProgress * 100) + "%", NamedTextColor.GRAY));
                        
                        enchantmentProgressLore.add(progressMessage.insertion(LORE_MARKER_PREFIX + "ENCHANTMENT_PROGRESS:" + progressKey));
                    }
                } else if (currentLevel == 0 && maxLevel > 0) {
                    // Se o encantamento ainda não está no item mas está configurado, mostrar progresso para ganhar nível 1
                    List<StatManager.EnchantmentUpgradeProgress> progressList;
                    if (isCustomEnchant) {
                        progressList = StatManager.getEnchantmentUpgradeProgress(item, enchantKey);
                    } else {
                        progressList = StatManager.getEnchantmentUpgradeProgress(item, enchantment);
                    }
                    
                    if (!progressList.isEmpty()) {
                        double totalProgress = progressList.stream().mapToDouble(StatManager.EnchantmentUpgradeProgress::getProgress).sum();
                        double averageProgress = totalProgress / progressList.size();
                        
                        // Usar a mesma formatação visual do encantamento
                        Component formattedEnchantName;
                        String displayName = null;
                        
                        if (isCustomEnchant) {
                            // Buscar no CUSTOM_EFFECTS do enchantments.yml
                            ConfigurationSection customEffectsSection = enchantmentsConfig != null 
                                ? enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS") : null;
                            String normalizedKey = StatManager.normalizeCustomEffect(enchantKey);
                            ConfigurationSection effectConfig = customEffectsSection != null 
                                ? customEffectsSection.getConfigurationSection(normalizedKey) : null;
                            displayName = effectConfig != null 
                                ? effectConfig.getString("display-name", enchantKey)
                                : enchantKey;
                        } else {
                            if (enchantmentsConfig != null) {
                                displayName = enchantmentsConfig.getString("ENCHANTMENTS." + enchantKey.toUpperCase(Locale.ROOT) + ".display-name");
                            }
                            if ((displayName == null || displayName.isEmpty()) && enchantment != null) {
                                displayName = capitalizeFirstLetter(enchantment.getKey().getKey().replace("_", " "));
                            }
                        }
                        
                        if (enchantmentsConfig != null) {
                            String loreFormat;
                            if (isCustomEnchant) {
                                ConfigurationSection customEffectsSection = enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS");
                                String normalizedKey = StatManager.normalizeCustomEffect(enchantKey);
                                ConfigurationSection effectConfig = customEffectsSection != null 
                                    ? customEffectsSection.getConfigurationSection(normalizedKey) : null;
                                loreFormat = effectConfig != null 
                                    ? effectConfig.getString("lore-format") : null;
                            } else {
                                loreFormat = enchantment != null ? enchantmentsConfig.getString("ENCHANTMENTS." + enchantKey.toUpperCase(Locale.ROOT) + ".lore-format") : null;
                            }
                            
                            if (loreFormat != null && !loreFormat.isEmpty()) {
                                String nameOnlyFormat = loreFormat.replace("%display_name%", displayName);
                                nameOnlyFormat = nameOnlyFormat.replace("%roman_level%", "").trim();
                                nameOnlyFormat = nameOnlyFormat.replaceAll("\\s+", " ").replace(": :", ":").replace(" :", ":").trim();
                                nameOnlyFormat = nameOnlyFormat.replaceAll("[:\\s]+$", "").trim();
                                formattedEnchantName = MINI_MESSAGE.deserialize(nameOnlyFormat);
                            } else {
                                formattedEnchantName = LegacyComponentSerializer.legacyAmpersand().deserialize("&b" + displayName);
                            }
                        } else {
                            formattedEnchantName = LegacyComponentSerializer.legacyAmpersand().deserialize("&b" + displayName);
                        }
                        
                        String progressBar = StatManager.createProgressBar(averageProgress, 15);
                        
                        String progressKey = isCustomEnchant ? enchantKey : (enchantment != null ? enchantment.getKey().getKey() : enchantKey);
                        Component progressMessage = formattedEnchantName
                            .append(Component.text(": ", NamedTextColor.GRAY))
                            .append(MiniMessage.miniMessage().deserialize(progressBar))
                            .append(Component.text(" " + String.format("%.0f", averageProgress * 100) + "%", NamedTextColor.GRAY));
                        
                        enchantmentProgressLore.add(progressMessage.insertion(LORE_MARKER_PREFIX + "ENCHANTMENT_PROGRESS:" + progressKey));
                    }
                }
            }
        }

        if (meta.hasEnchants()) {
            meta.getEnchants().forEach((enchantment, level) -> {
                if (!processedEnchantments.contains(enchantment)) {
                    String enchantKey = enchantment.getKey().getKey().toUpperCase(Locale.ROOT);
                    String enchantName = capitalizeFirstLetter(enchantment.getKey().getKey().replace("_", " "));
                    String loreFormat = enchantmentsConfig != null ? enchantmentsConfig.getString("ENCHANTMENTS." + enchantKey + ".lore-format") : null;
                    Component enchantComponent;
                    if (loreFormat != null && !loreFormat.isEmpty() && enchantmentsConfig != null) {
                        String displayName = enchantmentsConfig.getString("ENCHANTMENTS." + enchantKey + ".display-name", enchantName);
                        // Para encantamentos de nível único (level 1), não mostrar o "I"
                        String romanLevel = (level > 1) ? StatManager.toRoman(level) : "";
                        // Obter ícone variado baseado no nome do encantamento
                        String icon = getEnchantmentIcon(enchantKey);
                        String formattedEnchant = loreFormat.replace("%roman_level%", romanLevel).replace("%display_name%", displayName);
                        // Substituir o ícone padrão (✦) pelo ícone variado
                        formattedEnchant = formattedEnchant.replaceFirst("✦", icon);
                        enchantComponent = MINI_MESSAGE.deserialize(formattedEnchant).insertion(LORE_MARKER_PREFIX + "ENCHANTMENT:" + enchantment.getKey().getKey());
                    } else {
                        // Fallback para formato padrão se não houver configuração
                        // Para encantamentos de nível único (level 1), não mostrar o "I"
                        String romanLevel = (level > 1) ? StatManager.toRoman(level) : "";
                        String levelText = romanLevel.isEmpty() ? "" : " " + romanLevel;
                        // Obter ícone variado baseado no nome do encantamento
                        String icon = getEnchantmentIcon(enchantKey);
                        enchantComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(String.format("&b%s %s%s", icon, enchantName, levelText)).insertion(LORE_MARKER_PREFIX + "ENCHANTMENT:" + enchantment.getKey().getKey());
                    }
                    vanillaEnchantments.add(enchantComponent);
                }
            });
        }
        
        // Determinar tipo de item para uso em múltiplos lugares
        // itemCategory já foi declarado anteriormente na linha 403
        boolean isWeapon = "SWORDS".equals(itemCategory) 
            || item.getType().name().endsWith("_SWORD") 
            || item.getType() == Material.BOW 
            || item.getType() == Material.CROSSBOW 
            || item.getType() == Material.TRIDENT
            || item.getType().name().endsWith("_AXE") // Machados usados como arma
            || item.getType() == Material.MACE; // Mace (arma)
        
        boolean isTool = "PICKAXES".equals(itemCategory)
            || item.getType().name().endsWith("_PICKAXE")
            || item.getType().name().endsWith("_SHOVEL")
            || item.getType().name().endsWith("_HOE");
        
        boolean isArmor = "ARMOR_PIECES".equals(itemCategory) || "CHESTPLATE".equals(itemCategory);
        
        // Estatísticas serão adicionadas DEPOIS do Dono - armazenar temporariamente
        List<Component> statsLore = new ArrayList<>();
        if (config.getBoolean("display-tracked-stats.enabled", true)) {
            boolean addedStatsHeader = false;
            List<String> statsToDisplay = new ArrayList<>();
            
            // Para armas: mostrar estatísticas específicas de cada tipo
            if (isWeapon) {
                if (item.getType() == Material.BOW) {
                    // Arcos: Alvos na Mira e Dano Total
                    statsToDisplay = List.of("BOW_ARROWS_SHOT", "DAMAGE_DEALT");
                } else if (item.getType() == Material.CROSSBOW) {
                    // Bestas: Alvos na Mira e Dano Total
                    statsToDisplay = List.of("BOW_ARROWS_SHOT", "DAMAGE_DEALT");
                } else if (item.getType() == Material.TRIDENT) {
                    // Tridentes: Lançamentos e Dano com Tridente
                    statsToDisplay = List.of("TRIDENT_THROWN", "TRIDENT_DAMAGE");
                } else if (item.getType() == Material.MACE) {
                    // Maces: Altura Máxima e Maior Dano Aplicado
                    statsToDisplay = List.of("MACE_FALL_HEIGHT", "MACE_MAX_DAMAGE");
                } else {
                    // Outras armas: Mobs Abatidos, Players Mortos e Dano Total
                    statsToDisplay = List.of("MOB_KILLS", "PLAYER_KILLS", "DAMAGE_DEALT");
                }
            } 
            // Para ferramentas: mostrar estatísticas específicas de cada tipo
            else if (isTool) {
                if (item.getType().name().endsWith("_PICKAXE")) {
                    // Picaretas: Minérios Quebrados e Blocos Totais (soma de ORES_BROKEN + BLOCKS_BROKEN)
                    statsToDisplay = List.of("ORES_BROKEN", "BLOCKS_TOTAL");
                } else if (item.getType().name().endsWith("_HOE")) {
                    // Enxadas: Plantações Colhidas e Terras Aradas
                    statsToDisplay = List.of("FARM_HARVESTED", "HOE_SOIL_TILLED");
                } else if (item.getType().name().endsWith("_AXE")) {
                    // Machados (lenhador): Lenha Coletada
                    statsToDisplay = List.of("WOOD_CHOPPED");
                } else if (item.getType().name().endsWith("_SHOVEL")) {
                    // Pás: Blocos Excavados
                    statsToDisplay = List.of("BLOCKS_BROKEN");
                }
            }
            // Para armaduras: Dano Recebido
            else if (isArmor) {
                statsToDisplay = List.of("DAMAGE_TAKEN");
            }
            // Para elitros: Tempo de Voo
            else if (item.getType() == Material.ELYTRA) {
                statsToDisplay = List.of("ELYTRA_FLIGHT_TIME");
            }
            // Para escudos: Dano Suportado
            else if (item.getType() == Material.SHIELD) {
                statsToDisplay = List.of("DAMAGE_BLOCKED");
            }
            
            for (String statKey : statsToDisplay) {
                int statValue;
                String statDisplayName;
                
                // Para picaretas, calcular Blocos Totais como soma de ORES_BROKEN + BLOCKS_BROKEN
                if ("BLOCKS_TOTAL".equals(statKey) && item.getType().name().endsWith("_PICKAXE")) {
                    int oresBroken = StatManager.getStat(item, "ORES_BROKEN");
                    int blocksBroken = StatManager.getStat(item, "BLOCKS_BROKEN");
                    statValue = oresBroken + blocksBroken;
                    statDisplayName = LanguageManager.getRawString("stats.blocks_total");
                } else {
                    statValue = StatManager.getStat(item, statKey);
                    statDisplayName = LanguageManager.getRawString("stats." + statKey.toLowerCase(Locale.ROOT));
                }
                
                if (statValue > 0) {
                    if (!addedStatsHeader) {
                        addBlankLineIfNecessary(statsLore);
                        statsLore.add(LanguageManager.getMessage("lore.stats-header").insertion(LORE_MARKER_PREFIX + "STATS_HEADER"));
                        addedStatsHeader = true;
                    }
                    // Formatação especial para algumas estatísticas
                    String formattedValue;
                    if ("ELYTRA_FLIGHT_TIME".equals(statKey)) {
                        // Tempo de voo em segundos
                        formattedValue = String.format("%.1fs", statValue / 20.0);
                    } else if ("MACE_FALL_HEIGHT".equals(statKey)) {
                        // Altura em blocos
                        formattedValue = statValue + " blocos";
                    } else {
                        formattedValue = StatManager.formatNumber(statValue);
                    }
                    // Formatação bonita com gradiente cinza
                    String formattedStatLine = "<gradient:#a0a0a0:#d0d0d0>" + statDisplayName + ": <white>" + formattedValue + "</white></gradient>";
                    statsLore.add(MINI_MESSAGE.deserialize(formattedStatLine).insertion(LORE_MARKER_PREFIX + "STAT:" + statKey));
                }
            }
        }
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey resKey = new NamespacedKey(ItemStatsTracker.getInstance(), "resistance_bonus");
        // Badge de Item Mestre - verificar se o item atingiu o nível mestre
        int reincarnadoLevelForResistance = StatManager.getReincarnadoLevel(item);
        FileConfiguration mestreConfig = ItemStatsTracker.getInstance().getItemMestreConfig();
        int mestreLevel = mestreConfig != null ? mestreConfig.getInt("item_mestre.level", 1000) : 1000;
        NamespacedKey mestreKey = new NamespacedKey(ItemStatsTracker.getInstance(), "is_item_mestre");
        
        // Se o item atingiu o nível mestre mas não está marcado, marcar agora
        if (reincarnadoLevelForResistance >= mestreLevel && !pdc.getOrDefault(mestreKey, PersistentDataType.BOOLEAN, false)) {
            pdc.set(mestreKey, PersistentDataType.BOOLEAN, true);
            // O PDC é uma referência, então as mudanças já são aplicadas ao meta
        }
        
        // Exibir badge se for item mestre
        if (pdc.getOrDefault(mestreKey, PersistentDataType.BOOLEAN, false)) {
            addBlankLineIfNecessary(cleanedLore);
            cleanedLore.add(LanguageManager.getMessage("lore.mestre-badge").insertion(LORE_MARKER_PREFIX + "MESTRE_BADGE"));
        }
        // Apenas para armaduras: calcula/atualiza os bônus de resistência com base no nível de ascensão
        // isArmor já foi declarado anteriormente
        if (isArmor && reincarnadoLevelForResistance > 0) {
            FileConfiguration levelEffectsCfg = ItemStatsTracker.getInstance().getLevelEffectsConfig();
            if (levelEffectsCfg != null) {
                ConfigurationSection levelsSec = levelEffectsCfg.getConfigurationSection("LEVEL_EFFECTS");
                if (levelsSec != null) {
                    Map<String, Double> summed = new HashMap<>();
                    for (String levelKey : levelsSec.getKeys(false)) {
                        try {
                            int lvl = Integer.parseInt(levelKey);
                            if (lvl <= reincarnadoLevelForResistance) {
                                ConfigurationSection lvlSec = levelsSec.getConfigurationSection(levelKey);
                                if (lvlSec != null) {
                                    ConfigurationSection resSec = lvlSec.getConfigurationSection("resistance_bonuses");
                                    if (resSec != null) {
                                        for (String cause : resSec.getKeys(false)) {
                                            double val = resSec.getDouble(cause, 0.0);
                                            if (val > 0) {
                                                String causeUpper = cause.toUpperCase(Locale.ROOT);
                                                summed.put(causeUpper, summed.getOrDefault(causeUpper, 0.0) + val);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    // Se o PDC não existe ou está desatualizado em relação à soma esperada, reescreve
                    boolean needsRewrite = true;
                    if (pdc.has(resKey, PersistentDataType.TAG_CONTAINER_ARRAY)) {
                        PersistentDataContainer[] existing = pdc.get(resKey, PersistentDataType.TAG_CONTAINER_ARRAY);
                        Map<String, Double> current = new HashMap<>();
                        if (existing != null) {
                            for (PersistentDataContainer ec : existing) {
                                String cause = ec.get(new NamespacedKey(ItemStatsTracker.getInstance(), "cause"), PersistentDataType.STRING);
                                Double percent = ec.get(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE);
                                if (cause != null && percent != null) current.put(cause.toUpperCase(Locale.ROOT), percent);
                            }
                        }
                        needsRewrite = !current.equals(summed);
                    }
                    if (needsRewrite) {
                        if (summed.isEmpty()) {
                            pdc.remove(resKey);
                        } else {
                            List<PersistentDataContainer> containers = new ArrayList<>();
                            for (Map.Entry<String, Double> e : summed.entrySet()) {
                                PersistentDataContainer bonusContainer = pdc.getAdapterContext().newPersistentDataContainer();
                                bonusContainer.set(new NamespacedKey(ItemStatsTracker.getInstance(), "cause"), PersistentDataType.STRING, e.getKey());
                                bonusContainer.set(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE, e.getValue());
                                containers.add(bonusContainer);
                            }
                            pdc.set(resKey, PersistentDataType.TAG_CONTAINER_ARRAY, containers.toArray(new PersistentDataContainer[0]));
                            // IMPORTANTE: Aplicar o meta ao item após salvar os bônus no PDC
                            item.setItemMeta(meta);
                            // Reler o meta atualizado para garantir que os bônus salvos estejam disponíveis
                            meta = item.getItemMeta();
                            if (meta != null) {
                                pdc = meta.getPersistentDataContainer();
                            }
                        }
                    }
                }
            }
        }
        if (ItemStatsTracker.getInstance().getConfig().getBoolean("display-resistance.enabled", true)
            && pdc.has(resKey, PersistentDataType.TAG_CONTAINER_ARRAY)) {
            PersistentDataContainer[] bonuses = pdc.get(resKey, PersistentDataType.TAG_CONTAINER_ARRAY);
            if (bonuses != null && bonuses.length > 0) {
                // Lista de resistências permitidas para exibição na lore
                Set<String> allowedResistances = Set.of(
                    "FIRE", "LAVA", "BURN", "HOT", // Fogo
                    "PROJECTILE", "ARROW", "BOW", // Projéteis
                    "FALL", "FALLING", "FALL_DAMAGE", // Queda
                    "EXPLOSION", "BLAST", "ENTITY_EXPLOSION", "BLOCK_EXPLOSION", // Explosão
                    "ENTITY_ATTACK", "ATTACK", "MELEE", // Ataque/PvE
                    "PLAYER", "PVP" // Ataque/PvP
                );
                
                Map<String, Double> filteredResistances = new LinkedHashMap<>();
                for (PersistentDataContainer bonusContainer : bonuses) {
                    String cause = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "cause"), PersistentDataType.STRING);
                    Double percent = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE);
                    if (cause != null && percent != null && percent > 0) {
                        String causeUpper = cause.toUpperCase(Locale.ROOT);
                        // Verificar se a causa corresponde a alguma das resistências permitidas
                        boolean isAllowed = allowedResistances.stream().anyMatch(causeUpper::contains);
                        if (isAllowed) {
                            // Agrupar por tipo: Fogo, Projéteis, Queda, Explosão, Ataque/PvE, Ataque/PvP
                            // IMPORTANTE: Verificar PvP ANTES de PvE, pois "PLAYER" pode estar contido em outras strings
                            String category = "";
                            if (causeUpper.contains("PLAYER") || causeUpper.contains("PVP")) {
                                category = "PVP";
                            } else if (causeUpper.contains("FIRE") || causeUpper.contains("LAVA") || causeUpper.contains("BURN") || causeUpper.contains("HOT")) {
                                category = "FIRE";
                            } else if (causeUpper.contains("PROJECTILE") || causeUpper.contains("ARROW") || causeUpper.contains("BOW")) {
                                category = "PROJECTILE";
                            } else if (causeUpper.contains("FALL") || causeUpper.contains("FALLING")) {
                                category = "FALL";
                            } else if (causeUpper.contains("EXPLOSION") || causeUpper.contains("BLAST")) {
                                category = "EXPLOSION";
                            } else if (causeUpper.contains("ENTITY_ATTACK") || causeUpper.contains("ATTACK") || causeUpper.contains("MELEE")) {
                                category = "PVE";
                            }
                            
                            if (!category.isEmpty()) {
                                // Somar os valores se já existir uma resistência da mesma categoria
                                filteredResistances.put(category, filteredResistances.getOrDefault(category, 0.0) + percent);
                            }
                        }
                    }
                }
                
                if (!filteredResistances.isEmpty()) {
                    addBlankLineIfNecessary(cleanedLore);
                    cleanedLore.add(LanguageManager.getMessage("lore.resistance-header").insertion(LORE_MARKER_PREFIX + "RESISTANCE_HEADER"));
                    
                    // Exibir as resistências agrupadas com nomes bonitos
                    // Ordem de exibição: Fogo, Projéteis, Queda, Explosão, Ataque/PvE, Ataque/PvP
                    String[] displayOrder = {"FIRE", "PROJECTILE", "FALL", "EXPLOSION", "PVE", "PVP"};
                    for (String category : displayOrder) {
                        Double percent = filteredResistances.get(category);
                        if (percent == null || percent <= 0) {
                            continue;
                        }
                        
                        String displayName;
                        String colorGradient;
                        
                        switch (category) {
                            case "FIRE":
                                displayName = "Fogo";
                                colorGradient = getResistanceColor("FIRE");
                                break;
                            case "PROJECTILE":
                                displayName = "Projéteis";
                                colorGradient = getResistanceColor("PROJECTILE");
                                break;
                            case "FALL":
                                displayName = "Queda";
                                colorGradient = getResistanceColor("FALL");
                                break;
                            case "EXPLOSION":
                                displayName = "Explosão";
                                colorGradient = getResistanceColor("EXPLOSION");
                                break;
                            case "PVE":
                                displayName = "Ataque [PvE]";
                                colorGradient = getResistanceColor("ENTITY_ATTACK");
                                break;
                            case "PVP":
                                displayName = "Ataque [PvP]";
                                colorGradient = getResistanceColor("PLAYER");
                                break;
                            default:
                                displayName = category;
                                colorGradient = "<gradient:#c0c0c0:#e0e0e0>";
                        }
                        
                        String formattedLine = colorGradient + "Resistência a " + displayName + ": <#00F66A>+" + String.format("%.1f", percent) + "%</#00F66A></gradient>";
                        cleanedLore.add(MINI_MESSAGE.deserialize(formattedLine).insertion(LORE_MARKER_PREFIX + "RESISTANCE:" + category));
                    }
                }
            }
        }
        
        // Sistema de Attack % para armas (similar ao Protection % das armaduras)
        NamespacedKey attackKey = new NamespacedKey(ItemStatsTracker.getInstance(), "attack_bonus");
        // isWeapon já foi declarado anteriormente na linha 569
        // Usar o mesmo nível de reincarnação para armas
        int reincarnadoLevelForAttack = StatManager.getReincarnadoLevel(item);
        if (isWeapon && reincarnadoLevelForAttack > 0) {
            FileConfiguration levelEffectsCfg = ItemStatsTracker.getInstance().getLevelEffectsConfig();
            if (levelEffectsCfg != null) {
                ConfigurationSection levelsSec = levelEffectsCfg.getConfigurationSection("LEVEL_EFFECTS");
                if (levelsSec != null) {
                    Map<String, Double> summedAttack = new HashMap<>();
                    for (String levelKey : levelsSec.getKeys(false)) {
                        try {
                            int lvl = Integer.parseInt(levelKey);
                            if (lvl <= reincarnadoLevelForAttack) {
                                ConfigurationSection lvlSec = levelsSec.getConfigurationSection(levelKey);
                                if (lvlSec != null) {
                                    ConfigurationSection attackSec = lvlSec.getConfigurationSection("attack_bonuses");
                                    if (attackSec != null) {
                                        for (String targetType : attackSec.getKeys(false)) {
                                            double val = attackSec.getDouble(targetType, 0.0);
                                            if (val > 0) {
                                                String targetUpper = targetType.toUpperCase(Locale.ROOT);
                                                summedAttack.put(targetUpper, summedAttack.getOrDefault(targetUpper, 0.0) + val);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    // Se o PDC não existe ou está desatualizado em relação à soma esperada, reescreve
                    boolean needsRewrite = true;
                    if (pdc.has(attackKey, PersistentDataType.TAG_CONTAINER_ARRAY)) {
                        PersistentDataContainer[] existing = pdc.get(attackKey, PersistentDataType.TAG_CONTAINER_ARRAY);
                        Map<String, Double> current = new HashMap<>();
                        if (existing != null) {
                            for (PersistentDataContainer ec : existing) {
                                String targetType = ec.get(new NamespacedKey(ItemStatsTracker.getInstance(), "target_type"), PersistentDataType.STRING);
                                Double percent = ec.get(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE);
                                if (targetType != null && percent != null) current.put(targetType.toUpperCase(Locale.ROOT), percent);
                            }
                        }
                        needsRewrite = !current.equals(summedAttack);
                    }
                    if (needsRewrite) {
                        if (summedAttack.isEmpty()) {
                            pdc.remove(attackKey);
                        } else {
                            List<PersistentDataContainer> containers = new ArrayList<>();
                            for (Map.Entry<String, Double> e : summedAttack.entrySet()) {
                                PersistentDataContainer bonusContainer = pdc.getAdapterContext().newPersistentDataContainer();
                                bonusContainer.set(new NamespacedKey(ItemStatsTracker.getInstance(), "target_type"), PersistentDataType.STRING, e.getKey());
                                bonusContainer.set(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE, e.getValue());
                                containers.add(bonusContainer);
                            }
                            pdc.set(attackKey, PersistentDataType.TAG_CONTAINER_ARRAY, containers.toArray(new PersistentDataContainer[0]));
                            // IMPORTANTE: Aplicar o meta ao item após salvar os bônus no PDC
                            item.setItemMeta(meta);
                            // Reler o meta atualizado
                            meta = item.getItemMeta();
                            if (meta != null) {
                                pdc = meta.getPersistentDataContainer();
                            }
                        }
                    }
                    
                    // Exibir bônus de ataque para armas - usar summedAttack diretamente se houver dados
                    if (ItemStatsTracker.getInstance().getConfig().getBoolean("display-attack.enabled", true) && !summedAttack.isEmpty()) {
                        addBlankLineIfNecessary(cleanedLore);
                        cleanedLore.add(LanguageManager.getMessage("lore.attack-header").insertion(LORE_MARKER_PREFIX + "ATTACK_HEADER"));
                        // Agrupar e ordenar os ataques: PvP primeiro, depois PvE, depois outros
                        Map<String, Double> groupedAttack = new LinkedHashMap<>();
                        for (Map.Entry<String, Double> e : summedAttack.entrySet()) {
                            String targetType = e.getKey();
                            Double percent = e.getValue();
                            if (targetType != null && percent != null && percent > 0) {
                                String targetUpper = targetType.toUpperCase(Locale.ROOT);
                                // Agrupar PvP
                                if (targetUpper.contains("PLAYER") || targetUpper.contains("PVP")) {
                                    groupedAttack.put("PVP", groupedAttack.getOrDefault("PVP", 0.0) + percent);
                                } else {
                                    // Outros tipos (MOB, UNDEAD, etc.) como PvE
                                    groupedAttack.put("PVE", groupedAttack.getOrDefault("PVE", 0.0) + percent);
                                }
                            }
                        }
                        
                        // Exibir na ordem: PvP primeiro, depois PvE
                        String[] attackOrder = {"PVP", "PVE"};
                        for (String category : attackOrder) {
                            Double percent = groupedAttack.get(category);
                            if (percent != null && percent > 0) {
                                String displayName;
                                String colorGradient;
                                
                                if ("PVP".equals(category)) {
                                    displayName = "Ataque [PvP]";
                                    colorGradient = getAttackColor("PLAYER");
                                } else {
                                    displayName = "Ataque [PvE]";
                                    colorGradient = getAttackColor("MOB");
                                }
                                
                                String formattedLine = colorGradient + displayName + ": <#00F66A>+" + String.format("%.1f", percent) + "%</#00F66A></gradient>";
                                cleanedLore.add(MINI_MESSAGE.deserialize(formattedLine).insertion(LORE_MARKER_PREFIX + "ATTACK:" + category));
                            }
                        }
                    }
                }
            }
        } else if (isWeapon) {
            // Se for arma mas não tiver nível de ascensão, verificar se há dados no PDC para exibir
            if (ItemStatsTracker.getInstance().getConfig().getBoolean("display-attack.enabled", true)
                && pdc.has(attackKey, PersistentDataType.TAG_CONTAINER_ARRAY)) {
                PersistentDataContainer[] bonuses = pdc.get(attackKey, PersistentDataType.TAG_CONTAINER_ARRAY);
                if (bonuses != null && bonuses.length > 0) {
                    addBlankLineIfNecessary(cleanedLore);
                    cleanedLore.add(LanguageManager.getMessage("lore.attack-header").insertion(LORE_MARKER_PREFIX + "ATTACK_HEADER"));
                    // Agrupar e ordenar os ataques: PvP primeiro, depois PvE
                    Map<String, Double> groupedAttack = new LinkedHashMap<>();
                    for (PersistentDataContainer bonusContainer : bonuses) {
                        String targetType = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "target_type"), PersistentDataType.STRING);
                        Double percent = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE);
                        if (targetType != null && percent != null && percent > 0) {
                            String targetUpper = targetType.toUpperCase(Locale.ROOT);
                            // Agrupar PvP
                            if (targetUpper.contains("PLAYER") || targetUpper.contains("PVP")) {
                                groupedAttack.put("PVP", groupedAttack.getOrDefault("PVP", 0.0) + percent);
                            } else {
                                // Outros tipos (MOB, UNDEAD, etc.) como PvE
                                groupedAttack.put("PVE", groupedAttack.getOrDefault("PVE", 0.0) + percent);
                            }
                        }
                    }
                    
                    // Exibir na ordem: PvP primeiro, depois PvE
                    String[] attackOrder = {"PVP", "PVE"};
                    for (String category : attackOrder) {
                        Double percent = groupedAttack.get(category);
                        if (percent != null && percent > 0) {
                            String displayName;
                            String colorGradient;
                            
                            if ("PVP".equals(category)) {
                                displayName = "Ataque [PvP]";
                                colorGradient = getAttackColor("PLAYER");
                            } else {
                                displayName = "Ataque [PvE]";
                                colorGradient = getAttackColor("MOB");
                            }
                            
                            String formattedLine = colorGradient + displayName + ": <#00F66A>+" + String.format("%.1f", percent) + "%</#00F66A></gradient>";
                            cleanedLore.add(MINI_MESSAGE.deserialize(formattedLine).insertion(LORE_MARKER_PREFIX + "ATTACK:" + category));
                        }
                    }
                }
            }
        }
        
        // Sistema de Tool % para ferramentas (picaretas, machados, enxadas, pás)
        NamespacedKey toolKey = new NamespacedKey(ItemStatsTracker.getInstance(), "tool_bonus");
        // isTool já foi declarado anteriormente na linha 580
        int reincarnadoLevelForTool = StatManager.getReincarnadoLevel(item);
        if (isTool && reincarnadoLevelForTool > 0) {
            FileConfiguration levelEffectsCfg = ItemStatsTracker.getInstance().getLevelEffectsConfig();
            if (levelEffectsCfg != null) {
                ConfigurationSection levelsSec = levelEffectsCfg.getConfigurationSection("LEVEL_EFFECTS");
                if (levelsSec != null) {
                    Map<String, Double> summedTool = new HashMap<>();
                    for (String levelKey : levelsSec.getKeys(false)) {
                        try {
                            int lvl = Integer.parseInt(levelKey);
                            if (lvl <= reincarnadoLevelForTool) {
                                ConfigurationSection lvlSec = levelsSec.getConfigurationSection(levelKey);
                                if (lvlSec != null) {
                                    ConfigurationSection toolSec = lvlSec.getConfigurationSection("tool_bonuses");
                                    if (toolSec != null) {
                                        for (String bonusType : toolSec.getKeys(false)) {
                                            double val = toolSec.getDouble(bonusType, 0.0);
                                            if (val > 0) {
                                                String bonusUpper = bonusType.toUpperCase(Locale.ROOT);
                                                summedTool.put(bonusUpper, summedTool.getOrDefault(bonusUpper, 0.0) + val);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    // Se o PDC não existe ou está desatualizado em relação à soma esperada, reescreve
                    boolean needsRewrite = true;
                    if (pdc.has(toolKey, PersistentDataType.TAG_CONTAINER_ARRAY)) {
                        PersistentDataContainer[] existing = pdc.get(toolKey, PersistentDataType.TAG_CONTAINER_ARRAY);
                        Map<String, Double> current = new HashMap<>();
                        if (existing != null) {
                            for (PersistentDataContainer ec : existing) {
                                String bonusType = ec.get(new NamespacedKey(ItemStatsTracker.getInstance(), "bonus_type"), PersistentDataType.STRING);
                                Double percent = ec.get(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE);
                                if (bonusType != null && percent != null) current.put(bonusType.toUpperCase(Locale.ROOT), percent);
                            }
                        }
                        needsRewrite = !current.equals(summedTool);
                    }
                    if (needsRewrite) {
                        if (summedTool.isEmpty()) {
                            pdc.remove(toolKey);
                        } else {
                            List<PersistentDataContainer> containers = new ArrayList<>();
                            for (Map.Entry<String, Double> e : summedTool.entrySet()) {
                                PersistentDataContainer bonusContainer = pdc.getAdapterContext().newPersistentDataContainer();
                                bonusContainer.set(new NamespacedKey(ItemStatsTracker.getInstance(), "bonus_type"), PersistentDataType.STRING, e.getKey());
                                bonusContainer.set(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE, e.getValue());
                                containers.add(bonusContainer);
                            }
                            pdc.set(toolKey, PersistentDataType.TAG_CONTAINER_ARRAY, containers.toArray(new PersistentDataContainer[0]));
                            // IMPORTANTE: Aplicar o meta ao item após salvar os bônus no PDC
                            item.setItemMeta(meta);
                            // Reler o meta atualizado
                            meta = item.getItemMeta();
                            if (meta != null) {
                                pdc = meta.getPersistentDataContainer();
                            }
                        }
                    }
                    
                    // Exibir bônus de ferramentas - usar summedTool diretamente se houver dados
                    if (ItemStatsTracker.getInstance().getConfig().getBoolean("display-tool.enabled", true) && !summedTool.isEmpty()) {
                        addBlankLineIfNecessary(cleanedLore);
                        cleanedLore.add(LanguageManager.getMessage("lore.tool-header").insertion(LORE_MARKER_PREFIX + "TOOL_HEADER"));
                        for (Map.Entry<String, Double> e : summedTool.entrySet()) {
                            String bonusType = e.getKey();
                            Double percent = e.getValue();
                            if (bonusType != null && percent != null && percent > 0) {
                                String bonusDisplay = capitalizeFirstLetter(bonusType.replace("_", " ").toLowerCase(Locale.ROOT));
                                String colorGradient = getToolColor(bonusType);
                                String formattedLine = colorGradient + bonusDisplay + ": <#00F66A>+" + String.format("%.1f", percent) + "%</#00F66A></gradient>";
                                cleanedLore.add(MINI_MESSAGE.deserialize(formattedLine).insertion(LORE_MARKER_PREFIX + "TOOL:" + bonusType));
                            }
                        }
                    }
                }
            }
        } else if (isTool) {
            // Se for ferramenta mas não tiver nível de ascensão, verificar se há dados no PDC para exibir
            if (ItemStatsTracker.getInstance().getConfig().getBoolean("display-tool.enabled", true)
                && pdc.has(toolKey, PersistentDataType.TAG_CONTAINER_ARRAY)) {
                PersistentDataContainer[] bonuses = pdc.get(toolKey, PersistentDataType.TAG_CONTAINER_ARRAY);
                if (bonuses != null && bonuses.length > 0) {
                    addBlankLineIfNecessary(cleanedLore);
                    cleanedLore.add(LanguageManager.getMessage("lore.tool-header").insertion(LORE_MARKER_PREFIX + "TOOL_HEADER"));
                    for (PersistentDataContainer bonusContainer : bonuses) {
                        String bonusType = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "bonus_type"), PersistentDataType.STRING);
                        Double percent = bonusContainer.get(new NamespacedKey(ItemStatsTracker.getInstance(), "percent"), PersistentDataType.DOUBLE);
                        if (bonusType != null && percent != null && percent > 0) {
                            String bonusDisplay = capitalizeFirstLetter(bonusType.replace("_", " ").toLowerCase(Locale.ROOT));
                            String colorGradient = getToolColor(bonusType);
                            String formattedLine = colorGradient + bonusDisplay + ": <#00F66A>+" + String.format("%.1f", percent) + "%</#00F66A></gradient>";
                            cleanedLore.add(MINI_MESSAGE.deserialize(formattedLine).insertion(LORE_MARKER_PREFIX + "TOOL:" + bonusType));
                        }
                    }
                }
            }
        }
        
        // Separar efeitos vanilla e AE, removendo duplicatas
        List<String> vanillaEffects = new ArrayList<>();
        Set<String> aeEffectsSet = new HashSet<>(); // Usa Set para evitar duplicatas
        List<String> aeEffects = new ArrayList<>();
        
        for (String effectEntry : customEffects) {
            if (effectEntry != null && effectEntry.toUpperCase(Locale.ROOT).startsWith("AE:")) {
                String normalized = StatManager.normalizeCustomEffect(effectEntry);
                if (!normalized.isEmpty() && aeEffectsSet.add(normalized)) {
                    aeEffects.add(effectEntry); // Mantém o original para exibição
                }
            } else {
                vanillaEffects.add(effectEntry);
            }
        }
        
        // Adicionar seção de Efeitos (Vanilla)
        // Sempre mostrar se houver encantamentos ou se o sistema de limites estiver habilitado
        int vanillaCount = vanillaEnchantments.size() + vanillaEffects.size();
        int vanillaLimit = getEnchantmentLimit(item, "vanilla");
        boolean showEffectsSection = !vanillaEnchantments.isEmpty() || !vanillaEffects.isEmpty() || vanillaLimit >= 0;
        
        if (showEffectsSection) {
            addBlankLineIfNecessary(cleanedLore);
            // Header normal sem contador
            cleanedLore.add(LanguageManager.getMessage("lore.effects-header").insertion(EFFECT_LORE_MARKER_PREFIX + "HEADER"));
            
            // Adicionar encantamentos vanilla (apenas se houver)
            if (!vanillaEnchantments.isEmpty() || !vanillaEffects.isEmpty()) {
                // Adicionar encantamentos vanilla
                for (Component enchantComponent : vanillaEnchantments) {
                    cleanedLore.add(enchantComponent);
                }
                
                // Adicionar efeitos vanilla customizados
                for (String effectEntry : vanillaEffects) {
                    Component effectComponent = formatCustomEffect(effectEntry, enchantmentsConfig);
                    String normalizedMarker = effectEntry == null ? "UNKNOWN" : effectEntry.replace("/", ":").toUpperCase(Locale.ROOT);
                    cleanedLore.add(effectComponent.insertion(EFFECT_LORE_MARKER_PREFIX + normalizedMarker));
                }
            }
            
            // Adicionar contador abaixo dos encantamentos (se houver limite configurado)
            if (vanillaLimit >= 0) {
                String counterText = "<gradient:#4facfe:#00f2fe>" + vanillaCount + "/" + vanillaLimit + "</gradient>";
                cleanedLore.add(MINI_MESSAGE.deserialize(counterText).insertion(EFFECT_LORE_MARKER_PREFIX + "COUNTER"));
            }
        }
        
        // Adicionar seção de Mágicos (AE) se houver
        // Contar encantamentos AE
        int aeCount = 0;
        for (String effectEntry : customEffects) {
            if (effectEntry != null) {
                String effectUpper = effectEntry.toUpperCase(Locale.ROOT);
                if (effectUpper.startsWith("AE:")) {
                    aeCount++;
                }
            }
        }
        int magicalCount = aeCount;
        int aeLimit = getEnchantmentLimit(item, "ae");
        int magicalLimit = aeLimit >= 0 ? aeLimit : -1;
        
        // Sempre mostrar seção mágica se houver encantamentos ou se o sistema de limites estiver habilitado
        boolean showMagicalSection = !aeEffects.isEmpty() || magicalLimit >= 0;
        
        if (showMagicalSection) {
            addBlankLineIfNecessary(cleanedLore);
            // Header normal sem contador
            cleanedLore.add(LanguageManager.getMessage("lore.magical-effects-header").insertion(EFFECT_LORE_MARKER_PREFIX + "MAGICAL_HEADER"));
            
            // Adicionar encantamentos mágicos (apenas se houver)
            if (!aeEffects.isEmpty()) {
                for (String effectEntry : aeEffects) {
                    Component effectComponent = formatCustomEffect(effectEntry, enchantmentsConfig);
                    String normalizedMarker = effectEntry == null ? "UNKNOWN" : effectEntry.replace("/", ":").toUpperCase(Locale.ROOT);
                    cleanedLore.add(effectComponent.insertion(EFFECT_LORE_MARKER_PREFIX + normalizedMarker));
                }
            }
            
            // Adicionar contador abaixo dos encantamentos mágicos (se houver limite configurado)
            if (magicalLimit >= 0) {
                String counterText = "<#E43A96>" + magicalCount + "<#DD3E9B>/<#D7419F>" + magicalLimit + "</#D7419F>";
                cleanedLore.add(MINI_MESSAGE.deserialize(counterText).insertion(EFFECT_LORE_MARKER_PREFIX + "MAGICAL_COUNTER"));
            }
        }
        
        int reincarnadoLevel = StatManager.getReincarnadoLevel(item);
        if (reincarnadoLevel > 0 && ItemStatsTracker.getInstance().getConfig().getBoolean("display-reincarnado.enabled", true)) {
            addBlankLineIfNecessary(cleanedLore);
            cleanedLore.add(LanguageManager.getMessage("lore.reincarnado-level", 
                Placeholder.unparsed("level", StatManager.toRoman(reincarnadoLevel))
            ).insertion(LORE_MARKER_PREFIX + "REINCARNADO_LEVEL"));

            // Bônus de Drop
            double dropBonusPerLevel = config.getDouble("reincarnado.bonus-drop-percentage-per-level", 0.005);
            double totalDropBonus = reincarnadoLevel * dropBonusPerLevel * 100;
            if (totalDropBonus > 0) {
            cleanedLore.add(LanguageManager.getMessage("lore.reincarnado-bonus", 
                    Placeholder.unparsed("bonus", String.format("%.1f", totalDropBonus))
                ).insertion(LORE_MARKER_PREFIX + "REINCARNADO_BONUS_DROP"));
            }
            
            // Bônus de XP para todos os itens
            double expBonusPerLevel = config.getDouble("reincarnado.bonus-exp-percentage-per-level", 0.01);
            double totalExpBonus = reincarnadoLevel * expBonusPerLevel * 100;
            if (totalExpBonus > 0) {
                String expBonusText = String.format("<gray>Bônus de XP: <#00F66A>+%.1f%%</#00F66A>", totalExpBonus);
                cleanedLore.add(MINI_MESSAGE.deserialize(expBonusText).insertion(LORE_MARKER_PREFIX + "REINCARNADO_BONUS_XP"));
            }
        }
        
        // Só exibir dono se o item for rastreável
        String originalOwner = null;
        if (StatManager.itemDeveSerRastreado(item)) {
            originalOwner = StatManager.getOriginalOwner(item);
        }
        if (ItemStatsTracker.getInstance().getConfig().getBoolean("display-owner.enabled", true)
            && originalOwner != null && !originalOwner.isEmpty()) {
            addBlankLineIfNecessary(cleanedLore);
            // Formatação do Dono com gradiente letra por letra e nick usando código legacy &#FFFFFF&l&n
            String ownerHeader = "<bold><#FF4800>D<#FD5900>o<#FB6A00>n<#F87B00>o<#F68C00>:</bold>";
            Component ownerHeaderComponent = MINI_MESSAGE.deserialize(ownerHeader);
            // Usar LegacyComponentSerializer com hex colors para o nome do jogador
            // &#FFFFFF = branco em hex, &l = negrito, &n = sublinhado
            LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand().toBuilder()
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
            Component ownerNameComponent = legacySerializer.deserialize("&#FFFFFF&l&n" + originalOwner);
            Component ownerFormatted = ownerHeaderComponent.append(Component.text(" ")).append(ownerNameComponent);
            cleanedLore.add(ownerFormatted.insertion("IOwner:" + originalOwner));
            
            // Adicionar estatísticas DEPOIS do Dono (com espaço)
            if (!statsLore.isEmpty()) {
                addBlankLineIfNecessary(cleanedLore);
                cleanedLore.addAll(statsLore);
            }
        } else {
            // Se não há dono, adicionar estatísticas diretamente
            if (!statsLore.isEmpty()) {
                addBlankLineIfNecessary(cleanedLore);
                cleanedLore.addAll(statsLore);
            }
        }

        if (ItemStatsTracker.getInstance().getConfig().getBoolean("display-enchant-progress.enabled", true)
            && !enchantmentProgressLore.isEmpty()) {
            addBlankLineIfNecessary(cleanedLore);
            cleanedLore.add(LanguageManager.getMessage("lore.enchant-upgrade-header").insertion(LORE_MARKER_PREFIX + "ENCHANTMENT_PROGRESS_HEADER"));
            cleanedLore.addAll(enchantmentProgressLore);
        }

        // Garantir que meta não seja null antes de usar
        if (meta != null) {
            meta.lore(cleanedLore.isEmpty() ? null : cleanedLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Obtém o limite de encantamentos para um item e tipo específico.
     * Primeiro verifica a categoria do item, depois o material específico.
     * @param item O item
     * @param type O tipo de encantamento ("vanilla", "ae", "bp")
     * @return O limite configurado, ou -1 se não houver limite
     */
    private static int getEnchantmentLimit(ItemStack item, String type) {
        FileConfiguration config = ItemStatsTracker.getInstance().getConfig();
        if (!config.getBoolean("enchantment-limits.enabled", true)) {
            return -1; // Sistema desabilitado
        }
        
        Material material = item.getType();
        String materialName = material.name();
        
        // Primeiro, verificar limites por categoria (prioridade maior)
        String category = getItemCategoryForLimits(material);
        if (category != null) {
            ConfigurationSection categoryLimitsSection = config.getConfigurationSection("enchantment-limits.category-limits");
            if (categoryLimitsSection != null) {
                ConfigurationSection categoryLimits = categoryLimitsSection.getConfigurationSection(category);
                if (categoryLimits != null) {
                    int categoryLimit = categoryLimits.getInt(type, -1);
                    if (categoryLimit >= 0) {
                        return categoryLimit; // Retornar limite da categoria se encontrado
                    }
                }
            }
        }
        
        // Se não encontrou limite por categoria, verificar por material específico
        ConfigurationSection limitsSection = config.getConfigurationSection("enchantment-limits.limits");
        if (limitsSection == null) {
            return -1; // Sem configuração
        }
        
        ConfigurationSection itemLimits = limitsSection.getConfigurationSection(materialName);
        if (itemLimits == null) {
            return -1; // Item não tem limites configurados
        }
        
        return itemLimits.getInt(type, -1);
    }
    
    /**
     * Obtém a categoria específica do item para uso em limites de encantamentos.
     * Retorna categorias como HELMET, CHESTPLATE, LEGGINGS, BOOTS, SWORDS, BOWS, etc.
     * @param material O material do item
     * @return A categoria do item, ou null se não se encaixar em nenhuma categoria
     */
    private static String getItemCategoryForLimits(Material material) {
        String typeName = material.name().toUpperCase(Locale.ROOT);
        
        // Armaduras
        if (typeName.endsWith("_HELMET")) {
            return "HELMET";
        } else if (typeName.endsWith("_CHESTPLATE")) {
            return "CHESTPLATE";
        } else if (typeName.endsWith("_LEGGINGS")) {
            return "LEGGINGS";
        } else if (typeName.endsWith("_BOOTS")) {
            return "BOOTS";
        }
        // Armas
        else if (typeName.endsWith("_SWORD")) {
            return "SWORDS";
        } else if (material == Material.BOW || material == Material.CROSSBOW) {
            return "BOWS";
        } else if (material == Material.TRIDENT) {
            return "TRIDENT";
        } else if (material == Material.MACE) {
            return "MACE";
        }
        // Ferramentas
        else if (typeName.endsWith("_PICKAXE")) {
            return "PICKAXES";
        } else if (typeName.endsWith("_AXE")) {
            return "AXES";
        } else if (typeName.endsWith("_SHOVEL")) {
            return "SHOVELS";
        } else if (typeName.endsWith("_HOE")) {
            return "HOES";
        }
        
        return null; // Item não se encaixa em nenhuma categoria configurável
    }
    
    private static boolean isLikelyPluginLoreLine(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return false;
        }
        String lower = plainText.toLowerCase(Locale.ROOT);

        if (lower.equals("estatísticas:") || lower.equals("efeitos:") || lower.startsWith("efeitos:")
            || lower.equals("upgrades de encantamento:") || lower.startsWith("upgrades de encantamento:")
            || lower.equals("bônus de resistência:") || lower.equals("bônus de ataque:") || lower.equals("item mestre") 
            || lower.equals("parte do conjunto:") || lower.startsWith("parte do conjunto:")
            || lower.equals("mágicos:") || lower.startsWith("mágicos:")
            || lower.matches("\\d+/\\d+")) { // Filtrar contadores X/Y
            return true;
        }
        if (lower.startsWith("abates de") || lower.startsWith("dano causado") || lower.startsWith("dano em")
            || lower.startsWith("minérios") || lower.startsWith("madeiras") || lower.startsWith("plantações")
            || lower.startsWith("blocos totais") || lower.startsWith("blocos quebrados")
            || lower.startsWith("mobs abatidos") || lower.startsWith("players mortos") || lower.startsWith("dano total")
            || lower.startsWith("dono:") || lower.startsWith("ascensão") || lower.startsWith("bônus de drop")) {
            return true;
        }
        if (lower.startsWith("resistência a")) {
            return true;
        }
        if (plainText.startsWith("•")) {
            return true;
        }
        if (lower.contains("%") && lower.contains(":")) {
            return true;
        }
        if (lower.startsWith("lifesteal") || lower.startsWith("berserk") || lower.startsWith("overload") || lower.startsWith("frost")
            || lower.startsWith("epicness") || lower.startsWith("epicness iii") || lower.startsWith("epicness v")) {
            return true;
        }
        return false;
    }

    public static ItemStack updateArmorSetLore(ItemStack item, boolean equipped) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        // Remove existing armor set lore before adding new one
        lore.removeIf(line -> {
            String insertion = line.insertion();
            return insertion != null && insertion.startsWith(ARMOR_SET_LORE_MARKER_PREFIX);
        });

        if (equipped) {
            FileConfiguration config = ItemStatsTracker.getInstance().getConfig();
            var armorSetsSection = config.getConfigurationSection("armor-sets");

            if (armorSetsSection != null) {
                for (String setId : armorSetsSection.getKeys(false)) {
                    var armorSet = armorSetsSection.getConfigurationSection(setId);
                    if (armorSet == null) {
                        continue;
                    }

                    var pieces = armorSet.getConfigurationSection("pieces");
                    if (pieces == null) {
                        continue;
                    }

                    String itemMaterialName = item.getType().name();
                    String setDisplayName = armorSet.getString("display-name", "Conjunto Desconhecido");

                    if (itemMaterialName.equals(pieces.getString("HELMET")) ||
                        itemMaterialName.equals(pieces.getString("CHESTPLATE")) ||
                        itemMaterialName.equals(pieces.getString("LEGGINGS")) ||
                        itemMaterialName.equals(pieces.getString("BOOTS"))) {

                        String equippedLoreLine = String.format("<green>Parte do conjunto: %s (Equipado)</green>", setDisplayName);
                        lore.add(MINI_MESSAGE.deserialize(equippedLoreLine).insertion(ARMOR_SET_LORE_MARKER_PREFIX + setId));
                        break; // Found the set, no need to check other sets for this item
                    }
                }
            }
        }

        if (lore.isEmpty()) {
            meta.lore(null);
        } else {
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}
