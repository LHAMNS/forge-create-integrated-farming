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

package plus.dragons.createintegratedfarming.integration.farmersdelight.data;

import static plus.dragons.createintegratedfarming.common.registry.CIFBlocks.FISHING_NET;
import static plus.dragons.createintegratedfarming.common.registry.CIFBlocks.ROOST;

import com.simibubi.create.AllItems;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import java.util.function.Consumer;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.integration.ModIntegration;
import vectorwing.farmersdelight.common.registry.ModItems;

/**
 * Recipe provider for Farmer's Delight integration recipes.
 * These recipes are conditionally loaded only when Farmer's Delight is present.
 */
public class FDRecipeProvider extends RegistrateRecipeProvider {
    public FDRecipeProvider(PackOutput output) {
        super(CIFCommon.REGISTRATE, output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> output) {
        // Roost recipe using FD Canvas
        ConditionalRecipe.builder()
                .addCondition(new ModLoadedCondition(ModIntegration.Mods.FARMERS_DELIGHT))
                .addRecipe(
                        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ROOST.get())
                                .define('#', ModItems.CANVAS.get())
                                .define('b', Items.BAMBOO)
                                .define('c', Items.WHEAT)
                                .pattern("b b")
                                .pattern("#c#")
                                .pattern("b#b")
                                .unlockedBy("has_canvas", has(ModItems.CANVAS.get()))
                                ::save
                )
                .build(output, CIFCommon.asResource("roost"));

        // Fishing Net recipe using FD Safety Net
        ConditionalRecipe.builder()
                .addCondition(new ModLoadedCondition(ModIntegration.Mods.FARMERS_DELIGHT))
                .addRecipe(
                        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, FISHING_NET.get(), 2)
                                .define('#', ModItems.SAFETY_NET.get())
                                .define('/', Tags.Items.RODS_WOODEN)
                                .define('a', AllItems.ANDESITE_ALLOY.get())
                                .pattern("#/")
                                .pattern("/a")
                                .unlockedBy("has_safety_net", has(ModItems.SAFETY_NET.get()))
                                .unlockedBy("has_andesite_alloy", has(AllItems.ANDESITE_ALLOY.get()))
                                ::save
                )
                .build(output, CIFCommon.asResource("fishing_net"));
    }

}
