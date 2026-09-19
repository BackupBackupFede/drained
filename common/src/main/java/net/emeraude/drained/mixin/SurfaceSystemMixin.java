package net.emeraude.drained.mixin;

import net.emeraude.drained.DrainedConfig;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * A drained ocean looks like an exposed sea floor instead of growing a lawn.
 *
 * <p>This is not a bug fix — it is the mod's headline image. Without it, the biome is still
 * {@code deep_ocean} but the ground comes out as grass and dirt, and a drained ocean reads as a
 * green valley rather than as a basin that used to hold a sea.
 *
 * <p>The cause is that Minecraft's surface rules key off <em>blocks</em>, not biomes.
 * {@code buildSurface} walks each column and only records a {@code waterHeight} when it meets a
 * block with a fluid in it; any air resets it. With the water gone the column starts at the sea
 * floor and {@code waterHeight} stays {@link Integer#MIN_VALUE}, so every {@code minecraft:water}
 * condition in the rule tree answers "we are in open air" and the dry-land branch runs: grass block
 * over dirt, where vanilla would have laid gravel, sand or clay.
 *
 * <p>Handing the rules the sea level instead of {@code MIN_VALUE} restores the sea-floor materials
 * without putting a single block of water back. The comparison inside the condition —
 * {@code blockY >= waterHeight + offset + …} — does the rest on its own: at or above sea level it
 * still answers "open air" and land keeps its grass, below it the sea-floor rules apply. So there
 * is no need to know the y here, which is what keeps this to one argument.
 *
 * <p>Deliberately narrow: this changes only what the surface rules <em>believe</em>. No fluid is
 * placed, no terrain shape changes, and turning off {@link DrainedConfig#bareOceanFloors()} gives
 * the green basins straight back.
 *
 * <p>Cross-version note: {@code buildSurface} is not overloaded on either line, and {@code seaLevel}
 * is a {@code private final int} on both. {@code Context.updateY} <em>does</em> diverge — four
 * parameters on 26.2, six on 1.21.1 — so the injection point names the method without a descriptor
 * and lets Mixin resolve it from the instruction. {@code waterHeight} is argument 2 in both.
 */
@Mixin(SurfaceSystem.class)
public abstract class SurfaceSystemMixin {

    @Shadow @Final private int seaLevel;

    @ModifyArg(
            method = "buildSurface",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;updateY"),
            index = 2,
            require = 1)
    private int drained$seabedStaysBare(int waterHeight) {
        if (!DrainedConfig.bareOceanFloors() || !DrainedConfig.drainWater()) return waterHeight;
        // MIN_VALUE means "no fluid found in this column" — which, in a drained world, is every
        // column. Answer with the sea level so the rules paint a sea floor below it and leave
        // everything above it alone.
        return waterHeight == Integer.MIN_VALUE ? this.seaLevel : waterHeight;
    }
}
