package com.muhammaddaffa.playerprofiles.commands.subcommands;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.commands.abstraction.SubCommand;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportWebhookCommand extends SubCommand {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public @Nullable String getPermission() {
        return "playerprofiles.report";
    }

    @Override
    public @NotNull List<String> parseTabCompletion(PlayerProfiles plugin, CommandSender sender, String[] args) {
        if (args.length == 4) {
            return Arrays.asList("Hacking", "Claim_Griefing", "Scamming", "Use_of_Slurs", "TP_Trapping", "Glitch_Abuse", "Other");
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(PlayerProfiles plugin, CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Utils.color("&cUsage: /bmcplayerprofile reportwebhook <reporter> <target> <type> <reason...>"));
            return;
        }

        String reporterName = args[1];
        String targetName = args[2];
        String rawCategory = args[3];
        String formattedCategory = rawCategory.replace('_', ' ');

        String[] reasonTokens = Arrays.copyOfRange(args, 4, args.length);
        String reasonDescription = String.join(" ", reasonTokens).trim();

        if (ConfigValue.REPORT_NOTIFY_STAFF) {
            broadcastToStaff(reporterName, targetName, formattedCategory, reasonDescription);
        }

        String webhookEndpoint = ConfigValue.REPORT_WEBHOOK_URL;
        if (webhookEndpoint == null || webhookEndpoint.trim().isEmpty()) {
            Logger.info("[Reports] Report filed for " + targetName + " by " + reporterName + " (" + formattedCategory + "): " + reasonDescription);
            return;
        }

        dispatchDiscordWebhook(webhookEndpoint.trim(), reporterName, targetName, formattedCategory, reasonDescription);
    }

    private void broadcastToStaff(String reporter, String target, String category, String reason) {
        String staffNotice = Utils.color("&8[&c&lBMC REPORT&8] &e" + reporter + " &7reported &c" + target +
                " &7for &e" + category + "&7: &f" + reason);
        for (Player onlineStaff : Bukkit.getOnlinePlayers()) {
            if (onlineStaff.hasPermission(ConfigValue.REPORT_STAFF_PERMISSION) ||
                    onlineStaff.hasPermission("playerprofiles.admin") ||
                    onlineStaff.isOp()) {
                onlineStaff.sendMessage(staffNotice);
                onlineStaff.playSound(onlineStaff.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
            }
        }
    }

    private void dispatchDiscordWebhook(String webhookUrl, String reporter, String target, String category, String reason) {
        if (!webhookUrl.startsWith("https://") && !webhookUrl.startsWith("http://")) {
            Logger.warning("[Reports] Invalid Discord webhook URL configured: " + webhookUrl);
            return;
        }

        String jsonPayload = buildJsonPayload(reporter, target, category, reason);
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .header("User-Agent", "BMCPlayerProfiles-Reporter")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HTTP_CLIENT.sendAsync(postRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    int statusCode = response.statusCode();
                    if (statusCode < 200 || statusCode >= 300) {
                        Logger.warning("[Reports] Discord webhook returned HTTP status " + statusCode + ": " + response.body());
                    }
                })
                .exceptionally(throwable -> {
                    Logger.warning("[Reports] Failed to dispatch Discord report webhook: " + throwable.getMessage());
                    return null;
                });
    }

    private String buildJsonPayload(String reporter, String target, String category, String reason) {
        String safeReporter = escapeJson(reporter);
        String safeTarget = escapeJson(target);
        String safeCategory = escapeJson(category);
        String safeReason = escapeJson(reason);
        String currentTimestamp = Instant.now().toString();
        String jsonFieldKey = "val" + "ue";

        return "{"
                + "\"username\":\"BMC Reports\","
                + "\"avatar_url\":\"https://mc-heads.net/avatar/" + safeTarget + "/128.png\","
                + "\"embeds\":[{"
                + "\"title\":\"\\uD83D\\uDEA8 New Player Report\","
                + "\"color\":16717636,"
                + "\"fields\":["
                + "{\"name\":\"Reported player:\",\"" + jsonFieldKey + "\":\"`" + safeTarget + "`\",\"inline\":true},"
                + "{\"name\":\"reported by:\",\"" + jsonFieldKey + "\":\"`" + safeReporter + "`\",\"inline\":true},"
                + "{\"name\":\"Type:\",\"" + jsonFieldKey + "\":\"**" + safeCategory + "**\",\"inline\":false},"
                + "{\"name\":\"Reason:\",\"" + jsonFieldKey + "\":\"" + safeReason + "\",\"inline\":false}"
                + "],"
                + "\"thumbnail\":{\"url\":\"https://mc-heads.net/avatar/" + safeTarget + "/100.png\"},"
                + "\"footer\":{\"text\":\"BendersMC Moderation \\u2022 PlayerProfilesBMC\"},"
                + "\"timestamp\":\"" + currentTimestamp + "\""
                + "}]"
                + "}";
    }

    private String escapeJson(String rawText) {
        if (rawText == null) return "";
        StringBuilder buffer = new StringBuilder(rawText.length() + 16);
        for (int index = 0; index < rawText.length(); index++) {
            char character = rawText.charAt(index);
            switch (character) {
                case '"' -> buffer.append("\\\"");
                case '\\' -> buffer.append("\\\\");
                case '\b' -> buffer.append("\\b");
                case '\f' -> buffer.append("\\f");
                case '\n' -> buffer.append("\\n");
                case '\r' -> buffer.append("\\r");
                case '\t' -> buffer.append("\\t");
                default -> {
                    if (character < 32) {
                        buffer.append(String.format("\\u%04x", (int) character));
                    } else {
                        buffer.append(character);
                    }
                }
            }
        }
        return buffer.toString();
    }
}
