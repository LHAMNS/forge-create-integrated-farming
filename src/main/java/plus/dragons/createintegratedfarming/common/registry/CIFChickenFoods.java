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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.common.ranching.roost.chicken.ChickenFoodFluid;
import plus.dragons.createintegratedfarming.common.ranching.roost.chicken.ChickenFoodItem;
import plus.dragons.createintegratedfarming.integration.ModIntegration;

/**
 * Registry for chicken food items and fluids, replacing NeoForge DataMaps for Forge 1.20.1.
 * <p>
 * Data is loaded from JSON files by {@link plus.dragons.createintegratedfarming.common.ranching.roost.chicken.ChickenFoodReloadListener},
 * which calls {@link #reload(Map, Map)} to atomically replace all entries. Hardcoded defaults
 * in {@link #register()} serve as fallback values that are populated during mod initialization
 * and will be overridden once data packs are loaded via the reload listener.
 * <p>
 * Thread safety: The item and fluid maps are stored as volatile references to immutable maps.
 * The {@link #reload(Map, Map)} method atomically replaces both references, ensuring that
 * queries from the server tick thread always see a consistent snapshot even if a reload
 * occurs concurrently on the resource reload thread.
 * <p>
 * Lookup priority:
 * <ol>
 *   <li>Explicit entry in the item/fluid food map (from JSON data or hardcoded registration)</li>
 *   <li>Item tag fallback: items in {@link #CHICKEN_FOOD_TAG} receive default seed-equivalent values</li>
 * </ol>
 *
 * @see plus.dragons.createintegratedfarming.common.ranching.roost.chicken.ChickenFoodReloadListener
 */
public class CIFChickenFoods {
    private static final Logger LOGGER = CIFCommon.LOGGER;

    /**
     * Item tag for chicken food items. Items in this tag that are not explicitly registered
     * via data packs or {@link #registerItemFood} will use a default seed-equivalent {@link ChickenFoodItem}.
     * This provides a data-pack-driven way for modpacks to add chicken foods without code changes,
     * partially compensating for the lack of NeoForge DataMaps in Forge 1.20.1.
     */
    /**
     * Item tag for chicken food fallback. Uses minecraft namespace for upstream compatibility:
     * upstream NeoForge references {@code #minecraft:chicken_food} in DataMaps.
     */
    public static final TagKey<Item> CHICKEN_FOOD_TAG = TagKey.create(
            Registries.ITEM, new ResourceLocation("minecraft", "chicken_food"));

    /** Immutable holder for both food maps, allowing truly atomic replacement via single volatile write. */
    private record FoodMaps(Map<Item, ChickenFoodItem> items, Map<Fluid, ChickenFoodFluid> fluids) {}

    /**
     * Volatile reference to the current food maps. Replaced atomically by {@link #reload}.
     * Single volatile write ensures both item and fluid maps are always consistent.
     */
    private static volatile FoodMaps foodMaps = new FoodMaps(Collections.emptyMap(), Collections.emptyMap());

    /**
     * Maximum size for the tag fallback cache. In practice, the cache is bounded by the number
     * of unique items in the game, but this hard cap prevents pathological growth if a mod
     * generates dynamic items.
     */
    private static final int TAG_FALLBACK_CACHE_MAX_SIZE = 4096;

    /**
     * Cache for tag-based fallback results. When an item is not in the explicit map but matches
     * {@link #CHICKEN_FOOD_TAG}, the result is cached here to avoid repeated tag iteration.
     * Items confirmed NOT in the tag are also cached (as NEGATIVE_SENTINEL) to avoid repeated
     * tag checks on non-food items passing through belts/hoppers.
     * Cleared on {@link #reload} since tag membership may change with data packs.
     * Capped at {@link #TAG_FALLBACK_CACHE_MAX_SIZE} entries to prevent unbounded growth.
     */
    private static final Map<Item, ChickenFoodItem> tagFallbackCache = new ConcurrentHashMap<>();

    /** Sentinel value indicating an item was checked and is NOT a chicken food. Never returned to callers. */
    private static final ChickenFoodItem NEGATIVE_SENTINEL = new ChickenFoodItem(
            ConstantInt.of(0), ConstantInt.of(0), Optional.empty());

    /** Default food values for tag-based fallback, equivalent to vanilla seed food values. */
    private static final ChickenFoodItem DEFAULT_TAG_FOOD = new ChickenFoodItem(
            ConstantInt.of(2400),
            UniformInt.of(400, 800),
            Optional.empty());

