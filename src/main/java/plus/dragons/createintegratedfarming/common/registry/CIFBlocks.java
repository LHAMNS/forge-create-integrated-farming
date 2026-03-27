/*
 * Copyright (C) 2025  DragonsPlus
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package plus.dragons.createintegratedfarming.common.registry;

import static plus.dragons.createintegratedfarming.common.CIFCommon.REGISTRATE;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.common.fishing.net.FishingNetBlock;
import plus.dragons.createintegratedfarming.common.fishing.net.FishingNetMovementBehaviour;
import plus.dragons.createintegratedfarming.common.ranching.roost.RoostBlock;
import plus.dragons.createintegratedfarming.common.ranching.roost.RoostBlockItem;
import plus.dragons.createintegratedfarming.common.ranching.roost.chicken.ChickenRoostBlock;

public class CIFBlocks {
    public static final BlockEntry<FishingNetBlock> FISHING_NET = REGISTRATE
            .block("fishing_net", FishingNetBlock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.mapColor(DyeColor.BROWN).sound(SoundType.SCAFFOLDING).noOcclusion())
            .transform(TagGen.axeOnly())
            .tag(AllBlockTags.WINDMILL_SAILS.tag)
            .blockstate(BlockStateGen.directionalBlockProvider(false))
            .onRegister(block -> MovementBehaviour.REGISTRY.register(block, new FishingNetMovementBehaviour()))
            .simpleItem()
            .register();

    @SuppressWarnings("unchecked")
    public static final BlockEntry<Block> LAVA_FISHING_NET = new BlockEntry<>(REGISTRATE,
            RegistryObject.create(CIFCommon.asResource("lava_fishing_net"), Registries.BLOCK, CIFCommon.ID));

    public static final BlockEntry<RoostBlock> ROOST = REGISTRATE
            .block("roost", RoostBlock::new)
            .properties(prop -> prop.strength(1.5F).sound(SoundType.BAMBOO_WOOD))
            .blockstate((ctx, prov) -> prov.horizontalBlock(ctx.get(), AssetLookup.standardModel(ctx, prov)))
            .transform(TagGen.axeOnly())
            .item(RoostBlockItem::new)
            .build()
            .register();

    public static final BlockEntry<ChickenRoostBlock> CHICKEN_ROOST = REGISTRATE
            .block("chicken_roost", prop -> new ChickenRoostBlock(prop, ROOST))
            .properties(prop -> prop.strength(1.5F).sound(SoundType.BAMBOO_WOOD))
            .blockstate((ctx, prov) -> prov.horizontalBlock(ctx.get(), AssetLookup.standardModel(ctx, prov)))
            .item()
            .build()
            .register();

    public static void register(IEventBus modBus) {
        // Force class loading
    }
}
