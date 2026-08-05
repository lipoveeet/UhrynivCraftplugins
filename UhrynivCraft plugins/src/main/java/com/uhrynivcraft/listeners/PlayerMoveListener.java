package com.uhrynivcraft.listeners;

import com.uhrynivcraft.UhrynivCraft;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveListener implements Listener {

    private final UhrynivCraft plugin;

    public PlayerMoveListener(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        plugin.getAfkTracker().onMove(event.getPlayer().getUniqueId(), event.getTo());
    }
}
