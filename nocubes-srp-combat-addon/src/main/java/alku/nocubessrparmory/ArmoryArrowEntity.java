package alku.nocubessrparmory;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Faithful 1.12 arrow port. The original addon used 13 {@code EntityTippedArrow}
 * subclasses with {@code setSilent(true)}, per-kind damage/knockback/critical
 * flags and procedure callbacks. This single class implements those callbacks
 * for every original entity ID while keeping vanilla arrow physics.
 */
public final class ArmoryArrowEntity extends AbstractArrow implements ItemSupplier {
    private ArmoryLauncherItem.Kind kind = ArmoryLauncherItem.Kind.TWISTED_BOMB;
    private ItemStack renderStack;

    public ArmoryArrowEntity(EntityType<? extends ArmoryArrowEntity> type, Level level) {
        super(type, level);
        this.kind = ArmoryEntities.kindOf(type);
        this.pickup = Pickup.DISALLOWED;
    }

    public ArmoryArrowEntity(EntityType<? extends ArmoryArrowEntity> type, Level level, LivingEntity shooter,
            ArmoryLauncherItem.Kind kind, ItemStack launcher) {
        super(type, shooter, level, ItemStack.EMPTY, launcher == null || launcher.isEmpty() ? null : launcher);
        this.kind = kind;
        this.setSilent(true);
        this.setBaseDamage(kind.damage);
        this.setCritArrow(kind.crit);
        this.pickup = Pickup.DISALLOWED;
        if (kind.fireSeconds > 0) this.setRemainingFireTicks(kind.fireSeconds * 20);
    }

