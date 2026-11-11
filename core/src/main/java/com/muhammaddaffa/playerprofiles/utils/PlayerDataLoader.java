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
            if (floodgateUuid != null) {
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

        String[] subPaths = new String[]{
            "players/data",
            "playerdata",
            "players",
            "data"
        };

        // 1. Check all loaded worlds
        if (Bukkit.getServer() != null) {
            for (World world : Bukkit.getWorlds()) {
                File worldFolder = world.getWorldFolder();
                for (String sub : subPaths) {
                    File candidate = new File(new File(worldFolder, sub), fileName);
                    if (candidate.exists() && candidate.isFile()) {
                        return candidate;
                    }
                }
            }
        }

        // 2. Check world container
        File container = Bukkit.getServer() != null ? Bukkit.getWorldContainer() : new File(".");
        if (container != null && container.exists()) {
            File defaultWorld = new File(container, "world");
            if (defaultWorld.exists() && defaultWorld.isDirectory()) {
                for (String sub : subPaths) {
                    File candidate = new File(new File(defaultWorld, sub), fileName);
                    if (candidate.exists() && candidate.isFile()) {
                        return candidate;
                    }
                }
            }

            for (String sub : subPaths) {
                File candidate = new File(new File(container, sub), fileName);
                if (candidate.exists() && candidate.isFile()) {
                    return candidate;
                }
            }

            File[] folders = container.listFiles(File::isDirectory);
            if (folders != null) {
                for (File folder : folders) {
                    for (String sub : subPaths) {
                        File candidate = new File(new File(folder, sub), fileName);
                        if (candidate.exists() && candidate.isFile()) {
                            return candidate;
                        }
                    }
                }
            }
        }

        // 3. Fallback: check working directory and local "world"
        File currentDir = new File(".");
        File localWorld = new File(currentDir, "world");
        if (localWorld.exists() && localWorld.isDirectory()) {
            for (String sub : subPaths) {
                File candidate = new File(new File(localWorld, sub), fileName);
                if (candidate.exists() && candidate.isFile()) {
                    return candidate;
                }
            }
        }
        for (String sub : subPaths) {
            File candidate = new File(new File(currentDir, sub), fileName);
            if (candidate.exists() && candidate.isFile()) {
                return candidate;
            }
        }

        return null;
    }

    public static OfflineEquipment parseEquipment(NBTCompound root) {
        OfflineEquipment equipment = new OfflineEquipment();
        if (root == null) return equipment;

        NBTCompound playerCompound = root;
        if (!playerCompound.has("Inventory") && !playerCompound.has("equipment") && !playerCompound.has("Equipment")) {
            if (playerCompound.has("data") && playerCompound.getCompound("data") != null) {
                playerCompound = playerCompound.getCompound("data");
            } else if (playerCompound.has("Player") && playerCompound.getCompound("Player") != null) {
                playerCompound = playerCompound.getCompound("Player");
            }
        }

        int selectedSlot = playerCompound.getInt("SelectedItemSlot", 0);

        // 1. Parse standard Inventory list (for inventory slots, hotbar, and legacy armor)
        List<Object> inventory = playerCompound.getList("Inventory");
        for (Object elem : inventory) {
            if (!(elem instanceof NBTCompound itemTag)) continue;

            int slot = itemTag.getInt("Slot");
            String id = itemTag.getString("id");
            if (id.isEmpty()) id = itemTag.getString("Id");
            equipment.setRawItemId(slot, id);
            ItemStack stack = parseItemStack(itemTag);
            equipment.setItem(slot, stack);

            // Legacy Minecraft armor & hand slots in Inventory
            if (slot == 103) {
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

        // 2. Modern 26.x / 1.21.5+ "equipment" compound tag
        NBTCompound equipCompound = playerCompound.getCompound("equipment");
        if (equipCompound == null) {
            equipCompound = playerCompound.getCompound("Equipment");
        }
        if (equipCompound != null) {
            parseEquipmentCompound(equipCompound, equipment, selectedSlot);
        }

        // 3. Modern "equipment" list fallback if saved as an entry list
        List<Object> equipList = playerCompound.getList("equipment");
        if (equipList.isEmpty()) {
            equipList = playerCompound.getList("Equipment");
        }
        if (!equipList.isEmpty()) {
            for (Object equipElement : equipList) {
                if (equipElement instanceof NBTCompound comp) {
                    parseEquipmentEntry(comp, equipment, selectedSlot);
                }
            }
        }

        // 4. LivingEntity ArmorItems / HandItems lists fallback
        List<Object> armorItems = playerCompound.getList("ArmorItems");
        if (armorItems != null && !armorItems.isEmpty()) {
            if (armorItems.size() > 0 && armorItems.get(0) instanceof NBTCompound feetTag && !isEmptyItem(feetTag)) {
                ItemStack stack = parseItemStack(feetTag);
                equipment.setBoots(stack);
                equipment.setItem(100, stack);
                equipment.setRawItemId(100, feetTag.getString("id"));
            }
            if (armorItems.size() > 1 && armorItems.get(1) instanceof NBTCompound legsTag && !isEmptyItem(legsTag)) {
                ItemStack stack = parseItemStack(legsTag);
                equipment.setLeggings(stack);
                equipment.setItem(101, stack);
                equipment.setRawItemId(101, legsTag.getString("id"));
            }
            if (armorItems.size() > 2 && armorItems.get(2) instanceof NBTCompound chestTag && !isEmptyItem(chestTag)) {
                ItemStack stack = parseItemStack(chestTag);
                equipment.setChestplate(stack);
                equipment.setItem(102, stack);
                equipment.setRawItemId(102, chestTag.getString("id"));
            }
            if (armorItems.size() > 3 && armorItems.get(3) instanceof NBTCompound headTag && !isEmptyItem(headTag)) {
                ItemStack stack = parseItemStack(headTag);
                equipment.setHelmet(stack);
                equipment.setItem(103, stack);
                equipment.setRawItemId(103, headTag.getString("id"));
            }
        }

        List<Object> handItems = playerCompound.getList("HandItems");
        if (handItems != null && !handItems.isEmpty()) {
            if (handItems.size() > 0 && handItems.get(0) instanceof NBTCompound mainTag && !isEmptyItem(mainTag)) {
                ItemStack stack = parseItemStack(mainTag);
                equipment.setMainHand(stack);
                equipment.setItem(selectedSlot, stack);
                equipment.setRawItemId(selectedSlot, mainTag.getString("id"));
            }
            if (handItems.size() > 1 && handItems.get(1) instanceof NBTCompound offTag && !isEmptyItem(offTag)) {
                ItemStack stack = parseItemStack(offTag);
                equipment.setOffHand(stack);
                equipment.setItem(-106, stack);
                equipment.setItem(150, stack);
                equipment.setRawItemId(-106, offTag.getString("id"));
            }
        }

        return equipment;
    }

    private static void parseEquipmentCompound(NBTCompound equipCompound, OfflineEquipment equipment, int selectedSlot) {
        NBTCompound headTag = getChildCompound(equipCompound, "head", "Head", "helmet", "Helmet");
        if (headTag != null && !isEmptyItem(headTag)) {
            ItemStack stack = parseItemStack(headTag);
            equipment.setHelmet(stack);
            equipment.setItem(103, stack);
            equipment.setRawItemId(103, headTag.getString("id"));
        }

        NBTCompound chestTag = getChildCompound(equipCompound, "chest", "Chest", "chestplate", "Chestplate", "body", "Body");
        if (chestTag != null && !isEmptyItem(chestTag)) {
            ItemStack stack = parseItemStack(chestTag);
            equipment.setChestplate(stack);
            equipment.setItem(102, stack);
            equipment.setRawItemId(102, chestTag.getString("id"));
        }

        NBTCompound legsTag = getChildCompound(equipCompound, "legs", "Legs", "leggings", "Leggings");
        if (legsTag != null && !isEmptyItem(legsTag)) {
            ItemStack stack = parseItemStack(legsTag);
            equipment.setLeggings(stack);
            equipment.setItem(101, stack);
            equipment.setRawItemId(101, legsTag.getString("id"));
        }

        NBTCompound feetTag = getChildCompound(equipCompound, "feet", "Feet", "boots", "Boots");
        if (feetTag != null && !isEmptyItem(feetTag)) {
            ItemStack stack = parseItemStack(feetTag);
            equipment.setBoots(stack);
            equipment.setItem(100, stack);
            equipment.setRawItemId(100, feetTag.getString("id"));
        }

        NBTCompound offhandTag = getChildCompound(equipCompound, "offhand", "off_hand", "Offhand", "OffHand", "offHand");
        if (offhandTag != null && !isEmptyItem(offhandTag)) {
            ItemStack stack = parseItemStack(offhandTag);
            equipment.setOffHand(stack);
            equipment.setItem(-106, stack);
            equipment.setItem(150, stack);
            equipment.setRawItemId(-106, offhandTag.getString("id"));
        }

        NBTCompound mainhandTag = getChildCompound(equipCompound, "mainhand", "main_hand", "Mainhand", "MainHand", "mainHand");
        if (mainhandTag != null && !isEmptyItem(mainhandTag)) {
            ItemStack stack = parseItemStack(mainhandTag);
            equipment.setMainHand(stack);
            equipment.setItem(selectedSlot, stack);
            equipment.setRawItemId(selectedSlot, mainhandTag.getString("id"));
        }
    }

    private static void parseEquipmentEntry(NBTCompound entry, OfflineEquipment equipment, int selectedSlot) {
        if (isEmptyItem(entry)) return;
        String slotStr = entry.getString("slot").toLowerCase(Locale.ROOT);
        int slotInt = entry.getInt("Slot", entry.getInt("slot", -999));
        ItemStack stack = parseItemStack(entry);
        String id = entry.getString("id");
        if (id.isEmpty()) id = entry.getString("Id");

        if (slotStr.contains("head") || slotInt == 103) {
            equipment.setHelmet(stack);
            equipment.setItem(103, stack);
            equipment.setRawItemId(103, id);
        } else if (slotStr.contains("chest") || slotStr.contains("body") || slotInt == 102) {
            equipment.setChestplate(stack);
            equipment.setItem(102, stack);
            equipment.setRawItemId(102, id);
        } else if (slotStr.contains("leg") || slotInt == 101) {
            equipment.setLeggings(stack);
            equipment.setItem(101, stack);
            equipment.setRawItemId(101, id);
        } else if (slotStr.contains("feet") || slotStr.contains("boot") || slotInt == 100) {
            equipment.setBoots(stack);
            equipment.setItem(100, stack);
            equipment.setRawItemId(100, id);
        } else if (slotStr.contains("offhand") || slotStr.contains("off_hand") || slotInt == -106 || slotInt == 150) {
            equipment.setOffHand(stack);
            equipment.setItem(-106, stack);
            equipment.setItem(150, stack);
            equipment.setRawItemId(-106, id);
        } else if (slotStr.contains("mainhand") || slotStr.contains("main_hand") || slotInt == selectedSlot) {
            equipment.setMainHand(stack);
            equipment.setItem(selectedSlot, stack);
            equipment.setRawItemId(selectedSlot, id);
        }
    }

    private static NBTCompound getChildCompound(NBTCompound parent, String... possibleKeys) {
        if (parent == null) return null;
        for (String key : possibleKeys) {
            NBTCompound child = parent.getCompound(key);
            if (child != null && !child.isEmpty()) {
                return child;
            }
        }
        return null;
    }

    private static boolean isEmptyItem(NBTCompound itemTag) {
        if (itemTag == null || itemTag.isEmpty()) return true;
        String id = itemTag.getString("id");
        if (id.isEmpty()) id = itemTag.getString("Id");
        return id.isBlank() || id.equalsIgnoreCase("minecraft:air") || id.equalsIgnoreCase("air");
    }

    public static ItemStack parseItemStack(NBTCompound tag) {
        if (tag == null) return null;
        String id = tag.getString("id");
        if (id == null || id.isBlank()) id = tag.getString("Id");
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
        if (components != null) {
            if (components.has("minecraft:custom_name")) {
                meta.setDisplayName(Utils.color(cleanJsonText(components.getString("minecraft:custom_name"))));
            } else if (components.has("minecraft:item_name")) {
                meta.setDisplayName(Utils.color(cleanJsonText(components.getString("minecraft:item_name"))));
            }
            if (components.has("minecraft:lore")) {
                List<Object> loreList = components.getList("minecraft:lore");
                List<String> lore = new ArrayList<>();
                for (Object line : loreList) {
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
            if (enchants != null) {
                NBTCompound levels = enchants.getCompound("levels");
                NBTCompound target = levels != null ? levels : enchants;
                for (String enchKey : target.keySet()) {
                    Enchantment ench = parseEnchantment(enchKey);
                    if (ench != null) {
                        meta.addEnchant(ench, target.getInt(enchKey), true);
                    }
                }
            }
        }

        // Legacy 1.20.4 and older 'tag'
        NBTCompound legacyTag = tag.getCompound("tag");
        if (legacyTag != null) {
            NBTCompound display = legacyTag.getCompound("display");
            if (display != null) {
                if (display.has("Name")) {
                    meta.setDisplayName(Utils.color(cleanJsonText(display.getString("Name"))));
                }
                if (display.has("Lore")) {
                    List<Object> loreList = display.getList("Lore");
                    List<String> lore = new ArrayList<>();
                    for (Object line : loreList) {
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
            for (Object enchElem : enchants) {
                if (enchElem instanceof NBTCompound enchTag) {
                    String enchId = enchTag.getString("id");
                    int lvl = enchTag.getInt("lvl");
                    Enchantment ench = parseEnchantment(enchId);
                    if (ench != null) {
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
            if (nsKey != null) {
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
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"text\"\\s*:\\s*\"([^\"]*)\"").matcher(trimmed);
            StringBuilder combined = new StringBuilder();
            while (matcher.find()) {
                String token = matcher.group(1);
                if (!token.isEmpty()) {
                    combined.append(token);
                }
            }
            if (combined.length() > 0) {
                return combined.toString();
            }
        }
        return trimmed;
    }
}
