# Drained

**No water, no lava, nowhere — and the Nether portal becomes a lock.**

Multiloader Minecraft mod: one branch serves **NeoForge and Fabric** on **1.21.1 and 26.2**.
Pure server-side (Tier A): it works against a **vanilla client**, and only the server needs it.

---

## What it does

World generation stops producing fluid. Oceans, rivers, lakes, aquifers and springs come out dry —
and the basins are still carved, so a drained ocean is somewhere you walk down into, past
shipwrecks and monuments standing in open air. The same goes for lava: surface and underground
lakes, springs, the deep lava floor under the caves, the basalt deltas, and the Nether's lava sea.

Then the consequence, which is the actual game:

**Obsidian is made of water meeting lava.** With neither in the world, it cannot be made at all.
The Nether portal frame needs ten blocks of it, and the Nether is on the critical path to
everything — blaze powder, ender eyes, the End. So the only obsidian left in the Overworld is what
the world already holds: the frames of its **ruined portals**, and the odd lucky chest.

And most of that is turned to **crying obsidian**, which does not form a portal. The world fills
with ruins that look finished and never light. Each one still gives up about four real blocks, so
reaching the Nether means two or three ruins and the walk between them, with a diamond pickaxe to
keep sharp — and the crying obsidian you carry home is not a consolation prize, it is a
respawn anchor, recharged with glowstone you can only get on the other side.

Nothing is added to the game to make any of this happen. Every rule is vanilla; the mod only takes
away.

**What it deliberately does not touch:** buckets, cauldrons, rain, and the water already baked into
structure templates — a shipwreck keeps what it came with. Those leftovers are the safety valve that
stops a world becoming a dead end, and none of them re-opens obsidian, because there is no lava for
the water to meet.

One consequence worth knowing before you start a world: **ice stops being a water source.** Ice
spikes and icebergs survive in full — they place blocks, they do not freeze water — but plain ice,
the only kind that melts back into water, goes with the sea it was the surface of. What is left is
rain in a cauldron, and the water still sitting inside structures.

**Start a new world.** Only chunks generated after installing are affected; nothing is converted
retroactively.

---

## Configuration

`config/drained.properties`, written on first launch. Read once at load — restart to apply.

| Key | Default | Effect |
|---|---|---|
| `drain_water` | `true` | no generated water anywhere |
| `drain_lava` | `true` | no generated lava anywhere, Nether included. Turning this off gives obsidian back and undoes the lock |
| `bare_ocean_floors` | `true` | a drained sea floor keeps gravel, sand and clay instead of growing grass |
| `crying_portals` | `true` | ruined portal frames come out as crying obsidian |
| `real_obsidian_chance` | `0.45` | fraction of a ruin's obsidian that stays real. **The tuning knob** — see below |
| `drain_ocean_monuments` | `true` | the monument comes out dry instead of as a floating cube of water. Dry means no guardians — see below |
| `remove_obsidian_from_loot` | `false` | strip obsidian from the two chest tables below. Off: chests stay vanilla |
| `obsidian_loot_tables` | ruined portal + village weaponsmith | which tables lose their obsidian when the switch is on |

### `real_obsidian_chance` is the number that matters

A ruined portal carries roughly 8–14 obsidian and a frame needs 10, so the substitution rate decides
how far you walk. It was set by playing, and the first guess was wrong in an instructive way:

| rate | real per ruin | ruins for a frame | empty ruin | ruin giving ≤2 |
|---|---:|---:|---:|---:|
| `0.15` (first guess) | 1.4 | ~6.9 | **24 %** | 73 % |
| `0.30` | 2.9 | ~3.4 | 6 % | **47 %** |
| `0.45` (default) | 4.4 | ~2.3 | 1 % | 21 % |
| `0.60` | 5.8 | ~1.7 | 0 % | 6 % |
| `1.00` | 9.7 | 1 | 0 % | 0 % — substitution off |

**Read the last two columns, not the average.** Both retunes came from ruins that disappointed,
never from the hunt being too long: `0.15` left a quarter of ruins empty, and `0.30` still handed
two blocks or fewer to nearly half of them. A ruin that gives a token is barely better than one that
gives nothing. At `0.45` every ruin found is worth the walk, and a frame is still a little over two
of them.

Change it and make a **new world** — it applies at generation.

### Why a drained ocean would otherwise be green

Minecraft's surface rules key off blocks, not biomes. With the water gone, `buildSurface` finds no
fluid in the column, concludes the ground is in open air, and lays grass over dirt where it would
have laid gravel — so the basin comes out as a green valley and the whole image of the mod goes with
it. `bare_ocean_floors` hands the rules the sea level instead, and they paint a sea floor again
without a single block of water being placed. Measured, same seed, 2304 chunks: grass blocks go from
879,506 back to 92,702 against vanilla's 96,737, gravel from 945,342 to 1,544,197 — and below y=0
nothing changes at all (510,409 → 510,407 gravel), because the surface rules never reach that deep.

### The monument is the one structure that makes its own water

Every other structure sits in the ocean's water; the monument fills its own ~58-block box with it,
regardless of what was there. Left alone in a drained world it hangs in the air as a giant aquarium.
Draining it keeps the prismarine exactly where vanilla put it and hollows it with air — but a dry
monument spawns no guardians, so its sponges and prismarine are free to anyone who walks in.
`drain_ocean_monuments=false` gives the water, the guardians and the defended loot back.

### Chests are left vanilla — on purpose

Auditing every vanilla loot table turned up two that hand obsidian to an Overworld player:
`chests/ruined_portal` (1–2 blocks, in about half of them) and — easy to miss —
`chests/village/village_weaponsmith` (**3–7 blocks**, in about one in four).

