package com.muhammaddaffa.playerprofiles.commands;

import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.inventory.InventoryManager;
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
    public ProfileCommand(PlayerProfiles plugin){
        this.plugin = plugin;
    }

    public void registerThisCommand(){
        plugin.getCommand(COMMAND_NAME).setExecutor(this);
        plugin.getCommand(COMMAND_NAME).setTabCompleter(this);
        // Get the file configuration of config.yml
        FileConfiguration config = PlayerProfiles.CONFIG_DEFAULT.getConfig();
        // Get the aliases from the config
        List<String> aliases = config.getStringList("commandAliases.vprofile");
        if (aliases.isEmpty()) {
            aliases = config.getStringList("commandAliases.profile");
        }
        // Add all aliases to the command
        plugin.getCommand(COMMAND_NAME).getAliases().addAll(aliases);
        // Trying to register the aliases from the config
        try{
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            ((CommandMap) bukkitCommandMap.get(Bukkit.getServer())).register(COMMAND_NAME, plugin.getCommand(COMMAND_NAME));

            bukkitCommandMap.setAccessible(false);
        } catch (Exception ex){
            Logger.info("&cFailed to register /vprofile command");
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        // If the command sender is not player, return the code
        if(!(sender instanceof Player)){
            Logger.info("&cOnly players can execute /vprofile command");
            return true;
        }
        // Get the player object from the sender
        Player player = (Player) sender;
        // Get the InventoryManager
        InventoryManager inventoryManager = plugin.getInventoryManager();
        // Check if args length is 0 (/vprofile)
  if(args.length == 0) {

            // Permission: playerprofiles.vprofile (or legacy playerprofiles.profile)
            if(!player.hasPermission("playerprofiles.vprofile") && !player.hasPermission("playerprofiles.profile")){
                player.sendMessage(Utils.color(ConfigValue.NO_PERMISSION
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{permission}", "playerprofiles.vprofile")));
                return true;
            }
            // If the player has permission, open the profile
            inventoryManager.openInventory(null, player, player);
            // Stop the code here
            return true;
        }
        // Check if args length is 1 (/vprofile (player))
  if(args.length == 1) {

            // Permission: playerprofiles.vprofile.others (or legacy playerprofiles.profile.others)
            if(!player.hasPermission("playerprofiles.vprofile.others") && !player.hasPermission("playerprofiles.profile.others")){
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
            // If the target is offline or a Bedrock player via Floodgate, load asynchronously
            com.muhammaddaffa.playerprofiles.utils.PlayerDataLoader.loadTargetAsync(targetQuery, target -> {
      if(target == null) {

                    sender.sendMessage(Utils.color(ConfigValue.INVALID_PLAYER
                            .replace("{prefix}", ConfigValue.PREFIX)
                            .replace("{player}", targetQuery)));
                    return;
                }
                Executor.entity(player, () -> inventoryManager.openInventory(null, player, target));
            });
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if(args.length == 1 && (sender.hasPermission("playerprofiles.vprofile.others") || sender.hasPermission("playerprofiles.profile.others"))){
            return null;
        }
        return new ArrayList<>();
    }
}
