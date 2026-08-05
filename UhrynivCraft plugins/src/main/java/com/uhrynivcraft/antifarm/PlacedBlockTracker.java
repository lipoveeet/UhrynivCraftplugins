package com.uhrynivcraft.antifarm;

import com.uhrynivcraft.UhrynivCraft;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Prevents "generator farms" (cobblestone generators, sand/bamboo/sugar-cane
 * dupers, etc.) from paying out by tagging every block a player *places*
 * with a persistent-data flag, and refusing rewards when that flag is present
 * on break.
 *
 * Uses the block's BlockState PersistentDataContainer (Paper feature) which
 * survives chunk unload/reload, unlike a plain in-memory set.
 */
public class PlacedBlockTracker {

    private final NamespacedKey key;

    public PlacedBlockTracker(UhrynivCraft plugin) {
        this.key = new NamespacedKey(plugin, "player_placed");
    }

    public void markPlaced(Block block) {
        var state = block.getState();
        PersistentDataContainer pdc = state.getPersistentDataContainer();
        pdc.set(key, PersistentDataType.BYTE, (byte) 1);
        state.update(true, false);
    }

    public boolean wasPlacedByPlayer(Block block) {
        PersistentDataContainer pdc = block.getState().getPersistentDataContainer();
        return pdc.has(key, PersistentDataType.BYTE);
    }
}
