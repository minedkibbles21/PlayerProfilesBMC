package com.muhammaddaffa.playerprofiles.worldguardwrapper.wg7;

import com.muhammaddaffa.api.IRegionFinder;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegionFinder7 implements IRegionFinder {

    @Override
    public List<String> getRegions(Location location) {
        if (location == null || location.getWorld() == null) return Collections.emptyList();
        try {
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);
            com.sk89q.worldguard.protection.regions.RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.ApplicableRegionSet ars = container.createQuery().getApplicableRegions(loc);
            List<String> regionNames = new ArrayList<>();
            for (ProtectedRegion region : ars) {
                regionNames.add(region.getId());
            }
            return regionNames;
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

}