    /**
     * Registers a single item food entry into the current map.
     * <p>
     * Note: Entries registered this way will be overridden when the reload listener runs.
     * This method is primarily for hardcoded defaults during mod initialization.
     *
     * @param item the item to register (must not be null)
     * @param food the chicken food data (must not be null)
     */
    public static void registerItemFood(Item item, ChickenFoodItem food) {
        if (item == null) {
            LOGGER.warn("[CIFChickenFoods] Attempted to register null item, skipping");
            return;
        }
        if (food == null) {
            LOGGER.warn("[CIFChickenFoods] Attempted to register null food for item {}, skipping",
                    BuiltInRegistries.ITEM.getKey(item));
            return;
        }
        // Create a mutable copy, add entry, then atomically replace both maps
        FoodMaps current = foodMaps;
        Map<Item, ChickenFoodItem> newMap = new HashMap<>(current.items());
        newMap.put(item, food);
        foodMaps = new FoodMaps(Collections.unmodifiableMap(newMap), current.fluids());
    }

    /**
     * Registers a single fluid food entry into the current map.
     * <p>
     * Note: Entries registered this way will be overridden when the reload listener runs.
     * This method is primarily for hardcoded defaults during mod initialization.
     *
     * @param fluid the fluid to register (must not be null)
     * @param food  the chicken food data (must not be null)
     */
    public static void registerFluidFood(Fluid fluid, ChickenFoodFluid food) {
        if (fluid == null) {
            LOGGER.warn("[CIFChickenFoods] Attempted to register null fluid, skipping");
            return;
        }
        if (food == null) {
            LOGGER.warn("[CIFChickenFoods] Attempted to register null food for fluid {}, skipping",
                    BuiltInRegistries.FLUID.getKey(fluid));
            return;
        }
        FoodMaps current = foodMaps;
        Map<Fluid, ChickenFoodFluid> newMap = new HashMap<>(current.fluids());
        newMap.put(fluid, food);
        foodMaps = new FoodMaps(current.items(), Collections.unmodifiableMap(newMap));
    }

    /**
     * Atomically replaces all chicken food entries with data loaded from JSON.
     * Called by {@link plus.dragons.createintegratedfarming.common.ranching.roost.chicken.ChickenFoodReloadListener}
     * when data packs are loaded or reloaded.
     * <p>
     * If the loaded maps are empty (e.g., no JSON files found), the hardcoded defaults from
     * {@link #register()} are preserved as fallback. This ensures the mod always has functional
     * chicken food data even if no data packs provide chicken food JSON files.
     *
     * @param newItemFoods  the new item food map (must not be null; may be empty)
     * @param newFluidFoods the new fluid food map (must not be null; may be empty)
     */
    public static void reload(Map<Item, ChickenFoodItem> newItemFoods, Map<Fluid, ChickenFoodFluid> newFluidFoods, boolean explicitReplace) {
        FoodMaps current = foodMaps;
        if (newItemFoods == null) {
            LOGGER.warn("[CIFChickenFoods] reload() received null item foods map, keeping existing data");
            newItemFoods = new HashMap<>(current.items());
        }
        if (newFluidFoods == null) {
            LOGGER.warn("[CIFChickenFoods] reload() received null fluid foods map, keeping existing data");
            newFluidFoods = new HashMap<>(current.fluids());
        }

        // If both maps are empty and we have existing data, preserve defaults.
        // This prevents accidental wipe when all JSON entries are condition-skipped.
        // Callers that intentionally want to clear should use the explicitReplace mechanism.
        if (newItemFoods.isEmpty() && newFluidFoods.isEmpty() && !current.items().isEmpty() && !explicitReplace) {
            LOGGER.debug("[CIFChickenFoods] reload() received empty maps, preserving existing {} item(s) and {} fluid(s)",
                    current.items().size(), current.fluids().size());
            tagFallbackCache.clear();
            return;
        }

        // Defensively copy incoming maps to decouple from caller's references.
        // Then atomically replace both maps with a single volatile write.
        foodMaps = new FoodMaps(
                Collections.unmodifiableMap(new HashMap<>(newItemFoods)),
                Collections.unmodifiableMap(new HashMap<>(newFluidFoods)));
        tagFallbackCache.clear(); // Invalidate tag fallback cache since data may have changed

        LOGGER.debug("[CIFChickenFoods] Reloaded: {} item food(s), {} fluid food(s)",
                newItemFoods.size(), newFluidFoods.size());
    }

