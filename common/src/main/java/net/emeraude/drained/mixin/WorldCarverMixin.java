package net.emeraude.drained.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.emeraude.drained.DrainedFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rule 2, part six — the cave carvers stop pouring lava into the bottom of the world.
 *
 * <p>This one only showed up by generating a world and reading the region files back: with every
 * other rule in place, one chunk in five hundred still had lava in its palette, down among the
 * bedrock. Carvers are a fluid source in their own right and they never consult the fluid picker:
 *
 * <pre>
 *   if (blockPos.getY() &lt;= configuration.lavaLevel.resolveY(context)) return LAVA;
 * </pre>
 *
 * <p>{@code lavaLevel} is a data-driven anchor on every configured carver (y=-54 in vanilla), so
 * the deep lava lakes at the bottom of a cave system are carved in, not filled in — which is why
 * neutralising the picker's own {@code Blocks.LAVA} was not enough.
 *
 * <p>The replacement is cave air rather than {@code null}: {@code null} means "leave this block
 * solid", which would stop the caves being carved at all below y=-54 and reshape the deep world.
 * Cave air keeps vanilla's caves exactly where they are and merely leaves them dry — the same
 * treatment the oceans and the lakes get.
 *
 * <p>The aquifer branch of the same method is already covered by {@link AquiferMixin}; filtering
 * the return value catches both paths with one hook.
 *
 * <p>Cross-version note: {@code getCarveState} is private, so there is no bridge method to make
 * the name ambiguous, and its descriptor is byte-for-byte identical on 1.21.1 and 26.2 — the
 * generic {@code C configuration} erases to {@code CarverConfiguration} on both. The Nether has
 * its own carver which overrides {@code carveBlock} outright and never calls this; that is
 * {@link NetherWorldCarverMixin}.
 */
@Mixin(WorldCarver.class)
public abstract class WorldCarverMixin {

    @ModifyReturnValue(
            method =
                    "getCarveState(Lnet/minecraft/world/level/levelgen/carver/CarvingContext;"
                            + "Lnet/minecraft/world/level/levelgen/carver/CarverConfiguration;"
                            + "Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/level/levelgen/Aquifer;)"
                            + "Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN"))
    private BlockState drained$dryCaves(BlockState original) {
        // null is meaningful here: "do not carve". Leave it alone.
        if (original == null) return null;
        return DrainedFluids.isDrained(original) ? Blocks.CAVE_AIR.defaultBlockState() : original;
    }
}
