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

package plus.dragons.createintegratedfarming.common;

import com.simibubi.create.foundation.item.ItemDescription;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.dragons.createdragonsplus.common.CDPRegistrate;
import plus.dragons.createintegratedfarming.common.registry.CIFArmInteractionPoints;
import plus.dragons.createintegratedfarming.common.registry.CIFBlockEntities;
import plus.dragons.createintegratedfarming.common.registry.CIFBlockSpoutingBehaviours;
import plus.dragons.createintegratedfarming.common.registry.CIFBlocks;
import plus.dragons.createintegratedfarming.common.fishing.net.FishingNetFakePlayer;
import plus.dragons.createintegratedfarming.common.ranching.roost.chicken.ChickenFoodReloadListener;
import plus.dragons.createintegratedfarming.common.registry.CIFChickenFoods;
import plus.dragons.createintegratedfarming.common.registry.CIFCreativeModeTabs;
import plus.dragons.createintegratedfarming.common.registry.CIFRoostCapturables;
import plus.dragons.createintegratedfarming.config.CIFConfig;
import plus.dragons.createintegratedfarming.data.CIFData;
import plus.dragons.createintegratedfarming.integration.ModIntegration;
import plus.dragons.createintegratedfarming.integration.corndelight.CornDelightIntegration;
import plus.dragons.createintegratedfarming.integration.crabbersdelight.CrabbersDelightIntegration;
import plus.dragons.createintegratedfarming.integration.delightoflight.DelightOFlightIntegration;
import plus.dragons.createintegratedfarming.integration.farmersdelight.FDIntegration;
import plus.dragons.createintegratedfarming.integration.mynethersdelight.MNDIntegration;
import plus.dragons.createintegratedfarming.integration.netherdepthsupgrade.NDUIntegration;
import plus.dragons.createintegratedfarming.integration.untitledduck.UntitledDuckIntegration;

@Mod(CIFCommon.ID)
public class CIFCommon {
    public static final String ID = "create_integrated_farming";
    public static final String NAME = "Create: Integrated Farming";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    public static final CDPRegistrate REGISTRATE = (CDPRegistrate) new CDPRegistrate(ID)
            .setTooltipModifier(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE));

    public CIFCommon() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        REGISTRATE.registerEventListeners(modBus);
        CIFCreativeModeTabs.register(modBus);
        CIFBlocks.register(modBus);
        CIFBlockEntities.register(modBus);
        CIFArmInteractionPoints.register(modBus);
        // Farmer's Delight integration - register deferred registries and capabilities
        if (ModIntegration.FARMERS_DELIGHT.enabled()) {
            FDIntegration.registerCommon(modBus);
            FDIntegration.registerClient();
        }
        // Nether Depths Upgrade integration - register blocks
        if (ModIntegration.NETHER_DEPTHS_UPGRADE.enabled()) {
            NDUIntegration.registerCommon();
            NDUIntegration.registerClient();
        }
        // Untitled Duck Mod integration - register blocks and block entities
        if (ModIntegration.UNTITLED_DUCK.enabled()) {
            UntitledDuckIntegration.registerCommon(modBus);
            UntitledDuckIntegration.registerClient();
        }
        // My Nether's Delight integration - register arm interaction point types
        if (ModIntegration.MY_NETHERS_DELIGHT.enabled()) {
            MNDIntegration.registerCommon(modBus);
            MNDIntegration.registerClient();
        }
        // Crabber's Delight integration - register arm interaction point types
        if (ModIntegration.CRABBERS_DELIGHT.enabled()) {
            CrabbersDelightIntegration.registerCommon(modBus);
            CrabbersDelightIntegration.registerClient();
        }
        // Delight O' Flight integration - register client ponder
        if (ModIntegration.DELIGHT_O_FLIGHT.enabled()) {
            DelightOFlightIntegration.registerClient();
        }
        REGISTRATE.setCreativeModeTab(CIFCreativeModeTabs.BASE);
        modBus.addListener(this::onCommonSetup);
        // Register server-side reload listener for chicken food JSON data
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> {
            event.addListener(new ChickenFoodReloadListener());
            LOGGER.debug("[CIFCommon] Registered ChickenFoodReloadListener");
        });
        // Clean up shared FakePlayer pool when a dimension is unloaded
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.level.LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                FishingNetFakePlayer.onLevelUnload(serverLevel);
            }
        });
        CIFData.register();
        CIFConfig.register();
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CIFBlockSpoutingBehaviours::register);
        event.enqueueWork(CIFRoostCapturables::register);
        event.enqueueWork(CIFChickenFoods::register);
        // Farmer's Delight integration - register behaviours after registries are frozen
        if (ModIntegration.FARMERS_DELIGHT.enabled()) {
            event.enqueueWork(FDIntegration::commonSetup);
        }
        // Nether Depths Upgrade integration - no commonSetup needed
        // Untitled Duck Mod integration - register capturables
        if (ModIntegration.UNTITLED_DUCK.enabled()) {
            event.enqueueWork(UntitledDuckIntegration::commonSetup);
        }
        // My Nether's Delight integration - register spouting behaviours
        if (ModIntegration.MY_NETHERS_DELIGHT.enabled()) {
            event.enqueueWork(MNDIntegration::commonSetup);
        }
        // Corn Delight (mmlib) integration - register harvest behaviours
        if (ModIntegration.MMLIB.enabled()) {
            event.enqueueWork(CornDelightIntegration::commonSetup);
        }
        // Delight O' Flight integration - register harvest behaviours
        if (ModIntegration.DELIGHT_O_FLIGHT.enabled()) {
            event.enqueueWork(DelightOFlightIntegration::commonSetup);
        }
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(ID, path);
    }
}
