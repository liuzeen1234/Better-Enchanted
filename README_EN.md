# Better Enchanted

> Fabric Mod | Minecraft 1.20.1 | Java 17  
> Bring vanilla enchantment effects to food and potions — let them have enchantment abilities too.

---

## Introduction

**Better Enchanted** is a Minecraft Fabric mod whose core gameplay revolves around creatively applying vanilla weapon, bow, crossbow, and trident enchantment effects to food and thrown potions. Food enchantments trigger on the eater when consumed, while potion enchantments trigger on targets upon impact. All damage formulas reference MC 1.20.4 vanilla enchantment mechanics as closely as possible for balance and consistency.

---

## 0. Enchantment Acquisition Mechanism

By default, the vanilla enchanting table and anvil reject food and potions. This mod wires up the entire enchantment pipeline via Mixins so food/potions can be enchanted like weapons:

- **Enchanting table**:
  - `FoodPotionEnchantableMixin` makes `Item.isEnchantable()` return `true` for food/potions and provides reasonable enchantability values (plain food 10, potions 15, super/ultimate golden apple 22).
  - `EnchantingTableMixin` injects `EnchantmentHelper.getPossibleEntries()` to return the mod's custom list of available enchantments for food/potions.
- **Anvil (enchanted books)**:
  - `EnchantmentAcceptItemMixin` injects `Enchantment.isAcceptableItem()` so matching enchanted books can be applied to food/potions.
- **Enchantable scope**:
  - Food: Sharpness, Knockback, Fire Aspect, Efficiency, Frost Walker, Unbreaking
  - Potions: Sharpness, Power, Punch, Flame, Infinity, Unbreaking, Multishot, Quick Charge, Piercing, Channeling, Loyalty, Swift Throw
  - Super/Ultimate Enchanted Golden Apple: supports the full food + potion enchantment set

---

## I. Food Enchantments

Food enchantments apply to all edible items and are also compatible with cake blocks (cakes use a dedicated block enchantment storage system).

### 1.1 Sharpness

- **Effect**: Deals damage to the eater upon consumption
- **Formula**: `damage = 0.5 × level + 0.5`
  - Sharpness I = 1.0 damage, II = 1.5, III = 2.0, IV = 2.5, V = 3.0
- **Custom damage source**: Has a unique death message ("XXX had their throat cut by sharp food")
- **Cake support**: Reads sharpness level from CakeEnchantmentStorage at the cake's position

### 1.2 Knockback

- **Effect**: Applies knockback to the eater upon consumption
- **Direction**: Random direction (360° random angle), simulating being "flung" by the food
- **Strength formula**:
  - Horizontal: `strength = 0.5 × level`
  - Vertical: If on ground, gives a small upward boost (max 0.4)
- **Reference**: Adapted from MC 1.20.4 `PlayerEntity.attack()` knockback logic

### 1.3 Fire Aspect

- **Effect**: Ignites the eater upon consumption
- **Duration**: `level × 4` seconds
  - Fire Aspect I = 4s, Fire Aspect II = 8s
- **Reference**: Identical to MC 1.20.4 vanilla Fire Aspect logic (`target.setOnFireFor(level * 4)`)

### 1.4 Efficiency

- **Effect**: Speeds up eating
- **Formula**: Each level reduces eating time by 10%
  - Efficiency I = 10% faster, Efficiency V = 50% faster
- **Implementation**: Mixin into `Item.getMaxUseTime()` to reduce eating tick count

### 1.5 Frost Walker

- **Effect**: Grants Frost Walker effect after eating
- **Duration**: Base 20 seconds + 10 seconds per level
  - Frost Walker I = 30s, II = 40s, III = 50s
- **Ice formation**: Water beneath the player freezes into frosted ice during the effect
- **Continuous damage**: Takes 1 frost damage every 4 seconds, with interval reduced by 0.5s per level
  - Frost Walker I = every 3.5s, II = every 3s
- **Visual effect**: Health bar displays in blue frozen style
- **Configurable**: Frost Walker logging can be toggled via debug menu

