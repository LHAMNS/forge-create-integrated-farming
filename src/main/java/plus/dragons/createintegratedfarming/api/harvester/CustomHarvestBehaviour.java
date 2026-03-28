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

package plus.dragons.createintegratedfarming.api.harvester;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import plus.dragons.createdragonsplus.util.CodeReference;
import plus.dragons.createintegratedfarming.api.event.HarvestDropsModifyEvent;
import plus.dragons.createintegratedfarming.integration.ModIntegration;

/**
 * CustomHarvestBehaviour provides full control for a block's interaction with {@link HarvesterMovementBehaviour}.
 * <p>
 * For specifically disable the harvest behaviour, add the block to {@link AllTags.AllBlockTags#NON_HARVESTABLE} instead.
 */
@FunctionalInterface
public interface CustomHarvestBehaviour {
    SimpleRegistry<Block, CustomHarvestBehaviour> REGISTRY = SimpleRegistry.create();
    @Internal
    LoadingCache<ListTag, ItemStack> ENCHANTMENTS_DECODER_CACHE = CacheBuilder.newBuilder()
            .maximumSize(64)
            .concurrencyLevel(1)
            .build(new CacheLoader<>() {
                @Override
                public ItemStack load(ListTag key) {
                    ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
                    tool.getOrCreateTag().put("Enchantments", key.copy());
                    EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(tool), tool);
                    return tool;
                }
            });

    /**
     * Harvest the block and collect the drops.
     *
     * @param behaviour the actual {@link HarvesterMovementBehaviour} of the harvester,
     *                  usually used for calling {@link MovementBehaviour#dropItem(MovementContext, ItemStack)}.
     * @param context   the {@link MovementContext} of the harvester.
     * @param pos       the {@link BlockPos} of the block to be harvested.
     * @param state     the {@link BlockState} of the block to be harvested.
     */
    void harvest(HarvesterMovementBehaviour behaviour, MovementContext context, BlockPos pos, BlockState state);

    /**
     * Shortcut for {@code harvesterReplants} config.
     */
    static boolean replant() {
        return AllConfigs.server().kinetics.harvesterReplants.get();
    }

    /**
     * Shortcut for {@code harvestPartiallyGrown} config.
     */
    static boolean partial() {
        return AllConfigs.server().kinetics.harvestPartiallyGrown.get();
    }

    /**
     * Helper method for retrieving the correct harvest tool item from the context.
     * <p>
     * Use this method for keeping consistency with {@link ModIntegration#CREATE_ENCHANTABLE_MACHINERY}.
     */
    static ItemStack getHarvestTool(MovementContext context) {
        return getHarvestTool(context, ItemStack.EMPTY);
    }

    /**
     * Helper method for retrieving the correct harvest tool item from the context.
     * <p>
     * Use this method for keeping consistency with {@link ModIntegration#CREATE_ENCHANTABLE_MACHINERY}.
     */
    static ItemStack getHarvestTool(MovementContext context, ItemStack original) {
        if (ModIntegration.CREATE_ENCHANTABLE_MACHINERY.enabled()) {
            if (original.isEmpty())
                original = new ItemStack(Items.NETHERITE_PICKAXE);
            CompoundTag blockEntityData = context.blockEntityData;
            if (blockEntityData == null || !blockEntityData.contains("Enchantments"))
                return original;
            ListTag enchTag = blockEntityData.getList("Enchantments", 10);
            if (enchTag.isEmpty())
                return original;
            ItemStack cached = ENCHANTMENTS_DECODER_CACHE.getUnchecked(enchTag);
            // Copy to avoid mutating the caller's ItemStack
            ItemStack result = original.copy();
            EnchantmentHelper.setEnchantments(EnchantmentHelper.getEnchantments(cached), result);
            return result;
        }
        return original;
    }

    /**
     * Helper method for harvest a block and transform it into a specific state.
     * <p>
     * Derived from {@link BlockHelper#destroyBlockAs(Level, BlockPos, Player, ItemStack, float, Consumer)},
     * supports specifying the result state, useful for harvesting high crops.
     * <p>
     * <b>Platform note:</b> Upstream NeoForge uses {@code BlockDropsEvent} to allow mods to modify
     * drops and experience. On Forge 1.20.1, we fire {@link HarvestDropsModifyEvent} after
     * {@link Block#getDrops} to provide equivalent extensibility for other CIF integration mods.
     */
    @CodeReference(value = BlockHelper.class, targets = "destroyBlockAs", source = "create", license = "mit")
    static void harvestBlock(Level level, BlockPos pos, BlockState newState, @Nullable Player player, ItemStack usedTool, float effectChance, Consumer<ItemStack> droppedItemCallback) {
        BlockState state = level.getBlockState(pos);

        if (level.random.nextFloat() < effectChance)
            level.levelEvent(2001, pos, Block.getId(state));
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;

        int expToDrop = 0;
        if (player != null) {
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled())
                return;
            expToDrop = event.getExpToDrop();

            usedTool.mineBlock(level, state, pos, player);
            player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
        }

        if (level instanceof ServerLevel serverLevel &&
                level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) &&
                !level.restoringBlockSnapshots &&
                (player == null || !player.isCreative())) {
            List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, serverLevel, pos, blockEntity, player, usedTool));
            HarvestDropsModifyEvent dropEvent = new HarvestDropsModifyEvent(
                    serverLevel, pos, state, blockEntity, player, usedTool, drops, expToDrop);
            MinecraftForge.EVENT_BUS.post(dropEvent);
            if (!dropEvent.isCanceled()) {
                for (ItemStack itemStack : dropEvent.getDrops())
                    droppedItemCallback.accept(itemStack);
                if (dropEvent.getExperience() > 0)
                    state.getBlock().popExperience(serverLevel, pos, dropEvent.getExperience());
                state.spawnAfterBreak(serverLevel, pos, usedTool, true);
            }
        }

        level.setBlockAndUpdate(pos, newState);
    }
}
