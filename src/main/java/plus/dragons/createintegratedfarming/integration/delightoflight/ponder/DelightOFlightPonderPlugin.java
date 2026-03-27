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

package plus.dragons.createintegratedfarming.integration.delightoflight.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import plus.dragons.createintegratedfarming.client.ponder.CIFPonderPlugin;
import plus.dragons.createintegratedfarming.client.ponder.CIFPonderTags;

public class DelightOFlightPonderPlugin {
    public static void register() {
        CIFPonderPlugin.TAGS.add(DelightOFlightPonderPlugin::registerTags);
        CIFPonderPlugin.SCENES.add(DelightOFlightPonderPlugin::registerScene);
    }

    private static void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.addToTag(CIFPonderTags.FARMING_APPLIANCES)
                .add(BuiltInRegistries.BLOCK.getKey(Blocks.LIGHTNING_ROD));
    }

    public static void registerScene(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(BuiltInRegistries.BLOCK.getKey(Blocks.LIGHTNING_ROD))
                .addStoryBoard("delightoflight/weather_soil", DelightOFlightPonderScenes::catalyze, CIFPonderTags.FARMING_APPLIANCES);
    }
}
