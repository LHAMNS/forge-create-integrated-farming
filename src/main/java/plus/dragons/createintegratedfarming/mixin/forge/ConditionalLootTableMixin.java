/*
 * Copyright (C) 2025  DragonsPlus
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * Ported from NeoForge 1.21.1 to Forge 1.20.1
 *
 * This mixin adds NeoForge-style top-level "conditions" support to loot table loading.
 * In NeoForge 1.21.1, loot tables can have a top-level "conditions" array (using
 * neoforge:mod_loaded etc.) that prevents the table from loading if conditions aren't met.
 * Forge 1.20.1 only supports conditions on recipes, not loot tables.
 * This mixin bridges that gap by intercepting ForgeHooks.loadLootTable and checking
 * conditions before the JSON is parsed, preventing errors from unresolved item/block IDs.
 */
package plus.dragons.createintegratedfarming.mixin.forge;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds NeoForge-compatible top-level condition checking to Forge loot table loading.
 * When a loot table JSON contains a top-level "conditions" array, this mixin
 * evaluates those conditions before parsing. If conditions are not met (e.g.,
 * a required mod is not loaded), the loot table is replaced with an empty table,
 * preventing JSON parse errors from unregistered item/block IDs.
 */
@Mixin(ForgeHooks.class)
public abstract class ConditionalLootTableMixin {

    /**
     * Intercepts loot table loading to check for top-level conditions.
     * If conditions exist and are not satisfied, returns an empty LootTable
     * instead of attempting to parse the JSON (which would fail if it
     * references items/blocks from unloaded mods).
     */
    @Inject(method = "loadLootTable", at = @At("HEAD"), cancellable = true, remap = false)
    private static void createintegratedfarming$checkLootTableConditions(
            com.google.gson.Gson gson,
            ResourceLocation name,
            JsonElement data,
            boolean custom,
            CallbackInfoReturnable<LootTable> cir) {
        if (data != null && data.isJsonObject()) {
            JsonObject json = data.getAsJsonObject();
            // Support both "conditions" (NeoForge style) and "forge:conditions" (Forge recipe style)
            String conditionsKey = null;
            if (json.has("conditions")) {
                conditionsKey = "conditions";
            } else if (json.has("forge:conditions")) {
                conditionsKey = "forge:conditions";
            }
            if (conditionsKey != null) {
                try {
                    if (!CraftingHelper.processConditions(json, conditionsKey, ICondition.IContext.EMPTY)) {
                        // Conditions not met — return empty loot table to prevent parse errors
                        cir.setReturnValue(LootTable.EMPTY);
                    }
                } catch (Exception e) {
                    // If condition processing itself fails, let the original method handle it
                }
            }
        }
    }
}
