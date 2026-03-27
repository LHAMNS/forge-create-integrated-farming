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

package plus.dragons.createintegratedfarming.api.event;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired after {@link net.minecraft.world.level.block.Block#getDrops} is called during
 * {@link plus.dragons.createintegratedfarming.api.harvester.CustomHarvestBehaviour#harvestBlock},
 * allowing other mods to modify the drops list and experience amount before items are collected
 * by the harvester contraption.
 * <p>
 * This is the Forge 1.20.1 equivalent of NeoForge's {@code BlockDropsEvent}, scoped to
 * CIF's custom harvester logic. Cancelling this event prevents both drops and experience
 * from being produced.
 * <p>
 * Listeners can:
 * <ul>
 *   <li>Modify the {@link #getDrops() drops} list (add, remove, or replace items)</li>
 *   <li>Change the {@link #getExperience() experience} amount</li>
 *   <li>{@link #setCanceled Cancel} the event to suppress all drops and experience</li>
 * </ul>
 */
@Cancelable
public class HarvestDropsModifyEvent extends Event {
    private final ServerLevel level;
    private final BlockPos pos;
    private final BlockState state;
    @Nullable
    private final BlockEntity blockEntity;
    @Nullable
    private final Player player;
    private final ItemStack tool;
    private final List<ItemStack> drops;
    private int experience;

    /**
     * @param tool the tool used for harvesting. A defensive copy is stored internally;
     *             callers of {@link #getTool()} receive a fresh copy each time to prevent mutation.
     */
    public HarvestDropsModifyEvent(ServerLevel level, BlockPos pos, BlockState state,
                                   @Nullable BlockEntity blockEntity, @Nullable Player player,
                                   ItemStack tool, List<ItemStack> drops, int experience) {
        this.level = level;
        this.pos = pos.immutable();
        this.state = state;
        this.blockEntity = blockEntity;
        this.player = player;
        this.tool = tool; // Store original reference; getTool() returns a copy on demand
        this.drops = drops;
        this.experience = Math.max(0, experience);
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }

    @Nullable
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns a defensive copy of the tool state captured when the event was posted.
     */
    public ItemStack getTool() {
        return tool.copy();
    }

    /**
     * Returns a mutable list of drops. Listeners may add, remove, or replace entries.
     */
    public List<ItemStack> getDrops() {
        return drops;
    }

    public int getExperience() {
        return experience;
    }

    /**
     * Replaces the final experience payout, clamped to zero or above.
     */
    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
    }
}
