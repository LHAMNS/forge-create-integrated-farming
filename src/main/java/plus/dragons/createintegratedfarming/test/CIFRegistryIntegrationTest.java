/*
 * Copyright (C) 2025  DragonsPlus
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package plus.dragons.createintegratedfarming.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.registries.ForgeRegistries;
import plus.dragons.createintegratedfarming.api.harvester.CustomHarvestBehaviour;
import plus.dragons.createintegratedfarming.api.saw.SawableBlockTags;
import plus.dragons.createintegratedfarming.common.registry.CIFBlocks;
import plus.dragons.createintegratedfarming.common.registry.CIFBlockEntities;
import plus.dragons.createintegratedfarming.common.registry.CIFChickenFoods;
import plus.dragons.createintegratedfarming.common.ranching.roost.chicken.ChickenFoodItem;
import plus.dragons.createintegratedfarming.config.CIFConfig;
import plus.dragons.createintegratedfarming.integration.ModIntegration;

/**
 * Runtime registry and API integration tests for Create: Integrated Farming.
 * Verifies block/entity/food registrations, API contracts, and cross-module dependencies.
 */
@GameTestHolder("create_integrated_farming")
public class CIFRegistryIntegrationTest {

    /**
     * Verify all core blocks are registered.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testAllBlocksRegistered(GameTestHelper helper) {
        List<String> failures = new ArrayList<>();

        Block fishingNet = CIFBlocks.FISHING_NET.get();
        if (fishingNet == null || ForgeRegistries.BLOCKS.getKey(fishingNet) == null)
            failures.add("FISHING_NET");

        Block roost = CIFBlocks.ROOST.get();
        if (roost == null || ForgeRegistries.BLOCKS.getKey(roost) == null)
            failures.add("ROOST");

        Block chickenRoost = CIFBlocks.CHICKEN_ROOST.get();
        if (chickenRoost == null || ForgeRegistries.BLOCKS.getKey(chickenRoost) == null)
            failures.add("CHICKEN_ROOST");

        if (!failures.isEmpty()) {
            helper.fail("Missing blocks: " + String.join(", ", failures));
            return;
        }
        helper.succeed();
    }

    /**
     * Verify FishingNetBlock has the WATERLOGGED property.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testFishingNetHasWaterloggedProperty(GameTestHelper helper) {
        Block fishingNet = CIFBlocks.FISHING_NET.get();
        if (!fishingNet.defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED)) {
            helper.fail("FishingNetBlock is missing WATERLOGGED property");
            return;
        }
        helper.succeed();
    }

    /**
     * Verify ChickenRoost block entity type is registered and associated with the correct block.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testChickenRoostBlockEntityRegistered(GameTestHelper helper) {
        if (CIFBlockEntities.CHICKEN_ROOST.get() == null) {
            helper.fail("CHICKEN_ROOST block entity type is null");
            return;
        }
        // Verify the block entity type is valid for the chicken roost block
        if (!CIFBlockEntities.CHICKEN_ROOST.get().isValid(CIFBlocks.CHICKEN_ROOST.getDefaultState())) {
            helper.fail("CHICKEN_ROOST block entity type not valid for ChickenRoost block");
            return;
        }
        helper.succeed();
    }

    /**
     * Verify chicken food system has hardcoded defaults loaded.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testChickenFoodDefaultsLoaded(GameTestHelper helper) {
        Map<Item, ChickenFoodItem> foods = CIFChickenFoods.getItemFoods();
        if (foods == null || foods.isEmpty()) {
            helper.fail("Chicken food item map is empty — hardcoded defaults not loaded");
            return;
        }
        // Wheat seeds should be a default chicken food
        ChickenFoodItem seedFood = CIFChickenFoods.getItemFood(new ItemStack(Items.WHEAT_SEEDS));
        if (seedFood == null) {
            helper.fail("Wheat seeds not recognized as chicken food");
            return;
        }
        helper.succeed();
    }

    /**
     * Verify the CustomHarvestBehaviour registry is initialized and accessible.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testCustomHarvestBehaviourRegistryExists(GameTestHelper helper) {
        if (CustomHarvestBehaviour.REGISTRY == null) {
            helper.fail("CustomHarvestBehaviour.REGISTRY is null");
            return;
        }
        helper.succeed();
    }

    /**
     * Verify SawableBlockTags constants are non-null.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testSawableBlockTagsExist(GameTestHelper helper) {
        if (SawableBlockTags.VERTICAL_PLANTS == null) {
            helper.fail("SawableBlockTags.VERTICAL_PLANTS is null");
            return;
        }
        if (SawableBlockTags.FRAGILE_VERTICAL_PLANTS == null) {
            helper.fail("SawableBlockTags.FRAGILE_VERTICAL_PLANTS is null");
            return;
        }
        helper.succeed();
    }

    /**
     * Verify ModIntegration enum values are accessible and their id() method works.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testModIntegrationEnum(GameTestHelper helper) {
        List<String> failures = new ArrayList<>();

        for (ModIntegration integration : ModIntegration.values()) {
            if (integration.id() == null || integration.id().isEmpty()) {
                failures.add(integration.name() + " has null/empty id");
            }
        }

        // Verify specific well-known mod IDs
        if (!ModIntegration.FARMERS_DELIGHT.id().equals("farmersdelight")) {
            failures.add("FARMERS_DELIGHT id mismatch: " + ModIntegration.FARMERS_DELIGHT.id());
        }

        if (!failures.isEmpty()) {
            helper.fail("ModIntegration failures: " + String.join(", ", failures));
            return;
        }
        helper.succeed();
    }

    /**
     * Verify CIFConfig server config is accessible.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testServerConfigAccessible(GameTestHelper helper) {
        if (CIFConfig.server() == null) {
            helper.fail("CIFConfig.server() is null");
            return;
        }
        helper.succeed();
    }

    /**
     * Verify ChickenFoods tag is non-null.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testChickenFoodTagExists(GameTestHelper helper) {
        if (CIFChickenFoods.CHICKEN_FOOD_TAG == null) {
            helper.fail("CHICKEN_FOOD_TAG is null");
            return;
        }
        helper.succeed();
    }

    /**
     * Verify CDPRegistrate cross-module dependency: CIF should be able to access
     * Create: Dragons Plus' shared utility classes at runtime.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void testCDPDependencyAccessible(GameTestHelper helper) {
        try {
            // Verify CDPRegistrate is accessible
            Class<?> registrateClass = Class.forName("plus.dragons.createdragonsplus.common.CDPRegistrate");
            if (registrateClass == null) {
                helper.fail("CDPRegistrate class not found");
                return;
            }

            // Verify BehaviourProvider interface is accessible
            Class<?> behaviourProvider = Class.forName("plus.dragons.createdragonsplus.common.behaviours.BehaviourProvider");
            if (behaviourProvider == null || !behaviourProvider.isInterface()) {
                helper.fail("BehaviourProvider is not an accessible interface");
                return;
            }

            // Verify WaterAndLavaLoggedBlock is accessible
            Class<?> waterLavaBlock = Class.forName("plus.dragons.createdragonsplus.common.fluids.WaterAndLavaLoggedBlock");
            if (waterLavaBlock == null || !waterLavaBlock.isInterface()) {
                helper.fail("WaterAndLavaLoggedBlock is not an accessible interface");
                return;
            }
        } catch (ClassNotFoundException e) {
            helper.fail("CDP dependency class not found: " + e.getMessage());
            return;
        }
        helper.succeed();
    }
}
