package com.muhammaddaffa.playerprofiles.commands;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.commands.abstraction.SubCommand;
import com.muhammaddaffa.playerprofiles.commands.subcommands.ListGUICommand;
import com.muhammaddaffa.playerprofiles.commands.subcommands.OpenGUICommand;
import com.muhammaddaffa.playerprofiles.commands.subcommands.OpenProfileCommand;
import com.muhammaddaffa.playerprofiles.commands.subcommands.ReloadCommand;
import com.muhammaddaffa.playerprofiles.commands.subcommands.SetCombatTimerCommand;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "playerprofiles";

    private final Map<String, SubCommand> subCommandMap = new HashMap<>();

    private final PlayerProfiles plugin;
    public MainCommand(PlayerProfiles plugin){
        this.plugin = plugin;

        this.subCommandMap.put("reload", new ReloadCommand());
        this.subCommandMap.put("opengui", new OpenGUICommand());
        this.subCommandMap.put("listgui", new ListGUICommand());
        this.subCommandMap.put("openprofile", new OpenProfileCommand());
        SetCombatTimerCommand combatTimerCmd = new SetCombatTimerCommand();
        this.subCommandMap.put("setcombattimer", combatTimerCmd);
        this.subCommandMap.put("combattimer", combatTimerCmd);
    }

    public void registerThisCommand(){
        PluginCommand command = plugin.getCommand(COMMAND_NAME);
        if (command == null) return;
        command.setExecutor(this);
        command.setTabCompleter(this);

        FileConfiguration config = PlayerProfiles.CONFIG_DEFAULT.getConfig();
        List<String> aliases = config.getStringList("commandAliases.playerProfiles");
        command.getAliases().addAll(aliases);

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
                commandMap.register(COMMAND_NAME, command);
            }
        } catch (Exception ex){
            Logger.info("&cFailed to register /playerprofiles command");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {

        // Return if the args length is 0 and send help messages
        if(args.length == 0){
            this.sendHelpMessages(sender);
            return true;
        }

        // Trying to get subcommand from 'args[0]'
        SubCommand subCommand = this.subCommandMap.get(args[0].toLowerCase());

        // Return if there is no subcommand with 'args[0]' and send help messages
        if(subCommand == null) {
            this.sendHelpMessages(sender);
            return true;
        }

        // Check if sub command has permission
        if(subCommand.getPermission() != null){
            // Check if sender/player doesn't have permission for the subcommand
            if(!(sender.hasPermission(subCommand.getPermission()))){
                // Return and send messages
                sender.sendMessage(Utils.color(ConfigValue.NO_PERMISSION
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{permission}", subCommand.getPermission())));
                return true;
            }
        }

        // Execute the sub command
        subCommand.execute(plugin, sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {

        if(args.length == 1){
            return new ArrayList<>(this.subCommandMap.keySet());
        }

        if(args.length >= 2){
            SubCommand subCommand = this.subCommandMap.get(args[0].toLowerCase());
            if(subCommand == null) return null;

            if(subCommand.getPermission() == null){
                return subCommand.parseTabCompletion(plugin, sender, args);
            }
            if(sender.hasPermission(subCommand.getPermission())){
                return subCommand.parseTabCompletion(plugin, sender, args);
            }
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    private void sendHelpMessages(CommandSender sender){
        ConfigValue.HELP_MESSAGES.forEach(message ->
                sender.sendMessage(Utils.color(message)));
    }

}
