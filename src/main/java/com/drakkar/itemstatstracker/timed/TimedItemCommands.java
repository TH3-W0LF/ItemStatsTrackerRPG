package com.drakkar.itemstatstracker.timed;

import com.drakkar.itemstatstracker.ItemStatsTracker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comandos para gerenciar itens temporizados
 */
public class TimedItemCommands implements CommandExecutor, TabCompleter {
    
    private final ItemStatsTracker plugin;
    private final TimedItemManager manager;
    
    public TimedItemCommands(ItemStatsTracker plugin, TimedItemManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        if (subCommand.equals("give")) {
            return handleGive(sender, args);
        } else if (subCommand.equals("reload")) {
            return handleReload(sender);
        } else {
            sendHelp(sender);
            return true;
        }
    }
    
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("itemstatstracker.timed.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUso: /timed give <player> <seconds> [material] [amount]");
            return true;
        }
        
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cJogador não encontrado: " + args[1]);
            return true;
        }
        
        long seconds;
        try {
            seconds = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cTempo inválido: " + args[2]);
            return true;
        }
        
        long maxDuration = plugin.getConfig().getLong("timed-items.limits.max_duration_seconds", 86400);
        if (seconds > maxDuration) {
            sender.sendMessage("§cO tempo máximo permitido é " + maxDuration + " segundos.");
            return true;
        }
        
        Material material = Material.DIAMOND;
        int amount = 1;
        
        if (args.length >= 4) {
            try {
                material = Material.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage("§cMaterial inválido: " + args[3]);
                return true;
            }
        }
        
        if (args.length >= 5) {
            try {
                amount = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cQuantidade inválida: " + args[4]);
                return true;
            }
        }
        
        ItemStack item = new ItemStack(material, amount);
        manager.giveTimedItem(target, item, seconds);
        
        sender.sendMessage("§aItem temporizado dado para " + target.getName() + " por " + seconds + " segundos.");
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("itemstatstracker.timed.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.");
            return true;
        }
        
        plugin.reloadConfig();
        sender.sendMessage("§aConfiguração de itens temporizados recarregada.");
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Comandos de Itens Temporizados ===");
        sender.sendMessage("§e/timed give <player> <seconds> [material] [amount]");
        sender.sendMessage("§7  - Dá um item temporizado para um jogador");
        sender.sendMessage("§e/timed reload");
        sender.sendMessage("§7  - Recarrega a configuração");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("itemstatstracker.timed.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("give", "reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // Sugerir nomes de jogadores
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("60", "300", "3600", "86400");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            // Sugerir materiais comuns
            return Arrays.asList("DIAMOND", "IRON_INGOT", "GOLD_INGOT", "EMERALD");
        }
        
        return new ArrayList<>();
    }
}

