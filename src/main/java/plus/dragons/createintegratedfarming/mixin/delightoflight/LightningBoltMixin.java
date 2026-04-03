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

package plus.dragons.createintegratedfarming.mixin.delightoflight;

import com.cloudmeow.delightoflight.registry.DFBlocks;
import me.fallenbreath.conditionalmixin.api.annotation.Condition;
import me.fallenbreath.conditionalmixin.api.annotation.Restriction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import plus.dragons.createintegratedfarming.integration.ModIntegration;

@Restriction(require = @Condition(ModIntegration.Mods.DELIGHT_O_FLIGHT))
@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin extends Entity {
    public LightningBoltMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    protected abstract BlockPos getStrikePosition();

    @Inject(method = "powerLightningRod", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LightningRodBlock;onLightningStrike(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", shift = At.Shift.AFTER))
    private void createintegratedfarming$onLightningStrike(CallbackInfo ci) {
        BlockPos strikePos = this.getStrikePosition();
        Level world = this.level;
        BlockState rodState = world.getBlockState(strikePos);
        BlockPos basePos;
        if (rodState.hasProperty(DirectionalBlock.FACING)) {
            Direction rodFacing = rodState.getValue(DirectionalBlock.FACING);
            basePos = strikePos.relative(rodFacing.getOpposite());
        } else {
            // Fallback: assume vertical rod
            basePos = strikePos.below();
        }
        BlockPos[] roll = { basePos, basePos.east(), basePos.west(), basePos.north(), basePos.south() };
        for (var pos : roll) {
            var state = world.getBlockState(pos);
            if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) {
                world.setBlockAndUpdate(pos, DFBlocks.WEATHER_SOIL.get().defaultBlockState());
            } else if (state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND)) {
                world.setBlockAndUpdate(pos, DFBlocks.WEATHER_SOIL_FARMLAND.get().defaultBlockState());
            }
        }
    }
}
