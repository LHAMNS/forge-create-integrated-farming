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

package plus.dragons.createintegratedfarming.common.ranching.roost;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class RoostBlockItem extends BlockItem {
    public RoostBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        var capturable = RoostCapturable.REGISTRY.get(target.getType());
        return capturable == null ? InteractionResult.PASS : capturable.captureItem(target.level(), stack, hand, player, target);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null)
            return super.useOn(context);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SpawnerBlockEntity spawnerBE))
            return super.useOn(context);

        // Extract entity types from spawner via NBT since fields are not accessible in Forge 1.20.1
        CompoundTag spawnerTag = spawnerBE.saveWithoutMetadata();
        // Try SpawnPotentials first (list of weighted spawn entries)
        if (spawnerTag.contains("SpawnPotentials")) {
            ListTag potentials = spawnerTag.getList("SpawnPotentials", 10);
            for (int i = 0; i < potentials.size(); i++) {
                CompoundTag entry = potentials.getCompound(i);
                CompoundTag data = entry.getCompound("data");
                CompoundTag entityTag = data.getCompound("entity");
                Optional<EntityType<?>> optional = EntityType.by(entityTag);
                if (optional.isEmpty())
                    continue;
                var capturable = RoostCapturable.REGISTRY.get(optional.get());
                if (capturable == null)
                    continue;
                var entity = optional.get().create(level);
                if (entity == null)
                    continue;
                return capturable.captureItem(level, context.getItemInHand(), context.getHand(), player, entity);
            }
        }
        // Fallback to SpawnData (current spawn entry)
        if (spawnerTag.contains("SpawnData")) {
            CompoundTag spawnData = spawnerTag.getCompound("SpawnData");
            CompoundTag entityTag = spawnData.getCompound("entity");
            Optional<EntityType<?>> optional = EntityType.by(entityTag);
            if (optional.isPresent()) {
                var capturable = RoostCapturable.REGISTRY.get(optional.get());
                if (capturable != null) {
                    var entity = optional.get().create(level);
                    if (entity != null) {
                        return capturable.captureItem(level, context.getItemInHand(), context.getHand(), player, entity);
                    }
                }
            }
        }
        return super.useOn(context);
    }
}
