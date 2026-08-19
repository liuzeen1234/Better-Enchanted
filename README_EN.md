# Better Enchanted

> Fabric Mod | Minecraft 1.20.4 | Java 17  
> Bring vanilla enchantment effects to food and potions.

---

## Introduction

**Better Enchanted** is a Minecraft Fabric mod whose core gameplay revolves around creatively applying vanilla weapon, bow, crossbow, and trident enchantment effects to food and thrown potions. Food enchantments trigger on the eater when consumed, while potion enchantments trigger on targets upon impact. All damage formulas reference MC 1.20.4 vanilla enchantment mechanics for balance and consistency.

---

## I. Food Enchantments

Food enchantments apply to all edible items and are also compatible with cake blocks (cakes use a dedicated block enchantment storage system).

### 1.1 Sharpness

- **Effect**: Deals damage to the eater upon consumption
- **Formula**: `damage = 0.5 × level + 0.5`
  - Sharpness I = 1.0, II = 1.5, III = 2.0, IV = 2.5, V = 3.0
- **Custom damage source**: Unique death message ("XXX had their throat cut by sharp food")
- **Cake support**: Reads sharpness level from CakeEnchantmentStorage

### 1.2 Knockback

- **Effect**: Applies knockback to the eater upon consumption
- **Direction**: Random direction (360° random angle)
- **Strength formula**:
  - Horizontal: `strength = 0.5 × level`
  - Vertical: Small upward boost if on ground (max 0.4)
- **Reference**: Adapted from MC 1.20.4 `PlayerEntity.attack()` knockback logic

### 1.3 Fire Aspect

- **Effect**: Ignites the eater upon consumption
- **Duration**: `level × 4` seconds
  - Fire Aspect I = 4s, II = 8s
- **Reference**: Identical to MC 1.20.4 vanilla logic

### 1.4 Efficiency

- **Effect**: Speeds up eating
- **Formula**: Each level reduces eating time by 10%
  - Efficiency I = 10% faster, V = 50% faster
- **Implementation**: Mixin into `Item.getMaxUseTime()`

### 1.5 Frost Walker

- **Effect**: Grants Frost Walker effect after eating
- **Duration**: Base 20s + 10s per level
  - Frost Walker I = 30s, II = 40s, III = 50s
- **Ice formation**: Water beneath the player freezes during the effect
- **Continuous damage**: 1 frost damage every 4s, interval reduced by 0.5s per level
- **Visual**: Blue frozen health bar style

### 1.6 Unbreaking

- **Effect**: Chance to not consume food after eating
- **Formula**: Consumption probability = `1 / (level + 1)`
  - Unbreaking I = 50% no consumption, II = 66.7%, III = 75%
- **Cake handling**: Restores one cake bite when triggered; restores last-bite state if fully eaten

---

## II. Potion Enchantments — Weapon/Bow

Potion enchantments trigger when throwable potions hit. Full effect on direct hit, distance-attenuated within splash range (4 blocks).

### 2.1 Sharpness

- **Effect**: Thrown potion deals damage on hit
- **Formula**: `damage = 0.5 × level + 0.5` (same as food)
- **Splash**: Distance-attenuated (`damage × (1 - distance/4)`)
- **Stacking**: Adds to Power enchantment damage
- **Death message**: "XXX was shattered by a sharp potion"

### 2.2 Power

- **Effect**: Thrown potion deals damage on hit
- **Formula**: `damage = level + 1` (referencing bow Power)
  - Power I = 2.0, II = 3.0, III = 4.0, IV = 5.0, V = 6.0
- **Stacking**: Adds directly to Sharpness damage
- **Death message**: "XXX was obliterated by a powerful potion" (when Power ≥ Sharpness)

### 2.3 Punch

- **Effect**: Applies knockback on hit
- **Formula**: `knockbackStrength = punchLevel × 0.6`
- **Direction**:
  - Direct hit: Along potion flight direction
  - Splash: From impact point toward entity, strength attenuated by distance

### 2.4 Flame

