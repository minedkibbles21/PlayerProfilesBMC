package com.muhammaddaffa.playerprofiles.manager.profile;

import com.muhammaddaffa.mdlib.utils.Config;
import com.muhammaddaffa.playerprofiles.PlayerProfiles;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileManager {

    private static final String PROFILE_KEY = "d" + "ata";
    private final Map<String, Profile> profileMap = new HashMap<>();
    private boolean loaded;

    public Profile getOrCreate(Player player){
        return getOrCreate(player.getUniqueId());
    }

    public Profile getOrCreate(UUID uuid){
        return getOrCreate(uuid.toString());
    }

    public Profile getOrCreate(String uuid){
        return profileMap.computeIfAbsent(uuid, key -> new Profile(key, false, this::saveProfileData));
    }

    public boolean isProfileLocked(Player player){
        return getOrCreate(player).isLocked();
    }

    public boolean isProfileLocked(UUID uuid){
        return getOrCreate(uuid).isLocked();
    }

    public void lockProfile(Player player){
        getOrCreate(player).setLocked(true);
    }

    public void unlockProfile(Player player){
        getOrCreate(player).setLocked(false);
    }

    public void loadProfileData(){
        this.loaded = false;
        Config dataStore = PlayerProfiles.DATA_DEFAULT;
        if (dataStore == null) return;
        YamlConfiguration config = new YamlConfiguration();
        try {
            if (dataStore.getFile() != null) {
                config.load(dataStore.getFile());
            }
            if (config.contains(PROFILE_KEY) && !config.isConfigurationSection(PROFILE_KEY)) {
                throw new IllegalArgumentException("Invalid profile section");
            }
            Map<String, Profile> restored = new HashMap<>();
            if (config.isConfigurationSection(PROFILE_KEY)) {
                for (String uuid : config.getConfigurationSection(PROFILE_KEY).getKeys(false)) {
                    if (!config.isBoolean(PROFILE_KEY + "." + uuid)) throw new IllegalArgumentException("Invalid lock state");
                    restored.put(uuid, new Profile(uuid, config.getBoolean(PROFILE_KEY + "." + uuid), this::saveProfileData));
                }
            }
            dataStore.getConfig().loadFromString(config.saveToString());
            profileMap.clear();
            profileMap.putAll(restored);
            this.loaded = true;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load file; the original file has been preserved.");
        }
    }

    public void saveProfileData() {
        Config dataStore = PlayerProfiles.DATA_DEFAULT;
        if (!this.loaded || dataStore == null) return;
        FileConfiguration config = dataStore.getConfig();
        Map<String, Object> previous = config.isConfigurationSection(PROFILE_KEY)
                ? new HashMap<>(config.getConfigurationSection(PROFILE_KEY).getValues(false)) : null;
        for (Profile profile : profileMap.values()) {
            config.set(PROFILE_KEY + "." + profile.getUUID(), profile.isLocked());
        }
        Path file = dataStore.getFile().toPath();
        try {
            if (file != null) {
                atomicWriteFile(file, config.saveToString());
            }
        } catch (IOException ex) {
            config.set(PROFILE_KEY, null);
            if (previous != null) config.createSection(PROFILE_KEY, previous);
            throw new IllegalStateException("Unable to save profile locks; state was not changed.");
        }
    }

    private void atomicWriteFile(Path targetFile, String content) throws IOException {
        Path stagingFile = Files.createTempFile(targetFile.toAbsolutePath().getParent(), ".profiles-", ".part");
        try {
            if (stagingFile != null) {
                Files.writeString(stagingFile, content, StandardCharsets.UTF_8);
                try {
                    if (Files.exists(stagingFile)) {
                        Files.move(stagingFile, targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(stagingFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } finally {
            try {
                if (stagingFile != null) {
                    Files.deleteIfExists(stagingFile);
                }
            } catch (IOException ignored) { }
        }
    }
}
