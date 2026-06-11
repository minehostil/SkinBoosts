package com.minehostil.skinboosts.manager;

import com.minehostil.skinboosts.SkinBoostsPlugin;
import com.minehostil.skinboosts.model.SkinBoostData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SkinBoostManager {

    private final SkinBoostsPlugin plugin;
    private final Map<Integer, SkinBoostData> boosts = new HashMap<>();

    // 0.0 means no limit for that type
    private double maxEssence;
    private double maxMoney;
    private double maxToolXp;
    private double maxCyberXp;

    public SkinBoostManager(SkinBoostsPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------
    //  Load / Reload
    // ---------------------------------------------------------------

    public void loadFromConfig() {
        boosts.clear();
        FileConfiguration cfg = plugin.getConfig();

        // Load limits first
        ConfigurationSection maxSec = cfg.getConfigurationSection("max-multipliers");
        maxEssence = maxSec != null ? maxSec.getDouble("essence",  0.0) : 0.0;
        maxMoney   = maxSec != null ? maxSec.getDouble("money",    0.0) : 0.0;
        maxToolXp  = maxSec != null ? maxSec.getDouble("tool-xp",  0.0) : 0.0;
        maxCyberXp = maxSec != null ? maxSec.getDouble("cyber-xp", 0.0) : 0.0;

        ConfigurationSection skinsSection = cfg.getConfigurationSection("skins");
        if (skinsSection == null) {
            plugin.getLogger().warning("No 'skins' section found in config.yml!");
            return;
        }

        for (String key : skinsSection.getKeys(false)) {
            int cmd;
            try {
                cmd = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid CustomModelData key in config: '" + key + "' — skipping.");
                continue;
            }

            ConfigurationSection sec = skinsSection.getConfigurationSection(key);
            if (sec == null) continue;

            String name = sec.getString("name", "&7Unknown Skin");
            ConfigurationSection mults = sec.getConfigurationSection("multipliers");

            double essence = mults != null ? mults.getDouble("essence",  1.0) : 1.0;
            double money   = mults != null ? mults.getDouble("money",    1.0) : 1.0;
            double toolXp  = mults != null ? mults.getDouble("tool-xp",  1.0) : 1.0;
            double cyberXp = mults != null ? mults.getDouble("cyber-xp", 1.0) : 1.0;

            // Clamp to configured maximums
            essence = clamp("essence", cmd, essence, maxEssence);
            money   = clamp("money",   cmd, money,   maxMoney);
            toolXp  = clamp("tool-xp", cmd, toolXp,  maxToolXp);
            cyberXp = clamp("cyber-xp",cmd, cyberXp, maxCyberXp);

            boosts.put(cmd, new SkinBoostData(cmd, name, essence, money, toolXp, cyberXp));
        }

        plugin.getLogger().info("Loaded " + boosts.size() + " skin boost(s). "
                + "Limits — essence:" + fmtLimit(maxEssence)
                + " money:" + fmtLimit(maxMoney)
                + " tool-xp:" + fmtLimit(maxToolXp)
                + " cyber-xp:" + fmtLimit(maxCyberXp));
    }

    // ---------------------------------------------------------------
    //  Runtime set / remove
    // ---------------------------------------------------------------

    /**
     * Registers or updates a skin boost. Values are clamped to configured limits.
     * Returns the final SkinBoostData after clamping (may differ from input).
     */
    public SkinBoostData setSkin(int cmd, String name, double essence, double money,
                                 double toolXp, double cyberXp) {
        essence = clamp("essence",  cmd, essence, maxEssence);
        money   = clamp("money",    cmd, money,   maxMoney);
        toolXp  = clamp("tool-xp",  cmd, toolXp,  maxToolXp);
        cyberXp = clamp("cyber-xp", cmd, cyberXp, maxCyberXp);

        SkinBoostData data = new SkinBoostData(cmd, name, essence, money, toolXp, cyberXp);
        boosts.put(cmd, data);

        String path = "skins." + cmd;
        FileConfiguration cfg = plugin.getConfig();
        cfg.set(path + ".name", name);
        cfg.set(path + ".multipliers.essence",  essence);
        cfg.set(path + ".multipliers.money",    money);
        cfg.set(path + ".multipliers.tool-xp",  toolXp);
        cfg.set(path + ".multipliers.cyber-xp", cyberXp);
        plugin.saveConfig();

        return data;
    }

    public boolean removeSkin(int cmd) {
        if (!boosts.containsKey(cmd)) return false;
        boosts.remove(cmd);
        plugin.getConfig().set("skins." + cmd, null);
        plugin.saveConfig();
        return true;
    }

    // ---------------------------------------------------------------
    //  Queries
    // ---------------------------------------------------------------

    public SkinBoostData getBoost(int customModelData) {
        return boosts.get(customModelData);
    }

    public boolean hasBoost(int customModelData) {
        return boosts.containsKey(customModelData);
    }

    public Collection<SkinBoostData> getAllBoosts() {
        return Collections.unmodifiableCollection(boosts.values());
    }

    // Expose limits so the command can show them
    public SkinBoostsPlugin getPlugin() { return plugin; }
    public double getMaxEssence()  { return maxEssence; }
    public double getMaxMoney()    { return maxMoney; }
    public double getMaxToolXp()   { return maxToolXp; }
    public double getMaxCyberXp()  { return maxCyberXp; }

    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------

    /**
     * Clamps value to max. If max <= 0 the limit is disabled.
     * Logs a warning if the value was clamped.
     */
    private double clamp(String type, int cmd, double value, double max) {
        if (max <= 0.0 || value <= max) return value;
        plugin.getLogger().warning("CMD " + cmd + " — " + type + " multiplier " + value
                + " exceeds max " + max + ", clamping.");
        return max;
    }

    private String fmtLimit(double max) {
        return max <= 0.0 ? "unlimited" : String.valueOf(max);
    }
}