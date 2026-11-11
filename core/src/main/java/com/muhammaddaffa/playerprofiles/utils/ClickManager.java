package com.muhammaddaffa.playerprofiles.utils;

import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.inventory.ProfileTarget;
import com.muhammaddaffa.playerprofiles.inventory.items.GUIItem;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUI;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUIManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ClickManager {

    private static final Pattern TAG_SPLIT_PATTERN = Pattern.compile("(?<=\\[CONSOLE\\] |\\[PLAYER\\] |\\[SOUND\\] |\\[MESSAGE\\] |\\[MESSAGEPLAYER\\] |\\[MESSAGETARGET\\] |\\[OPENGUIMENU\\] |\\[ZMENU\\] |\\[ZM\\] |\\[CLOSE\\])");
    private static final Pattern COLOR_STRIP_PATTERN = Pattern.compile("(?i)&[0-9a-fk-or]|§[0-9a-fk-or]|<#[0-9a-fA-F]{6}>|\\{#[0-9a-fA-F]{6}\\}");

    public static void handleInventoryClick(GUIItem targetItem, Player player, Player target, InventoryClickEvent event) {
        handleInventoryClick(targetItem, player, ProfileTarget.of(target), event, false);
    }

    public static void handleInventoryClick(GUIItem targetItem, Player player, Player target, InventoryClickEvent event, boolean forced) {
        handleInventoryClick(targetItem, player, ProfileTarget.of(target), event, forced);
    }

    public static void handleInventoryClick(GUIItem targetItem, Player player, ProfileTarget target, InventoryClickEvent event, boolean forced) {
        event.setCancelled(true);
        if (event.getClick() == ClickType.LEFT) {
            handleInventoryLeftClick(targetItem, player, target, forced);
        } else if (event.getClick() == ClickType.RIGHT) {
            handleInventoryRightClick(targetItem, player, target, forced);
        }
    }

    private static void handleInventoryLeftClick(GUIItem targetItem, Player player, ProfileTarget target, boolean forced) {
        targetItem.leftCommands().forEach(command -> handleTask(command, player, target, forced));
    }

    private static void handleInventoryRightClick(GUIItem targetItem, Player player, ProfileTarget target, boolean forced) {
        targetItem.rightCommands().forEach(command -> handleTask(command, player, target, forced));
    }

    private static void handleTask(String command, Player player, ProfileTarget target, boolean forced) {
        String[] segments = TAG_SPLIT_PATTERN.split(command);
        String actionTag = segments[0];
        List<String> argumentTokens = new ArrayList<>(Arrays.asList(segments).subList(1, segments.length));
        String rawTask = segments.length > 1 ? String.join(" ", argumentTokens) : "";
        String parsedTask = Utils.tryParsePAPI(rawTask, player, target)
                .replace("{player}", player.getName())
                .replace("{target}", target.getName());

        if (actionTag.equalsIgnoreCase("[CONSOLE] ")) {
            String plainCommand = stripColors(parsedTask);
            Executor.sync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plainCommand));
            return;
        }

        if (actionTag.equalsIgnoreCase("[PLAYER] ")) {
            String plainCommand = stripColors(parsedTask);
            player.performCommand(plainCommand);
            return;
        }

        if (actionTag.equalsIgnoreCase("[MESSAGE] ")) {
            player.sendMessage(Utils.color(parsedTask));
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(Utils.color(parsedTask));
            }
            return;
        }

        if (actionTag.equalsIgnoreCase("[MESSAGEPLAYER] ")) {
            player.sendMessage(Utils.color(parsedTask));
            return;
        }

        if (actionTag.equalsIgnoreCase("[MESSAGETARGET] ")) {
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(Utils.color(parsedTask));
            }
            return;
        }

        if (actionTag.equalsIgnoreCase("[OPENGUIMENU] ")) {
            String menuKey = stripColors(parsedTask).trim();
            CustomGUIManager guiManager = PlayerProfiles.getInstance().getCustomGUIManager();
            CustomGUI targetGui = guiManager.getByName(menuKey);
            if (targetGui == null) {
                Logger.info("&c" + player.getName() + " attempted to open unknown GUI: " + menuKey);
                return;
            }
            PlayerProfiles.getInstance().getInventoryManager().openInventory(targetGui, player, target, forced);
            return;
        }

        if (actionTag.equalsIgnoreCase("[ZMENU] ") || actionTag.equalsIgnoreCase("[ZM] ")) {
            String zmenuName = stripColors(parsedTask).trim();
            String fullCommand = zmenuName.contains(" ") ? "zmenu open " + zmenuName : "zmenu open " + zmenuName + " " + player.getName();
            Executor.sync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fullCommand));
            return;
        }

        if (actionTag.equalsIgnoreCase("[CLOSE]")) {
            player.closeInventory();
        }
    }

    private static String stripColors(String input) {
        if (input == null || input.isEmpty()) return "";
        return COLOR_STRIP_PATTERN.matcher(input).replaceAll("");
    }
}