- **Effect**: Ignites targets on hit
- **Direct hit**: 5 seconds of fire (100 ticks)
- **Splash**: Fire duration attenuated by distance
- **Level**: Only level I

### 2.5 Channeling

- **Effect**: AOE lightning adapted from trident Channeling
- **Conditions**:
  1. Thunderstorm weather required
  2. Target must have sky visibility
- **Range**: All living entities within 4 blocks of impact
- **Result**: Lightning bolt at each qualifying entity's position
- **Level**: Only level I

---

## III. Potion Enchantments — Crossbow

### 3.1 Multishot

- **Effect**: Single throw produces multiple potions in cone spread
- **Count**: Total projectiles = `2 + level`
  - Multishot I = 3, II = 4, III = 5
- **Distribution**:
  - Primary: Straight along aim direction
  - First 8 extras: Evenly on 10° cone circumference
  - Beyond 8: Random within cone (sqrt for uniform area)
- **Consumption**: Only 1 potion consumed
- **Compatibility**: Works with all Swift Throw modes

### 3.2 Quick Charge

- **Effect**: Reduces Infinity cooldown time
- **Formula**: -20% per level (base 30s)
  - Lv I = 24s, II = 18s, III = 12s, IV = 6s, V = 0s
- **Prerequisite**: Requires Infinity enchantment

### 3.3 Piercing

- **Effect**: Potion passes through entities
- **Mechanics**:
  - Passes through `level` entities (hits `level + 1` total)
  - Applies enchantment effects to each pierced entity
  - No duplicate hits on same entity
  - Destroys on block hit or pierce limit reached

---

## IV. Potion Enchantments — Trident

### 4.1 Loyalty

- **Effect**: Potion returns to thrower after 2 seconds
- **Return speed**: `0.5 × level` blocks/tick
  - Lv I = 10 b/s, II = 20 b/s, III = 30 b/s
- **Collision**: Consumed if hitting entity/block during flight or return
- **Success**: Returns potion item to player inventory

---

## V. Universal Potion Enchantments

### 5.1 Infinity

- **Effect**: No potion consumption on throw
- **Cooldown**:
  - Custom system based on NBT tag (`InfinityMarked`)
  - Only affects marked items, not identical unmarked ones
  - Base cooldown: 30s (reduced by Quick Charge)
- **With Unbreaking**: Pass → no cooldown, Fail → enter cooldown
- **Without Unbreaking**: Always enters cooldown
- **Client**: Gray semi-transparent cooldown overlay via network sync

### 5.2 Unbreaking

- **Effect**: Chance to not consume potion
- **Formula**: Same as food (consumption = `1/(level+1)`)
- **With Infinity**: Determines whether cooldown triggers

---

## VI. Custom Enchantments

### 6.1 Swift Throw

- **Effect**: Increases throw speed and flattens trajectory
- **Speed**: `base(0.5) × (1 + 0.5 × level)`
- **Angle**: Upward offset = `80 / (4 + level)` degrees (0 when < 1°)
- **Physics mode (Lv 1–20)**: Normal throw with gravity
- **Raycast mode (Lv >20)**: Instant hit via per-tick raytrace; invisible entity, no gravity; crit particles every 0.5 blocks (max 64)
- **Compatibility**: Works with Multishot

---

## VII. Utilities

### 7.1 Entity Health HUD

- Shows target HP when crosshair aims at entity
- Configurable detection distance (default 128 blocks)
- Toggle via debug menu

### 7.2 Item HUD

- Shows main hand item name and count (top-left)
- **Advanced mode**: Shows durability and full NBT data
- Toggle via debug menu

### 7.3 Debug Menu

- Custom keybind (unbound by default)
- Sub-menus: health display, item display, log toggles (9 modules)

### 7.4 Config

- File: `config/hello-mod.json`
- Auto-save on change, auto-load on startup
- Human-readable JSON

---

## VIII. Architecture

### 8.1 Tech Stack