### 1.6 Unbreaking

- **Effect**: Chance to not consume food after eating
- **Probability formula**: References MC 1.20.4 vanilla Unbreaking formula
  - Consumption probability = `1 / (level + 1)`
  - Non-consumption probability = `level / (level + 1)`
  - Unbreaking I = 50% no consumption, II = 66.7%, III = 75%
- **Cake special handling**: When triggered, restores one cake bite (BITES - 1); if the cake was just fully eaten, restores it to the last-bite state

---

## II. Potion Enchantments — Weapon/Bow Category

Potion enchantments trigger when throwable potions (splash/lingering) are thrown and hit something. All damage/effects apply at full strength to directly hit entities, and with distance-based falloff to entities within splash range (4 blocks).

### 2.1 Sharpness [Potion]

- **Effect**: Thrown potion deals damage to hit entities
- **Formula**: `damage = 0.5 × level + 0.5` (same as food Sharpness)
- **Splash**: Distance-attenuated damage within 4 blocks (`damage × (1 - distance/4)`)
- **Stacking**: Stacks additively with Power enchantment damage
- **Custom damage source**: Unique death message ("XXX was shattered by a sharp potion")

### 2.2 Power [Potion]

- **Effect**: Thrown potion deals damage to hit entities
- **Formula**: References MC 1.20.4 bow Power enchantment
  - `damage = level + 1` (simulating arrow base=2 Power bonus)
  - Power I = 2.0, II = 3.0, III = 4.0, IV = 5.0, V = 6.0
- **Stacking**: Adds directly to Sharpness damage
- **Damage source**: When Power damage ≥ Sharpness damage, uses Power-specific death message ("XXX was obliterated by a powerful potion")

### 2.3 Punch [Potion]

- **Effect**: Thrown potion applies knockback to hit entities
- **Formula**: References MC 1.20.4 bow Punch enchantment (`AbstractArrowEntity.onHit`)
  - `knockbackStrength = punchLevel × 0.6`
- **Direction**:
  - Direct hit: Along potion flight direction (normalized horizontal components)
  - Splash range: From impact point toward entity, strength attenuated by distance

### 2.4 Flame [Potion]

- **Effect**: Thrown potion ignites targets on hit
- **Direct hit**: 5 seconds of fire (100 ticks), matching MC 1.20.4 bow Flame rules
- **Splash range**: Fire duration attenuated by distance (`ceil(5 × (1 - distance/4))` seconds)
- **Level**: Only level I (matching vanilla)

### 2.5 Channeling [Potion]

- **Effect**: MC 1.20.4 trident Channeling rules adapted as AOE version
- **Conditions**:
  1. Must be thunderstorm weather (`world.isThundering()`)
  2. Target entity position must have sky visibility (`isSkyVisible`)
- **Range**: All living entities within 4 blocks of potion impact point
- **Result**: Each qualifying entity gets a lightning bolt summoned at its position
- **Lightning attribution**: Lightning channeler set to the thrower (affects drops and XP)
- **Level**: Only level I (matching vanilla)

---

## III. Potion Enchantments — Crossbow Category

### 3.1 Multishot [Potion]

- **Effect**: Single throw produces multiple potions in a cone spread
- **Count formula**: Total projectiles = `2 + level`
  - Multishot I = 3, II = 4, III = 5
- **Distribution rules**:
  - First potion (primary) flies straight along aim direction (cone center)
  - First 8 extra projectiles distribute evenly on a 10° cone circumference
  - Beyond 8 extras: randomly distributed within the cone (0°–10°, using sqrt for area-uniform distribution)
- **Consumption**: Only consumes 1 potion
- **Compatibility**: Fully compatible with all Swift Throw modes (physics/raycast)

### 3.2 Quick Charge [Potion]

- **Effect**: Reduces Infinity enchantment cooldown time
- **Formula**: Each level reduces cooldown by 20% (base 30 seconds)
  - Lv I = 24s, Lv II = 18s, Lv III = 12s, Lv IV = 6s, Lv V = 0s (no cooldown)
