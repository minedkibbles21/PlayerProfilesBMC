package com.muhammaddaffa.playerprofiles.proxy.bungee;

import com.muhammaddaffa.playerprofiles.proxy.ProxyProtocol;
import com.muhammaddaffa.playerprofiles.proxy.ProxyStorageFinder;
import com.muhammaddaffa.playerprofiles.proxy.SecretManager;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Level;

public class BMCPlayerProfilesBungee extends Plugin implements Listener {

    private SecretManager secretManager;
    private File playerDirectoryLocation;

    @Override
    public void onEnable() {
        getProxy().registerChannel(ProxyProtocol.CHANNEL_IDENTIFIER);
        getProxy().getPluginManager().registerListener(this, this);

        File pluginDirectory = getDataFolder();
        if (!pluginDirectory.exists()) {
            pluginDirectory.mkdirs();
        }

        this.secretManager = new SecretManager(pluginDirectory, "secret.pem");
        try {
            this.secretManager.ensureSecretLoaded();
            getLogger().info("[Proxy] Secret key active at " + this.secretManager.getSecretFile().getName());
        } catch (IOException loadException) {
            getLogger().log(Level.SEVERE, "[Proxy] Could not load or generate secret.pem", loadException);
        }

        File configFile = new File(pluginDirectory, "config.yml");
        if (!configFile.exists()) {
            try (InputStream defaultStream = getResourceAsStream("config.yml")) {
      if(defaultStream != null) {

                    Files.copy(defaultStream, configFile.toPath());
                } else {
                    Configuration defaultConfig = new Configuration();
                    defaultConfig.set("playerdataPath", "playerdata");
                    ConfigurationProvider.getProvider(YamlConfiguration.class).save(defaultConfig, configFile);
                }
            } catch (IOException createException) {
                getLogger().log(Level.WARNING, "[Proxy] Could not create config.yml", createException);
            }
        }

        String configuredPath = "playerdata";
        try {
            Configuration loadedConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
            configuredPath = loadedConfig.getString("playerdataPath", "playerdata");
        } catch (IOException readConfigException) {
            getLogger().log(Level.WARNING, "[Proxy] Could not parse config.yml, using default path", readConfigException);
        }

        File resolvedDirectory = new File(configuredPath);
        if (!resolvedDirectory.isAbsolute()) {
            resolvedDirectory = new File(pluginDirectory, configuredPath);
        }
        this.playerDirectoryLocation = resolvedDirectory;

        getLogger().info("[Proxy] BMCPlayerProfiles BungeeCord bridge ready on channel " + ProxyProtocol.CHANNEL_IDENTIFIER);
        getLogger().info("[Proxy] Reading proxy profiles from: " + this.playerDirectoryLocation.getAbsolutePath());
    }

    @Override
    public void onDisable() {
        getProxy().unregisterChannel(ProxyProtocol.CHANNEL_IDENTIFIER);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent messageEvent) {
        if (!ProxyProtocol.CHANNEL_IDENTIFIER.equals(messageEvent.getTag())) {
            return;
        }

        byte[] rawBytes = messageEvent.getData();
        ProxyProtocol.InboundQuery query = ProxyProtocol.parseQuery(rawBytes, this.secretManager);
    if(query == null) {

            return;
        }

        if (!(messageEvent.getSender() instanceof Server sendingServer)) {
            return;
        }

        if (!query.authenticated()) {
            getLogger().warning("[Proxy] Rejected unauthorized query from " + sendingServer.getInfo().getName());
            try {
                byte[] forbiddenResponse = ProxyProtocol.serializeResponse(
                        query.queryIdentifier(),
                        ProxyProtocol.STATUS_FORBIDDEN,
                        null,
                        this.secretManager
                );
                sendingServer.sendData(ProxyProtocol.CHANNEL_IDENTIFIER, forbiddenResponse);
            } catch (IOException responseException) {
                getLogger().log(Level.WARNING, "[Proxy] Failed to send forbidden reply", responseException);
            }
            return;
        }

        UUID targetPlayerUuid = query.requestedPlayerUuid();
        byte[] playerContentBytes = ProxyStorageFinder.readPlayerBytes(this.playerDirectoryLocation, targetPlayerUuid);
    if(playerContentBytes == null) {

            playerContentBytes = ProxyStorageFinder.readPlayerBytes(new File("."), targetPlayerUuid);
        }

        byte statusIndicator = (playerContentBytes != null && playerContentBytes.length > 0)
                ? ProxyProtocol.STATUS_SUCCESS
                : ProxyProtocol.STATUS_ABSENT;

        try {
            byte[] responsePacket = ProxyProtocol.serializeResponse(
                    query.queryIdentifier(),
                    statusIndicator,
                    playerContentBytes,
                    this.secretManager
            );
            sendingServer.sendData(ProxyProtocol.CHANNEL_IDENTIFIER, responsePacket);
        } catch (IOException replyException) {
            getLogger().log(Level.WARNING, "[Proxy] Failed to send profile response", replyException);
        }
    }
}
