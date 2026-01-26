package org.codecooker.mod.init;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.codecooker.mod.Mod;

import java.util.function.Function;
import java.util.concurrent.ThreadLocalRandom;

public class ModItems {
    public static final Item EXAMPLE_ITEM = register("example_item", ExampleItem::new, new ExampleItem.Properties());


    public static <GenericItem extends Item> GenericItem register(String name, Function<Item.Properties, GenericItem> itemFactory, Item.Properties settings) {
        // Create the item key.
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mod.MOD_ID, name));

        // Create the item instance.
        GenericItem item = itemFactory.apply(settings.setId(itemKey));

        // Register the item.
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }



    public static void init(){
        // Get the event for modifying entries in the ingredients group.
        // And register an event handler that adds our suspicious item to the ingredients group.
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register((itemGroup) -> itemGroup.accept(ModItems.EXAMPLE_ITEM));

        FuelRegistryEvents.BUILD.register((builder, context) -> {
            builder.add(ModItems.EXAMPLE_ITEM, 30 * 20);
        });

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {

            if (level.isClientSide()){
                return ;
            }

            PrimedTnt tnt = EntityType.TNT.create(level, EntitySpawnReason.EVENT);

            int chance = ThreadLocalRandom.current().nextInt(100);

            if (tnt != null && chance <= 1) {
                tnt.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                tnt.setFuse(10); // 1 second == 20 ticks
                level.addFreshEntity(tnt);
            }
        });

        UseBlockCallback.EVENT.register(((player, level, hand, hitResult) -> {
            if(level.isClientSide()){
                return InteractionResult.PASS;
            }

            ItemStack item = player.getItemInHand(hand);
            if (!(item.getItem() instanceof BlockItem)){
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos().relative(hitResult.getDirection());

            if(level.getBlockState(pos).canBeReplaced()){

                int chance = ThreadLocalRandom.current().nextInt(100);
                PrimedTnt tnt = EntityType.TNT.create(level, EntitySpawnReason.EVENT);

                if (tnt != null && chance <= 2) {

                    tnt.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    tnt.setFuse(10); // 1 second == 20 ticks

                    level.addFreshEntity(tnt);

                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }));

        
    }
}


