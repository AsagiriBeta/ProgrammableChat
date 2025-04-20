package AsagiriBeta.programmablechat.client;

import net.fabricmc.api.ModInitializer;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;

public class ModEntryPoint implements ModInitializer, ModMenuApi {
    @Override
    public void onInitialize() {
        new AutoMessageMod().onInitialize();
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new AutoMessageScreen();
    }
}