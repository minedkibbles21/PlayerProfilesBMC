# BMCPlayerProfiles

A high-performance, lightweight player profile menu plugin custom-built for Minecraft **26.3** (with backward compatibility for all **26.x** versions and **1.21.11**). Features custom GUI creation, full color customization (Adventure MiniMessage, Hex, and legacy colors), PlaceholderAPI expansion for seamless **zMenu** and **DeluxeMenus** integration, profile locking, and optional combat/region checks.

- **Author**: `aglerr, Starfruit2210, MinedKibbles21`
- **Data Folder**: `plugins/BMCPlayerProfiles/`
- **Java Runtime**: **Java 25 LTS**

---

## Features

- **Extensive Color Customization**: Supports modern MiniMessage tags (`<blue>`, `<red>`, `<gradient:...>`, `<rainbow>`), hex codes (`&#RRGGBB` and `{#RRGGBB}`), and native Minecraft legacy color codes (`&a`, `&c`, `&6`).
- **Zero Overhead & Lightweight**: Efficient profile caching, atomic file writes (`data.yml`), and task management to ensure zero server lag.
- **Full zMenu & DeluxeMenus Compatibility**: Built-in PlaceholderAPI expansion exposes profile lock states, view checks, and symbols for requirements, conditions, and custom menus.
- **Interactive Profile Viewer**: View profiles via `/vprofile` or shift-right-clicking players.
- **Custom GUI Engine**: Create unlimited custom GUIs with per-item permissions, priorities, visitor/owner views, and custom click actions (`[CONSOLE]`, `[PLAYER]`, `[MESSAGEPLAYER]`, `[MESSAGETARGET]`, `[OPENGUIMENU]`, `[CLOSE]`).
- **WorldGuard & Combat Integrations**: Integrates with WorldGuard (v6 and v7) and combat loggers (CombatLogX, DeluxeCombat) to disable profiles in PvP or restricted regions.

---

## Placeholders

### 1. Built-in GUI & Action Placeholders
These placeholders can be used in `gui.yml`, custom menu configs, item names, lores, and click action commands:

| Placeholder | Description | Example Output |
|---|---|---|
| `{player}` | Username of the player opening the menu | `Steve` |
| `{target}` | Username of the target whose profile is opened | `Alex` |
| `{player_displayname}` | Display name / nickname of the viewer | `[VIP] Steve` |
| `{target_displayname}` | Display name / nickname of the target | `[Mod] Alex` |
| `{player_status}` | Formatted lock status with color | `&aUnlocked` / `&cLocked` |
| `{target_status}` | Formatted lock status of the target with color | `&aUnlocked` / `&cLocked` |
| `{player_status_plain}` | Plain lock status without colors | `Unlocked` / `Locked` |
| `{target_status_plain}` | Plain lock status of target without colors | `Unlocked` / `Locked` |
| `{player_status_symbol}` | Visual lock symbol of the viewer | `🔓` / `🔒` |
| `{target_status_symbol}` | Visual lock symbol of the target player | `🔓` / `🔒` |
| `{player_health}` | Health of the viewer | `20.0` |
| `{target_health}` | Health of the target player | `18.5` |
| `{player_level}` | Experience level of the viewer | `30` |
| `{target_level}` | Experience level of the target player | `42` |
| `{player_exp}` | Experience points progress of the viewer | `0.45` |
| `{target_exp}` | Experience points progress of the target | `0.80` |
| `{player_ping}` | Network ping latency of the viewer | `24` |
| `{target_ping}` | Network ping latency of the target player | `38` |
| `{player_gamemode}` | Game mode of the viewer | `SURVIVAL` |
| `{target_gamemode}` | Game mode of the target player | `SURVIVAL` |
| `{player_uuid}` | UUID of the viewer | `e06197f6-3987-4312-a224-aa4c3302c98b` |
| `{target_uuid}` | UUID of the target player | `c06197f6-3987-4312-a224-aa4c3302c98c` |
| `{player_world}` | Current world of the viewer | `world` |
| `{target_world}` | Current world of the target player | `world_nether` |

