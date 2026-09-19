package net.emeraude.drained;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * One file, hand-editable on a headless server: {@code config/drained.properties}.
 *
 * <p>Written with {@link java.util.Properties} on purpose — it is JDK-only, so there is no library
 * whose name or API could drift between Minecraft versions. The file is written back with a plain
 * {@link PrintWriter} rather than {@code Properties.store} so the comments and the key order
 * survive.
 *
 * <p>Values are read on every call from the mixins, which run on worldgen threads — hence
 * {@code volatile}. The defaults are the mod's intended behaviour, so a missing or unreadable
 * config file degrades to "the mod does what it says on the tin" rather than to "the mod is off".
 */
public final class DrainedConfig {

    private static final String FILE_NAME = Drained.MOD_ID + ".properties";

    private static final String KEY_DRAIN_WATER = "drain_water";
    private static final String KEY_DRAIN_LAVA = "drain_lava";
    private static final String KEY_BARE_OCEAN_FLOORS = "bare_ocean_floors";
    private static final String KEY_CRYING_PORTALS = "crying_portals";
    private static final String KEY_REAL_OBSIDIAN_CHANCE = "real_obsidian_chance";
    private static final String KEY_DRAIN_OCEAN_MONUMENTS = "drain_ocean_monuments";
    private static final String KEY_REMOVE_OBSIDIAN_FROM_LOOT = "remove_obsidian_from_loot";
    private static final String KEY_OBSIDIAN_LOOT_TABLES = "obsidian_loot_tables";

    /**
     * The two vanilla loot tables that put obsidian into a player's hands in the Overworld,
     * established by reading every {@code data/minecraft/loot_table/} entry of both target
     * versions. The Nether ones (nether_bridge, the bastions, piglin bartering) are deliberately
     * absent: by the time you can reach them the lock is already open, and the Nether is where
     * obsidian is supposed to become available again.
     *
     * <p>{@code blocks/ender_chest} also drops obsidian but is not a source — you had to spend
     * eight of them to place the chest.
     *
     * <p>Matched against a loot table's {@code random_sequence}, which vanilla sets to the table's
     * own id.
     */
    private static final String DEFAULT_OBSIDIAN_LOOT_TABLES =
            "minecraft:chests/ruined_portal,minecraft:chests/village/village_weaponsmith";

    private static volatile boolean drainWater = true;
    private static volatile boolean drainLava = true;
    private static volatile boolean bareOceanFloors = true;
    private static volatile boolean cryingPortals = true;
    private static volatile float realObsidianChance = 0.45F;
    private static volatile boolean drainOceanMonuments = true;
    private static volatile boolean removeObsidianFromLoot = false;
    private static volatile Set<String> obsidianLootTables = parseList(DEFAULT_OBSIDIAN_LOOT_TABLES);

    private DrainedConfig() {}

    /**
     * Rule 1 — world generation stops producing water: oceans, rivers, lakes cut by the noise,
     * aquifers, and water springs.
     *
     * <p>Only affects chunks generated <em>after</em> the mod is installed. Water already in an
     * existing world, and water baked into structure templates, stays.
     */
    public static boolean drainWater() {
        return drainWater;
    }

    /**
     * Rule 2 — world generation stops producing lava: surface and underground lava lakes, lava
     * springs, lava aquifers, the deep lava floor below y=-54, the basalt deltas' lava, and the
     * Nether's lava sea.
     *
     * <p>Turning this off re-opens obsidian: one bucket of water over one pool of lava and the
     * whole point of the mod is gone. It exists for modpacks that want the dry Overworld without
     * the progression lock.
     */
    public static boolean drainLava() {
        return drainLava;
    }

    /**
     * Whether a drained sea floor keeps its sea-floor materials instead of growing grass.
     *
     * <p>Minecraft's surface rules read blocks rather than biomes: with no water in the column they
     * conclude the ground is open air and lay grass over dirt, so a drained ocean comes out as a
     * green valley. With this on, the rules are told the sea level and paint gravel, sand and clay
     * as they would under a real sea — without a single block of water being placed.
     *
     * <p>Nothing above sea level changes either way.
     */
    public static boolean bareOceanFloors() {
        return bareOceanFloors;
    }

    /** Rule 3 — whether ruined portal frames come out mostly crying obsidian. */
    public static boolean cryingPortals() {
        return cryingPortals;
    }

