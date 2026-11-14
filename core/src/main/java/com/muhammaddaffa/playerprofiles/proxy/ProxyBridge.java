package com.muhammaddaffa.playerprofiles.proxy;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ProxyBridge {

    private record ProfileCacheEntry(byte[] rawNbtBytes, long expiryTimestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTimestamp;
        }
    }

    private static PlayerProfiles pluginInstance;
    private static SecretManager secretManager;
    private static boolean bridgeActive = false;

    private static final Map<UUID, CompletableFuture<byte[]>> pendingQueries = new ConcurrentHashMap<>();
    private static final Map<UUID, ProfileCacheEntry> profileCache = new ConcurrentHashMap<>();

    public static synchronized void initialize(PlayerProfiles mainPlugin) {
        pluginInstance = mainPlugin;
  if(!ConfigValue.PROXY_ENABLED) {

            bridgeActive = false;
            return;
        }

        secretManager = new SecretManager(mainPlugin.getDataFolder(), ConfigValue.PROXY_SECRET_FILE);
        try {
            secretManager.ensureSecretLoaded();
            Logger.info("&6[BMCPlayerProfiles] &a[Proxy] Loaded secret key from &e" + secretManager.getSecretFile().getName());
            Logger.info("&6[BMCPlayerProfiles] &e[Proxy] Copy secret.pem to your BungeeCord or Velocity proxy to link them.");
        } catch (IOException loadException) {
            Logger.info("&c[BMCPlayerProfiles] [Proxy] Failed to load/generate secret.pem: " + loadException.getMessage());
        }

        try {
            mainPlugin.getServer().getMessenger().registerOutgoingPluginChannel(mainPlugin, ProxyProtocol.CHANNEL_IDENTIFIER);
            mainPlugin.getServer().getMessenger().registerIncomingPluginChannel(
                    mainPlugin,
                    ProxyProtocol.CHANNEL_IDENTIFIER,
                    new PluginMessageListener() {
                        @Override
                        public void onPluginMessageReceived(@NotNull String channelName, @NotNull Player carrierPlayer, @NotNull byte[] incomingPayload) {
                            handleIncomingResponse(incomingPayload);
                        }
                    }
            );
            bridgeActive = true;
            Logger.info("&6[BMCPlayerProfiles] &a[Proxy] Bridge active on channel &e" + ProxyProtocol.CHANNEL_IDENTIFIER);
        } catch (Exception registrationException) {
            Logger.info("&c[BMCPlayerProfiles] [Proxy] Channel registration failed: " + registrationException.getMessage());
        }
    }

    public static synchronized void shutdown() {
  if(!bridgeActive || pluginInstance == null) {

            return;
        }
        try {
            pluginInstance.getServer().getMessenger().unregisterOutgoingPluginChannel(pluginInstance, ProxyProtocol.CHANNEL_IDENTIFIER);
            pluginInstance.getServer().getMessenger().unregisterIncomingPluginChannel(pluginInstance, ProxyProtocol.CHANNEL_IDENTIFIER);
        } catch (Exception unregisterException) {
            // Channel cleanup
        }
        pendingQueries.clear();
        profileCache.clear();
        bridgeActive = false;
    }

    private static void handleIncomingResponse(byte[] incomingPayload) {
      if(secretManager == null || incomingPayload == null) {

            return;
        }

        ProxyProtocol.InboundResponse response = ProxyProtocol.parseResponse(incomingPayload, secretManager);
        if (response == null || !response.authenticated()) {
            return;
        }

        CompletableFuture<byte[]> pendingFuture = pendingQueries.remove(response.queryIdentifier());
    if(pendingFuture != null) {

            byte[] deliveredBytes = (response.statusIndicator() == ProxyProtocol.STATUS_SUCCESS)
                    ? response.contentBytes()
                    : null;
            pendingFuture.complete(deliveredBytes);
        }
    }

    public static byte[] fetchPlayerNbtBytes(UUID targetPlayerUuid) {
  if(!ConfigValue.PROXY_ENABLED || targetPlayerUuid == null) {

            return null;
        }

        ProfileCacheEntry existingEntry = profileCache.get(targetPlayerUuid);
        if (existingEntry != null && !existingEntry.isExpired()) {
            return existingEntry.rawNbtBytes();
        }

        String operationMode = ConfigValue.PROXY_MODE != null ? ConfigValue.PROXY_MODE.toUpperCase() : "AUTO";

        // Direct shared storage lookup
        if ("SHARED_FOLDER".equals(operationMode) || "AUTO".equals(operationMode)) {
            File configuredDirectory = new File(ConfigValue.PROXY_PLAYERDATA_PATH);
            byte[] directStorageBytes = ProxyStorageFinder.readPlayerBytes(configuredDirectory, targetPlayerUuid);
    if(directStorageBytes != null && directStorageBytes.length > 0) {

                profileCache.put(targetPlayerUuid, new ProfileCacheEntry(directStorageBytes, System.currentTimeMillis() + 30_000L));
                return directStorageBytes;
            }
            if ("SHARED_FOLDER".equals(operationMode)) {
                return null;
            }
        }

        // Plugin messaging bridge lookup
    if(secretManager == null || pluginInstance == null) {

            return null;
        }

        Player activeCarrier = null;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.isOnline()) {
                activeCarrier = onlinePlayer;
                break;
            }
        }
      if(activeCarrier == null) {

            return null;
        }

        UUID queryIdentifier = UUID.randomUUID();
        CompletableFuture<byte[]> responseFuture = new CompletableFuture<>();
        pendingQueries.put(queryIdentifier, responseFuture);

        try {
            byte[] queryPacket = ProxyProtocol.serializeQuery(queryIdentifier, targetPlayerUuid, secretManager);
            activeCarrier.sendPluginMessage(pluginInstance, ProxyProtocol.CHANNEL_IDENTIFIER, queryPacket);

            long timeoutDuration = ConfigValue.PROXY_TIMEOUT_MILLIS;
            byte[] retrievedBytes = responseFuture.get(timeoutDuration, TimeUnit.MILLISECONDS);
  if(retrievedBytes != null && retrievedBytes.length > 0) {

                profileCache.put(targetPlayerUuid, new ProfileCacheEntry(retrievedBytes, System.currentTimeMillis() + 30_000L));
                return retrievedBytes;
            }
        } catch (Exception queryException) {
            // Timeout or transmission error
        } finally {
            pendingQueries.remove(queryIdentifier);
        }

        return null;
    }

    public static SecretManager getSecretManager() {
        return secretManager;
    }

    public static boolean isBridgeActive() {
        return bridgeActive;
    }
}
