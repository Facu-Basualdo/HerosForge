# Hero's Forge

Level up your character RPG-style. Spend **experience** to buy **points** in the stats you want, up to 100 each: **40 stats in 5 branches**. More health, more damage (bows and crossbows too), critical hits, dodge, a recharging shield, slower hunger, an item magnet, villager discounts…

Every point costs the same **30 experience points**, whether you are level 2 or level 40. Changed your mind? Removing a point refunds half. **Points are permanent**: you keep them when you die.

## 40 stats in 5 branches

Branches only group the stats: nothing is locked and you can buy any of them from the start. Maxing everything takes 4,000 points (120,000 experience), so you'll have to choose.

### Warrior

| Stat | At 100 points |
|---|---|
| Melee damage | +25 |
| Attack speed | +30% |
| Ranged damage (bow, crossbow, thrown trident) | +100% |
| Dexterity (drawing bows and crossbows) | +40% speed |
| Sweeping edge | +50% of the damage |
| Heavy blow | +1 knockback (like Knockback I) |
| Critical hit | 20% chance to deal +50% |
| Execution | +50% damage to enemies below 30% health |

### Guardian

| Stat | At 100 points |
|---|---|
| Health | +30 hearts |
| Armor | +8 |
| Knockback resistance | +50% |
| Armor toughness | +8 |
| Blast stability (explosion knockback) | −50% |
| Resistances (fire, explosions and magic) | −30% damage |
| Retaliation | Attackers take back 30% of the melee damage |
| Shield | 4 absorption hearts that recharge |
| Dodge | 15% chance to avoid an attack |

### Survivor

| Stat | At 100 points |
|---|---|
| Regeneration | +9 hearts per minute |
| Life steal (melee and ranged) | 10% of the damage you deal |
| Metabolism | Hunger drains half as fast |
| Nutrition | Food fills +50% |
| Glutton | Eat and drink twice as fast |
| Tenacity | Negative effects last half as long |
| Fireproof | Burn for half as long |
| Breathing | Like Respiration III |

### Explorer

| Stat | At 100 points |
|---|---|
| Speed | +20% |
| Jump | +20% (1.8 blocks) |
| Safe fall | +10 blocks with no damage |
| Step | Walk up full blocks without jumping |
| Swimming | Water slows you down 50% less |
| Stealth | Sneak at 70% speed instead of 30% |
| Lightness | −25% gravity: jump a bit higher, fall slower |

### Artisan

| Stat | At 100 points |
|---|---|
| Mining speed | +100% |
| Underwater mining | +80% (like Aqua Affinity) |
| Reach (blocks and mobs) | +2 blocks |
| Luck (fishing) | +3 |
| Magnet | Pull items and experience from 6 blocks away |
| Equipment care | 50% chance not to use durability |
| Bargaining | −30% villager prices (like Hero of the Village I) |
| Wisdom | +25% experience from orbs |

- **You pay in experience points, not levels**: the purchase lowers your bar and, if needed, your level.
- **You buy what you can afford**: ask for 5 points with enough for 3, and you get (and pay for) 3.
- Critical hits and execution work with arrows too. Dodge only avoids attacks from something (a mob, an arrow, another player): lava and falls still hurt.
- The shield recharges half a heart per second after 5 seconds without taking damage.

## How to use it

### Chest menu (works with vanilla clients)

**`/stats`** (or `/estadisticas`) opens a chest menu with the 5 branches.

- **Click** buys 1 point, **shift-click** buys 5.
- **Right-click** removes 1 and refunds half, **shift-right-click** removes 5.
- Each item shows your points, what they give, what the next one adds and its cost (green if you can afford it).

### Own screen (optional)

Install the mod on your client too and press **K** (or use the button in your inventory) for a dedicated screen: one tab per branch, **+1**, **+5** and **-1** buttons, and the next point's effect and cost on hover.

### Commands

| Command | What it does | Who |
|---|---|---|
| `/stats` | Opens the menu | everyone |
| `/stats view [player]` | Your points (or someone else's) in chat | everyone (others: op) |
| `/stats buy <stat> [amount]` | Buys points | everyone |
| `/stats refund <stat> [amount]` | Removes points, refunds half | everyone |
| `/stats give <player> <stat> <amount>` | Gives points for free | op |
| `/stats reset <player>` | Sets everything to 0 (no refund) | op |
| `/stats reload` | Reloads the config | op |

Stat ids are in Spanish, as in the config (for example `/stats buy vida 5`); the command suggests them as you type.

## Configuration

Every value (cost, refund, and what each point gives and its cap) is in `config/herosforge.json` and reloads with `/stats reload`, no restart needed. With **[Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu)** you can also edit it in-game, with a name and a tooltip for every option.

## Languages

Menus, the screen and messages follow each player's game language: **English or Spanish**.

## Installation

All the logic runs on the server: **players don't need to install anything**. On the client the mod is optional and adds the own screen and the Mod Menu settings.

Requires **Minecraft 26.3**, **Fabric Loader 0.19.5+**, **[Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)** and **Java 25**.

## Works well with

- **Bloodmoon & Bosses**: the default balance is designed around its mob levels. With the expected gear and points for each zone, a zombie takes 5 or 6 hits anywhere in the world.
- **Soul Link** (3.0.0 or newer): points belong to the whole linked group.
- **Mythic Loot**: life steal and ranged damage stack with its affixes.

License: MIT. Feel free to use it in modpacks.
