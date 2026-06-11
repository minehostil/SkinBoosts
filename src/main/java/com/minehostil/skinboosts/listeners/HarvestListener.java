package com.minehostil.skinboosts.listeners;

import com.minehostil.skinboosts.SkinBoostsPlugin;
import com.minehostil.skinboosts.manager.SkinBoostManager;
import com.minehostil.skinboosts.model.SkinBoostData;

// RivalHarvesterHoes
import me.rivaldev.harvesterhoes.api.events.HoeEssenceReceiveEnchantEvent;
import me.rivaldev.harvesterhoes.api.events.HoeMoneyReceiveEnchant;
import me.rivaldev.harvesterhoes.api.events.HoeXPGainEvent;

// RivalMobSwords
import me.rivaldev.mobsword.rivalmobswords.api.SwordEssenceReceiveEnchantEvent;
import me.rivaldev.mobsword.rivalmobswords.api.SwordMoneyReceiveEvent;
import me.rivaldev.mobsword.rivalmobswords.api.SwordEXPReceiveEvent;

// RivalPickaxes
import me.rivaldev.pickaxes.api.events.PickaxeEssenceReceiveEnchantEvent;
import me.rivaldev.pickaxes.api.events.PickaxeMoneyReceiveEnchant;
import me.rivaldev.pickaxes.api.events.PickaxeXPGainEvent;

// CyberLevels
import com.bitaspire.cyberlevels.event.ExpChangeEvent;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HarvestListener implements Listener {

    private final SkinBoostManager manager;
    private final boolean showParticles;
    private final boolean rivalHoesEnabled;
    private final boolean rivalSwordsEnabled;
    private final boolean rivalPickaxesEnabled;
    private final boolean cyberEnabled;

    public HarvestListener(SkinBoostsPlugin plugin, SkinBoostManager manager) {
        this.manager = manager;
        this.showParticles       = plugin.getConfig().getBoolean("show-particles", true);
        this.rivalHoesEnabled    = plugin.getServer().getPluginManager().getPlugin("RivalHarvesterHoes") != null;
        this.rivalSwordsEnabled  = plugin.getServer().getPluginManager().getPlugin("RivalMobSwords") != null;
        this.rivalPickaxesEnabled = plugin.getServer().getPluginManager().getPlugin("RivalPickaxes") != null;
        this.cyberEnabled        = plugin.getServer().getPluginManager().getPlugin("CyberLevels") != null;
    }

    // ---------------------------------------------------------------
    //  RivalHarvesterHoes
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHoeEssence(HoeEssenceReceiveEnchantEvent event) {
        if (!rivalHoesEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getEssenceMultiplier() <= 1.0) return;
        event.setEssence(event.getEssence() * boost.getEssenceMultiplier());
        maybeParticle(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHoeMoney(HoeMoneyReceiveEnchant event) {
        if (!rivalHoesEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getMoneyMultiplier() <= 1.0) return;
        event.setMoney(event.getMoney() * boost.getMoneyMultiplier());
        maybeParticle(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHoeToolXP(HoeXPGainEvent event) {
        if (!rivalHoesEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getToolXpMultiplier() <= 1.0) return;
        event.setXP(event.getXP() * boost.getToolXpMultiplier());
        maybeParticle(event.getPlayer());
    }

    // ---------------------------------------------------------------
    //  RivalMobSwords
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwordEssence(SwordEssenceReceiveEnchantEvent event) {
        if (!rivalSwordsEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getEssenceMultiplier() <= 1.0) return;
        event.setEssence(event.getEssence() * boost.getEssenceMultiplier());
        maybeParticle(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwordMoney(SwordMoneyReceiveEvent event) {
        if (!rivalSwordsEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getMoneyMultiplier() <= 1.0) return;
        event.setMoney(event.getMoney() * boost.getMoneyMultiplier());
        maybeParticle(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwordXP(SwordEXPReceiveEvent event) {
        if (!rivalSwordsEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getToolXpMultiplier() <= 1.0) return;
        // SwordEXP uses int
        int boosted = (int) Math.round(event.getEXP() * boost.getToolXpMultiplier());
        event.setEXP(boosted);
        maybeParticle(event.getPlayer());
    }

    // ---------------------------------------------------------------
    //  RivalPickaxes
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickaxeEssence(PickaxeEssenceReceiveEnchantEvent event) {
        if (!rivalPickaxesEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getEssenceMultiplier() <= 1.0) return;
        event.setEssence(event.getEssence() * boost.getEssenceMultiplier());
        maybeParticle(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickaxeMoney(PickaxeMoneyReceiveEnchant event) {
        if (!rivalPickaxesEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getMoneyMultiplier() <= 1.0) return;
        event.setMoney(event.getMoney() * boost.getMoneyMultiplier());
        maybeParticle(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickaxeXP(PickaxeXPGainEvent event) {
        if (!rivalPickaxesEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getToolXpMultiplier() <= 1.0) return;
        event.setXP(event.getXP() * boost.getToolXpMultiplier());
        maybeParticle(event.getPlayer());
    }

    // ---------------------------------------------------------------
    //  CyberLevels
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCyberXP(ExpChangeEvent event) {
        if (!cyberEnabled) return;
        Player player = event.getUser().getPlayer();
        if (player == null) return;
        SkinBoostData boost = getBoostFromHand(player);
        if (boost == null || boost.getCyberXpMultiplier() <= 1.0) return;
        event.setAmount(event.getAmount() * boost.getCyberXpMultiplier());
        maybeParticle(player);
    }

    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------

    private SkinBoostData getBoostFromHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return null;
        return manager.getBoost(meta.getCustomModelData());
    }

    private void maybeParticle(Player player) {
        if (!showParticles) return;
        player.getWorld().spawnParticle(
                Particle.VILLAGER_HAPPY,
                player.getLocation().add(0, 1, 0),
                6, 0.3, 0.3, 0.3, 0.0
        );
    }
}