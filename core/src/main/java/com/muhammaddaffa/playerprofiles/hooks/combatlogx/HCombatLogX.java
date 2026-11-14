package com.muhammaddaffa.playerprofiles.hooks.combatlogx;

import com.github.sirblobman.combatlogx.api.ICombatLogX;
import com.github.sirblobman.combatlogx.api.manager.ICombatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class HCombatLogX {

    public static boolean isInCombat(Player player){
        org.bukkit.plugin.Plugin combatLogPlugin = Bukkit.getPluginManager().getPlugin("CombatLogX");
        if (!(combatLogPlugin instanceof ICombatLogX combatLogX) || !combatLogPlugin.isEnabled()) return false;
        ICombatManager combatManager = combatLogX.getCombatManager();
        return combatManager != null && combatManager.isInCombat(player);
    }

}
