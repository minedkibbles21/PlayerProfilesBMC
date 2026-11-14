package com.muhammaddaffa.playerprofiles.inventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OfflineEquipment {

    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack mainHand;
    private ItemStack offHand;
    private final Map<Integer, ItemStack> slots = new HashMap<>();
    private final Map<Integer, String> rawItemIds = new HashMap<>();

    public OfflineEquipment() {
    }

    private ItemStack air() {
        if (Bukkit.getServer() == null) return null;
        return new ItemStack(Material.AIR);
    }

    public ItemStack getHelmet() {
        return helmet != null ? helmet.clone() : air();
    }

    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }

    public ItemStack getChestplate() {
        return chestplate != null ? chestplate.clone() : air();
    }

    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate;
    }

    public ItemStack getLeggings() {
        return leggings != null ? leggings.clone() : air();
    }

    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings;
    }

    public ItemStack getBoots() {
        return boots != null ? boots.clone() : air();
    }

    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }

    public ItemStack getMainHand() {
        return mainHand != null ? mainHand.clone() : air();
    }

    public void setMainHand(ItemStack mainHand) {
        this.mainHand = mainHand;
    }

    public ItemStack getOffHand() {
        return offHand != null ? offHand.clone() : air();
    }

    public void setOffHand(ItemStack offHand) {
        this.offHand = offHand;
    }

    public ItemStack getItem(int slot) {
        ItemStack storedStack = slots.get(slot);
        return storedStack != null ? storedStack.clone() : air();
    }

    public void setItem(int slot, ItemStack storedStack) {
        if (storedStack != null) {
            this.slots.put(slot, storedStack);
        }
    }

    public void setRawItemId(int slot, String id) {
        this.rawItemIds.put(slot, id);
    }

    public String getRawItemId(int slot) {
        return this.rawItemIds.get(slot);
    }

    public Map<Integer, ItemStack> getSlots() {
        return Collections.unmodifiableMap(slots);
    }

    public Map<Integer, String> getRawItemIds() {
        return Collections.unmodifiableMap(rawItemIds);
    }
}
