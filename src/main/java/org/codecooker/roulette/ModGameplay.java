package org.codecooker.roulette;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModGameplay {

    private static final Map<UUID, Revolver> REVOLVERS = new HashMap<>();
    private static final Object2LongMap<UUID> GRACE_UNTIL = new Object2LongOpenHashMap<>();
    static { GRACE_UNTIL.defaultReturnValue(0L); }

    private enum Source {Break, Place};



    public static final class Revolver {
        private boolean[] cylinder = new boolean[0];
        private int bullets;
        private int remainingBullets = 0;
        private int pos;
        private int clicks;

        void load(int chambers, int bullets, RandomSource random) {
            bullets = Math.min(bullets, chambers);

            cylinder = new boolean[chambers];
            this.bullets = bullets;
            this.remainingBullets = bullets;

            for (int i = 0; i < chambers; i++) {
                cylinder[i] = i < bullets;
            }

            for (int i = chambers - 1; i > 0; i--) {
                int index = random.nextInt(i + 1);
                boolean temp = cylinder[index];
                cylinder[index] = cylinder[i];
                cylinder[i] = temp;
            }

            pos = random.nextInt(chambers);
            clicks = 0;
        }

        void ensureLoaded(TNTRouletteConfig config, RandomSource random) {
            if(cylinder.length != config.chambers || bullets != config.bullets) {
                load(config.chambers, config.bullets, random);
            }
        }

        boolean pull(RandomSource random, boolean respin) {
            boolean bang = cylinder[pos];
            clicks++;

            if (respin && !bang) {
                pos = random.nextInt(cylinder.length);
            } else{
                pos = (pos + 1) % cylinder.length;
            }

            return bang;
        }

        int clicks() {
            return clicks;
        }
        int chambers() {
            return cylinder.length;
        }
    }

    private static void showClick(Player player, Level level, BlockPos pos, Revolver revolver, TNTRouletteConfig config){
        if(config.clickSound){
            level.playLocalSound(pos, SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0f, 1.0f, false);
        }

        if(!config.showCylinder) return;

        String text;
        if(config.respinAfterClick){
            text = "Click.... (Cylinder Spun)";
        } else  {
            int used = revolver.clicks();
            text = "Click  [" + "○".repeat(used) + "●".repeat(revolver.chambers() - used) + "]";
        }

        player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GRAY), true);
    }

    private static boolean pullTrigger(ServerPlayer player, ServerLevel level, BlockPos pos, Source source) {
        TNTRouletteConfig config = Roulette.config();
        UUID id = player.getUUID();

        if (!config.enabled || player.isSpectator() ||
                (player.getAbilities().instabuild && !config.affectCreative) ||
                (level.getGameTime() < GRACE_UNTIL.getLong(id))) {
            return false;
        }

        int baseChance =0;
        if(source == Source.Break){
            baseChance = config.breakBaseChance;
        } else if(source == Source.Place){
            baseChance = config.placeBaseChance;
        }

        RandomSource random = player.getRandom();

        if(random.nextInt(100) >= baseChance){
            return false;
        }

        Revolver revolver = REVOLVERS.computeIfAbsent(id, k -> new Revolver());
        revolver.ensureLoaded(config, random);

        if(!revolver.pull(random, config.respinAfterClick)){
            showClick(player, level, pos, revolver, config);
            return false;
        }

        revolver.remainingBullets--;
        if(revolver.remainingBullets == 0){
            revolver.load(config.chambers, config.bullets, random);
        }

        GRACE_UNTIL.put(id, level.getGameTime() + config.graceSeconds * 20L);

        if (config.showCylinder) {
            player.displayClientMessage(Component.literal("BOOM!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        }

        if (random.nextInt(100) < config.misfireChance) {
            spawnDud(level, pos);
        } else {
            spawnTnt(level, pos, config.fuseTicks);
            if (config.announceInChat && level.getServer() != null) {
                level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("").append(player.getDisplayName()).append(Component.literal(" GOT THE BOOM!")), false);
            }
        }

        return true;
    }

    private static void spawnTnt(ServerLevel level, BlockPos pos, int fuseTicks) {
        PrimedTnt tnt = EntityType.TNT.create(level, EntitySpawnReason.EVENT);
        if(tnt == null) return;

        tnt.setPos(pos.getX() + 0.5f, pos.getY(), pos.getZ() + 0.5f);
        tnt.setFuse(fuseTicks);

        level.addFreshEntity(tnt);
        level.playSound(null, pos, SoundEvents.TNT_PRIMED,  SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static void spawnDud(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.TNT_PRIMED,  SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE,  SoundSource.PLAYERS, 1.0f, 1.0f);

        level.sendParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, 20, .6f, .6f, .6f, .2f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, 20, .3f, .3f, .3f, .1f);
    }

    private static InteractionResult onPlace(Player player, Level level, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!Roulette.config().placeEnabled) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(interactionHand);
        if (!(stack.getItem() instanceof BlockItem)) return InteractionResult.PASS;

        BlockPos pos = blockHitResult.getBlockPos();
        if (!player.isSecondaryUseActive() && level.getBlockState(pos).getMenuProvider(level, pos) != null) return InteractionResult.PASS;

        BlockPlaceContext ctx = new BlockPlaceContext(player, interactionHand, stack, blockHitResult);
        if (!ctx.canPlace()) return InteractionResult.PASS;

        if (pullTrigger(sp, sl, ctx.getClickedPos(), Source.Place)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void onBreak(Level level, Player player, BlockPos blockPos, BlockState blockState, @Nullable BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) return;

        TNTRouletteConfig config = Roulette.config();
        if(!config.breakEnabled) return;

        if(config.ignoreInstantBreakBlocks && blockState.getDestroySpeed(level, blockPos) <= 0.4f ) return;

        pullTrigger(sp, sl, blockPos, Source.Break);
    }



    public static void init(){
        PlayerBlockBreakEvents.AFTER.register(ModGameplay::onBreak);
        UseBlockCallback.EVENT.register(ModGameplay::onPlace);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            REVOLVERS.remove(handler.getPlayer().getUUID());
            GRACE_UNTIL.removeLong(handler.getPlayer().getUUID());
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            REVOLVERS.clear();
            GRACE_UNTIL.clear();
        });
    }

}




