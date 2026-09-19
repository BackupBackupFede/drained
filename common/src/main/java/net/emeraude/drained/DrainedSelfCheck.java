package net.emeraude.drained;

/**
 * Off by default. Turn on with {@code -Ddrained.selfcheck=true}, or by setting
 * {@code DRAINED_SELFCHECK=true} in the environment, to prove at startup that every mixin still
 * applies.
 *
 * <p>Mixins are applied when their target class is loaded, so a broken injection point stays
 * invisible until something happens to touch that class. Half of this mod's targets are cold:
 * {@code RuinedPortalPiece} only loads when a ruined portal generates, {@code DeltaFeature} only in
 * a basalt delta, {@code LakeFeature} only when a lava lake rolls. A server that starts cleanly
 * therefore proves very little, and "it compiles" proves nothing at all — Mixin resolves none of
 * its targets until runtime.
 *
 * <p>Force-loading every target turns that into an immediate, loud failure. It is how two of this
 * mod's build-time bugs were caught: a {@code @WrapWithCondition} naming {@code Feature} where the
 * constant pool said {@code DeltaFeature}, and an overloaded {@code makeSettings} that Mixin
 * answered with "Scanned 0 target(s)" rather than an ambiguity error.
 *
 * <p><b>Run it on every line of MATRIX after a Minecraft version bump.</b> That is when signatures
 * drift, and it is exactly when a silently unapplied mixin would otherwise ship.
 */
final class DrainedSelfCheck {

    private static final String PROPERTY = "drained.selfcheck";

    /** Every class this mod mixes into. Keep in step with {@code drained.mixins.json}. */
    private static final String[] TARGETS = {
        "net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator",
        "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer",
        "net.minecraft.world.level.levelgen.SurfaceRules$StateRule",
        "net.minecraft.world.level.levelgen.SurfaceSystem",
        "net.minecraft.world.level.levelgen.feature.SpringFeature",
        "net.minecraft.world.level.levelgen.feature.LakeFeature",
        "net.minecraft.world.level.levelgen.feature.DeltaFeature",
        "net.minecraft.world.level.levelgen.carver.WorldCarver",
        "net.minecraft.world.level.levelgen.carver.NetherWorldCarver",
        "net.minecraft.world.level.levelgen.structure.structures.RuinedPortalPiece",
        "net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces$OceanMonumentPiece",
        "net.minecraft.world.level.storage.loot.LootTable",
    };

    private DrainedSelfCheck() {}

    static void runIfRequested() {
        // The environment variable is the practical one: a Gradle `runServer` forks its own JVM
        // and inherits the environment, so DRAINED_SELFCHECK=true reaches the server without
        // touching either loader's run configuration.
        if (!Boolean.getBoolean(PROPERTY) && !"true".equals(System.getenv("DRAINED_SELFCHECK"))) {
            return;
        }

        int failed = 0;
        for (String name : TARGETS) {
            try {
                Class.forName(name, false, DrainedSelfCheck.class.getClassLoader());
                Drained.LOGGER.info("selfcheck ok   {}", name);
            } catch (Throwable t) {
                failed++;
                Drained.LOGGER.error("selfcheck FAIL {}", name, t);
            }
        }
        if (failed == 0) {
            Drained.LOGGER.info("selfcheck: all {} mixin targets applied cleanly", TARGETS.length);
        } else {
            Drained.LOGGER.error("selfcheck: {} of {} mixin targets FAILED", failed, TARGETS.length);
        }
    }
}
