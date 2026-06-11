package com.minehostil.skinboosts.commands;

import com.minehostil.skinboosts.SkinBoostsPlugin;
import com.minehostil.skinboosts.manager.SkinBoostManager;
import com.minehostil.skinboosts.model.Messages;
import com.minehostil.skinboosts.model.SkinBoostData;
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

public class SkinBoostCommand implements CommandExecutor, TabCompleter {

    private final SkinBoostManager manager;
    private final Messages msg;

    public SkinBoostCommand(SkinBoostsPlugin plugin, SkinBoostManager manager, Messages msg) {
        this.manager = manager;
        this.msg = msg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("skinboosts.admin")) {
            sender.sendMessage(msg.get("no-permission"));
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

    private void handleSet(CommandSender sender, String[] args) {
        // Accepts both:
        //   /skinboost set <cmd> <nombre> <essence> <money> <tool-xp> <cyber-xp>  (7 args)
        //   /skinboost set <cmd> <essence> <money> <tool-xp> <cyber-xp>            (6 args, no name)
        if (args.length < 6) {
            sender.sendMessage(msg.get("usage-set"));
            sender.sendMessage(msg.get("usage-set-example"));
            return;
        }

        int customModelData;
        if (args[1].equalsIgnoreCase("hand")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(msg.get("player-only"));
                return;
            }
            customModelData = getCmdFromHand(p);
            if (customModelData == -1) {
                sender.sendMessage(msg.get("no-cmd-in-hand"));
                return;
            }
        } else {
            try {
                customModelData = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(msg.get("invalid-cmd", "value", args[1]));
                return;
            }
        }

        // Detect if args[2] is a number (no name provided) or a name
        String name;
        int offset;
        if (isDouble(args[2])) {
            name = String.valueOf(customModelData); // use CMD as name
            offset = 2;
        } else {
            name = args[2].replace("_", " ");
            offset = 3;
        }

        if (args.length < offset + 4) {
            sender.sendMessage(msg.get("usage-set"));
            sender.sendMessage(msg.get("usage-set-example"));
            return;
        }

        double essence, money, toolXp, cyberXp;
        try {
            essence = Double.parseDouble(args[offset]);
            money   = Double.parseDouble(args[offset + 1]);
            toolXp  = Double.parseDouble(args[offset + 2]);
            cyberXp = Double.parseDouble(args[offset + 3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(msg.get("invalid-multipliers"));
            return;
        }

        SkinBoostData saved = manager.setSkin(customModelData, name, essence, money, toolXp, cyberXp);

        sender.sendMessage(msg.get("skin-saved",
                "cmd", String.valueOf(customModelData), "name", name));
        sender.sendMessage(msg.get("skin-saved-values",
                "essence", fmt(saved.getEssenceMultiplier()),
                "money",   fmt(saved.getMoneyMultiplier()),
                "toolxp",  fmt(saved.getToolXpMultiplier()),
                "cyberxp", fmt(saved.getCyberXpMultiplier())));

        if (saved.getEssenceMultiplier() < essence || saved.getMoneyMultiplier() < money
                || saved.getToolXpMultiplier() < toolXp || saved.getCyberXpMultiplier() < cyberXp) {
            sender.sendMessage(msg.get("skin-clamped"));
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg.get("usage-remove"));
            return;
        }
        int cmd;
        try {
            cmd = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(msg.get("invalid-cmd", "value", args[1]));
            return;
        }

        if (manager.removeSkin(cmd)) {
            sender.sendMessage(msg.get("skin-removed", "cmd", String.valueOf(cmd)));
        } else {
            sender.sendMessage(msg.get("skin-not-found", "cmd", String.valueOf(cmd)));
        }
    }

    private void handleList(CommandSender sender) {
        Collection<SkinBoostData> all = manager.getAllBoosts();
        if (all.isEmpty()) {
            sender.sendMessage(msg.get("no-skins"));
            return;
        }
        sender.sendMessage(msg.get("list-header", "count", String.valueOf(all.size())));
        sender.sendMessage(msg.get("list-limits",
                "essence", fmtLimit(manager.getMaxEssence()),
                "money",   fmtLimit(manager.getMaxMoney()),
                "toolxp",  fmtLimit(manager.getMaxToolXp()),
                "cyberxp", fmtLimit(manager.getMaxCyberXp())));
        for (SkinBoostData d : all) {
            sender.sendMessage(msg.getRaw("list-entry",
                    "cmd",     String.valueOf(d.getCustomModelData()),
                    "name",    d.getName(),
                    "essence", fmt(d.getEssenceMultiplier()),
                    "money",   fmt(d.getMoneyMultiplier()),
                    "toolxp",  fmt(d.getToolXpMultiplier()),
                    "cyberxp", fmt(d.getCyberXpMultiplier())));
        }
    }

    private void handleReload(CommandSender sender) {
        manager.getPlugin().reloadConfig();
        manager.loadFromConfig();
        msg.reload();
        sender.sendMessage(msg.get("reload-done", "count", String.valueOf(manager.getAllBoosts().size())));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        int cmd;
        if (args.length >= 2) {
            try {
                cmd = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(msg.get("invalid-cmd", "value", args[1]));
                return;
            }
        } else if (sender instanceof Player p) {
            cmd = getCmdFromHand(p);
            if (cmd == -1) {
                sender.sendMessage(msg.get("no-cmd-in-hand"));
                return;
            }
        } else {
            sender.sendMessage(msg.get("usage-info"));
            return;
        }

        SkinBoostData data = manager.getBoost(cmd);
        if (data == null) {
            sender.sendMessage(msg.get("info-not-found", "cmd", String.valueOf(cmd)));
            return;
        }

        sender.sendMessage(msg.get("info-header", "cmd", String.valueOf(cmd), "name", data.getName()));
        sender.sendMessage(msg.getRaw("info-essence", "value", fmt(data.getEssenceMultiplier())));
        sender.sendMessage(msg.getRaw("info-money",   "value", fmt(data.getMoneyMultiplier())));
        sender.sendMessage(msg.getRaw("info-toolxp",  "value", fmt(data.getToolXpMultiplier())));
        sender.sendMessage(msg.getRaw("info-cyberxp", "value", fmt(data.getCyberXpMultiplier())));
    }

    // ---------------------------------------------------------------
    //  Tab completion
    // ---------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("set", "remove", "list", "reload", "info");
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) return List.of("hand", "<customModelData>");
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

    private boolean isDouble(String s) {
        try { Double.parseDouble(s); return true; }
        catch (NumberFormatException e) { return false; }
    }

    private String fmt(double value) {
        return String.format("%.2f", value);
    }

    private String fmtLimit(double max) {
        return max <= 0.0 ? "∞" : (max + "x");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg.get("help-header"));
        sender.sendMessage(msg.getRaw("help-set"));
        sender.sendMessage(msg.getRaw("help-remove"));
        sender.sendMessage(msg.getRaw("help-list"));
        sender.sendMessage(msg.getRaw("help-info"));
        sender.sendMessage(msg.getRaw("help-reload"));
    }
}