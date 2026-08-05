package com.uhrynivcraft.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class MessageUtil {

    private MessageUtil() {}

    public static String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    /** Replaces {key} placeholders in a message using the provided map, then colorizes it. */
    public static String format(String message, Map<String, String> placeholders) {
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return color(result);
    }

    public static String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }
}
