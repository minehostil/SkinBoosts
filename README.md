# SkinBoosts

A Paper 1.21.x plugin that applies reward multipliers to players based on the **CustomModelData** (skin) of the tool they are holding. Supports RivalHarvesterHoes, RivalMobSwords, RivalPickaxes, and CyberLevels.

---

## How It Works

SkinBoosts listens to the native reward events fired by each supported plugin. When a reward event triggers, it reads the `CustomModelData` of the tool in the player's main hand and checks if a boost is registered for that value. If found, it multiplies the reward directly in the event before it is processed — no fallbacks, no approximations.

```
Player harvests → RivalHoes fires HoeEssenceReceiveEnchantEvent
→ SkinBoosts reads CMD from hand (e.g. 1001)
→ Finds boost: essence x1.5
→ event.setEssence(essence * 1.5)
→ Player receives boosted amount
```

The system is **stateless** — there is no per-player cache. The boost is resolved in real time on every event, so changing tools mid-session takes effect immediately on the next harvest.

---

## Supported Plugins

| Plugin | Events |
|---|---|
| RivalHarvesterHoes | Essence, Money, Tool XP |
| RivalMobSwords | Essence, Money, XP |
| RivalPickaxes | Essence, Money, XP |
| CyberLevels | XP |

All integrations are soft-depend — the plugin loads and runs normally even if any of them are absent.

---

## Requirements

- Paper 1.21.x
- Java 21
- One or more of the supported plugins (optional but needed for boosts to fire)
- PlaceholderAPI (optional, for placeholders)

---

## Installation

1. Place `SkinBoosts.jar` in your `plugins/` folder.
2. Restart the server — `plugins/SkinBoosts/config.yml` is generated automatically.
3. Register your skins with `/skinboost set` or edit `config.yml` directly.

---

## Configuration

```yaml
skins:
  1001:
    name: '&cFire Pick'        # Display name (optional, cosmetic only)
    multipliers:
      essence: 1.5             # 1.0 = no change, 2.0 = double
      money: 1.2
      tool-xp: 1.5
      cyber-xp: 1.3

max-multipliers:               # Hard cap per type. 0.0 = no limit
  essence: 5.0
  money: 5.0
  tool-xp: 5.0
  cyber-xp: 5.0

prefix: '&8[&6SkinBoosts&8] &r'
```

All messages are configurable under the `messages:` section. Changes take effect after `/skinboost reload`.

---

## Commands

All commands require the `skinboosts.admin` permission (default: op).

| Command | Description |
|---|---|
| `/skinboost set <cmd\|hand> [name] <essence> <money> <tool-xp> <cyber-xp> [player]` | Register or update a skin boost. Use `hand` to read CMD from item in hand. Name and player are optional. If player is provided, applies the CMD to their item in hand. |
| `/skinboost remove <cmd>` | Remove a registered skin boost. |
| `/skinboost list` | List all registered skins and active limits. |
| `/skinboost info [cmd]` | Show multipliers for a CMD. Reads from hand if CMD is omitted. |
| `/skinboost apply <cmd> [player]` | Apply a CustomModelData to the item in hand. Player is optional in-game, required from console. |
| `/skinboost reset [player]` | Remove the CustomModelData from the item in hand and delete its registered boost. Player is optional in-game, required from console. |
| `/skinboost reload` | Reload `config.yml` without restarting. |

### Examples

```bash
# Register a boost for CMD 1001 (name optional)
/skinboost set 1001 1.5 1.2 1.5 1.3

# Register reading CMD from item in hand
/skinboost set hand Fire_Pick 2.0 1.5 2.0 1.5

# Register boost and apply CMD to your item in hand (in-game)
/skinboost set 730 3.0 3.0 3.0 3.0

# Register boost and apply CMD to a player's item in hand (console or in-game)
skinboost set 730 3.0 3.0 3.0 3.0 Steve

# Apply CMD 730 to the item in your hand
/skinboost apply 730

# Apply CMD 730 to another player's item in hand
/skinboost apply 730 Steve

# Reset item in your hand
/skinboost reset

# Reset from console
/skinboost reset Steve
```

---

## Placeholders

Requires PlaceholderAPI. All placeholders read the item in the player's **main hand**.

| Placeholder | Example value | Description |
|---|---|---|
| `%skinboosts_essence%` | `1.50` | Essence multiplier |
| `%skinboosts_money%` | `1.20` | Money multiplier |
| `%skinboosts_toolxp%` | `1.50` | Tool XP multiplier |
| `%skinboosts_cyberxp%` | `1.30` | Cyber XP multiplier |
| `%skinboosts_essence_fmt%` | `x1.50` | Essence multiplier with x prefix |
| `%skinboosts_money_fmt%` | `x1.20` | Money multiplier with x prefix |
| `%skinboosts_toolxp_fmt%` | `x1.50` | Tool XP multiplier with x prefix |
| `%skinboosts_cyberxp_fmt%` | `x1.30` | Cyber XP multiplier with x prefix |
| `%skinboosts_name%` | `Fire Pick` | Display name of the active skin |
| `%skinboosts_cmd%` | `1001` | Raw CustomModelData value |
| `%skinboosts_active%` | `true` | Whether the item has a registered boost |

Returns `1.00` / `false` / `None` / `-1` if the item has no registered boost.

---

## Building

Place the required JARs in the `libs/` folder:

```
libs/
├── RivalHarvesterHoes.jar
├── RivalMobSwords.jar
├── RivalPickaxes.jar
└── CyberLevels.jar
```

Then build with Maven:

```bash
mvn package
```

The compiled JAR will be at `target/SkinBoosts-1.0.0.jar`.

---

## Project Structure

```
src/main/java/com/minehostil/skinboosts/
├── SkinBoostsPlugin.java            Main plugin class
├── commands/
│   └── SkinBoostCommand.java        /skinboost command handler
├── listeners/
│   └── HarvestListener.java         Native event listeners
├── manager/
│   └── SkinBoostManager.java        Boost registry and config persistence
├── model/
│   ├── SkinBoostData.java           Boost data POJO
│   └── Messages.java                Config-driven message system
└── placeholders/
    └── SkinBoostsExpansion.java     PlaceholderAPI expansion
```

---

## Performance

- No schedulers, no async tasks, no per-player state.
- Boost resolution is a single `HashMap.get()` call per event — O(1).
- All reward multiplication happens synchronously inside the event handler, same as the original plugin would.