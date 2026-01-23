package org.codecooker.mod.init;


import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class ExampleItem extends Item {

    public ExampleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.translatable("itemTooltip.mod.example_item").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public InteractionResult useOn(UseOnContext useOnContext){

        Level world = useOnContext.getLevel();
        BlockPos frontOfPlayer = useOnContext.getClickedPos();

        if(world.isClientSide()){
            return InteractionResult.PASS;
        }

        LightningBolt lightningBolt = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
        lightningBolt.setPos(frontOfPlayer.getCenter());
        world.addFreshEntity(lightningBolt);

        return InteractionResult.SUCCESS;
    }
}
