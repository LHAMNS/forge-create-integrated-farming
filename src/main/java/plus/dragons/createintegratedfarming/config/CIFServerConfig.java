/*
 * Copyright (C) 2025  DragonsPlus
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Ported from NeoForge 1.21.1 to Forge 1.20.1
 */
package plus.dragons.createintegratedfarming.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CIFServerConfig {
    // Fishing
    public final ForgeConfigSpec.BooleanValue fishingNetChecksOpenWater;
    public final ForgeConfigSpec.IntValue fishingNetCooldownMultiplier;
    public final ForgeConfigSpec.IntValue fishingNetMaxRecordedBlocks;
    public final ForgeConfigSpec.BooleanValue fishingNetCaptureCreatureInWater;
    public final ForgeConfigSpec.DoubleValue fishingNetCapturedCreatureMaxSize;
    public final ForgeConfigSpec.BooleanValue fishingNetCapturedCreatureDropExpNugget;
    // Ranching
    public final ForgeConfigSpec.BooleanValue leashedEntitySitsAutomatically;
    public final ForgeConfigSpec.IntValue roostingInventorySlotCount;
    public final ForgeConfigSpec.IntValue roostingInventorySlotSize;
    // Farming
    public final ForgeConfigSpec.BooleanValue mushroomColoniesDropSelf;

    public CIFServerConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Settings for Fishing utilities").push("fishing");
        fishingNetChecksOpenWater = builder
                .comment("If Fishing Net should check for open water.",
                        "When disabled, the open water check will be skipped and return false.")
                .define("fishingNetChecksOpenWater", true);
        fishingNetCooldownMultiplier = builder
                .comment("Fishing Net's cooldown will be multiplied by this value.",
                        "The base cooldown is the same as Fishing Rod's lure speed (100 ~ 600 ticks).")
                .defineInRange("fishingNetCooldownMultiplier", 8, 1, 256);
        fishingNetMaxRecordedBlocks = builder
                .comment("The maximum amount of the visited valid blocks for fishing recorded by the Fishing Net.",
                        "Fishing Net's chance of successful catch depends on [amount] / [maximum amount] of visited valid blocks.",
                        "Increasing this value will reduce the efficiency of Fishing Net that travels in a fixed short route.",
                        "Example: Fishing Net placed near the rotating axis.")
                .defineInRange("fishingNetMaxRecordedBlocks", 8, 1, 64);
        fishingNetCaptureCreatureInWater = builder
                .comment("If Fishing Net should capture small creature and automatically process them.",
                        "\"Process\" means captured entity will be discard and all drops will be collected.")
                .define("fishingNetCaptureCreatureInWater", true);
        fishingNetCapturedCreatureMaxSize = builder
                .comment("The maximum size (width and height) of creatures that can be caught by the Fishing Net.")
                .defineInRange("fishingNetCapturedCreatureMaxSize", 0.7, 0.01, 10.0);
        fishingNetCapturedCreatureDropExpNugget = builder
                .comment("If creatures captured by Fishing Net should drop Nugget of Experience.")
                .define("fishingNetCapturedCreatureDropExpNugget", true);
        builder.pop();

        builder.comment("Settings for Ranching utilities").push("ranching");
        leashedEntitySitsAutomatically = builder
                .comment("If leashed entity automatically sits on unoccupied seat.",
                        "When enabled, falls back to vanilla Create behaviour.",
                        "When disabled, seated leashable entity can be dismounted by lead.")
                .define("leashedEntitySitsAutomatically", false);
        roostingInventorySlotCount = builder
                .comment("The amount of Inventory Slot that the Chicken Roost has available.",
                        "[Server restart required]")
                .defineInRange("roostingInventorySlotCount", 9, 1, 27);
        roostingInventorySlotSize = builder
                .comment("The amount of items per Inventory slot that the Chicken Roost can hold.",
                        "[Server restart required]")
                .defineInRange("roostingInventorySlotSize", 1, 1, 16);
        builder.pop();

        builder.comment("Settings for Farming utilities").push("farming");
        mushroomColoniesDropSelf = builder
                .comment("When harvested by Harvester, if mushroom colonies drops itself instead of corresponding mushroom.")
                .define("mushroomColoniesDropSelf", false);
        builder.pop();
    }
}
