package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.entity.PrimitiveVariantEntity;
import alku.csrp.registry.ModDamageTypes;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Port of SRP 1.10.9 {@code item/VengeanceGrappleHandler} (441 lines) plus the Book of Vengeance's
 * knockback burst from {@code item/ItemBookOfVengeance}.
 *
 * <p>The grapple is a three stage server-authoritative sequence:</p>
 * <ol>
 *   <li><b>WINDUP</b> — 20 ticks of chain particles and ticking sounds;</li>
 *   <li><b>PULL</b> — the player is pulled at speed 1.8 toward the target until contact (2.2 blocks) or
 *       40 ticks, then <b>impact 1</b>: a 14.0F blast over a 5 block radius that debuffs and launches
 *       everything caught, and which damps the player's own horizontal velocity;</li>
 *   <li><b>BOUNCE</b> — 14 ticks with the original's three part velocity curve, ending in
 *       <b>impact 2</b>: 6.0F on the target plus a distance-scaled blast (up to 10.0F over 3.5 blocks)
 *       and a lightning strike.</li>
 * </ol>
 *
 * <p>Every number here is the original's; the per-stage specification is recorded in
 * {@code docs/gap/VENGEANCE_GRAPPLE_REPORT.md}.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class BookOfVengeanceEvents {
    /** {@code ItemBookOfVengeance.COOLDOWN_TICKS} — the grapple's vanilla item cooldown. */
    public static final int GRAPPLE_COOLDOWN_TICKS = 60;
    /** {@code ItemBookOfVengeance.KB_COOLDOWN_TICKS} — stored in the item's {@code srp_kb_next} tag. */
    public static final int KNOCKBACK_COOLDOWN_TICKS = 80;
    /** {@code ItemBookOfVengeance.RANGE}. */
    public static final double GRAPPLE_RANGE = 32.0D;
    /** {@code ItemBookOfVengeance.KB_STRENGTH}. */
    private static final double KB_STRENGTH = 2.2D;
    /** {@code ItemBookOfVengeance.KB_UP}. */
    private static final double KB_UP = 0.4D;
    /** {@code ItemBookOfVengeance.KB_SELF_JUMP_Y}. */
    private static final double KB_SELF_JUMP_Y = 0.42D;
    private static final double KB_RADIUS = 5.0D;

    private static final int WINDUP_TICKS = 20;
    private static final int PULL_MAX_TICKS = 40;
    private static final double PULL_SPEED = 1.8D;
    private static final double CONTACT_DISTANCE = 2.2D;
    private static final float IMPACT1_DAMAGE = 14.0F;
    private static final double IMPACT1_RADIUS = 5.0D;
    private static final double IMPACT1_FORWARD = 1.9D;
    private static final double IMPACT1_UP = 0.65D;
    private static final int BOUNCE_TICKS = 14;
    private static final double BOUNCE_PULL_SPEED = 2.35D;
    private static final float IMPACT2_TARGET_DAMAGE = 6.0F;
    private static final double IMPACT2_TARGET_UP = 0.35D;
    private static final double IMPACT2_RADIUS = 3.5D;
    private static final float IMPACT2_MAX_DAMAGE = 10.0F;
    private static final double IMPACT2_KNOCKBACK = 0.85D;
    private static final int EFFECT_DURATION_TICKS = 100;
    private static final double VELOCITY_CLAMP_HORIZONTAL = 2.8D;
    private static final double VELOCITY_CLAMP_VERTICAL = 2.2D;
    /** {@code ParticleVengeance.SPARKS}: {@code SPELL_MOB} tinted 0.65 / 0.1 / 0.9. */
    private static final float SPARK_RED = 0.65F;
    private static final float SPARK_GREEN = 0.1F;
    private static final float SPARK_BLUE = 0.9F;
    /** {@code ParticleVengeance.HEAVY_BLEED}: {@code SPELL_MOB} tinted 0.55 / 0.02 / 0.02. */
    private static final float BLEED_RED = 0.55F;
    private static final float BLEED_GREEN = 0.02F;
    private static final float BLEED_BLUE = 0.02F;

    private static final Map<ServerLevel, List<GrappleChain>> CHAINS = new WeakHashMap<>();

    private BookOfVengeanceEvents() {
    }

    /** {@code VengeanceGrappleHandler.start}. */
    public static void beginGrapple(ServerLevel level, ServerPlayer player, LivingEntity target) {
        List<GrappleChain> chains = CHAINS.computeIfAbsent(level, ignored -> new ArrayList<>());
        chains.removeIf(chain -> chain.playerId.equals(player.getUUID()));
        GrappleChain chain = new GrappleChain(player.getUUID(), target.getUUID(), target.position());
        chains.add(chain);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.VENGEANCE_PAPER.get(),
                SoundSource.PLAYERS, 2.0F, 1.0F);
    }

    /**
     * {@code ItemBookOfVengeance.doKnockbackBurst}: a 5 block horizontal burst that pushes everything
     * away with a distance falloff and lifts the caster.
     */
    public static void knockbackBurst(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.VENGEANCE_CHAIN_IMPACT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.VENGEANCE_IMPACT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        spawnPinkBurst(level, new Vec3(player.getX(), player.getY() + 0.9D, player.getZ()), 26);

        AABB box = player.getBoundingBox().inflate(KB_RADIUS, 1.5D, KB_RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                candidate -> candidate.isAlive() && candidate != player)) {
            double dx = entity.getX() - player.getX();
            double dz = entity.getZ() - player.getZ();
            double distSq = dx * dx + dz * dz;
            if (distSq < 1.0E-4D) {
                dx = (level.getRandom().nextDouble() - 0.5D) * 0.2D;
                dz = (level.getRandom().nextDouble() - 0.5D) * 0.2D;
                distSq = dx * dx + dz * dz;
            }
            double dist = Math.sqrt(distSq);
            double nx = dx / dist;
            double nz = dz / dist;
            double push = knockbackPush(dist);
            Vec3 velocity = entity.getDeltaMovement();
            entity.setDeltaMovement(velocity.x + nx * push,
                    Math.max(velocity.y, knockbackLift(dist)), velocity.z + nz * push);
            entity.syncVelocity = true;
        }

        Vec3 playerVelocity = player.getDeltaMovement();
        player.setDeltaMovement(playerVelocity.x, Math.max(playerVelocity.y, KB_SELF_JUMP_Y),
                playerVelocity.z);
        player.syncVelocity = true;
        player.setOnGround(false);
        player.fallDistance = 0.0F;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 1.6F);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1D, player.getZ(),
                8, 0.25D, 0.05D, 0.25D, 0.02D);
    }

    /**
     * {@code doKnockbackBurst}'s horizontal push: {@code 2.2 * (0.45 + 0.55 * falloff)} where
     * {@code falloff = 1 - min(1, distance / 5)}.
     */
    public static double knockbackPush(double distance) {
        return KB_STRENGTH * (0.45D + 0.55D * knockbackFalloff(distance));
    }

    /** {@code doKnockbackBurst}'s lift: {@code 0.4 + 0.15 * falloff}. */
    public static double knockbackLift(double distance) {
        return KB_UP + 0.15D * knockbackFalloff(distance);
    }

    private static double knockbackFalloff(double distance) {
        return 1.0D - Math.min(1.0D, distance / KB_RADIUS);
    }

    /** {@code spawnPinkBurst}: redstone dust tinted 1.0 / 0.25 / 0.85. */
    private static void spawnPinkBurst(ServerLevel level, Vec3 center, int points) {
        for (int index = 0; index < points; index++) {
            double ox = (level.getRandom().nextDouble() - 0.5D) * 1.1D;
            double oy = (level.getRandom().nextDouble() - 0.5D) * 0.6D;
            double oz = (level.getRandom().nextDouble() - 0.5D) * 1.1D;
            level.sendParticles(
                    ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 1.0F, 0.25F, 0.85F),
                    center.x + ox, center.y + oy, center.z + oz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<GrappleChain> chains = CHAINS.get(level);
        if (chains == null) {
            return;
        }
        Iterator<GrappleChain> iterator = chains.iterator();
        while (iterator.hasNext()) {
            GrappleChain chain = iterator.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(chain.playerId);
            LivingEntity target = resolveTarget(level, chain.targetId);
            if (player == null || !player.isAlive() || player.level() != level || target == null) {
                iterator.remove();
                continue;
            }
            chain.lastTargetPosition = target.position();
            // The original breaks the chain (with a fire-extinguish hiss) once line of sight is lost.
            if (!player.hasLineOfSight(target)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.4F);
                iterator.remove();
                continue;
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
                || event.getDrops().stream().anyMatch(drop -> drop.getItem().is(ModItems.BOOK_OF_VENGEANCE))) {
            return;
        }
        event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(),
                new ItemStack(ModItems.BOOK_OF_VENGEANCE.get())));
    }

    /** @return true when the chain is finished. */
    private static boolean tickChain(ServerLevel level, ServerPlayer player,
            LivingEntity target, GrappleChain chain) {
        chain.phaseTicks++;
        switch (chain.phase) {
            case WINDUP -> {
                spawnChainParticles(level, player, target, ChainParticle.SPELL);
                if (chain.phaseTicks % 4 == 0) {
                    Vec3 center = new Vec3(player.getX(), player.getY() + 0.2D, player.getZ());
                    spawnSparks(level, center, 16, 1.2F);
                }
                if (chain.phaseTicks % 6 == 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.25F, 1.8F);
                }
                if (chain.phaseTicks >= WINDUP_TICKS) {
                    level.playSound(null, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                            target.getZ(), ModSounds.VENGEANCE_CHAIN_IMPACT.get(), SoundSource.PLAYERS,
                            1.0F, 1.0F);
                    chain.advance(Phase.PULL);
                }
            }
            case PULL -> {
                spawnChainParticles(level, player, target, ChainParticle.CRIT);
                pullPlayerToward(player, target, PULL_SPEED);
                if (player.distanceTo(target) <= CONTACT_DISTANCE || chain.phaseTicks > PULL_MAX_TICKS) {
                    impact1(level, player, target, chain);
                    chain.advance(Phase.BOUNCE);
                }
            }
            case BOUNCE -> {
                // timer counts down from BOUNCE_TICKS in the original; phaseTicks counts up here.
                int timer = BOUNCE_TICKS - chain.phaseTicks + 1;
                if (timer % 4 == 0) {
                    for (UUID id : chain.affected) {
                        if (level.getEntity(id) instanceof LivingEntity affected && affected.isAlive()) {
                            spawnHeavyBleed(level, affected);
                        }
                    }
                }
                Vec3 velocity = player.getDeltaMovement();
                if (timer >= 12) {
                    player.setDeltaMovement(velocity.x * 0.4D, Math.max(velocity.y, 0.55D),
                            velocity.z * 0.4D);
                    player.syncVelocity = true;
                } else if (timer >= 6) {
                    player.setDeltaMovement(velocity.x * 0.65D, Math.max(velocity.y, -0.02D),
                            velocity.z * 0.65D);
                    player.syncVelocity = true;
                } else {
                    pullPlayerToward(player, target, BOUNCE_PULL_SPEED);
                }
                player.fallDistance = 0.0F;
                if (chain.phaseTicks >= BOUNCE_TICKS) {
                    impact2(level, player, target, chain);
                    return true;
                }
            }
        }
        return false;
    }

    /** {@code impact1}: 14.0F over 5 blocks, debuffs, launched along the caster's look vector. */
    private static void impact1(ServerLevel level, ServerPlayer player, LivingEntity target,
            GrappleChain chain) {
        Vec3 position = target.position();
        level.playSound(null, position.x, position.y, position.z, ModSounds.VENGEANCE_IMPACT.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, position.x, position.y, position.z, ModSounds.VENGEANCE_ROCK.get(),
                SoundSource.PLAYERS, 0.9F, 0.9F);
        spawnImpactDust(level, new Vec3(position.x, position.y, position.z), 28);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY() + 0.2D,
                target.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);

        AABB box = new AABB(
                target.getX() - IMPACT1_RADIUS, target.getY() - 2.0D, target.getZ() - IMPACT1_RADIUS,
                target.getX() + IMPACT1_RADIUS, target.getY() + 4.0D, target.getZ() + IMPACT1_RADIUS);
        Vec3 look = player.getLookAngle();
        double length = look.length();
        Vec3 direction = length > 1.0E-4D ? look.scale(1.0D / length) : look;

        chain.affected.clear();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                candidate -> candidate.isAlive() && candidate != player)) {
            if (entity.distanceTo(target) > IMPACT1_RADIUS) {
                continue;
            }
            chain.affected.add(entity.getUUID());
            applyVengeanceDebuffs(entity, player);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    ModSounds.VENGEANCE_WHOOSH.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
            entity.hurt(vengeanceDamage(level, player), IMPACT1_DAMAGE);
            Vec3 velocity = entity.getDeltaMovement();
            entity.setDeltaMovement(direction.x * IMPACT1_FORWARD,
                    Math.max(velocity.y, direction.y * IMPACT1_FORWARD + IMPACT1_UP),
                    direction.z * IMPACT1_FORWARD);
            entity.syncVelocity = true;
        }

        Vec3 playerVelocity = player.getDeltaMovement();
        player.setDeltaMovement(playerVelocity.x * 0.15D, playerVelocity.y, playerVelocity.z * 0.15D);
        player.syncVelocity = true;
        player.fallDistance = 0.0F;
    }

    /** {@code impact2AndLightning}: 6.0F on the target plus a distance-scaled 3.5 block blast. */
    private static void impact2(ServerLevel level, ServerPlayer player, LivingEntity target,
            GrappleChain chain) {
        Vec3 position = target.position();
        level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 0.7F, 1.2F);
        spawnLightningExplosion(level, new Vec3(position.x, position.y + 0.1D, position.z));
        level.playSound(null, position.x, position.y, position.z, ModSounds.VENGEANCE_ROCK.get(),
                SoundSource.PLAYERS, 0.9F, 0.9F);
        level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.15D, target.getZ(),
                3, 0.15D, 0.1D, 0.15D, 0.0D);

        if (target.isAlive()) {
            target.hurt(vengeanceDamage(level, player), IMPACT2_TARGET_DAMAGE);
            Vec3 velocity = target.getDeltaMovement();
            target.setDeltaMovement(velocity.x, Math.max(velocity.y, IMPACT2_TARGET_UP), velocity.z);
            target.syncVelocity = true;
        }

        AABB box = new AABB(
                target.getX() - IMPACT2_RADIUS, target.getY() - 1.5D, target.getZ() - IMPACT2_RADIUS,
                target.getX() + IMPACT2_RADIUS, target.getY() + 2.5D, target.getZ() + IMPACT2_RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                candidate -> candidate.isAlive() && candidate != player)) {
            double distance = entity.distanceTo(target);
            if (distance > IMPACT2_RADIUS) {
                continue;
            }
            float scale = 1.0F - (float) (distance / IMPACT2_RADIUS);
            entity.hurt(explosionDamage(level, player), IMPACT2_MAX_DAMAGE * scale);
            Vec3 push = new Vec3(entity.getX() - target.getX(), 0.0D, entity.getZ() - target.getZ());
            double pushLength = push.length();
            if (pushLength > 1.0E-4D) {
                push = push.scale(1.0D / pushLength);
                double knockback = IMPACT2_KNOCKBACK * scale;
                Vec3 velocity = entity.getDeltaMovement();
                entity.setDeltaMovement(velocity.x + push.x * knockback,
                        Math.max(velocity.y, 0.25D + 0.2D * scale),
                        velocity.z + push.z * knockback);
                entity.syncVelocity = true;
            }
        }

        player.fallDistance = 0.0F;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.9F, 1.0F);
        LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (lightning != null) {
            lightning.snapTo(target.getX(), target.getY(), target.getZ());
            lightning.setCause(player);
            level.addFreshEntity(lightning);
        }
        chain.affected.clear();
    }

    /** {@code applyVengeanceDebuffs}: the three effects the original applies for 100 ticks each. */
    private static void applyVengeanceDebuffs(LivingEntity entity, ServerPlayer player) {
        entity.addEffect(new MobEffectInstance(ModMobEffects.DOD_SMOKE_TRAIL, EFFECT_DURATION_TICKS,
                0, false, true), player);
        entity.addEffect(new MobEffectInstance(ModMobEffects.BLEED, EFFECT_DURATION_TICKS,
                0, false, true), player);
        entity.addEffect(new MobEffectInstance(ModMobEffects.DEBAR, EFFECT_DURATION_TICKS,
                0, false, true), player);
    }

    /** {@code VengeanceGrappleHandler.pullPlayerToward}: the velocity is set, not accelerated. */
    private static void pullPlayerToward(ServerPlayer player, LivingEntity target, double speed) {
        Vec3 to = new Vec3(
                target.getX() - player.getX(),
                target.getY() + target.getEyeHeight() * 0.5D - (player.getY() + player.getEyeHeight()),
                target.getZ() - player.getZ());
        double length = to.length();
        if (length < 1.0E-4D) {
            return;
        }
        Vec3 direction = to.scale(1.0D / length);
        double vx = Mth.clamp(direction.x * speed, -VELOCITY_CLAMP_HORIZONTAL, VELOCITY_CLAMP_HORIZONTAL);
        double vy = Mth.clamp(direction.y * speed, -VELOCITY_CLAMP_VERTICAL, VELOCITY_CLAMP_VERTICAL);
        double vz = Mth.clamp(direction.z * speed, -VELOCITY_CLAMP_HORIZONTAL, VELOCITY_CLAMP_HORIZONTAL);
        player.setDeltaMovement(vx, vy, vz);
        player.syncVelocity = true;
        player.fallDistance = 0.0F;
    }

    private static void spawnChainParticles(ServerLevel level, ServerPlayer player, LivingEntity target,
            ChainParticle kind) {
        Vec3 from = player.getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        Vec3 path = to.subtract(from);
        for (int step = 0; step <= 14; step++) {
            Vec3 point = from.add(path.scale(step / 14.0D));
            if (kind == ChainParticle.CRIT) {
                level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            } else {
                level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
                                SPARK_RED, SPARK_GREEN, SPARK_BLUE),
                        point.x, point.y, point.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            }
        }
    }

    /** {@code ParticleVengeance.SPARKS}: purple {@code SPELL_MOB} plus crit and end rod sparks. */
    private static void spawnSparks(ServerLevel level, Vec3 center, int count, float speed) {
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, SPARK_RED, SPARK_GREEN, SPARK_BLUE),
                center.x, center.y, center.z, count, 0.35D, 0.35D, 0.35D, speed * 0.1D);
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, count / 2,
                0.3D, 0.3D, 0.3D, speed * 0.08D);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, count / 3,
                0.25D, 0.25D, 0.25D, speed * 0.02D);
    }

    /** {@code ParticleVengeance.IMPACT_DUST}: block dust plus damage indicators. */
    private static void spawnImpactDust(ServerLevel level, Vec3 center, int count) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                center.x, center.y + 0.2D, center.z, count, 0.9D, 0.35D, 0.9D, 0.12D);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, center.y + 0.3D, center.z,
                count / 2, 0.6D, 0.4D, 0.6D, 0.05D);
    }

    /** {@code ParticleVengeance.HEAVY_BLEED}: dark red {@code SPELL_MOB} plus dripping lava. */
    private static void spawnHeavyBleed(ServerLevel level, LivingEntity entity) {
        Vec3 center = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
        level.sendParticles(
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, BLEED_RED, BLEED_GREEN, BLEED_BLUE),
                center.x, center.y, center.z, 14, 0.35D, 0.35D, 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.DRIPPING_LAVA, center.x, center.y, center.z, 4,
                0.3D, 0.3D, 0.3D, 0.0D);
    }

    /** {@code ParticleVengeance.LIGHTNING_EXPL}: huge explosion, large smoke and crit sparks. */
    private static void spawnLightningExplosion(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, position.x, position.y + 0.5D, position.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, position.x, position.y + 0.3D, position.z,
                24, 0.6D, 0.4D, 0.6D, 0.08D);
        level.sendParticles(ParticleTypes.CRIT, position.x, position.y + 0.6D, position.z,
                20, 0.5D, 0.4D, 0.5D, 0.12D);
    }

    private static LivingEntity resolveTarget(ServerLevel level, UUID targetId) {
        Entity entity = level.getEntity(targetId);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static DamageSource vengeanceDamage(ServerLevel level, ServerPlayer player) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(ModDamageTypes.RICARDO), player);
    }

    /** {@code DamageSource.causeExplosionDamage(player)} for the second impact's blast. */
    private static DamageSource explosionDamage(ServerLevel level, ServerPlayer player) {
        return level.damageSources().explosion(player, player);
    }

    /** Which particle the chain uses in the windup and pull stages. */
    private enum ChainParticle {
        SPELL,
        CRIT
    }

    private enum Phase {
        WINDUP,
        PULL,
        BOUNCE
    }

    private static final class GrappleChain {
        private final UUID playerId;
        private final UUID targetId;
        private final List<UUID> affected = new ArrayList<>();
        private Vec3 lastTargetPosition;
        private Phase phase = Phase.WINDUP;
        private int phaseTicks;

        private GrappleChain(UUID playerId, UUID targetId, Vec3 lastTargetPosition) {
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