> Any external PlaceholderAPI placeholder (e.g. `%vault_eco_balance%`, `%player_ping%`) can also be used directly inside menu titles, names, lores, and command actions.

---

### 2. PlaceholderAPI Expansion
BMCPlayerProfiles automatically registers placeholders under both `%bmcplayerprofiles_*%` and legacy `%playerprofiles_*%` (with `%bmcplayerpoints_*%` supported for backward compatibility). Use these in **zMenu**, **DeluxeMenus**, scoreboards, chat formatters, or tab lists:

| Placeholder | Description | Output |
|---|---|---|
| `%bmcplayerprofiles_locked%` | Check if player's profile is locked (boolean string) | `true` / `false` |
| `%bmcplayerprofiles_is_locked%` | Check if player's profile is locked (yes/no) | `yes` / `no` |
| `%bmcplayerprofiles_status%` | Formatted lock status with color codes | `&aUnlocked` / `&cLocked` |
| `%bmcplayerprofiles_status_plain%` | Plain text lock status without colors | `Unlocked` / `Locked` |
| `%bmcplayerprofiles_symbol%` | Visual lock icon of the player | `🔓` / `🔒` |
| `%bmcplayerprofiles_lock_symbol%` | Alias for lock icon | `🔓` / `🔒` |
| `%bmcplayerprofiles_locked_<player>%` | Check if a specific player's profile is locked | `true` / `false` |
| `%bmcplayerprofiles_is_locked_<player>%` | Check if a specific player's profile is locked (yes/no) | `yes` / `no` |
| `%bmcplayerprofiles_status_<player>%` | Formatted lock status for a specific player | `&aUnlocked` / `&cLocked` |
| `%bmcplayerprofiles_status_plain_<player>%` | Plain lock status for a specific player | `Unlocked` / `Locked` |
| `%bmcplayerprofiles_symbol_<player>%` | Visual lock icon for a specific player | `🔓` / `🔒` |
| `%bmcplayerprofiles_lock_symbol_<player>%` | Alias for target lock icon | `🔓` / `🔒` |
| `%bmcplayerprofiles_can_view_<player>%` | Check if viewer can view target (permissions, world, combat) | `true` / `false` |

*(All above placeholders can also be referenced using `%playerprofiles_<placeholder>%`)*

---

## Color Customization Guide

All messages, GUI titles, item names, and lore entries support multiple color methods simultaneously:

- **MiniMessage Tags**:
  - `<blue>`, `<red>`, `<green>`, `<gold>`, `<yellow>`, `<gray>`, `<light_purple>`, etc.
  - `<gradient:#55ff55:#00aa00>Gradient Text</gradient>`
  - `<rainbow>Rainbow Text</rainbow>`
  - `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`
- **Hex Colors**:
  - `&#55ff55`
  - `{#55ff55}`
- **Legacy Color Codes**:
  - `&0` - `&9`, `&a` - `&f`, `&l`, `&m`, `&n`, `&o`, `&r`

---

## Commands & Permissions

