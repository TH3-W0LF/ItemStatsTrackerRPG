package com.drakkar.itemstatstracker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LanguageManager {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static FileConfiguration messagesConfig;

    public static void loadMessages(ItemStatsTracker plugin) {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Carregar os padrões do JAR para garantir que todas as chaves existam
        InputStream defaultConfigStream = plugin.getResource("messages.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    public static String getRawString(String path) {
        return messagesConfig.getString(path, "§cMissing message: " + path);
    }

    public static Component getMessage(String path, TagResolver... resolvers) {
        String message = messagesConfig.getString(path, "<red>Missing message: " + path + "</red>");
        if (message == null || message.isEmpty()) {
            return Component.text("");
        }
        return MINI_MESSAGE.deserialize(message, resolvers);
    }
    
    public static String getLegacyMessage(String path, TagResolver... resolvers) {
        String message = messagesConfig.getString(path, "§cMissing message: " + path);
        if (message == null || message.isEmpty()) {
            return "";
        }
        Component component = MINI_MESSAGE.deserialize(message, resolvers);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
