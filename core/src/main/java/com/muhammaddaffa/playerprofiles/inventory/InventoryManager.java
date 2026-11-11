package com.muhammaddaffa.playerprofiles.inventory;

import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.hooks.combatlogx.HCombatLogX;
import com.muhammaddaffa.playerprofiles.hooks.deluxecombat.HDeluxeCombat;
import com.muhammaddaffa.playerprofiles.inventory.items.ItemsLoader;
import com.muhammaddaffa.playerprofiles.manager.DependencyManager;
import com.muhammaddaffa.playerprofiles.manager.combat.CombatManager;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUI;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import com.muhammaddaffa.playerprofiles.worldguardwrapper.WorldGuardWrapper;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class InventoryManager {
    private final PlayerProfiles plugin;
    private final ItemsLoader itemsLoader;

    public InventoryManager(PlayerProfiles plugin) {
        this.plugin = plugin;
        itemsLoader = new ItemsLoader(plugin);
    }

    public void initialize() { itemsLoader.loadItems(); }
    public void reInitialize() { itemsLoader.reloadItems(); }

    public void closeInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof ProfileInventory) {
                player.closeInventory();
            }
        }
    }

    public void openInventory(@Nullable CustomGUI customGUI, Player player, Player target) {
        openInventory(customGUI, player, ProfileTarget.of(target), false);
    }

    public void openInventory(@Nullable CustomGUI customGUI, Player player, ProfileTarget target) {
        openInventory(customGUI, player, target, false);
    }

    public boolean openInventory(@Nullable CustomGUI customGUI, Player player, Player target, boolean forced) {
        return openInventory(customGUI, player, ProfileTarget.of(target), forced);
    }

    /** Admin forced-open commands can explicitly bypass the normal viewing policy. */
    public boolean openInventory(@Nullable CustomGUI customGUI, Player player, ProfileTarget target, boolean forced) {
        if (player == null || !player.isOnline()) return false;
        if (target == null) {
            tell(player, ConfigValue.INVALID_PLAYER.replace("{player}", "unknown"));
            return false;
        }
        if (!forced && !canView(player, target)) return false;
        if (customGUI == null && ConfigValue.ZMENU_ENABLED && ConfigValue.ZMENU_PROFILE_MENU != null && !ConfigValue.ZMENU_PROFILE_MENU.isEmpty()) {
            String menu = Utils.tryParsePAPI(ConfigValue.ZMENU_PROFILE_MENU, player, target).trim();
            String fullCommand = menu.contains(" ") ? "zmenu open " + menu : "zmenu open " + menu + " " + player.getName();
            Executor.sync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fullCommand));
            return true;
        }
        FileConfiguration config = customGUI == null ? PlayerProfiles.GUI_DEFAULT.getConfig() : customGUI.config();
        try {
            ItemsLoader.validateMenu(config);
        } catch (IllegalArgumentException ex) {
            Logger.severe("Cannot open a menu: " + ex.getMessage());
            tell(player, PlayerProfiles.CONFIG_DEFAULT.getConfig().getString("messages.menuUnavailable",
                    "{prefix} &cThis menu is unavailable. Please contact an administrator."));
            return false;
        }
        ProfileInventory inventory = new ProfileInventory(customGUI == null ? itemsLoader.getMainMenuItems() : customGUI.items(),
                player, target, config.getInt("size"), config.getString("title", "Inventory"), config, forced);
        inventory.open(player);
        return true;
    }

    public boolean canView(Player player, Player target) {
        return canView(player, ProfileTarget.of(target));
    }

    public boolean canView(Player player, ProfileTarget target) {
        if (player == null || target == null || !player.isOnline()) return false;
        if (target.isOnline() && target.getPlayer() != null) {
            if (!player.canSee(target.getPlayer()) || (ConfigValue.DISABLE_NPC_PROFILE && target.getPlayer().hasMetadata("NPC"))) return false;
        }
        boolean isOwner = player.getUniqueId().equals(target.getUniqueId());
        String permission = isOwner ? "playerprofiles.profile" : "playerprofiles.profile.others";
        if (!player.hasPermission(permission)) {
            tell(player, ConfigValue.NO_PERMISSION.replace("{permission}", permission));
            return false;
        }
        // Owners must still be able to open their own menu to unlock their profile.
        if (!isOwner && target.isProfileLocked()) {
            tell(player, ConfigValue.LOCKED_PROFILE);
            return false;
        }
        if (ConfigValue.DISABLED_WORLDS.contains(player.getWorld().getName())) {
            tell(player, ConfigValue.DISABLED_WORLD_MESSAGE);
            return false;
        }
        if (DependencyManager.WORLD_GUARD && Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            com.muhammaddaffa.api.IRegionFinder finder = WorldGuardWrapper.getInstance().getRegionFinder();
            if (finder.getRegions(player.getLocation()).stream().anyMatch(ConfigValue.DISABLED_REGIONS::contains)) {
                tell(player, ConfigValue.PLAYER_DISABLED_REGIONS);
                return false;
            }
            if (target.isOnline() && target.getPlayer() != null) {
                if (finder.getRegions(target.getPlayer().getLocation()).stream().anyMatch(ConfigValue.DISABLED_REGIONS::contains)) {
                    tell(player, ConfigValue.TARGET_DISABLED_REGIONS);
                    return false;
                }
            }
        }
        if (ConfigValue.DISABLE_IN_COMBAT_ENABLED) {
            if (CombatManager.isInCombat(player)) {
                long remaining = CombatManager.getRemainingSeconds(player);
                tell(player, ConfigValue.DISABLE_IN_COMBAT_MESSAGE
                        .replace("{time}", String.valueOf(remaining)));
                return false;
            }
            if (ConfigValue.DISABLE_IN_COMBAT_CHECK_TARGET && target.isOnline() && target.getPlayer() != null) {
                Player onlineTarget = target.getPlayer();
                if (CombatManager.isInCombat(onlineTarget)) {
                    long remaining = CombatManager.getRemainingSeconds(onlineTarget);
                    tell(player, ConfigValue.DISABLE_IN_COMBAT_TARGET_MESSAGE
                            .replace("{player}", onlineTarget.getName())
                            .replace("{time}", String.valueOf(remaining)));
                    return false;
                }
            }
        }
        return true;
    }

    private void tell(Player player, String message) {
        player.sendMessage(Utils.color(message.replace("{prefix}", ConfigValue.PREFIX)));
    }
}