    /** Original RenderSnowball icons: invisible for launchers, item icons for bombs/blade. */
    @Override
    public ItemStack getItem() {
        if (renderStack == null) {
            renderStack = switch (kind) {
                case HEAD_BOMB -> new ItemStack(AddonItems.HEAD_BOMB.get());
                case HOST_BOMB, HOST_TENTACLE -> new ItemStack(AddonItems.HOST_BOMB.get());
                case PESTILENT_SHURIKEN -> new ItemStack(AddonItems.PESTILENT_SHURIKEN.get());
                case TWISTED_BOMB -> new ItemStack(AddonItems.TWISTED_BOMB.get());
                case OVERLORD_BLADE -> new ItemStack(AddonItems.OVERLORD_CORE.get());
                default -> new ItemStack(AddonItems.INVISIBLE_PROJECTILE.get());
            };
        }
        return renderStack;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void tick() {
        super.tick();
        if (!isRemoved() && !inGround && level() instanceof ServerLevel server) {
            spawnTrail(server);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;
        if (result.getEntity() instanceof LivingEntity target) {
            double speed = getDeltaMovement().length();
            float damage = (float) Math.ceil(speed * kind.damage);
            if (kind.crit) damage += random.nextInt((int) (damage / 2.0F) + 2);
            DamageSource source = damageSources().arrow(this, getOwner());
            boolean hurt = target.hurt(source, damage);
            if (hurt) {
                if (isOnFire()) target.igniteForTicks(100);
                applyKnockback(target);
            }
            applyHitEffects(target);
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) {
            applyBlockEffects(result);
            discard();
        }
    }

    private void applyKnockback(LivingEntity target) {
        if (kind.knockback <= 0) return;
        Vec3 motion = getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal <= 0.0D) return;
        double factor = kind.knockback * 0.6D / horizontal;
        target.push(motion.x * factor, 0.1D, motion.z * factor);
    }

    private void applyHitEffects(LivingEntity target) {
        switch (kind) {
            case EVOLUTION_BOW -> {
                ignite(target, 12);
                playHitSound();
            }
            case GORE_BOW -> {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 1, false, true));
                playHitSound();
            }
            case TWISTED_BOW -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 1, false, true));
                playHitSound();
            }
            case PESTILENT_MIASM -> {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true));
                playHitSound();
            }
            case PESTILENT_SHURIKEN ->
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true));
            case FLAMETHROWER -> ignite(target, 20);
            case INCINERATOR -> ignite(target, 30);
            case PLASMA_TORCH -> ignite(target, 10);
            case HEAD_BOMB, TWISTED_BOMB -> summonPayload();
            case HOST_BOMB, HOST_TENTACLE -> explode();
            case OVERLORD_BLADE -> {
                summon("csrp:anc_pod");
                if (level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(),
                            20, 1.0D, 1.0D, 1.0D, 0.2D);
                }
            }
            default -> { }
        }
    }

    private void applyBlockEffects(BlockHitResult result) {
        double cx = result.getBlockPos().getX() + 0.5D;
        double cy = result.getBlockPos().getY() + 0.5D;
        double cz = result.getBlockPos().getZ() + 0.5D;
        switch (kind) {
            case FLAMETHROWER -> {
                particles(ParticleTypes.FLAME, cx, cy, cz, 20, 1.0D, 1.0D, 1.0D, 0.0D);
                particles(ParticleTypes.LAVA, cx, cy, cz, 5, 1.0D, 1.0D, 1.0D, 0.0D);
            }
            case INCINERATOR -> {
                particles(ParticleTypes.DRAGON_BREATH, cx, cy, cz, 20, 1.0D, 1.0D, 1.0D, 0.0D);
                particles(ParticleTypes.SMOKE, cx, cy, cz, 5, 1.0D, 1.0D, 1.0D, 0.0D);
            }
            case PLASMA_TORCH -> {
                particles(ParticleTypes.FLAME, cx, cy, cz, 12, 1.0D, 1.0D, 1.0D, 0.0D);
                particles(ParticleTypes.SMOKE, cx, cy, cz, 5, 1.0D, 1.0D, 1.0D, 0.0D);
            }
            case PESTILENT_SHURIKEN -> playNeutralSound(SoundEvents.STONE_HIT);
            case EVOLUTION_BOW, GORE_BOW, TWISTED_BOW, PESTILENT_MIASM -> playHitSound();
            case HEAD_BOMB, TWISTED_BOMB -> summonPayload();
            case HOST_BOMB, HOST_TENTACLE -> explode();
            case OVERLORD_BLADE -> {
                summon("csrp:anc_pod");
                particles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 20, 1.0D, 1.0D, 1.0D, 0.2D);
            }
            default -> { }
        }
    }

    private void spawnTrail(ServerLevel server) {
        switch (kind) {
            case FLAMETHROWER -> {
                server.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(),
                        5, 0.1D, 0.1D, 0.1D, 0.0D);
                server.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            case INCINERATOR -> {
                server.sendParticles(ParticleTypes.DRAGON_BREATH, getX(), getY(), getZ(),
                        5, 0.1D, 0.1D, 0.1D, 0.0D);
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            case PLASMA_TORCH -> {
                server.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(),
                        5, 0.1D, 0.1D, 0.1D, 0.0D);
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            case OVERLORD_BLADE -> server.sendParticles(ParticleTypes.LARGE_SMOKE,
                    getX(), getY(), getZ(), 3, 0.01D, 0.01D, 0.01D, 0.0D);
            case HEAD_BOMB -> server.sendParticles(ParticleTypes.SMOKE,
                    getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            case TWISTED_BOMB -> {
                server.sendParticles(ParticleTypes.SMOKE, getX() + 0.2D, getY(), getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                server.sendParticles(ParticleTypes.SMOKE, getX() + 0.2D, getY(), getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            case PESTILENT_MIASM, PESTILENT_SHURIKEN -> {
                if (random.nextFloat() < 0.6F) {
                    server.sendParticles(ParticleTypes.EFFECT, getX(), getY() + 0.5D, getZ(),
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            default -> { }
        }
    }

    /** Original HeadBombHit / TwistedBombHit: three summons plus nested 70/60/50% rolls. */
    private void summonPayload() {
        String primary = kind == ArmoryLauncherItem.Kind.HEAD_BOMB ? "sim_humanhead" : "rupter";
        String secondary = kind == ArmoryLauncherItem.Kind.HEAD_BOMB ? "sim_villagerhead" : "rupter";
        summon("csrp:" + primary);
        summon("csrp:" + primary);
        summon("csrp:" + secondary);
        if (random.nextFloat() < 0.7F) {
            summon("csrp:" + secondary);
            if (random.nextFloat() < 0.6F) {
                summon("csrp:" + primary);
                if (random.nextFloat() < 0.5F) summon("csrp:" + primary);
            }
        }
        particles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0.1D, 0.1D, 0.1D, 0.1D);
    }

    private void summon(String id) {
        if (!(level() instanceof ServerLevel server)) return;
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return;
        BuiltInRegistries.ENTITY_TYPE.getOptional(key).ifPresent(type -> {
            Entity entity = type.create(server);
            if (entity != null) {
                entity.moveTo(getX(), getY(), getZ(), random.nextFloat() * 360.0F, 0.0F);
                server.addFreshEntity(entity);
            }
        });
    }

    private void explode() {
        level().explode(null, getX(), getY(), getZ(), 4.0F, Level.ExplosionInteraction.MOB);
    }

    private void ignite(LivingEntity target, int seconds) {
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), seconds * 20));
    }

    private void playHitSound() {
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ARROW_HIT, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private void playNeutralSound(net.minecraft.sounds.SoundEvent sound) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private void particles(net.minecraft.core.particles.ParticleOptions type, double x, double y, double z,
            int count, double dx, double dy, double dz, double speed) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(type, x, y, z, count, dx, dy, dz, speed);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("ArmoryKind", kind.name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ArmoryKind")) {
            try {
                kind = ArmoryLauncherItem.Kind.valueOf(tag.getString("ArmoryKind"));
                renderStack = null;
            } catch (IllegalArgumentException ignored) {
                // Keep the entity type-derived kind.
            }
        }
    }
}
