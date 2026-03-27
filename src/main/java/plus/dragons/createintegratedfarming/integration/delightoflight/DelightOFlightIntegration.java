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

package plus.dragons.createintegratedfarming.integration.delightoflight;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.integration.ModIntegration;
import plus.dragons.createintegratedfarming.integration.delightoflight.ponder.DelightOFlightPonderPlugin;
import plus.dragons.createintegratedfarming.integration.delightoflight.registry.DelightOFlightHarvestBehaviors;

/**
 * Entry point for the Delight O' Flight integration module.
 * All calls to this class are guarded behind {@link ModIntegration#DELIGHT_O_FLIGHT}{@code .enabled()}
 * checks in {@link CIFCommon}, so Delight O' Flight classes are never loaded unless the mod is present.
 */
public class DelightOFlightIntegration {
    /**
     * Called during {@link net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent}
     * if Delight O' Flight is loaded. Registers harvest behaviours.
     */
    public static void commonSetup() {
        DelightOFlightHarvestBehaviors.register();
    }

    /**
     * Called during client construction if Delight O' Flight is loaded.
     * Registers client-side ponder scenes and tags.
     */
    public static void registerClient() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            DelightOFlightPonderPlugin.register();
        }
    }
}
