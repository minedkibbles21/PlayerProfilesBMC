package com.muhammaddaffa.playerprofiles.commands;

import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.inventory.InventoryManager;
import com.muhammaddaffa.playerprofiles.utils.PlayerDataLoader;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import org.bukkit.Bukkit;
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

public class ProfileCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "vprofile";
    private final PlayerProfiles plugin;

    public ProfileCommand(PlayerProfiles plugin) {
        this.plugin = plugin;
    }

    public void registerThisCommand() {
        org.bukkit.command.PluginCommand command = plugin.getCommand(COMMAND_NAME);
        if (command == null) return;
        command.setExecutor(this);
        command.setTabCompleter(this);

        FileConfiguration config = PlayerProfiles.CONFIG_DEFAULT.getConfig();
        List<String> aliases = config.getStringList("commandAliases.vprofile");
        if (aliases.isEmpty()) {
            aliases = config.getStringList("commandAliases.profile");
        }
        command.getAliases().addAll(aliases);

        try {
            CommandMap commandMap = null;
            try {
                commandMap = Bukkit.getCommandMap();
            } catch (Throwable ignored) {}
            if (commandMap == null) {
                Field mapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                mapField.setAccessible(true);
                commandMap = (CommandMap) mapField.get(Bukkit.getServer());
            }
            if (commandMap != null) {
                commandMap.register(COMMAND_NAME, command);
            }
        } catch (Exception ex) {
            Logger.info("&cFailed to register aliases for /vprofile");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Logger.info("&cOnly players can execute /vprofile command");
            return true;
        }

        InventoryManager inventoryManager = plugin.getInventoryManager();
        if (args.length == 0) {
            if (!player.hasPermission("playerprofiles.vprofile") && !player.hasPermission("playerprofiles.profile")) {
                player.sendMessage(Utils.color(ConfigValue.NO_PERMISSION
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{permission}", "playerprofiles.vprofile")));
                return true;
            }
            inventoryManager.openInventory(null, player, player);
            return true;
        }

        if (!player.hasPermission("playerprofiles.vprofile.others") && !player.hasPermission("playerprofiles.profile.others")) {
            player.sendMessage(Utils.color(ConfigValue.NO_PERMISSION
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{permission}", "playerprofiles.vprofile.others")));
            return true;
        }

        String targetQuery = args[0];
        Player onlineTarget = Bukkit.getPlayer(targetQuery);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            inventoryManager.openInventory(null, player, onlineTarget);
            return true;
        }

        PlayerDataLoader.loadTargetAsync(targetQuery, target -> {
            if (target == null) {
                sender.sendMessage(Utils.color(ConfigValue.INVALID_PLAYER
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{player}", targetQuery)));
                return;
            }
            Executor.entity(player, () -> inventoryManager.openInventory(null, player, target));
        });
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && (sender.hasPermission("playerprofiles.vprofile.others") || sender.hasPermission("playerprofiles.profile.others"))) {
            return null;
        }
        return new ArrayList<>();
    }
}