By default both are left exactly as they are. A lucky chest is part of the hunt, not a leak in it,
and it keeps the mod closer to its rule of only taking away what fluids made. For a harder lock,
where ruined portal frames are the only source, set `remove_obsidian_from_loot=true`. The Nether's
tables are left alone either way: by the time you can reach them, the lock is already open.

---

## Building

The Gradle **process** needs JDK 25 even when a target's toolchain is Java 21:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25'
```

```bash
./gradlew build                             # default line (26.2)
./gradlew build -Pminecraft_version=1.21.1  # the other MATRIX line
```

In PowerShell the property must be quoted — `'-Pminecraft_version=1.21.1'`.

Per-Minecraft-version values live only in `MATRIX` (`build.gradle`). A change is not finished until
it builds on every line.

---

## How it is built

Twelve mixins in `common/`, no assets, no data pack, no registry entries — which is why Fabric API is
not a dependency and there is no `pack.mcmeta`.

Minecraft generates fluid from **five independent mechanisms**, and removing one does nothing about
the other four. That is the whole shape of this mod:

| # | Mechanism | Mixin | Stops |
|---|---|---|---|
| 1 | fluid picker | `NoiseBasedChunkGeneratorMixin` | the sea fluid (oceans, rivers, **the Nether's lava sea**) and the deep lava floor below y=-54 |
| 2 | aquifers | `AquiferMixin` | underground aquifers making their own water and lava from `lavaNoise` |
| 3 | features | `SpringFeatureMixin` | water and lava springs, Overworld and Nether |
| | | `LakeFeatureMixin` | lava lakes — the basin and its barrier still get carved, only the fluid is skipped |
| | | `DeltaFeatureMixin` | the lava in basalt deltas, leaving magma rims around dry craters |
| 4 | carvers | `WorldCarverMixin` | cave carvers pouring lava in below y=-54 |
| | | `NetherWorldCarverMixin` | the Nether carver flooding its own caves below y=31 |
| 5 | surface rules | `SurfaceRuleStateMixin` | the Nether's surface rule painting lava into every hole below y=31 |
| — | surface look | `SurfaceSystemMixin` | a drained sea floor growing grass instead of staying bare |
| — | structures | `RuinedPortalPieceMixin` | ruined portal frames being real obsidian |
| — | structures | `OceanMonumentPieceMixin` | the monument arriving as a cube of water hanging in the air |
| — | loot | `LootTableMixin` | *(opt-in, off by default)* obsidian coming out of the two chests that carry it |

Each one carries a comment saying *why that seam* and what diverges between 1.21.1 and 26.2. Three
traps are worth repeating here:

- **Fluid comes from more places than the fluid picker.** The last two mechanisms were found by
  generating a world and reading the region files back, not by reading the code: the **carvers**
  left one lava chunk in five hundred, and the Nether's **surface rule** — a data-driven
  `"if hole then lava"` in `nether.json` — left flat pools at y 21-23 that had survived all seven
  earlier rules.
- **An ambiguous method name resolves to nothing, silently-ish.** Where a target name is overloaded
  (`makeSettings`) or has a synthetic bridge (`carveBlock`), Mixin reports *"Scanned 0 target(s)"*
  rather than an ambiguity error. Both are targeted by full descriptor.
- **javap omits the owner when it is the current class.** `DeltaFeature` calls the `setBlock` it
  inherits from `Feature`, but the constant pool says `DeltaFeature` — naming `Feature` matches
  nothing.

---

## Verifying

"It compiles" proves nothing here — Mixin resolves none of its targets until runtime — and "the
server started" proves little more, because half the targets are cold classes.

**`DrainedSelfCheck`** force-loads every mixin target at init, turning a bad injection point into an
immediate, loud failure. It is off unless you ask for it:

```bash
DRAINED_SELFCHECK=true ./gradlew :fabric:runServer
```

Run it on every line of MATRIX after a Minecraft version bump. It is what caught two of this mod's
build-time bugs.

### What has actually been measured

Worlds were generated, then the region files were read back and the block palettes counted.

**Fluids** — same seed, mod loaded both times, draining toggled in the config:

| 26.2 / Fabric | chunks | `minecraft:water` | `minecraft:lava` |
|---|---:|---:|---:|
| draining off (control) | 529 | 21 chunks | 5 chunks |
| draining on | 529 | **0** | **0** |

At scale, with the Nether force-loaded too:

| drained world | chunks | `water` | `lava` |
|---|---:|---:|---:|
| Overworld | 3686 | 28 chunks — **every one inside a structure** | **0** |
| Nether | 2436 | **0** | 1 block, inside a nether fortress |

Structure fluid is kept on purpose (see above), so those two figures are the intended end state,
not a leak.

**The portal lock** — a ruined portal found in the generated world: **1 real obsidian, 16 crying**.
Right on target for the original 0.15 setting; the default is now 0.45.

**The loot filter, when switched on** — 160 draws from the two filtered tables via `/loot spawn`:

| item | count |
|---|---:|
| `obsidian` | **0** |
| `lodestone` | 62 |
| `iron_nugget` | 57 |
| `flint_and_steel` | 55 |
| `iron_ingot` | 45 |

`lodestone` matters: that pool is one 26.2 **added** to the ruined portal table. It comes through
untouched, which is the whole argument for filtering in code instead of shipping a replacement
loot-table JSON that would have frozen the table at whatever version it was copied from.

A separate control run confirmed the filter is targeted, not global: `chests/bastion_other` still
yields obsidian and crying obsidian, as intended.

### Still to confirm in game

Frozen biomes still making ice; ruined portal placement surviving the missing lava; fortresses and
bastions over the drained lava sea; and a real timed playthrough to settle `real_obsidian_chance`.

---

## License

MIT. Support: <https://ko-fi.com/acesoverdeuces777>
