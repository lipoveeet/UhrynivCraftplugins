package com.uhrynivcraft.antifarm;

import com.uhrynivcraft.UhrynivCraft;
import com.uhrynivcraft.config.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Tags mobs with how they were spawned (natural / spawner / egg / other) so that
 * EntityDeathListener can look up the correct reward multiplier at kill time,
 * per the anti-farm spec: natural=100%, spawner=20%, egg=0%.
 */
public class MobSpawnTracker {

    private final NamespacedKey key;
    private final ConfigManager config;

    public MobSpawnTracker(UhrynivCraft plugin) {
        this.key = new NamespacedKey(plugin, "spawn_reason");
        this.config = plugin.getConfigManager();
    }

    public void tag(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, reason.name());
    }

    public double getMultiplierFor(Entity entity) {
        String reason = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (reason == null) {
            // No tag = spawned before the plugin loaded, or edge-case entity type; treat as natural
            return config.getNaturalMobMultiplier();
        }
        return switch (reason) {
            case "NATURAL", "JOCKEY", "REINFORCEMENTS", "VILLAGE_DEFENSE", "VILLAGE_INVASION",
                 "BREEDING", "MOUNT", "PATROL", "RAID", "NETHER_PORTAL", "CHUNK_GEN" ->
                    config.getNaturalMobMultiplier();
            case "SPAWNER", "SPAWNER_EGG" -> config.getSpawnerMobMultiplier();
            case "EGG", "SHOULDER_ENTITY", "DISPENSE_EGG", "CUSTOM" -> config.getSpawnEggMobMultiplier();
            default -> config.getOtherMobMultiplier();
        };
    }
}
