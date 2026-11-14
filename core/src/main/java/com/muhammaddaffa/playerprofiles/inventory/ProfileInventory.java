package com.muhammaddaffa.playerprofiles.inventory;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.playerprofiles.ConfigValue;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.inventory.items.GUIItem;
import com.muhammaddaffa.playerprofiles.utils.ClickManager;
import com.muhammaddaffa.playerprofiles.utils.ItemManager;
import com.muhammaddaffa.playerprofiles.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProfileInventory extends FastInv {
    private final FileConfiguration config;
    private final boolean forced;
    private final String targetName;
    private final List<HandleTask> tasks = new ArrayList<>();

    public ProfileInventory(List<GUIItem> items, Player player, Player target, int size, String title) {
        this(items, player, ProfileTarget.of(target), size, title, PlayerProfiles.GUI_DEFAULT.getConfig(), false);
    }

    public ProfileInventory(List<GUIItem> items, Player player, Player target, int size, String title,
                            FileConfiguration config, boolean forced) {
        this(items, player, ProfileTarget.of(target), size, title, config, forced);
    }

    public ProfileInventory(List<GUIItem> items, Player player, ProfileTarget target, int size, String title) {
        this(items, player, target, size, title, PlayerProfiles.GUI_DEFAULT.getConfig(), false);
    }

    public ProfileInventory(List<GUIItem> items, Player player, ProfileTarget target, int size, String title,
                            FileConfiguration config, boolean forced) {
        super(size, Utils.color(Utils.tryParsePAPI(title, player, target)));
        this.config = config;
        this.forced = forced;
        this.targetName = target.getName();
        setAllItems(items, player, target);
        if (target.isOnline() && ConfigValue.AUTO_REFRESH_ENABLED) {
            tasks.add(Executor.syncTimer(0L, Math.max(1, ConfigValue.AUTO_REFRESH_TICK), () -> {
                if (!player.isOnline() || !target.isOnline()) {
                    close(player);
                    return;
                }
                setAllItems(items, player, target);
            }));
        }
        if (target.isOnline() && ConfigValue.DISTANCE_CHECK_ENABLED) {
            tasks.add(Executor.syncTimer(0L, 20L, () -> checkDistance(player, target)));
        }
        addCloseHandler(event -> cancelTasks());
    }

    private void cancelTasks() {
        tasks.forEach(HandleTask::cancel);
        tasks.clear();
    }

    private void close(Player player) {
        cancelTasks();
        if (player != null && player.isOnline()) player.closeInventory();
    }

    private boolean visible(GUIItem guiEntry, Player player, ProfileTarget target) {
        boolean isOwner = player.getUniqueId().equals(target.getUniqueId());
        return (!guiEntry.usePermission() || player.hasPermission(guiEntry.permission())) &&
                (!guiEntry.onlyVisitor() || !isOwner) &&
                (!guiEntry.onlyOwner() || isOwner);
    }

    private void setAllItems(List<GUIItem> items, Player player, ProfileTarget target) {
        // Clear both contents and handlers so hidden items cannot keep old actions.
        clearItems();
        List<GUIItem> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparingInt(GUIItem::priority));
        for (GUIItem guiEntry : sorted) {
            if (!visible(guiEntry, player, target)) continue;
            ItemStack stack = ItemManager.createGUIItem(guiEntry, player, target);
            List<Integer> slots = new ArrayList<>();
            for (Integer slotIndex : guiEntry.slots()) {
                if (slotIndex != null && slotIndex >= 0 && slotIndex < getInventory().getSize()) {
                    slots.add(slotIndex);
                }
            }
            setItems(slots, stack, event -> {
                event.setCancelled(true);
                if (!player.isOnline() || (target.isOnline() && !target.getPlayer().isOnline()) ||
                        (!forced && !PlayerProfiles.getInstance().getInventoryManager().canView(player, target))) {
                    close(player);
                    return;
                }
                // Recheck immediately; permission revocation need not wait for refresh.
                if (!visible(guiEntry, player, target)) {
                    setAllItems(items, player, target);
                    return;
                }
                ClickManager.handleInventoryClick(guiEntry, player, target, event, forced);
            });
        }
        ItemManager.fillItem(this, config);
    }

    private void checkDistance(Player player, ProfileTarget target) {
        if (player == null || !player.isOnline()) {
            cancelTasks();
            return;
        }
        Player onlineTarget = target.getPlayer();
        if (onlineTarget == null || !onlineTarget.isOnline() || !player.getWorld().equals(onlineTarget.getWorld()) ||
                player.getLocation().distance(onlineTarget.getLocation()) > ConfigValue.MAXIMUM_DISTANCE) {
            close(player);
            player.sendMessage(Utils.color(ConfigValue.DISTANCE_TOO_FAR
                    .replace("{prefix}", ConfigValue.PREFIX).replace("{player}", targetName)));
        }
    }
}
