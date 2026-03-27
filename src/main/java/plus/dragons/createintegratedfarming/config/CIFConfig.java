/*
 * Copyright (C) 2025  DragonsPlus
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Ported from NeoForge 1.21.1 to Forge 1.20.1
 */
package plus.dragons.createintegratedfarming.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class CIFConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final CIFServerConfig SERVER;

    static {
        Pair<CIFServerConfig, ForgeConfigSpec> serverPair = new ForgeConfigSpec.Builder().configure(CIFServerConfig::new);
        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }

    public static CIFServerConfig server() { return SERVER; }
}
