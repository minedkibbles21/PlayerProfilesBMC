package com.muhammaddaffa.playerprofiles.inventory;

import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import com.muhammaddaffa.playerprofiles.hooks.floodgate.FloodgateHook;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public interface ProfileTarget {

    String getName();

    String getDisplayName();

    UUID getUniqueId();

    boolean isOnline();

    Player getPlayer();

    OfflinePlayer getOfflinePlayer();

    ItemStack getHelmet();

    ItemStack getChestplate();

    ItemStack getLeggings();

    ItemStack getBoots();

    ItemStack getMainHand();

    ItemStack getOffHand();

    ItemStack getItem(int slot);

    boolean isProfileLocked();

    boolean isBedrock();

    static ProfileTarget of(Player player) {
        return new OnlineProfileTarget(player);
    }

    static ProfileTarget of(OfflinePlayer offlinePlayer) {
        if (offlinePlayer instanceof Player onlinePlayer) {
            return new OnlineProfileTarget(onlinePlayer);
        }
        if (offlinePlayer != null && offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
            return new OnlineProfileTarget(offlinePlayer.getPlayer());
        }
        boolean isBedrock = offlinePlayer != null && com.muhammaddaffa.playerprofiles.hooks.floodgate.FloodgateHook.isBedrockPlayer(offlinePlayer.getUniqueId());
        return new OfflineProfileTarget(offlinePlayer, null, isBedrock);
    }

    static ProfileTarget of(OfflinePlayer offlinePlayer, OfflineEquipment equipment, boolean isBedrock) {
        return new OfflineProfileTarget(offlinePlayer, equipment, isBedrock);
    }

    class OnlineProfileTarget implements ProfileTarget {
        private final Player player;

        public OnlineProfileTarget(Player player) {
            this.player = player;
        }

        @Override
        public String getName() {
            return player.getName();
        }

        @Override
        public String getDisplayName() {
            return player.getDisplayName();
        }

        @Override
        public UUID getUniqueId() {
            return player.getUniqueId();
        }

        @Override
        public boolean isOnline() {
            return player.isOnline();
        }

        @Override
        public Player getPlayer() {
            return player;
        }

        @Override
        public OfflinePlayer getOfflinePlayer() {
            return player;
        }

        @Override
        public ItemStack getHelmet() {
            ItemStack item = player.getInventory().getHelmet();
            return item != null ? item.clone() : new ItemStack(Material.AIR);
        }

        @Override
        public ItemStack getChestplate() {
            ItemStack item = player.getInventory().getChestplate();
            return item != null ? item.clone() : new ItemStack(Material.AIR);
        }

        @Override
        public ItemStack getLeggings() {
            ItemStack item = player.getInventory().getLeggings();
            return item != null ? item.clone() : new ItemStack(Material.AIR);
        }

        @Override
        public ItemStack getBoots() {
            ItemStack item = player.getInventory().getBoots();
            return item != null ? item.clone() : new ItemStack(Material.AIR);
        }

        @Override
        public ItemStack getMainHand() {
            ItemStack item = player.getInventory().getItemInMainHand();
            return item != null ? item.clone() : new ItemStack(Material.AIR);
        }

        @Override
        public ItemStack getOffHand() {
            ItemStack item = player.getInventory().getItemInOffHand();
            return item != null ? item.clone() : new ItemStack(Material.AIR);
        }

        @Override
        public ItemStack getItem(int slot) {
            if (slot < 0 || slot >= player.getInventory().getSize()) return new ItemStack(Material.AIR);
            ItemStack item = player.getInventory().getItem(slot);
            return item != null ? item.clone() : new ItemStack(Material.AIR);
        }

        @Override
        public boolean isProfileLocked() {
            return PlayerProfiles.getInstance().getProfileManager().isProfileLocked(player);
        }

        @Override
        public boolean isBedrock() {
            return FloodgateHook.isBedrockPlayer(player.getUniqueId());
        }
    }

    class OfflineProfileTarget implements ProfileTarget {
        private final OfflinePlayer offlinePlayer;
        private final OfflineEquipment equipment;
        private final boolean bedrock;

        public OfflineProfileTarget(OfflinePlayer offlinePlayer, OfflineEquipment equipment, boolean bedrock) {
            this.offlinePlayer = offlinePlayer;
            this.equipment = equipment != null ? equipment : new OfflineEquipment();
            this.bedrock = bedrock;
        }

        @Override
        public String getName() {
            String name = offlinePlayer.getName();
            return name != null ? name : "Unknown";
        }

        @Override
        public String getDisplayName() {
            return getName();
        }

        @Override
        public UUID getUniqueId() {
            return offlinePlayer.getUniqueId();
        }

        @Override
        public boolean isOnline() {
            return false;
        }

        @Override
        public Player getPlayer() {
            return null;
        }

        @Override
        public OfflinePlayer getOfflinePlayer() {
            return offlinePlayer;
        }

        @Override
        public ItemStack getHelmet() {
            return equipment.getHelmet();
        }

        @Override
        public ItemStack getChestplate() {
            return equipment.getChestplate();
        }

        @Override
        public ItemStack getLeggings() {
            return equipment.getLeggings();
        }

        @Override
        public ItemStack getBoots() {
            return equipment.getBoots();
        }

        @Override
        public ItemStack getMainHand() {
            return equipment.getMainHand();
        }

        @Override
        public ItemStack getOffHand() {
            return equipment.getOffHand();
        }

        @Override
        public ItemStack getItem(int slot) {
            return equipment.getItem(slot);
        }

        @Override
        public boolean isProfileLocked() {
            return PlayerProfiles.getInstance().getProfileManager().isProfileLocked(offlinePlayer.getUniqueId());
        }

        @Override
        public boolean isBedrock() {
            return bedrock;
        }
    }
}
