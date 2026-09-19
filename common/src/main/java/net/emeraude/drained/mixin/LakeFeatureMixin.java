package net.emeraude.drained.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.emeraude.drained.DrainedFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rules 1 and 2, part four — lakes are dug but not filled.
 *
 * <p>{@code lake_lava_surface} and {@code lake_lava_underground} are features, so like springs they
 * survive an emptied fluid picker. In vanilla they are the visible lava pools you fall into on the
 * surface, and each one is a portal frame waiting to happen.
 *
 * <p>The condition is on the block write rather than on the whole feature on purpose.
 * {@code LakeFeature.place} does three different writes with this call: the fluid, the air above
 * it, and the stone barrier that holds the lake in (plus ice, for water lakes). Filtering only the
 * fluid keeps the basin and its barrier exactly where vanilla put them, so a drained lake reads as
 * a dry crater rather than as a missing feature — the same treatment the oceans get.
 *
 * <p>Reading the lake's fluid from its configuration was not an option: {@code BlockStateProvider}
 * {@code .getState} takes {@code (RandomSource, BlockPos)} on 1.21.1 and
 * {@code (LevelReader, RandomSource, BlockPos)} on 26.2. The state arriving at {@code setBlock} is
 * the same information, one step later, and its descriptor —
 * {@code WorldGenLevel.setBlock(BlockPos, BlockState, int)Z} — is identical on both lines
 * (verified in the compiled bytecode of each).
 */
@Mixin(LakeFeature.class)
public abstract class LakeFeatureMixin {

    @WrapWithCondition(
            method = "place",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/WorldGenLevel;setBlock("
                                            + "Lnet/minecraft/core/BlockPos;"
                                            + "Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean drained$noLakeFluid(
            WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
        return !DrainedFluids.isDrained(state);
    }
}
