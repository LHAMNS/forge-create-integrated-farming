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

import static plus.dragons.createintegratedfarming.common.registry.CIFBlocks.*;

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
import net.minecraftforge.common.crafting.conditions.NotCondition;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.integration.ModIntegration;

/**
 * Recipe provider for Create: Integrated Farming fallback recipes.
 * Ported from NeoForge 1.21.1 to Forge 1.20.1.
 *
 * <p>Key differences from the NeoForge version:
 * <ul>
 *   <li>Uses {@link RegistrateRecipeProvider} with {@code Consumer<FinishedRecipe>} instead of {@code RecipeOutput}</li>
 *   <li>No {@code HolderLookup.Provider} parameter (not available in Forge 1.20.1)</li>
 *   <li>Uses Forge {@link ConditionalRecipe} instead of NeoForge condition system</li>
 *   <li>Uses vanilla {@link ShapedRecipeBuilder} instead of CDP VanillaRecipeBuilders</li>
 * </ul>
 */
public class CIFRecipeProvider extends RegistrateRecipeProvider {
    public CIFRecipeProvider(PackOutput output) {
        super(CIFCommon.REGISTRATE, output);
    }

    // Fallback recipe if Farmer's Delight is not loaded
    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> output) {
        // Fallback Roost recipe (when Farmer's Delight is not loaded)
        ConditionalRecipe.builder()
                .addCondition(new NotCondition(new ModLoadedCondition(ModIntegration.Mods.FARMERS_DELIGHT)))
                .addRecipe(consumer ->
                        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ROOST.get())
                                .define('#', Items.HAY_BLOCK)
                                .define('b', Items.BAMBOO)
                                .define('c', Items.WHEAT)
                                .pattern("b b")
                                .pattern("#c#")
                                .pattern("b#b")
                                .unlockedBy("has_hay_block", has(Items.HAY_BLOCK))
                                .save(consumer)
                )
                .generateAdvancement()
                .build(output, CIFCommon.asResource("fallback_roost"));

        // Fallback Fishing Net recipe (when Farmer's Delight is not loaded)
        ConditionalRecipe.builder()
                .addCondition(new NotCondition(new ModLoadedCondition(ModIntegration.Mods.FARMERS_DELIGHT)))
                .addRecipe(consumer ->
                        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, FISHING_NET.get(), 2)
                                .define('#', Items.BROWN_WOOL)
                                .define('/', Tags.Items.RODS_WOODEN)
                                .define('a', AllItems.ANDESITE_ALLOY.get())
                                .pattern("#/")
                                .pattern("/a")
                                .unlockedBy("has_brown_wool", has(Items.BROWN_WOOL))
                                .unlockedBy("has_andesite_alloy", has(AllItems.ANDESITE_ALLOY.get()))
                                .save(consumer)
                )
                .generateAdvancement()
                .build(output, CIFCommon.asResource("fallback_fishing_net"));
    }

}
