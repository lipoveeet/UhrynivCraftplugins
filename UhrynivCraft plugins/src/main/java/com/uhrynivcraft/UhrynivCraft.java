package com.uhrynivcraft;

import com.uhrynivcraft.antifarm.AfkTracker;
import com.uhrynivcraft.antifarm.MiningTracker;
import com.uhrynivcraft.antifarm.MobSpawnTracker;
import com.uhrynivcraft.antifarm.PlacedBlockTracker;
import com.uhrynivcraft.commands.UpCommand;
import com.uhrynivcraft.commands.UpShopCommand;
import com.uhrynivcraft.config.ConfigManager;
import com.uhrynivcraft.database.DatabaseManager;
import com.uhrynivcraft.economy.EconomyManager;
import com.uhrynivcraft.gui.ShopGUI;
import com.uhrynivcraft.gui.ShopListener;
import com.uhrynivcraft.listeners.BlockBreakListener;
import com.uhrynivcraft.listeners.BlockPlaceListener;
import com.uhrynivcraft.listeners.CreatureSpawnListener;
import com.uhrynivcraft.listeners.EntityDeathListener;
import com.uhrynivcraft.listeners.PlayerConnectionListener;
import com.uhrynivcraft.listeners.PlayerFishListener;
import com.uhrynivcraft.listeners.PlayerMoveListener;
import com.uhrynivcraft.placeholder.UhrynivPlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point of the UhrynivCraft plugin.
 * Owns and wires together every manager/tracker; other classes reach these
 * via the getters below rather than constructing their own instances.
 */
public final class UhrynivCraft extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;

    private MiningTracker miningTracker;
    private AfkTracker afkTracker;
    private PlacedBlockTracker placedBlockTracker;
    private MobSpawnTracker mobSpawnTracker;

    private ShopGUI shopGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(this);

        if (!databaseManager.connect()) {
            getLogger().severe("Не вдалося підключитися до MySQL! Плагін UhrynivCraft вимикається.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.economyManager = new EconomyManager(this);
        this.miningTracker = new MiningTracker(configManager);
        this.afkTracker = new AfkTracker(configManager);
        this.placedBlockTracker = new PlacedBlockTracker(this);
        this.mobSpawnTracker = new MobSpawnTracker(this);
        this.shopGUI = new ShopGUI(this);

        registerCommands();
        registerListeners();
        registerPlaceholderApi();
        loadOnlinePlayers();
        startPeriodicSaveTask();

        getLogger().info("UhrynivCraft увімкнено успішно.");
    }

    @Override
    public void onDisable() {
        if (economyManager != null) {
            economyManager.saveAllSync();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("UhrynivCraft вимкнено, дані збережено.");
    }

    private void registerCommands() {
        UpCommand upCommand = new UpCommand(this);
        getCommand("up").setExecutor(upCommand);
        getCommand("up").setTabCompleter(upCommand);
        getCommand("upshop").setExecutor(new UpShopCommand(this));
    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new BlockBreakListener(this), this);
        pm.registerEvents(new BlockPlaceListener(this), this);
        pm.registerEvents(new EntityDeathListener(this), this);
        pm.registerEvents(new CreatureSpawnListener(this), this);
        pm.registerEvents(new PlayerFishListener(this), this);
        pm.registerEvents(new PlayerMoveListener(this), this);
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new ShopListener(this), this);
    }

    private void registerPlaceholderApi() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new UhrynivPlaceholderExpansion(this).register();
            getLogger().info("Інтеграцію з PlaceholderAPI зареєстровано.");
        }
    }

    /** Handles /reload or a plugin (re)enable where players are already connected. */
    private void loadOnlinePlayers() {
        for (var player : Bukkit.getOnlinePlayers()) {
            economyManager.loadPlayer(player, null);
        }
    }

    /** Periodically flushes dirty in-memory balances to MySQL so a crash never loses more than N seconds of data. */
    private void startPeriodicSaveTask() {
        long intervalTicks = configManager.getSaveIntervalSeconds() * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> economyManager.flushDirtyEntries(),
                intervalTicks, intervalTicks);
    }

    // ---- Accessors used throughout the plugin ----

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public MiningTracker getMiningTracker() {
        return miningTracker;
    }

    public AfkTracker getAfkTracker() {
        return afkTracker;
    }

    public PlacedBlockTracker getPlacedBlockTracker() {
        return placedBlockTracker;
    }

    public MobSpawnTracker getMobSpawnTracker() {
        return mobSpawnTracker;
    }

    public ShopGUI getShopGUI() {
        return shopGUI;
    }
}
