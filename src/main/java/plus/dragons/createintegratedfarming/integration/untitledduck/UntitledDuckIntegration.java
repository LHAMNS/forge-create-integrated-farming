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

package plus.dragons.createintegratedfarming.integration.untitledduck;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.integration.ModIntegration;
import plus.dragons.createintegratedfarming.integration.untitledduck.ponder.UntitledDuckPonderPlugin;
import plus.dragons.createintegratedfarming.integration.untitledduck.registry.UntitledDuckBlockEntities;
import plus.dragons.createintegratedfarming.integration.untitledduck.registry.UntitledDuckBlocks;
import plus.dragons.createintegratedfarming.integration.untitledduck.registry.UntitledDuckCapturables;

/**
 * Entry point for the Untitled Duck Mod integration module.
 * All calls to this class are guarded behind {@link ModIntegration#UNTITLED_DUCK}{@code .enabled()}
 * checks in {@link CIFCommon}, so duck mod classes are never loaded unless the mod is present.
 */
public class UntitledDuckIntegration {
    /**
     * Called during mod construction if Untitled Duck Mod is loaded.
     * Registers blocks and block entities via Registrate.
     */
    public static void registerCommon(IEventBus modBus) {
        UntitledDuckBlocks.register();
        UntitledDuckBlockEntities.register();
    }

    /**
     * Called during {@link net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent}
     * if Untitled Duck Mod is loaded. Registers capturables that require
     * registries to be frozen.
     */
    public static void commonSetup() {
        UntitledDuckCapturables.register();
    }

    /**
     * Adds UntitledDuck roost items to the creative tab.
     * Called from {@link plus.dragons.createintegratedfarming.common.registry.CIFCreativeModeTabs}
     * when Untitled Duck Mod is loaded.
     */
    public static void addCreativeTabItems(CreativeModeTab.Output output) {
        UntitledDuckBlocks.addCreativeTabItems(output);
    }

    /**
     * Called during client construction if Untitled Duck Mod is loaded.
     * Registers client-side ponder scenes and tags.
     */
    public static void registerClient() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            UntitledDuckPonderPlugin.register();
        }
    }
}
