package com.minehostil.skinboosts.manager;

import com.minehostil.skinboosts.SkinBoostsPlugin;
import com.minehostil.skinboosts.model.SkinBoostData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SkinBoostManager {

    private final SkinBoostsPlugin plugin;

    // ------------------------------------------------------------------
    //  Registro GLOBAL (definiciones de skins / fallback) — igual que antes
    // ------------------------------------------------------------------
    private final Map<Integer, SkinBoostData> boosts = new HashMap<>();

    // ------------------------------------------------------------------
    //  Registro POR JUGADOR (nuevo): uuid -> (cmd -> boost personal)
    //  Prioridad de resolución: personal > global
    // ------------------------------------------------------------------
    private final Map<UUID, Map<Integer, SkinBoostData>> playerBoosts = new ConcurrentHashMap<>();

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

        plugin.getLogger().info("Loaded " + boosts.size() + " global skin boost(s). "
                + "Limits — essence:" + fmtLimit(maxEssence)
                + " money:" + fmtLimit(maxMoney)
                + " tool-xp:" + fmtLimit(maxToolXp)
                + " cyber-xp:" + fmtLimit(maxCyberXp));

        // Re-clamp personal boosts already in memory against (possibly new) limits
        for (Map<Integer, SkinBoostData> map : playerBoosts.values()) {
            for (Map.Entry<Integer, SkinBoostData> e : map.entrySet()) {
                SkinBoostData d = e.getValue();
                e.setValue(new SkinBoostData(
                        e.getKey(), d.getName(),
                        clamp("essence",  e.getKey(), d.getEssenceMultiplier(), maxEssence),
                        clamp("money",    e.getKey(), d.getMoneyMultiplier(),   maxMoney),
                        clamp("tool-xp",  e.getKey(), d.getToolXpMultiplier(),  maxToolXp),
                        clamp("cyber-xp", e.getKey(), d.getCyberXpMultiplier(), maxCyberXp)));
            }
        }
    }

    // ---------------------------------------------------------------
    //  Runtime set / remove — GLOBAL (igual que antes)
    // ---------------------------------------------------------------

    /**
     * Registers or updates a GLOBAL skin boost. Values are clamped to configured limits.
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
    //  POR JUGADOR — NUEVO
    // ---------------------------------------------------------------

    /**
     * Resuelve el boost para un jugador y CMD concreto.
     * Prioridad: boost personal del jugador > boost global del config.
     */
    public SkinBoostData getBoost(UUID playerId, int customModelData) {
        SkinBoostData personal = getPlayerBoosts(playerId).get(customModelData);
        return personal != null ? personal : boosts.get(customModelData);
    }

    /**
     * Registra o actualiza el boost PERSONAL de un jugador para un CMD.
     * No toca el registro global. Valores con clamp a los límites configurados.
     * Devuelve el SkinBoostData final tras el clamp.
     */
    public SkinBoostData setPlayerBoost(UUID playerId, int cmd, String name, double essence, double money,
                                        double toolXp, double cyberXp) {
        essence = clamp("essence",  cmd, essence, maxEssence);
        money   = clamp("money",    cmd, money,   maxMoney);
        toolXp  = clamp("tool-xp",  cmd, toolXp,  maxToolXp);
        cyberXp = clamp("cyber-xp", cmd, cyberXp, maxCyberXp);

        SkinBoostData data = new SkinBoostData(cmd, name, essence, money, toolXp, cyberXp);
        getPlayerBoosts(playerId).put(cmd, data);
        savePlayerFile(playerId);
        return data;
    }

    /** Boost personal de un jugador para un CMD (solo personal, sin fallback). null si no existe. */
    public SkinBoostData getPlayerBoost(UUID playerId, int cmd) {
        return getPlayerBoosts(playerId).get(cmd);
    }

    public boolean hasPlayerBoost(UUID playerId, int cmd) {
        return getPlayerBoosts(playerId).containsKey(cmd);
    }

    /** Elimina el boost personal de un jugador para un CMD. No toca el registro global. */
    public boolean removePlayerBoost(UUID playerId, int cmd) {
        Map<Integer, SkinBoostData> map = getPlayerBoosts(playerId);
        if (map.remove(cmd) == null) return false;
        savePlayerFile(playerId);
        return true;
    }

    /** Elimina TODOS los boosts personales de un jugador (borra su archivo). No toca el global. */
    public boolean removePlayer(UUID playerId) {
        playerBoosts.remove(playerId);
        File file = playerFile(playerId);
        return !file.exists() || file.delete();
    }

    /** Vista de solo lectura de los boosts personales de un jugador. */
    public Map<Integer, SkinBoostData> getPlayerBoostsView(UUID playerId) {
        return Collections.unmodifiableMap(getPlayerBoosts(playerId));
    }

    // ---------------------------------------------------------------
    //  Queries globales (igual que antes)
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
    //  Persistencia por jugador: plugins/SkinBoosts/players/<uuid>.yml
    // ---------------------------------------------------------------

    private Map<Integer, SkinBoostData> getPlayerBoosts(UUID playerId) {
        // computeIfAbsent sirve de caché: el archivo se lee solo la primera vez
        return playerBoosts.computeIfAbsent(playerId, this::loadPlayerFile);
    }

    private Map<Integer, SkinBoostData> loadPlayerFile(UUID playerId) {
        Map<Integer, SkinBoostData> map = new HashMap<>();
        File file = playerFile(playerId);
        if (!file.exists()) return map;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection boostsSec = yaml.getConfigurationSection("boosts");
        if (boostsSec == null) return map;

        for (String key : boostsSec.getKeys(false)) {
            int cmd;
            try {
                cmd = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid CMD key in player file " + file.getName() + ": '" + key + "' — skipping.");
                continue;
            }

            ConfigurationSection sec = boostsSec.getConfigurationSection(key);
            if (sec == null) continue;

            String name = sec.getString("name", String.valueOf(cmd));
            ConfigurationSection mults = sec.getConfigurationSection("multipliers");

            double essence = mults != null ? mults.getDouble("essence",  1.0) : 1.0;
            double money   = mults != null ? mults.getDouble("money",    1.0) : 1.0;
            double toolXp  = mults != null ? mults.getDouble("tool-xp",  1.0) : 1.0;
            double cyberXp = mults != null ? mults.getDouble("cyber-xp", 1.0) : 1.0;

            // Clamp con los límites actuales (pueden haber cambiado desde que se guardó)
            essence = clamp("essence",  cmd, essence, maxEssence);
            money   = clamp("money",    cmd, money,   maxMoney);
            toolXp  = clamp("tool-xp",  cmd, toolXp,  maxToolXp);
            cyberXp = clamp("cyber-xp", cmd, cyberXp, maxCyberXp);

            map.put(cmd, new SkinBoostData(cmd, name, essence, money, toolXp, cyberXp));
        }

        return map;
    }

    private void savePlayerFile(UUID playerId) {
        File file = playerFile(playerId);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<Integer, SkinBoostData> e : getPlayerBoosts(playerId).entrySet()) {
            String base = "boosts." + e.getKey();
            SkinBoostData d = e.getValue();
            yaml.set(base + ".name", d.getName());
            yaml.set(base + ".multipliers.essence",  d.getEssenceMultiplier());
            yaml.set(base + ".multipliers.money",    d.getMoneyMultiplier());
            yaml.set(base + ".multipliers.tool-xp",  d.getToolXpMultiplier());
            yaml.set(base + ".multipliers.cyber-xp", d.getCyberXpMultiplier());
        }

        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player boost file: " + file.getName(), ex);
        }
    }

    private File playerFile(UUID playerId) {
        return new File(new File(plugin.getDataFolder(), "players"), playerId + ".yml");
    }

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