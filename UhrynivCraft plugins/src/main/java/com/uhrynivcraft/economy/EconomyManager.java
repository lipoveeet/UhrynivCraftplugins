package com.uhrynivcraft.economy;

import com.uhrynivcraft.UhrynivCraft;
import com.uhrynivcraft.antifarm.ActivityLimiter;
import com.uhrynivcraft.config.ConfigManager;
import com.uhrynivcraft.database.DatabaseManager;
import com.uhrynivcraft.database.PlayerDAO;
import com.uhrynivcraft.model.PlayerData;
import com.uhrynivcraft.model.TransactionType;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central entry point for every balance change in the plugin.
 * Keeps an in-memory cache of PlayerData for online players (fast, thread-safe)
 * and periodically flushes dirty entries to MySQL asynchronously.
 *
 * IMPORTANT: reward(...) is the ONLY method that should be used by listeners
 * granting UP for gameplay activity, since it is the one that applies the
 * anti-farm hourly-tier multiplier before crediting the balance.
 */
public class EconomyManager {

    private final UhrynivCraft plugin;
    private final ConfigManager config;
    private final DatabaseManager database;
    private final PlayerDAO dao;
    private final ActivityLimiter activityLimiter;

    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public EconomyManager(UhrynivCraft plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.database = plugin.getDatabaseManager();
        this.dao = new PlayerDAO(database);
        this.activityLimiter = new ActivityLimiter(config);
    }

    /** Loads a player's data into the cache asynchronously, then runs the callback on the main thread. */
    public void loadPlayer(Player player, Runnable onLoaded) {
        UUID uuid = player.getUniqueId();
        database.getExecutor().submit(() -> {
            try {
                PlayerData data = dao.loadOrCreate(uuid, player.getName(), config.getStartingBalance());
                cache.put(uuid, data);
            } catch (SQLException e) {
                plugin.getLogger().severe("Не вдалося завантажити дані гравця " + player.getName() + ": " + e.getMessage());
                cache.put(uuid, new PlayerData(uuid, player.getName(), 0.0, 0.0));
            } finally {
                if (onLoaded != null) {
                    plugin.getServer().getScheduler().runTask(plugin, onLoaded);
                }
            }
        });
    }

    /** Immediately persists and drops a player's cache entry (called on quit). */
    public void unloadPlayer(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        database.getExecutor().submit(() -> {
            try {
                dao.save(data);
            } catch (SQLException e) {
                plugin.getLogger().severe("Не вдалося зберегти дані гравця " + data.getName() + ": " + e.getMessage());
            }
        });
        cache.remove(uuid);
    }

    public PlayerData getCached(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * Grants a gameplay reward, applying the hourly anti-farm multiplier automatically.
     * Any additional anti-farm multiplier (mining speed, AFK, spawn source, etc.) must
     * already be baked into {@code baseAmount} by the caller before this is invoked.
     *
     * @return the actual amount credited after the hourly tier multiplier
     */
    public double reward(UUID uuid, double baseAmount, TransactionType type, String reason) {
        if (baseAmount <= 0) return 0.0;
        PlayerData data = cache.get(uuid);
        if (data == null) return 0.0;

        double hourIncome = data.getHourIncome();
        double tierMultiplier = activityLimiter.getMultiplier(hourIncome);
        double finalAmount = baseAmount * tierMultiplier;

        if (finalAmount <= 0) return 0.0;

        data.addBalance(finalAmount, true);
        logTransactionAsync(uuid, type, finalAmount, reason);
        return finalAmount;
    }

    /** Direct balance change that does NOT count towards hourly income or anti-farm tiers (pay, admin, shop). */
    public boolean addRaw(UUID uuid, double amount, TransactionType type, String reason) {
        PlayerData data = cache.get(uuid);
        if (data == null) return false;
        data.addBalance(amount, false);
        logTransactionAsync(uuid, type, amount, reason);
        return true;
    }

    public boolean subtractRaw(UUID uuid, double amount, TransactionType type, String reason) {
        PlayerData data = cache.get(uuid);
        if (data == null) return false;
        boolean success = data.subtractBalance(amount);
        if (success) {
            logTransactionAsync(uuid, type, -amount, reason);
        }
        return success;
    }

    private void logTransactionAsync(UUID uuid, TransactionType type, double amount, String reason) {
        database.getExecutor().submit(() -> {
            try {
                dao.logTransaction(uuid, type, amount, reason);
            } catch (SQLException e) {
                plugin.getLogger().warning("Не вдалося записати транзакцію: " + e.getMessage());
            }
        });
    }

    /** Periodic flush of all dirty cached entries to MySQL. Runs on the async executor. */
    public void flushDirtyEntries() {
        for (PlayerData data : cache.values()) {
            if (!data.isDirty()) continue;
            database.getExecutor().submit(() -> {
                try {
                    dao.save(data);
                    data.markClean();
                } catch (SQLException e) {
                    plugin.getLogger().warning("Не вдалося зберегти баланс гравця " + data.getName() + ": " + e.getMessage());
                }
            });
        }
    }

    /** Saves every cached entry synchronously - used on plugin disable. */
    public void saveAllSync() {
        for (PlayerData data : cache.values()) {
            try {
                dao.save(data);
            } catch (SQLException e) {
                plugin.getLogger().severe("Не вдалося зберегти дані при вимкненні: " + e.getMessage());
            }
        }
    }

    public List<PlayerDAO.TopEntry> getTopBalancesSync(int limit) throws SQLException {
        return dao.getTopBalances(limit);
    }

    public PlayerDAO getDao() {
        return dao;
    }
}
