package com.uhrynivcraft.config;

import com.uhrynivcraft.UhrynivCraft;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central place to read config.yml. Call reload() to re-read from disk
 * (used by /up reload). Other classes should always go through this
 * manager rather than caching values themselves so a reload takes effect
 * immediately everywhere.
 */
public class ConfigManager {

    private final UhrynivCraft plugin;
    private FileConfiguration config;

    // Cached, parsed reward maps (material name -> reward)
    private Map<String, Double> blockRewards = new LinkedHashMap<>();
    private Map<String, Double> logRewards = new LinkedHashMap<>();
    private Map<String, Double> cropRewards = new LinkedHashMap<>();
    private Map<String, Double> fishRewards = new LinkedHashMap<>();
    private Map<String, Double> mobRewards = new LinkedHashMap<>();

    // Anti-farm hourly tiers, sorted ascending by "up-to"
    private List<HourTier> hourTiers = new ArrayList<>();

    public record HourTier(double upTo, double multiplier) {}

    public ConfigManager(UhrynivCraft plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        blockRewards = readRewardSection("rewards.blocks");
        logRewards = readRewardSection("rewards.logs");
        cropRewards = readRewardSection("rewards.crops");
        fishRewards = readRewardSection("rewards.fish");
        mobRewards = readRewardSection("rewards.mobs");

        hourTiers.clear();
        List<Map<?, ?>> rawTiers = config.getMapList("antifarm.limits");
        for (Map<?, ?> raw : rawTiers) {
            Object upToObj = raw.get("up-to");
            Object multObj = raw.get("multiplier");
            double upTo = (upToObj instanceof Number) ? ((Number) upToObj).doubleValue() : -1;
            double mult = (multObj instanceof Number) ? ((Number) multObj).doubleValue() : 1.0;
            hourTiers.add(new HourTier(upTo, mult));
        }
        // Ensure sorted ascending, with -1 (infinite) always last
        hourTiers.sort((a, b) -> {
            if (a.upTo() < 0) return 1;
            if (b.upTo() < 0) return -1;
            return Double.compare(a.upTo(), b.upTo());
        });
    }

    private Map<String, Double> readRewardSection(String path) {
        Map<String, Double> map = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            map.put(key.toUpperCase(), section.getDouble(key));
        }
        return map;
    }

    public FileConfiguration raw() {
        return config;
    }

    public Map<String, Double> getBlockRewards() {
        return blockRewards;
    }

    public Map<String, Double> getLogRewards() {
        return logRewards;
    }

    public Map<String, Double> getCropRewards() {
        return cropRewards;
    }

    public Map<String, Double> getFishRewards() {
        return fishRewards;
    }

    public Map<String, Double> getMobRewards() {
        return mobRewards;
    }

    public List<HourTier> getHourTiers() {
        return hourTiers;
    }

    public String getCurrencySymbol() {
        return config.getString("economy.currency-symbol", "UP");
    }

    public int getSaveIntervalSeconds() {
        return config.getInt("economy.save-interval-seconds", 60);
    }

    public double getStartingBalance() {
        return config.getDouble("economy.starting-balance", 0.0);
    }

    // ---- Anti-farm settings ----

    public int getMaxBlocksPerSecond() {
        return config.getInt("antifarm.mining-speed.max-blocks-per-second", 8);
    }

    public double getOverSpeedMultiplier() {
        return config.getDouble("antifarm.mining-speed.over-speed-multiplier", 0.0);
    }

    public int getSameBlockStreakThreshold() {
        return config.getInt("antifarm.mining-speed.same-block-streak-threshold", 40);
    }

    public double getSameBlockDecayPerBlock() {
        return config.getDouble("antifarm.mining-speed.same-block-decay-per-block", 0.02);
    }

    public double getSameBlockMultiplierFloor() {
        return config.getDouble("antifarm.mining-speed.same-block-multiplier-floor", 0.1);
    }

    public boolean isPlacedBlockProtectionEnabled() {
        return config.getBoolean("antifarm.placed-blocks.enabled", true);
    }

    public int getAfkStillSeconds() {
        return config.getInt("antifarm.afk.still-seconds-threshold", 20);
    }

    public double getAfkMovementTolerance() {
        return config.getDouble("antifarm.afk.movement-tolerance", 0.35);
    }

    public double getAfkMultiplier() {
        return config.getDouble("antifarm.afk.afk-multiplier", 0.0);
    }

    public double getNaturalMobMultiplier() {
        return config.getDouble("antifarm.mobs.natural-multiplier", 1.0);
    }

    public double getSpawnerMobMultiplier() {
        return config.getDouble("antifarm.mobs.spawner-multiplier", 0.2);
    }

    public double getSpawnEggMobMultiplier() {
        return config.getDouble("antifarm.mobs.spawn-egg-multiplier", 0.0);
    }

    public double getOtherMobMultiplier() {
        return config.getDouble("antifarm.mobs.other-multiplier", 0.0);
    }

    // ---- Messages ----

    public String getMessage(String key) {
        return config.getString("messages." + key, "&c[Missing message: " + key + "]");
    }

    // ---- MySQL ----

    public String getMysqlHost() {
        return config.getString("mysql.host", "localhost");
    }

    public int getMysqlPort() {
        return config.getInt("mysql.port", 3306);
    }

    public String getMysqlDatabase() {
        return config.getString("mysql.database", "uhrynivcraft");
    }

    public String getMysqlUsername() {
        return config.getString("mysql.username", "root");
    }

    public String getMysqlPassword() {
        return config.getString("mysql.password", "");
    }

    public boolean getMysqlUseSSL() {
        return config.getBoolean("mysql.useSSL", false);
    }

    public int getPoolMaxSize() {
        return config.getInt("mysql.pool.maximum-pool-size", 10);
    }

    public int getPoolMinIdle() {
        return config.getInt("mysql.pool.minimum-idle", 2);
    }

    public long getPoolConnectionTimeout() {
        return config.getLong("mysql.pool.connection-timeout-ms", 10000);
    }

    public long getPoolIdleTimeout() {
        return config.getLong("mysql.pool.idle-timeout-ms", 600000);
    }

    public long getPoolMaxLifetime() {
        return config.getLong("mysql.pool.max-lifetime-ms", 1800000);
    }
}