    /**
     * The fraction of a ruined portal's obsidian that stays real, in {@code [0, 1]}.
     *
     * <p>This is <em>the</em> tuning knob of the whole mod, and it was set by playing it, twice.
     * Both corrections came from the same place, and the average was never the useful number:
     *
     * <ul>
     *   <li>0.15 gave 1.4 real blocks per ruin — but <b>24% of ruins gave nothing at all</b>.
     *       Several hundred blocks of travel ending in an empty ruin reads as unfair, not as hard.
     *   <li>0.30 fixed the empty ruin (6%) and still felt thin, because <b>47% of ruins gave two
     *       blocks or fewer</b>. A ruin that hands over a token is barely better than one that
     *       hands over nothing.
     * </ul>
     *
     * <p>At 0.45 a ruin gives about 4.4 real blocks, a frame costs a little over two ruins, one ruin
     * in a hundred is empty, and the token payout drops to 21%. Every ruin found is worth the walk,
     * which is what the design needed — the scarcity was never supposed to be a lottery.
     *
     * <p>The failure rates matter more than the mean here, so if this is retuned again, look at how
     * often a ruin disappoints rather than at how many blocks it averages. 1.0 disables the
     * substitution entirely, which is the same as {@code crying_portals=false}.
     */
    public static float realObsidianChance() {
        return realObsidianChance;
    }

    /**
     * Whether an ocean monument comes out dry.
     *
     * <p>The monument is the one structure that <em>creates</em> its own water rather than sitting
     * in the ocean's, so a drained world leaves it as a cube of water hanging in the air. Draining
     * it is the only coherent look, but it has a real cost: guardians need water to spawn, so a dry
     * monument hands over its sponges and prismarine for the price of walking in.
     *
     * <p>Set to false to keep monuments flooded — guardians, loot defended, and a very odd
     * silhouette.
     */
    public static boolean drainOceanMonuments() {
        return drainOceanMonuments;
    }

    /**
     * Whether obsidian is filtered out of {@link #obsidianLootTables()}. <b>Off by default.</b>
     *
     * <p>Chest loot is left vanilla on purpose: a lucky ruined portal chest (1-2 obsidian, in about
     * half of them) or village weaponsmith chest (3-7, in about one in four) is part of the hunt,
     * not a leak in it. Turn this on for a harder lock, where the ruins' frames are the only source.
     */
    public static boolean removeObsidianFromLoot() {
        return removeObsidianFromLoot;
    }

    /** The loot tables obsidian is filtered out of, as {@code namespace:path} random-sequence ids. */
    public static Set<String> obsidianLootTables() {
        return obsidianLootTables;
    }

