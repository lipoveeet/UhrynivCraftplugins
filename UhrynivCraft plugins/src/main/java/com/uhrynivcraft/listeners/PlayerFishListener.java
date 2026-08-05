package com.uhrynivcraft.listeners;

import com.uhrynivcraft.UhrynivCraft;
import com.uhrynivcraft.model.TransactionType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class PlayerFishListener implements Listener {

    private final UhrynivCraft plugin;

    public PlayerFishListener(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item item)) return;

        Player player = event.getPlayer();
        String fishType = item.getItemStack().getType().name();

        Double base = plugin.getConfigManager().getFishRewards().get(fishType);
        if (base == null || base <= 0) return;

        // AFK fishing bots are the classic farm here - apply the AFK multiplier
        double afkMultiplier = plugin.getAfkTracker().getMultiplierFor(player.getUniqueId());
        double adjustedBase = base * afkMultiplier;
        if (adjustedBase <= 0) return;

        plugin.getEconomyManager().reward(player.getUniqueId(), adjustedBase, TransactionType.FISHING, fishType);
    }
}
