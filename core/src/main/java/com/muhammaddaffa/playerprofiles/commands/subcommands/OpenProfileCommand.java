package com.muhammaddaffa.playerprofiles.commands.subcommands;

import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.commands.abstraction.SubCommand;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenProfileCommand extends SubCommand {

    @Override
    public @Nullable String getPermission() {
        return "playerprofiles.admin";
    }

    @Override
    public @NotNull List<String> parseTabCompletion(PlayerProfiles plugin, CommandSender sender, String[] args) {
        if(args.length == 2){
            return Collections.singletonList("(target-player)");
        }
        if(args.length == 3){
            return Collections.singletonList("(open-for)");
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(PlayerProfiles plugin, CommandSender sender, String[] args) {
        // The full command is /playerprofiles openprofile (target) (for-player) - args length = 3
        if(args.length < 3){
            sender.sendMessage(Utils.color("&cUsage: /playerprofiles openprofile (target) (for-player)"));
            return;
        }
        // Get the viewer player object
        Player player = Bukkit.getPlayer(args[2]);
        if(player == null || !player.isOnline()){
            sender.sendMessage(Utils.color(ConfigValue.INVALID_PLAYER
                    .replace("{prefix}", ConfigValue.PREFIX)
                    .replace("{player}", args[2])));
            return;
        }

        String targetQuery = args[1];
        Player onlineTarget = Bukkit.getPlayer(targetQuery);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            plugin.getInventoryManager().openInventory(null, player, onlineTarget, true);
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
                    plugin.getInventoryManager().openInventory(null, player, target, true));
        });
    }

}
