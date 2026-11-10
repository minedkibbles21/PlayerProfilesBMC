package com.muhammaddaffa.playerprofiles.utils;

import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.playerprofiles.hooks.floodgate.FloodgateHook;
import com.muhammaddaffa.playerprofiles.inventory.OfflineEquipment;
import com.muhammaddaffa.playerprofiles.inventory.ProfileTarget;
import com.muhammaddaffa.playerprofiles.utils.nbt.NBTCompound;
import com.muhammaddaffa.playerprofiles.utils.nbt.NBTReader;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class PlayerDataLoader {

    public static void loadTargetAsync(String name, Consumer<ProfileTarget> callback) {
        Executor.async(() -> {
            ProfileTarget target = loadTargetSync(name);
            callback.accept(target);
        });
    }

    public static ProfileTarget loadTargetSync(String name) {
        if (name == null || name.isBlank()) return null;

        // 1. Check online players first
        org.bukkit.entity.Player online = Bukkit.getPlayerExact(name);
        if (online != null && online.isOnline()) {
            return ProfileTarget.of(online);
        }

        // 2. Resolve offline / Bedrock player
        OfflinePlayer offlinePlayer = resolveOfflinePlayer(name);
        if (offlinePlayer == null) return null;

        UUID uuid = offlinePlayer.getUniqueId();
        boolean isBedrock = FloodgateHook.isBedrockPlayer(uuid);

        File dataFile = getPlayerDataFile(uuid);
        if (dataFile == null || !dataFile.exists()) {
            // If the player has never joined and no data file exists, return null
            if (!offlinePlayer.hasPlayedBefore() && offlinePlayer.getLastLogin() == 0) {
                return null;
            }
            // Return target with empty equipment if file is missing but player existed
            return ProfileTarget.of(offlinePlayer, new OfflineEquipment(), isBedrock);
        }

        try {
            NBTCompound root = NBTReader.readFile(dataFile);
            OfflineEquipment equipment = parseEquipment(root);
            return ProfileTarget.of(offlinePlayer, equipment, isBedrock);
        } catch (Exception ex) {
            return ProfileTarget.of(offlinePlayer, new OfflineEquipment(), isBedrock);
        }
    }

    public static OfflinePlayer resolveOfflinePlayer(String query) {
        if (query == null || query.isBlank()) return null;

        // Try direct exact match
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(query);
        if (offline != null && (offline.hasPlayedBefore() || getPlayerDataFile(offline.getUniqueId()) != null)) {
            return offline;
        }

        // Geyser / Floodgate Bedrock player checks
        String prefix = FloodgateHook.getPrefix();
        if (FloodgateHook.isEnabled()) {
            // Check with prefix if not already prefixed
            if (!query.startsWith(prefix)) {
                OfflinePlayer prefixed = Bukkit.getOfflinePlayerIfCached(prefix + query);
                if (prefixed != null && (prefixed.hasPlayedBefore() || getPlayerDataFile(prefixed.getUniqueId()) != null)) {
                    return prefixed;
                }
            } else {
                // If query starts with prefix, check stripped
                String stripped = query.substring(prefix.length());
                OfflinePlayer withoutPrefix = Bukkit.getOfflinePlayerIfCached(stripped);
                if (withoutPrefix != null && (withoutPrefix.hasPlayedBefore() || getPlayerDataFile(withoutPrefix.getUniqueId()) != null)) {
                    return withoutPrefix;
                }
            }

            // Query Floodgate UUID resolver
            UUID floodgateUuid = FloodgateHook.getUuidFor(query);
    if(floodgateUuid != null) {

                OfflinePlayer floodgatePlayer = Bukkit.getOfflinePlayer(floodgateUuid);
                if (floodgatePlayer != null) return floodgatePlayer;
            }
        }

        // Fallback: search all known offline players
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            String opName = op.getName();
            if (opName != null && opName.equalsIgnoreCase(query)) {
                return op;
            }
            if (opName != null && FloodgateHook.isEnabled() && !query.startsWith(prefix) && opName.equalsIgnoreCase(prefix + query)) {
                return op;
            }
        }

        // Try standard Bukkit lookup
        OfflinePlayer fallback = Bukkit.getOfflinePlayer(query);
        if (fallback != null && (fallback.hasPlayedBefore() || getPlayerDataFile(fallback.getUniqueId()) != null)) {
            return fallback;
        }
        return null;
    }

    public static File getPlayerDataFile(UUID uuid) {
        if (uuid == null) return null;
        String fileName = uuid + ".dat";

        // Check world containers
        for (World world : Bukkit.getWorlds()) {
            File pd = new File(world.getWorldFolder(), "playerdata");
            File file = new File(pd, fileName);
            if (file.exists()) return file;
        }

        File container = Bukkit.getWorldContainer();
        File defaultWorld = new File(new File(container, "world"), "playerdata");
        File file = new File(defaultWorld, fileName);
        if (file.exists()) return file;

        File directPd = new File(container, "playerdata");
        file = new File(directPd, fileName);
        if (file.exists()) return file;

        return null;
    }

    public static OfflineEquipment parseEquipment(NBTCompound root) {
        OfflineEquipment equipment = new OfflineEquipment();
        if (root == null) return equipment;

        int selectedSlot = root.getInt("SelectedItemSlot", 0);
        List<Object> inventory = root.getList("Inventory");
  for(Object elem : inventory) {

            if (!(elem instanceof NBTCompound itemTag)) continue;

            int slot = itemTag.getInt("Slot");
            String id = itemTag.getString("id");
            equipment.setRawItemId(slot, id);
            ItemStack stack = parseItemStack(itemTag);
            equipment.setItem(slot, stack);

            // Standard Minecraft equipment slots
    if(slot == 103) {

                equipment.setHelmet(stack);
            } else if (slot == 102) {
                equipment.setChestplate(stack);
            } else if (slot == 101) {
                equipment.setLeggings(stack);
            } else if (slot == 100) {
                equipment.setBoots(stack);
            } else if (slot == -106 || slot == 150) {
                equipment.setOffHand(stack);
            } else if (slot == selectedSlot) {
                equipment.setMainHand(stack);
            }
        }
        return equipment;
    }

    public static ItemStack parseItemStack(NBTCompound tag) {
        if (tag == null) return null;
        String id = tag.getString("id");
        if (id == null || id.isBlank()) return null;

        // Outside live server runtime (e.g. standalone test environment)
        if (Bukkit.getServer() == null) return null;

        Material material = Material.matchMaterial(id);
        if (material == null && id.startsWith("minecraft:")) {
            material = Material.matchMaterial(id.substring(10));
        }
        if (material == null || material.isAir()) return new ItemStack(Material.AIR);

        int count = tag.getInt("count", tag.getInt("Count", 1));
        ItemStack stack = new ItemStack(material, Math.max(1, count));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        // Modern 1.20.5+ / 26.3 components
        NBTCompound components = tag.getCompound("components");
  if(components != null) {

            if (components.has("minecraft:custom_name")) {
                meta.setDisplayName(Utils.color(cleanJsonText(components.getString("minecraft:custom_name"))));
            }
            if (components.has("minecraft:lore")) {
                List<Object> loreList = components.getList("minecraft:lore");
                List<String> lore = new ArrayList<>();
    for(Object line : loreList) {

                    if (line instanceof String s) lore.add(Utils.color(cleanJsonText(s)));
                }
                meta.setLore(lore);
            }
            if (components.has("minecraft:damage") && meta instanceof Damageable d) {
                d.setDamage(components.getInt("minecraft:damage"));
            }
            if (components.has("minecraft:custom_model_data")) {
                meta.setCustomModelData(components.getInt("minecraft:custom_model_data"));
            }
            NBTCompound enchants = components.getCompound("minecraft:enchantments");
    if(enchants != null) {

                NBTCompound levels = enchants.getCompound("levels");
                NBTCompound target = levels != null ? levels : enchants;
                for (String enchKey : target.keySet()) {
                    Enchantment ench = parseEnchantment(enchKey);
    if(ench != null) {

                        meta.addEnchant(ench, target.getInt(enchKey), true);
                    }
                }
            }
        }

        // Legacy 1.20.4 and older 'tag'
        NBTCompound legacyTag = tag.getCompound("tag");
  if(legacyTag != null) {

            NBTCompound display = legacyTag.getCompound("display");
    if(display != null) {

                if (display.has("Name")) {
                    meta.setDisplayName(Utils.color(cleanJsonText(display.getString("Name"))));
                }
                if (display.has("Lore")) {
                    List<Object> loreList = display.getList("Lore");
                    List<String> lore = new ArrayList<>();
  for(Object line : loreList) {

                        if (line instanceof String s) lore.add(Utils.color(cleanJsonText(s)));
                    }
                    meta.setLore(lore);
                }
            }
            if (legacyTag.has("Damage") && meta instanceof Damageable d) {
                d.setDamage(legacyTag.getInt("Damage"));
            }
            if (legacyTag.has("CustomModelData")) {
                meta.setCustomModelData(legacyTag.getInt("CustomModelData"));
            }
            List<Object> enchants = legacyTag.getList("Enchantments");
            if (enchants.isEmpty()) enchants = legacyTag.getList("ench");
      for(Object enchElem : enchants) {
      if(enchElem instanceof NBTCompound enchTag) {

                    String enchId = enchTag.getString("id");
                    int lvl = enchTag.getInt("lvl");
                    Enchantment ench = parseEnchantment(enchId);
  if(ench != null) {

                        meta.addEnchant(ench, lvl, true);
                    }
                }
            }
        }

        stack.setItemMeta(meta);
        return stack;
    }

    private static Enchantment parseEnchantment(String key) {
        if (key == null || key.isBlank()) return null;
        try {
            NamespacedKey nsKey = NamespacedKey.fromString(key.toLowerCase(Locale.ROOT));
      if(nsKey != null) {

                Enchantment ench = Enchantment.getByKey(nsKey);
                if (ench != null) return ench;
            }
        } catch (Exception ignored) {}
        String clean = key.replace("minecraft:", "").toUpperCase(Locale.ROOT);
        return Enchantment.getByName(clean);
    }

    private static String cleanJsonText(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.contains("\"text\":")) {
            int idx = trimmed.indexOf("\"text\":\"");
      if(idx != -1) {

                int end = trimmed.indexOf("\"", idx + 8);
    if(end != -1) {

                    return trimmed.substring(idx + 8, end);
                }
            }
        }
        return trimmed;
    }
}
