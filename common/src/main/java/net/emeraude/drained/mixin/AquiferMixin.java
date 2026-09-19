package net.emeraude.drained.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.emeraude.drained.DrainedFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rules 1 and 2, part two — the underground aquifers stop making their own fluid.
 *
 * <p>Emptying the global fluid picker ({@link NoiseBasedChunkGeneratorMixin}) is not enough on its
 * own. {@code NoiseBasedAquifer} decides the fluid of each aquifer cell in
 * {@code computeFluidType}, and that method reaches for {@code lavaNoise} independently of the
 * picker: below y=-10 it flips a cell to lava whenever the noise is strong enough. Worse, the
 * branch is guarded by "the global fluid here is <em>not</em> lava" — so draining the picker
 * <em>enables</em> it more often rather than less.
 *
 * <p>{@code computeFluidType} is where every aquifer fluid comes from, and it is called once per
 * aquifer grid cell and cached, not once per block, so this is a cold path.
 *
 * <p>Between the picker and this method there is no third source: every {@code BlockState} that
 * {@code computeSubstance} can return is either the picker's answer or this one.
 *
 * <p>Cross-version note: {@code Aquifer.NoiseBasedAquifer} is a public static member on 1.21.1 and
 * an (implicitly public) interface member class on 26.2, and
 * {@code computeFluidType(int, int, int, Aquifer.FluidStatus, int)} has the same descriptor on
 * both. Only the enclosing type is ever named, never {@code FluidStatus}'s accessor — which is a
 * plain field on 1.21.1 and a record component on 26.2.
 */
@Mixin(Aquifer.NoiseBasedAquifer.class)
public abstract class AquiferMixin {

    @ModifyReturnValue(method = "computeFluidType", at = @At("RETURN"))
    private BlockState drained$noAquiferFluid(BlockState original) {
        return DrainedFluids.isDrained(original) ? Blocks.AIR.defaultBlockState() : original;
    }
}
