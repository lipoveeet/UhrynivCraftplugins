package com.uhrynivcraft.commands;

import com.uhrynivcraft.UhrynivCraft;
import com.uhrynivcraft.database.PlayerDAO;
import com.uhrynivcraft.model.PlayerData;
import com.uhrynivcraft.model.TransactionType;
import com.uhrynivcraft.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UpCommand implements CommandExecutor, TabCompleter {

    private final UhrynivCraft plugin;

    public UpCommand(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showBalance(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "pay" -> handlePay(sender, args);
            case "top" -> handleTop(sender);
            case "admin" -> handleAdmin(sender, args);
            case "reload" -> handleReload(sender);
            default -> showBalance(sender);
        }
        return true;
    }

    private void showBalance(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("player-only"));
            return;
        }
        PlayerData data = plugin.getEconomyManager().getCached(player.getUniqueId());
        if (data == null) {
            MessageUtil.send(sender, "&cВаші дані ще завантажуються, спробуйте за секунду.");
            return;
        }
        MessageUtil.send(sender, plugin.getConfigManager().getMessage("balance-header"));
        MessageUtil.send(sender, MessageUtil.format(plugin.getConfigManager().getMessage("balance-line"),
                Map.of("balance", MessageUtil.formatAmount(data.getBalance()))));
        MessageUtil.send(sender, MessageUtil.format(plugin.getConfigManager().getMessage("balance-hour-line"),
                Map.of("hour", MessageUtil.formatAmount(data.getHourIncome()))));
    }

    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("player-only"));
            return;
        }
        if (args.length < 3) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("pay-usage"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId().equals(player.getUniqueId()) || (!target.hasPlayedBefore() && !target.isOnline())) {
            MessageUtil.send(sender, MessageUtil.format(plugin.getConfigManager().getMessage("player-not-found"),
                    Map.of("player", args[1])));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("pay-invalid-amount"));
            return;
        }
        if (amount <= 0) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("pay-invalid-amount"));
            return;
        }

        boolean success = plugin.getEconomyManager().subtractRaw(player.getUniqueId(), amount,
                TransactionType.PAY_SEND, "pay to " + target.getName());
        if (!success) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("pay-not-enough"));
            return;
        }

        // If the recipient is online, credit their cache directly; otherwise credit the DB directly.
        Player targetOnline = target.getPlayer();
        if (targetOnline != null) {
            plugin.getEconomyManager().addRaw(targetOnline.getUniqueId(), amount, TransactionType.PAY_RECEIVE,
                    "pay from " + player.getName());
            MessageUtil.send(targetOnline, MessageUtil.format(plugin.getConfigManager().getMessage("pay-success-receiver"),
                    Map.of("amount", MessageUtil.formatAmount(amount), "player", player.getName())));
        } else {
            plugin.getDatabaseManager().getExecutor().submit(() -> {
                try {
                    PlayerData offlineData = plugin.getEconomyManager().getDao()
                            .loadOrCreate(target.getUniqueId(), target.getName(), 0.0);
                    offlineData.addBalance(amount, false);
                    plugin.getEconomyManager().getDao().save(offlineData);
                    plugin.getEconomyManager().getDao().logTransaction(target.getUniqueId(),
                            TransactionType.PAY_RECEIVE, amount, "pay from " + player.getName());
                } catch (Exception e) {
                    plugin.getLogger().warning("Не вдалося нарахувати оплату офлайн гравцю: " + e.getMessage());
                }
            });
        }

        MessageUtil.send(sender, MessageUtil.format(plugin.getConfigManager().getMessage("pay-success-sender"),
                Map.of("amount", MessageUtil.formatAmount(amount), "player", target.getName())));
    }

    private void handleTop(CommandSender sender) {
        plugin.getDatabaseManager().getExecutor().submit(() -> {
            try {
                List<PlayerDAO.TopEntry> top = plugin.getEconomyManager().getTopBalancesSync(10);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtil.send(sender, plugin.getConfigManager().getMessage("top-header"));
                    int position = 1;
                    for (PlayerDAO.TopEntry entry : top) {
                        MessageUtil.send(sender, MessageUtil.format(plugin.getConfigManager().getMessage("top-line"),
                                Map.of("position", String.valueOf(position),
                                        "player", entry.name(),
                                        "balance", MessageUtil.formatAmount(entry.balance()))));
                        position++;
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Не вдалося отримати топ балансів: " + e.getMessage());
            }
        });
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("uhrynivcraft.admin")) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 4 || !(args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("take"))) {
            MessageUtil.send(sender, "&cВикористання: /up admin <give|take> <player> <amount>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("pay-invalid-amount"));
            return;
        }
        if (amount <= 0) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("pay-invalid-amount"));
            return;
        }

        boolean give = args[1].equalsIgnoreCase("give");
        Player online = target.getPlayer();

        if (online != null) {
            if (give) {
                plugin.getEconomyManager().addRaw(online.getUniqueId(), amount, TransactionType.ADMIN_GIVE, "admin give by " + sender.getName());
            } else {
                plugin.getEconomyManager().subtractRaw(online.getUniqueId(), amount, TransactionType.ADMIN_TAKE, "admin take by " + sender.getName());
            }
        } else {
            plugin.getDatabaseManager().getExecutor().submit(() -> {
                try {
                    PlayerData offlineData = plugin.getEconomyManager().getDao()
                            .loadOrCreate(target.getUniqueId(), target.getName() != null ? target.getName() : args[2], 0.0);
                    if (give) {
                        offlineData.addBalance(amount, false);
                    } else if (!offlineData.subtractBalance(amount)) {
                        return;
                    }
                    plugin.getEconomyManager().getDao().save(offlineData);
                    plugin.getEconomyManager().getDao().logTransaction(target.getUniqueId(),
                            give ? TransactionType.ADMIN_GIVE : TransactionType.ADMIN_TAKE, give ? amount : -amount,
                            "admin by " + sender.getName());
                } catch (Exception e) {
                    plugin.getLogger().warning("Не вдалося виконати адмін-операцію офлайн: " + e.getMessage());
                }
            });
        }

        String key = give ? "admin-give-success" : "admin-take-success";
        MessageUtil.send(sender, MessageUtil.format(plugin.getConfigManager().getMessage(key),
                Map.of("amount", MessageUtil.formatAmount(amount), "player", args[2])));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("uhrynivcraft.admin")) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        plugin.getConfigManager().load();
        plugin.getShopGUI().reload();
        MessageUtil.send(sender, plugin.getConfigManager().getMessage("reload-success"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("pay", "top"));
            if (sender.hasPermission("uhrynivcraft.admin")) {
                options.add("admin");
                options.add("reload");
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return filter(List.of("give", "take"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pay")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream().filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
}
