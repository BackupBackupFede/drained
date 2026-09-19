package net.emeraude.drained.mixin;

import java.util.ArrayList;
import java.util.List;
import net.emeraude.drained.DrainedConfig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.structures.RuinedPortalPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Rule 3 — the world fills with portals that look finished and never light.
 *
 * <p>This is the rule that turns a deprivation into a game. Without it the lock is trivial:
 * ruined portals sit roughly every few hundred blocks in every biome and each carries eight to
 * fourteen obsidian, so the ten blocks a frame needs are one or two ruins away and the hunt is
 * over in twenty minutes.
 *
 * <p>With it, each ruin yields one or two real blocks and sometimes none — see
 * {@link DrainedConfig#realObsidianChance()}, which is the number to tune in game. The rest is
 * crying obsidian, which cannot form a portal but is not a wasted drop either: it is what a
 * respawn anchor is made of, and the anchor recharges on glowstone, which only the Nether has. The
 * mod hands itself its own checkpoint system without adding anything.
 *
 * <p>Applies to ruined portals in the Nether as well as the Overworld. That is deliberate but it
 * is the sharpest edge in the design: Nether ruins are what you cannibalise when your portal
 * breaks and you are stranded, so making them scarce is what makes carrying spare obsidian a real
 * decision. Worth watching in the first long play session.
 *
 * <p>Implementation: vanilla builds a list of {@link ProcessorRule}s and hands it to a
 * {@code RuleProcessor} that rots the structure as it is placed — gold blocks to air, lava to
 * magma, netherrack to magma. Prepending one more rule is the whole change. Order is safe: the
 * processor returns on the first matching rule and none of vanilla's rules match obsidian.
 *
 * <p>Cross-version note: {@code makeSettings} is overloaded, and a bare name selector resolves to
 * <em>nothing</em> there — Mixin reports "Scanned 0 target(s)" rather than an ambiguity error,
 * which is a silent-looking failure worth remembering. Both full descriptors are therefore listed
 * and exactly one matches per line: 26.2 added a leading {@code HolderLookup.Provider} parameter.
 * The injection point itself is stable — {@code RuleProcessor(List)} is identical on both — as are
 * {@code ProcessorRule(RuleTest, RuleTest, BlockState)} and
 * {@code RandomBlockMatchTest(Block, float)}.
 */
@Mixin(RuinedPortalPiece.class)
public abstract class RuinedPortalPieceMixin {

    @ModifyArg(
            method = {
                // 26.2
                "makeSettings(Lnet/minecraft/core/HolderLookup$Provider;"
                        + "Lnet/minecraft/world/level/block/Mirror;"
                        + "Lnet/minecraft/world/level/block/Rotation;"
                        + "Lnet/minecraft/world/level/levelgen/structure/structures/RuinedPortalPiece$VerticalPlacement;"
                        + "Lnet/minecraft/core/BlockPos;"
                        + "Lnet/minecraft/world/level/levelgen/structure/structures/RuinedPortalPiece$Properties;)"
                        + "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;",
                // 1.21.1 — same method, before the HolderLookup.Provider parameter was added
                "makeSettings(Lnet/minecraft/world/level/block/Mirror;"
                        + "Lnet/minecraft/world/level/block/Rotation;"
                        + "Lnet/minecraft/world/level/levelgen/structure/structures/RuinedPortalPiece$VerticalPlacement;"
                        + "Lnet/minecraft/core/BlockPos;"
                        + "Lnet/minecraft/world/level/levelgen/structure/structures/RuinedPortalPiece$Properties;)"
                        + "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;",
            },
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/structure/templatesystem/"
                                            + "RuleProcessor;<init>(Ljava/util/List;)V"),
            require = 1)
    private static List<? extends ProcessorRule> drained$cryingFrames(
            List<? extends ProcessorRule> rules) {
        if (!DrainedConfig.cryingPortals()) return rules;

        float realChance = DrainedConfig.realObsidianChance();
        if (realChance >= 1.0F) return rules;

        // RandomBlockMatchTest fires with the probability given, so the probability of the
        // *substitution* is the complement of the fraction that stays real obsidian.
        List<ProcessorRule> withCrying = new ArrayList<>(rules.size() + 1);
        withCrying.add(
                new ProcessorRule(
                        new RandomBlockMatchTest(Blocks.OBSIDIAN, 1.0F - realChance),
                        AlwaysTrueTest.INSTANCE,
                        Blocks.CRYING_OBSIDIAN.defaultBlockState()));
        withCrying.addAll(rules);
        return withCrying;
    }
}
