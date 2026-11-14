package com.muhammaddaffa.playerprofiles.proxy.velocity;

import com.google.inject.Inject;
import com.muhammaddaffa.playerprofiles.proxy.ProxyProtocol;
import com.muhammaddaffa.playerprofiles.proxy.ProxyStorageFinder;
import com.muhammaddaffa.playerprofiles.proxy.SecretManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Plugin(
        id = "bmcplayerprofiles",
        name = "BMCPlayerProfiles",
        version = "8.0.4",
        authors = {"MinedKibbles21"},
        description = "Velocity proxy companion for BMCPlayerProfiles to serve player.dat across the network"
)
public class BMCPlayerProfilesVelocity {

    public static final MinecraftChannelIdentifier VELOCITY_CHANNEL =
            MinecraftChannelIdentifier.from("bmc:profiles");

    private final ProxyServer proxyInstance;
    private final Logger logOutput;
    private final Path folderPath;

    private SecretManager secretManager;
    private File playerDirectoryLocation;

    @Inject
    public BMCPlayerProfilesVelocity(ProxyServer proxyInstance, Logger logOutput, @DataDirectory Path folderPath) {
        this.proxyInstance = proxyInstance;
        this.logOutput = logOutput;
        this.folderPath = folderPath;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent initEvent) {
        this.proxyInstance.getChannelRegistrar().register(VELOCITY_CHANNEL);

        File pluginDirectory = this.folderPath.toFile();
        if (!pluginDirectory.exists()) {
            pluginDirectory.mkdirs();
        }

        this.secretManager = new SecretManager(pluginDirectory, "secret.pem");
        try {
            this.secretManager.ensureSecretLoaded();
            this.logOutput.info("[Proxy] Secret key active at {}", this.secretManager.getSecretFile().getName());
        } catch (IOException loadException) {
            this.logOutput.error("[Proxy] Could not load or generate secret.pem", loadException);
        }

        File configFile = new File(pluginDirectory, "config.yml");
        if (!configFile.exists()) {
            try {
                List<String> defaultLines = List.of(
                        "# BMCPlayerProfiles Velocity Proxy Configuration",
                        "playerdataPath: \"playerdata\""
                );
                Files.write(configFile.toPath(), defaultLines);
            } catch (IOException createException) {
                this.logOutput.warn("[Proxy] Could not create config.yml", createException);
            }
        }

        String configuredPath = "playerdata";
        if (configFile.exists()) {
            try {
                List<String> configLines = Files.readAllLines(configFile.toPath());
  for(String configLine : configLines) {

                    String trimmed = configLine.trim();
                    if (trimmed.startsWith("playerdataPath:")) {
                        String extracted = trimmed.substring("playerdataPath:".length()).trim();
                        extracted = extracted.replace("\"", "").replace("'", "");
                        if (!extracted.isEmpty()) {
                            configuredPath = extracted;
                        }
                    }
                }
            } catch (IOException readConfigException) {
                this.logOutput.warn("[Proxy] Could not read config.yml", readConfigException);
            }
        }

        File resolvedDirectory = new File(configuredPath);
        if (!resolvedDirectory.isAbsolute()) {
            resolvedDirectory = new File(pluginDirectory, configuredPath);
        }
        this.playerDirectoryLocation = resolvedDirectory;

        this.logOutput.info("[Proxy] BMCPlayerProfiles Velocity bridge ready on channel bmc:profiles");
        this.logOutput.info("[Proxy] Reading proxy profiles from: {}", this.playerDirectoryLocation.getAbsolutePath());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent messageEvent) {
        ChannelIdentifier channelId = messageEvent.getIdentifier();
        if (!VELOCITY_CHANNEL.equals(channelId)) {
            return;
        }

        byte[] rawBytes = messageEvent.getData();
        ProxyProtocol.InboundQuery query = ProxyProtocol.parseQuery(rawBytes, this.secretManager);
    if(query == null) {

            return;
        }

        messageEvent.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(messageEvent.getSource() instanceof ServerConnection serverConnection)) {
            return;
        }

        if (!query.authenticated()) {
            this.logOutput.warn("[Proxy] Rejected unauthorized query from backend server {}",
                    serverConnection.getServerInfo().getName());
            try {
                byte[] forbiddenResponse = ProxyProtocol.serializeResponse(
                        query.queryIdentifier(),
                        ProxyProtocol.STATUS_FORBIDDEN,
                        null,
                        this.secretManager
                );
                serverConnection.sendPluginMessage(VELOCITY_CHANNEL, forbiddenResponse);
            } catch (IOException responseException) {
                this.logOutput.warn("[Proxy] Failed to send forbidden reply", responseException);
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
            serverConnection.sendPluginMessage(VELOCITY_CHANNEL, responsePacket);
        } catch (IOException replyException) {
            this.logOutput.warn("[Proxy] Failed to send profile response", replyException);
        }
    }
}