- **Prerequisite**: Must be used with Infinity enchantment; has no effect alone

### 3.3 Piercing [Potion]

- **Effect**: References MC 1.20.4 crossbow Piercing rules
- **Mechanics**:
  - Potion can pass through `level` entities (hitting `level + 1` total)
  - Applies enchantment effects (Sharpness/Power/Punch/Flame) to each pierced entity
  - Already-pierced entities won't be hit again
  - Destroys normally and triggers splash effects when hitting a block or reaching pierce limit

---

## IV. Potion Enchantments — Trident Category

### 4.1 Loyalty [Potion]

- **Effect**: Adapted from MC 1.20.4 trident Loyalty rules
- **Mechanics**:
  - Potion automatically returns to thrower 2 seconds after being thrown
  - Return speed = `0.5 × level` blocks/tick
    - Lv I = 10 blocks/s, Lv II = 20 blocks/s, Lv III = 30 blocks/s
  - If it hits an entity or block during flight/return, it's consumed normally (won't return)
  - Successful return gives the potion item back to the player
- **Consumption logic**: Potion is consumed immediately on throw (entity itself carries return logic)

---

## V. Universal Potion Enchantments

### 5.1 Infinity [Potion]

- **Effect**: Throwing potion doesn't consume it
- **Cooldown system**:
  - Custom cooldown system based on NBT tag (`InfinityMarked`)
  - Only affects items with the Infinity enchantment, not other identical items
  - Base cooldown: 30 seconds (reduced by Quick Charge)
- **Interaction with Unbreaking**:
  - With Unbreaking: perform durability check — pass → no cooldown, fail → enter cooldown
  - Without Unbreaking: always enters cooldown
- **Client display**: Gray semi-transparent cooldown overlay synced via networking
- **Cooldown blocking**: Cannot use the item again while on cooldown

### 5.2 Unbreaking [Potion]

- **Effect**: Chance to not consume potion after throwing
- **Formula**: Same as food Unbreaking (consumption probability = `1/(level+1)`)
- **Interaction with Infinity**: Check result determines whether cooldown triggers (see above)

---

## VI. Custom Enchantments

### 6.1 Swift Throw

