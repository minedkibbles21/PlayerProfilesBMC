package com.muhammaddaffa.playerprofiles.commands.subcommands;

import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.commands.abstraction.SubCommand;
import com.muhammaddaffa.playerprofiles.inventory.InventoryManager;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUI;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUIManager;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OpenGUICommand extends SubCommand {

    @Override
    public @Nullable String getPermission() {
        return "playerprofiles.admin";
    }

    @Override
    public List<String> parseTabCompletion(PlayerProfiles plugin, CommandSender sender, String[] args) {
        if(args.length == 2){
            return null;
        }
        if(args.length == 3){
            return null;
        }
        if(args.length == 4){
            return plugin.getCustomGUIManager().getListName();
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(PlayerProfiles plugin, CommandSender sender, String[] args) {
        // The full command is /bmcplayerprofile opengui (player) (target) (gui-name) - args length = 4
        // So we want to tell the command sender if the the args doesn't enough
        if(args.length < 4){
            sender.sendMessage(Utils.color("&cUsage: /bmcplayerprofile opengui (player) (target) (gui-name)"));
            return;
        }
        // First get the player object from args[1]
        Player player = Bukkit.getPlayer(args[1]);
        // Check if the player is not valid
        if(player == null){
            // If the player isn't valid, we want to tell the command sender
            sender.sendMessage(Utils.color(ConfigValue.INVALID_PLAYER
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{player}", args[1])));
            // Stop the code here
            return;
        }
        // After the check above, we already guaranteed the Player would not be null
        CustomGUIManager customGUIManager = plugin.getCustomGUIManager();
        CustomGUI customGUI = customGUIManager.getByName(args[3]);
        if(customGUI == null){
            sender.sendMessage(Utils.color(ConfigValue.INVALID_GUI_NAME
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{gui}", args[3])));
            return;
        }

        String targetQuery = args[2];
        Player onlineTarget = Bukkit.getPlayer(targetQuery);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            plugin.getInventoryManager().openInventory(customGUI, player, onlineTarget, true);
            return;
        }

        com.muhammaddaffa.playerprofiles.utils.PlayerDataLoader.loadTargetAsync(targetQuery, target -> {
            if (target == null) {
                sender.sendMessage(Utils.color(ConfigValue.INVALID_PLAYER
                        .replace("{prefix}", ConfigValue.PREFIX)
                        .replace("{player}", targetQuery)));
                return;
            }
            com.muhammaddaffa.mdlib.utils.Executor.entity(player, () ->
                    plugin.getInventoryManager().openInventory(customGUI, player, target, true));
        });
    }

}
