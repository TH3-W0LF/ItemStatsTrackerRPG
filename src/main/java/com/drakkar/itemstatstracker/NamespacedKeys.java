package com.drakkar.itemstatstracker;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class NamespacedKeys {

    public static NamespacedKey ITEM_UUID;
    public static PersistentDataType<String, String> UUID_TYPE = PersistentDataType.STRING;

    public static void init(JavaPlugin plugin) {
        ITEM_UUID = new NamespacedKey(plugin, "item_uuid");
    }
}

