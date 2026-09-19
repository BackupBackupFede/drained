package net.emeraude.drained.mixin;

import java.util.Optional;
import java.util.function.Consumer;
import net.emeraude.drained.DrainedConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Rule 4 — the chests stop leaking obsidian into the Overworld.
 *
 * <p>Reading every loot table shipped by both target versions turns up exactly two that put
 * obsidian into an Overworld player's hands, and the second one is easy to miss:
 *
 * <ul>
 *   <li>{@code chests/ruined_portal} — 1-2 obsidian at weight 40, so roughly a coin flip per chest;
 *   <li>{@code chests/village/village_weaponsmith} — <b>3-7 obsidian</b> at weight 5 over 3-8
 *       rolls, so about one weaponsmith chest in four. Villages are common and safe, and a single
 *       lucky chest is most of a portal frame. Left open, this alone makes the ruin hunt optional.
 * </ul>
 *
 * <p>The Nether's tables (nether_bridge, the bastions, piglin bartering) are deliberately left
 * alone: reaching them means the lock is already open, and obsidian is meant to flow again there.
 * {@code blocks/ender_chest} is a refund, not a source.
 *
 * <p>Done in code rather than by shipping replacement loot table JSON, because a datapack override
 * replaces a table wholesale and would freeze it at the version it was copied from — 26.2 already
 * adds a lodestone pool to the ruined portal chest and a second pool to the weaponsmith that a
 * 1.21.1-shaped copy would silently delete. Filtering the output keeps every other vanilla change,
 * now and at the next Minecraft drop.
 *
 * <p>{@code getRandomItemsRaw(LootContext, Consumer)} is the single funnel every public
 * {@code getRandomItems} path goes through, on both lines, with an identical descriptor.
 *
 * <p>Cross-version note on identity: a table's own id is not reachable from {@code common/} —
 * NeoForge's {@code getLootTableId()} is a loader patch that Fabric does not have. What is
 * reachable is {@code randomSequence}, which vanilla sets to the table's id and which erases to a
 * plain {@code Ljava/util/Optional;} on both lines, so shadowing it as {@code Optional<?>} matches
 * {@code Optional<ResourceLocation>} on 1.21.1 and {@code Optional<Identifier>} on 26.2 without
 * ever naming the type that was renamed between them.
 */
@Mixin(LootTable.class)
public abstract class LootTableMixin {

    @Shadow @Final private Optional<?> randomSequence;

    @ModifyVariable(
            method =
                    "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;"
                            + "Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 1)
    private Consumer<ItemStack> drained$filterObsidian(Consumer<ItemStack> output) {
        if (!DrainedConfig.removeObsidianFromLoot()) return output;
        if (this.randomSequence.isEmpty()) return output;
        if (!DrainedConfig.obsidianLootTables().contains(this.randomSequence.get().toString())) {
            return output;
        }
        return stack -> {
            if (!stack.is(Items.OBSIDIAN)) {
                output.accept(stack);
            }
        };
    }
}
