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

import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHandlerWrapper;
import com.simibubi.create.foundation.item.ItemHelper;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plus.dragons.createintegratedfarming.config.CIFConfig;

public abstract class AnimalRoostBlockEntity extends SmartBlockEntity {
    protected final ItemStackHandler inventory;
    public final IItemHandler outputHandler;
    protected LazyOptional<IItemHandler> outputCapability;
    protected int feedCooldown;
    protected int eggTime = productionCooldown();
    /**
     * Cached center position to avoid repeated Vec3 allocation in lazyTick.
     * Recomputed in lazyTick if the block entity has been moved (e.g., by piston).
     */
    protected Vec3 centerPos;
    /** Tracks worldPosition to detect block entity movement (piston, etc.). */
    private BlockPos lastKnownPos;

    public int productionCooldown() {
        return 12000;
    }

    protected abstract ResourceLocation productionLootTable();

    public AnimalRoostBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
        this.centerPos = Vec3.atCenterOf(pos);
        this.lastKnownPos = pos.immutable();
        this.inventory = new ItemStackHandler(CIFConfig.server().roostingInventorySlotCount.get()) {
            @Override
            public int getSlotLimit(int slot) {
                return CIFConfig.server().roostingInventorySlotSize.get();
            }
        };
        this.outputHandler = new ItemHandlerWrapper(inventory) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }
        };
        this.outputCapability = LazyOptional.of(() -> outputHandler);
    }

    public @Nullable IItemHandler getItemHandler(@Nullable Direction direction) {
        if (direction == getBlockState().getValue(HorizontalDirectionalBlock.FACING))
            return null;
        return outputHandler;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == getBlockState().getValue(HorizontalDirectionalBlock.FACING))
                return LazyOptional.empty().cast();
            return outputCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        outputCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        outputCapability = LazyOptional.of(() -> outputHandler);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new DirectBeltInputBehaviour(this)
                .onlyInsertWhen(side -> side == getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite())
                .considerOccupiedWhen(side -> feedCooldown > 0)
                .setInsertionHandler(this::tryInsertFrom));
    }

    @Override
    public void initialize() {
        assert level != null;
        super.initialize();
        if (eggTime >= productionCooldown()) {
            eggTime = productionCooldown() / 2 + level.random.nextInt(productionCooldown() / 2);
        }
    }

    @Override
    public void lazyTick() {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        // Detect if block entity was moved (e.g., by piston) and refresh cached position
        if (!worldPosition.equals(lastKnownPos)) {
            centerPos = Vec3.atCenterOf(worldPosition);
            lastKnownPos = worldPosition.immutable();
        }
        boolean changed = false;
        if (feedCooldown > 0) {
            int prev = feedCooldown;
            feedCooldown = Math.max(0, feedCooldown - lazyTickRate);
            if (prev > 0 && feedCooldown == 0)
                changed = true;
        }
        if (eggTime > 0) {
            int prev = eggTime;
            eggTime = Math.max(0, eggTime - lazyTickRate);
            if (prev > 0 && eggTime == 0)
                changed = true;
        }
        if (eggTime <= 0) {
            // Performance: cheap capacity check before rolling loot table
            boolean hasSpace = false;
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (inventory.getStackInSlot(i).getCount() < inventory.getSlotLimit(i)) {
                    hasSpace = true;
                    break;
                }
            }
            if (!hasSpace) {
                eggTime = 200; // Retry in 10 seconds
                changed = true;
            } else {
                boolean inserted = false;
                ResourceLocation lootTableId = productionLootTable();
                LootTable lootTable = serverLevel.getServer().getLootData().getLootTable(lootTableId);
                LootParams lootParams = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.BLOCK_STATE, getBlockState())
                        .withParameter(LootContextParams.ORIGIN, centerPos)
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, this)
                        .create(LootContextParamSets.BLOCK);
                var lootStacks = lootTable.getRandomItems(lootParams);
                for (var stack : lootStacks) {
                    ItemStack remainder = ItemHandlerHelper.insertItem(inventory, stack, false);
                    inserted |= stack.getCount() != remainder.getCount();
                    // Drop any remainder that couldn't fit in the inventory
                    if (!remainder.isEmpty()) {
                        Block.popResource(serverLevel, worldPosition, remainder);
                    }
                }
                if (inserted) {
                    int cooldown = productionCooldown();
                    eggTime = cooldown / 2 + level.random.nextInt(cooldown / 2);
                    level.playSound(
                            null, worldPosition, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS,
                            1.0F, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
                    changed = true;
                } else {
                    // Inventory full after loot roll: retry in 10 seconds
                    eggTime = 200;
                    changed = true;
                }
            }
        }
        if (changed)
            notifyUpdate();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("EggLayTime", eggTime);
        tag.putInt("FeedCooldown", feedCooldown);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        // Bug fix: use productionCooldown() instead of hardcoded 12000
        eggTime = Mth.clamp(tag.getInt("EggLayTime"), 0, productionCooldown());
        feedCooldown = tag.getInt("FeedCooldown");
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, inventory);
    }

    protected ItemStack tryInsertFrom(TransportedItemStack transported, Direction side, boolean simulate) {
        assert level != null;
        ItemStack stack = transported.stack.copy();
        if (feedItem(stack, simulate)) {
            stack.shrink(1);
        }
        return stack;
    }

    public abstract boolean feedItem(ItemStack stack, boolean simulate);
}
