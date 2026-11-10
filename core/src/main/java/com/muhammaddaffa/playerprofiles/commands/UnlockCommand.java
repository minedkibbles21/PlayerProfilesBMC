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

public class UnlockCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "unlockprofile";
    private final PlayerProfiles plugin;

    public UnlockCommand(PlayerProfiles plugin) {
        this.plugin = plugin;
    }

    public void registerThisCommand() {
        plugin.getCommand(COMMAND_NAME).setExecutor(this);
        plugin.getCommand(COMMAND_NAME).setTabCompleter(this);

        FileConfiguration config = PlayerProfiles.CONFIG_DEFAULT.getConfig();
        List<String> aliases = config.getStringList("commandAliases.unlockProfile");
        plugin.getCommand(COMMAND_NAME).getAliases().addAll(aliases);

        try {
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            ((CommandMap) bukkitCommandMap.get(Bukkit.getServer())).register(COMMAND_NAME, plugin.getCommand(COMMAND_NAME));
            bukkitCommandMap.setAccessible(false);
        } catch (Exception ex) {
            Logger.info("&cFailed to register /unlockprofile command");
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("playerprofiles.admin") && !sender.hasPermission("playerprofiles.unlockprofile.others")) {
            sender.sendMessage(Utils.color(ConfigValue.NO_PERMISSION
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{permission}", "playerprofiles.admin")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Utils.color("&cUsage: /unlockprofile <player>"));
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            plugin.getProfileManager().unlockProfile(targetPlayer);
            sender.sendMessage(Utils.color(ConfigValue.UNLOCK_PROFILE_OTHERS
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{player}", targetPlayer.getName())));
            return true;
        }

        OfflinePlayer offlineTarget = PlayerDataLoader.resolveOfflinePlayer(targetName);
        if (offlineTarget != null) {
            plugin.getProfileManager().getOrCreate(offlineTarget.getUniqueId()).setLocked(false);
            String resolvedName = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;
            sender.sendMessage(Utils.color(ConfigValue.UNLOCK_PROFILE_OTHERS
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
        if (args.length == 1 && (sender.hasPermission("playerprofiles.admin") || sender.hasPermission("playerprofiles.unlockprofile.others"))) {
            return null;
        }
        return new ArrayList<>();
    }
}
