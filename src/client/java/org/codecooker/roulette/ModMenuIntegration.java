package org.codecooker.roulette;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory(){
        return parent -> AutoConfig.getConfigScreen(TNTRouletteConfig.class, parent).get();
    }
}
