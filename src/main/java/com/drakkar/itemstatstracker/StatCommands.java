package com.drakkar.itemstatstracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Bukkit;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import com.drakkar.itemstatstracker.utility.AdvancedEnchantmentsBridge;

public final class StatCommands implements CommandExecutor, TabCompleter {

    private final ItemStatsTracker plugin;

    public StatCommands(ItemStatsTracker plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("itemstats.admin")) {
            sender.sendMessage(LanguageManager.getMessage("command.no-permission"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(LanguageManager.getMessage("command.player-only"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(LanguageManager.getMessage("command.giveascension.usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "reload" -> {
                plugin.loadYamls(); // loadYamls agora também recarrega o messages.yml
                sender.sendMessage(LanguageManager.getMessage("command.reload-success"));
                return true;
            }
            case "info" -> {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    sender.sendMessage(LanguageManager.getMessage("command.invalid-item"));
                    return true;
                }

                showReincarnadoProgress(player, item);
                showEnchantmentUpgradeProgress(player, item);

                return true;
            }
            case "cleardono", "clearowner" -> {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    sender.sendMessage(LanguageManager.getMessage("command.invalid-item"));
                    return true;
                }

                if (StatManager.getOriginalOwner(item) == null) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Este item não possui um dono registrado.</yellow>"));
                    return true;
                }

                StatManager.removeOriginalOwner(item);
                ItemStack updatedItem = LoreManager.updateLore(item);
                
                // Atualizar o item no inventário do jogador diretamente
                final ItemStatsTracker plugin = ItemStatsTracker.getInstance();
                plugin.getIgnoreArmorChangeEvent().add(player.getUniqueId());
                
                EquipmentSlot slot = findItemSlot(player, item);
                if (slot != null) {
                    if (slot == EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(updatedItem);
                    } else if (slot == EquipmentSlot.OFF_HAND) {
                        player.getInventory().setItemInOffHand(updatedItem);
                    } else if (slot == EquipmentSlot.HEAD) {
                        player.getInventory().setHelmet(updatedItem);
                    } else if (slot == EquipmentSlot.CHEST) {
                        player.getInventory().setChestplate(updatedItem);
                    } else if (slot == EquipmentSlot.LEGS) {
                        player.getInventory().setLeggings(updatedItem);
                    } else if (slot == EquipmentSlot.FEET) {
                        player.getInventory().setBoots(updatedItem);
                    }
                } else {
                    // Se não encontrou o slot, tentar atualizar no slot da mão principal
                    player.getInventory().setItemInMainHand(updatedItem);
                }
                
                // Remover do ignore list após um delay para permitir que o evento seja ignorado
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getIgnoreArmorChangeEvent().remove(player.getUniqueId());
                }, 1L);
                
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Dono do item removido com sucesso!</green>"));
                return true;
            }
            case "timer" -> {
                if (args.length < 2) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#ff4d4d:#ff6b6b>⚠</gradient> <gray>Uso: <white>/ist timer <segundos></white></gray>"));
                    return true;
                }
                
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    sender.sendMessage(LanguageManager.getMessage("command.invalid-item"));
                    return true;
                }
                
                long seconds;
                try {
                    seconds = Long.parseLong(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#ff4d4d:#ff6b6b>⚠</gradient> <gradient:#ff4d4d:#ff8c42>Tempo inválido: <white>" + args[1] + "</white></gradient>"));
                    return true;
                }
                
                if (seconds <= 0) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#ff4d4d:#ff6b6b>⚠</gradient> <gradient:#ff4d4d:#ff8c42>O tempo deve ser maior que 0.</gradient>"));
                    return true;
                }
                
