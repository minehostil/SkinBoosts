package com.minehostil.skinboosts.placeholders;

import com.minehostil.skinboosts.model.SkinBoostData;
import com.minehostil.skinboosts.manager.SkinBoostManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for SkinBoosts.
 *
 * All placeholders read the item currently in the player's main hand.
 *
 * Available placeholders:
 *   %skinboosts_essence%    → essence multiplier (e.g. "1.50")
 *   %skinboosts_money%      → money multiplier
 *   %skinboosts_toolxp%     → tool XP multiplier
 *   %skinboosts_cyberxp%    → cyber XP multiplier
 *   %skinboosts_name%       → display name of the active skin, or "None"
 *   %skinboosts_cmd%        → raw CustomModelData int, or "-1" if none
 *   %skinboosts_active%     → "true" / "false"
 *
 * Formatted variants (show as "x1.50" with the x prefix):
 *   %skinboosts_essence_fmt%
 *   %skinboosts_money_fmt%
 *   %skinboosts_toolxp_fmt%
 *   %skinboosts_cyberxp_fmt%
 */
public class SkinBoostsExpansion extends PlaceholderExpansion {

    private final SkinBoostManager manager;

    public SkinBoostsExpansion(SkinBoostManager manager) {
        this.manager = manager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "skinboosts";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MineHostil";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        // Keep registered through /papi reload
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        // All placeholders require an online player (we need inventory access)
        if (offlinePlayer == null || !offlinePlayer.isOnline()) return "0";
        Player player = offlinePlayer.getPlayer();
        if (player == null) return "0";

        SkinBoostData data = getBoostFromHand(player);

        return switch (params.toLowerCase()) {
            case "essence"     -> data != null ? fmt(data.getEssenceMultiplier())  : "1.00";
            case "money"       -> data != null ? fmt(data.getMoneyMultiplier())    : "1.00";
            case "toolxp"      -> data != null ? fmt(data.getToolXpMultiplier())   : "1.00";
            case "cyberxp"     -> data != null ? fmt(data.getCyberXpMultiplier())  : "1.00";

            case "essence_fmt" -> data != null ? "x" + fmt(data.getEssenceMultiplier())  : "x1.00";
            case "money_fmt"   -> data != null ? "x" + fmt(data.getMoneyMultiplier())    : "x1.00";
            case "toolxp_fmt"  -> data != null ? "x" + fmt(data.getToolXpMultiplier())   : "x1.00";
            case "cyberxp_fmt" -> data != null ? "x" + fmt(data.getCyberXpMultiplier())  : "x1.00";

            case "name"        -> data != null ? colorize(data.getName()) : "None";
            case "cmd"         -> data != null ? String.valueOf(data.getCustomModelData()) : "-1";
            case "active"      -> data != null ? "true" : "false";

            default -> null; // Unknown placeholder → PAPI shows it unparsed
        };
    }

    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------

    /**
     * Reads the CustomModelData from the player's main-hand item and
     * returns the matching SkinBoostData, or null if not registered.
     */
    private SkinBoostData getBoostFromHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return null;
        return manager.getBoost(meta.getCustomModelData());
    }

    /** Format a double to 2 decimal places. */
    private String fmt(double value) {
        return String.format("%.2f", value);
    }

    /** Translate legacy & color codes for display names. */
    private String colorize(String s) {
        return s.replace("&", "§");
    }
}