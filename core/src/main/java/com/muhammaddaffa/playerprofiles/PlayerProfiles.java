package com.muhammaddaffa.playerprofiles;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.playerprofiles.commands.BMCPlayerProfileReloadCommand;
import com.muhammaddaffa.playerprofiles.commands.LockCommand;
import com.muhammaddaffa.playerprofiles.commands.MainCommand;
import com.muhammaddaffa.playerprofiles.commands.ProfileCommand;
import com.muhammaddaffa.playerprofiles.commands.ToggleCommand;
import com.muhammaddaffa.playerprofiles.commands.UnlockCommand;
import com.muhammaddaffa.playerprofiles.hooks.placeholderapi.PlayerProfilesExpansion;

import com.muhammaddaffa.playerprofiles.inventory.InventoryManager;
import com.muhammaddaffa.playerprofiles.listeners.PlayerInteract;
import com.muhammaddaffa.playerprofiles.manager.DependencyManager;
import com.muhammaddaffa.playerprofiles.manager.combat.CombatManager;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGUIManager;
import com.muhammaddaffa.playerprofiles.manager.customgui.CustomGuiCreator;
import com.muhammaddaffa.playerprofiles.manager.profile.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerProfiles extends JavaPlugin {

    private static PlayerProfiles instance;

    public static Config DATA_DEFAULT, CONFIG_DEFAULT, GUI_DEFAULT, GUI_CREATOR;

    private final InventoryManager inventoryManager = new InventoryManager(this);
    private final CustomGUIManager customGUIManager = new CustomGUIManager(this);
    private final ProfileManager profileManager = new ProfileManager();
    private final PlayerInteract playerInteract = new PlayerInteract(this);
    private final CustomGuiCreator customGuiCreator = new CustomGuiCreator(this);
    private final CombatManager combatManager = new CombatManager();

    @Override
    public void onLoad() {
        MDLib.inject(this);
        MDLib.registerWorldGuard();
    }

    @Override
    public void onEnable(){
        instance = this;
        // Injecting the libs
        MDLib.onEnable(this);
        // Check the dependency
        DependencyManager.checkDependency();
        // Register PlaceholderAPI expansion if present
  if(DependencyManager.PLACEHOLDER_API) {

            new PlayerProfilesExpansion(this, "bmcplayerprofiles").register();
            new PlayerProfilesExpansion(this, "bmcplayerpoints").register();
            new PlayerProfilesExpansion(this, "playerprofiles").register();
        }
        // Initialize all config
        initializeConfig();
        // Initialize all custom gui creator
        customGuiCreator.createCustomGui();
        // Initialize all config values
        ConfigValue.initialize();
        // Load all items for the inventory
        inventoryManager.initialize();
        // Load all custom guis
        customGUIManager.loadCustomGUI();
        // Load all profile data
        profileManager.loadProfileData();
        // Register the player interact and combat events
        Bukkit.getPluginManager().registerEvents(playerInteract, this);
        Bukkit.getPluginManager().registerEvents(combatManager, this);
        // Initialize proxy bridge if enabled
        com.muhammaddaffa.playerprofiles.proxy.ProxyBridge.initialize(this);
        // Register all commands
        registerCommands();
        com.muhammaddaffa.mdlib.utils.Logger.info("&6[BMCPlayerProfiles] &eBendersMC Private Build - Authorized for BMC Devs only.");
    }

    @Override
    public void onDisable(){
        try {
            inventoryManager.closeInventories();
            profileManager.saveProfileData();
            CombatManager.clearAll();
            com.muhammaddaffa.playerprofiles.proxy.ProxyBridge.shutdown();
        } finally {
            MDLib.shutdown();
            instance = null;
        }
    }

    private void initializeConfig() {
        CONFIG_DEFAULT      = new Config("config.yml", null, true);
        GUI_DEFAULT         = new Config("gui.yml", null, true);
        DATA_DEFAULT        = new Config("data.yml", null, false);
        GUI_CREATOR         = new Config("gui-creator.yml", null, true);

        CONFIG_DEFAULT.setShouldUpdate(true);
        Config.updateConfigs();
        Config.reload();
    }

    public void reloadAllThing(){
        // Menus keep item lists and tasks from the previous configuration.
        inventoryManager.closeInventories();
        Config.reload();
        DependencyManager.checkDependency();
        // Re-initialize the config value
        ConfigValue.initialize();
        if (!ConfigValue.COOLDOWN_ENABLED) playerInteract.clearCooldowns();
        CombatManager.clearAll();
        com.muhammaddaffa.playerprofiles.proxy.ProxyBridge.shutdown();
        com.muhammaddaffa.playerprofiles.proxy.ProxyBridge.initialize(this);
        // Reload all items for the profile inventory
        inventoryManager.reInitialize();
        // Reload all custom gui creator
        customGuiCreator.createCustomGui();
        // Reload all custom guis
        customGUIManager.reloadCustomGUI();
    }

    private void registerCommands(){
        // Register /playerprofiles command
        // Register /bmcplayerprofile command
        new MainCommand(this).registerThisCommand();
        // Register /profile command
        // Register /bmcplayerprofilereload command
        new BMCPlayerProfileReloadCommand(this).registerThisCommand();
        // Register /vprofile command
        new ProfileCommand(this).registerThisCommand();
        // Register /lockprofile command
        new LockCommand(this).registerThisCommand();
        // Register /unlockprofile command
        new UnlockCommand(this).registerThisCommand();
        // Register /toggleprofile command
        new ToggleCommand(this).registerThisCommand();
    }

    public static PlayerProfiles getInstance() {
        return instance;
    }

    public InventoryManager getInventoryManager(){
        return inventoryManager;
    }

    public CustomGUIManager getCustomGUIManager() {
        return customGUIManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }
}
