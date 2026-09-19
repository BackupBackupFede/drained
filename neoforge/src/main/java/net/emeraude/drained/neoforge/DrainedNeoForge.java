package net.emeraude.drained.neoforge;

import net.emeraude.drained.Drained;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

/**
 * NeoForge entry point. Every rule is a mixin in {@code common/}; all this does is hand the
 * loader's config directory over.
 */
@Mod(Drained.MOD_ID)
public final class DrainedNeoForge {

    public DrainedNeoForge(IEventBus modBus, ModContainer container) {
        Drained.init(FMLPaths.CONFIGDIR.get());
    }
}
