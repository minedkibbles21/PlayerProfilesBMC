package com.muhammaddaffa.playerprofiles.manager.combat;

import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.hooks.combatlogx.HCombatLogX;
import com.muhammaddaffa.playerprofiles.hooks.deluxecombat.HDeluxeCombat;
import com.muhammaddaffa.playerprofiles.inventory.ProfileInventory;
import com.muhammaddaffa.playerprofiles.manager.DependencyManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager implements Listener {

    private static final Map<UUID, Long> combatExpirations = new ConcurrentHashMap<>();

    public static void tagCombat(Player player) {
        if (player == null) return;
        tagCombat(player.getUniqueId(), ConfigValue.DISABLE_IN_COMBAT_DURATION, System.currentTimeMillis());
    }

    public static void tagCombat(UUID playerUuid, int durationSeconds) {
        tagCombat(playerUuid, durationSeconds, System.currentTimeMillis());
    }

    public static void tagCombat(UUID playerUuid, int durationSeconds, long currentTimeMillis) {
        if (playerUuid == null || durationSeconds <= 0) return;
        long durationMillis = durationSeconds * 1000L;
        combatExpirations.put(playerUuid, currentTimeMillis + durationMillis);
    }

    public static boolean isInCombat(Player player) {
        if (player == null || !ConfigValue.DISABLE_IN_COMBAT_ENABLED) return false;

        if (DependencyManager.COMBAT_LOG_X && HCombatLogX.isInCombat(player)) return true;
        if (DependencyManager.DELUXE_COMBAT && HDeluxeCombat.isInCombat(player)) return true;

        return isInCombat(player.getUniqueId(), System.currentTimeMillis());
    }

    public static boolean isInCombat(UUID playerUuid, long currentTimeMillis) {
        if (playerUuid == null || ConfigValue.DISABLE_IN_COMBAT_DURATION <= 0) return false;

        Long expiryTime = combatExpirations.get(playerUuid);
        if (expiryTime == null) return false;

        if (expiryTime > currentTimeMillis) {
            return true;
        }

        combatExpirations.remove(playerUuid);
        return false;
    }

    public static long getRemainingSeconds(Player player) {
        if (player == null) return 0L;
        return getRemainingSeconds(player.getUniqueId(), System.currentTimeMillis());
    }

    public static long getRemainingSeconds(UUID playerUuid, long currentTimeMillis) {
        if (playerUuid == null) return 0L;
        Long expiryTime = combatExpirations.get(playerUuid);
        if (expiryTime == null) {
            return ConfigValue.DISABLE_IN_COMBAT_DURATION;
        }
        long delta = expiryTime - currentTimeMillis;
        if (delta <= 0) return 0L;
        long remaining = (delta + 999L) / 1000L;
        if (ConfigValue.DISABLE_IN_COMBAT_DURATION > 0) {
            remaining = Math.min(remaining, ConfigValue.DISABLE_IN_COMBAT_DURATION);
        }
        return Math.max(1L, remaining);
    }

    public static void clear(UUID playerUuid) {
        combatExpirations.remove(playerUuid);
    }

    public static void clearAll() {
        combatExpirations.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!ConfigValue.DISABLE_IN_COMBAT_ENABLED || ConfigValue.DISABLE_IN_COMBAT_DURATION <= 0) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolvePlayerDamager(event.getDamager());
        if (attacker != null) {
            if (ConfigValue.DISABLE_IN_COMBAT_PVP) {
                tagCombat(attacker);
                tagCombat(victim);
            }
        } else if (ConfigValue.DISABLE_IN_COMBAT_PVE) {
            tagCombat(victim);
        }

        closeOpenProfileIfConfigured(victim);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!ConfigValue.DISABLE_IN_COMBAT_ENABLED) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        if (ConfigValue.DISABLE_IN_COMBAT_PVE && !(event instanceof EntityDamageByEntityEvent) && ConfigValue.DISABLE_IN_COMBAT_DURATION > 0) {
            tagCombat(victim);
        }

        closeOpenProfileIfConfigured(victim);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId());
    }

    private void closeOpenProfileIfConfigured(Player player) {
        if (!ConfigValue.DISABLE_IN_COMBAT_CLOSE_ON_DAMAGE) return;
        try {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof ProfileInventory) {
                player.closeInventory();
            }
        } catch (Throwable ignored) {}
    }

    private Player resolvePlayerDamager(Entity damagerEntity) {
        if (damagerEntity instanceof Player directAttacker) {
            return directAttacker;
        }
        if (damagerEntity instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
