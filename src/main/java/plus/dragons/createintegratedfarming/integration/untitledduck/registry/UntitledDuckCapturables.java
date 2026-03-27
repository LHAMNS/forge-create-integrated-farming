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

package plus.dragons.createintegratedfarming.integration.untitledduck.registry;

import net.untitledduckmod.common.init.ModEntityTypes;
import plus.dragons.createintegratedfarming.common.ranching.roost.RoostCapturable;
import plus.dragons.createintegratedfarming.integration.untitledduck.ranching.roost.duck.DuckRoostBlock;
import plus.dragons.createintegratedfarming.integration.untitledduck.ranching.roost.goose.GooseRoostBlock;

public class UntitledDuckCapturables {
    public static void register() {
        RoostCapturable.REGISTRY.register(ModEntityTypes.DUCK.get(), new DuckRoostBlock.Capturable());
        RoostCapturable.REGISTRY.register(ModEntityTypes.GOOSE.get(), new GooseRoostBlock.Capturable());
    }
}
