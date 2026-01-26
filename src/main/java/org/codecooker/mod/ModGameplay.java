package org.codecooker.mod;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

public class ModGameplay {

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    private static void spawnTNT(Level level, BlockPos pos){
        PrimedTnt tnt = EntityType.TNT.create(level, EntitySpawnReason.EVENT);

        assert tnt != null;

        tnt.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        tnt.setFuse(Mod.CONFIG.fuseTicks);

        level.addFreshEntity(tnt);
    }

    private static void playerBreaksBlock(Level level, Player player, BlockPos blockPos, BlockState blockState, @Nullable BlockEntity blockEntity) {
        if(level.isClientSide()){
            return;
        }

        if (random.nextInt(100) < Mod.CONFIG.breakChance){
            spawnTNT(level, blockPos);
        }
    }

    private static InteractionResult playerPlacesBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if(level.isClientSide()){
            return InteractionResult.PASS;
        }

        ItemStack item = player.getItemInHand(hand);
        if (!(item.getItem() instanceof BlockItem)){
            return InteractionResult.PASS;
        }

        BlockPos pos = hitResult.getBlockPos().relative(hitResult.getDirection());

        if(level.getBlockState(pos).canBeReplaced()){
            if (random.nextInt(100) < Mod.CONFIG.placeChance) {

                BlockPos placePos = hitResult.getBlockPos().relative(hitResult.getDirection());

                spawnTNT(level, placePos);

                if(!player.getAbilities().instabuild)
                    item.shrink(1);

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private static void registerEvents(){
        PlayerBlockBreakEvents.AFTER.register(ModGameplay::playerBreaksBlock);
        UseBlockCallback.EVENT.register(ModGameplay::playerPlacesBlock);
    }

    public static void init(){
        registerEvents();
    }
}
