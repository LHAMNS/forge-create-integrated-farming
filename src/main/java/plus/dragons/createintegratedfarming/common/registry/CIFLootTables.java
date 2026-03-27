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

import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import plus.dragons.createintegratedfarming.common.CIFCommon;

public class CIFLootTables {
    public static final ResourceLocation CHICKEN_ROOST = CIFCommon.asResource("gameplay/roost/chicken");

    public static void generate(RegistrateLootTableProvider provider) {
        provider.addLootAction(LootContextParamSets.BLOCK, CIFLootTables::buildRoostProducts);
    }

    private static void buildRoostProducts(BiConsumer<ResourceLocation, LootTable.Builder> lootTables) {
        lootTables.accept(CHICKEN_ROOST, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .add(LootItem.lootTableItem(Items.EGG))));
    }
}
