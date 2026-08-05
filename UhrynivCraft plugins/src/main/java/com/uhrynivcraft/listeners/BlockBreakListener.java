package com.uhrynivcraft.listeners;

import com.uhrynivcraft.UhrynivCraft;
import com.uhrynivcraft.model.TransactionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Map;

/**
 * Grants Uhryniv Points for mining ores/blocks, chopping logs, and harvesting
 * fully-grown crops. All rewards pass through EconomyManager#reward which
 * applies the hourly anti-farm tier, and are pre-multiplied here by the
 * mining-speed/streak check and AFK check and the placed-block check.
 */
public class BlockBreakListener implements Listener {

    private final UhrynivCraft plugin;

    public BlockBreakListener(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();

        // Never reward blocks the player placed themselves (anti generator-farm)
        if (plugin.getConfigManager().isPlacedBlockProtectionEnabled()
                && plugin.getPlacedBlockTracker().wasPlacedByPlayer(block)) {
            return;
        }

        String matName = type.name();
        Map<String, Double> blockRewards = plugin.getConfigManager().getBlockRewards();
        Map<String, Double> logRewards = plugin.getConfigManager().getLogRewards();
        Map<String, Double> cropRewards = plugin.getConfigManager().getCropRewards();

        Double base = null;
        TransactionType txType = null;
        String statColumn = null;

        if (blockRewards.containsKey(matName)) {
            base = blockRewards.get(matName);
            txType = TransactionType.MINING;
            statColumn = "blocks_broken";
        } else if (logRewards.containsKey(matName)) {
            base = logRewards.get(matName);
            txType = TransactionType.WOODCUTTING;
            statColumn = "blocks_broken";
        } else if (cropRewards.containsKey(matName)) {
            // Only reward fully-grown crops, not immature ones being trampled/broken early
            if (!isFullyGrown(block)) {
                return;
            }
            base = cropRewards.get(matName);
            txType = TransactionType.FARMING;
            statColumn = "crops_harvested";
        }

        if (base == null || base <= 0) return;

        double antiFarmMultiplier = plugin.getMiningTracker().recordBreakAndGetMultiplier(player.getUniqueId(), type)
                * plugin.getAfkTracker().getMultiplierFor(player.getUniqueId());

        double adjustedBase = base * antiFarmMultiplier;
        if (adjustedBase <= 0) return;

        plugin.getEconomyManager().reward(player.getUniqueId(), adjustedBase, txType, matName);

        String column = statColumn;
        plugin.getDatabaseManager().getExecutor().submit(() -> {
            try {
                plugin.getEconomyManager().getDao().incrementStat(player.getUniqueId(), column, 1);
            } catch (Exception ignored) {
                // Stat tracking failures should never affect gameplay
            }
        });
    }

    private boolean isFullyGrown(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        // Blocks without an age property (e.g. melon/pumpkin fruit itself) are always "grown"
        return true;
    }
}
