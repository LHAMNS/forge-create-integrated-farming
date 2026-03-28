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

package plus.dragons.createintegratedfarming.integration.mynethersdelight.registry;

import com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import plus.dragons.createintegratedfarming.integration.ModIntegration;

public class MNDBlockSpoutingBehaviors {
    private static final TagKey<Fluid> LETEOS_BOOSTER = TagKey.create(Registries.FLUID, new ResourceLocation("mynethersdelight", "leteos_booster"));

    public static void register() {
        BlockSpoutingBehaviour.BY_BLOCK.register(
                BuiltInRegistries.BLOCK.get(ModIntegration.MY_NETHERS_DELIGHT.asResource("letios_compost")),
                MNDBlockSpoutingBehaviors::fillLetiosCompost);
    }

    private static int fillLetiosCompost(Level level, BlockPos pos, SpoutBlockEntity spout, FluidStack fluid, boolean simulate) {
        if (!(fluid.getFluid().is(LETEOS_BOOSTER) || fluid.getFluid() == Fluids.LAVA) || !level.dimensionType().ultraWarm())
            return 0;
        if (!simulate && level instanceof ServerLevel) {
            BlockState state = level.getBlockState(pos);
            // Verify the block is still letios compost before triggering random tick
            if (!state.is(BuiltInRegistries.BLOCK.get(ModIntegration.MY_NETHERS_DELIGHT.asResource("letios_compost"))))
                return 0;
            state.randomTick((ServerLevel) level, pos, level.random);
        }
        return 250;
    }
}
