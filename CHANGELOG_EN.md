# Changelog

## v1.2.0 (relative to v1.1.1)

### Architecture Refactoring

#### Debug Menu Extracted into Standalone Mod
- Fully separated the debug menu, HUD overlays, and behavior logging system from the main mod into an independent **debug-menu** Mod
- The main mod no longer contains any UI screens (DebugMenuScreen, HealthHudSettingScreen, etc.), HUD rendering (EntityHealthHud, ItemCountHud), behavior log Mixins (5 total), or entity NBT network sync packets
- Added Debug Menu API: any mod can register debug toggles via `DebugMenuApi.register()`, managed and displayed by the debug-menu Mod
- The main mod detects the debug mod via `FabricLoader.isModLoaded("debug-menu")` and only registers toggles when present
- Without debug-menu installed, the main mod runs normally with all debug logging disabled and zero overhead

### Removed Files (from main Mod)

- `client/DebugMenuScreen.java` — Debug menu UI
- `client/DebugLogSettingScreen.java` — Log toggle settings UI
- `client/HealthHudSettingScreen.java` — Health HUD settings UI
- `client/ItemHudSettingScreen.java` — Item HUD settings UI
- `client/EntityHealthHud.java` — Entity health HUD renderer
- `config/ModConfig.java` — Old config management (split into standalone DebugLogConfig)
- `network/EntityNbtRequestC2SPacket.java` — Entity NBT request packet
- `network/EntityNbtResponseS2CPacket.java` — Entity NBT response packet
- `network/EntityNbtCache.java` — Client-side NBT cache
- `mixin/PlayerBehaviorLogMixin.java` — Player behavior log (server-side)
- `mixin/PlayerBlockInteractLogMixin.java` — Block interaction log (server-side)
- `mixin/client/BehaviorLogClientMixin.java` — Client screen log
- `mixin/client/BehaviorLogKeyboardMixin.java` — Client keyboard log
- `mixin/client/BehaviorLogMouseMixin.java` — Client mouse log

### New Files

- `DebugToggleRegistration.java` — Optional debug toggle registration (only active when debug-menu is present)
- `com/debugmenu/api/DebugMenuApi.java` — API stub
- `com/debugmenu/api/DebugToggleEntry.java` — API stub

### Configuration Changes

- Debug log toggles migrated from `config/hello-mod.json` to `config/better-enchanted-debug.json`
- HUD settings migrated to debug-menu Mod's `config/debug-menu.json`

### Size Optimization

- Main mod reduced by 14 Java source files and 5 Mixin injection points
- Estimated jar size reduction of ~30-40%

---

## v1.1.1 (relative to v1.1.0)

### New Features

#### Food / Potion Enchanting Table Support
- Food and potions can now be enchanted directly in the enchanting table (the table provides the appropriate list of available enchantments)
- Food and potions can now receive enchantments via enchanted books on the anvil
- Provides reasonable enchantability values for food/potions (plain food 10, potions 15, super/ultimate enchanted golden apple 22)
- Enchantable scope — Food: Sharpness/Knockback/Fire Aspect/Efficiency/Frost Walker/Unbreaking; Potions: Sharpness/Power/Punch/Flame/Infinity/Unbreaking/Multishot/Quick Charge/Piercing/Channeling/Loyalty/Swift Throw; Super/Ultimate Golden Apple supports all food + potion enchantments

### Enhancements

#### Player Behavior Log System
- Added real-time player behavior tracking log, recording attacks, interactions, damage taken, deaths, item drops, jumps, hotbar switches, sprint/sneak state changes, movement, item pickups, etc.
- Added block interaction log, recording place, break, and interact block events
- Added client-side behavior log (keyboard input, mouse clicks, client tick events)
- Debug menu now includes a "Player Behavior Log" toggle button (BehaviorLog toggle)
- Added `playerBehaviorLogEnabled` config option with persistent storage
- Added Chinese and English localization text

---

## v1.1.0 (relative to v1.0.0)

### New Items

#### Super Enchanted Golden Apple
- Added Super Enchanted Golden Apple item with dual-mode switching (eat mode / throw mode)
- Supports anvil enchanting system with stackable enchantment effects
- Anvil crafting bypasses the vanilla "Too Expensive" limitation
- Anvil enchantment penalty (RepairCost) locked at a fixed value of 10 to prevent cost escalation
- Supports potion effect storage: potions can be associated during crafting and released when eaten/thrown
- Item tooltip displays currently stored potion effect information

#### Ultimate Enchanted Golden Apple
- Added Ultimate Enchanted Golden Apple item as an advanced version of the Super Enchanted Golden Apple
- Unconditionally summons lightning when hitting hostile mobs on throw
- Custom damage type (bypasses armor and enchantment protection)
- Added Ultimate Golden Apple challenge advancement, triggered via advancement reward listener

### Enchantment Adjustments

#### Swift Throw Enchantment
- Enchanting table max level is 10, and can only be applied to books (must transfer to items via anvil)
- Anvil max level changed to 19 to prevent triggering raycast mode
- Fixed Swift Throw level >20 raycast mode pitchOffset issue, projectiles now accurately aim at crosshair

### Enhancements

#### Entity Health HUD
- Settings menu now includes a "Show Detailed Info" button
- When enabled, displays complete entity NBT info (0.5x scale, right-aligned)
- ActiveEffects (potion effects) displayed first below the health line (green highlight)
- Full NBT data (including potion effects) synced via server network packets
- Entities without health display `[-/-]`
- Held item NBT display scale unified to 0.5x

### Bug Fixes
- Fixed incorrect RepairCost lock value for super golden apple anvil operations
- Fixed Swift Throw high-level projectile offset not aligning with crosshair

### Other
- Added Super Enchanted Golden Apple design document (`SUPER_GOLDEN_APPLE_DESIGN.md`)
- Added Ultimate Enchanted Golden Apple design document (`ULTIMATE_GOLDEN_APPLE_DESIGN.md`)
- Updated README and DEV_PLAN with new feature sections and implementation details
