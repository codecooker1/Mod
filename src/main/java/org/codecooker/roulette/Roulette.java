package org.codecooker.roulette;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.codecooker.roulette.init.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Roulette implements ModInitializer {
    public static final String MOD_ID = "roulette";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    @Override
    public void onInitialize() {
        LOGGER.info("Hello");

        AutoConfig.register(TNTRouletteConfig.class, GsonConfigSerializer::new);

        ModItems.init();
        LOGGER.info("Loaded Items.");
        ModGameplay.init();
        LOGGER.info("Loaded Gameplay.");

    }

    public static TNTRouletteConfig config(){
        return AutoConfig.getConfigHolder(TNTRouletteConfig.class).getConfig();
    }

    public static Identifier id(String path){
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }


}
