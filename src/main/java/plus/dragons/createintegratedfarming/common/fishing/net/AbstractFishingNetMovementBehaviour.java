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

package plus.dragons.createintegratedfarming.common.fishing.net;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import plus.dragons.createintegratedfarming.config.CIFConfig;

public abstract class AbstractFishingNetMovementBehaviour<T extends AbstractFishingNetContext<?>> implements MovementBehaviour {
    /** Cached entity type test to avoid allocation per scan cycle. */
    private static final EntityTypeTest<net.minecraft.world.entity.Entity, LivingEntity> LIVING_ENTITY_TEST = EntityTypeTest.forClass(LivingEntity.class);
    /** Maximum number of entities captured per scan cycle to prevent lag spikes in dense areas. */
    private static final int MAX_CAPTURES_PER_SCAN = 3;

    protected abstract T getFishingNetContext(MovementContext context, ServerLevel level);

    /** Cached max size for entity capture, set before each entity scan in tick(). */
    protected float cachedMaxSize;

    protected boolean canCaptureEntity(LivingEntity entity) {
        if (entity instanceof Enemy)
            return false;
        if (entity.isBaby())
            return false;
        if (entity instanceof WaterAnimal) {
            var dimensions = entity.getDimensions(Pose.SWIMMING);
            return dimensions.height < cachedMaxSize && dimensions.width < cachedMaxSize;
        }
        return false;
    }

    protected void onCaptureEntity(MovementContext context, ServerLevel level, T fishing, LivingEntity entity) {
        if (!entity.isBaby() && level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            if (entity instanceof Mob mob) {
                var lootTable = level.getServer().getLootData().getLootTable(mob.getLootTable());
                var lootParams = fishing.buildCaptureLootContext(context, level, entity);
                lootTable.getRandomItems(lootParams, entity.getLootTableSeed(), item -> dropItem(context, item));
            }
            if (CIFConfig.server().fishingNetCapturedCreatureDropExpNugget.get()) {
                int experience = 0;
                if (entity instanceof Mob mob) {
                    experience = ForgeEventFactory.getExperienceDrop(entity, fishing.getPlayer(), mob.getExperienceReward());
                }
                int nuggetCount = (experience + 2) / 3;
                int maxStackSize = AllItems.EXP_NUGGET.get().getMaxStackSize();
                nuggetCount = Math.min(nuggetCount, maxStackSize);
                if (nuggetCount > 0) {
                    dropItem(context, new ItemStack(AllItems.EXP_NUGGET.get(), nuggetCount));
                }
            }
        }
        entity.discard();
    }

    @Override
    public void tick(MovementContext context) {
        if (context.world instanceof ServerLevel level) {
            var fishing = getFishingNetContext(context, level);
            if (fishing.timeUntilCatch > 0)
                fishing.timeUntilCatch--;
            if ((level.getGameTime() + context.hashCode()) % 20 == 0 && CIFConfig.server().fishingNetCaptureCreatureInWater.get()) {
                this.cachedMaxSize = (float) CIFConfig.server().fishingNetCapturedCreatureMaxSize.get().doubleValue();
                VoxelShape shape = context.state.getShape(level, context.localPos);
                if (shape.isEmpty()) return;
                AABB area = shape.bounds()
                        .expandTowards(context.motion.scale(5))
                        .move(context.position)
                        .inflate(0.2);
                List<LivingEntity> candidates = level.getEntities(LIVING_ENTITY_TEST, area, this::canCaptureEntity);
                candidates.stream().limit(MAX_CAPTURES_PER_SCAN)
                        .forEach(entity -> this.onCaptureEntity(context, level, fishing, entity));
            }
        }
    }

    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        if (context.world instanceof ServerLevel level) {
            var fishing = getFishingNetContext(context, level);
            var isValid = fishing.visitNewPosition(level, pos);
            if (!isValid || fishing.timeUntilCatch > 0)
                return;
            if (fishing.canCatch()) {
                var params = fishing.buildFishingLootContext(context, level, pos);
                LootTable lootTable = fishing.getLootTable(level, pos);
                List<ItemStack> loots = lootTable.getRandomItems(params);
                var event = new ItemFishedEvent(loots, 0, fishing.getFishingHook());
                MinecraftForge.EVENT_BUS.post(event);
                if (!event.isCanceled()) {
                    loots.forEach(stack -> dropItem(context, stack));
                }
            }
            fishing.reset(level);
        }
    }

    @Override
    public void stopMoving(MovementContext context) {
        if (context.world instanceof ServerLevel level && context.temporaryData instanceof AbstractFishingNetContext<?> fishing) {
            fishing.invalidate(level);
            context.temporaryData = null;
        }
    }
}
