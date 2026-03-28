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

package plus.dragons.createintegratedfarming.data;

import static plus.dragons.createintegratedfarming.common.CIFCommon.REGISTRATE;

import com.tterrag.registrate.providers.ProviderType;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import plus.dragons.createintegratedfarming.client.ponder.CIFPonderPlugin;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.common.registry.CIFLootTables;

/**
 * Data generation entry point for Create: Integrated Farming.
 * Ported from NeoForge 1.21.1 to Forge 1.20.1.
 */
@Mod.EventBusSubscriber(modid = CIFCommon.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CIFData {

    public static void register() {
        REGISTRATE.addDataGenerator(ProviderType.LOOT, CIFLootTables::generate);
        REGISTRATE.registerBuiltinLocalization("tooltips");
        REGISTRATE.registerPonderLocalization(CIFPonderPlugin::new);
        REGISTRATE.registerForeignLocalization();
    }

    @SubscribeEvent
    public static void generate(final GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        var server = event.includeServer();
        generator.addProvider(server, new CIFRecipeProvider(output));
        // Farmer's Delight integration recipes — only if FD is on the classpath.
        // FDRecipeProvider directly imports vectorwing.farmersdelight classes,
        // so instantiating it without FD causes NoClassDefFoundError.
        try {
            Class.forName("vectorwing.farmersdelight.FarmersDelight");
            generator.addProvider(server, new plus.dragons.createintegratedfarming.integration.farmersdelight.data.FDRecipeProvider(output));
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            CIFCommon.LOGGER.info("[CIFData] Farmer's Delight not found on classpath, skipping FD recipe datagen");
        }
    }
}
