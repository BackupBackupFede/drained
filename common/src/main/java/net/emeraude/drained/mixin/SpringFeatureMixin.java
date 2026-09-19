package net.emeraude.drained.mixin;

import net.emeraude.drained.DrainedFluids;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rules 1 and 2, part three — no springs.
 *
 * <p>Springs are the single fluid source blocks tucked into stone walls and cave ceilings:
 * {@code spring_water}, {@code spring_lava}, {@code spring_lava_frozen} in the Overworld, and the
 * open/closed/delta lava springs in the Nether. They are placed as features, entirely outside the
 * noise and aquifer machinery, so nothing in {@link NoiseBasedChunkGeneratorMixin} or
 * {@link AquiferMixin} touches them.
 *
 * <p>A single lava spring is enough to undo the whole mod: one water bucket poured over it and
 * obsidian is back. This is not a decoration fix.
 *
 * <p>Cancelled at the head rather than filtered at the {@code setBlock} call, so that the fluid
 * tick vanilla schedules on the next line is never scheduled either.
 *
 * <p>Cross-version note: the spring's fluid is read from {@code SpringConfiguration.state}, a
 * public final field on both lines, and {@code place} erases to
 * {@code (FeaturePlaceContext)Z} on both.
 */
@Mixin(SpringFeature.class)
public abstract class SpringFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void drained$noSprings(
            FeaturePlaceContext<SpringConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        if (DrainedFluids.isDrained(context.config().state.createLegacyBlock())) {
            cir.setReturnValue(false);
        }
    }
}
