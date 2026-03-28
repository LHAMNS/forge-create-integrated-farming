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

package plus.dragons.createintegratedfarming.integration.mynethersdelight.farming.harvest;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.soytutta.mynethersdelight.common.block.PowderyCaneBlock;
import com.soytutta.mynethersdelight.common.block.PowderyCannonBlock;
import com.soytutta.mynethersdelight.common.registry.MNDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class PowderyCannonArmInteractionPoint extends ArmInteractionPoint {
    // Upstream bug fix: cache the random count between simulate and real extraction.
    // Without caching, simulate might return 2 peppers but real extraction returns 1,
    // causing the arm to get stuck with a leftover pepper it can't place anywhere.
    private int cachedExtractCount = -1;

    public PowderyCannonArmInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
        super(type, level, pos, state);
    }

    @Override
    public Mode getMode() {
        return Mode.TAKE;
    }

    @Override
    public ItemStack extract(int slot, int amount, boolean simulate) {
        BlockState state = level.getBlockState(pos);
        if ((state.getBlock() instanceof PowderyCaneBlock || state.getBlock() instanceof PowderyCannonBlock) && state.getValue(BlockStateProperties.LIT)) {
            // Determine count: use cached value if available, otherwise roll new random
            int j;
            if (cachedExtractCount > 0) {
                j = cachedExtractCount;
            } else {
                j = 1 + level.random.nextInt(2);
            }

            if (simulate) {
                // Cache for the real extraction that follows
                cachedExtractCount = j;
            } else {
                // Reset cache and perform the actual block state change
                cachedExtractCount = -1;
                state = state.setValue(BlockStateProperties.LIT, false);
                level.setBlockAndUpdate(pos, state);
            }
            return new ItemStack(MNDItems.BULLET_PEPPER.get(), j);
        }
        cachedExtractCount = -1;
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotCount() {
        return 1;
    }

    public static class Type extends ArmInteractionPointType {
        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return state.getBlock() instanceof PowderyCaneBlock || state.getBlock() instanceof PowderyCannonBlock;
        }

        @Nullable
        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new PowderyCannonArmInteractionPoint(this, level, pos, state);
        }
    }
}
