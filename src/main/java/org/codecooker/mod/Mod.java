package org.codecooker.mod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.codecooker.mod.init.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mod implements ModInitializer {
    public static final String MOD_ID = "mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        LOGGER.info("Hello");
        ModItems.init();
    }

    public static Identifier id(String path){
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }


}
