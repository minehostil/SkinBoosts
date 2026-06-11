package com.minehostil.skinboosts;

import com.minehostil.skinboosts.commands.SkinBoostCommand;
import com.minehostil.skinboosts.listeners.HarvestListener;
import com.minehostil.skinboosts.manager.SkinBoostManager;
import com.minehostil.skinboosts.model.Messages;
import com.minehostil.skinboosts.placeholders.SkinBoostsExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SkinBoostsPlugin extends JavaPlugin {

    private SkinBoostManager boostManager;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        boostManager = new SkinBoostManager(this);
        boostManager.loadFromConfig();

        messages = new Messages(this);

        if (Bukkit.getPluginManager().getPlugin("RivalHarvesterHoes") == null)
            getLogger().warning("RivalHarvesterHoes not found — Essence/Money/ToolXP boosts disabled.");

        if (Bukkit.getPluginManager().getPlugin("CyberLevels") == null)
            getLogger().warning("CyberLevels not found — CyberXP boosts disabled.");

        getServer().getPluginManager().registerEvents(new HarvestListener(this, boostManager), this);

        SkinBoostCommand cmdExecutor = new SkinBoostCommand(this, boostManager, messages);
        getCommand("skinboost").setExecutor(cmdExecutor);
        getCommand("skinboost").setTabCompleter(cmdExecutor);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SkinBoostsExpansion(boostManager).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        getLogger().info("SkinBoosts enabled — " + boostManager.getAllBoosts().size() + " skin(s) loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkinBoosts disabled.");
    }

    public SkinBoostManager getBoostManager() { return boostManager; }
    public Messages getMessages()              { return messages; }
}