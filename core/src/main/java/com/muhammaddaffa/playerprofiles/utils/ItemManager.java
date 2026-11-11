package com.muhammaddaffa.playerprofiles.utils;

import com.muhammaddaffa.mdlib.fastinv.FastInv;
import com.muhammaddaffa.mdlib.utils.ItemBuilder;
import com.muhammaddaffa.mdlib.xseries.XMaterial;
import com.muhammaddaffa.playerprofiles.inventory.ProfileTarget;
import com.muhammaddaffa.playerprofiles.inventory.items.GUIItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

public class ItemManager {

    public static ItemStack createItem(GUIItem guiEntry, Player player, Player target) {
        return createItem(guiEntry, player, ProfileTarget.of(target));
    }

    public static ItemStack createItem(GUIItem guiEntry, Player player, ProfileTarget target) {
        if (guiEntry.material().isBlank()) return errorItem(guiEntry);
        ItemBuilder builder;
        if (guiEntry.material().contains(";")) {
            String[] parts = guiEntry.material().split(";", 2);
            if (parts[1].isBlank()) return errorItem(guiEntry);
            switch (parts[0].toUpperCase(java.util.Locale.ROOT)) {
                case "SLOT", "SLOTS" -> {
                    ItemStack stack = slotItem(guiEntry, target, parts[1]);
                    if (stack.getType() == Material.AIR) return stack;
                    builder = new ItemBuilder(stack);
                }
                case "HEAD", "HEADS" -> builder = headBuilder(guiEntry, player, target, parts[1]);
                default -> { return errorItem(guiEntry); }
            }
        } else {
            Optional<XMaterial> material = XMaterial.matchXMaterial(guiEntry.material());
            ItemStack stack = material.map(XMaterial::parseItem).orElse(null);
            if (stack == null) return errorItem(guiEntry);
            if (stack.getType().isAir()) return stack;
            if (stack.getItemMeta() == null) return errorItem(guiEntry);
            builder = configuredBuilder(guiEntry, player, target, stack);
        }
        if (guiEntry.hideAttributes()) builder.flags(ItemFlag.HIDE_ATTRIBUTES);
        if (guiEntry.glowing()) builder.enchant(Enchantment.UNBREAKING).flags(ItemFlag.HIDE_ENCHANTS);
        ItemStack stack = builder.build();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        String model = guiEntry.itemModel();
        if (isVersionAtLeast(1, 21, 2) && model != null && !model.isEmpty()) {
            NamespacedKey key = NamespacedKey.fromString(model);
            if (key == null) return errorItem(guiEntry);
            meta.setItemModel(key);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemBuilder configuredBuilder(GUIItem guiEntry, Player player, ProfileTarget target, ItemStack stack) {
        return new ItemBuilder(stack)
                .name(Utils.tryParsePAPI(guiEntry.name(), player, target))
                .lore(Utils.tryParsePAPI(guiEntry.lore(), player, target))
                .amount(Math.max(1, guiEntry.amount()))
                .customModelData(guiEntry.customModelData());
    }

    private static ItemBuilder headBuilder(GUIItem guiEntry, Player player, ProfileTarget target, String textValue) {
        ItemBuilder builder = configuredBuilder(guiEntry, player, target, new ItemStack(Material.PLAYER_HEAD));
        String identifier = textValue.replace("{player}", player.getName()).replace("{target}", target.getName());
        try {
            if (identifier.equals(player.getName())) {
                builder.skull(player);
            } else if (identifier.equals(target.getName())) {
                if (target.isOnline() && target.getPlayer() != null) {
                    builder.skull(target.getPlayer());
                } else {
                    builder.skull(target.getOfflinePlayer());
                }
            } else {
                builder.skull(identifier);
            }
        } catch (RuntimeException ex) {
            builder = configuredBuilder(guiEntry, player, target, new ItemStack(Material.PLAYER_HEAD));
        }
        return builder;
    }

    private static ItemStack slotItem(GUIItem guiEntry, ProfileTarget target, String slotIndex) {
        try {
            int slot = Integer.parseInt(slotIndex);
            return target.getItem(slot);
        } catch (NumberFormatException ex) {
            return errorItem(guiEntry);
        }
    }

    public static ItemStack createGUIItem(GUIItem guiEntry, Player player, Player target) {
        return createGUIItem(guiEntry, player, ProfileTarget.of(target));
    }

    public static ItemStack createGUIItem(GUIItem guiEntry, Player player, ProfileTarget target) {
        if (guiEntry.type().contains(";")) {
            String[] parts = guiEntry.type().split(";", 2);
            if (parts[1].isBlank()) return errorItem(guiEntry);
            return switch (parts[0].toUpperCase(java.util.Locale.ROOT)) {
                case "SLOT", "SLOTS" -> slotItem(guiEntry, target, parts[1]);
                case "HEAD", "HEADS" -> headBuilder(guiEntry, player, target, parts[1]).build();
                default -> errorItem(guiEntry);
            };
        }
        return switch (guiEntry.type()) {
            case "HELMET" -> createArmorItem(guiEntry, player, target, target.getHelmet());
            case "CHESTPLATE" -> createArmorItem(guiEntry, player, target, target.getChestplate());
            case "LEGGINGS" -> createArmorItem(guiEntry, player, target, target.getLeggings());
            case "BOOTS" -> createArmorItem(guiEntry, player, target, target.getBoots());
            case "MAIN_HAND" -> createArmorItem(guiEntry, player, target, target.getMainHand());
            case "OFF_HAND" -> createArmorItem(guiEntry, player, target, target.getOffHand());
            default -> createItem(guiEntry, player, target);
        };
    }

    public static void fillItem(FastInv inventory, FileConfiguration config) {
        if (!config.getBoolean("fillItems.enabled")) return;
        String materialName = config.getString("fillItems.material", "AIR");
        if (materialName.isBlank()) return;
        Optional<XMaterial> optionalMat = XMaterial.matchXMaterial(materialName);
        if (optionalMat.isEmpty()) return;
        ItemStack templateStack = optionalMat.get().parseItem();
        if (templateStack == null || templateStack.getItemMeta() == null) return;

        ItemBuilder builder = new ItemBuilder(templateStack)
                .name(Utils.color(config.getString("fillItems.name", "")))
                .lore(Utils.color(config.getStringList("fillItems.lore")))
                .customModelData(config.getInt("fillItems.customModelData"));

        ItemStack finalStack = builder.build();
        String fillModel = config.getString("fillItems.itemModel", config.getString("fillItems.item_model", ""));
        if (isVersionAtLeast(1, 21, 2) && fillModel != null && !fillModel.isBlank()) {
            NamespacedKey modelKey = NamespacedKey.fromString(fillModel);
            if (modelKey != null && finalStack.getItemMeta() != null) {
                ItemMeta fillMeta = finalStack.getItemMeta();
                fillMeta.setItemModel(modelKey);
                finalStack.setItemMeta(fillMeta);
            }
        }

        int inventorySize = inventory.getInventory().getSize();
        for (int i = 0; i < inventorySize; i++) {
            ItemStack slotStack = inventory.getInventory().getItem(i);
            if (slotStack != null) continue;
            inventory.setItem(i, finalStack);
        }
    }

    private static ItemStack createArmorItem(GUIItem guiEntry, Player player, ProfileTarget target, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return createItem(guiEntry, player, target);
        return stack.clone();
    }

    private static ItemStack errorItem(GUIItem guiEntry) {
        return new ItemBuilder(XMaterial.BARRIER.parseItem())
                .name("&cInvalid Material!")
                .lore("&7Please check your configuration for item '{item}'".replace("{item}", guiEntry.name()),
                        " ",
                        "&7Additional Information:",
                        "&7Material: {material}".replace("{material}", guiEntry.material()))
                .build();
    }

    public static boolean isVersionAtLeast(int major, int minor, int patch) {
        try {
            if (Bukkit.getServer() == null) return true;
            return isVersionAtLeast(Bukkit.getBukkitVersion(), major, minor, patch);
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean isVersionAtLeast(String bukkitVersion, int major, int minor, int patch) {
        String version = bukkitVersion.split("-")[0];
        String[] parts = version.split("\\.");

        if (parts.length < 2) return false;
        try {
            int maj = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
            int min = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
            int pat = 0;
            if (parts.length > 2) {
                String clean = parts[2].replaceAll("[^0-9]", "");
                if (!clean.isEmpty()) {
                    pat = Integer.parseInt(clean);
                }
            }

            if (maj != major) return maj > major;
            if (min != minor) return min > minor;
            return pat >= patch;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