| Command | Usage | Description | Permission |
|---|---|---|---|
| `/vprofile` | `/vprofile [player]` | Open your own or another player's profile | `playerprofiles.vprofile` (own)<br>`playerprofiles.vprofile.others` (others) |
| `/bmcplayerprofilereload` | `/bmcplayerprofilereload` | Dedicated reload for all configuration files and custom menus | `playerprofiles.admin` |
| `/bmcplayerprofile help` | `/bmcplayerprofile help` | Show administrative help menu | `playerprofiles.admin` |
| `/bmcplayerprofile opengui` | `/bmcplayerprofile opengui <player> <target> <gui>` | Open a custom menu for a player | `playerprofiles.admin` |
| `/bmcplayerprofile openprofile` | `/bmcplayerprofile openprofile <target> <player>` | Force open a target's profile for a player | `playerprofiles.admin` |
| `/bmcplayerprofile listgui` | `/bmcplayerprofile listgui` | List all loaded custom GUIs | `playerprofiles.admin` |
| `/bmcplayerprofile setcombattimer` | `/bmcplayerprofile setcombattimer <seconds>` | View or update combat timer duration | `playerprofiles.admin` |
| `/lockprofile` | `/lockprofile <player>` | (Admin) Lock a player's profile | `playerprofiles.admin` or `playerprofiles.lockprofile.others` |
| `/unlockprofile` | `/unlockprofile <player>` | (Admin) Unlock a player's profile | `playerprofiles.admin` or `playerprofiles.unlockprofile.others` |
| `/toggleprofile` | `/toggleprofile <player>` | (Admin) Toggle a player's profile lock state | `playerprofiles.admin` or `playerprofiles.toggleprofile.others` |

### Command Aliases
Configured in `config.yml`:
- `/vprofile`: `/viewprofile`, `/vp`
- `/bmcplayerprofile`: `/bmcplayerprofiles`, `/playerprofiles`, `/playerprofile`, `/playerp`, `/pp`
- `/bmcplayerprofilereload`: `/bmcprofilereload`, `/playerprofilesreload`
- `/lockprofile`: `/profilelock`, `/lock`
- `/unlockprofile`: `/profileunlock`, `/unlock`
- `/toggleprofile`: `/profiletoggle`, `/togglep`

---

## zMenu & DeluxeMenus Integration

### 1. Global Profile Delegation (Option 3)
For large servers (e.g. 1k+ CCU), you can delegate `/vprofile` and shift-right-click interactions directly to a zMenu inventory while retaining all permissions, lock states, distance checks, combat checks, and region restrictions.

In `config.yml`:
```yaml
zmenu:
  enabled: true
  profileMenu: "profile" # Name of your zMenu inventory (profile.yml -> "profile")
```
When a player executes `/vprofile` or shift-right-clicks a player, BMCPlayerProfiles validates all checks and immediately opens that zMenu inventory without intermediate menus.

### 2. Dedicated Action Tag (Option 1)
Any item inside `gui.yml` or custom menus can open a zMenu inventory directly using `[ZMENU] <inventory>` or `[ZM] <inventory>`:
```yaml
items:
  stats_button:
    material: BOOK
    name: "<gold>View Stats"
    slots: [ 14 ]
    leftClickCommands:
      - "[ZMENU] player_stats"
    rightClickCommands:
      - "[ZM] player_stats"
```

### 3. PlaceholderAPI Integration (DeluxeMenus & zMenu)
Check lock status in DeluxeMenus requirements:
```yaml
open_requirement:
  requirements:
    profile_unlocked:
      type: "!string equals"
      input: "%bmcplayerprofiles_locked%"
      output: "true"
      deny_commands:
        - "[message] &cThis profile is currently locked!"
```
Or display status in a zMenu item:
```yaml
items:
  status_button:
    item:
      material: PAPER
      name: "<gold>Profile Status"
      lore:
        - "<gray>State: %bmcplayerprofiles_status%"
        - "<gray>Icon: %bmcplayerprofiles_symbol%"
```

---

## Building from Source

Requires **JDK 25** and **Maven 3.9+**:

```sh
mvn clean package
```

The compiled plugin JAR will be located at:
```
target/BMCPlayerProfiles.jar
```

To compile explicitly with the Paper 26.3 profile:
```sh
mvn -Ppaper-26.3 clean package
```

Run test checks:
```sh
./checks/run.sh
```

---

## License & Usage Restrictions

**All Rights Reserved.**
This project is private and proprietary to **BendersMC Developers (`BMC Dev`)**.

Unauthorized copying, redistribution, public hosting, modification, or usage of this codebase or its compiled artifacts outside of authorized BendersMC development Team is strictly prohibited. Original underlying base by `aglerr` and `Starfruit2210`; customized and maintained by `MinedKibbles21`.
