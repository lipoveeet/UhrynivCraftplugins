package com.uhrynivcraft.antifarm;

import com.uhrynivcraft.config.ConfigManager;
import org.bukkit.Material;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player block-breaking behaviour to detect autoclickers/macros
 * (breaking blocks faster than humanly possible) and one-block-type farms
 * (e.g. AFK cobblestone generator on a macro).
 */
public class MiningTracker {

    private final ConfigManager config;

    private record BreakWindow(long windowStartMillis, int countInWindow) {}
    private record StreakState(Material material, int streak) {}

    private final Map<UUID, BreakWindow> breakWindows = new ConcurrentHashMap<>();
    private final Map<UUID, StreakState> streaks = new ConcurrentHashMap<>();

    public MiningTracker(ConfigManager config) {
        this.config = config;
    }

    /**
     * Records a block break and returns the anti-farm multiplier that should be
     * applied on top of the hourly-tier multiplier (speed check * streak decay).
     */
    public double recordBreakAndGetMultiplier(UUID uuid, Material material) {
        double speedMultiplier = checkSpeed(uuid);
        double streakMultiplier = checkStreak(uuid, material);
        return speedMultiplier * streakMultiplier;
    }

    private double checkSpeed(UUID uuid) {
        long now = System.currentTimeMillis();
        BreakWindow window = breakWindows.get(uuid);

        if (window == null || now - window.windowStartMillis() >= 1000) {
            // Start a new 1-second window
            breakWindows.put(uuid, new BreakWindow(now, 1));
            return 1.0;
        }

        int newCount = window.countInWindow() + 1;
        breakWindows.put(uuid, new BreakWindow(window.windowStartMillis(), newCount));

        if (newCount > config.getMaxBlocksPerSecond()) {
            return config.getOverSpeedMultiplier();
        }
        return 1.0;
    }

    private double checkStreak(UUID uuid, Material material) {
        StreakState state = streaks.get(uuid);
        int newStreak;
        if (state == null || state.material() != material) {
            newStreak = 1;
        } else {
            newStreak = state.streak() + 1;
        }
        streaks.put(uuid, new StreakState(material, newStreak));

        int threshold = config.getSameBlockStreakThreshold();
        if (newStreak <= threshold) {
            return 1.0;
        }
        int over = newStreak - threshold;
        double decayed = 1.0 - (over * config.getSameBlockDecayPerBlock());
        return Math.max(config.getSameBlockMultiplierFloor(), decayed);
    }

    public void clearPlayer(UUID uuid) {
        breakWindows.remove(uuid);
        streaks.remove(uuid);
    }
}
