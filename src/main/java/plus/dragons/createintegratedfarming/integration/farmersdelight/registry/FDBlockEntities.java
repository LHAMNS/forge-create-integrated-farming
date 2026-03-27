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

package plus.dragons.createintegratedfarming.integration.farmersdelight.registry;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plus.dragons.createdragonsplus.common.behaviours.BehaviourProvider;
import plus.dragons.createdragonsplus.common.registry.CDPCapabilities;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.integration.farmersdelight.logistics.basket.BasketBehaviourProvider;
import vectorwing.farmersdelight.common.block.entity.BasketBlockEntity;

/**
 * Attaches Forge capabilities to Farmer's Delight block entities.
 * <p>
 * In Forge 1.20.1, capabilities are attached via {@link AttachCapabilitiesEvent}
 * on the game event bus (not the mod bus), unlike NeoForge's {@code RegisterCapabilitiesEvent}.
 */
public class FDBlockEntities {
    private static final ResourceLocation BASKET_BEHAVIOUR_CAP = CIFCommon.asResource("basket_behaviour_provider");

    /**
     * Register the capability attachment listener on the Forge game event bus.
     * Called during mod construction when FD is loaded.
     */
    public static void register(IEventBus modBus) {
        // Forge capabilities attach via the game event bus, but we register
        // the listener here so it's called at the right time.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addGenericListener(
                BlockEntity.class, FDBlockEntities::onAttachCapabilities);
    }

    private static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof BasketBlockEntity basket) {
            event.addCapability(BASKET_BEHAVIOUR_CAP, new BasketBehaviourCapProvider(basket));
        }
    }

    private static class BasketBehaviourCapProvider implements ICapabilityProvider {
        private final BasketBlockEntity basket;
        private LazyOptional<BehaviourProvider> lazy;

        BasketBehaviourCapProvider(BasketBlockEntity basket) {
            this.basket = basket;
            this.lazy = LazyOptional.of(() -> new BasketBehaviourProvider(basket));
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == CDPCapabilities.BEHAVIOUR_PROVIDER) {
                return lazy.cast();
            }
            return LazyOptional.empty();
        }
    }
}