| Component | Technology |
|-----------|-----------|
| Framework | Fabric Loader 0.15.3 + Fabric API 0.97.1 |
| Game | Minecraft 1.20.4 |
| Java | 17 |
| Build | Gradle + Fabric Loom 1.7 |
| Injection | Mixin |
| Mappings | Yarn 1.20.4+build.3:v2 |

### 8.2 Mixin Targets

| Mixin | Target | Purpose |
|-------|--------|---------|
| PlayerEatFoodMixin | `PlayerEntity.eatFood()` | Food enchant triggers |
| CakeBlockMixin | `CakeBlock.tryEat()` | Cake enchant triggers |
| CakePlaceMixin | `CakeBlock.onPlaced()` | Store enchant on placement |
| EfficientEatingMixin | `Item.getMaxUseTime()` | Efficiency speed boost |
| UnbreakingFoodMixin | `PlayerEntity.eatFood()` RETURN | Food durability check |
| PotionItemMixin | `ThrowablePotionItem.use()` | Potion throw logic |
| PotionEntityMixin | `PotionEntity.onCollision()` | Potion hit effects |
| SwiftThrowTickMixin | `PotionEntity.tick()` | Swift Throw raycast |
| PiercingPotionMixin | `PotionEntity` collision | Piercing logic |
| LoyaltyPotionMixin | `PotionEntity.tick()` | Loyalty return |
| LoyaltyCollisionMixin | `PotionEntity` collision | Loyalty collision |
| LivingEntityDamageCooldownMixin | `LivingEntity` | I-frame adjustment |

### 8.3 Package Structure

```
com.example.hellomod
├── HelloMod.java              # Main entrypoint
├── block/                     # Cake storage, block entities
├── client/                    # HUDs, screens, client sync
├── config/                    # Persistent configuration
├── damage/                    # Custom damage sources
├── debug/                     # Debug log control
├── effect/                    # Custom effects (Frost Walker)
├── enchantment/               # Enchantment system + cooldown
└── mixin/                     # All Mixin classes
```

### 8.4 Design Decisions

1. **Cake handling**: Cakes are blocks, not items. Uses `CakeEnchantmentStorage` to bind enchantment data to coordinates — written on placement, cleared when eaten.

2. **Potion data transfer**: Enchantments stored in potion ItemStack NBT, passed to projectile entity via `setItem(stack)`, read on impact via `EnchantmentHelper.getLevel()`.

3. **Infinity cooldown**: Custom UUID-based manager + NBT tag instead of vanilla `ItemCooldownManager` (which affects all items of same type).

4. **Swift Throw raycast**: High levels (>20) cause wall-clipping with physics speed. Uses per-tick raytrace teleportation for correct collision.

---

## IX. Enchantment Summary

| Enchantment | Food | Potion | Max Level | Notes |
|-------------|:----:|:------:|:---------:|-------|
| Sharpness | ✅ | ✅ | V | Damages eater / target |
| Knockback | ✅ | — | II | Random direction |
| Fire Aspect | ✅ | — | II | 4s per level |
| Efficiency | ✅ | — | V | -10% eat time per level |
| Frost Walker | ✅ | — | III | Ice + frost damage |
| Unbreaking | ✅ | ✅ | III | Chance no consumption |
| Power | — | ✅ | V | Stacks with Sharpness |
| Punch | — | ✅ | II | Flight-direction knockback |
| Flame | — | ✅ | I | 5s fire |
| Infinity | — | ✅ | I | No consumption + cooldown |
| Channeling | — | ✅ | I | Thunderstorm AOE lightning |
| Multishot | — | ✅ | III+ | Cone spread |
| Quick Charge | — | ✅ | V | Reduces Infinity cooldown |
| Piercing | — | ✅ | IV | Passes through entities |
| Loyalty | — | ✅ | III | Returns after 2s |
| Swift Throw | — | ✅ | ∞ | Custom enchantment |

---

## X. Requirements

- **Minecraft**: 1.20.4
- **Fabric Loader**: ≥ 0.15.0
- **Fabric API**: Any compatible version
- **Java**: ≥ 17
- **License**: MIT
