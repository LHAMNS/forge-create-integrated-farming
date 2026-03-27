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

package plus.dragons.createintegratedfarming.integration.delightoflight.ponder;

import com.cloudmeow.delightoflight.registry.DFBlocks;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class DelightOFlightPonderScenes {
    public static void catalyze(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("lightning_rod.charge_soil", "Charging Soil");
        scene.configureBasePlate(0, 0, 5);
        var inner = util.select().fromTo(1, 0, 1, 3, 0, 3);
        scene.world().showSection(util.select().layer(0).substract(inner), Direction.DOWN);
        var fakeBase = scene.world().showIndependentSection(util.select().fromTo(1, 2, 1, 3, 2, 3), Direction.DOWN);
        scene.world().moveSection(fakeBase, util.vector().of(0, -2, 0), 0);
        scene.idle(15);
        scene.world().hideIndependentSection(fakeBase, Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(inner, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);

        scene.overlay().showText(100)
                .text("Charging process of Soil to Weather Soil can be completed via Lightning Rod by Lightning Strike")
                .pointAt(util.vector().centerOf(2, 1, 2))
                .attachKeyFrame()
                .placeNearTarget();

        scene.idle(20);
        scene.world().createEntity(level -> {
            var lightning = EntityType.LIGHTNING_BOLT.create(level);
            lightning.moveTo(Vec3.atBottomCenterOf(util.grid().at(2, 1, 2)));
            return lightning;
        });
        scene.world().replaceBlocks(util.select().position(1, 0, 2).add(util.select().position(2, 0, 1)).add(util.select().position(2, 0, 2)), DFBlocks.WEATHER_SOIL.get().defaultBlockState(), false);
        scene.world().replaceBlocks(util.select().position(3, 0, 2).add(util.select().position(2, 0, 3)), DFBlocks.WEATHER_SOIL_FARMLAND.get().defaultBlockState(), false);
        scene.idle(80);
    }
}
