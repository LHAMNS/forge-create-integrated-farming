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

package plus.dragons.createintegratedfarming.integration.corndelight;

import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.integration.ModIntegration;
import plus.dragons.createintegratedfarming.integration.corndelight.registry.CornDelightHarvestBehaviours;

/**
 * Entry point for the Corn Delight (mmlib) integration module.
 * All calls to this class are guarded behind {@link ModIntegration#MMLIB}{@code .enabled()}
 * checks in {@link CIFCommon}, so mmlib classes are never loaded unless the mod is present.
 */
public class CornDelightIntegration {
    /**
     * Called during {@link net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent}
     * if mmlib is loaded. Registers harvest behaviours.
     */
    public static void commonSetup() {
        CornDelightHarvestBehaviours.register();
    }
}
