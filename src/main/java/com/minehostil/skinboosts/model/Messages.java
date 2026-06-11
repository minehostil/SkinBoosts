package com.minehostil.skinboosts.model;

import com.minehostil.skinboosts.SkinBoostsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;

/**
 * Reads all messages from config.yml and applies placeholder replacement.
 * Reload-safe: call reload() after plugin.reloadConfig().
 */
public class Messages {

    private final SkinBoostsPlugin plugin;
    private FileConfiguration cfg;

    public Messages(SkinBoostsPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    public void reload() {
        this.cfg = plugin.getConfig();
    }

    /**
     * Gets a message from config, colorizes it, and replaces placeholders.
     * Placeholders are passed as key-value pairs: get("key", "cmd", "1001", "name", "Fuego")
     */
    public String get(String key, String... placeholders) {
        String prefix = colorize(cfg.getString("prefix", "&8[&6SkinBoosts&8] &r"));
        String raw = cfg.getString("messages." + key, "&cMissing message: " + key);
        String msg = colorize(raw);

        // Replace placeholders in pairs: {key} -> value
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }

        return prefix + msg;
    }

    /** Get a message without the plugin prefix (for list entries, etc.) */
    public String getRaw(String key, String... placeholders) {
        String raw = cfg.getString("messages." + key, "&cMissing message: " + key);
        String msg = colorize(raw);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return msg;
    }

    private String colorize(String s) {
        return s.replace("&", "§");
    }
}