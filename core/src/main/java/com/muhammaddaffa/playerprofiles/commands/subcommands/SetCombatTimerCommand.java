package com.muhammaddaffa.playerprofiles.commands.subcommands;

import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.commands.abstraction.SubCommand;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetCombatTimerCommand extends SubCommand {

    @Override
    public @Nullable String getPermission() {
        return "playerprofiles.admin";
    }

    @Override
    public @NotNull List<String> parseTabCompletion(PlayerProfiles plugin, CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("10", "15", "20", "30", "45", "60");
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(PlayerProfiles plugin, CommandSender sender, String[] args) {
        if (args.length < 2) {
            int currentDuration = ConfigValue.DISABLE_IN_COMBAT_DURATION;
            sender.sendMessage(Utils.color("&6[BMCPlayerProfiles] &eCurrent combat timer: &6" + currentDuration + " &eseconds."));
            sender.sendMessage(Utils.color("&7Usage: /playerprofiles setcombattimer <seconds>"));
            return;
        }

        String secondsToken = args[1];
        int parsedSeconds;
        try {
            parsedSeconds = Integer.parseInt(secondsToken);
            if (parsedSeconds < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            sender.sendMessage(Utils.color("&cPlease specify a valid non-negative number of seconds."));
            return;
        }

        PlayerProfiles.CONFIG_DEFAULT.getConfig().set("options.disableInCombat.duration", parsedSeconds);
        PlayerProfiles.CONFIG_DEFAULT.saveConfig();
        ConfigValue.DISABLE_IN_COMBAT_DURATION = parsedSeconds;

        sender.sendMessage(Utils.color("&6[BMCPlayerProfiles] &aCombat timer set to &e" + parsedSeconds + " &aseconds and saved to config.yml!"));
    }
}
