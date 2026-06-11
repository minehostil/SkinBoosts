package com.minehostil.skinboosts.listeners;

import com.bitaspire.cyberlevels.event.ExpChangeEvent;
import com.minehostil.skinboosts.model.SkinBoostData;
import com.minehostil.skinboosts.manager.SkinBoostManager;
import com.minehostil.skinboosts.SkinBoostsPlugin;
import me.rivaldev.harvesterhoes.api.events.HoeEssenceReceiveEnchantEvent;
import me.rivaldev.harvesterhoes.api.events.HoeMoneyReceiveEnchant;
import me.rivaldev.harvesterhoes.api.events.HoeXPGainEvent;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Listens to native RivalHarvesterHoes and CyberLevels events.
 * No reflection, no fallbacks — values come directly from each event.
 *
 * Priority HIGH: we run after default handlers so the base value
 * is already calculated, then we scale it up before MONITOR listeners.
 */
public class HarvestListener implements Listener {

    private final SkinBoostManager manager;
    private final boolean showParticles;
    private final boolean rivalEnabled;
    private final boolean cyberEnabled;

    public HarvestListener(SkinBoostsPlugin plugin, SkinBoostManager manager) {
        this.manager = manager;
        this.showParticles = plugin.getConfig().getBoolean("show-particles", true);
        this.rivalEnabled  = plugin.getServer().getPluginManager().getPlugin("RivalHarvesterHoes") != null;
        this.cyberEnabled  = plugin.getServer().getPluginManager().getPlugin("CyberLevels") != null;
    }

    // ---------------------------------------------------------------
    //  RivalHarvesterHoes — Essence
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEssence(HoeEssenceReceiveEnchantEvent event) {
        if (!rivalEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getEssenceMultiplier() <= 1.0) return;

        event.setEssence(event.getEssence() * boost.getEssenceMultiplier());
        maybeParticle(event.getPlayer());
    }

    // ---------------------------------------------------------------
    //  RivalHarvesterHoes — Money
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMoney(HoeMoneyReceiveEnchant event) {
        if (!rivalEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getMoneyMultiplier() <= 1.0) return;

        event.setMoney(event.getMoney() * boost.getMoneyMultiplier());
        maybeParticle(event.getPlayer());
    }

    // ---------------------------------------------------------------
    //  RivalHarvesterHoes — Tool XP
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToolXP(HoeXPGainEvent event) {
        if (!rivalEnabled) return;
        SkinBoostData boost = getBoostFromHand(event.getPlayer());
        if (boost == null || boost.getToolXpMultiplier() <= 1.0) return;

        event.setXP(event.getXP() * boost.getToolXpMultiplier());
        maybeParticle(event.getPlayer());
    }

    // ---------------------------------------------------------------
    //  CyberLevels — XP
    // ---------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCyberXP(ExpChangeEvent event) {
        if (!cyberEnabled) return;
        Player player = event.getUser().getPlayer();
        if (player == null) return;

        SkinBoostData boost = getBoostFromHand(player);
        if (boost == null || boost.getCyberXpMultiplier() <= 1.0) return;

        // setAmount controls the XP being added in this event
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