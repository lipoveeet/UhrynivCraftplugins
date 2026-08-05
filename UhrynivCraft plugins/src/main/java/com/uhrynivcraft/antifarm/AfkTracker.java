package com.uhrynivcraft.antifarm;

import com.uhrynivcraft.config.ConfigManager;
import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks whether a player has been standing (near-)still for too long.
 * Fed by PlayerMoveListener; consulted by every reward source to zero
 * out rewards for AFK mob/crop/fishing farms driven by autoclickers or
 * AFK pools where the player never actually moves.
 */
public class AfkTracker {

    private final ConfigManager config;

    private record PositionRecord(double x, double y, double z, long stillSinceMillis) {}

    private final Map<UUID, PositionRecord> lastPositions = new ConcurrentHashMap<>();

    public AfkTracker(ConfigManager config) {
        this.config = config;
    }

    /** Call on every PlayerMoveEvent to update tracked position/stillness. */
    public void onMove(UUID uuid, Location to) {
        PositionRecord prev = lastPositions.get(uuid);
        double tolerance = config.getAfkMovementTolerance();

        if (prev == null) {
            lastPositions.put(uuid, new PositionRecord(to.getX(), to.getY(), to.getZ(), System.currentTimeMillis()));
            return;
        }

        double dx = Math.abs(to.getX() - prev.x());
        double dy = Math.abs(to.getY() - prev.y());
        double dz = Math.abs(to.getZ() - prev.z());
        boolean movedSignificantly = (dx + dy + dz) > tolerance;

        if (movedSignificantly) {
            // Reset the "still since" timer
            lastPositions.put(uuid, new PositionRecord(to.getX(), to.getY(), to.getZ(), System.currentTimeMillis()));
        }
        // If not moved significantly, keep the original stillSinceMillis (position stays effectively the same)
    }

    /** @return true if the player has been standing still longer than the configured threshold. */
    public boolean isAfk(UUID uuid) {
        PositionRecord record = lastPositions.get(uuid);
        if (record == null) return false;
        long stillMillis = System.currentTimeMillis() - record.stillSinceMillis();
        return stillMillis >= config.getAfkStillSeconds() * 1000L;
    }

    public double getMultiplierFor(UUID uuid) {
        return isAfk(uuid) ? config.getAfkMultiplier() : 1.0;
    }

    public void clearPlayer(UUID uuid) {
        lastPositions.remove(uuid);
    }
}
