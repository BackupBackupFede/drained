package net.emeraude.drained.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.emeraude.drained.DrainedFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.carver.NetherWorldCarver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rule 2, part seven — the Nether's cave carver stops flooding its own caves.
 *
 * <p>{@code NetherWorldCarver} overrides {@code carveBlock} entirely rather than going through
 * {@link WorldCarverMixin}'s {@code getCarveState}, and hard-codes the rule: everything carved
 * below {@code minGenY + 31} becomes lava instead of cave air. That is a second lava sea under
 * the first one, and neither the fluid picker nor the aquifer has any say in it.
 *
 * <p>Cross-version note: {@code carveBlock} is overridden with a narrowed configuration type, so
 * the class carries a synthetic bridge method of the same name — a bare {@code "carveBlock"}
 * selector is therefore ambiguous, and Mixin answers ambiguity with "Scanned 0 target(s)" rather
 * than an error (the same trap as {@link RuinedPortalPieceMixin}). The full descriptor is used
 * instead, and it is identical on 1.21.1 and 26.2.
 *
 * <p>The injection point is the fluid-to-block conversion, whose descriptor is likewise unchanged
 * between the two lines and appears exactly once in the method. The neighbouring
 * {@code chunk.setBlockState} call could not have been used: it takes a trailing boolean on
 * 1.21.1 that 26.2 dropped.
 */
@Mixin(NetherWorldCarver.class)
public abstract class NetherWorldCarverMixin {

    @ModifyExpressionValue(
            method =
                    "carveBlock(Lnet/minecraft/world/level/levelgen/carver/CarvingContext;"
                            + "Lnet/minecraft/world/level/levelgen/carver/CaveCarverConfiguration;"
                            + "Lnet/minecraft/world/level/chunk/ChunkAccess;"
                            + "Ljava/util/function/Function;"
                            + "Lnet/minecraft/world/level/chunk/CarvingMask;"
                            + "Lnet/minecraft/core/BlockPos$MutableBlockPos;"
                            + "Lnet/minecraft/core/BlockPos$MutableBlockPos;"
                            + "Lnet/minecraft/world/level/levelgen/Aquifer;"
                            + "Lorg/apache/commons/lang3/mutable/MutableBoolean;)Z",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/material/FluidState;createLegacyBlock()"
                                            + "Lnet/minecraft/world/level/block/state/BlockState;"),
            require = 1)
    private BlockState drained$dryNetherCaves(BlockState original) {
        return DrainedFluids.isDrained(original) ? Blocks.CAVE_AIR.defaultBlockState() : original;
    }
}
