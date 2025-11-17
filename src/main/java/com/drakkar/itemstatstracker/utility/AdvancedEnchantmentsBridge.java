package com.drakkar.itemstatstracker.utility;

import com.drakkar.itemstatstracker.ItemStatsTracker;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Integração leve com o plugin AdvancedEnchantments, usando tab-complete do próprio comando
 * para descobrir encantamentos e níveis válidos sem depender do JAR em tempo de compilação.
 */
public final class AdvancedEnchantmentsBridge {

    private static final String PLUGIN_NAME = "AdvancedEnchantments";
    private static final String COMMAND_LABEL = "ae";

    private AdvancedEnchantmentsBridge() {
        throw new IllegalStateException("Utility class");
    }

    private static PluginCommand getAeCommand() {
        return Bukkit.getPluginCommand(COMMAND_LABEL);
    }

    private static boolean isPluginActive() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        return plugin != null && plugin.isEnabled();
    }

    public static boolean isAvailable() {
        return isPluginActive() && getAeCommand() != null;
    }

    public static List<String> suggestEnchants(Player player, String partial) {
        if (!isAvailable() || player == null) {
            return Collections.emptyList();
        }
        PluginCommand command = getAeCommand();
        if (command == null) {
            return Collections.emptyList();
        }
        String sanitized = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
        String[] args = sanitized.isEmpty()
            ? new String[]{"enchant", ""}
            : new String[]{"enchant", sanitized};
        return safeTabComplete(command, player, args)
            .stream()
            .map(s -> s.toLowerCase(Locale.ROOT))
            .collect(Collectors.toList());
    }

    public static List<String> suggestLevels(Player player, String enchantIdLower) {
        if (!isAvailable() || player == null || enchantIdLower == null || enchantIdLower.isEmpty()) {
            return Collections.emptyList();
        }
        PluginCommand command = getAeCommand();
        if (command == null) {
            return Collections.emptyList();
        }
        String[] args = new String[]{"enchant", enchantIdLower.toLowerCase(Locale.ROOT), ""};
        return safeTabComplete(command, player, args);
    }

    public static OptionalInt resolveHighestLevel(Player player, String enchantIdLower) {
        return suggestLevels(player, enchantIdLower).stream()
            .filter(s -> s.matches("\\d+"))
            .mapToInt(Integer::parseInt)
            .max();
    }

    private static List<String> safeTabComplete(PluginCommand command, Player player, String[] args) {
        try {
            List<String> suggestions = command.tabComplete(player, COMMAND_LABEL, args);
            return suggestions != null ? suggestions : Collections.emptyList();
        } catch (Throwable throwable) {
            ItemStatsTracker.getInstance().getLogger()
                .log(Level.FINE, "Falha ao consultar tab-complete do AdvancedEnchantments", throwable);
            return Collections.emptyList();
        }
    }
}

