import com.muhammaddaffa.playerprofiles.inventory.items.GUIItem;
import com.muhammaddaffa.playerprofiles.inventory.items.ItemsLoader;
import com.muhammaddaffa.playerprofiles.manager.profile.Profile;
import com.muhammaddaffa.playerprofiles.manager.profile.ProfileManager;
import com.muhammaddaffa.playerprofiles.utils.ItemManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.UUID;

/** Run with assertions enabled; no test framework or server is required. */
public class BehaviorChecks {
    public static void main(String[] args) throws Exception {
        ProfileManager manager = new ProfileManager();
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Profile profile = manager.getOrCreate(uuid);
        assert !profile.isLocked();
        profile.setLocked(true);
        assert manager.getOrCreate(uuid.toString()) == profile;
        assert manager.getOrCreate(uuid).isLocked();
        assert manager.getOrCreate(UUID.randomUUID()) != profile;
        assert new Profile(uuid.toString(), true).isLocked();

        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                items:
                  first:
                    type: HELMET
                    material: DIAMOND_HELMET
                    amount: 2
                    name: '&a{target}'
                    slots: [0, 8]
                    glowing: true
                    hideAttributes: true
                    usePermission: true
                    permission: example.permission
                    lore: ['{target_status}', '%player_name%']
                    leftClickCommands: ['[PLAYER] profile {target}']
                    rightClickCommands: ['[CLOSE]']
                    customModelData: 123
                    itemModel: minecraft:diamond_helmet
                    onlyOwner: true
                    onlyVisitor: false
                    priority: 7
                  defaults: {}
                """);
        List<GUIItem> items = ItemsLoader.readItems(config);
        assert items.size() == 2;
        assert items.getFirst().equals(new GUIItem("HELMET", "DIAMOND_HELMET", 2,
                "&a{target}", List.of(0, 8), true, true, true, "example.permission",
                List.of("{target_status}", "%player_name%"),
                List.of("[PLAYER] profile {target}"), List.of("[CLOSE]"),
                123, "minecraft:diamond_helmet", true, false, 7));
        GUIItem defaults = items.get(1);
        assert defaults.type().equals("DUMMY");
        assert defaults.material().equals("BARRIER");
        assert defaults.amount() == 0;
        assert defaults.priority() == 100;
        assert defaults.permission().equals("invalid.permission");
        assert defaults.itemModel().isEmpty();
        assert defaults.slots().isEmpty();
        assert defaults.leftCommands().isEmpty();
        assert defaults.rightCommands().isEmpty();
        assert ItemsLoader.readItems(new YamlConfiguration()).isEmpty();
        for (String resource : List.of("gui.yml", "custom-gui/punish-gui.yml")) {
            List<GUIItem> bundled = ItemsLoader.readItems(YamlConfiguration.loadConfiguration(
                    new File("core/src/main/resources", resource)));
            assert !bundled.isEmpty() : resource;
            assert bundled.stream().allMatch(item -> !item.slots().isEmpty());
        }
        for (String version : List.of("1.21.5-R0.1-SNAPSHOT", "1.21.11-R0.1-SNAPSHOT", "26.0-R0.1-SNAPSHOT", "26.3-R0.1-SNAPSHOT", "26.3.1-R0.1", "26.3.build.979-stable")) {
            assert ItemManager.isVersionAtLeast(version, 1, 9, 0);
            assert ItemManager.isVersionAtLeast(version, 1, 14, 0);
            assert ItemManager.isVersionAtLeast(version, 1, 21, 2);
        }
        assert !ItemManager.isVersionAtLeast("1.8.8-R0.1", 1, 9, 0);
        assert !ItemManager.isVersionAtLeast("1.7.10-R0.1", 1, 9, 0);
        assert !ItemManager.isVersionAtLeast("1.13.2-R0.1", 1, 14, 0);
        assert !ItemManager.isVersionAtLeast("invalid", 1, 14, 0);
        assert !ItemManager.isVersionAtLeast("1.x-R0.1", 1, 14, 0);
        for (String bad : List.of("size: 0", "size: 10", "size: 63", "size: '27'",
                "size: 9\nitems: false", "size: 9\nitems: {bad: false}",
                "size: 9\nitems: {bad: {slots: [-1]}}", "size: 9\nitems: {bad: {slots: [9]}}",
                "size: 9\nitems: {bad: {slots: ['0']}}")) {
            config.loadFromString(bad);
            boolean rejected = false;
            try { ItemsLoader.validateMenu(config); }
            catch (IllegalArgumentException expected) { rejected = true; }
            assert rejected : bad;
        }
        config.loadFromString("size: 9");
        ItemsLoader.validateMenu(config);
        config.loadFromString("size: 9\nitems: {first: {slots: [0, 8]}}");
        ItemsLoader.validateMenu(config);

        // Verify Floodgate Bedrock UUID detection
        UUID bedrockUuid = new UUID(0L, 123456789L);
        assert com.muhammaddaffa.playerprofiles.hooks.floodgate.FloodgateHook.isBedrockPlayer(bedrockUuid);
        UUID javaUuid = UUID.randomUUID();
        assert !com.muhammaddaffa.playerprofiles.hooks.floodgate.FloodgateHook.isBedrockPlayer(new UUID(1L, javaUuid.getLeastSignificantBits()));

        // Verify NBT parsing with GZIP
        java.io.ByteArrayOutputStream byteOut = new java.io.ByteArrayOutputStream();
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.util.zip.GZIPOutputStream(byteOut))) {
            out.writeByte(10); // TAG_Compound
            out.writeUTF("");  // Root name
            // SelectedItemSlot: 0
            out.writeByte(3); // TAG_Int
            out.writeUTF("SelectedItemSlot");
            out.writeInt(0);
            // Inventory: List of Compounds
            out.writeByte(9); // TAG_List
            out.writeUTF("Inventory");
            out.writeByte(10); // elemType = TAG_Compound
            out.writeInt(2);   // 2 items

            // Item 1: Helmet in slot 103
            out.writeByte(1); // TAG_Byte (Slot)
            out.writeUTF("Slot");
            out.writeByte(103);
            out.writeByte(8); // TAG_String (id)
            out.writeUTF("id");
            out.writeUTF("minecraft:diamond_helmet");
            out.writeByte(3); // TAG_Int (count)
            out.writeUTF("count");
            out.writeInt(1);
            out.writeByte(0); // TAG_End

            // Item 2: MainHand in slot 0
            out.writeByte(1); // TAG_Byte (Slot)
            out.writeUTF("Slot");
            out.writeByte(0);
            out.writeByte(8); // TAG_String (id)
            out.writeUTF("id");
            out.writeUTF("minecraft:netherite_sword");
            out.writeByte(3); // TAG_Int (count)
            out.writeUTF("count");
            out.writeInt(1);
            out.writeByte(0); // TAG_End

            out.writeByte(0); // TAG_End root
        }

        com.muhammaddaffa.playerprofiles.utils.nbt.NBTCompound rootTag =
                com.muhammaddaffa.playerprofiles.utils.nbt.NBTReader.readStream(new java.io.ByteArrayInputStream(byteOut.toByteArray()));
        assert rootTag.getInt("SelectedItemSlot") == 0;
        com.muhammaddaffa.playerprofiles.inventory.OfflineEquipment equipment =
                com.muhammaddaffa.playerprofiles.utils.PlayerDataLoader.parseEquipment(rootTag);
        assert "minecraft:diamond_helmet".equals(equipment.getRawItemId(103));
        assert "minecraft:netherite_sword".equals(equipment.getRawItemId(0));
        assert equipment.getRawItemId(102) == null;

        System.out.println("Behavior checks passed: profile state, item defaults/order, version boundaries, offline NBT parsing, Bedrock detection and menu validation.");
    }
}
