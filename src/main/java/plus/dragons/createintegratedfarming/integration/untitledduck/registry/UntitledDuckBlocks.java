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

import static plus.dragons.createintegratedfarming.common.CIFCommon.REGISTRATE;
import static plus.dragons.createintegratedfarming.common.registry.CIFBlocks.ROOST;

import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.SoundType;
import plus.dragons.createintegratedfarming.integration.untitledduck.ranching.roost.duck.DuckRoostBlock;
import plus.dragons.createintegratedfarming.integration.untitledduck.ranching.roost.goose.GooseRoostBlock;

public class UntitledDuckBlocks {
    public static final BlockEntry<DuckRoostBlock> DUCK_ROOST_NORMAL = registerDuckRoost("duck_roost_normal", (byte) 0);
    public static final BlockEntry<DuckRoostBlock> DUCK_ROOST_FEMALE = registerDuckRoost("duck_roost_female", (byte) 1);
    public static final BlockEntry<DuckRoostBlock> DUCK_ROOST_CAMPBELL = registerDuckRoost("duck_roost_campbell", (byte) 2);
    public static final BlockEntry<DuckRoostBlock> DUCK_ROOST_PEKIN = registerDuckRoost("duck_roost_pekin", (byte) 3);
    public static final BlockEntry<GooseRoostBlock> GOOSE_ROOST_NORMAL = registerGooseRoost("goose_roost_normal", (byte) 0);
    public static final BlockEntry<GooseRoostBlock> GOOSE_ROOST_CANADIAN = registerGooseRoost("goose_roost_canadian", (byte) 1);
    public static final BlockEntry<GooseRoostBlock> GOOSE_ROOST_PING = registerGooseRoost("goose_roost_ping", (byte) 2);
    public static final BlockEntry<GooseRoostBlock> GOOSE_ROOST_SUS = registerGooseRoost("goose_roost_sus", (byte) 3);
    public static final BlockEntry<GooseRoostBlock> GOOSE_ROOST_UNTITLED = registerGooseRoost("goose_roost_untitled", (byte) 4);

    private static BlockEntry<DuckRoostBlock> registerDuckRoost(String path, byte variant) {
        return REGISTRATE.block(path, prop -> new DuckRoostBlock(prop, ROOST, variant))
                .lang("Duck Roost")
                .properties(prop -> prop.strength(1.5F).sound(SoundType.BAMBOO_WOOD))
                .blockstate((ctx, prov) -> prov.horizontalBlock(ctx.get(), AssetLookup.standardModel(ctx, prov)))
                .item()
                .build()
                .register();
    }

    private static BlockEntry<GooseRoostBlock> registerGooseRoost(String path, byte variant) {
        return REGISTRATE.block(path, prop -> new GooseRoostBlock(prop, ROOST, variant))
                .lang("Goose Roost")
                .properties(prop -> prop.strength(1.5F).sound(SoundType.BAMBOO_WOOD))
                .blockstate((ctx, prov) -> prov.horizontalBlock(ctx.get(), AssetLookup.standardModel(ctx, prov)))
                .item()
                .build()
                .register();
    }

    /**
     * Adds all duck/goose roost variants to the given creative tab output.
     * Called from {@link plus.dragons.createintegratedfarming.integration.untitledduck.UntitledDuckIntegration}.
     */
    public static void addCreativeTabItems(CreativeModeTab.Output output) {
        output.accept(DUCK_ROOST_NORMAL);
        output.accept(DUCK_ROOST_FEMALE);
        output.accept(DUCK_ROOST_CAMPBELL);
        output.accept(DUCK_ROOST_PEKIN);
        output.accept(GOOSE_ROOST_NORMAL);
        output.accept(GOOSE_ROOST_CANADIAN);
        output.accept(GOOSE_ROOST_PING);
        output.accept(GOOSE_ROOST_SUS);
        output.accept(GOOSE_ROOST_UNTITLED);
    }

    public static void register() {
        // Force class loading
    }
}
