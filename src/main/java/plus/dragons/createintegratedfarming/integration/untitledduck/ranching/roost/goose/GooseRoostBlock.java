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

package plus.dragons.createintegratedfarming.integration.untitledduck.ranching.roost.goose;

import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.untitledduckmod.common.entity.GooseEntity;
import net.untitledduckmod.common.init.ModEntityTypes;
import net.untitledduckmod.common.init.ModSoundEvents;
import plus.dragons.createintegratedfarming.common.ranching.roost.RoostBlock;
import plus.dragons.createintegratedfarming.common.ranching.roost.RoostCapturable;
import plus.dragons.createintegratedfarming.integration.untitledduck.registry.UntitledDuckBlockEntities;
import plus.dragons.createintegratedfarming.integration.untitledduck.registry.UntitledDuckBlocks;

public class GooseRoostBlock extends RoostBlock implements IBE<GooseRoostBlockEntity> {
    protected final Supplier<? extends Block> empty;
    protected final byte variant;

    public GooseRoostBlock(Properties properties, Supplier<? extends Block> empty, byte variant) {
        super(properties);
        this.empty = empty;
        this.variant = variant;
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

        // Lead extraction: use lead to extract the goose
        if (stack.is(Items.LEAD)) {
            // Upstream bug fix: prevent client-side execution creating ghost entities
            if (level.isClientSide) return InteractionResult.SUCCESS;
            // Upstream bug fix: re-validate block state to prevent race condition duplication
            BlockState currentState = level.getBlockState(pos);
            if (!currentState.is(this)) return InteractionResult.PASS;
            GooseEntity goose = gooseVariant(level);
            goose.setPos(pos.getCenter());
            goose.setLeashedTo(player, true);
            level.addFreshEntity(goose);
            level.setBlockAndUpdate(pos, empty.get().withPropertiesOf(state));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // Try feeding with item in hand
        if (!stack.isEmpty()) {
            return onBlockEntityUse(level, pos, coop -> {
                // Simulate on client to check if feeding is possible without modifying state
                if (coop.feedItem(stack, level.isClientSide)) {
                    if (!level.isClientSide && !player.getAbilities().instabuild)
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
                            player, pos, ModSoundEvents.GOOSE_LAY_EGG.get(), SoundSource.BLOCKS,
                            1.0F, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            return InteractionResult.PASS;
        });
    }

    private GooseEntity gooseVariant(Level level) {
        GooseEntity goose = new GooseEntity(ModEntityTypes.GOOSE.get(), level);
        if (variant < 2) {
            goose.setVariant(variant);
        } else {
            switch (variant) {
                case 2:
                    goose.setCustomName(Component.literal("ping"));
                    break;
                case 3:
                    goose.setCustomName(Component.literal("sus"));
                    break;
                default:
                    goose.setCustomName(Component.literal("untitled"));
            }
        }
        return goose;
    }

    private static Block blockVariant(GooseEntity goose) {
        if (goose.getCustomName() != null) {
            if (goose.getCustomName().getString().equals("ping")) {
                return UntitledDuckBlocks.GOOSE_ROOST_PING.get();
            } else if (goose.getCustomName().getString().equals("sus")) {
                return UntitledDuckBlocks.GOOSE_ROOST_SUS.get();
            } else if (goose.getCustomName().getString().equals("untitled")) {
                return UntitledDuckBlocks.GOOSE_ROOST_UNTITLED.get();
            }
        }
        // Upstream bug fix: original used case 0b100 (=4), but GooseEntity.setVariant(1)
        // stores variant=1 for Canadian goose. 0b100 never matches, causing Canadian
        // geese to be misidentified as normal on round-trip (extract + recapture).
        return switch (goose.getVariant()) {
            case 1 -> UntitledDuckBlocks.GOOSE_ROOST_CANADIAN.get();
            default -> UntitledDuckBlocks.GOOSE_ROOST_NORMAL.get();
        };
    }

    public static BlockState withVariantPropertiesOf(BlockState state, GooseEntity goose) {
        BlockState blockstate = blockVariant(goose).defaultBlockState();

        for (Property<?> property : state.getBlock().getStateDefinition().getProperties()) {
            if (blockstate.hasProperty(property)) {
                blockstate = copyProperty(state, blockstate, property);
            }
        }

        return blockstate;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState sourceState, BlockState targetState, Property<T> property) {
        return targetState.setValue(property, sourceState.getValue(property));
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
        if (!isMoving)
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
    public Class<GooseRoostBlockEntity> getBlockEntityClass() {
        return GooseRoostBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GooseRoostBlockEntity> getBlockEntityType() {
        return UntitledDuckBlockEntities.GOOSE_ROOST.get();
    }

    public static class Capturable implements RoostCapturable {
        @Override
        public InteractionResult captureBlock(Level level, BlockState state, BlockPos pos, ItemStack stack, Player player, Entity entity) {
            // Prevent capturing into already-occupied goose roost
            if (state.getBlock() instanceof GooseRoostBlock)
                return InteractionResult.PASS;
            if (entity instanceof GooseEntity goose && !goose.isBaby()) {
                // Upstream bug fix: prevent client-side execution of capture logic
                if (level.isClientSide) return InteractionResult.SUCCESS;
                level.setBlockAndUpdate(pos, withVariantPropertiesOf(state, goose));
                goose.playSound(ModSoundEvents.GOOSE_HONK.get());
                goose.discard();
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }

        @Override
        public InteractionResult captureItem(Level level, ItemStack stack, InteractionHand hand, Player player, Entity entity) {
            if (entity instanceof GooseEntity goose && !goose.isBaby()) {
                // Prevent client-side inventory manipulation to avoid desync in multiplayer
                if (level.isClientSide) return InteractionResult.SUCCESS;
                if (player.getAbilities().instabuild)
                    player.getInventory().placeItemBackInInventory(new ItemStack(blockVariant(goose).asItem()));
                else {
                    if (stack.getCount() == 1) player.setItemInHand(hand, new ItemStack(blockVariant(goose).asItem()));
                    else {
                        player.getInventory().placeItemBackInInventory(new ItemStack(blockVariant(goose).asItem()));
                        stack.shrink(1);
                    }
                }
                goose.playSound(ModSoundEvents.GOOSE_HONK.get());
                goose.discard();
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }
    }
}
