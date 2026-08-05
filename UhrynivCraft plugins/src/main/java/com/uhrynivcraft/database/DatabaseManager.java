package com.uhrynivcraft.database;

import com.uhrynivcraft.UhrynivCraft;
import com.uhrynivcraft.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Owns the HikariCP connection pool and an async executor used for
 * all MySQL I/O so the main server thread is never blocked.
 */
public class DatabaseManager {

    private final UhrynivCraft plugin;
    private HikariDataSource dataSource;
    private ExecutorService executor;

    public DatabaseManager(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    /** @return true if the connection pool and schema initialized successfully. */
    public boolean connect() {
        ConfigManager cfg = plugin.getConfigManager();

        HikariConfig hikariConfig = new HikariConfig();
        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%s&autoReconnect=true&characterEncoding=utf8&useUnicode=true",
                cfg.getMysqlHost(), cfg.getMysqlPort(), cfg.getMysqlDatabase(), cfg.getMysqlUseSSL()
        );
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(cfg.getMysqlUsername());
        hikariConfig.setPassword(cfg.getMysqlPassword());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(cfg.getPoolMaxSize());
        hikariConfig.setMinimumIdle(cfg.getPoolMinIdle());
        hikariConfig.setConnectionTimeout(cfg.getPoolConnectionTimeout());
        hikariConfig.setIdleTimeout(cfg.getPoolIdleTimeout());
        hikariConfig.setMaxLifetime(cfg.getPoolMaxLifetime());
        hikariConfig.setPoolName("UhrynivCraft-Pool");

        // Sensible perf defaults for MySQL Connector/J
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            this.dataSource = new HikariDataSource(hikariConfig);
            this.executor = Executors.newFixedThreadPool(Math.max(2, cfg.getPoolMaxSize() / 2));
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Не вдалося підключитися до MySQL: " + e.getMessage());
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(16) NOT NULL,
                    balance DOUBLE NOT NULL DEFAULT 0,
                    total_earned DOUBLE NOT NULL DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    amount DOUBLE NOT NULL,
                    reason VARCHAR(100),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_transactions_uuid (uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS activity_limits (
                    uuid VARCHAR(36) NOT NULL,
                    action VARCHAR(50) NOT NULL,
                    hour VARCHAR(30) NOT NULL,
                    earned DOUBLE NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid, action, hour)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid VARCHAR(36) PRIMARY KEY,
                    blocks_broken BIGINT NOT NULL DEFAULT 0,
                    mobs_killed BIGINT NOT NULL DEFAULT 0,
                    crops_harvested BIGINT NOT NULL DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
