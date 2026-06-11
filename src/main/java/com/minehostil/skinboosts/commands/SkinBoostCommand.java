package com.minehostil.skinboosts.commands;

import com.minehostil.skinboosts.model.SkinBoostData;
import com.minehostil.skinboosts.manager.SkinBoostManager;
import com.minehostil.skinboosts.SkinBoostsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * /skinboost <subcommand> [args]
 *
 * Subcommands:
 *   set <cmd> <name> <essence> <money> <tool-xp> <cyber-xp>
 *       OR
 *   set hand <name> <essence> <money> <tool-xp> <cyber-xp>
 *       (reads CMD from the item currently in hand)
 *
 *   remove <cmd>
 *   list
 *   reload
 *   info [cmd]   — shows current multipliers for a skin
 */
public class SkinBoostCommand implements CommandExecutor, TabCompleter {

    private final SkinBoostsPlugin plugin;
    private final SkinBoostManager manager;

    private static final String PREFIX_RAW = "&8[&6SkinBoosts&8] &r";

    public SkinBoostCommand(SkinBoostsPlugin plugin, SkinBoostManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("skinboosts.admin")) {
            msg(sender, "&cNo tienes permiso para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set"    -> handleSet(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list"   -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "info"   -> handleInfo(sender, args);
            default       -> sendHelp(sender);
        }
        return true;
    }

    // ---------------------------------------------------------------
    //  Subcommand handlers
    // ---------------------------------------------------------------

    /**
     * /skinboost set <cmd|hand> <name> <essence> <money> <tool-xp> <cyber-xp>
     */
    private void handleSet(CommandSender sender, String[] args) {
        // Need: set + cmdOrHand + name + 4 multipliers = 7 args
        if (args.length < 7) {
            msg(sender, "&eUso: &f/skinboost set <cmd|hand> <nombre> <essence> <dinero> <tool-xp> <cyber-xp>");
            msg(sender, "&7Ejemplo: &f/skinboost set 1001 \"Pico_Fuego\" 1.5 1.2 1.5 1.3");
            return;
        }

        int customModelData;
        if (args[1].equalsIgnoreCase("hand")) {
            if (!(sender instanceof Player p)) {
                msg(sender, "&cDebes ser jugador para usar 'hand'.");
                return;
            }
            customModelData = getCmdFromHand(p);
            if (customModelData == -1) {
                msg(sender, "&cEl item en tu mano no tiene CustomModelData.");
                return;
            }
        } else {
            try {
                customModelData = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                msg(sender, "&cValor de CustomModelData inválido: &f" + args[1]);
                return;
            }
        }

        String name = args[2].replace("_", " ");

        double essence, money, toolXp, cyberXp;
        try {
            essence = Double.parseDouble(args[3]);
            money   = Double.parseDouble(args[4]);
            toolXp  = Double.parseDouble(args[5]);
            cyberXp = Double.parseDouble(args[6]);
        } catch (NumberFormatException e) {
            msg(sender, "&cMultiplicadores inválidos. Usa números (ej. 1.5).");
            return;
        }

        SkinBoostData saved = manager.setSkin(customModelData, name, essence, money, toolXp, cyberXp);
        msg(sender, "&aSkin &f" + customModelData + " &a(" + name + ") &aguardada correctamente.");
        msg(sender, "&7  Essence: &f" + saved.getEssenceMultiplier()
                + "x &7| Money: &f" + saved.getMoneyMultiplier()
                + "x &7| ToolXP: &f" + saved.getToolXpMultiplier()
                + "x &7| CyberXP: &f" + saved.getCyberXpMultiplier() + "x");
        // Warn if any value was clamped
        if (saved.getEssenceMultiplier() < essence || saved.getMoneyMultiplier() < money
                || saved.getToolXpMultiplier() < toolXp || saved.getCyberXpMultiplier() < cyberXp) {
            msg(sender, "&eAlgún multiplicador fue reducido al máximo permitido.");
        }
    }

    /**
     * /skinboost remove <cmd>
     */
    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            msg(sender, "&eUso: &f/skinboost remove <cmd>");
            return;
        }
        int cmd;
        try {
            cmd = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            msg(sender, "&cValor inválido: &f" + args[1]);
            return;
        }

