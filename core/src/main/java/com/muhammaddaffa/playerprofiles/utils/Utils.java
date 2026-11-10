package com.muhammaddaffa.playerprofiles.utils;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.xseries.XSound;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.inventory.ProfileTarget;
import com.muhammaddaffa.playerprofiles.manager.DependencyManager;
import com.muhammaddaffa.playerprofiles.manager.profile.ProfileManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Utils {

    private static final java.util.regex.Pattern BRACKET_HEX_PATTERN =
            java.util.regex.Pattern.compile("\\{([#][0-9a-fA-F]{6})\\}");

    public static String color(String message) {
        if (message == null) return null;
        if (message.isEmpty()) return "";
        if (message.indexOf('{') != -1 && message.indexOf('#') != -1) {
            message = BRACKET_HEX_PATTERN.matcher(message).replaceAll("<$1>");
        }
        return Common.color(message);
    }

    public static List<String> color(List<String> messages) {
        if (messages == null) return null;
        return messages.stream().map(Utils::color).collect(Collectors.toList());
    }

    public static boolean hasCustomModelData(){
        return ItemManager.isVersionAtLeast(1, 14, 0);
    }

    public static String tryParsePAPI(@NotNull String message, Player player, Player target){
        return tryParsePAPI(message, player, ProfileTarget.of(target));
    }

    public static List<String> tryParsePAPI(@NotNull List<String> messages, Player player, Player target){
        return tryParsePAPI(messages, player, ProfileTarget.of(target));
    }

    public static String tryParsePAPI(@NotNull String message, Player player, ProfileTarget target){
        // Get the profile manager
        ProfileManager profileManager = PlayerProfiles.getInstance().getProfileManager();
        boolean pLocked = profileManager.isProfileLocked(player);
        boolean tLocked = target.isProfileLocked();
        String playerStatus = pLocked ? "&cLocked" : "&aUnlocked";
        String targetStatus = target.isOnline() ? (tLocked ? "&cLocked" : "&aUnlocked") : (tLocked ? "&cLocked" : "&7Offline");
        String playerStatusPlain = pLocked ? "Locked" : "Unlocked";
        String targetStatusPlain = target.isOnline() ? (tLocked ? "Locked" : "Unlocked") : (tLocked ? "Locked" : "Offline");
        String playerSymbol = pLocked ? "🔒" : "🔓";
        String targetSymbol = tLocked ? "🔒" : (target.isOnline() ? "🔓" : "⚪");

        String papiApplied;
        if (target.isOnline() && target.getPlayer() != null) {
            papiApplied = Common.papi(target.getPlayer(), message);
        } else if (DependencyManager.PLACEHOLDER_API) {
            papiApplied = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(target.getOfflinePlayer(), message);
        } else {
            papiApplied = message;
        }

        Player onlineTarget = target.getPlayer();

        return papiApplied
                .replace("{player}", player.getName())
                .replace("{target}", target.getName())
                .replace("{player_displayname}", player.getDisplayName())
                .replace("{target_displayname}", target.getDisplayName())
                .replace("{player_status}", playerStatus)
                .replace("{target_status}", targetStatus)
                .replace("{player_status_plain}", playerStatusPlain)
                .replace("{target_status_plain}", targetStatusPlain)
                .replace("{player_status_symbol}", playerSymbol)
                .replace("{target_status_symbol}", targetSymbol)
                .replace("{target_is_bedrock}", String.valueOf(target.isBedrock()))
                .replace("{player_health}", String.valueOf(player.getHealth()))
                .replace("{target_health}", onlineTarget != null ? String.valueOf(onlineTarget.getHealth()) : "0")
                .replace("{player_exp}", String.valueOf(player.getExp()))
                .replace("{target_exp}", onlineTarget != null ? String.valueOf(onlineTarget.getExp()) : "0")
                .replace("{player_level}", String.valueOf(player.getLevel()))
                .replace("{target_level}", onlineTarget != null ? String.valueOf(onlineTarget.getLevel()) : "0")
                .replace("{player_ping}", String.valueOf(player.getPing()))
                .replace("{target_ping}", onlineTarget != null ? String.valueOf(onlineTarget.getPing()) : "0")
                .replace("{player_gamemode}", player.getGameMode().name())
                .replace("{target_gamemode}", onlineTarget != null ? onlineTarget.getGameMode().name() : "OFFLINE")
                .replace("{player_uuid}", player.getUniqueId().toString())
                .replace("{target_uuid}", target.getUniqueId().toString())
                .replace("{player_world}", player.getWorld().getName())
                .replace("{target_world}", onlineTarget != null ? onlineTarget.getWorld().getName() : "None");
    }

    public static List<String> tryParsePAPI(@NotNull List<String> messages, Player player, ProfileTarget target){
        return messages.stream().map(message -> tryParsePAPI(message, player, target)).collect(Collectors.toList());
    }

    public static void playSound(Player player, String soundPath){
        // Get the config.yml
        FileConfiguration config = PlayerProfiles.CONFIG_DEFAULT.getConfig();
        // Get the path to the sound
        String path = "sounds." + soundPath;
        // Return if the sound isn't enabled
        if(!config.getBoolean(path + ".enabled"))
            return;
        // Get the 1.8 - 1.17 sound support optional
        Optional<XSound> optional = XSound.matchXSound(config.getString(path + ".sound"));
        // If the sound is invalid, return
        if(!optional.isPresent())
            return;
        // Get the player location
        Location location = player.getLocation();
        // Get the sound from the optional XSound
        Sound sound = optional.get().parseSound();
        // Get the volume
        float volume = (float) config.getDouble(path + ".volume");
        // Get the pitch
        float pitch = (float) config.getDouble(path + ".pitch");
        // Finally play the sound to the player
        if (sound != null) player.playSound(location, sound, volume, pitch);
    }

    public static boolean hasOffHand() {
        return ItemManager.isVersionAtLeast(1, 9, 0);
    }

}
