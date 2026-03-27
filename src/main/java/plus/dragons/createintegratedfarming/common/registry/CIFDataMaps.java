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

package plus.dragons.createintegratedfarming.common.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import plus.dragons.createintegratedfarming.api.datamap.DataMapType;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.common.ranching.roost.chicken.ChickenFoodFluid;
import plus.dragons.createintegratedfarming.common.ranching.roost.chicken.ChickenFoodItem;

/**
 * Compatibility layer replacing NeoForge DataMaps for Forge 1.20.1.
 * <p>
 * In the original NeoForge version, chicken food data was stored in
 * {@code net.neoforged.neoforge.registries.datamaps.DataMapType} registries.
 * Since Forge 1.20.1 does not have DataMaps, this class exposes lightweight
 * {@link DataMapType} tokens with the same public field names as upstream,
 * and delegates actual lookups to {@link CIFChickenFoods} which uses static HashMaps.
 *
 * @see CIFChickenFoods
 */
public class CIFDataMaps {
    /**
     * Data map type for chicken food items.
     * <p>
     * Upstream equivalent: {@code DataMapType<Item, ChickenFoodItem>} registered under
     * {@code createintegratedfarming:chicken_food} in the Item registry.
     * On Forge 1.20.1, this is a lightweight type token; actual data lookup is performed
     * by {@link CIFChickenFoods#getItemFood(ItemStack)}.
     */
    public static final DataMapType<Item, ChickenFoodItem> CHICKEN_FOOD_ITEMS = DataMapType
            .of(new ResourceLocation(CIFCommon.ID, "chicken_food"));

    /**
     * Data map type for chicken food fluids.
     * <p>
     * Upstream equivalent: {@code DataMapType<Fluid, ChickenFoodFluid>} registered under
     * {@code createintegratedfarming:chicken_food} in the Fluid registry.
     * On Forge 1.20.1, this is a lightweight type token; actual data lookup is performed
     * by {@link CIFChickenFoods#getFluidFood(FluidStack)}.
     */
    public static final DataMapType<Fluid, ChickenFoodFluid> CHICKEN_FOOD_FLUIDS = DataMapType
            .of(new ResourceLocation(CIFCommon.ID, "chicken_food"));

    /**
     * Initializes data map registrations.
     * Delegates to {@link CIFChickenFoods#register()} which populates the static maps.
     */
    public static void register() {
        CIFChickenFoods.register();
    }

    /**
     * Looks up chicken food data for the given item.
     * Equivalent to the NeoForge DataMap lookup for CHICKEN_FOOD_ITEMS.
     */
    public static @Nullable ChickenFoodItem getChickenFoodItem(ItemStack stack) {
        return CIFChickenFoods.getItemFood(stack);
    }

    /**
     * Looks up chicken food data for the given fluid.
     * Equivalent to the NeoForge DataMap lookup for CHICKEN_FOOD_FLUIDS.
     */
    public static @Nullable ChickenFoodFluid getChickenFoodFluid(FluidStack fluid) {
        return CIFChickenFoods.getFluidFood(fluid);
    }
}
