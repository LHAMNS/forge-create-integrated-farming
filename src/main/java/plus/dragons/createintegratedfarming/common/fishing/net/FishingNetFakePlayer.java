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

package plus.dragons.createintegratedfarming.common.fishing.net;

import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.FakePlayer;

/**
 * Fake player used as the "caster" for fishing net loot context.
 * Shared per-dimension to avoid creating thousands of heavyweight ServerPlayer
 * instances when many fishing nets are active (common in large automation setups).
 * <p>
 * Thread-safety: Minecraft's server tick is single-threaded. Pool access occurs
 * only from the server tick thread. Cleanup is triggered by LevelEvent.Unload
 * (see CIFCommon event registration).
 */
public class FishingNetFakePlayer extends FakePlayer {
    public static final GameProfile FISHING_NET_PROFILE = new GameProfile(UUID.fromString("e538508b-ef48-405b-98b4-b99d853fd961"), "Fishing Net");

    /**
     * Per-dimension shared instance pool. Uses HashMap with explicit cleanup
     * via {@link #onLevelUnload} for deterministic lifecycle management,
     * following the same pattern as Forge's FakePlayerFactory.
     */
    private static final Map<ServerLevel, FishingNetFakePlayer> POOL = new HashMap<>();

    private FishingNetFakePlayer(ServerLevel level) {
        super(level, FISHING_NET_PROFILE);
    }

    /**
     * Returns a shared FakePlayer for the given level, resetting transient state
     * to prevent cross-fishing-net pollution from loot functions or event handlers
     * that may modify the player (inventory, potion effects, attributes, etc.).
     * <p>
     * Position must be set by the caller before use.
     */
    public static FishingNetFakePlayer get(ServerLevel level) {
        FishingNetFakePlayer player = POOL.computeIfAbsent(level, FishingNetFakePlayer::new);
        resetState(player);
        return player;
    }

    /**
     * Removes the cached FakePlayer for a dimension being unloaded.
     * Called from the LevelEvent.Unload handler in CIFCommon.
     */
    public static void onLevelUnload(ServerLevel level) {
        POOL.remove(level);
    }

    /**
     * Clears the entire pool. Called on server shutdown (ServerStoppingEvent)
     * to ensure no stale references survive a crash or restart cycle.
     */
    public static void clearAll() {
        POOL.clear();
    }

    /**
     * Resets mutable transient state that could leak between fishing net uses.
     * Only resets fields that loot functions or event handlers might reasonably modify.
     */
    private static void resetState(FishingNetFakePlayer player) {
        // Clear inventory — loot functions could add items via SetPlayerAction or similar
        player.getInventory().clearContent();
        // Remove all potion effects — event handlers might apply effects to the killer
        player.removeAllEffects();
        // Reset fishing hook reference — LavaFishingBobberEntity constructor sets player.fishing
        player.fishing = null;
    }
}
