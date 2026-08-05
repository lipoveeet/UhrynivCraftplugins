package com.uhrynivcraft.listeners;

import com.uhrynivcraft.UhrynivCraft;
import com.uhrynivcraft.model.TransactionType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {

    private final UhrynivCraft plugin;

    public EntityDeathListener(UhrynivCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String mobType = event.getEntityType().name();
        Double base = plugin.getConfigManager().getMobRewards().get(mobType);
        if (base == null || base <= 0) return;

        double spawnMultiplier = plugin.getMobSpawnTracker().getMultiplierFor(event.getEntity());
        double afkMultiplier = plugin.getAfkTracker().getMultiplierFor(killer.getUniqueId());

        double adjustedBase = base * spawnMultiplier * afkMultiplier;
        if (adjustedBase <= 0) return;

        plugin.getEconomyManager().reward(killer.getUniqueId(), adjustedBase, TransactionType.MOB_KILL, mobType);

        plugin.getDatabaseManager().getExecutor().submit(() -> {
            try {
                plugin.getEconomyManager().getDao().incrementStat(killer.getUniqueId(), "mobs_killed", 1);
            } catch (Exception ignored) {
            }
        });
    }
}
