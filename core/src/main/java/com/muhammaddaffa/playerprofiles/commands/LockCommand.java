package com.muhammaddaffa.playerprofiles.commands;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
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

public class LockCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "lockprofile";
    private final PlayerProfiles plugin;

    public LockCommand(PlayerProfiles plugin) {
        this.plugin = plugin;
    }

    public void registerThisCommand() {
        plugin.getCommand(COMMAND_NAME).setExecutor(this);
        plugin.getCommand(COMMAND_NAME).setTabCompleter(this);

        FileConfiguration config = PlayerProfiles.CONFIG_DEFAULT.getConfig();
        List<String> aliases = config.getStringList("commandAliases.lockProfile");
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
            Logger.info("&cFailed to register /lockprofile command");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("playerprofiles.admin") && !sender.hasPermission("playerprofiles.lockprofile.others")) {
            sender.sendMessage(Utils.color(ConfigValue.NO_PERMISSION
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{permission}", "playerprofiles.admin")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Utils.color("&cUsage: /lockprofile <player>"));
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            plugin.getProfileManager().lockProfile(targetPlayer);
            sender.sendMessage(Utils.color(ConfigValue.LOCK_PROFILE_OTHERS
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{player}", targetPlayer.getName())));
            return true;
        }

        OfflinePlayer offlineTarget = PlayerDataLoader.resolveOfflinePlayer(targetName);
        if (offlineTarget != null) {
            plugin.getProfileManager().getOrCreate(offlineTarget.getUniqueId()).setLocked(true);
            String resolvedName = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;
            sender.sendMessage(Utils.color(ConfigValue.LOCK_PROFILE_OTHERS
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
        if (args.length == 1 && (sender.hasPermission("playerprofiles.admin") || sender.hasPermission("playerprofiles.lockprofile.others"))) {
            return null;
        }
        return new ArrayList<>();
    }
}
