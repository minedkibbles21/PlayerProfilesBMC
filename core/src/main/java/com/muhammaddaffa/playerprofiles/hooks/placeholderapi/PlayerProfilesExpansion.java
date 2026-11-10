package com.muhammaddaffa.playerprofiles.hooks.placeholderapi;

import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class PlayerProfilesExpansion extends PlaceholderExpansion {

    private final PlayerProfiles plugin;
    private final String identifier;

    public PlayerProfilesExpansion(PlayerProfiles plugin, String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public @NotNull String getAuthor() {
        return "MinedKibbles21";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("locked")) {
            if (player == null) return "false";
            return String.valueOf(plugin.getProfileManager().isProfileLocked(player.getUniqueId()));
        }
        if (lower.equals("is_locked")) {
            if (player == null) return "no";
            return plugin.getProfileManager().isProfileLocked(player.getUniqueId()) ? "yes" : "no";
        }
        if (lower.equals("status")) {
            if (player == null) return "&aUnlocked";
            return plugin.getProfileManager().isProfileLocked(player.getUniqueId()) ? "&cLocked" : "&aUnlocked";
        }
        if (lower.equals("status_plain")) {
            if (player == null) return "Unlocked";
            return plugin.getProfileManager().isProfileLocked(player.getUniqueId()) ? "Locked" : "Unlocked";
        }
        if (lower.equals("symbol") || lower.equals("lock_symbol")) {
            if (player == null) return "🔓";
            return plugin.getProfileManager().isProfileLocked(player.getUniqueId()) ? "🔒" : "🔓";
        }
        if (lower.startsWith("locked_")) {
            String targetName = params.substring("locked_".length());
            OfflinePlayer target = resolveTarget(targetName);
            if (target != null) {
                return String.valueOf(plugin.getProfileManager().isProfileLocked(target.getUniqueId()));
            }
        }
        if (lower.startsWith("is_locked_")) {
            String targetName = params.substring("is_locked_".length());
            OfflinePlayer target = resolveTarget(targetName);
            if (target != null) {
                return plugin.getProfileManager().isProfileLocked(target.getUniqueId()) ? "yes" : "no";
            }
        }
        if (lower.startsWith("status_plain_")) {
            String targetName = params.substring("status_plain_".length());
            OfflinePlayer target = resolveTarget(targetName);
            if (target != null) {
                return plugin.getProfileManager().isProfileLocked(target.getUniqueId()) ? "Locked" : "Unlocked";
            }
        }
        if (lower.startsWith("status_")) {
            String targetName = params.substring("status_".length());
            OfflinePlayer target = resolveTarget(targetName);
            if (target != null) {
                return plugin.getProfileManager().isProfileLocked(target.getUniqueId()) ? "&cLocked" : "&aUnlocked";
            }
        }
        if (lower.startsWith("lock_symbol_")) {
            String targetName = params.substring("lock_symbol_".length());
            OfflinePlayer target = resolveTarget(targetName);
            if (target != null) {
                return plugin.getProfileManager().isProfileLocked(target.getUniqueId()) ? "🔒" : "🔓";
            }
        }
        if (lower.startsWith("symbol_")) {
            String targetName = params.substring("symbol_".length());
            OfflinePlayer target = resolveTarget(targetName);
            if (target != null) {
                return plugin.getProfileManager().isProfileLocked(target.getUniqueId()) ? "🔒" : "🔓";
            }
        }
        if (lower.startsWith("can_view_")) {
            String targetName = params.substring("can_view_".length());
            OfflinePlayer target = resolveTarget(targetName);
            if (target != null && player instanceof Player onlineViewer) {
                return String.valueOf(plugin.getInventoryManager().canView(onlineViewer, target.getPlayer() != null ? target.getPlayer() : onlineViewer));
            }
            return "false";
        }
        return null;
    }

    private OfflinePlayer resolveTarget(String targetName) {
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) return online;
        return com.muhammaddaffa.playerprofiles.utils.PlayerDataLoader.resolveOfflinePlayer(targetName);
    }
}
