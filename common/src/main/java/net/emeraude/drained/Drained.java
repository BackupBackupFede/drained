package net.emeraude.drained;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-agnostic entry point — the whole mod is TIER A (server side).
 *
 * <p>Drained removes every fluid from world generation, and then removes the one thing that
 * removal would otherwise have handed back for free:
 *
 * <ol>
 *   <li><b>No water is generated.</b> Oceans, rivers, lakes cut by the noise, aquifers and springs
 *       come out dry. The ocean basins are still carved — you can walk into them.
 *   <li><b>No lava is generated, anywhere.</b> Surface and underground lakes, springs, the deep
 *       lava floor below y=-54, lava aquifers, and the Nether's lava sea.
 *   <li><b>Obsidian cannot be crafted</b>, because that needs water meeting lava and neither
 *       exists. So the only obsidian left in the Overworld is what the ruined portals carry —
 *       and {@link DrainedConfig#realObsidianChance() most of that} is turned to crying obsidian,
 *       which does not form a portal.
 *   <li><b>Chest loot stops topping up the supply</b> in the two vanilla tables that leak
 *       obsidian into the Overworld.
 * </ol>
 *
 * <p>The result is a vanilla progression lock: the Nether — and therefore blaze powder, ender eyes
 * and the End — is reachable only by hunting ruined portals for the few real obsidian blocks they
 * still hold. Nothing is added to the game to make that happen; the mod only takes away.
 *
 * <p>Everything else is left strictly alone: buckets, cauldrons, rain, ice, and the water already
 * baked into structure templates. Those leftovers are the safety valve that keeps a world from
 * becoming a dead end.
 *
 * <p>All rules live in {@code mixin/}; this class only holds identity, logging and the config load.
 * Nothing here may touch a client-only Minecraft class.
 */
public final class Drained {

    public static final String MOD_ID = "drained";
    public static final Logger LOGGER = LoggerFactory.getLogger("Drained");

    private static boolean initialized = false;

    private Drained() {}

    /**
     * Called once from each loader's entry point, as early as possible.
     *
     * @param configDir the loader's config directory — {@code FabricLoader.getConfigDir()} or
     *     {@code FMLPaths.CONFIGDIR}. Passed in because that is the one thing the two loaders
     *     spell differently.
     */
    public static synchronized void init(Path configDir) {
        if (initialized) return;
        initialized = true;

        DrainedConfig.load(configDir);

        LOGGER.info(
                "{} loaded — water: {}, lava: {}, crying portals: {} (real obsidian {}%), loot: {}",
                MOD_ID,
                DrainedConfig.drainWater() ? "drained" : "vanilla",
                DrainedConfig.drainLava() ? "drained" : "vanilla",
                DrainedConfig.cryingPortals() ? "on" : "off",
                Math.round(DrainedConfig.realObsidianChance() * 100.0F),
                DrainedConfig.removeObsidianFromLoot() ? "obsidian removed" : "vanilla");

        // Off unless -Ddrained.selfcheck=true. Worth running on every MATRIX line after a
        // Minecraft version bump: it is the only thing that proves the mixins still apply.
        DrainedSelfCheck.runIfRequested();
    }
}
