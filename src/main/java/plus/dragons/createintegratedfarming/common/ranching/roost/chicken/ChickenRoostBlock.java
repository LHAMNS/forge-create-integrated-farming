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

import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import plus.dragons.createintegratedfarming.common.ranching.roost.RoostBlock;
import plus.dragons.createintegratedfarming.common.ranching.roost.RoostCapturable;
import plus.dragons.createintegratedfarming.common.registry.CIFBlockEntities;

public class ChickenRoostBlock extends RoostBlock implements IBE<ChickenRoostBlockEntity>, RoostCapturable {
    protected final Supplier<? extends Block> empty;

    public ChickenRoostBlock(Properties properties, Supplier<? extends Block> empty) {
        super(properties);
        this.empty = empty;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Upstream bug fix: spectators should not interact with roost blocks
        if (player.isSpectator()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        // First check leashed entity capture from parent RoostBlock
        InteractionResult leashResult = super.use(state, level, pos, player, hand, hitResult);
        if (leashResult.consumesAction())
            return leashResult;

        // Lead extraction: use lead to extract the chicken
        if (stack.is(Items.LEAD)) {
            // Upstream bug fix: prevent client-side execution creating ghost entities
            if (level.isClientSide) return InteractionResult.SUCCESS;
            // Upstream bug fix: re-validate block state to prevent race condition duplication
            BlockState currentState = level.getBlockState(pos);
            if (!currentState.is(this)) return InteractionResult.PASS;
            Chicken chicken = new Chicken(EntityType.CHICKEN, level);
            chicken.setPos(pos.getCenter());
            chicken.setLeashedTo(player, true);
            level.addFreshEntity(chicken);
            level.setBlockAndUpdate(pos, empty.get().withPropertiesOf(state));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Try feeding with item in hand
        if (!stack.isEmpty()) {
            return onBlockEntityUse(level, pos, coop -> {
                if (coop.feedItem(stack, false)) {
                    if (!player.getAbilities().instabuild)
                        stack.shrink(1);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
                return InteractionResult.PASS;
            });
        }

        // Empty hand: extract items
        return onBlockEntityUse(level, pos, coop -> {
            for (int slot = 0; slot < coop.outputHandler.getSlots(); slot++) {
                ItemStack extracted = coop.outputHandler.extractItem(slot, 64, false);
                if (!extracted.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(extracted);
                    level.playSound(
                            player, pos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS,
                            1.0F, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            return InteractionResult.PASS;
        });
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        super.updateEntityAfterFallOn(level, entity);
        if (!(entity instanceof ItemEntity itemEntity))
            return;
        if (!entity.isAlive())
            return;
        if (entity.level().isClientSide)
            return;

        DirectBeltInputBehaviour inputBehaviour = BlockEntityBehaviour.get(level, entity.blockPosition(), DirectBeltInputBehaviour.TYPE);
        if (inputBehaviour == null)
            return;
        ItemStack remainder = inputBehaviour.handleInsertion(itemEntity.getItem(), Direction.UP, false);
        itemEntity.setItem(remainder);
        if (remainder.isEmpty())
            itemEntity.discard();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return ItemHelper.calcRedstoneFromBlockEntity(this, level, pos);
    }

    @Override
    public Class<ChickenRoostBlockEntity> getBlockEntityClass() {
        return ChickenRoostBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ChickenRoostBlockEntity> getBlockEntityType() {
        return CIFBlockEntities.CHICKEN_ROOST.get();
    }

    @Override
    public InteractionResult captureBlock(Level level, BlockState state, BlockPos pos, ItemStack stack, Player player, Entity entity) {
        // Prevent capturing into an already-occupied roost (this block IS the occupied variant)
        if (state.is(this))
            return InteractionResult.PASS;
        if (entity instanceof Chicken chicken && !chicken.isBaby()) {
            // Upstream bug fix: prevent client-side execution of capture logic
            if (level.isClientSide) return InteractionResult.SUCCESS;
            level.setBlockAndUpdate(pos, this.withPropertiesOf(state));
            chicken.playSound(SoundEvents.CHICKEN_HURT);
            chicken.discard();
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult captureItem(Level level, ItemStack stack, InteractionHand hand, Player player, Entity entity) {
        if (entity instanceof Chicken chicken && !chicken.isBaby()) {
            if (player.getAbilities().instabuild)
                player.getInventory().placeItemBackInInventory(new ItemStack(this));
            else {
                if (stack.getCount() == 1) player.setItemInHand(hand, new ItemStack(this));
                else {
                    player.getInventory().placeItemBackInInventory(new ItemStack(this));
                    stack.shrink(1);
                }
            }
            chicken.playSound(SoundEvents.CHICKEN_HURT);
            chicken.discard();
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        return InteractionResult.PASS;
    }
}
