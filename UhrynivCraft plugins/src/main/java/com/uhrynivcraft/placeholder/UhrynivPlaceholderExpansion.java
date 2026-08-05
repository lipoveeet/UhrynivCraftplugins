package com.uhrynivcraft.placeholder;

import com.uhrynivcraft.UhrynivCraft;
import com.uhrynivcraft.model.PlayerData;
import com.uhrynivcraft.util.MessageUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Registers %uhryniv_balance%, %uhryniv_total%, %uhryniv_hour_income%
 * so TAB and other PAPI-consuming plugins can display them.
 */
public class UhrynivPlaceholderExpansion extends PlaceholderExpansion {

    private final UhrynivCraft plugin;

    public UhrynivPlaceholderExpansion(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "uhryniv";
    }

    @Override
    public @NotNull String getAuthor() {
        return "UhrynivCraft Team";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        PlayerData data = plugin.getEconomyManager().getCached(player.getUniqueId());
        if (data == null) return "0";

        return switch (params.toLowerCase()) {
            case "balance" -> MessageUtil.formatAmount(data.getBalance());
            case "total" -> MessageUtil.formatAmount(data.getTotalEarned());
            case "hour_income" -> MessageUtil.formatAmount(data.getHourIncome());
            default -> null;
        };
    }
}
