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

package plus.dragons.createintegratedfarming.api.datamap;

import net.minecraft.resources.ResourceLocation;

/**
 * Lightweight wrapper mirroring NeoForge's {@code net.neoforged.neoforge.registries.datamaps.DataMapType}
 * for API compatibility. In the upstream NeoForge version, DataMapType is a registry-backed data map
 * keyed by ResourceLocation. On Forge 1.20.1, data maps do not exist, so this class serves only as
 * a type token that downstream mods can reference via the same public field names
 * ({@link plus.dragons.createintegratedfarming.common.registry.CIFDataMaps#CHICKEN_FOOD_ITEMS},
 *  {@link plus.dragons.createintegratedfarming.common.registry.CIFDataMaps#CHICKEN_FOOD_FLUIDS}).
 * <p>
 * Actual data lookup is performed by {@link plus.dragons.createintegratedfarming.common.registry.CIFChickenFoods}.
 *
 * @param <R> the registry element type (e.g. Item, Fluid)
 * @param <T> the data type (e.g. ChickenFoodItem, ChickenFoodFluid)
 */
public final class DataMapType<R, T> {
    private final ResourceLocation id;

    private DataMapType(ResourceLocation id) {
        this.id = id;
    }

    public static <R, T> DataMapType<R, T> of(ResourceLocation id) {
        return new DataMapType<>(id);
    }

    public ResourceLocation id() {
        return id;
    }
}
