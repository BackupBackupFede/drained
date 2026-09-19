package net.emeraude.drained.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.emeraude.drained.DrainedConfig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rules 1 and 2, part one — the world stops being filled with fluid.
 *
 * <p>{@code createFluidPicker} builds the three fluid levels every noise chunk generator hands to
 * its aquifers, and there is nothing else to it:
 *
 * <pre>
 *   lava  below y=-54          -&gt; Blocks.LAVA           (the deep lava floor)
 *   settings.defaultFluid()    -&gt; at sea level          (oceans/rivers, and the Nether's lava sea)
 *   air   below the world      -&gt; Blocks.AIR
 * </pre>
 *
 * <p>Both fluid-bearing expressions are neutralised here, once per chunk generator: the generator
 * memoizes its picker, so these run at world load and cost nothing per chunk afterwards.
 *
 * <p>This is also what drains the <b>Nether</b>. The Nether generates with aquifers disabled, so
 * it never reaches {@link AquiferMixin}; its lava sea is purely {@code defaultFluid()} at sea
 * level 32, and killing that empties it. The End answers air here already and is untouched.
 *
 * <p>Two reasons this is a call-site interception rather than a mixin on
 * {@code NoiseGeneratorSettings.defaultFluid()}: the settings object is a record whose accessor is
 * also the codec's getter, so overriding it would rewrite the value on the way out to disk as
 * well; and {@code createFluidPicker} is the accessor's only caller in either version.
 *
 * <p>Cross-version note: {@code createFluidPicker} is private static on both 1.21.1 and 26.2, and
 * both injection descriptors — {@code NoiseGeneratorSettings.defaultFluid()BlockState} and the
 * {@code Blocks.LAVA} field get — are byte-for-byte identical on both, so one mixin in
 * {@code common/} covers all four targets. Vanilla 26.2 does exactly this itself behind its
 * {@code DEBUG_DISABLE_FLUID_GENERATION} flag, which is a good sign that an empty picker is a
 * shape the generator expects.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {

    /**
     * The sea. Water in the Overworld, lava in the Nether — whichever it is, it stops being placed
     * and the basin is left open to walk into.
     */
    @ModifyExpressionValue(
            method = "createFluidPicker",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;"
                                            + "defaultFluid()Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState drained$noSeaFluid(BlockState original) {
        // Anything that is not water or lava belongs to a generator we know nothing about.
        if (original.is(Blocks.WATER) && DrainedConfig.drainWater()) {
            return Blocks.AIR.defaultBlockState();
        }
        if (original.is(Blocks.LAVA) && DrainedConfig.drainLava()) {
            return Blocks.AIR.defaultBlockState();
        }
        return original;
    }

    /**
     * The deep lava floor below y=-54. It is a hard-coded {@code Blocks.LAVA} rather than part of
     * the settings, and {@code computeSubstance} short-circuits straight to lava whenever the
     * global picker answers lava — so without this the bottom of every Overworld cave system would
     * still be a lava sea.
     */
    @ModifyExpressionValue(
            method = "createFluidPicker",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/world/level/block/Blocks;LAVA:Lnet/minecraft/world/level/block/Block;",
                            opcode = Opcodes.GETSTATIC),
            require = 1)
    private static Block drained$noDeepLava(Block original) {
        return DrainedConfig.drainLava() ? Blocks.AIR : original;
    }
}
