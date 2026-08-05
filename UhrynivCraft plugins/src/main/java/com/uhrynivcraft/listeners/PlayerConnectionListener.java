package com.uhrynivcraft.listeners;

import com.uhrynivcraft.UhrynivCraft;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final UhrynivCraft plugin;

    public PlayerConnectionListener(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getEconomyManager().loadPlayer(event.getPlayer(), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        plugin.getEconomyManager().unloadPlayer(uuid);
        plugin.getMiningTracker().clearPlayer(uuid);
        plugin.getAfkTracker().clearPlayer(uuid);
    }
}