        if (manager.removeSkin(cmd)) {
            msg(sender, "&aSkin &f" + cmd + " &aeliminada correctamente.");
        } else {
            msg(sender, "&cNo existe ninguna skin registrada con CMD &f" + cmd + "&c.");
        }
    }

    private void handleList(CommandSender sender) {
        Collection<SkinBoostData> all = manager.getAllBoosts();
        if (all.isEmpty()) {
            msg(sender, "&7No hay skins registradas.");
            return;
        }
        // Show configured limits
        msg(sender, "&6=== Skins Registradas (" + all.size() + ") ===");
        msg(sender, "&7Límites — E:&f" + fmtLimit(manager.getMaxEssence())
                + " &7M:&f" + fmtLimit(manager.getMaxMoney())
                + " &7TX:&f" + fmtLimit(manager.getMaxToolXp())
                + " &7CX:&f" + fmtLimit(manager.getMaxCyberXp()));
        for (SkinBoostData d : all) {
            msg(sender, "&e" + d.getCustomModelData() + " &7— &f" + d.getName()
                    + " &7| E:&f" + d.getEssenceMultiplier()
                    + "x &7M:&f" + d.getMoneyMultiplier()
                    + "x &7TX:&f" + d.getToolXpMultiplier()
                    + "x &7CX:&f" + d.getCyberXpMultiplier() + "x");
        }
    }

    private String fmtLimit(double max) {
        return max <= 0.0 ? "∞" : (max + "x");
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        manager.loadFromConfig();
        msg(sender, "&aConfiguración recargada. &7(" + manager.getAllBoosts().size() + " skins)");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        int cmd;
        if (args.length >= 2) {
            try {
                cmd = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                msg(sender, "&cValor inválido: &f" + args[1]);
                return;
            }
        } else if (sender instanceof Player p) {
            cmd = getCmdFromHand(p);
            if (cmd == -1) {
                msg(sender, "&cEl item en mano no tiene CustomModelData.");
                return;
            }
        } else {
            msg(sender, "&eUso: &f/skinboost info <cmd>");
            return;
        }

        SkinBoostData data = manager.getBoost(cmd);
        if (data == null) {
            msg(sender, "&7No hay boost registrado para CMD &f" + cmd + "&7.");
            return;
        }
        msg(sender, "&6Info de CMD &f" + cmd + " &7— &f" + data.getName());
        msg(sender, "&7  Essence:  &f" + data.getEssenceMultiplier() + "x");
        msg(sender, "&7  Dinero:   &f" + data.getMoneyMultiplier() + "x");
        msg(sender, "&7  Tool XP:  &f" + data.getToolXpMultiplier() + "x");
        msg(sender, "&7  Cyber XP: &f" + data.getCyberXpMultiplier() + "x");
    }

    // ---------------------------------------------------------------
    //  Tab completion
    // ---------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "remove", "list", "reload", "info");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return List.of("hand", "<customModelData>");
        }
        return List.of();
    }

    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------

    private int getCmdFromHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.hasItemMeta()) return -1;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return -1;
        return meta.getCustomModelData();
    }

    private void msg(CommandSender sender, String message) {
        sender.sendMessage(colorize(PREFIX_RAW + message));
    }

    private String colorize(String s) {
        return s.replace("&", "§");
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "&6=== SkinBoosts Commands ===");
        msg(sender, "&e/skinboost set <cmd|hand> <nombre> <essence> <money> <tool-xp> <cyber-xp>");
        msg(sender, "&e/skinboost remove <cmd>");
        msg(sender, "&e/skinboost list");
        msg(sender, "&e/skinboost info [cmd]");
        msg(sender, "&e/skinboost reload");
    }
}