package com.muhammaddaffa.playerprofiles.commands;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.manager.profile.Profile;
import com.muhammaddaffa.playerprofiles.utils.PlayerDataLoader;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ToggleCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "toggleprofile";
    private final PlayerProfiles plugin;

    public ToggleCommand(PlayerProfiles plugin) {
        this.plugin = plugin;
    }

    public void registerThisCommand() {
        plugin.getCommand(COMMAND_NAME).setExecutor(this);
        plugin.getCommand(COMMAND_NAME).setTabCompleter(this);

        FileConfiguration config = PlayerProfiles.CONFIG_DEFAULT.getConfig();
        List<String> aliases = config.getStringList("commandAliases.toggleprofile");
        if (aliases.isEmpty()) {
            aliases = config.getStringList("commandAliases.toggleProfile");
        }
        plugin.getCommand(COMMAND_NAME).getAliases().addAll(aliases);

        try {
            CommandMap commandMap = null;
            try {
                commandMap = Bukkit.getCommandMap();
            } catch (Throwable ignored) {}
            if (commandMap == null) {
                Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                bukkitCommandMap.setAccessible(true);
                commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            }
            if (commandMap != null) {
                commandMap.register(COMMAND_NAME, plugin.getCommand(COMMAND_NAME));
            }
        } catch (Exception ex) {
            Logger.info("&cFailed to register /toggleprofile command");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("playerprofiles.admin") && !sender.hasPermission("playerprofiles.toggleprofile.others")) {
            sender.sendMessage(Utils.color(ConfigValue.NO_PERMISSION
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{permission}", "playerprofiles.admin")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Utils.color("&cUsage: /toggleprofile <player>"));
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            if (plugin.getProfileManager().getOrCreate(targetPlayer).isLocked()) {
                plugin.getProfileManager().unlockProfile(targetPlayer);
                sender.sendMessage(Utils.color(ConfigValue.UNLOCK_PROFILE_OTHERS
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{player}", targetPlayer.getName())));
            } else {
                plugin.getProfileManager().lockProfile(targetPlayer);
                sender.sendMessage(Utils.color(ConfigValue.LOCK_PROFILE_OTHERS
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{player}", targetPlayer.getName())));
            }
            return true;
        }

        OfflinePlayer offlineTarget = PlayerDataLoader.resolveOfflinePlayer(targetName);
        if (offlineTarget != null) {
            Profile profile = plugin.getProfileManager().getOrCreate(offlineTarget.getUniqueId());
            boolean wasLocked = profile.isLocked();
            profile.setLocked(!wasLocked);
            String resolvedName = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;
            String statusNotice = wasLocked ? ConfigValue.UNLOCK_PROFILE_OTHERS : ConfigValue.LOCK_PROFILE_OTHERS;
            sender.sendMessage(Utils.color(statusNotice
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{player}", resolvedName)));
            return true;
        }

        sender.sendMessage(Utils.color(ConfigValue.INVALID_PLAYER
                .replace("{prefix}", ConfigValue.PREFIX)
                .replace("{player}", targetName)));
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && (sender.hasPermission("playerprofiles.admin") || sender.hasPermission("playerprofiles.toggleprofile.others"))) {
            return null;
        }
        return new ArrayList<>();
    }
}
