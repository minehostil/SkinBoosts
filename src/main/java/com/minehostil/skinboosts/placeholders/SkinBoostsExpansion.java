package com.minehostil.skinboosts.placeholders;

import com.minehostil.skinboosts.manager.SkinBoostManager;
import com.minehostil.skinboosts.model.SkinBoostData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        return "1.1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(Player player, @NotNull String params) {
        if (player == null) return null;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.hasItemMeta()) return defaults(params);
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return defaults(params);

        int cmd = meta.getCustomModelData();

        // Resolución por jugador: personal > global
        SkinBoostData boost = manager.getBoost(player.getUniqueId(), cmd);
        if (boost == null) return defaults(params);

        return switch (params.toLowerCase()) {
            case "essence"     -> fmt(boost.getEssenceMultiplier());
            case "money"       -> fmt(boost.getMoneyMultiplier());
            case "toolxp"      -> fmt(boost.getToolXpMultiplier());
            case "cyberxp"     -> fmt(boost.getCyberXpMultiplier());
            case "essence_fmt" -> "x" + fmt(boost.getEssenceMultiplier());
            case "money_fmt"   -> "x" + fmt(boost.getMoneyMultiplier());
            case "toolxp_fmt"  -> "x" + fmt(boost.getToolXpMultiplier());
            case "cyberxp_fmt" -> "x" + fmt(boost.getCyberXpMultiplier());
            case "name"        -> boost.getName() != null ? boost.getName() : "None";
            case "cmd"         -> String.valueOf(boost.getCustomModelData());
            case "active"      -> "true";
            default            -> null;
        };
    }

    private String defaults(String params) {
        return switch (params.toLowerCase()) {
            case "essence", "money", "toolxp", "cyberxp" -> "1.00";
            case "essence_fmt", "money_fmt", "toolxp_fmt", "cyberxp_fmt" -> "x1.00";
            case "name"   -> "None";
            case "cmd"    -> "-1";
            case "active" -> "false";
            default       -> null;
        };
    }

    private String fmt(double value) {
        return String.format("%.2f", value);
    }
}