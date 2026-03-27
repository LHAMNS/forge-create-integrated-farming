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

package plus.dragons.createintegratedfarming.integration.farmersdelight;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.integration.ModIntegration;
import plus.dragons.createintegratedfarming.integration.farmersdelight.ponder.FDPonderPlugin;
import plus.dragons.createintegratedfarming.integration.farmersdelight.registry.FDArmInteractionPointTypes;
import plus.dragons.createintegratedfarming.integration.farmersdelight.registry.FDBlockEntities;
import plus.dragons.createintegratedfarming.integration.farmersdelight.registry.FDBlockSpoutingBehaviours;
import plus.dragons.createintegratedfarming.integration.farmersdelight.registry.FDHarvestBehaviors;

/**
 * Entry point for the Farmer's Delight integration module.
 * All calls to this class are guarded behind {@link ModIntegration#FARMERS_DELIGHT}{@code .enabled()}
 * checks in {@link CIFCommon}, so FD classes are never loaded unless the mod is present.
 */
public class FDIntegration {
    /**
     * Called during mod construction if Farmer's Delight is loaded.
     * Registers deferred registries onto the mod event bus.
     */
    public static void registerCommon(IEventBus modBus) {
        FDArmInteractionPointTypes.register(modBus);
        FDBlockEntities.register(modBus);
    }

    /**
     * Called during {@link net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent}
     * if Farmer's Delight is loaded. Registers behaviours that require
     * registries to be frozen.
     */
    public static void commonSetup() {
        FDHarvestBehaviors.register();
        FDBlockSpoutingBehaviours.register();
    }

    /**
     * Called during client construction if Farmer's Delight is loaded.
     * Registers client-side ponder scenes and tags.
     */
    public static void registerClient() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FDPonderPlugin.register();
        }
    }
}
