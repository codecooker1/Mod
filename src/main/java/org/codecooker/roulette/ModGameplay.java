package org.codecooker.roulette;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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

import java.util.*;

public class ModGameplay {

    private static final Map<UUID, Revolver> REVOLVERS = new HashMap<>();
    private static final Object2LongMap<UUID> GRACE_UNTIL = new Object2LongOpenHashMap<>();
    static { GRACE_UNTIL.defaultReturnValue(0L); }

    private enum Source {Break, Place}

    private static final Component BOOM = Component.literal("BOOM!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);

    private record PendingPlace(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState beforeState) {}

    private static final List<PendingPlace> PENDING_PLACES = new ArrayList<>();

    public static final class Revolver {
        private boolean[] cylinder = new boolean[0];
        private int bullets;
        private int pos;
        private int clicks;

        void load(int chambers, int bullets, RandomSource random) {
            bullets = Math.min(bullets, chambers);

            cylinder = new boolean[chambers];
            this.bullets = bullets;

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
            cylinder[pos] = false;

            if (respin) {
                pos = random.nextInt(cylinder.length);
            } else{
                pos = (pos + 1) % cylinder.length;
            }

            return bang;
        }

        boolean cycleComplete(){
            return clicks >= cylinder.length;
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
            level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        if(!config.showCylinder) return;

        String text;
        if(config.respinAfterClick){
            text = "Click.... (Cylinder Spun)";
        } else  {
            int used = revolver.clicks();
            text = "Click  [" + "o".repeat(used) + "*".repeat(revolver.chambers() - used) + "]";
        }

        player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.GRAY), true);
    }

    private static boolean canPullTrigger(ServerPlayer player, ServerLevel level, TNTRouletteConfig config, Source source) {
        if (!config.enabled || player.isSpectator()) return false;
        if (player.getAbilities().instabuild && !config.affectCreative) return false;
        if (source == Source.Place && !config.placeEnabled) return false;
        if (source == Source.Break && !config.breakEnabled) return false;
        return level.getGameTime() >= GRACE_UNTIL.getLong(player.getUUID());
    }

    private static boolean passedBaseChance(RandomSource random, TNTRouletteConfig config, Source source) {
        int baseChance = source == Source.Place ? config.placeBaseChance : config.breakBaseChance;
        return random.nextInt(100) < baseChance;
    }

    private static boolean pullTrigger(ServerPlayer player, ServerLevel level, BlockPos pos, TNTRouletteConfig config, Source source) {
        UUID id = player.getUUID();
        RandomSource random = player.getRandom();

        Revolver revolver = REVOLVERS.computeIfAbsent(id, k -> new Revolver());
        revolver.ensureLoaded(config, random);

        boolean gotBang = revolver.pull(random, config.respinAfterClick);

        if(!gotBang){
            showClick(player, level, pos, revolver, config);
            if(revolver.cycleComplete()){
                revolver.load(config.chambers, config.bullets, random);
                System.out.println(revolver.clicks());
            }
            return false;
        }

        if(revolver.cycleComplete()){
            revolver.load(config.chambers, config.bullets, random);
            System.out.println(revolver.clicks());
        }

        GRACE_UNTIL.put(id, level.getGameTime() + config.graceSeconds * 20L);

        if (config.showCylinder) {
            player.displayClientMessage(BOOM, true);
        }

        if (random.nextInt(100) < config.misfireChance) {
            spawnDud(level, pos);
            return false;
        } else {
            spawnTnt(level, pos, config.fuseTicks);
            if (config.announceInChat) {
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

        TNTRouletteConfig config = Roulette.config();
        if(!canPullTrigger(sp, sl, config, Source.Place)) return InteractionResult.PASS;
        if (blockHitResult.getType() != BlockHitResult.Type.BLOCK) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(interactionHand);
        if (!(stack.getItem() instanceof BlockItem)) return InteractionResult.PASS;

        if(!passedBaseChance(player.getRandom(), config, Source.Place)) return InteractionResult.PASS;

        BlockPos target = new BlockPlaceContext(player, interactionHand, stack, blockHitResult).getClickedPos().immutable();

        PENDING_PLACES.removeIf(p -> p.player() == sp && p.pos().equals(target));
        PENDING_PLACES.add(new PendingPlace(sp, sl, target, sl.getBlockState(target)));

        return InteractionResult.PASS;
    }

    private static void processPendingPlace() {
        if (PENDING_PLACES.isEmpty()) return;

        try {
            TNTRouletteConfig config = Roulette.config();

            for  (PendingPlace p : PENDING_PLACES) {
                ServerLevel level = p.level;
                ServerPlayer player = p.player;

                if(player.isRemoved() || player.level() != level) continue;

                BlockState now = level.getBlockState(p.pos());

                if(now.isAir() || now == p.beforeState()) continue;

                if(!canPullTrigger(player, level, config, Source.Place)) continue;

                if(pullTrigger(player, level, p.pos(), config, Source.Place)){
                    level.destroyBlock(p.pos(), false);
                }
            }
        } finally {
            PENDING_PLACES.clear();
        }
    }

    private static void onBreak(Level level, Player player, BlockPos blockPos, BlockState blockState, @Nullable BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) return;

        TNTRouletteConfig config = Roulette.config();
        if(!canPullTrigger(sp, sl, config, Source.Break)) return;

        if(config.ignoreInstantBreakBlocks && blockState.getDestroySpeed(level, blockPos) <= 0.4f ) return;
        if(!passedBaseChance(sp.getRandom(), config, Source.Break)) return;

        pullTrigger(sp, sl, blockPos, config, Source.Break);
    }



    public static void init(){
        PlayerBlockBreakEvents.AFTER.register(ModGameplay::onBreak);
        UseBlockCallback.EVENT.register(ModGameplay::onPlace);
        ServerTickEvents.END_SERVER_TICK.register(server -> processPendingPlace());

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            REVOLVERS.remove(handler.getPlayer().getUUID());
            GRACE_UNTIL.removeLong(handler.getPlayer().getUUID());
            PENDING_PLACES.removeIf(p -> p.player() == handler.getPlayer());
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            REVOLVERS.clear();
            GRACE_UNTIL.clear();
            PENDING_PLACES.clear();
        });
    }

}




