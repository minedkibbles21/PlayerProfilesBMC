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
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public class BMCPlayerProfileReloadCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "bmcplayerprofilereload";
    private final PlayerProfiles plugin;

    public BMCPlayerProfileReloadCommand(PlayerProfiles plugin) {
        this.plugin = plugin;
    }

    public void registerThisCommand() {
        PluginCommand command = plugin.getCommand(COMMAND_NAME);
        if (command == null) return;
        command.setExecutor(this);
        command.setTabCompleter(this);

        FileConfiguration configuration = PlayerProfiles.CONFIG_DEFAULT.getConfig();
        List<String> registeredAliases = configuration.getStringList("commandAliases.bmcplayerprofilereload");
        for (String aliasEntry : registeredAliases) {
            if (!command.getAliases().contains(aliasEntry)) {
                command.getAliases().add(aliasEntry);
            }
        }

        try {
            CommandMap serverCommandMap = null;
            try {
                serverCommandMap = Bukkit.getCommandMap();
            } catch (Throwable ignored) {}
            if (serverCommandMap == null) {
                Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                serverCommandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
            }
            if (serverCommandMap != null) {
                serverCommandMap.register(COMMAND_NAME, command);
            }
        } catch (Exception ex) {
            Logger.info("&cFailed to register /bmcplayerprofilereload command");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("playerprofiles.admin")) {
            sender.sendMessage(Utils.color(ConfigValue.NO_PERMISSION
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{permission}", "playerprofiles.admin")));
            return true;
        }

        plugin.reloadAllThing();
        sender.sendMessage(Utils.color(ConfigValue.RELOAD
                .replace("{prefix}", ConfigValue.PREFIX)));
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
