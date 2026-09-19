package net.emeraude.drained.fabric;

import net.emeraude.drained.Drained;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric entry point. Every rule is a mixin in {@code common/}; all this does is hand the loader's
 * config directory over, which is the one thing the two loaders spell differently.
 *
 * <p>Deliberately no Fabric API dependency: the mod ships no assets, no data pack and registers
 * nothing, so neither {@code fabric-resource-loader-v0} nor {@code fabric-registry-sync-v0} has
 * anything to do, and plain Fabric Loader is one less install requirement on a server.
 */
public final class DrainedFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Drained.init(FabricLoader.getInstance().getConfigDir());
    }
}
