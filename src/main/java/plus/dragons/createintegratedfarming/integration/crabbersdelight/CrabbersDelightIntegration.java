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

package plus.dragons.createintegratedfarming.integration.crabbersdelight;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.integration.ModIntegration;
import plus.dragons.createintegratedfarming.integration.crabbersdelight.ponder.CrabbersDelightPonderPlugin;
import plus.dragons.createintegratedfarming.integration.crabbersdelight.registry.CrabbersDelightArmInteractionPointTypes;

/**
 * Entry point for the Crabber's Delight integration module.
 * All calls to this class are guarded behind {@link ModIntegration#CRABBERS_DELIGHT}{@code .enabled()}
 * checks in {@link CIFCommon}, so Crabber's Delight classes are never loaded unless the mod is present.
 */
public class CrabbersDelightIntegration {
    /**
     * Called during mod construction if Crabber's Delight is loaded.
     * Registers deferred registries onto the mod event bus.
     */
    public static void registerCommon(IEventBus modBus) {
        CrabbersDelightArmInteractionPointTypes.register(modBus);
    }

    /**
     * Called during client construction if Crabber's Delight is loaded.
     * Registers client-side ponder tags.
     */
    public static void registerClient() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CrabbersDelightPonderPlugin.register();
        }
    }
}
