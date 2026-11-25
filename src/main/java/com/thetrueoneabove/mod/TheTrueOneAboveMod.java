package com.thetrueoneabove.mod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Mod(TheTrueOneAboveMod.MODID)
public class TheTrueOneAboveMod {
    public static final String MODID = "thetrueoneabove";
    public static final String TRANSFORMED_TAG = "thetrueoneabove_transformed";

    private static final int TIME_STOP_TICKS = 20 * 10;

    // Time-stop state (server thread only).
    private static int freezeTicks = 0;
    private static UUID freezeCaster = null;
    private static final Map<UUID, double[]> frozen = new HashMap<>();

    public TheTrueOneAboveMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        String message = event.getMessage().getString().trim().toLowerCase(Locale.ROOT);

        if (message.equals("let there be light")) {
            event.setCanceled(true);
            letThereBeLight(event.getPlayer());
        } else if (message.contains("the first and the last")) {
            event.setCanceled(true);
            startTimeStop(event.getPlayer());
        }
    }

    private void letThereBeLight(ServerPlayer caster) {
        transform(caster);

        MinecraftServer server = caster.getServer();
        if (server == null) return;

        // Remove non-player entities in every loaded dimension. Players are explicitly excluded.
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Player)) {
                    entity.discard();
                }
            }
        }

        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new WhiteFlashPacket());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(Component.literal("TheTrueOneAbove has spoken: LET THERE BE LIGHT."));
        }
    }

    // ---------------------------------------------------------------- time stop

    private void startTimeStop(ServerPlayer caster) {
        MinecraftServer server = caster.getServer();
        if (server == null) return;

        frozen.clear();
        freezeTicks = TIME_STOP_TICKS;
        freezeCaster = caster.getUUID();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new TimeStopPacket(TIME_STOP_TICKS, player.getUUID().equals(freezeCaster)));
        }
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("").append(caster.getDisplayName()).append(" has stopped time for 10 seconds."),
                false);
    }

    private static void endTimeStop() {
        freezeTicks = 0;
        freezeCaster = null;
        frozen.clear();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && freezeTicks > 0 && --freezeTicks == 0) {
            endTimeStop();
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        endTimeStop();
    }

    /** Living, non-player entities skip their whole tick while time is stopped (AI, gravity, effects, aging). */
    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (freezeTicks <= 0) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity instanceof Player) return;
        event.setCanceled(true);
    }

    /** Everything else (items, arrows, TNT, minecarts, ...) is pinned to where it was when time stopped. */
    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER || freezeTicks <= 0) return;
        if (!(event.level instanceof ServerLevel level)) return;

        for (Entity e : level.getAllEntities()) {
            if (e instanceof Player || e instanceof LivingEntity) continue;
            if (isExemptFromFreeze(e)) continue;

            double[] s = frozen.get(e.getUUID());
            if (s == null) {
                s = new double[] {e.getX(), e.getY(), e.getZ(),
                        e.getDeltaMovement().x, e.getDeltaMovement().y, e.getDeltaMovement().z,
                        e instanceof PrimedTnt tnt ? tnt.getFuse() : 0};
                frozen.put(e.getUUID(), s);
            } else {
                e.setPos(s[0], s[1], s[2]);
                e.setDeltaMovement(s[3], s[4], s[5]);
                if (e instanceof PrimedTnt tnt) tnt.setFuse((int) s[6]);
            }
        }
    }

    private static boolean isExemptFromFreeze(Entity e) {
        // Vehicles the player is riding, and projectiles fired by the time-stopper, keep moving.
        for (Entity passenger : e.getPassengers()) {
            if (passenger instanceof Player) return true;
        }
        if (e instanceof Projectile projectile && projectile.getOwner() != null
                && projectile.getOwner().getUUID().equals(freezeCaster)) {
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- transformation

    private static void transform(ServerPlayer player) {
        player.getPersistentData().putBoolean(TRANSFORMED_TAG, true);
        player.setCustomName(Component.literal("TheTrueOneAbove"));
        player.setCustomNameVisible(true);
        player.getAbilities().invulnerable = true;
        player.onUpdateAbilities();
        applyPowers(player);
    }

    private static void applyPowers(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60 * 60, 4, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60 * 60, 2, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20 * 60 * 60, 3, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60 * 60, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60 * 60, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 60, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60 * 60, 4, false, false, true));
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!player.getPersistentData().getBoolean(TRANSFORMED_TAG)) return;
        player.getAbilities().invulnerable = true;
        if (player.tickCount % 100 == 0) applyPowers(player);
        if (player.tickCount % 8 == 0) {
            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                    5, 0.45, 0.75, 0.45, 0.015);
        }
    }
}
