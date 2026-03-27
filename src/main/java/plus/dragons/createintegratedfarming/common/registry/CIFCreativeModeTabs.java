/*
 * Copyright (C) 2025  DragonsPlus
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Ported from NeoForge 1.21.1 to Forge 1.20.1
 */
package plus.dragons.createintegratedfarming.common.registry;

import static plus.dragons.createintegratedfarming.common.registry.CIFBlocks.*;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllCreativeModeTabs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.integration.ModIntegration;
import plus.dragons.createintegratedfarming.integration.untitledduck.UntitledDuckIntegration;

public class CIFCreativeModeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CIFCommon.ID);

    public static final RegistryObject<CreativeModeTab> BASE = TABS.register("base", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + CIFCommon.ID + ".base"))
                    .withTabsBefore(AllCreativeModeTabs.BASE_CREATIVE_TAB.getId())
                    .icon(AllBlocks.MECHANICAL_HARVESTER::asStack)
                    .displayItems(CIFCreativeModeTabs::buildBaseContents)
                    .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    private static void buildBaseContents(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(FISHING_NET);
        LAVA_FISHING_NET.ifPresent(block -> output.accept(block));
        output.accept(ROOST);
        output.accept(CHICKEN_ROOST);
        // UntitledDuck integration: add duck/goose roost variants if the mod is loaded
        if (ModIntegration.UNTITLED_DUCK.enabled()) {
            UntitledDuckIntegration.addCreativeTabItems(output);
        }
    }
}
