package net.emeraude.drained.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import net.emeraude.drained.DrainedFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.DeltaFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rule 2, part five — the basalt deltas keep their shape and lose their lava.
 *
 * <p>The delta feature paints two block states: {@code contents}, which is lava, and {@code rim},
 * which is magma. Filtering only the fluid write leaves the magma rims standing around empty
 * craters — the Nether's own version of a drained ocean, and the last place lava could still have
 * survived the picker.
 *
 * <p>Cross-version note: deltas write through the protected helper inherited from {@code Feature}
 * rather than through the level directly. The owner in the constant pool is {@code DeltaFeature},
 * not {@code Feature} — javac emits the static type of the receiver, and {@code this} is a
 * {@code DeltaFeature} — so naming {@code Feature} here matches nothing at all. Verified in the
 * compiled bytecode of both lines, where the entry is identical.
 */
@Mixin(DeltaFeature.class)
public abstract class DeltaFeatureMixin {

    @WrapWithCondition(
            method = "place",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/feature/DeltaFeature;setBlock("
                                            + "Lnet/minecraft/world/level/LevelWriter;"
                                            + "Lnet/minecraft/core/BlockPos;"
                                            + "Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private boolean drained$noDeltaLava(
            DeltaFeature instance, LevelWriter level, BlockPos pos, BlockState state) {
        return !DrainedFluids.isDrained(state);
    }
}