- **Effect**: Increases potion throw speed and trajectory flatness
- **Max level at enchanting table**: 10 (books only; cannot be applied directly to potions/items)
- **Max legal level at anvil**: 19 (cannot reach the >20 raycast mode, preserving the rarity of the super golden apple's Swift Throw 25)
- **Speed formula**: `actual speed = base speed (0.5) × (1 + 0.5 × level)`
- **Angle formula**: Upward offset angle `y = 80 / (4 + level)` degrees
  - Higher levels = flatter trajectory
  - When offset angle < 1°, set to 0 (perfectly flat throw)
- **Two modes**:
  - **Level 1–20 (Physics mode)**: Normal physics throw with speed and direction calculated by formula, affected by gravity
  - **Level >20 (Raycast mode)**: Instant hit via per-tick raytrace teleportation; potion entity is invisible and gravity-free; crit particles spawn every 0.5 blocks along the firing direction (up to 64 blocks)
- **Compatibility**: Fully compatible with Multishot — extra projectiles also use the same Swift Throw mode

---

## VII. Super Enchanted Golden Apple

### 7.1 Item Overview

| Property | Description |
|----------|-------------|
| Item ID | `hello-mod:super_enchanted_golden_apple` |
| Text Color | Light purple (LIGHT_PURPLE) |
| Built-in Enchantment | Swift Throw 25 |
| Acquisition | 3×3 crafting table (Gold Blocks ×4 + Splash Potions ×2 + Lingering Potions ×2 + Golden Apple ×1) |

### 7.2 Mode Switching

- Left-click while held to switch between eat/throw mode, **5 game tick** cooldown
- Dynamic name: "Super Enchanted Golden Apple" in eat mode, "Throwable Super Enchanted Golden Apple" in throw mode

### 7.3 Eat Mode

- Eating time 32 ticks (1.6s), accelerated by Efficiency enchantment
- Eat effects = Base effects + Splash potion effects + Lingering potion effects (100% duration)
- **Base effects**: Regeneration V (30s) + Absorption IV (2min) + Resistance I (5min) + Fire Resistance I (5min)

### 7.4 Throw Mode

- Built-in Swift Throw 25 (raycast instant-hit)
- On impact: first triggers **splash effects** (base buffs + splash potion effects, distance-attenuated), then spawns **area effect cloud** (base buffs + lingering potion effects)
- Enchantment extra effects (Sharpness/Power/Punch/Flame/Channeling/Multishot) all work

### 7.5 Potion Effect Storage

- On crafting, reads potion effects from splash/lingering potions in recipe, merges by rules and stores in NBT
- Tooltip displays full splash effects and cloud effects info

### 7.6 Anvil Compatibility

- **Bypasses the "Too Expensive" 39-level cap**: The super enchanted golden apple ignores the vanilla 39-level experience cost limit during anvil operations, so it can always be enchanted
- RepairCost locked at 10 to prevent exponential penalty growth
- Additional enchantments can be applied via anvil (Sharpness/Power/Punch/Flame/Infinity/Unbreaking/Quick Charge/Channeling/Multishot/Piercing/Loyalty)

---

## VIII. Ultimate Enchanted Golden Apple

### 8.1 Item Overview

| Property | Description |
|----------|-------------|
| Item ID | `hello-mod:ultimate_enchanted_golden_apple` |
| Text Color | Bright gold (GOLD) |
| Built-in Enchantments | Efficiency 9, Unbreaking 10, Swift Throw 25, Infinity 1 |
| Acquisition | Custom advancement reward (Super Enchanted Golden Apple with all 14 valid enchantments at max level) |

### 8.2 Mode Switching

- Left-click while held to switch between eat/throw mode, **2 game tick** cooldown (faster than Super version)
- Dynamic name change: "Ultimate Enchanted Golden Apple" in eat mode, "Throwable Ultimate Enchanted Golden Apple" in throw mode

### 8.3 Eat Mode

- Efficiency 9 reduces eating time to ~3 ticks (nearly instant)
- **Fixed effects** (no potion NBT):

| Effect | Level | Duration |
|--------|-------|----------|
| Regeneration | V | 60s |
| Resistance | III | 60s |
| Strength | V | 60s |

- **Consumption**: Unbreaking 10 check passes (91% chance) → not consumed + 3s use cooldown; fails (9%) → consumed, no cooldown

### 8.4 Throw Mode

- Swift Throw 25 (>20) → raycast instant-hit mode
- **4-block radius detection on impact**:
  - Hostile mobs: 100 true damage (ignores armor) + unconditional lightning strike
  - Friendly mobs/Players: Regeneration V (60s)
- Enchantment extra effects (Sharpness/Power/Punch/Flame/Channeling) apply to all entities in range
- **Consumption**: Built-in Infinity + Unbreaking 10; no consumption on throw, enters 30s cooldown (reduced by Quick Charge); Unbreaking check pass = no cooldown

### 8.5 Anvil Compatibility

- Shares the anvil mechanism with the super version: **bypasses the "Too Expensive" 39-level cap**, RepairCost locked at 10
- Additional enchantments can be applied via anvil (same enchantment list as the super version)

### 8.6 Advancement Requirements

- All 14 valid enchantments on a Super Enchanted Golden Apple at their legal max level:
  - Swift Throw 25, Sharpness V, Power V, Punch II, Flame I, Channeling I, Multishot 10, Infinity I, Unbreaking III, Quick Charge III, Knockback II, Fire Aspect II, Efficiency V, Frost Walker II
- Trigger timing: When the player takes out a qualifying Super Enchanted Golden Apple from the anvil, `SuperAppleAnvilMixin` detects it and grants the advancement
- Once completed, `AdvancementRewardMixin` issues the reward: 1 Ultimate Enchanted Golden Apple (with 4 built-in enchantments) + 1000 experience levels
- If the player's inventory is full, the reward item drops on the ground

---

## IX. Utility Features

### 9.1 Entity Health HUD

- Displays target's current HP / max HP on screen when crosshair is aimed at an entity
- Detection distance is configurable (default 128 blocks)
- Can be independently toggled via debug menu

### 9.2 Held Item HUD

- Displays main hand item name and count in the top-left corner
- **Advanced mode**: Additionally shows item durability and full NBT tag data
- Can be independently toggled via debug menu

### 9.3 Debug Menu

- Opened via custom keybind (unbound by default, configurable in key settings)
- Provides sub-menus:
  - Entity health display settings (toggle + detection distance slider)
  - Held item display settings (toggle + advanced mode toggle)
  - Debug log toggles (independent control of 10 log modules: cake/place/food/potion/damage/swift throw/client/frost walker/infinity cooldown/player behavior)

### 9.4 Persistent Configuration

- Config file located at `config/hello-mod.json`
- All setting changes auto-save; auto-loads on startup
- Human-readable JSON format, supports manual editing

---

## X. Technical Architecture

### 10.1 Core Tech Stack

| Component | Technology |
|-----------|-----------|
| Mod Framework | Fabric Loader 0.15.11 + Fabric API 0.92.2 |
| Game Version | Minecraft 1.20.1 |
| Java Version | Java 17 |
| Build Tool | Gradle + Fabric Loom 1.7 |
| Code Modification | Mixin Injection |
| Mappings | Yarn 1.20.1+build.10:v2 |

### 10.2 Mixin Injection Points

| Mixin Class | Target | Purpose |
|-------------|--------|---------|
| FoodPotionEnchantableMixin | `Item.isEnchantable()` / `Item.getEnchantability()` | Make food/potions enchantable in the table and provide enchantability values |
| EnchantingTableMixin | `EnchantmentHelper.getPossibleEntries()` | Return custom enchantment list for food/potions in the enchanting table |
| EnchantmentAcceptItemMixin | `Enchantment.isAcceptableItem()` | Allow food/potions to be enchanted via enchanted books on the anvil |
| PlayerEatFoodMixin | `PlayerEntity.eatFood()` | Food enchantment triggers (Sharpness/Knockback/Fire/Frost) |
| CakeBlockMixin | `CakeBlock.tryEat()` | Cake enchantment triggers |
| CakePlaceMixin | `CakeBlock.onPlaced()` | Store enchantment data when placing cake |
| EfficientEatingMixin | `Item.getMaxUseTime()` | Efficiency eating speed boost |
| UnbreakingFoodMixin | `PlayerEntity.eatFood()` RETURN | Food Unbreaking check |
| PotionItemMixin | `ThrowablePotionItem.use()` | Potion throw logic (Infinity/Unbreaking/Multishot/consumption) |
| PotionEntityMixin | `PotionEntity.onCollision()` | Potion hit effects (Sharpness/Power/Punch/Flame/Channeling) |
| SwiftThrowTickMixin | `PotionEntity.tick()` | Swift Throw raycast mode |
| MultishotEnchantmentMixin | — | Multishot helper |
| PiercingPotionMixin | `PotionEntity` collision | Piercing logic |
| LoyaltyPotionMixin | `PotionEntity.tick()` | Loyalty return logic |
| LoyaltyCollisionMixin | `PotionEntity` collision | Loyalty return collision handling |
| LivingEntityDamageCooldownMixin | `LivingEntity.damage()` | Clears invincibility frames for potion/lightning damage |
| SuperAppleCraftingMixin | `CraftingScreenHandler.updateResult` | Add Swift Throw + potion data when crafting super golden apple |
| SuperAppleAnvilMixin | `AnvilScreenHandler.updateResult` | Bypass 39-level cap + lock RepairCost + advancement trigger |
| AdvancementRewardMixin | `PlayerAdvancementTracker.grantCriterion()` | Ultimate golden apple advancement reward |
| PlayerBehaviorLogMixin | `PlayerEntity` behavior | Player behavior logging (attack/interact/hurt/death/drop/jump, etc.) |
| PlayerBlockInteractLogMixin | Block interaction | Block place/break/interact logging |
| client/DrawContextCooldownMixin | `DrawContext.drawItemInSlot()` | Infinity cooldown overlay rendering |
| client/SwiftThrowRenderMixin | Client | Hide potion entity in raycast mode |
| client/SuperAppleAttackMixin | `MinecraftClient.doAttack()` | Golden apple left-click mode switch |
| client/BehaviorLogClientMixin | Client tick | Client behavior logging (tick events) |
| client/BehaviorLogKeyboardMixin | Client keyboard | Client behavior logging (keyboard input) |
| client/BehaviorLogMouseMixin | Client mouse | Client behavior logging (mouse clicks) |

### 10.3 Package Structure

```
com.example.hellomod
├── HelloMod.java                    # Main entrypoint, registers events and systems
├── advancement/                     # Advancement checks
│   └── UltimateAppleChecker.java    # Ultimate golden apple enchantment checker
├── block/                           # Cake enchantment storage, block entities
│   ├── CakeEnchantmentStorage.java  # In-memory cake enchantment data management
│   ├── EnchantedCakeBlockEntity.java
│   └── HelloModBlockEntities.java
├── client/                          # Client-side features
│   ├── HelloModClient.java          # Client entrypoint
│   ├── EmptyEntityRenderer.java     # Empty renderer (thrown entity invisible, avoids high-speed visual bug)
│   ├── EntityHealthHud.java         # Entity health HUD
│   ├── InfinityCooldownClientState.java # Infinity cooldown client sync
│   ├── DebugMenuScreen.java         # Debug menu UI
│   ├── DebugLogSettingScreen.java   # Log toggle UI
│   ├── HealthHudSettingScreen.java  # Health HUD settings UI
│   └── ItemHudSettingScreen.java    # Item HUD settings UI
├── config/                          # Configuration persistence
│   └── ModConfig.java
├── damage/                          # Custom damage sources
│   ├── ModDamageTypes.java
│   ├── SharpFoodDamageSource.java
│   ├── SharpPotionDamageSource.java
│   ├── PowerPotionDamageSource.java
│   └── UltimateAppleDamageSource.java
├── debug/                           # Debug log control
│   └── DebugLogConfig.java
├── effect/                          # Custom effects
│   └── FrostWalkerFoodEffect.java   # Frost Walker food effect (tick event)
├── enchantment/                     # Enchantment system
│   ├── ModEnchantments.java         # Custom enchantment registration
│   ├── SwiftThrowEnchantment.java   # Swift Throw enchantment definition
│   ├── InfinityCooldownManager.java # Infinity cooldown server-side management
│   └── InfinityCooldownSync.java    # Cooldown state network sync
├── entity/                          # Custom entities
│   ├── ModEntities.java             # Entity registration
│   ├── SuperGoldenAppleEntity.java  # Super golden apple thrown entity
│   └── UltimateGoldenAppleEntity.java # Ultimate golden apple thrown entity
├── item/                            # Custom items
│   ├── ModItems.java                # Item registration
│   ├── SuperEnchantedGoldenAppleItem.java  # Super enchanted golden apple
│   ├── UltimateEnchantedGoldenAppleItem.java # Ultimate enchanted golden apple
│   ├── SuperAppleCraftingHandler.java      # Crafting helper (legacy, compat)
│   └── SuperApplePotionMerger.java         # Potion effect merge helper
├── network/                         # Networking
│   ├── SuperAppleModeSwitchC2SPacket.java  # Golden apple mode switch sync
│   ├── EntityNbtRequestC2SPacket.java      # Entity NBT request (C2S)
│   ├── EntityNbtResponseS2CPacket.java     # Entity NBT response (S2C)
│   └── EntityNbtCache.java                 # Client NBT cache (500ms expiry)
└── mixin/                           # All Mixin injection classes
    ├── client/                      # Client-side Mixins
    └── ...                          # See table above
```

### 10.4 Key Design Decisions

1. **Cake special handling**: Cakes are blocks, not items, so ItemStack NBT can't be read directly. Uses `CakeEnchantmentStorage` to bind enchantment data to block coordinates — written on placement, cleared when fully eaten.

2. **Potion enchantment data transfer**: Enchantments are stored in the potion ItemStack's NBT. On throw, the complete ItemStack is passed to the projectile entity via `potionEntity.setItem(stack)`. On impact, `EnchantmentHelper.getLevel()` reads from the entity's stack.

3. **Infinity cooldown system**: Doesn't use vanilla `ItemCooldownManager` (which affects all items of the same type). Instead uses a custom cooldown manager keyed by player UUID + NBT tag (`InfinityMarked`), only affecting specifically marked items.

4. **Swift Throw raycast**: At high Swift Throw levels (>20), physics-based throw speed is too fast causing wall-clipping and collision detection failures. Switches to per-tick raytrace teleportation to ensure correct collision detection.

5. **Anvil bypasses "Too Expensive" cap**: The vanilla anvil refuses operations when the experience cost exceeds 39 levels. `SuperAppleAnvilMixin` bypasses this check by temporarily flagging the player as creative mode at the start of `updateResult`, then restoring the original state afterward, ensuring super/ultimate golden apples can always be worked on the anvil.

6. **Empty rendering for thrown entities**: The super/ultimate golden apple thrown entities use `EmptyEntityRenderer` (`shouldRender()` returns false), because at Swift Throw 25 raycast mode the entity moves extremely fast and rendering the texture produces visual artifacts. Players see a crit-particle trajectory instead of the flying item entity.

7. **Removing invincibility frames for potion/lightning damage**: `LivingEntityDamageCooldownMixin` inspects the damage source at the head of `damage()`. If it's `sharp_potion`, `power_potion`, or `lightning_bolt`, it resets `timeUntilRegen` and `hurtTime` to 0, ensuring multiple potion enchantment damages and Channeling damage stack correctly within the same tick.

8. **Multishot level cap modification**: `MultishotEnchantmentMixin` injects `Enchantment.getMaxLevel()` to raise the Multishot enchantment's vanilla max level from 1 to 10, supporting a larger spread of thrown projectiles.

9. **Entity NBT network sync**: The entity detail HUD needs to display the server's full NBT data (including potion effects). This is achieved through the cooperation of `EntityNbtRequestC2SPacket` (client request) → `EntityNbtResponseS2CPacket` (server response) → `EntityNbtCache` (client cache, expires after 500ms) for cross-side data sync.

---

## XI. Enchantment Overview Table

| Enchantment | Food | Potion | Max Level | Notes |
|-------------|:----:|:------:|:---------:|-------|
| Sharpness | ✅ | ✅ | V | Food damages eater, potion damages target |
| Knockback | ✅ | — | II | Random direction |
| Fire Aspect | ✅ | — | II | 4s per level |
| Efficiency | ✅ | — | V | -10% eating time per level |
| Frost Walker | ✅ | — | III | Ice + continuous damage |
| Unbreaking | ✅ | ✅ | III | Chance to not consume |
| Power | — | ✅ | V | Stacks with Sharpness |
| Punch | — | ✅ | II | Knockback along flight direction |
| Flame | — | ✅ | I | 5s fire |
| Infinity | — | ✅ | I | No consumption + cooldown |
| Channeling | — | ✅ | I | Thunderstorm AOE lightning |
| Multishot | — | ✅ | 10 | Cone spread (vanilla cap is 1, raised to 10 via Mixin) |
| Quick Charge | — | ✅ | V | Reduces Infinity cooldown |
| Piercing | — | ✅ | IV | Passes through multiple entities |
| Loyalty | — | ✅ | III | Returns after 2s |
| Swift Throw | — | ✅ | ∞ (legal max 19) | Custom enchantment; table max 10 (books only), anvil legal max 19 |

---

## XII. Runtime Environment & Dependencies

- **Minecraft**: 1.20.1
- **Fabric Loader**: ≥ 0.15.0
- **Fabric API**: Any compatible version
- **Java**: ≥ 17
- **License**: MIT
