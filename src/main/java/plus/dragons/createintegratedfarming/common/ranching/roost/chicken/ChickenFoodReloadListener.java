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

package plus.dragons.createintegratedfarming.common.ranching.roost.chicken;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import plus.dragons.createintegratedfarming.common.CIFCommon;
import plus.dragons.createintegratedfarming.common.registry.CIFChickenFoods;

/**
 * Server-side JSON resource reload listener for chicken food data.
 * <p>
 * Loads chicken food definitions from JSON files located at:
 * <pre>
 *   data/&lt;namespace&gt;/create_integrated_farming/chicken_food/&lt;name&gt;.json
 * </pre>
 * <p>
 * Each JSON file may contain an {@code "items"} object and/or a {@code "fluids"} object,
 * where keys are registry names of items/fluids and values are food property objects matching
 * the upstream NeoForge DataMap format:
 * <pre>{@code
 * {
 *   "replace": false,
 *   "items": {
 *     "minecraft:wheat_seeds": {
 *       "progress": { "type": "minecraft:constant", "value": 2400 },
 *       "cooldown": { "type": "minecraft:uniform", "min_inclusive": 400, "max_inclusive": 800 }
 *     }
 *   },
 *   "fluids": {
 *     "createadditions:seed_oil": {
 *       "progress": 2400,
 *       "cooldown": { "type": "minecraft:uniform", "min_inclusive": 400, "max_inclusive": 800 },
 *       "amount": 100
 *     }
 *   }
 * }
 * }</pre>
 * <p>
 * Supports data pack overriding: later data packs override earlier ones for the same file path.
 * The {@code "replace"} flag, when set to {@code true}, clears all previously loaded entries
 * from that file before adding new ones (standard data pack convention).
 * <p>
 * This listener replaces the upstream NeoForge DataMap system for Forge 1.20.1, providing
 * equivalent data-pack-driven chicken food registration.
 *
 * @see CIFChickenFoods
 * @see ChickenFoodItem
 * @see ChickenFoodFluid
 */
