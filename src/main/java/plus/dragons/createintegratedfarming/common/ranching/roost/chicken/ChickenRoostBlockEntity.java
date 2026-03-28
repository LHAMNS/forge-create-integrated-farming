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

package plus.dragons.createintegratedfarming.common.ranching.roost.chicken;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import plus.dragons.createintegratedfarming.common.ranching.roost.AnimalRoostBlockEntity;
import plus.dragons.createintegratedfarming.common.registry.CIFChickenFoods;
import plus.dragons.createintegratedfarming.common.registry.CIFLootTables;

public class ChickenRoostBlockEntity extends AnimalRoostBlockEntity {
    @Override
    protected ResourceLocation productionLootTable() {
        return CIFLootTables.CHICKEN_ROOST;
    }

    public ChickenRoostBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean feedItem(ItemStack stack, boolean simulate) {
        assert level != null;
        if (feedCooldown > 0 || eggTime <= 0)
            return false;
        var food = CIFChickenFoods.getItemFood(stack);
        if (food == null)
            return false;
        if (simulate)
            return true;
        feed(food);
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        Vec3 feedPos = Vec3.atBottomCenterOf(worldPosition)
                .add(facing.getStepX() * .5f, 13 / 16f, facing.getStepZ() * .5f);
        Optional<ItemStack> convertsTo = food.usingConvertsTo();
        if (convertsTo.isPresent()) {
            Containers.dropItemStack(level, feedPos.x, feedPos.y, feedPos.z, convertsTo.get().copy());
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new ItemParticleOption(ParticleTypes.ITEM, stack),
                    feedPos.x, feedPos.y, feedPos.z,
                    1, 0, 0, 0, 0.0);
        } else {
            level.addParticle(
                    new ItemParticleOption(ParticleTypes.ITEM, stack),
                    feedPos.x, feedPos.y, feedPos.z,
                    0, 0, 0);
        }
        return true;
    }

    public int feedFluid(FluidStack fluid, boolean simulate) {
        if (feedCooldown > 0 || eggTime <= 0)
            return 0;
        var food = CIFChickenFoods.getFluidFood(fluid);
        if (food == null)
            return 0;
        // Bug fix: check that the fluid actually has enough amount before consuming
        if (fluid.getAmount() < food.amount()) return 0;
        if (simulate)
            return food.amount();
        feed(food);
        return food.amount();
    }

    public void feed(ChickenFood food) {
        assert level != null;
        eggTime = Math.max(0, eggTime - food.getProgress(level.random));
        feedCooldown = food.getCooldown(level.random);
        level.playSound(
                null, worldPosition, SoundEvents.CHICKEN_AMBIENT, SoundSource.BLOCKS,
                1.0F, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
        notifyUpdate();
    }
}
