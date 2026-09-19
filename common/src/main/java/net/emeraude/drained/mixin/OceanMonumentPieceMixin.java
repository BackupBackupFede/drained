package net.emeraude.drained.mixin;

import net.emeraude.drained.DrainedConfig;
import net.emeraude.drained.DrainedFluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The ocean monument stops arriving as a cube of water hanging in the air.
 *
 * <p>Every other fluid in this mod comes out of world generation, and structure fluid is kept on
 * purpose — a shipwreck with water still in it is a small, deliberate leftover. The monument is the
 * exception, and it looks absurd: {@code generateWaterBox} does not <em>inherit</em> the ocean, it
 * <b>creates</b> it, filling the whole 58-block bounding box with water below sea level and air
 * above, regardless of what was there. Drain the ocean around it and the monument is left as a
 * gigantic floating aquarium — the one thing in a drained world that reads as broken rather than
 * as drained.
 *
 * <p>Filtering the fluid write leaves the prismarine exactly where vanilla put it and hollows the
 * monument with air instead, which is the same treatment the oceans, lakes, caves and deltas get.
 *
 * <p><b>Balance consequence:</b> a dry monument cannot spawn guardians — they need water — so its
 * sponges and prismarine become free for anyone who walks in. That was already half true (the ocean around it is gone either way), but this
 * settles it. {@link DrainedConfig#drainOceanMonuments()} exists so the call stays reversible:
 * turn it off and the monument keeps its water, guardians and all.
 *
 * <p>Cross-version note: {@code OceanMonumentPiece} is a protected inner class, so it is targeted
 * by binary name. {@code generateWaterBox} is not overloaded and has the same descriptor on both
 * lines, and — the trap met twice already in this mod — the owner of the {@code placeBlock} call
 * in the constant pool is {@code OceanMonumentPieces$OceanMonumentPiece} itself, not the
 * {@code StructurePiece} the method is declared on.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces$OceanMonumentPiece")
public abstract class OceanMonumentPieceMixin {

    @ModifyArg(
            method = "generateWaterBox",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/structure/structures/"
                                            + "OceanMonumentPieces$OceanMonumentPiece;placeBlock("
                                            + "Lnet/minecraft/world/level/WorldGenLevel;"
                                            + "Lnet/minecraft/world/level/block/state/BlockState;III"
                                            + "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)V"),
            index = 1,
            require = 1)
    private BlockState drained$dryMonument(BlockState state) {
        if (!DrainedConfig.drainOceanMonuments()) return state;
        return DrainedFluids.isDrained(state) ? Blocks.AIR.defaultBlockState() : state;
    }
}