                long maxDuration = plugin.getConfig().getLong("timed-items.limits.max_duration_seconds", 86400);
                if (seconds > maxDuration) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#ff4d4d:#ff6b6b>⚠</gradient> <gradient:#ff4d4d:#ff8c42>O tempo máximo permitido é <white>" + maxDuration + " segundos</white>.</gradient>"));
                    return true;
                }
                
                // Verificar se o sistema de itens temporizados está ativo
                com.drakkar.itemstatstracker.timed.TimedItemManager timedManager = plugin.getTimedItemManager();
                if (timedManager == null) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#ff4d4d:#ff6b6b>⚠</gradient> <gradient:#ff4d4d:#ff8c42>O sistema de itens temporizados está desabilitado.</gradient>"));
                    return true;
                }
                
                // Adicionar timer ao item existente (sem dar o item novamente)
                ItemStack timedItem = timedManager.addTimerToItem(player, item.clone(), seconds);
                
                // Atualizar o item no inventário
                final ItemStatsTracker pluginInstance = ItemStatsTracker.getInstance();
                pluginInstance.getIgnoreArmorChangeEvent().add(player.getUniqueId());
                
                EquipmentSlot slot = findItemSlot(player, item);
                if (slot != null) {
                    if (slot == EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(timedItem);
                    } else if (slot == EquipmentSlot.OFF_HAND) {
                        player.getInventory().setItemInOffHand(timedItem);
                    } else if (slot == EquipmentSlot.HEAD) {
                        player.getInventory().setHelmet(timedItem);
                    } else if (slot == EquipmentSlot.CHEST) {
                        player.getInventory().setChestplate(timedItem);
                    } else if (slot == EquipmentSlot.LEGS) {
                        player.getInventory().setLeggings(timedItem);
                    } else if (slot == EquipmentSlot.FEET) {
                        player.getInventory().setBoots(timedItem);
                    }
                } else {
                    player.getInventory().setItemInMainHand(timedItem);
                }
                
                // Remover do ignore list após um delay
                pluginInstance.getServer().getScheduler().runTaskLater(pluginInstance, () -> {
                    pluginInstance.getIgnoreArmorChangeEvent().remove(player.getUniqueId());
                }, 1L);
                
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<gradient:#00ff88:#00ffaa>✓</gradient> <gradient:#ff8c42:#ffd700>Timer de <white>" + seconds + " segundos</white> adicionado ao item!</gradient>"));
                return true;
            }
            case "set", "add" -> {
                if (args.length < 3) {
                    String usageMessage = LanguageManager.getRawString("command.giveascension.usage").replace("giveascension", subCommand + " <estatistica> <valor>");
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(usageMessage));
                    return true;
                }

                String statType = args[1].toUpperCase(Locale.ROOT);
                if (!StatManager.getRegisteredStats().contains(statType)) {
                    String unknownStatMessage = LanguageManager.getRawString("command.unknown-subcommand").replace("Subcomando", "Estatística");
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(unknownStatMessage));
                    return true;
                }

                int value;
                try {
                    value = Integer.parseInt(args[2]);
                } catch (NumberFormatException exception) {
                    sender.sendMessage(LanguageManager.getMessage("command.giveascension.invalid-amount", Placeholder.unparsed("amount", args[2])));
                    return true;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    sender.sendMessage(LanguageManager.getMessage("command.invalid-item"));
                    return true;
                }

                if (subCommand.equals("set")) {
                    StatManager.setStat(item, statType, Math.max(value, 0));
                } else {
                    StatManager.incrementStat(player, item, statType, value);
                }

                updateItemAndIgnoreEvent(player, item);
                String successMessage = LanguageManager.getRawString("command.reload-success").replace("Configurações recarregadas com sucesso.", "Estatística atualizada: " + statType);
                sender.sendMessage(MiniMessage.miniMessage().deserialize(successMessage));
                return true;
            }
            case "giverr" -> {
                if (args.length < 2) { // Alterado de 3 para 2, pois o jogador pode ser omitido
                    sender.sendMessage(LanguageManager.getMessage("command.giverr.usage"));
                    return true;
                }

                Player targetPlayer = null;
                int amount;

                // Tenta interpretar o segundo argumento como um número.
                // Se funcionar, o alvo é o próprio jogador.
                try {
                    amount = Integer.parseInt(args[1]);
                    targetPlayer = player; // O alvo é quem executa o comando
                } catch (NumberFormatException e) {
                    // Se não for um número, deve ser um nome de jogador.
                    if (args.length < 3) {
                        sender.sendMessage(LanguageManager.getMessage("command.giverr.usage"));
                        return true;
                    }
                    targetPlayer = plugin.getServer().getPlayer(args[1]);
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(LanguageManager.getMessage("command.giverr.invalid-amount", Placeholder.unparsed("amount", args[2])));
                        return true;
                    }
                }

                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    sender.sendMessage(LanguageManager.getMessage("command.giverr.player-not-found", Placeholder.unparsed("player", args[1])));
                    return true;
                }

                ItemStack itemToAscend = targetPlayer.getInventory().getItemInMainHand();
                if (itemToAscend == null || itemToAscend.getType() == Material.AIR) {
                    sender.sendMessage(LanguageManager.getMessage("command.giverr.target-no-item", Placeholder.unparsed("player", targetPlayer.getName())));
                    return true;
                }

                int currentLevel = StatManager.getReincarnadoLevel(itemToAscend);
                int newLevel = currentLevel + amount;
                StatManager.setReincarnadoLevel(itemToAscend, newLevel);
                applyResistanceBonuses(itemToAscend, newLevel);
                handleRenameOnReincarnado(targetPlayer, itemToAscend, newLevel);
                handleItemMestre(targetPlayer, itemToAscend, newLevel);
                runLevelCommands(targetPlayer, itemToAscend, newLevel);
                updateItemAndIgnoreEvent(targetPlayer, itemToAscend);

                sender.sendMessage(LanguageManager.getMessage("command.giverr.success-sender", 
                    Placeholder.unparsed("amount", String.valueOf(amount)),
                    Placeholder.unparsed("player", targetPlayer.getName())));
                
                // Só envia mensagem para o alvo se for outra pessoa
                if (!targetPlayer.equals(sender)) {
                    targetPlayer.sendMessage(LanguageManager.getMessage("command.giverr.success-target", 
                        Placeholder.unparsed("amount", String.valueOf(amount)),
                        Placeholder.unparsed("level", String.valueOf(newLevel))));
                }
                return true;
            }
            case "addeffect" -> {
                if (args.length < 2) {
                    sender.sendMessage(LanguageManager.getMessage("command.addeffect.usage"));
                    return true;
                }

                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem == null || heldItem.getType() == Material.AIR) {
                    sender.sendMessage(LanguageManager.getMessage("command.invalid-item"));
                    return true;
                }

                // Novo formato: /ist addeffect AE/VN <tipo_item> <encantamento> <nível>
                // Formato antigo (compatibilidade): /ist addeffect <encantamento>
                String rawEffect;
                String itemTypeFilter = null;
                
                if (args.length >= 4 && (args[1].equalsIgnoreCase("AE") || args[1].equalsIgnoreCase("VN"))) {
                    // Novo formato
                    itemTypeFilter = args[2].toLowerCase(Locale.ROOT);
                    String enchantName = args[3];
                    String level = args.length > 4 ? args[4] : "";
                    rawEffect = args[1] + ":" + enchantName + (level.isEmpty() ? "" : ":" + level);
                    
                    // Validar se o tipo de item corresponde ao item na mão
                    if (!isItemTypeMatch(heldItem.getType(), itemTypeFilter)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<red>O tipo de item especificado (" + itemTypeFilter + ") não corresponde ao item na mão.</red>"));
                        return true;
                    }
                } else {
                    // Formato antigo (compatibilidade)
                    rawEffect = args.length == 2
                        ? args[1]
                        : String.join(":", Arrays.copyOfRange(args, 1, args.length));
                }

                ParsedCustomEffect effect = parseCustomEffect(rawEffect);
                if (effect == null) {
                    sender.sendMessage(LanguageManager.getMessage("command.addeffect.invalid-effect",
                        Placeholder.unparsed("effect", rawEffect)));
                    return true;
                }

                FileConfiguration enchantmentsConfig = plugin.getEnchantmentsConfig();
                ConfigurationSection effectSection = enchantmentsConfig != null
                    ? enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS." + effect.baseKey())
                    : null;
                
                String finalBaseKey = effect.baseKey();
                
                // Tenta variações do nome se não encontrar (com/sem underscore)
                if (effectSection == null && effect.tokens().length > 1) {
                    String enchantName = effect.tokens()[1];
                    // Tenta com underscore se não tinha (ex: VEINMINER -> VEIN_MINER)
                    if (!enchantName.contains("_")) {
                        // Converte camelCase para SNAKE_CASE (ex: VeinMiner -> VEIN_MINER)
                        String withUnderscore = effect.tokens()[0] + ":" + enchantName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
                        ConfigurationSection trySection = enchantmentsConfig != null
                            ? enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS." + withUnderscore)
                            : null;
                        if (trySection != null) {
                            effectSection = trySection;
                            finalBaseKey = withUnderscore;
                        }
                    }
                    // Tenta sem underscore se tinha (ex: VEIN_MINER -> VEINMINER)
                    if (effectSection == null && enchantName.contains("_")) {
                        String withoutUnderscore = effect.tokens()[0] + ":" + enchantName.replace("_", "");
                        ConfigurationSection trySection = enchantmentsConfig != null
                            ? enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS." + withoutUnderscore)
                            : null;
                        if (trySection != null) {
                            effectSection = trySection;
                            finalBaseKey = withoutUnderscore;
                        }
                    }
                }
                
                // Para encantamentos vanilla (VN), verificar diretamente
                if ("VN".equalsIgnoreCase(effect.pluginId()) && effect.tokens().length > 1) {
                    String enchantName = effect.tokens()[1].toUpperCase(Locale.ROOT);
                    @SuppressWarnings("deprecation")
                    Enchantment vanillaEnchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase(Locale.ROOT)));
                    if (vanillaEnchant == null) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<red>Encantamento vanilla inválido: " + enchantName + "</red>"));
                        return true;
                    }
                    // Verificar se o encantamento pode ser aplicado ao item
                    if (!vanillaEnchant.canEnchantItem(heldItem)) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<red>O encantamento " + enchantName + " não pode ser aplicado a este tipo de item.</red>"));
                        return true;
                    }
                    // Para vanilla, aplicar diretamente
                    int level = effect.providedLevel() > 0 ? effect.providedLevel() : 1;
                    
                    // Validar nível (1-30000, exceto encantamentos de nível único que são sempre 1)
                    if (isSingleLevelEnchantment(enchantName)) {
                        level = 1; // Forçar nível 1 para encantamentos de nível único
                    } else {
                        // Validar que o nível está entre 1 e 30000
                        if (level < 1) {
                            level = 1;
                        } else if (level > 30000) {
                            sender.sendMessage(MiniMessage.miniMessage().deserialize(
                                "<red>O nível máximo para encantamentos vanilla é 30.000.</red>"));
                            return true;
                        }
                    }
                    
                    heldItem.addUnsafeEnchantment(vanillaEnchant, level);
                    updateItemAndIgnoreEvent(player, heldItem);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<green>Encantamento vanilla " + enchantName + " " + level + " aplicado com sucesso!</green>"));
                    return true;
                }
                
                if (effectSection == null) {
                    sender.sendMessage(LanguageManager.getMessage("command.addeffect.invalid-effect",
                        Placeholder.unparsed("effect", effect.baseKey())));
                    return true;
                }

                if ("AE".equalsIgnoreCase(effect.pluginId()) && effect.tokens().length > 1 && AdvancedEnchantmentsBridge.isAvailable()) {
                    String enchantLower = effect.tokens()[1].toLowerCase(Locale.ROOT);
                    List<String> available = AdvancedEnchantmentsBridge.suggestEnchants(player, enchantLower);
                    if (!available.contains(enchantLower)) {
                        sender.sendMessage(LanguageManager.getMessage("command.addeffect.invalid-effect",
                            Placeholder.unparsed("effect", effect.tokens()[1])));
                        return true;
                    }
                }

                List<String> existingEffects = StatManager.getCustomEffects(heldItem);

                int chosenLevel = effect.providedLevel() > 0 ? effect.providedLevel() : 0;
                if (chosenLevel <= 0 && "AE".equalsIgnoreCase(effect.pluginId()) && effect.tokens().length > 1 && AdvancedEnchantmentsBridge.isAvailable()) {
                    OptionalInt resolved = AdvancedEnchantmentsBridge.resolveHighestLevel(player, effect.tokens()[1].toLowerCase(Locale.ROOT));
                    if (resolved.isPresent()) {
                        chosenLevel = resolved.getAsInt();
                    }
                }
                if (chosenLevel <= 0 && effectSection != null) {
                    chosenLevel = Math.max(effectSection.getInt("default-level", 1), 1);
                }
                if (chosenLevel <= 0) {
                    chosenLevel = 1;
                }

                String storedEffect = finalBaseKey + ":" + chosenLevel;

                if (existingEffects.contains(storedEffect)) {
                    sender.sendMessage(LanguageManager.getMessage("command.addeffect.duplicate",
                        Placeholder.component("effect", LoreManager.formatCustomEffectDisplay(storedEffect))));
                    return true;
                }

                // Verificar limites de encantamentos
                String enchantType = effect.pluginId().toUpperCase(Locale.ROOT); // AE ou VANILLA
                if (!checkEnchantmentLimit(player, heldItem, enchantType, existingEffects)) {
                    return true; // Mensagem de erro já foi enviada
                }

                // Remove variações antigas para manter apenas uma entrada por efeito
                for (String existing : new ArrayList<>(existingEffects)) {
                    if (existing.equals(finalBaseKey) || existing.startsWith(finalBaseKey + ":")) {
                        StatManager.removeCustomEffect(heldItem, existing);
                    }
                }

                StatManager.addCustomEffect(heldItem, storedEffect);
                // Passa o finalBaseKey para garantir que o comando use o nome correto
                applyExternalCustomEffect(player, effect, chosenLevel, effectSection, finalBaseKey);
                updateItemAndIgnoreEvent(player, heldItem);

                sender.sendMessage(LanguageManager.getMessage("command.addeffect.success",
                    Placeholder.component("effect", LoreManager.formatCustomEffectDisplay(storedEffect))));
                return true;
            }
            case "setarrow" -> {
                // ... (Este comando pode ser traduzido depois, pois é menos usado)
                return true;
            }
            case "gema" -> {
                return handleGemaCommand(player, args);
            }
            case "acessorio", "acessorios" -> {
                // Comandos futuros de acessórios serão implementados aqui
                // Por enquanto, redireciona para o comando /acessorios
                player.performCommand("acessorios");
                return true;
            }
            case "reincarnado" -> {
                boolean silent = args.length > 1 && args[1].equalsIgnoreCase("silent");

                ItemStack itemReincarnado = player.getInventory().getItemInMainHand();
                if (itemReincarnado == null || itemReincarnado.getType() == Material.AIR) {
                    if (!silent) sender.sendMessage(LanguageManager.getMessage("command.invalid-item"));
                    return true;
                }

                int currentReincarnadoLevel = StatManager.getReincarnadoLevel(itemReincarnado);
                int maxReincarnadoLevel = plugin.getConfig().getInt("reincarnado.max-level", 100);

                if (currentReincarnadoLevel >= maxReincarnadoLevel) {
                    if (!silent) sender.sendMessage(LanguageManager.getMessage("command.info.reincarnado-max-level"));
                    return true;
                }

                List<StatManager.ReincarnadoCriterion> criteria = StatManager.getReincarnadoCriteria(itemReincarnado);
                if (criteria.isEmpty()) {
                    if (!silent) sender.sendMessage(LanguageManager.getMessage("command.info.reincarnado-no-criteria"));
                    return true;
                }

                boolean allCriteriaMet = true;
                
                if (!silent) showReincarnadoProgress(player, itemReincarnado);

                for (StatManager.ReincarnadoCriterion criterion : criteria) {
                    int currentStatValue = StatManager.getStat(itemReincarnado, criterion.getStatType());
                    double requiredValueForNextLevel = criterion.getRequiredValuePerLevel() * (currentReincarnadoLevel + 1);
                    if (currentStatValue < requiredValueForNextLevel) {
                        allCriteriaMet = false;
                        break;
                    }
                }
                
                if (!allCriteriaMet) {
                    if (!silent) sender.sendMessage(LanguageManager.getMessage("command.reincarnado.fail"));
                    return true;
                }

                // Mantém o "overflow" das estatísticas
                for (StatManager.ReincarnadoCriterion criterion : criteria) {
                    int currentStatValue = StatManager.getStat(itemReincarnado, criterion.getStatType());
                    int requiredValue = criterion.getRequiredValuePerLevel() * (currentReincarnadoLevel + 1);
                    int overflow = Math.max(0, currentStatValue - requiredValue);
                    StatManager.setStat(itemReincarnado, criterion.getStatType(), overflow);
                }

                // Incrementa o nível de reincarnação
                int newLevel = currentReincarnadoLevel + 1;
                StatManager.setReincarnadoLevel(itemReincarnado, newLevel);
                
                // Aplica os novos bônus de resistência do level_effects.yml
                applyResistanceBonuses(itemReincarnado, newLevel);

                // Renomeia item na reincarnação se configurado
                handleRenameOnReincarnado(player, itemReincarnado, newLevel);

                // ITEM MESTRE - nível final
                handleItemMestre(player, itemReincarnado, newLevel);

                // Executa comandos do nível atingido (integrações externas)
                runLevelCommands(player, itemReincarnado, newLevel);

                // Atualiza a lore
                updateItemAndIgnoreEvent(player, itemReincarnado);

                sender.sendMessage(LanguageManager.getMessage("command.reincarnado.success", Placeholder.unparsed("level", String.valueOf(newLevel))));
                if (!silent) {
                    sender.sendMessage(LanguageManager.getMessage("command.reincarnado.success", Placeholder.unparsed("level", String.valueOf(newLevel))));
                    sender.sendMessage(LanguageManager.getMessage("command.reincarnado.reset-info"));
                }
                return true;
            }
            default -> {
                sender.sendMessage(LanguageManager.getMessage("command.unknown-subcommand"));
                return true;
            }
        }
    }

    private void applyResistanceBonuses(ItemStack item, int newLevel) {
        if (item == null || !item.hasItemMeta()) return;
        if (!"ARMOR_PIECES".equals(StatManager.getItemCategory(item.getType()))) return; // só armaduras
        
        FileConfiguration levelEffectsConfig = plugin.getLevelEffectsConfig();
        if (levelEffectsConfig == null) return;
        
        ConfigurationSection levelsRoot = levelEffectsConfig.getConfigurationSection("LEVEL_EFFECTS");
        if (levelsRoot == null) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Soma bônus de todos os níveis <= newLevel
        java.util.Map<String, Double> summed = new java.util.HashMap<>();
        for (String levelKey : levelsRoot.getKeys(false)) {
            try {
                int lvl = Integer.parseInt(levelKey);
                if (lvl <= newLevel) {
                    ConfigurationSection resSec = levelsRoot.getConfigurationSection(levelKey + ".resistance_bonuses");
                    if (resSec != null) {
                        for (String causeKey : resSec.getKeys(false)) {
                            double percent = resSec.getDouble(causeKey);
                            if (percent > 0) {
                                String causeUpper = causeKey.toUpperCase(Locale.ROOT);
                                summed.put(causeUpper, summed.getOrDefault(causeUpper, 0.0) + percent);
                            }
                        }
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        // Reescreve o PDC apenas se houver algo
        NamespacedKey resKey = new NamespacedKey(plugin, "resistance_bonus");
        if (summed.isEmpty()) {
            pdc.remove(resKey);
        } else {
            java.util.List<PersistentDataContainer> containers = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, Double> e : summed.entrySet()) {
                PersistentDataContainer bonusContainer = pdc.getAdapterContext().newPersistentDataContainer();
                bonusContainer.set(new NamespacedKey(plugin, "cause"), PersistentDataType.STRING, e.getKey());
                bonusContainer.set(new NamespacedKey(plugin, "percent"), PersistentDataType.DOUBLE, e.getValue());
                containers.add(bonusContainer);
            }
            pdc.set(resKey, PersistentDataType.TAG_CONTAINER_ARRAY, containers.toArray(new PersistentDataContainer[0]));
        }
        
        item.setItemMeta(meta);
    }

    /**
     * Verifica se o item pode ter mais encantamentos do tipo especificado.
     * Primeiro verifica a categoria do item, depois o material específico.
     * @param player O jogador que está tentando adicionar o encantamento
     * @param item O item que está sendo encantado
     * @param enchantType O tipo de encantamento (AE ou VANILLA)
     * @param existingEffects Lista de efeitos customizados já existentes no item
     * @return true se pode adicionar, false se atingiu o limite
     */
    private boolean checkEnchantmentLimit(Player player, ItemStack item, String enchantType, List<String> existingEffects) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("enchantment-limits.enabled", true)) {
            return true; // Sistema desabilitado, permite tudo
        }
        
        Material material = item.getType();
        String materialName = material.name();
        
        // Normalizar tipo de encantamento
        String typeKey;
        if ("AE".equals(enchantType)) {
            typeKey = "ae";
        } else {
            typeKey = "vanilla";
        }
        
        int maxLimit = -1;
        
        // Primeiro, verificar limites por categoria (prioridade maior)
        String category = getItemCategoryForLimits(material);
        if (category != null) {
            ConfigurationSection categoryLimitsSection = config.getConfigurationSection("enchantment-limits.category-limits");
            if (categoryLimitsSection != null) {
                ConfigurationSection categoryLimits = categoryLimitsSection.getConfigurationSection(category);
                if (categoryLimits != null) {
                    maxLimit = categoryLimits.getInt(typeKey, -1);
                }
            }
        }
        
        // Se não encontrou limite por categoria, verificar por material específico
        if (maxLimit < 0) {
            ConfigurationSection limitsSection = config.getConfigurationSection("enchantment-limits.limits");
            if (limitsSection != null) {
                ConfigurationSection itemLimits = limitsSection.getConfigurationSection(materialName);
                if (itemLimits != null) {
                    maxLimit = itemLimits.getInt(typeKey, -1);
                }
            }
        }
        
        if (maxLimit < 0) {
            return true; // Sem limite configurado para este tipo
        }
        
        // Contar encantamentos existentes do tipo especificado
        int currentCount = 0;
        
        if ("vanilla".equals(typeKey)) {
            // Contar encantamentos vanilla
            if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
                currentCount = item.getItemMeta().getEnchants().size();
            }
        } else {
            // Contar encantamentos customizados (AE)
            for (String effect : existingEffects) {
                String effectUpper = effect.toUpperCase(Locale.ROOT);
                if (effectUpper.startsWith(enchantType + ":")) {
                    currentCount++;
                }
            }
        }
        
        if (currentCount >= maxLimit) {
            // Limite atingido, enviar mensagem de erro
            String messageKey = "enchantment-limits.messages.limit-reached-" + typeKey.toLowerCase(Locale.ROOT);
            String message = config.getString(messageKey, 
                config.getString("enchantment-limits.messages.limit-reached", 
                    "<red>Este item já atingiu o limite de encantamentos <type> (<current>/<max>).</red>"));
            
            message = message.replace("<type>", enchantType)
                            .replace("<current>", String.valueOf(currentCount))
                            .replace("<max>", String.valueOf(maxLimit));
            
            player.sendMessage(MiniMessage.miniMessage().deserialize(message));
            return false;
        }
        
        return true; // Pode adicionar
    }
    
    /**
     * Obtém a categoria específica do item para uso em limites de encantamentos.
     * Retorna categorias como HELMET, CHESTPLATE, LEGGINGS, BOOTS, SWORDS, BOWS, etc.
     * @param material O material do item
     * @return A categoria do item, ou null se não se encaixar em nenhuma categoria
     */
    private String getItemCategoryForLimits(Material material) {
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
    
    private boolean handleGemaCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Uso: /ist gema <setslots|clear> [valor]</red>"));
            return true;
        }

        String gemaSubCommand = args[1].toLowerCase(Locale.ROOT);
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(LanguageManager.getMessage("command.invalid-item"));
            return true;
        }

        // Verificar se o item é rastreável
        if (!StatManager.itemDeveSerRastreado(item)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Este item não pode receber gemas.</red>"));
            return true;
        }

        switch (gemaSubCommand) {
            case "setslots" -> {
                if (args.length < 3) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Uso: /ist gema setslots <número></red>"));
                    return true;
                }

                int slots;
                try {
                    slots = Integer.parseInt(args[2]);
                    if (slots < 0 || slots > 10) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>O número de slots deve estar entre 0 e 10.</red>"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Número inválido: " + args[2] + "</red>"));
                    return true;
                }

                GemaManager.setTotalSlots(item, slots);
                updateItemAndIgnoreEvent(player, item);

                if (slots == 0) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Slots de gema removidos do item.</green>"));
                } else {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>O item agora tem " + slots + " slot(s) de gema.</green>"));
                }
                return true;
            }
            case "clear" -> {
                int totalSlots = GemaManager.getTotalSlots(item);
                if (totalSlots == 0) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>O item não possui slots de gema configurados.</yellow>"));
                    return true;
                }

                GemaManager.limparTodasGemas(item);
                updateItemAndIgnoreEvent(player, item);

                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Todas as gemas foram removidas do item.</green>"));
                return true;
            }
            default -> {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Subcomando inválido: " + gemaSubCommand + "</red>"));
                player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Subcomandos disponíveis: setslots, clear</yellow>"));
                return true;
            }
        }
    }

    private void updateItemAndIgnoreEvent(Player player, ItemStack item) {
        final ItemStatsTracker plugin = ItemStatsTracker.getInstance();
        plugin.getIgnoreArmorChangeEvent().add(player.getUniqueId());

        // Verificar se o item é rastreável antes de definir dono ou atualizar lore
        if (StatManager.itemDeveSerRastreado(item)) {
            if (StatManager.getOriginalOwner(item) == null) {
                StatManager.setOriginalOwner(item, player.getName());
            }
            LoreManager.updateLore(item);
        }

        // Verifica se o item está em um slot de equipamento para evitar erros
        EquipmentSlot slot = findItemSlot(player, item);
        if (slot != null) {
             player.getInventory().setItem(slot, item);
        } else {
            // Se não for um equipamento, provavelmente está na mão
            player.getInventory().setItemInMainHand(item);
        }
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getIgnoreArmorChangeEvent().remove(player.getUniqueId());
        }, 1L);
    }
    
    private EquipmentSlot findItemSlot(Player player, ItemStack item) {
        if (item == null) return null;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        if (mainHand != null && mainHand.isSimilar(item)) return EquipmentSlot.HAND;
        if (offHand != null && offHand.isSimilar(item)) return EquipmentSlot.OFF_HAND;
        if (helmet != null && helmet.isSimilar(item)) return EquipmentSlot.HEAD;
        if (chestplate != null && chestplate.isSimilar(item)) return EquipmentSlot.CHEST;
        if (leggings != null && leggings.isSimilar(item)) return EquipmentSlot.LEGS;
        if (boots != null && boots.isSimilar(item)) return EquipmentSlot.FEET;
        
        // Fallback para a mão principal se não for encontrado em nenhum outro lugar
        if (mainHand != null && mainHand.isSimilar(item)) {
            return EquipmentSlot.HAND;
        }

        return null;
    }

    private void runLevelCommands(Player player, ItemStack item, int level) {
        FileConfiguration levelCfg = plugin.getLevelEffectsConfig();
        if (levelCfg == null) return;
        List<String> commands = levelCfg.getStringList("LEVEL_EFFECTS." + level + ".commands_on_reach");
        if (commands == null || commands.isEmpty()) return;
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName())
                : capitalizeMaterial(item.getType().name());
        for (String raw : commands) {
            String cmd = raw
                    .replace("{player}", player.getName())
                    .replace("{level}", String.valueOf(level))
                    .replace("{item}", itemName);
            
            // Detectar se é um comando de broadcast com MiniMessage
            if (cmd.trim().toLowerCase(Locale.ROOT).startsWith("broadcast ")) {
                String message = cmd.substring("broadcast ".length()).trim();
                // Se contém tags MiniMessage (<gradient>, <bold>, etc.), usar broadcast direto
                if (message.contains("<") && message.contains(">")) {
                    Component component = MiniMessage.miniMessage().deserialize(message);
                    Bukkit.broadcast(component);
                    continue;
                }
            }
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partialMatches(args[0], List.of("set", "add", "reload", "reincarnado", "setarrow", "giverr", "addeffect", "gema", "acessorio", "cleardono", "info", "timer"));
        }

        if (args.length == 2 && "giverr".equalsIgnoreCase(args[0])) {
            // Sugerir jogadores online
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && "giverr".equalsIgnoreCase(args[0])) {
            return partialMatches(args[2], List.of("1", "5", "10"));
        }

        if (args.length == 2 && "addeffect".equalsIgnoreCase(args[0])) {
            return partialMatches(args[1], List.of("AE", "VN"));
        }

        if (args.length == 3 && "addeffect".equalsIgnoreCase(args[0])) {
            if (args[1].equalsIgnoreCase("AE") || args[1].equalsIgnoreCase("VN")) {
                // Sugerir tipos de item
                List<String> itemTypes = List.of("sword", "pickaxe", "axe", "shovel", "hoe", 
                    "bow", "crossbow", "trident", "mace", "helmet", "chestplate", "leggings", "boots");
                return partialMatches(args[2], itemTypes);
            }
        }

        if (args.length == 4 && "addeffect".equalsIgnoreCase(args[0])) {
            if (args[1].equalsIgnoreCase("AE") && sender instanceof Player playerSender && AdvancedEnchantmentsBridge.isAvailable()) {
                // Sugerir encantamentos AE compatíveis com o tipo de item
                List<String> suggestions = AdvancedEnchantmentsBridge.suggestEnchants(playerSender, args[3]);
                if (!suggestions.isEmpty()) {
                    return suggestions.stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toList());
                }
            } else if (args[1].equalsIgnoreCase("VN")) {
                // Sugerir encantamentos vanilla compatíveis com o tipo de item
                List<String> vanillaEnchants = getVanillaEnchantsForItemType(args[2]);
                return partialMatches(args[3], vanillaEnchants);
            }
        }

        if (args.length == 5 && "addeffect".equalsIgnoreCase(args[0])) {
            if (args[1].equalsIgnoreCase("AE") && sender instanceof Player playerSender && AdvancedEnchantmentsBridge.isAvailable()) {
                // Sugerir níveis para encantamentos AE
                List<String> levels = AdvancedEnchantmentsBridge.suggestLevels(playerSender, args[3].toLowerCase(Locale.ROOT));
                if (!levels.isEmpty()) {
                    return levels;
                }
            } else if (args[1].equalsIgnoreCase("VN")) {
                // Sugerir níveis para encantamentos vanilla (até 30.000, exceto encantamentos de nível único)
                String enchantName = args[3].toUpperCase(Locale.ROOT);
                if (isSingleLevelEnchantment(enchantName)) {
                    // Encantamentos de nível único (como Mending, Infinity, etc.)
                    return partialMatches(args[4], List.of("1"));
                } else {
                    // Encantamentos que podem ir até 30.000
                    List<String> commonLevels = new ArrayList<>();
                    // Adicionar níveis comuns para facilitar
                    for (int i = 1; i <= 10; i++) {
                        commonLevels.add(String.valueOf(i));
                    }
                    // Adicionar alguns valores altos comuns
                    commonLevels.addAll(List.of("50", "100", "500", "1000", "5000", "10000", "30000"));
                    return partialMatches(args[4], commonLevels);
                }
            }
        }

        if (args.length == 2 && ("set".equalsIgnoreCase(args[0]) || "add".equalsIgnoreCase(args[0]))) {
            return partialMatches(args[1], StatManager.getRegisteredStats());
        }

        if (args.length == 3 && ("set".equalsIgnoreCase(args[0]) || "add".equalsIgnoreCase(args[0]))) {
            return partialMatches(args[2], List.of("1", "10", "100"));
        }

        if (args.length == 2 && "timer".equalsIgnoreCase(args[0])) {
            return partialMatches(args[1], Arrays.asList("60", "300", "3600", "86400"));
        }
        if (args.length == 2 && "setarrow".equalsIgnoreCase(args[0])) {
            return partialMatches(args[1], List.of("fire", "explosive"));
        }

        return Collections.emptyList();
    }

    private List<String> partialMatches(String token, List<String> options) {
        if (token == null || token.isEmpty()) {
            return new ArrayList<>(options);
        }

        String lower = token.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }

    private ParsedCustomEffect parseCustomEffect(String rawEffect) {
        if (rawEffect == null || rawEffect.trim().isEmpty()) {
            return null;
        }
        String sanitized = rawEffect.trim().replace("/", ":").replace(" ", ":").toUpperCase(Locale.ROOT);
        while (sanitized.contains("::")) {
            sanitized = sanitized.replace("::", ":");
        }
        if (sanitized.startsWith(":")) {
            sanitized = sanitized.substring(1);
        }
        if (sanitized.endsWith(":")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        if (sanitized.isEmpty()) {
            return null;
        }

        String[] tokens = sanitized.split(":");
        if (tokens.length < 2) {
            return null; // precisa ao menos de plugin + efeito
        }

        int providedLevel = -1;
        String lastToken = tokens[tokens.length - 1];
        if (lastToken.matches("\\d+")) {
            providedLevel = Integer.parseInt(lastToken);
            tokens = Arrays.copyOf(tokens, tokens.length - 1);
        }

        String baseKey = String.join(":", tokens);
        return new ParsedCustomEffect(baseKey, tokens, providedLevel);
    }

    private record ParsedCustomEffect(String baseKey, String[] tokens, int providedLevel) {
        private ParsedCustomEffect {
            Objects.requireNonNull(baseKey, "baseKey");
            Objects.requireNonNull(tokens, "tokens");
        }

        String pluginId() {
            return tokens.length > 0 ? tokens[0] : "";
        }
    }

    private void applyExternalCustomEffect(Player player, ParsedCustomEffect effect, int level, ConfigurationSection effectSection, String actualBaseKey) {
        if (player == null || effect == null || level <= 0) {
            return;
        }

        FileConfiguration enchantmentsConfig = plugin.getEnchantmentsConfig();
        if (effectSection == null && enchantmentsConfig != null) {
            effectSection = enchantmentsConfig.getConfigurationSection("CUSTOM_EFFECTS." + (actualBaseKey != null ? actualBaseKey : effect.baseKey()));
        }
        if (effectSection == null) {
            return;
        }

        String applyCommand = effectSection.getString("apply-command");
        if (applyCommand == null || applyCommand.isEmpty()) {
            return;
        }

        // Extrai o nome do encantamento do actualBaseKey (ex: AE:VEIN_MINER -> VEIN_MINER)
        String actualEffectName = actualBaseKey != null && actualBaseKey.contains(":") 
            ? actualBaseKey.substring(actualBaseKey.indexOf(":") + 1)
            : (effect.tokens().length > 1 ? effect.tokens()[1] : "");

        Map<String, String> replacements = new HashMap<>();
        replacements.put("{player}", player.getName());
        replacements.put("{base_key}", actualBaseKey != null ? actualBaseKey : effect.baseKey());
        replacements.put("{base_key_lower}", (actualBaseKey != null ? actualBaseKey : effect.baseKey()).toLowerCase(Locale.ROOT));
        replacements.put("{plugin}", effect.pluginId());
        replacements.put("{plugin_lower}", effect.pluginId().toLowerCase(Locale.ROOT));
        replacements.put("{effect}", actualEffectName);
        replacements.put("{effect_lower}", actualEffectName.toLowerCase(Locale.ROOT));
        replacements.put("{raw_effect}", (actualBaseKey != null ? actualBaseKey : effect.baseKey()) + ":" + level);
        replacements.put("{level}", String.valueOf(level));
        replacements.put("{level_provided}", effect.providedLevel() > 0 ? String.valueOf(effect.providedLevel()) : String.valueOf(level));

        String[] tokens = effect.tokens();
        for (int i = 0; i < tokens.length; i++) {
            replacements.put("{part" + i + "}", tokens[i]);
            replacements.put("{part" + i + "_lower}", tokens[i].toLowerCase(Locale.ROOT));
        }

        String command = applyCommand;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            command = command.replace(entry.getKey(), entry.getValue());
        }

        command = command.replaceAll("\\s+", " ").trim();
        if (command.isEmpty()) {
            return;
        }

        boolean hasLeadingSlash = command.startsWith("/");
        String finalCommand = hasLeadingSlash ? command.substring(1) : command;

        if ("AE".equalsIgnoreCase(effect.pluginId())) {
            player.performCommand(finalCommand);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        }
    }

    private void showReincarnadoProgress(Player player, ItemStack item) {
        int currentReincarnadoLevel = StatManager.getReincarnadoLevel(item);
        int maxReincarnadoLevel = plugin.getConfig().getInt("reincarnado.max-level", 100);

        if (currentReincarnadoLevel >= maxReincarnadoLevel) {
            Component maxLevelMsg = LanguageManager.getMessage("command.info.reincarnado-max-level");
            if (maxLevelMsg != null && !maxLevelMsg.equals(Component.empty())) {
                player.sendMessage(maxLevelMsg);
            }
            return;
        }

        List<StatManager.ReincarnadoCriterion> criteria = StatManager.getReincarnadoCriteria(item);
        if (criteria.isEmpty()) {
            Component noCriteriaMsg = LanguageManager.getMessage("command.info.reincarnado-no-criteria");
            if (noCriteriaMsg != null && !noCriteriaMsg.equals(Component.empty())) {
                player.sendMessage(noCriteriaMsg);
            }
            return;
        }

        Component headerMsg = LanguageManager.getMessage("command.info.reincarnado-header", Placeholder.unparsed("level", String.valueOf(currentReincarnadoLevel + 1)));
        if (headerMsg != null && !headerMsg.equals(Component.empty())) {
            player.sendMessage(headerMsg);
        }
        
        for (StatManager.ReincarnadoCriterion criterion : criteria) {
            int currentStatValue = StatManager.getStat(item, criterion.getStatType());
            double requiredValueForNextLevel = criterion.getRequiredValuePerLevel() * (currentReincarnadoLevel + 1);

            if (requiredValueForNextLevel <= 0) {
                 player.sendMessage(LanguageManager.getMessage("command.info.reincarnado-criterion-complete", 
                    Placeholder.unparsed("display_name", criterion.getDisplayName()),
                    Placeholder.unparsed("required", "0")));
                continue;
            }

            double progressPercentage = Math.min(1.0, Math.max(0.0, currentStatValue / requiredValueForNextLevel));

            Component progressMessage = LanguageManager.getMessage("command.info.reincarnado-criterion-progress",
                Placeholder.unparsed("display_name", criterion.getDisplayName()),
                Placeholder.unparsed("current", String.valueOf(currentStatValue)),
                Placeholder.unparsed("required", String.valueOf((int) requiredValueForNextLevel)),
                Placeholder.component("progress_bar", MiniMessage.miniMessage().deserialize(StatManager.createProgressBar(progressPercentage, 20))),
                Placeholder.unparsed("percent", String.format("%.1f", Math.min(100.0, progressPercentage * 100)))
            );
            
            if (progressMessage != null && !progressMessage.equals(Component.empty())) {
                player.sendMessage(progressMessage);
            }
        }
    }

    private void showEnchantmentUpgradeProgress(Player player, ItemStack item) {
        FileConfiguration config = plugin.getConfig();
        FileConfiguration enchantmentsConfig = plugin.getEnchantmentsConfig();
        
        String itemCategory = StatManager.getItemCategory(item.getType());
        ConfigurationSection categoryUpgrades = config.getConfigurationSection("enchantment-upgrades." + itemCategory);
        if (categoryUpgrades == null) {
            categoryUpgrades = config.getConfigurationSection("enchantment-upgrades.default");
        }

        if (categoryUpgrades == null) {
            player.sendMessage(LanguageManager.getMessage("command.info.enchant-upgrade-no-criteria"));
            return;
        }

        boolean hasUpgrades = false;
        List<Component> progressComponents = new ArrayList<>();

        for (String enchantKey : categoryUpgrades.getKeys(false)) {
            @SuppressWarnings("deprecation")
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey.toLowerCase(Locale.ROOT)));
            if (enchantment == null) continue;

            int currentLevel = item.getEnchantmentLevel(enchantment);
            int maxLevel = categoryUpgrades.getInt(enchantKey + ".max-level", currentLevel);

            if (currentLevel < maxLevel) {
                hasUpgrades = true;
                List<StatManager.EnchantmentUpgradeProgress> progressList = StatManager.getEnchantmentUpgradeProgress(item, enchantment);
                if (!progressList.isEmpty()) {
                    double totalProgress = progressList.stream().mapToDouble(StatManager.EnchantmentUpgradeProgress::getProgress).sum();
                    double averageProgress = totalProgress / progressList.size();
                    
                    String enchantDisplayName = enchantmentsConfig.getString("ENCHANTMENTS." + enchantKey.toUpperCase(Locale.ROOT) + ".display-name", LoreManager.capitalizeFirstLetter(enchantment.getKey().getKey().replace("_", " ")));
                    
                    progressComponents.add(LanguageManager.getMessage("command.info.enchant-upgrade-progress",
                        Placeholder.unparsed("display_name", enchantDisplayName),
                        Placeholder.component("progress_bar", MiniMessage.miniMessage().deserialize(StatManager.createProgressBar(averageProgress, 15))),
                        Placeholder.unparsed("percent", String.format("%.0f", averageProgress * 100))
                    ));
                }
            }
        }

        if (!hasUpgrades) {
             player.sendMessage(LanguageManager.getMessage("command.info.enchant-upgrade-max-level"));
        } else if (!progressComponents.isEmpty()) {
            player.sendMessage(LanguageManager.getMessage("command.info.enchant-upgrade-header"));
            progressComponents.forEach(player::sendMessage);
        }
    }

    private void handleRenameOnReincarnado(Player player, ItemStack item, int newLevel) {
        FileConfiguration cfg = plugin.getConfig();
        boolean enabled = cfg.getBoolean("reincarnado.rename.enabled", false);
        if (!enabled) return;

        String nameTemplate = cfg.getString("reincarnado.rename.name-template", "<white><base_name> <gray>L<level>");
        String useCommandPath = "reincarnado.rename.use-command";
        boolean useCommand = cfg.getBoolean(useCommandPath, true);
        String baseName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName())
                : capitalizeMaterial(item.getType().name());

        String newName = nameTemplate
                .replace("<base_name>", baseName)
                .replace("<level>", String.valueOf(newLevel));

        if (useCommand) {
            String cmdTemplate = cfg.getString("reincarnado.rename.command-template", "itemeditar renomear {player} {new_name}");
            String cmd = cmdTemplate
                    .replace("{player}", player.getName())
                    .replace("{new_name}", newName.replace("<", "").replace(">", "")); // se o comando usa & e não MiniMessage
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize(newName));
                item.setItemMeta(meta);
            }
        }
    }

    private void handleItemMestre(Player player, ItemStack item, int newLevel) {
        FileConfiguration mestre = plugin.getItemMestreConfig();
        if (mestre == null) return;
        int mestreLevel = mestre.getInt("item_mestre.level", 1000);
        if (newLevel != mestreLevel) return;

        // Broadcast
        String bc = mestre.getString("item_mestre.broadcast_message", "<gold><bold>ITEM MESTRE!</bold> <gray><player> dominou <item>");
        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName())
                : capitalizeMaterial(item.getType().name());
        Component msg = MiniMessage.miniMessage().deserialize(bc
                .replace("<player>", player.getName())
                .replace("<item>", itemName));
        Bukkit.getServer().broadcast(msg);

        // Comandos extras
        List<String> cmds = mestre.getStringList("item_mestre.extra_commands");
        for (String raw : cmds) {
            String cmd = raw
                    .replace("{player}", player.getName())
                    .replace("{item}", itemName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        // Marca PDC e adiciona badge na lore
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "is_item_mestre"), PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
            updateItemAndIgnoreEvent(player, item);
        }

        // Envia Title bonito usando Adventure API
        Component title = Component.text("ITEM MESTRE!").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
        Component subtitle = Component.text("Você dominou seu item!").color(NamedTextColor.YELLOW);
        player.showTitle(Title.title(title, subtitle));

        // Renome especial do Mestre, se definido
        if (mestre.getBoolean("item_mestre.rename.enabled", false)) {
            String newName = mestre.getString("item_mestre.rename.name-template", "<gradient:gold:yellow><bold>ITEM MESTRE</bold> <gray><base_name>")
                    .replace("<base_name>", itemName);
            if (mestre.getBoolean("item_mestre.rename.use-command", true)) {
                String cmdTemplate = mestre.getString("item_mestre.rename.command-template", "itemeditar renomear {player} {new_name}");
                String cmd = cmdTemplate
                        .replace("{player}", player.getName())
                        .replace("{new_name}", newName.replace("<", "").replace(">", ""));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else {
                ItemMeta meta2 = item.getItemMeta();
                if (meta2 != null) {
                    meta2.displayName(MiniMessage.miniMessage().deserialize(newName));
                    item.setItemMeta(meta2);
                }
            }
        }
    }

    private String capitalizeMaterial(String name) {
        String lower = name.toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = lower.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
    
    /**
     * Verifica se o tipo de material corresponde ao tipo de item especificado.
     * @param material O material do item
     * @param itemType O tipo de item especificado (sword, pickaxe, etc.)
     * @return true se corresponde, false caso contrário
     */
    private boolean isItemTypeMatch(Material material, String itemType) {
        if (itemType == null || itemType.isEmpty()) {
            return true; // Sem filtro, aceita tudo
        }
        
        String materialName = material.name().toUpperCase(Locale.ROOT);
        String itemTypeUpper = itemType.toUpperCase(Locale.ROOT);
        
        // Mapeamento de tipos de item para padrões de material
        switch (itemTypeUpper) {
            case "SWORD":
                return materialName.endsWith("_SWORD");
            case "PICKAXE":
                return materialName.endsWith("_PICKAXE");
            case "AXE":
                return materialName.endsWith("_AXE");
            case "SHOVEL":
                return materialName.endsWith("_SHOVEL");
            case "HOE":
                return materialName.endsWith("_HOE");
            case "BOW":
                return material == Material.BOW;
            case "CROSSBOW":
                return material == Material.CROSSBOW;
            case "TRIDENT":
                return material == Material.TRIDENT;
            case "MACE":
                return material == Material.MACE;
            case "HELMET":
                return materialName.endsWith("_HELMET");
            case "CHESTPLATE":
                return materialName.endsWith("_CHESTPLATE");
            case "LEGGINGS":
                return materialName.endsWith("_LEGGINGS");
            case "BOOTS":
                return materialName.endsWith("_BOOTS");
            default:
                // Tentar correspondência direta
                return materialName.contains(itemTypeUpper);
        }
    }
    
    /**
     * Obtém os encantamentos vanilla compatíveis com um tipo de item.
     * @param itemType O tipo de item
     * @return Lista de nomes de encantamentos vanilla compatíveis
     */
    private List<String> getVanillaEnchantsForItemType(String itemType) {
        List<String> enchants = new ArrayList<>();
        if (itemType == null || itemType.isEmpty()) {
            return enchants;
        }
        
        String itemTypeUpper = itemType.toUpperCase(Locale.ROOT);
        
        // Encantamentos comuns para espadas
        if (itemTypeUpper.equals("SWORD")) {
            enchants.addAll(List.of("SHARPNESS", "SMITE", "BANE_OF_ARTHROPODS", "KNOCKBACK", 
                "FIRE_ASPECT", "LOOTING", "SWEEPING_EDGE", "UNBREAKING", "MENDING"));
        }
        // Encantamentos para picaretas
        else if (itemTypeUpper.equals("PICKAXE")) {
            enchants.addAll(List.of("EFFICIENCY", "FORTUNE", "SILK_TOUCH", "UNBREAKING", "MENDING"));
        }
        // Encantamentos para machados
        else if (itemTypeUpper.equals("AXE")) {
            enchants.addAll(List.of("EFFICIENCY", "FORTUNE", "SILK_TOUCH", "UNBREAKING", "MENDING", "SHARPNESS"));
        }
        // Encantamentos para pás
        else if (itemTypeUpper.equals("SHOVEL")) {
            enchants.addAll(List.of("EFFICIENCY", "FORTUNE", "SILK_TOUCH", "UNBREAKING", "MENDING"));
        }
        // Encantamentos para enxadas
        else if (itemTypeUpper.equals("HOE")) {
            enchants.addAll(List.of("EFFICIENCY", "FORTUNE", "SILK_TOUCH", "UNBREAKING", "MENDING"));
        }
        // Encantamentos para arcos
        else if (itemTypeUpper.equals("BOW")) {
            enchants.addAll(List.of("POWER", "PUNCH", "FLAME", "INFINITY", "UNBREAKING", "MENDING"));
        }
        // Encantamentos para bestas
        else if (itemTypeUpper.equals("CROSSBOW")) {
            enchants.addAll(List.of("MULTISHOT", "PIERCING", "QUICK_CHARGE", "UNBREAKING", "MENDING"));
        }
        // Encantamentos para tridentes
        else if (itemTypeUpper.equals("TRIDENT")) {
            enchants.addAll(List.of("IMPALING", "RIPTIDE", "LOYALTY", "CHANNELING", "UNBREAKING", "MENDING"));
        }
        // Encantamentos para armaduras
        else if (itemTypeUpper.equals("HELMET") || itemTypeUpper.equals("CHESTPLATE") || 
                 itemTypeUpper.equals("LEGGINGS") || itemTypeUpper.equals("BOOTS")) {
            enchants.addAll(List.of("PROTECTION", "FIRE_PROTECTION", "BLAST_PROTECTION", 
                "PROJECTILE_PROTECTION", "RESPIRATION", "AQUA_AFFINITY", "THORNS", 
                "DEPTH_STRIDER", "SOUL_SPEED", "SWIFT_SNEAK", "UNBREAKING", "MENDING"));
        }
        
        return enchants;
    }
    
    /**
     * Verifica se um encantamento vanilla é de nível único (como Mending, Infinity, etc.).
     * @param enchantName Nome do encantamento em maiúsculas
     * @return true se for de nível único, false caso contrário
     */
    private boolean isSingleLevelEnchantment(String enchantName) {
        if (enchantName == null || enchantName.isEmpty()) {
            return false;
        }
        
        // Encantamentos que são sempre nível único
        List<String> singleLevelEnchants = List.of(
            "MENDING", "INFINITY", "SILK_TOUCH", "AQUA_AFFINITY", 
            "BINDING_CURSE", "VANISHING_CURSE", "FLAME", "CHANNELING"
        );
        
        return singleLevelEnchants.contains(enchantName);
    }
}
