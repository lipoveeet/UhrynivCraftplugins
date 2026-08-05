package com.uhrynivcraft.antifarm;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks blocks that were placed by a player, so breaking them later
 * (e.g. player-placed cobblestone from a generator) does not grant reward.
 */
public class PlacedBlockTracker {

    private final Set<String> placedBlocks = Collections.synchronizedSet(new HashSet<>());

    private String keyOf(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public void markPlaced(Block block) {
        placedBlocks.add(keyOf(block.getLocation()));
    }

    public boolean isPlayerPlaced(Block block) {
        return placedBlocks.contains(keyOf(block.getLocation()));
    }

    public void unmark(Block block) {
        placedBlocks.remove(keyOf(block.getLocation()));
    }
}
