package com.muhammaddaffa.playerprofiles.commands;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ToggleCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "toggleprofile";

    private final PlayerProfiles plugin;
    public ToggleCommand(PlayerProfiles plugin){
        this.plugin = plugin;
    }

    public void registerThisCommand(){
        plugin.getCommand(COMMAND_NAME).setExecutor(this);
        plugin.getCommand(COMMAND_NAME).setTabCompleter(this);
        // Get the file configuration of config.yml
        // Trying to register the aliases from the config
        try{
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            ((CommandMap) bukkitCommandMap.get(Bukkit.getServer())).register(COMMAND_NAME, plugin.getCommand(COMMAND_NAME));

            bukkitCommandMap.setAccessible(false);
        } catch (Exception ex){
            Logger.info("&cFailed to register /toggleprofile command");
            ex.printStackTrace();
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
        Player target = Bukkit.getPlayer(args[0]);
        if (target != null && target.isOnline()) {
            if (plugin.getProfileManager().getOrCreate(target).isLocked()) {
                plugin.getProfileManager().unlockProfile(target);
                sender.sendMessage(Utils.color(ConfigValue.UNLOCK_PROFILE_OTHERS
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{player}", target.getName())));
            } else {
                plugin.getProfileManager().lockProfile(target);
                sender.sendMessage(Utils.color(ConfigValue.LOCK_PROFILE_OTHERS
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{player}", target.getName())));
            }
            return true;
        }
        org.bukkit.OfflinePlayer offline = com.muhammaddaffa.playerprofiles.utils.PlayerDataLoader.resolveOfflinePlayer(args[0]);
        if (offline != null) {
            var profile = plugin.getProfileManager().getOrCreate(offline.getUniqueId());
            boolean wasLocked = profile.isLocked();
            profile.setLocked(!wasLocked);
            String message = wasLocked ? ConfigValue.UNLOCK_PROFILE_OTHERS : ConfigValue.LOCK_PROFILE_OTHERS;
            sender.sendMessage(Utils.color(message
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{player}", offline.getName() != null ? offline.getName() : args[0])));
            return true;
        }
        sender.sendMessage(Utils.color(ConfigValue.INVALID_PLAYER
                .replace("{prefix}", ConfigValue.PREFIX)
                .replace("{player}", args[0])));
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
