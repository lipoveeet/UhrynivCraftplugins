package com.uhrynivcraft.commands;

import com.uhrynivcraft.UhrynivCraft;
import com.uhrynivcraft.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UpShopCommand implements CommandExecutor {

    private final UhrynivCraft plugin;

    public UpShopCommand(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("player-only"));
            return true;
        }
        plugin.getShopGUI().openMainMenu(player);
        return true;
    }
}
