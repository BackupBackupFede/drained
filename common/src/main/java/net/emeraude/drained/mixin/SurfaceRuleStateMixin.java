package net.emeraude.drained.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.emeraude.drained.DrainedFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rules 1 and 2, part eight — the surface rules stop painting lava into the Nether's floor.
 *
 * <p>The last hiding place, and the least obvious one. The Nether's noise settings carry a
 * <em>surface rule</em> that ends in:
 *
 * <pre>
 *   { "if_true": { "type": "minecraft:hole" },
 *     "then_run": { "type": "minecraft:block", "result_state": { "Name": "minecraft:lava" } } }
 * </pre>
 *
 * <p>That is a fourth mechanism, independent of the fluid picker, the aquifers, the features and
 * the carvers — which is why lava survived all seven other rules and turned up as flat one- and
 * two-block-deep pools at y 21-23, sitting in plain netherrack with no structure and no cave air
 * anywhere near them.
 *
 * <p>{@code StateRule} is the leaf of the whole surface-rule tree: every {@code minecraft:block}
 * rule source, in every dimension, hands its state out through here. Filtering it for water and
 * lava alone leaves grass, sand, terracotta banding and everything else exactly as vanilla has it.
 *
 * <p>Air rather than {@code null} on purpose: {@code null} means "this rule does not apply", so the
 * enclosing sequence would fall through to the next rule and fill the hole with netherrack. Air
 * keeps vanilla's hole where vanilla put it and merely leaves it dry — the same choice made for
 * the oceans, the lakes and the caves.
 *
 * <p>Cross-version note: {@code SurfaceRules.StateRule} is a private record on 26.2 and a
 * package-private one on 1.21.1, so it cannot be named as a class literal and is targeted by
 * binary name instead. {@code tryApply(int, int, int)} has the same descriptor on both lines.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$StateRule")
public abstract class SurfaceRuleStateMixin {

    @ModifyReturnValue(method = "tryApply", at = @At("RETURN"), require = 1)
    private BlockState drained$noSurfaceFluid(BlockState original) {
        if (original == null) return null;
        return DrainedFluids.isDrained(original) ? Blocks.AIR.defaultBlockState() : original;
    }
}