    /**
     * Looks up chicken food data for the given item stack.
     * <p>
     * Lookup priority:
     * <ol>
     *   <li>Explicit entry in the item food map</li>
     *   <li>Tag fallback: items in {@link #CHICKEN_FOOD_TAG} get default seed-equivalent values</li>
     * </ol>
     *
     * @param stack the item stack to look up (may be null)
     * @return the chicken food data, or {@code null} if the item is not a chicken food
     */
    public static @Nullable ChickenFoodItem getItemFood(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == null) {
            return null;
        }
        // Read volatile reference once for consistent snapshot
        Map<Item, ChickenFoodItem> snapshot = foodMaps.items();
        ChickenFoodItem food = snapshot.get(item);
        if (food != null) {
            return food;
        }
        // Check tag fallback cache (avoids repeated tag iteration on hot path).
        // Positive hits return DEFAULT_TAG_FOOD; negative hits return NEGATIVE_SENTINEL.
        food = tagFallbackCache.get(item);
        if (food != null) {
            return food == NEGATIVE_SENTINEL ? null : food;
        }
        // Tag-based fallback: items in the chicken_food tag get default seed-equivalent values.
        // This allows modpacks to add chicken foods via data packs without code changes.
        // Both positive AND negative results are cached to prevent repeated tag iteration
        // on belts/hoppers where non-food items pass through frequently.
        try {
            if (stack.is(CHICKEN_FOOD_TAG)) {
                if (tagFallbackCache.size() < TAG_FALLBACK_CACHE_MAX_SIZE) {
                    tagFallbackCache.put(item, DEFAULT_TAG_FOOD);
                }
                return DEFAULT_TAG_FOOD;
            }
        } catch (Exception e) {
            LOGGER.debug("[CIFChickenFoods] Error checking tag for item {}: {}",
                    BuiltInRegistries.ITEM.getKey(item), e.getMessage());
        }
        // Cache negative result to avoid re-checking this item on every tick
        if (tagFallbackCache.size() < TAG_FALLBACK_CACHE_MAX_SIZE) {
            tagFallbackCache.put(item, NEGATIVE_SENTINEL);
        }
        return null;
    }

    /**
     * Looks up chicken food data for the given fluid stack.
     *
     * @param fluid the fluid stack to look up (may be null)
     * @return the chicken food data, or {@code null} if the fluid is not a chicken food
     */
    public static @Nullable ChickenFoodFluid getFluidFood(@Nullable FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            return null;
        }
        Fluid fluidType = fluid.getFluid();
        if (fluidType == null) {
            return null;
        }
        // Read volatile reference once for consistent snapshot
        Map<Fluid, ChickenFoodFluid> snapshot = foodMaps.fluids();
        return snapshot.get(fluidType);
    }

    /**
     * Returns an unmodifiable view of the current item food map.
     * Useful for debugging or integration purposes.
     *
     * @return the current item food map (never null)
     */
    public static Map<Item, ChickenFoodItem> getItemFoods() {
        return foodMaps.items();
    }

    /**
     * Returns an unmodifiable view of the current fluid food map.
     * Useful for debugging or integration purposes.
     *
     * @return the current fluid food map (never null)
     */
    public static Map<Fluid, ChickenFoodFluid> getFluidFoods() {
        return foodMaps.fluids();
    }

    /**
     * Registers hardcoded default chicken food entries.
     * <p>
     * These defaults serve as fallback values until the reload listener loads JSON data from data packs.
     * Once the reload listener runs (which happens after world load), JSON-defined entries will replace
     * these defaults. If no JSON data packs are present, these hardcoded values remain in effect.
     * <p>
     * Called during {@link net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent} via
     * {@link CIFCommon#onCommonSetup}.
     */
    public static void register() {
        LOGGER.debug("[CIFChickenFoods] Registering hardcoded default chicken foods");

        // Register vanilla chicken food items (equivalent to CHICKEN_FOOD tag items)
        ChickenFoodItem seedFood = new ChickenFoodItem(
                ConstantInt.of(2400),
                UniformInt.of(400, 800),
                Optional.empty());
        registerItemFood(Items.WHEAT_SEEDS, seedFood);
        registerItemFood(Items.MELON_SEEDS, seedFood);
        registerItemFood(Items.PUMPKIN_SEEDS, seedFood);
        registerItemFood(Items.BEETROOT_SEEDS, seedFood);
        registerItemFood(Items.TORCHFLOWER_SEEDS, seedFood);
        registerItemFood(Items.PITCHER_POD, seedFood);

        // Register Create Crafts & Additions seed oil if the mod is loaded
        if (ModIntegration.CREATE_CRAFT_AND_ADDITIONS.enabled()) {
            try {
                Fluid seedOil = BuiltInRegistries.FLUID.get(
                        new ResourceLocation(ModIntegration.Mods.CREATE_CRAFT_AND_ADDITIONS, "seed_oil"));
                if (seedOil != net.minecraft.world.level.material.Fluids.EMPTY) {
                    registerFluidFood(seedOil, new ChickenFoodFluid(
                            ConstantInt.of(2400),
                            UniformInt.of(400, 800),
                            100));
                } else {
                    LOGGER.debug("[CIFChickenFoods] Create Crafts & Additions loaded but seed_oil fluid not found");
                }
            } catch (Exception e) {
                LOGGER.warn("[CIFChickenFoods] Failed to register seed_oil fluid food: {}", e.getMessage());
            }
        }

        LOGGER.info("[CIFChickenFoods] Registered {} hardcoded item food(s) and {} hardcoded fluid food(s)",
                foodMaps.items().size(), foodMaps.fluids().size());
    }
}
