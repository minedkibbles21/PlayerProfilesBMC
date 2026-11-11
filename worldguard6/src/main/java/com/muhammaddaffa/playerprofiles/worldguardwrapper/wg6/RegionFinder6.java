package com.muhammaddaffa.playerprofiles.worldguardwrapper.wg6;

import com.muhammaddaffa.api.IRegionFinder;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegionFinder6 implements IRegionFinder {

    @Override
    public List<String> getRegions(Location location) {
        if (location == null || location.getWorld() == null) return Collections.emptyList();
        try {
            com.sk89q.worldguard.protection.ApplicableRegionSet ars = WGBukkit.getPlugin().getRegionContainer().createQuery().getApplicableRegions(location);
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
