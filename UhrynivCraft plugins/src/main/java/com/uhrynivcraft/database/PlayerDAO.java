package com.uhrynivcraft.database;

import com.uhrynivcraft.model.PlayerData;
import com.uhrynivcraft.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data-access layer. Every method here does blocking JDBC I/O and must
 * only be called from the DatabaseManager's async executor, never the main thread.
 */
public class PlayerDAO {

    private final DatabaseManager db;

    public PlayerDAO(DatabaseManager db) {
        this.db = db;
    }

    /** Loads a player's row, creating a fresh one (with the given starting balance) if absent. */
    public PlayerData loadOrCreate(UUID uuid, String name, double startingBalance) throws SQLException {
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT balance, total_earned FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Keep the stored name up to date in case the player changed it
                        updateName(conn, uuid, name);
                        return new PlayerData(uuid, name, rs.getDouble("balance"), rs.getDouble("total_earned"));
                    }
                }
            }
            // No row yet - create one
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO players (uuid, name, balance, total_earned) VALUES (?, ?, ?, ?)")) {
                insert.setString(1, uuid.toString());
                insert.setString(2, name);
                insert.setDouble(3, startingBalance);
                insert.setDouble(4, 0.0);
                insert.executeUpdate();
            }
            try (PreparedStatement statsInsert = conn.prepareStatement(
                    "INSERT IGNORE INTO player_stats (uuid, blocks_broken, mobs_killed, crops_harvested) VALUES (?, 0, 0, 0)")) {
                statsInsert.setString(1, uuid.toString());
                statsInsert.executeUpdate();
            }
            return new PlayerData(uuid, name, startingBalance, 0.0);
        }
    }

    private void updateName(Connection conn, UUID uuid, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE players SET name = ? WHERE uuid = ?")) {
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    /** Persists the current balance/total_earned for a single player (used by the periodic save task). */
    public void save(PlayerData data) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE players SET name = ?, balance = ?, total_earned = ? WHERE uuid = ?")) {
            ps.setString(1, data.getName());
            ps.setDouble(2, data.getBalance());
            ps.setDouble(3, data.getTotalEarned());
            ps.setString(4, data.getUuid().toString());
            ps.executeUpdate();
        }
    }

    public void logTransaction(UUID uuid, TransactionType type, double amount, String reason) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO transactions (uuid, type, amount, reason) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());
            ps.setDouble(3, amount);
            ps.setString(4, reason);
            ps.executeUpdate();
        }
    }

    public record TopEntry(String name, double balance) {}

    public List<TopEntry> getTopBalances(int limit) throws SQLException {
        List<TopEntry> results = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT name, balance FROM players ORDER BY balance DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new TopEntry(rs.getString("name"), rs.getDouble("balance")));
                }
            }
        }
        return results;
    }

    public void incrementStat(UUID uuid, String column, long amount) throws SQLException {
        // column is never user input - only called internally with fixed literals below
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO player_stats (uuid, " + column + ") VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE " + column + " = " + column + " + VALUES(" + column + ")")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, amount);
            ps.executeUpdate();
        }
    }

    /** Reads and, if outside the current hour bucket, resets the persisted activity_limits earned value. */
    public double getActivityEarned(UUID uuid, String action, String hourBucket) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT earned FROM activity_limits WHERE uuid = ? AND action = ? AND hour = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, action);
            ps.setString(3, hourBucket);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("earned");
                }
            }
        }
        return 0.0;
    }

    public void addActivityEarned(UUID uuid, String action, String hourBucket, double amount) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO activity_limits (uuid, action, hour, earned) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE earned = earned + VALUES(earned)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, action);
            ps.setString(3, hourBucket);
            ps.setDouble(4, amount);
            ps.executeUpdate();
        }
    }
}
