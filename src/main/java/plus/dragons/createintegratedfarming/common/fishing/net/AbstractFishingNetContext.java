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

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import plus.dragons.createintegratedfarming.config.CIFConfig;

public abstract class AbstractFishingNetContext<T extends FishingHook> {
    protected final FishingNetFakePlayer player;
    protected final ItemStack fishingRod;
    protected final T fishingHook;
    protected final RandomSource random;
    /** Cached config value: maximum number of recorded water/lava blocks for catch probability. Read once at construction. */
    protected final int maxRecordedBlocks;
    /** Cached config value: cooldown multiplier for fishing. Read once at construction. */
    protected final int cooldownMultiplier;
    /** Cached enchantment value: lure speed reduction in ticks (lureLevel * 100). Read once at construction. */
    private final int lureSpeed;
    /** Cached enchantment value: luck of the sea level. Read once at construction. */
    private final int luckLevel;
    private final LongOpenHashSet visitedBlocks;
    public int timeUntilCatch;

    public AbstractFishingNetContext(ServerLevel level, ItemStack fishingRod) {
        this.player = new FishingNetFakePlayer(level);
        this.fishingRod = fishingRod;
        this.fishingHook = createFishingHook(level);
        this.fishingHook.setOwner(player);
        this.random = fishingHook.random;
        // Cache config values once at construction to avoid per-tick config reads
        this.maxRecordedBlocks = CIFConfig.server().fishingNetMaxRecordedBlocks.get();
        this.cooldownMultiplier = CIFConfig.server().fishingNetCooldownMultiplier.get();
        // Cache enchantment-derived constants to avoid repeated EnchantmentHelper lookups
        int lureLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FISHING_SPEED, fishingRod);
        this.lureSpeed = lureLevel * 100;
        this.luckLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FISHING_LUCK, fishingRod);
        this.visitedBlocks = new LongOpenHashSet(Math.min(16, maxRecordedBlocks));
        this.reset(level);
    }

    protected abstract T createFishingHook(ServerLevel level);

    protected abstract boolean isPosValidForFishing(ServerLevel level, BlockPos pos);

    public abstract LootTable getLootTable(ServerLevel level, BlockPos pos);

    public FishingNetFakePlayer getPlayer() {
        return player;
    }

    public T getFishingHook() {
        return fishingHook;
    }

    public ItemStack getFishingRod() {
        return fishingRod;
    }

    public void reset(ServerLevel level) {
        this.visitedBlocks.clear();
        // Fix: clamp minimum cooldown to 20 ticks (1 second) to prevent negative values from high Lure levels
        this.timeUntilCatch = Math.max(20, (Mth.nextInt(random, 100, 600) - lureSpeed) * cooldownMultiplier);
    }

    public boolean visitNewPositon(ServerLevel level, BlockPos pos) {
        if (isPosValidForFishing(level, pos)) {
            if (visitedBlocks.size() < maxRecordedBlocks)
                visitedBlocks.add(pos.asLong());
            return true;
        }
        return false;
    }

    public LootParams buildCaptureLootContext(MovementContext context, ServerLevel level, LivingEntity entity) {
        player.setPos(context.position);
        return new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, context.position)
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
                .withParameter(LootContextParams.KILLER_ENTITY, player)
                .withParameter(LootContextParams.DIRECT_KILLER_ENTITY, fishingHook)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                .create(LootContextParamSets.ENTITY);
    }

    public LootParams buildFishingLootContext(MovementContext context, ServerLevel level, BlockPos pos) {
        fishingHook.setPos(context.position);
        player.setPos(context.position);
        return new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, context.position)
                .withParameter(LootContextParams.TOOL, fishingRod)
                .withParameter(LootContextParams.THIS_ENTITY, fishingHook)
                .withParameter(LootContextParams.KILLER_ENTITY, player)
                .withLuck((float) luckLevel)
                .create(LootContextParamSets.FISHING);
    }

    public boolean canCatch() {
        if (maxRecordedBlocks == 0)
            return true;
        return random.nextInt(maxRecordedBlocks) < visitedBlocks.size();
    }

    public void invalidate(ServerLevel level) {
        fishingHook.discard();
        player.discard();
    }
}
