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

package plus.dragons.createintegratedfarming.integration.corndelight.farming.harvest;

import cn.mcmod_mmf.mmlib.block.HighCropBlock;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import plus.dragons.createintegratedfarming.api.harvester.CustomHarvestBehaviour;

public class HighCropHarvestBehaviour implements CustomHarvestBehaviour {
    private final HighCropBlock crop;

    protected HighCropHarvestBehaviour(HighCropBlock crop) {
        this.crop = crop;
    }

    public static @Nullable HighCropHarvestBehaviour create(Block block) {
        if (block instanceof HighCropBlock crop)
            return new HighCropHarvestBehaviour(crop);
        return null;
    }

    @Override
    public void harvest(HarvesterMovementBehaviour behaviour, MovementContext context, BlockPos pos, BlockState state) {
        Level level = context.world;
        boolean replant = CustomHarvestBehaviour.replant();
        int age = crop.getAge(state);
        if (age < crop.getMaxAge() && !CustomHarvestBehaviour.partial())
            return;
        ItemStack harvestTool = CustomHarvestBehaviour.getHarvestTool(context);
        if (replant) {
            int growUpperAge = crop.getGrowUpperAge();
            if (age < growUpperAge)
                return;
            // Upstream bug fix: clear upper parts before replanting lower part
            BlockPos above = pos.above();
            BlockState aboveState = level.getBlockState(above);
            while (aboveState.is(crop)) {
                BlockHelper.destroyBlockAs(level, above, null, harvestTool, 1,
                        stack -> behaviour.dropItem(context, stack));
                above = above.above();
                aboveState = level.getBlockState(above);
            }
            MutableBoolean seedSubtracted = new MutableBoolean(false);
            CustomHarvestBehaviour.harvestBlock(
                    level, pos,
                    // Upstream bug fix: UPPER should be reset to false after replanting
                    crop.getStateForAge(growUpperAge).setValue(HighCropBlock.UPPER, false),
                    null,
                    harvestTool,
                    1,
                    stack -> {
                        if (!seedSubtracted.getValue() && stack.is(crop.asItem())) {
                            stack.shrink(1);
                            seedSubtracted.setTrue();
                        }
                        behaviour.dropItem(context, stack);
                    });
        } else {
            destroy(level, behaviour, context, pos, state, harvestTool);
        }
    }

    /**
     * Destroys a high crop block and all connected parts above it.
     * <p>
     * Upstream bug fix: the original only checked upward when {@code UPPER=true},
     * meaning if the harvester hit the lower half, the upper half would be left
     * floating. This fix always checks upward regardless of the current block's
     * UPPER state, ensuring the entire plant is harvested as a unit.
     */
    protected void destroy(Level level, HarvesterMovementBehaviour behaviour, MovementContext context, BlockPos pos, BlockState state, ItemStack harvestTool) {
        // Always check and destroy any connected crop blocks above, regardless of
        // whether the current block is the upper or lower part.
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (aboveState.is(crop)) {
            destroy(level, behaviour, context, abovePos, aboveState, harvestTool);
        }
        BlockHelper.destroyBlockAs(
                level,
                pos,
                null,
                harvestTool,
                1,
                stack -> behaviour.dropItem(context, stack));
    }
}
