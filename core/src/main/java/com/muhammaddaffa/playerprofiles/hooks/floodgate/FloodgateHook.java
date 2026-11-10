package com.muhammaddaffa.playerprofiles.hooks.floodgate;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FloodgateHook {

    private static Boolean enabled = null;
    private static Object floodgateApiInstance = null;
    private static Method isFloodgatePlayerMethod = null;
    private static Method isFloodgateIdMethod = null;
    private static Method getPlayerPrefixMethod = null;
    private static Method getUuidForMethod = null;

    public static boolean isEnabled() {
        if (enabled == null) {
            init();
        }
        return enabled;
    }

    private static synchronized void init() {
        if (enabled != null) return;
        try {
            if (Bukkit.getPluginManager() != null && Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
                Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Method getInstance = apiClass.getMethod("getInstance");
                floodgateApiInstance = getInstance.invoke(null);
                if (floodgateApiInstance != null) {
                    try { isFloodgatePlayerMethod = apiClass.getMethod("isFloodgatePlayer", UUID.class); } catch (Exception ignored) {}
                    try { isFloodgateIdMethod = apiClass.getMethod("isFloodgateId", UUID.class); } catch (Exception ignored) {}
                    try { getPlayerPrefixMethod = apiClass.getMethod("getPlayerPrefix"); } catch (Exception ignored) {}
                    try { getUuidForMethod = apiClass.getMethod("getUuidFor", String.class); } catch (Exception ignored) {}
                    enabled = true;
                    return;
                }
            }
        } catch (Throwable ignored) {}
        enabled = false;
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        if (uuid == null) return false;
        // Standard Floodgate Bedrock UUIDs have 0 for the most significant bits (e.g. 00000000-0000-0000-xxxx-xxxxxxxxxxxx)
        if (uuid.getMostSignificantBits() == 0L) return true;
        if (isEnabled() && floodgateApiInstance != null) {
            try {
                if (isFloodgatePlayerMethod != null) {
                    return (boolean) isFloodgatePlayerMethod.invoke(floodgateApiInstance, uuid);
                }
                if (isFloodgateIdMethod != null) {
                    return (boolean) isFloodgateIdMethod.invoke(floodgateApiInstance, uuid);
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    public static String getPrefix() {
        if (isEnabled() && floodgateApiInstance != null && getPlayerPrefixMethod != null) {
            try {
                String prefix = (String) getPlayerPrefixMethod.invoke(floodgateApiInstance);
                if (prefix != null && !prefix.isEmpty()) return prefix;
            } catch (Exception ignored) {}
        }
        return ".";
    }

    public static UUID getUuidFor(String username) {
        if (username == null || username.isBlank()) return null;
        if (isEnabled() && floodgateApiInstance != null && getUuidForMethod != null) {
            try {
                Object result = getUuidForMethod.invoke(floodgateApiInstance, username);
                if (result instanceof CompletableFuture<?> future) {
                    Object uuid = future.get(500, TimeUnit.MILLISECONDS);
                    if (uuid instanceof UUID u) return u;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
