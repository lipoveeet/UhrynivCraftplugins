package com.uhrynivcraft.antifarm;

import com.uhrynivcraft.config.ConfigManager;

/**
 * Applies the hourly tiered reward multiplier (0-500 = 100%, 500-1000 = 50%, ...).
 * The multiplier used for a reward is based on how much the player has already
 * earned in the rolling current hour, BEFORE this reward is added.
 */
public class ActivityLimiter {

    private final ConfigManager config;

    public ActivityLimiter(ConfigManager config) {
        this.config = config;
    }

    /**
     * @param currentHourIncome UP already earned by the player in the last rolling hour
     * @return multiplier in range [0, 1] to apply to the base reward
     */
    public double getMultiplier(double currentHourIncome) {
        for (ConfigManager.HourTier tier : config.getHourTiers()) {
            if (tier.upTo() < 0 || currentHourIncome < tier.upTo()) {
                return tier.multiplier();
            }
        }
        // Fallback - should not happen if config always has a -1 (infinite) tier
        return 0.05;
    }
}
