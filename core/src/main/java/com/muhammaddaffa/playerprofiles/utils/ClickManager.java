package com.muhammaddaffa.playerprofiles.utils;

import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.inventory.ProfileTarget;
import com.muhammaddaffa.playerprofiles.inventory.items.GUIItem;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUI;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUIManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ClickManager {

    private static final Pattern pattern = Pattern.compile("(?<=\\[CONSOLE\\] |\\[PLAYER\\] |\\[SOUND\\] |\\[MESSAGE\\] |\\[MESSAGEPLAYER\\] |\\[MESSAGETARGET\\] |\\[OPENGUIMENU\\] |\\[ZMENU\\] |\\[ZM\\] |\\[CLOSE\\])");

    public static void handleInventoryClick(GUIItem item, Player player, Player target, InventoryClickEvent event){
        handleInventoryClick(item, player, ProfileTarget.of(target), event, false);
    }

    public static void handleInventoryClick(GUIItem item, Player player, Player target, InventoryClickEvent event, boolean forced) {
        handleInventoryClick(item, player, ProfileTarget.of(target), event, forced);
    }

    public static void handleInventoryClick(GUIItem item, Player player, ProfileTarget target, InventoryClickEvent event, boolean forced) {
        // Cancel the click to keep displayed items in the menu.
        event.setCancelled(true);
        // If the click type is LEFT, handle the left click
        if(event.getClick() == ClickType.LEFT)
            handleInventoryLeftClick(item, player, target, forced);
        // If the click type is RIGHT, handle the right click
        if(event.getClick() == ClickType.RIGHT)
            handleInventoryRightClick(item, player, target, forced);
    }

    private static void handleInventoryLeftClick(GUIItem item, Player player, ProfileTarget target, boolean forced){
        // Loop through all left click commands and handle the task
        item.leftCommands().forEach(command -> handleTask(command, player, target, forced));
    }

    private static void handleInventoryRightClick(GUIItem item, Player player, ProfileTarget target, boolean forced){
        // Loop through all right click commands and handle the task
        item.rightCommands().forEach(command -> handleTask(command, player, target, forced));
    }

    private static void handleTask(String command, Player player, ProfileTarget target, boolean forced) {
        // Get the array from the splitted pattern
        String[] cmds = pattern.split(command);
        String tag = cmds[0];
        // Get the list of arguments
        List<String> taskList = new ArrayList<>(Arrays.asList(cmds).subList(1, cmds.length));
        String task = cmds.length > 1 ? String.join(" ", taskList) : "";
        // Get the final arguments with parsed placeholder
        String finalTask = Utils.tryParsePAPI(task, player, target)
                .replace("{player}", player.getName())
                .replace("{target}", target.getName());
        // Check if the tag is CONSOLE
        if(tag.equalsIgnoreCase("[CONSOLE] ")){
            // Remove the color so the command could work
            String cmd = ChatColor.stripColor(finalTask);
            // Execute a command in console via Executor.sync for Folia global thread safety
            Executor.sync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
            return;
        }
        // Check if the tag is PLAYER
        if(tag.equalsIgnoreCase("[PLAYER] ")){
            // Remove the color so the command could work
            finalTask = ChatColor.stripColor(finalTask);
            // Make the player perform a command
            player.performCommand(finalTask);
            return;
        }
        if (tag.equalsIgnoreCase("[MESSAGE] ")) {
            // Send the player a message
            player.sendMessage(Utils.color(finalTask));
            // Send the target a message if online
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(Utils.color(finalTask));
            }
            return;
        }
        // Check if the tag is MESSAGE
        if(tag.equalsIgnoreCase("[MESSAGEPLAYER] ")) {
            // Send the player a message
            player.sendMessage(Utils.color(finalTask));
            return;
        }
        if (tag.equalsIgnoreCase("[MESSAGETARGET] ")) {
            // Send the target a message if online
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(Utils.color(finalTask));
            }
            return;
        }
        // Check if the tag is OPENGUIMENU
        if(tag.equalsIgnoreCase("[OPENGUIMENU] ")){
            finalTask = ChatColor.stripColor(finalTask);
            // Get the custom gui manager
            CustomGUIManager customGUIManager = PlayerProfiles.getInstance().getCustomGUIManager();
            // Get the custom gui from the task
            CustomGUI customGUI = customGUIManager.getByName(finalTask);
            // Return if the custom gui is invalid
            if (customGUI == null) {
                Logger.info("&c" + player.getName() + " trying to open an invalid GUI! (" + finalTask + ")");
                return;
            }
            // If the custom gui is valid, open the inventory to the player
            PlayerProfiles.getInstance().getInventoryManager().openInventory(customGUI, player, target, forced);
            return;
        }
        if (tag.equalsIgnoreCase("[ZMENU] ") || tag.equalsIgnoreCase("[ZM] ")) {
            String cmd = ChatColor.stripColor(finalTask).trim();
            String fullCommand = cmd.contains(" ") ? "zmenu open " + cmd : "zmenu open " + cmd + " " + player.getName();
            Executor.sync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fullCommand));
            return;
        }
        // Check if the tag is CLOSE
        if(tag.equalsIgnoreCase("[CLOSE]")){
            // Close the inventory
            player.closeInventory();
        }
    }

}
