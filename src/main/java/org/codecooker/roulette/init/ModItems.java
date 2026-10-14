package org.codecooker.roulette.init;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import org.codecooker.roulette.Roulette;

import java.util.function.Function;

public class ModItems {
    public static final Item EXAMPLE_ITEM = register("example_item", ExampleItem::new, new ExampleItem.Properties());

    public static <GenericItem extends Item> GenericItem register(String name, Function<Item.Properties, GenericItem> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Roulette.MOD_ID, name));

        GenericItem item = itemFactory.apply(settings.setId(itemKey));

        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }



    public static void init(){
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register((itemGroup) -> itemGroup.accept(ModItems.EXAMPLE_ITEM));

        FuelRegistryEvents.BUILD.register((builder, context) -> {
            builder.add(ModItems.EXAMPLE_ITEM, 30 * 20);
        });
    }
}


