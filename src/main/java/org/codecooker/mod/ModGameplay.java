package org.codecooker.mod;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ModGameplay {

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    private static final Object2IntMap<UUID> playerStreaks = new Object2IntOpenHashMap<>();

    static {
        playerStreaks.defaultReturnValue(0);
    }

    private static final int BASE_CHANCE = 1;
    private static final int ESCALATION_INCREMENT = 2;

    private static int getPlayerStreak(Player player) {
        return playerStreaks.getInt(player.getUUID());
    }

    private static void incrementPlayerStreak(Player player) {
        playerStreaks.put(player.getUUID(), getPlayerStreak(player) + 1);
    }

    private static void resetStreak(Player player) {
        playerStreaks.removeInt(player.getUUID());
    }

    private static void spawnTNT(Level level, BlockPos pos){
        PrimedTnt tnt = EntityType.TNT.create(level, EntitySpawnReason.EVENT);

        assert tnt != null;

        tnt.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        tnt.setFuse(Mod.CONFIG.fuseTicks);

        level.addFreshEntity(tnt);
    }

    private static void spawnDud(Level level, BlockPos pos){
        if (level instanceof ServerLevel serverLevel){
            serverLevel.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0f, 1.0f);
            serverLevel.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0f, 0.5f);

            serverLevel.sendParticles(
                    ParticleTypes.FIREWORK,
                    pos.getX() + .5f,
                    pos.getY() + .5f,
                    pos.getZ() + .5f,
                    20,
                    .6f, .6f, .6f,
                    .2f);
            serverLevel.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    pos.getX() + .5f,
                    pos.getY() + .5f,
                    pos.getZ() + .5f,
                    20,
                    .3f, .3f, .3f,
                    .1f);
        }
    }

    private static void playerBreaksBlock(Level level, Player player, BlockPos blockPos, BlockState blockState, @Nullable BlockEntity blockEntity) {
        if(level.isClientSide()){
            return;
        }

        int streak = getPlayerStreak(player);
        int currentChance = BASE_CHANCE + streak * ESCALATION_INCREMENT;

        if(random.nextInt(100) < currentChance){
            if(random.nextInt(100) < 10){
                spawnDud(level, blockPos);
                resetStreak(player);
            } else {
                spawnTNT(level, blockPos);
                resetStreak(player);
            }
        }
        else {
            incrementPlayerStreak(player);
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
            int streak = getPlayerStreak(player);
            int currentChance = BASE_CHANCE + streak * ESCALATION_INCREMENT;

            if(random.nextInt(100) < currentChance){
                BlockPos placePos = hitResult.getBlockPos().relative(hitResult.getDirection());

                if(random.nextInt(100) < 10){
                    spawnDud(level, placePos);
                    resetStreak(player);
                } else {
                    spawnTNT(level, placePos);
                    resetStreak(player);
                }

                if(player.getAbilities().instabuild){
                    item.shrink(1);
                }

                return InteractionResult.SUCCESS;
            }
            else {
                incrementPlayerStreak(player);
            }
        }
        return InteractionResult.PASS;
    }

    private static void registerEvents(){
        PlayerBlockBreakEvents.AFTER.register(ModGameplay::playerBreaksBlock);
        UseBlockCallback.EVENT.register(ModGameplay::playerPlacesBlock);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playerStreaks.remove(handler.player.getUUID());
        });
    }
 
    public static void init(){
        registerEvents();
    }
}




