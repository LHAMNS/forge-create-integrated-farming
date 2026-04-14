# Forge-Create: Integrated Farming

A Forge 1.20.1 port of [Create: Integrated Farming](https://github.com/DragonsPlusMinecraft/CreateIntegratedFarming), originally developed for NeoForge 1.21.1 by **DragonsPlusMinecraft**.

## Acknowledgements

Thanks to the following contributors for feedback and suggestions:

- **[@PhyX-Meow](https://github.com/phyxmeow)** - Feature request discussion on Central Kitchen compatibility ([#1](https://github.com/LHAMNS/forge-create-integrated-farming/issues/1))

This mod adds farming, fishing, and ranching automation to [Create](https://www.curseforge.com/minecraft/mc-mods/create), with deep integration for Farmer's Delight and other food mods.

## Features

### Fishing
- **Fishing Net** - Attach to contraptions, moves through water to automatically catch fish
- **Lava Fishing Net** - Catches lava creatures (requires Nether Depths Upgrade)
- **Biome-based catches** - Different loot tables for rivers, oceans, swamps, and the Nether
- **Enchantment support** - Lure reduces wait time, Luck of the Sea improves catches

### Ranching
- **Roost** - Empty housing block for capturing animals via lead
- **Chicken Roost** - Automated egg production with configurable inventory
- **Duck/Goose Roost** - 9 variants matching Untitled Duck Mod breeds
- **Belt/Hopper compatible** - Auto-extract products, auto-feed via belt input

### Farmer's Delight Integration
- **Mechanical Arm** - Works with FD Basket, Cooking Pot, Cutting Board
- **Spout Composting** - Pour water on Organic Compost to accelerate composting
- **Blaze Stove** - Blaze Burner directly heats FD Cooking Pot
- **Enhanced Harvester** - Harvests FD crops (tomatoes, rice, mushroom colonies)

### Data-Driven Chicken Foods
- JSON-based chicken food system in `data/<namespace>/create_integrated_farming/chicken_food/`
- Tag-based fallback via `#minecraft:chicken_food`
- Supports custom progress, cooldown (IntProvider), and item conversion

## Requirements

| Dependency | Version |
|------------|---------|
| Minecraft  | 1.20.1  |
| Forge      | 47.2.0+ |
| Create     | 6.0.8+  |
| Create: Dragons Plus | 1.0.0+ |

## Optional Compatibility

| Mod | Features |
|-----|----------|
| [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight) | Arm interaction, harvester, spout, stove, recipes |
| [Nether Depths Upgrade](https://www.curseforge.com/minecraft/mc-mods/nether-depths-upgrade) | Lava Fishing Net |
| [Untitled Duck Mod](https://www.curseforge.com/minecraft/mc-mods/untitled-duck-mod) | Duck/Goose roost variants |
| [My Nether's Delight](https://www.curseforge.com/minecraft/mc-mods/my-nethers-delight) | Arm interaction, spout, Ponder |
| [Crabber's Delight](https://www.curseforge.com/minecraft/mc-mods/crabbers-delight) | Crab trap arm interaction |
| [Corn Delight](https://www.curseforge.com/minecraft/mc-mods/corn-delight) | High crop harvesting |
| [Delight O' Flight](https://www.curseforge.com/minecraft/mc-mods/delight-o-flight) | Cloudshroom colony harvesting |
| [Create: Enchantable Machinery](https://www.curseforge.com/minecraft/mc-mods/create-enchantable-machinery) | Enchanted harvester/saw support |

## Installation

1. Install Forge 1.20.1, Create 6.0.8+, and Create: Dragons Plus 1.0.0+
2. Place `forge-create-integrated-farming-1.0.0-mc1.20.1-all.jar` in your `mods/` folder
3. (Optional) Install any of the compatible mods listed above
4. Launch the game

## License

This project is licensed under **LGPL-3.0-or-later**.

- Original code: Copyright (C) 2025 DragonsPlusMinecraft
- Ported to Forge 1.20.1 under the same license
- Original project: https://github.com/DragonsPlusMinecraft/CreateIntegratedFarming

## Bug Fixes Over Upstream

This port includes fixes for several bugs present in the original NeoForge version:

- Fixed ChickenFoodItem/ChickenFoodFluid `getCooldown()` returning progress instead of cooldown
- Fixed UntitledAnimalRoostBlockEntity `feedItem()` not consuming food items
- Fixed roost product extraction only reading slot 0 (now iterates all slots)
- Fixed occupied roost swallowing leashed adult animals
- Fixed `using_converts_to` remainder item only working once (shared instance mutation)
