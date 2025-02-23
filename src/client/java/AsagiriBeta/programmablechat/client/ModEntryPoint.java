package AsagiriBeta.programmablechat.client;

import net.fabricmc.api.ModInitializer;

public class ModEntryPoint implements ModInitializer {
    @Override
    public void onInitialize() {
        new AutoMessageMod().onInitialize();
    }
}