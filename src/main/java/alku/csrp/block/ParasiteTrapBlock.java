package alku.csrp.block;

import alku.csrp.effect.EffectStacking;
import alku.csrp.entity.Parasite;
import alku.csrp.infection.InfectionMechanics;
import alku.csrp.registry.ModDamageTypes;
import alku.csrp.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;

/** Contact hazards used by the original Biomass Block and Maw. */
public final class ParasiteTrapBlock extends Block {
    private static final int CORROSION_DURATION_TICKS = 100;
    private static final int VIRAL_DURATION_TICKS = 200;
    private static final int VIRAL_AMPLIFIER = 1;
    private static final int BIOMASS_COTH_DURATION_TICKS = 1_000;
    private static final int BIOMASS_COTH_AMPLIFIER = 3;
    private final Kind kind;

    public ParasiteTrapBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        affect(level, pos, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (kind == Kind.BIOMASS || isStandingOnTop(pos, entity)) {
            affect(level, pos, entity);
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (kind != Kind.BIOMASS) {
            return;
        }
        if (random.nextFloat() < 0.25F) {
            level.addParticle(ParticleTypes.COMPOSTER,
                    pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                    pos.getY() + 0.05D + random.nextDouble() * 0.9D,
                    pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                    0.0D, 0.01D, 0.0D);
        }
        if (random.nextFloat() < 0.1F) {
            level.addParticle(ParticleTypes.ITEM_SLIME,
                    pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                    pos.getY() + 0.01D,
                    pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                    (random.nextDouble() - 0.5D) * 0.02D,
                    -0.07D - random.nextDouble() * 0.03D,
                    (random.nextDouble() - 0.5D) * 0.02D);
        }
    }

    private void affect(Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof LivingEntity living)
                || living instanceof Parasite || living instanceof Player player && player.getAbilities().instabuild
                || kind == Kind.MAW && !isStandingOnTop(pos, entity)) {
            return;
        }
        long gameTime = level.getGameTime();
        String cooldownKey = kind.cooldownKey;
        if (gameTime < living.getPersistentData().getLong(cooldownKey)) {
            return;
        }
        living.getPersistentData().putLong(cooldownKey, gameTime + kind.cooldownTicks);
        living.hurt(damageSource(serverLevel, kind.damageType), 1.0F);
        if (kind == Kind.BIOMASS) {
            living.removeEffect(ModMobEffects.CORROSION.get());
            InfectionMechanics.applyCothEffect(living, null,
                    BIOMASS_COTH_DURATION_TICKS, BIOMASS_COTH_AMPLIFIER, false, true);
        }
        living.addEffect(new MobEffectInstance(ModMobEffects.CORROSION.get(),
                CORROSION_DURATION_TICKS, 0, false, kind == Kind.BIOMASS));
        EffectStacking.apply(living, ModMobEffects.VIRAL.get(), VIRAL_DURATION_TICKS, VIRAL_AMPLIFIER);
    }

    private static boolean isStandingOnTop(BlockPos pos, Entity entity) {
        return entity.getBoundingBox().minY >= pos.getY() + 0.999D
                && entity.getX() >= pos.getX() && entity.getX() < pos.getX() + 1.0D
                && entity.getZ() >= pos.getZ() && entity.getZ() < pos.getZ() + 1.0D;
    }

    private static DamageSource damageSource(ServerLevel level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type));
    }

    public enum Kind {
        BIOMASS(20, "csrp_biomass_next_apply", ModDamageTypes.BIOMASS),
        MAW(10, "csrp_mouth_next_apply", ModDamageTypes.PARASITE_MOUTH);

        private final int cooldownTicks;
        private final String cooldownKey;
        private final ResourceKey<DamageType> damageType;

        Kind(int cooldownTicks, String cooldownKey, ResourceKey<DamageType> damageType) {
            this.cooldownTicks = cooldownTicks;
            this.cooldownKey = cooldownKey;
            this.damageType = damageType;
        }
    }
}
