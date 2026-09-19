package net.emeraude.drained;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The single answer to "should this block state exist in a generated chunk?", shared by every
 * worldgen mixin so the two config switches mean the same thing everywhere.
 *
 * <p>Only water and lava are ever considered. Anything else — including a modded generator's own
 * default fluid — is left alone, which keeps this mod out of the blast radius of dimensions it
 * knows nothing about.
 *
 * <p>Server-side only, like the rest of {@code common/}.
 */
public final class DrainedFluids {

    private DrainedFluids() {}

    /** Whether {@code state} is a fluid this mod is configured to remove from world generation. */
    public static boolean isDrained(BlockState state) {
        if (state.is(Blocks.WATER)) return DrainedConfig.drainWater();
        if (state.is(Blocks.LAVA)) return DrainedConfig.drainLava();
        return false;
    }
}