public class ChickenFoodReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = CIFCommon.LOGGER;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * Resource directory: {@code data/<namespace>/create_integrated_farming/chicken_food/}
     */
    private static final String DIRECTORY = CIFCommon.ID + "/chicken_food";

    public ChickenFoodReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Item, ChickenFoodItem> itemFoods = new HashMap<>();
        Map<Fluid, ChickenFoodFluid> fluidFoods = new HashMap<>();
        boolean explicitReplace = false;
        int itemCount = 0;
        int fluidCount = 0;
        int errorCount = 0;

        if (entries == null || entries.isEmpty()) {
            // No JSON files found — keep hardcoded defaults from CIFChickenFoods.register()
            LOGGER.debug("[ChickenFoodReloadListener] No chicken food JSON files found, keeping hardcoded defaults");
            return;
        }

        for (Map.Entry<ResourceLocation, JsonElement> fileEntry : entries.entrySet()) {
            ResourceLocation fileId = fileEntry.getKey();
            JsonElement rawElement = fileEntry.getValue();

            if (fileId == null) {
                LOGGER.warn("[ChickenFoodReloadListener] Skipping entry with null resource location");
                errorCount++;
                continue;
            }

            if (rawElement == null || !rawElement.isJsonObject()) {
                LOGGER.warn("[ChickenFoodReloadListener] Skipping '{}': expected JSON object, got {}",
                        fileId, rawElement == null ? "null" : rawElement.getClass().getSimpleName());
                errorCount++;
                continue;
            }

            try {
                JsonObject root = rawElement.getAsJsonObject();

                // Check forge:conditions — skip this file if conditions are not met
                // (e.g., mod not loaded). This ensures conditional chicken food entries
                // like seed_oil (requires createadditions) are properly gated.
                if (root.has("forge:conditions")) {
                    try {
                        if (!net.minecraftforge.common.crafting.CraftingHelper.processConditions(
                                GsonHelper.getAsJsonArray(root, "forge:conditions"),
                                net.minecraftforge.common.crafting.conditions.ICondition.IContext.EMPTY)) {
                            LOGGER.debug("[ChickenFoodReloadListener] Skipping '{}': forge:conditions not met", fileId);
                            continue;
                        }
                    } catch (Exception e) {
                        LOGGER.warn("[ChickenFoodReloadListener] '{}': Error processing forge:conditions: {}", fileId, e.getMessage());
                        errorCount++;
                        continue;
                    }
                }

                // Handle "replace" flag: if true, clear all previously loaded entries.
                // This follows the standard data pack convention where replace clears
                // the entire collection before adding new entries.
                boolean replace = GsonHelper.getAsBoolean(root, "replace", false);
                if (replace) {
                    LOGGER.debug("[ChickenFoodReloadListener] File '{}' has replace=true, clearing all previously loaded entries", fileId);
                    itemFoods.clear();
                    fluidFoods.clear();
                    explicitReplace = true;
                }

                // Parse item foods
                if (root.has("items")) {
                    JsonElement itemsElement = root.get("items");
                    if (itemsElement == null || !itemsElement.isJsonObject()) {
                        LOGGER.warn("[ChickenFoodReloadListener] '{}': 'items' field is not a JSON object, skipping items section", fileId);
                    } else {
                        JsonObject itemsObj = itemsElement.getAsJsonObject();
                        for (Map.Entry<String, JsonElement> entry : itemsObj.entrySet()) {
                            String itemKey = entry.getKey();
                            try {
                                ChickenFoodItem food = parseChickenFoodItem(itemKey, entry.getValue(), fileId);
                                if (food == null) {
                                    errorCount++;
                                    continue;
                                }
                                if (itemKey.startsWith("#")) {
                                    // Tag syntax: expand tag to all items in the tag
                                    String tagId = itemKey.substring(1);
                                    TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(tagId));
                                    int tagItemCount = 0;
                                    for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                                        itemFoods.put(holder.value(), food);
                                        tagItemCount++;
                                    }
                                    if (tagItemCount == 0) {
                                        LOGGER.warn("[ChickenFoodReloadListener] '{}': item tag '{}' is empty or does not exist", fileId, itemKey);
                                    } else {
                                        itemCount += tagItemCount;
                                    }
                                } else {
                                    Item item = resolveItem(itemKey);
                                    if (item == null) {
                                        LOGGER.warn("[ChickenFoodReloadListener] '{}': unknown item '{}', skipping", fileId, itemKey);
                                        errorCount++;
                                        continue;
                                    }
                                    itemFoods.put(item, food);
                                    itemCount++;
                                }
                            } catch (Exception e) {
                                LOGGER.warn("[ChickenFoodReloadListener] '{}': failed to parse item food entry '{}': {}",
                                        fileId, itemKey, e.getMessage());
                                errorCount++;
                            }
                        }
                    }
                }

                // Parse fluid foods
                if (root.has("fluids")) {
                    JsonElement fluidsElement = root.get("fluids");
                    if (fluidsElement == null || !fluidsElement.isJsonObject()) {
                        LOGGER.warn("[ChickenFoodReloadListener] '{}': 'fluids' field is not a JSON object, skipping fluids section", fileId);
                    } else {
                        JsonObject fluidsObj = fluidsElement.getAsJsonObject();
                        for (Map.Entry<String, JsonElement> entry : fluidsObj.entrySet()) {
                            String fluidKey = entry.getKey();
                            try {
                                ChickenFoodFluid food = parseChickenFoodFluid(fluidKey, entry.getValue(), fileId);
                                if (food == null) {
                                    errorCount++;
                                    continue;
                                }
                                if (fluidKey.startsWith("#")) {
                                    // Tag syntax: expand tag to all fluids in the tag
                                    String tagId = fluidKey.substring(1);
                                    TagKey<Fluid> tag = TagKey.create(Registries.FLUID, new ResourceLocation(tagId));
                                    int tagFluidCount = 0;
                                    for (var holder : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
                                        fluidFoods.put(holder.value(), food);
                                        tagFluidCount++;
                                    }
                                    if (tagFluidCount == 0) {
                                        LOGGER.warn("[ChickenFoodReloadListener] '{}': fluid tag '{}' is empty or does not exist", fileId, fluidKey);
                                    } else {
                                        fluidCount += tagFluidCount;
                                    }
                                } else {
                                    Fluid fluid = resolveFluid(fluidKey);
                                    if (fluid == null) {
                                        LOGGER.warn("[ChickenFoodReloadListener] '{}': unknown fluid '{}', skipping", fileId, fluidKey);
                                        errorCount++;
                                        continue;
                                    }
                                    fluidFoods.put(fluid, food);
                                    fluidCount++;
                                }
                            } catch (Exception e) {
                                LOGGER.warn("[ChickenFoodReloadListener] '{}': failed to parse fluid food entry '{}': {}",
                                        fileId, fluidKey, e.getMessage());
                                errorCount++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[ChickenFoodReloadListener] '{}': failed to parse JSON file: {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        // If both maps are empty and no file explicitly used replace:true,
        // all entries were skipped (e.g. forge:conditions not met).
        // Preserve the hardcoded defaults from CIFChickenFoods.register().
        if (itemFoods.isEmpty() && fluidFoods.isEmpty() && !explicitReplace) {
            LOGGER.debug("[ChickenFoodReloadListener] All {} file(s) were skipped or yielded no entries, keeping hardcoded defaults", entries.size());
            return;
        }

        // Atomically replace all food maps in CIFChickenFoods
        CIFChickenFoods.reload(itemFoods, fluidFoods);

        LOGGER.info("[ChickenFoodReloadListener] Loaded {} item food(s) and {} fluid food(s) from {} file(s){}",
                itemCount, fluidCount, entries.size(),
                errorCount > 0 ? " (" + errorCount + " error(s) skipped)" : "");
    }

    // ---- Item food parsing ----

    /**
     * Parses a {@link ChickenFoodItem} from a JSON element.
     *
     * @param itemKey  the item registry key string (for logging)
     * @param element  the JSON element containing food properties
     * @param fileId   the source file resource location (for logging)
     * @return the parsed food, or {@code null} if parsing failed
     */
    private @Nullable ChickenFoodItem parseChickenFoodItem(String itemKey, JsonElement element, ResourceLocation fileId) {
        if (element == null || !element.isJsonObject()) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': item '{}' value is not a JSON object", fileId, itemKey);
            return null;
        }

        JsonObject obj = element.getAsJsonObject();

        // Parse required "progress" field
        if (!obj.has("progress")) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': item '{}' missing required 'progress' field", fileId, itemKey);
            return null;
        }
        IntProvider progress = parseIntProvider(obj.get("progress"), "progress", itemKey, fileId);
        if (progress == null) return null;

        // Parse required "cooldown" field
        if (!obj.has("cooldown")) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': item '{}' missing required 'cooldown' field", fileId, itemKey);
            return null;
        }
        IntProvider cooldown = parseIntProvider(obj.get("cooldown"), "cooldown", itemKey, fileId);
        if (cooldown == null) return null;

        // Parse optional "using_converts_to" field
        Optional<ItemStack> usingConvertsTo = Optional.empty();
        if (obj.has("using_converts_to")) {
            try {
                JsonElement convertsToElement = obj.get("using_converts_to");
                if (convertsToElement != null && !convertsToElement.isJsonNull()) {
                    ItemStack parsed = parseItemStack(convertsToElement, fileId, itemKey);
                    if (parsed != null) {
                        usingConvertsTo = Optional.of(parsed);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[ChickenFoodReloadListener] '{}': item '{}' failed to parse 'using_converts_to': {}",
                        fileId, itemKey, e.getMessage());
                // Non-fatal: proceed without conversion item
            }
        }

        // Validate progress range (0..12000, matching upstream codec constraint)
        try {
            int minProgress = progress.getMinValue();
            int maxProgress = progress.getMaxValue();
            if (minProgress < 0 || maxProgress > 12000) {
                LOGGER.warn("[ChickenFoodReloadListener] '{}': item '{}' progress range [{}, {}] exceeds allowed [0, 12000]",
                        fileId, itemKey, minProgress, maxProgress);
                return null;
            }
        } catch (Exception e) {
            // Some IntProvider implementations may not support getMinValue/getMaxValue cleanly
            LOGGER.debug("[ChickenFoodReloadListener] '{}': item '{}' could not validate progress range: {}",
                    fileId, itemKey, e.getMessage());
        }

        // Validate cooldown is non-negative
        try {
            if (cooldown.getMinValue() < 0) {
                LOGGER.warn("[ChickenFoodReloadListener] '{}': item '{}' cooldown min value {} is negative",
                        fileId, itemKey, cooldown.getMinValue());
                return null;
            }
        } catch (Exception e) {
            LOGGER.debug("[ChickenFoodReloadListener] '{}': item '{}' could not validate cooldown range: {}",
                    fileId, itemKey, e.getMessage());
        }

        return new ChickenFoodItem(progress, cooldown, usingConvertsTo);
    }

    // ---- Fluid food parsing ----

    /**
     * Parses a {@link ChickenFoodFluid} from a JSON element.
     *
     * @param fluidKey the fluid registry key string (for logging)
     * @param element  the JSON element containing food properties
     * @param fileId   the source file resource location (for logging)
     * @return the parsed food, or {@code null} if parsing failed
     */
    private @Nullable ChickenFoodFluid parseChickenFoodFluid(String fluidKey, JsonElement element, ResourceLocation fileId) {
        if (element == null || !element.isJsonObject()) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': fluid '{}' value is not a JSON object", fileId, fluidKey);
            return null;
        }

        JsonObject obj = element.getAsJsonObject();

        // Parse required "progress" field
        if (!obj.has("progress")) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': fluid '{}' missing required 'progress' field", fileId, fluidKey);
            return null;
        }
        IntProvider progress = parseIntProvider(obj.get("progress"), "progress", fluidKey, fileId);
        if (progress == null) return null;

        // Parse required "cooldown" field
        if (!obj.has("cooldown")) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': fluid '{}' missing required 'cooldown' field", fileId, fluidKey);
            return null;
        }
        IntProvider cooldown = parseIntProvider(obj.get("cooldown"), "cooldown", fluidKey, fileId);
        if (cooldown == null) return null;

        // Parse required "amount" field
        if (!obj.has("amount")) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': fluid '{}' missing required 'amount' field", fileId, fluidKey);
            return null;
        }
        int amount;
        try {
            amount = GsonHelper.getAsInt(obj, "amount");
        } catch (JsonSyntaxException e) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': fluid '{}' 'amount' is not a valid integer: {}",
                    fileId, fluidKey, e.getMessage());
            return null;
        }
        if (amount <= 0) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': fluid '{}' 'amount' must be positive, got {}",
                    fileId, fluidKey, amount);
            return null;
        }

        // Validate progress range (0..12000, matching upstream codec constraint)
        try {
            int minProgress = progress.getMinValue();
            int maxProgress = progress.getMaxValue();
            if (minProgress < 0 || maxProgress > 12000) {
                LOGGER.warn("[ChickenFoodReloadListener] '{}': fluid '{}' progress range [{}, {}] exceeds allowed [0, 12000]",
                        fileId, fluidKey, minProgress, maxProgress);
                return null;
            }
        } catch (Exception e) {
            LOGGER.debug("[ChickenFoodReloadListener] '{}': fluid '{}' could not validate progress range: {}",
                    fileId, fluidKey, e.getMessage());
        }

        // Validate cooldown is non-negative
        try {
            if (cooldown.getMinValue() < 0) {
                LOGGER.warn("[ChickenFoodReloadListener] '{}': fluid '{}' cooldown min value {} is negative",
                        fileId, fluidKey, cooldown.getMinValue());
                return null;
            }
        } catch (Exception e) {
            LOGGER.debug("[ChickenFoodReloadListener] '{}': fluid '{}' could not validate cooldown range: {}",
                    fileId, fluidKey, e.getMessage());
        }

        return new ChickenFoodFluid(progress, cooldown, amount);
    }

    // ---- IntProvider parsing ----

    /**
     * Parses an {@link IntProvider} from a JSON element.
     * <p>
     * Supports all standard Minecraft IntProvider types:
     * <ul>
     *   <li>{@code minecraft:constant} - a single constant value (also supports bare integer shorthand)</li>
     *   <li>{@code minecraft:uniform} - uniform random between min_inclusive and max_inclusive</li>
     *   <li>{@code minecraft:biased_to_bottom} - biased towards min_inclusive</li>
     *   <li>{@code minecraft:clamped} - delegates to a source provider, clamping the result</li>
     *   <li>{@code minecraft:clamped_normal} - normal distribution clamped to [min..max]</li>
     * </ul>
     *
     * @param element   the JSON element (integer literal or object with "type")
     * @param fieldName the field name for error messages
     * @param entryKey  the entry key for error messages
     * @param fileId    the source file ID for error messages
     * @return the parsed IntProvider, or {@code null} if parsing failed
     */
    private @Nullable IntProvider parseIntProvider(JsonElement element, String fieldName, String entryKey, ResourceLocation fileId) {
        if (element == null) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' is null", fileId, entryKey, fieldName);
            return null;
        }

        try {
            // Shorthand: bare integer is treated as constant
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return ConstantInt.of(element.getAsInt());
            }

            if (!element.isJsonObject()) {
                LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' must be an integer or JSON object, got {}",
                        fileId, entryKey, fieldName, element.getClass().getSimpleName());
                return null;
            }

            JsonObject obj = element.getAsJsonObject();

            if (!obj.has("type")) {
                // If no type field, check for "value" (implicit constant)
                if (obj.has("value")) {
                    return ConstantInt.of(GsonHelper.getAsInt(obj, "value"));
                }
                // Check for min/max (implicit uniform)
                if (obj.has("min_inclusive") && obj.has("max_inclusive")) {
                    int min = GsonHelper.getAsInt(obj, "min_inclusive");
                    int max = GsonHelper.getAsInt(obj, "max_inclusive");
                    if (min > max) {
                        LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' uniform min ({}) > max ({})",
                                fileId, entryKey, fieldName, min, max);
                        return null;
                    }
                    return UniformInt.of(min, max);
                }
                LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' object missing 'type' field",
                        fileId, entryKey, fieldName);
                return null;
            }

            String type = GsonHelper.getAsString(obj, "type");
            if (type == null || type.isEmpty()) {
                LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' has empty 'type'",
                        fileId, entryKey, fieldName);
                return null;
            }

            return parseIntProviderByType(type, obj, fieldName, entryKey, fileId);
        } catch (Exception e) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' failed to parse IntProvider: {}",
                    fileId, entryKey, fieldName, e.getMessage());
            return null;
        }
    }

    /**
     * Parses an IntProvider by its explicit type string.
     */
    private @Nullable IntProvider parseIntProviderByType(String type, JsonObject obj,
                                                          String fieldName, String entryKey, ResourceLocation fileId) {
        switch (type) {
            case "minecraft:constant":
            case "constant": {
                if (!obj.has("value")) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' constant type missing 'value'",
                            fileId, entryKey, fieldName);
                    return null;
                }
                return ConstantInt.of(GsonHelper.getAsInt(obj, "value"));
            }

            case "minecraft:uniform":
            case "uniform": {
                if (!obj.has("min_inclusive") || !obj.has("max_inclusive")) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' uniform type missing 'min_inclusive' or 'max_inclusive'",
                            fileId, entryKey, fieldName);
                    return null;
                }
                int min = GsonHelper.getAsInt(obj, "min_inclusive");
                int max = GsonHelper.getAsInt(obj, "max_inclusive");
                if (min > max) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' uniform min ({}) > max ({})",
                            fileId, entryKey, fieldName, min, max);
                    return null;
                }
                return UniformInt.of(min, max);
            }

            case "minecraft:biased_to_bottom":
            case "biased_to_bottom": {
                if (!obj.has("min_inclusive") || !obj.has("max_inclusive")) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' biased_to_bottom type missing 'min_inclusive' or 'max_inclusive'",
                            fileId, entryKey, fieldName);
                    return null;
                }
                int min = GsonHelper.getAsInt(obj, "min_inclusive");
                int max = GsonHelper.getAsInt(obj, "max_inclusive");
                if (min > max) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' biased_to_bottom min ({}) > max ({})",
                            fileId, entryKey, fieldName, min, max);
                    return null;
                }
                return net.minecraft.util.valueproviders.BiasedToBottomInt.of(min, max);
            }

            case "minecraft:clamped":
            case "clamped": {
                if (!obj.has("source") || !obj.has("min_inclusive") || !obj.has("max_inclusive")) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' clamped type missing 'source', 'min_inclusive', or 'max_inclusive'",
                            fileId, entryKey, fieldName);
                    return null;
                }
                IntProvider source = parseIntProvider(obj.get("source"), fieldName + ".source", entryKey, fileId);
                if (source == null) return null;
                int min = GsonHelper.getAsInt(obj, "min_inclusive");
                int max = GsonHelper.getAsInt(obj, "max_inclusive");
                if (min > max) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' clamped min ({}) > max ({})",
                            fileId, entryKey, fieldName, min, max);
                    return null;
                }
                return net.minecraft.util.valueproviders.ClampedInt.of(source, min, max);
            }

            case "minecraft:clamped_normal":
            case "clamped_normal": {
                if (!obj.has("mean") || !obj.has("deviation") || !obj.has("min_inclusive") || !obj.has("max_inclusive")) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' clamped_normal type missing required fields (need 'mean', 'deviation', 'min_inclusive', 'max_inclusive')",
                            fileId, entryKey, fieldName);
                    return null;
                }
                float mean = GsonHelper.getAsFloat(obj, "mean");
                float deviation = GsonHelper.getAsFloat(obj, "deviation");
                int min = GsonHelper.getAsInt(obj, "min_inclusive");
                int max = GsonHelper.getAsInt(obj, "max_inclusive");
                if (min > max) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' clamped_normal min ({}) > max ({})",
                            fileId, entryKey, fieldName, min, max);
                    return null;
                }
                return net.minecraft.util.valueproviders.ClampedNormalInt.of(mean, deviation, min, max);
            }

            default:
                LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' field '{}' unknown IntProvider type '{}'",
                        fileId, entryKey, fieldName, type);
                return null;
        }
    }

    // ---- ItemStack parsing ----

    /**
     * Parses an {@link ItemStack} from a JSON element.
     * Supports both string shorthand ({@code "minecraft:bowl"}) and object form
     * ({@code {"item": "minecraft:bowl", "count": 1}}).
     *
     * @param element the JSON element
     * @param fileId  source file (for logging)
     * @param context context key (for logging)
     * @return the parsed ItemStack, or {@code null} if parsing failed
     */
    private @Nullable ItemStack parseItemStack(JsonElement element, ResourceLocation fileId, String context) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        try {
            // String shorthand: just an item ID
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String itemId = element.getAsString();
                Item item = resolveItem(itemId);
                if (item == null || item == Items.AIR) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' using_converts_to references unknown item '{}'",
                            fileId, context, itemId);
                    return null;
                }
                return new ItemStack(item);
            }

            // Object form: { "item": "...", "count": N }
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("item")) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' using_converts_to object missing 'item' field",
                            fileId, context);
                    return null;
                }
                String itemId = GsonHelper.getAsString(obj, "item");
                Item item = resolveItem(itemId);
                if (item == null || item == Items.AIR) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' using_converts_to references unknown item '{}'",
                            fileId, context, itemId);
                    return null;
                }
                int count = GsonHelper.getAsInt(obj, "count", 1);
                if (count <= 0) {
                    LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' using_converts_to has non-positive count {}",
                            fileId, context, count);
                    return null;
                }
                return new ItemStack(item, count);
            }

            LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' using_converts_to has unexpected type: {}",
                    fileId, context, element.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            LOGGER.warn("[ChickenFoodReloadListener] '{}': entry '{}' failed to parse using_converts_to: {}",
                    fileId, context, e.getMessage());
            return null;
        }
    }

    // ---- Registry resolution ----

    /**
     * Resolves an item registry name to an {@link Item}.
     *
     * @param key the registry name string (e.g. "minecraft:wheat_seeds")
     * @return the resolved Item, or {@code null} if the key is invalid or the item is air/not found
     */
    private @Nullable Item resolveItem(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        try {
            ResourceLocation loc = new ResourceLocation(key);
            Item item = BuiltInRegistries.ITEM.get(loc);
            // BuiltInRegistries.ITEM.get() returns Items.AIR for unknown keys
            if (item == Items.AIR && !"minecraft:air".equals(key)) {
                return null;
            }
            return item;
        } catch (Exception e) {
            LOGGER.warn("[ChickenFoodReloadListener] Invalid item key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Resolves a fluid registry name to a {@link Fluid}.
     *
     * @param key the registry name string (e.g. "createadditions:seed_oil")
     * @return the resolved Fluid, or {@code null} if the key is invalid or the fluid is empty/not found
     */
    private @Nullable Fluid resolveFluid(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        try {
            ResourceLocation loc = new ResourceLocation(key);
            Fluid fluid = BuiltInRegistries.FLUID.get(loc);
            // BuiltInRegistries.FLUID.get() returns Fluids.EMPTY for unknown keys
            if (fluid == Fluids.EMPTY) {
                return null;
            }
            return fluid;
        } catch (Exception e) {
            LOGGER.warn("[ChickenFoodReloadListener] Invalid fluid key '{}': {}", key, e.getMessage());
            return null;
        }
    }
}
