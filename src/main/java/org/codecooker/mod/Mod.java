package org.codecooker.mod;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.codecooker.mod.init.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class Mod implements ModInitializer {
    public static final String MOD_ID = "mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static TNTRouletteConfig CONFIG;
    public final Random random = new Random();

    @Override
    public void onInitialize() {
        LOGGER.info("Hello");

        AutoConfig.register(TNTRouletteConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(TNTRouletteConfig.class).getConfig();

        ModItems.init();
        LOGGER.info("Loaded Items.");
        ModGameplay.init();
        LOGGER.info("Loaded Gameplay.");

    }

    public static Identifier id(String path){
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }


}
