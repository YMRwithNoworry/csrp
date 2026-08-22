package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.PrimitiveVariantEntity;
import alku.csrp.registry.ModDamageTypes;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;

/** Server-authoritative Book of Vengeance attacks and Ricardo drop handling. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class BookOfVengeanceEvents {
    public static final int SLAM_COOLDOWN_TICKS = 80;
    public static final int PULSE_COOLDOWN_TICKS = 40;
    private static final int MARK_DELAY_TICKS = 10;
    private static final int FIRST_DASH_TICKS = 12;
    private static final int BOUNCE_TICKS = 8;
    private static final int SECOND_DASH_TICKS = 10;
    private static final int EFFECT_DURATION_TICKS = 100;
    private static final double CONTACT_DISTANCE_SQR = 6.25D;
    private static final double PULSE_RADIUS = 6.0D;
    private static final Map<ServerLevel, List<SlamChain>> CHAINS = new WeakHashMap<>();

    private BookOfVengeanceEvents() {
    }

    public static void beginSlamChain(ServerLevel level, ServerPlayer player, LivingEntity target) {
        List<SlamChain> chains = CHAINS.computeIfAbsent(level, ignored -> new ArrayList<>());
        chains.removeIf(chain -> chain.playerId.equals(player.getUUID()));
        chains.add(new SlamChain(player.getUUID(), target.getUUID(), target.position()));
        sendPurpleMark(level, player.getEyePosition(), target.getBoundingBox().getCenter());
        level.playSound(null, target.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 1.2F, 0.65F);
    }

    public static void pulse(ServerLevel level, ServerPlayer player) {
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(PULSE_RADIUS),
                entity -> entity.isAlive() && !entity.isSpectator())) {
            Vec3 away = entity.position().subtract(player.position());
            Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
            if (horizontal.lengthSqr() < 1.0E-4D) {
                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                horizontal = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            } else {
                horizontal = horizontal.normalize();
            }
            double distanceScale = 1.0D - Math.min(0.55D, Math.sqrt(away.lengthSqr()) / 12.0D);
            entity.push(horizontal.x * 1.65D * distanceScale, 0.55D,
                    horizontal.z * 1.65D * distanceScale);
        }
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0D, player.getZ(),
                48, 2.4D, 0.7D, 2.4D, 0.12D);
        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0D, player.getZ(),
                32, 1.8D, 0.5D, 1.8D, 0.18D);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.0F, 1.45F);
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {return;}
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<SlamChain> chains = CHAINS.get(level);
        if (chains == null) {
            return;
        }
        Iterator<SlamChain> iterator = chains.iterator();
        while (iterator.hasNext()) {
            SlamChain chain = iterator.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(chain.playerId);
            if (player == null || !player.isAlive() || player.level() != level) {
                iterator.remove();
                continue;
            }
            LivingEntity target = resolveTarget(level, chain.targetId);
            if (target != null) {
                chain.lastTargetPosition = target.position();
            }
            if (tickChain(level, player, target, chain)) {
                iterator.remove();
            }
        }
        if (chains.isEmpty()) {
            CHAINS.remove(level);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void dropFromRicardo(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof PrimitiveVariantEntity reeker) || !reeker.isRicardoVariant()
                || !(entity.level() instanceof ServerLevel level)
                || event.getDrops().stream().anyMatch(drop -> drop.getItem().is(ModItems.BOOK_OF_VENGEANCE.get()))) {
            return;
        }
        event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(),
                new ItemStack(ModItems.BOOK_OF_VENGEANCE.get())));
    }

    private static boolean tickChain(ServerLevel level, ServerPlayer player,
            LivingEntity target, SlamChain chain) {
        chain.phaseTicks++;
        switch (chain.phase) {
            case MARK -> {
                if (chain.phaseTicks % 2 == 0) {
                    sendPurpleMark(level, player.getEyePosition(), targetCenter(target, chain));
                }
                if (chain.phaseTicks >= MARK_DELAY_TICKS) {
                    chain.advance(Phase.FIRST_DASH);
                    level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1,
                            SoundSource.PLAYERS, 1.0F, 0.8F);
                }
            }
            case FIRST_DASH -> {
                dash(player, targetCenter(target, chain), 1.55D);
                if (hasReached(player, target, chain) || chain.phaseTicks >= FIRST_DASH_TICKS) {
                    slam(level, player, target, chain.lastTargetPosition, 7.0F, false);
                    bounce(player, chain.lastTargetPosition);
                    chain.advance(Phase.BOUNCE);
                }
            }
            case BOUNCE -> {
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.6D,
                        player.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.01D);
                if (chain.phaseTicks >= BOUNCE_TICKS) {
                    chain.advance(Phase.SECOND_DASH);
                    level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2,
                            SoundSource.PLAYERS, 1.0F, 0.7F);
                }
            }
            case SECOND_DASH -> {
                dash(player, targetCenter(target, chain), 1.85D);
                if (hasReached(player, target, chain) || chain.phaseTicks >= SECOND_DASH_TICKS) {
                    slam(level, player, target, chain.lastTargetPosition, 11.0F, true);
                    return true;
                }
            }
        }
        return false;
    }

    private static LivingEntity resolveTarget(ServerLevel level, UUID targetId) {
        Entity entity = level.getEntity(targetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static Vec3 targetCenter(LivingEntity target, SlamChain chain) {
        return target == null ? chain.lastTargetPosition.add(0.0D, 0.8D, 0.0D)
                : target.getBoundingBox().getCenter();
    }

    private static void dash(ServerPlayer player, Vec3 destination, double speed) {
        Vec3 direction = destination.subtract(player.getEyePosition());
        if (direction.lengthSqr() > 1.0E-4D) {
            player.setDeltaMovement(direction.normalize().scale(speed));
            player.hurtMarked = true;
        }
    }

    private static boolean hasReached(ServerPlayer player, LivingEntity target, SlamChain chain) {
        return target != null
                ? player.getBoundingBox().inflate(0.75D).intersects(target.getBoundingBox())
                : player.position().distanceToSqr(chain.lastTargetPosition) <= CONTACT_DISTANCE_SQR;
    }

    private static void bounce(ServerPlayer player, Vec3 targetPosition) {
        Vec3 away = player.position().subtract(targetPosition);
        away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 1.0E-4D) {
            away = player.getLookAngle().multiply(-1.0D, 0.0D, -1.0D);
        }
        if (away.lengthSqr() < 1.0E-4D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        }
        away = away.normalize();
        player.setDeltaMovement(away.x * 1.05D, 0.78D, away.z * 1.05D);
        player.hurtMarked = true;
    }

    private static void slam(ServerLevel level, ServerPlayer player, LivingEntity target,
            Vec3 position, float damage, boolean summonLightning) {
        sendSlamParticles(level, position, summonLightning);
        level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, summonLightning ? 1.25F : 0.9F,
                summonLightning ? 0.7F : 1.1F);
        if (target != null && target.isAlive()) {
            target.invulnerableTime = 0;
            target.hurt(vengeanceDamage(level, player), damage);
            target.addEffect(new MobEffectInstance(ModMobEffects.BLEED.get(), EFFECT_DURATION_TICKS,
                    0, false, true), player);
            target.addEffect(new MobEffectInstance(ModMobEffects.DEBAR.get(), EFFECT_DURATION_TICKS,
                    0, false, true), player);
            target.addEffect(new MobEffectInstance(ModMobEffects.DOD_SMOKE_TRAIL.get(),
                    EFFECT_DURATION_TICKS, 0, false, true), player);
        }
        if (summonLightning) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(position.x, position.y, position.z);
                lightning.setCause(player);
                level.addFreshEntity(lightning);
            }
        }
    }

    private static DamageSource vengeanceDamage(ServerLevel level, ServerPlayer player) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ModDamageTypes.RICARDO), player);
    }

    private static void sendPurpleMark(ServerLevel level, Vec3 from, Vec3 target) {
        Vec3 path = target.subtract(from);
        for (int step = 0; step <= 14; step++) {
            Vec3 point = from.add(path.scale(step / 14.0D));
            level.sendParticles(ParticleTypes.WITCH, point.x, point.y, point.z,
                    1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
        for (int step = 0; step <= 8; step++) {
            level.sendParticles(ParticleTypes.WITCH, target.x, target.y - 0.8D + step * 0.2D,
                    target.z, 1, 0.03D, 0.0D, 0.03D, 0.0D);
        }
    }

    private static void sendSlamParticles(ServerLevel level, Vec3 position, boolean finalSlam) {
        level.sendParticles(ParticleTypes.DRAGON_BREATH, position.x, position.y + 0.4D, position.z,
                finalSlam ? 48 : 28, 1.1D, 0.45D, 1.1D, 0.08D);
        level.sendParticles(ParticleTypes.POOF, position.x, position.y + 0.4D, position.z,
                finalSlam ? 36 : 20, 0.9D, 0.35D, 0.9D, 0.18D);
        level.sendParticles(ParticleTypes.WITCH, position.x, position.y + 0.8D, position.z,
                finalSlam ? 30 : 16, 0.8D, 0.7D, 0.8D, 0.06D);
    }

    private enum Phase {
        MARK,
        FIRST_DASH,
        BOUNCE,
        SECOND_DASH
    }

    private static final class SlamChain {
        private final UUID playerId;
        private final UUID targetId;
        private Vec3 lastTargetPosition;
        private Phase phase = Phase.MARK;
        private int phaseTicks;

        private SlamChain(UUID playerId, UUID targetId, Vec3 lastTargetPosition) {
            this.playerId = playerId;
            this.targetId = targetId;
            this.lastTargetPosition = lastTargetPosition;
        }

        private void advance(Phase next) {
            phase = next;
            phaseTicks = 0;
        }
    }
}