    static void load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        try {
            if (Files.exists(file)) {
                read(file);
            } else {
                write(file);
            }
        } catch (IOException e) {
            Drained.LOGGER.warn(
                    "Could not read or create {} — falling back to defaults (everything drained)",
                    file,
                    e);
        }
    }

    private static void read(Path file) throws IOException {
        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        drainWater = bool(props, KEY_DRAIN_WATER, true);
        drainLava = bool(props, KEY_DRAIN_LAVA, true);
        bareOceanFloors = bool(props, KEY_BARE_OCEAN_FLOORS, true);
        cryingPortals = bool(props, KEY_CRYING_PORTALS, true);
        realObsidianChance = fraction(props, KEY_REAL_OBSIDIAN_CHANCE, 0.45F);
        drainOceanMonuments = bool(props, KEY_DRAIN_OCEAN_MONUMENTS, true);
        removeObsidianFromLoot = bool(props, KEY_REMOVE_OBSIDIAN_FROM_LOOT, false);
        obsidianLootTables =
                parseList(props.getProperty(KEY_OBSIDIAN_LOOT_TABLES, DEFAULT_OBSIDIAN_LOOT_TABLES));
    }

    private static Set<String> parseList(String raw) {
        Set<String> out = new LinkedHashSet<>();
        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(out::add);
        return Set.copyOf(out);
    }

    private static boolean bool(Properties props, String key, boolean fallback) {
        String raw = props.getProperty(key);
        if (raw == null) return fallback;
        raw = raw.trim();
        if (raw.equalsIgnoreCase("true")) return true;
        if (raw.equalsIgnoreCase("false")) return false;
        Drained.LOGGER.warn("Config key '{}' is '{}', expected true or false — using {}", key, raw, fallback);
        return fallback;
    }

    private static float fraction(Properties props, String key, float fallback) {
        String raw = props.getProperty(key);
        if (raw == null) return fallback;
        try {
            float value = Float.parseFloat(raw.trim());
            if (value >= 0.0F && value <= 1.0F) return value;
            Drained.LOGGER.warn("Config key '{}' is '{}', expected 0.0 to 1.0 — using {}", key, raw, fallback);
        } catch (NumberFormatException e) {
            Drained.LOGGER.warn("Config key '{}' is '{}', expected a number — using {}", key, raw, fallback);
        }
        return fallback;
    }

    private static void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (PrintWriter out =
                new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            out.println("# Drained");
            out.println("#");
            out.println("# No water, no lava, nowhere. Obsidian cannot be made, so the only way to");
            out.println("# the Nether is the obsidian still standing in the world's ruined portals.");
            out.println("#");
            out.println("# Read once, when the mod loads. Restart to apply.");
            out.println("# Only chunks generated AFTER installing are affected: this is a mod for a new world.");
            out.println();
            out.println("# World generation stops producing water: oceans, rivers, lakes, aquifers,");
            out.println("# springs. Basins are still carved, so a drained ocean is a place you walk into.");
            out.println("# Rain, cauldrons, ice and the water inside structures are untouched.");
            out.println(KEY_DRAIN_WATER + "=" + drainWater);
            out.println();
            out.println("# World generation stops producing lava: lakes, springs, aquifers, the deep");
            out.println("# lava below y=-54, basalt delta lava, and the Nether's lava sea.");
            out.println("# Turning this off gives obsidian back (water bucket + lava = obsidian) and");
            out.println("# undoes the progression lock this mod is built on.");
            out.println(KEY_DRAIN_LAVA + "=" + drainLava);
            out.println();
            out.println("# A drained sea floor keeps gravel, sand and clay instead of growing grass.");
            out.println("# Minecraft decides that from the blocks, not the biome: with no water in the");
            out.println("# column it assumes open air and lays down grass, so a drained ocean would");
            out.println("# otherwise look like a green valley. No water is placed either way, and");
            out.println("# nothing above sea level changes.");
            out.println(KEY_BARE_OCEAN_FLOORS + "=" + bareOceanFloors);
            out.println();
            out.println("# Ruined portal frames are placed as crying obsidian instead of obsidian,");
            out.println("# except for the fraction below. Crying obsidian does not form a portal, so");
            out.println("# the world fills with portals that look complete and never light.");
            out.println(KEY_CRYING_PORTALS + "=" + cryingPortals);
            out.println();
            out.println("# Fraction of a ruined portal's obsidian that stays real, 0.0 to 1.0.");
            out.println("# THE tuning knob, set by playing it. Change it here and make a NEW world -");
            out.println("# it applies at generation, so an existing world keeps what it already has.");
            out.println("#");
            out.println("#     p   real/ruin  ruins/frame  empty ruin  ruin giving <=2");
            out.println("#  0.15         1.4          6.9         24%              73%");
            out.println("#  0.30         2.9          3.4          6%              47%");
            out.println("#  0.45         4.4          2.3          1%              21%   (default)");
            out.println("#  0.60         5.8          1.7          0%               6%");
            out.println("#  1.00         9.7          1.0          0%               0%   (vanilla)");
            out.println("#");
            out.println("# Watch the last two columns, not the average: both retunes so far came from");
            out.println("# ruins that disappointed, never from the hunt being too long.");
            out.println(KEY_REAL_OBSIDIAN_CHANCE + "=" + realObsidianChance);
            out.println();
            out.println("# Ocean monuments create their own water instead of sitting in the ocean's,");
            out.println("# so without this they are left as a cube of water hanging in the air.");
            out.println("# Draining one also means no guardians (they need water), so its sponges and");
            out.println("# prismarine become free. Set to false to keep monuments flooded.");
            out.println(KEY_DRAIN_OCEAN_MONUMENTS + "=" + drainOceanMonuments);
            out.println();
            out.println("# Chest loot is left VANILLA by default: a lucky ruined portal chest (1-2");
            out.println("# obsidian, about half of them) or village weaponsmith chest (3-7, about one");
            out.println("# in four) is part of the hunt. Set to true for a harder lock, where the");
            out.println("# ruins' frames are the only obsidian in the Overworld.");
            out.println(KEY_REMOVE_OBSIDIAN_FROM_LOOT + "=" + removeObsidianFromLoot);
            out.println();
            out.println("# Which loot tables lose their obsidian when the switch above is on, comma");
            out.println("# separated. These are the two vanilla tables that hand obsidian to an");
            out.println("# Overworld player; the Nether ones are left alone either way.");
            out.println(KEY_OBSIDIAN_LOOT_TABLES + "=" + DEFAULT_OBSIDIAN_LOOT_TABLES);
        }
    }
}
