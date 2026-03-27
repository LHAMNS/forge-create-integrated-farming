# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0-mc1.20.1] - 2026-03-28

### Added
- Complete port of Create: Integrated Farming from NeoForge 1.21.1 to Forge 1.20.1
- Fishing Net with contraption-based automated fishing
- Lava Fishing Net (Nether Depths Upgrade integration)
- Roost / Chicken Roost with automated egg production
- Duck Roost (4 variants) and Goose Roost (5 variants) via Untitled Duck Mod
- Farmer's Delight deep integration (arm, harvester, spout, stove)
- My Nether's Delight integration (arm, spout, Ponder)
- Crabber's Delight integration (crab trap arm interaction)
- Corn Delight integration (high crop harvesting)
- Delight O' Flight integration (cloudshroom harvesting)
- Create: Enchantable Machinery integration (enchanted harvester/saw)
- JSON-based chicken food data system with tag fallback
- Forge condition support in chicken food JSON files
- Full Ponder scenes for all features (9 NBT storyboards)
- Complete Chinese (zh_cn) localization
- Config system with per-feature toggles and range constraints

### Fixed (upstream bugs)
- `ChickenFoodItem.getCooldown()` now correctly returns cooldown (was returning progress)
- `ChickenFoodFluid.getCooldown()` same fix as above
- `UntitledAnimalRoostBlockEntity.feedItem()` now returns true on success (was always false)
- Roost product extraction now iterates all inventory slots (was only slot 0)
- Occupied roosts now reject leashed animals instead of swallowing them
- `using_converts_to` remainder now uses `.copy()` to prevent shared instance mutation

### Performance Improvements
- Roost `notifyUpdate()` only fires on state transitions (not every tick)
- Fishing net entity scan staggered across 20 ticks via hashCode offset
- Config values cached as final fields in fishing context (no per-tick reads)
- Chicken food tag fallback caches both positive and negative results
- Tomato rope BlockState cached with lazy initialization

### Platform Adaptations
- NeoForge DataMapType -> JSON ReloadListener + HashMap registry
- NeoForge BlockDropsEvent -> custom HarvestDropsModifyEvent (Cancelable, mutable drops/xp)
- NeoForge WaterAndLavaLoggedBlock -> imported from Create: Dragons Plus
- NeoForge capability registration -> Forge getCapability/LazyOptional/AttachCapabilitiesEvent
- Forge condition processing in chicken food JSON files
- All Mixin targets verified with SRG names for runtime compatibility
- conditional-mixin embedded via JarJar (no separate installation needed)
