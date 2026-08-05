package com.uhrynivcraft.listeners;

import com.uhrynivcraft.UhrynivCraft;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {

    private final UhrynivCraft plugin;

    public BlockPlaceListener(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfigManager().isPlacedBlockProtectionEnabled()) return;
        plugin.getPlacedBlockTracker().markPlaced(event.getBlockPlaced());
    }
}
