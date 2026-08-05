package com.uhrynivcraft.listeners;

import com.uhrynivcraft.UhrynivCraft;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class CreatureSpawnListener implements Listener {

    private final UhrynivCraft plugin;

    public CreatureSpawnListener(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        plugin.getMobSpawnTracker().tag(event.getEntity(), event.getSpawnReason());
    }
}
