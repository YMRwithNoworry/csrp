package alku.csrp.entity;

import alku.csrp.Csrp;
import alku.csrp.registry.ModEntities;
import alku.csrp.infection.BlockInfestation;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModItems;
import alku.csrp.world.SrpCoreSystems;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;

/** Legacy Nexus families: stationary stage growth, reinforcement, and battlefield support. */
public final class NexusParasiteEntity extends PrimitiveParasiteEntity {
    private final RawAnimation IDLE = ParasiteAnimations.loop(this, "idle");
    private final RawAnimation WALK = ParasiteAnimations.loop(this, "walk");
    private final RawAnimation FLY = ParasiteAnimations.loop(this, "fly");
    private final RawAnimation ATTACK = ParasiteAnimations.play(this, "attack");
    private static final int STAGE_ONE_MIN_GROWTH = 4_800;
    private static final int STAGE_ONE_GROWTH_VARIANCE = 1_201;
    private static final int TEMPORARY_BECKON_LIFETIME = 300;

    private final Kind kind;
    private final List<String> storedParasiteIds = new ArrayList<>();
    private int growthTicks;
    private int growthDelayTicks;
    private int summonCooldown;
    private int bombCooldown;
    private int supportCooldown;
    private int blockBreakCooldown;
    private int forcedEvolutionCooldown;
    private int attackFlashTicks;
    private int colonyPlacementProgress;
    private int temporaryLifetimeTicks = -1;
    private boolean canGrow = true;

    public NexusParasiteEntity(EntityType<? extends NexusParasiteEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = kind.experience;
        growthDelayTicks = kind.stage == 0 || kind.stage == 4 ? -1
                : STAGE_ONE_MIN_GROWTH + random.nextInt(STAGE_ONE_GROWTH_VARIANCE);
    }

    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, kind.maxHealth)
                .add(Attributes.ARMOR, kind.armor)
                .add(Attributes.ATTACK_DAMAGE, kind.attackDamage)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, kind.stage >= 4 ? 64.0D : 16.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        if (activeKind().isRooterBall()) {
            return;
        }
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10,
                true, false, this::isValidParasiteTarget));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        getNavigation().stop();
        decrementCooldowns();

        Kind activeKind = activeKind();
        if (activeKind.isRooterBall()) {
            return;
        }
        if (activeKind.family == Family.BECKON && temporaryLifetimeTicks > 0
                && --temporaryLifetimeTicks <= 0) {
            discard();
            return;
        }
        if (canGrow && growthDelayTicks > 0 && level() instanceof ServerLevel serverLevel) {
            int phase = SrpWorldData.get(serverLevel).evolutionPhase();
            int minimumPhase = activeKind.stage + 2;
            if (phase >= minimumPhase && (phase > minimumPhase || tickCount % 4 == 0)
                    && ++growthTicks >= growthDelayTicks && evolve()) {
                return;
            }
        }

        if (activeKind.family == Family.ROOTER && supportCooldown <= 0) {
            applyRooterSupport(activeKind.stage);
            supportCooldown = 200;
        }
        if (activeKind.family == Family.DISPATCHER && tickCount % 40 == 0) {
            storeNearbyParasite();
        }
        if (activeKind.family == Family.DISPATCHER && activeKind.stage == 4) {
            tryPlaceFirstColony();
        }
        if (summonCooldown <= 0 && performFamilyAbility(activeKind)) {
            summonCooldown = activeKind.summonCooldown;
        }
        if (activeKind.family == Family.BECKON && activeKind.stage == 4
                && forcedEvolutionCooldown <= 0 && forceEvolveNearbyParasite()) {
            forcedEvolutionCooldown = 100;
        }
        if (activeKind.family == Family.BECKON && activeKind.stage == 4 && level().isThundering()
                && tickCount % 20 == 0) {
            createStormVortex();
        }
        if (activeKind.family == Family.BECKON && tickCount % Math.max(20, 100 - activeKind.stage * 15) == 0
                && level() instanceof ServerLevel serverLevel) {
            BlockInfestation.infestAround(serverLevel, blockPosition().below(),
                    Math.max(0, Math.min(3, activeKind.stage - 1)));
        }
        if (tickCount % 10 == 0) {
            breakBlocksTowardsTarget(activeKind);
        }
    }

    private void tryPlaceFirstColony() {
        if (tickCount < 1_200 || random.nextInt(10) != 0 || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!SrpWorldData.get(serverLevel).colonies().isEmpty()) {
            colonyPlacementProgress = -1_000;
            return;
        }
        if (++colonyPlacementProgress < 200) {
            return;
        }
        colonyPlacementProgress = -100;
        for (int attempt = 0; attempt < 3; attempt++) {
            int offsetX = (5 + random.nextInt(7)) * (random.nextBoolean() ? 1 : -1);
            int offsetZ = (5 + random.nextInt(7)) * (random.nextBoolean() ? 1 : -1);
            if (SrpCoreSystems.placeColony(serverLevel, blockPosition().offset(offsetX, 0, offsetZ))) {
                colonyPlacementProgress = -1_000;
                return;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Kind activeKind = activeKind();
        boolean damageRedirected = false;
        if (activeKind.family == Family.ROOTER && !level().isClientSide) {
            List<NexusParasiteEntity> cysts = rootmassCystsInRange(activeKind);
            if (!cysts.isEmpty()) {
                // Legacy Rooters split subsequent hits between nearby Rootmass Cysts.
                float sharedDamage = amount / cysts.size();
                for (NexusParasiteEntity cyst : cysts) {
                    cyst.hurt(source, sharedDamage);
                }
                damageRedirected = true;
            } else {
                spawnRootmassCysts(activeKind.rootmassCystSpawnLimit());
            }
        }

        float incomingDamage = damageRedirected ? 0.0F : amount;
        boolean hurt = super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? incomingDamage * 4.0F : incomingDamage);
        if (!hurt || level().isClientSide || activeKind.isRooterBall()) {
            return hurt;
        }
        attackFlashTicks = 12;
        if (bombCooldown <= 0) {
            spawnBombVolley(activeKind.bombCount, (float) activeKind.attackDamage);
            bombCooldown = 100;
        }
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected int maxDamageAdaptationHits() {
        return switch (activeKind().stage) {
            case 1 -> 10;
            case 2 -> 8;
            case 3 -> 6;
            case 4 -> 4;
            default -> 0;
        };
    }

    @Override
    protected float damageAdaptationPerHit() {
        return switch (activeKind().stage) {
            case 1 -> 0.07F;
            case 2 -> 0.125F;
            case 3 -> 0.17F;
            case 4 -> 0.25F;
            default -> 0.0F;
        };
    }

    @Override
    protected int maxLearnableDamageSources() {
        return switch (activeKind().stage) {
            case 1 -> 5;
            case 2 -> 10;
            case 3 -> 15;
            case 4 -> 23;
            default -> 0;
        };
    }

    @Override
    protected float damageAdaptationLearningChance() {
        return switch (activeKind().stage) {
            case 1 -> 0.70F;
            case 2 -> 0.80F;
            case 3, 4 -> 0.90F;
            default -> 0.0F;
        };
    }

    @Override
    protected float fireAdaptationSuppressionChance() {
        return switch (activeKind().stage) {
            case 1 -> 0.70F;
            case 2 -> 0.50F;
            case 3 -> 0.30F;
            case 4 -> 0.10F;
            default -> 0.0F;
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement_controller", 4, this::movementAnimation));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("nexus_growth", growthTicks);
        tag.putInt("nexus_growth_delay", growthDelayTicks);
        tag.putInt("nexus_summon_cooldown", summonCooldown);
        tag.putInt("nexus_bomb_cooldown", bombCooldown);
        tag.putInt("nexus_support_cooldown", supportCooldown);
        tag.putInt("nexus_block_break_cooldown", blockBreakCooldown);
        tag.putInt("nexus_forced_evolution_cooldown", forcedEvolutionCooldown);
        tag.putInt("nexus_attack_flash", attackFlashTicks);
        tag.putInt("nexus_temporary_lifetime", temporaryLifetimeTicks);
        tag.putBoolean("nexus_can_grow", canGrow);
        ListTag storedParasites = new ListTag();
        storedParasiteIds.forEach(id -> storedParasites.add(StringTag.valueOf(id)));
        tag.put("nexus_dispatcher_stored", storedParasites);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        growthTicks = tag.getInt("nexus_growth");
        growthDelayTicks = tag.contains("nexus_growth_delay") ? tag.getInt("nexus_growth_delay")
                : defaultGrowthDelay();
        summonCooldown = tag.getInt("nexus_summon_cooldown");
        bombCooldown = tag.getInt("nexus_bomb_cooldown");
        supportCooldown = tag.getInt("nexus_support_cooldown");
        blockBreakCooldown = tag.getInt("nexus_block_break_cooldown");
        forcedEvolutionCooldown = tag.getInt("nexus_forced_evolution_cooldown");
        attackFlashTicks = tag.getInt("nexus_attack_flash");
        temporaryLifetimeTicks = tag.contains("nexus_temporary_lifetime")
                ? tag.getInt("nexus_temporary_lifetime") : -1;
        canGrow = !tag.contains("nexus_can_grow") || tag.getBoolean("nexus_can_grow");
        storedParasiteIds.clear();
        ListTag storedParasites = tag.getList("nexus_dispatcher_stored", Tag.TAG_STRING);
        for (int index = 0; index < storedParasites.size(); index++) {
            String id = storedParasites.getString(index);
            if (ResourceLocation.tryParse(id) != null) {
                storedParasiteIds.add(id);
            }
        }
    }

    public Kind getKind() {
        return activeKind();
    }

    private PlayState movementAnimation(AnimationState<NexusParasiteEntity> state) {
        if (attackFlashTicks > 0) {
            return state.setAndContinue(ATTACK);
        }
        if (!state.isMoving()) {
            return state.setAndContinue(IDLE);
        }
        return state.setAndContinue(!onGround() || Math.abs(getDeltaMovement().y) > 0.08D ? FLY : WALK);
    }

    private void decrementCooldowns() {
        if (summonCooldown > 0) summonCooldown--;
        if (bombCooldown > 0) bombCooldown--;
        if (supportCooldown > 0) supportCooldown--;
        if (blockBreakCooldown > 0) blockBreakCooldown--;
        if (forcedEvolutionCooldown > 0) forcedEvolutionCooldown--;
        if (attackFlashTicks > 0) attackFlashTicks--;
    }

    private boolean performFamilyAbility(Kind activeKind) {
        LivingEntity target = getTarget();
        return switch (activeKind.family) {
            case BECKON -> target != null && summonBeckonParasites(activeKind, target);
            case DISPATCHER -> target != null && summonDispatcherDefenses(activeKind, target);
            case ROOTER -> {
                if (activeKind.stage == 4 && target != null) {
                    spawnPodVolley(target);
                }
                yield true;
            }
            case ROOTERBALL -> false;
        };
    }

    private void storeNearbyParasite() {
        double range = getAttributeValue(Attributes.FOLLOW_RANGE);
        List<Mob> candidates = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(range),
                this::isStorableDispatcherParasite);
        if (candidates.isEmpty()) {
            return;
        }
        Mob candidate = candidates.get(random.nextInt(candidates.size()));
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType());
        if (id == null || !Csrp.MODID.equals(id.getNamespace())) {
            return;
        }
        storedParasiteIds.add(id.toString());
        candidate.discard();
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, candidate.getX(),
                    candidate.getY() + candidate.getBbHeight() * 0.5D, candidate.getZ(),
                    12, 0.25D, 0.35D, 0.25D, 0.02D);
        }
    }

    private boolean isStorableDispatcherParasite(Mob candidate) {
        return candidate != this
                && candidate instanceof Parasite
                && candidate.getTarget() == null
                && candidate.onGround()
                && candidate.tickCount > 100
                && !candidate.isPassenger()
                && !candidate.isVehicle()
                && !(candidate instanceof NexusParasiteEntity)
                && !(candidate instanceof PreeminentParasiteEntity)
                && !(candidate instanceof AncientParasiteEntity)
                && !(candidate instanceof AbominationEntity)
                && !(candidate instanceof ArchitectEntity)
                && !(candidate instanceof DeterrentParasiteEntity)
                && !(candidate instanceof MarauderTendrilEntity);
    }

    private boolean summonBeckonParasites(Kind activeKind, LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel) || nearbyParasiteCount(18.0D) >= activeKind.activeCap) {
            return false;
        }
        if (activeKind.stage == 4) {
            int points = 12;
            int spawnedCount = 0;
            int availableSlots = Math.max(0, activeKind.activeCap - nearbyParasiteCount(18.0D));
            while (points >= 3 && spawnedCount < 4 && spawnedCount < availableSlots) {
                float roll = random.nextFloat();
                int stage = roll < 0.30F ? 1 : roll < 0.70F ? 2 : 3;
                int cost = stage == 3 ? 4 : 3;
                if (cost > points) {
                    stage = random.nextBoolean() ? 1 : 2;
                    cost = 3;
                }
                NexusParasiteEntity spawned = createNexus(serverLevel, Family.BECKON, stage);
                if (!spawnNexus(spawned, target, 5.0D + spawnedCount)) {
                    break;
                }
                points -= cost;
                spawnedCount++;
            }
            return spawnedCount > 0;
        }
        EntityType<? extends Mob> type = switch (activeKind.stage) {
            case 1 -> ModEntities.RUPTER.get();
            case 2 -> random.nextBoolean() ? ModEntities.RUPTER.get() : ModEntities.BUGLIN.get();
            default -> switch (random.nextInt(4)) {
                case 0 -> ModEntities.PRI_SUMMONER.get();
                case 1 -> ModEntities.PRI_LONGARMS.get();
                case 2 -> ModEntities.PRI_REEKER.get();
                default -> ModEntities.RUPTER.get();
            };
        };
        return spawnMob(type, target, 4.0D);
    }

    private boolean forceEvolveNearbyParasite() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        List<LivingEntity> candidates = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(16.0D), this::canForceEvolve);
        if (candidates.isEmpty()) {
            return false;
        }

        LivingEntity source = candidates.get(random.nextInt(candidates.size()));
        EntityType<?> targetType = forcedEvolutionType(source);
        Entity created = targetType == null ? null : targetType.create(serverLevel);
        if (!(created instanceof Mob replacement)) {
            return false;
        }

        replacement.moveTo(source.getX(), source.getY(), source.getZ(), source.getYRot(), source.getXRot());
        replacement.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(replacement.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        replacement.setCustomName(source.getCustomName());
        replacement.setCustomNameVisible(source.isCustomNameVisible());
        if (source instanceof Mob sourceMob) {
            replacement.setTarget(sourceMob.getTarget());
        }
        replacement.setHealth(Math.max(1.0F,
                replacement.getMaxHealth() * source.getHealth() / Math.max(1.0F, source.getMaxHealth())));
        serverLevel.addFreshEntity(replacement);

        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning != null) {
            lightning.moveTo(source.position());
            lightning.setVisualOnly(true);
            serverLevel.addFreshEntity(lightning);
        }
        source.discard();
        return true;
    }

    private boolean canForceEvolve(LivingEntity candidate) {
        if (candidate == this || !candidate.isAlive()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType());
        if (id.getNamespace().equals(Csrp.MODID) && id.getPath().startsWith("pri_")) {
            ResourceLocation adaptedId = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                    "ada_" + id.getPath().substring("pri_".length()));
            return BuiltInRegistries.ENTITY_TYPE.containsKey(adaptedId);
        }
        return isAssimilatedTier(candidate);
    }

    private EntityType<?> forcedEvolutionType(LivingEntity candidate) {
        if (!canForceEvolve(candidate)) {
            return null;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType());
        String path = id.getPath();
        if (id.getNamespace().equals(Csrp.MODID) && path.startsWith("pri_")) {
            ResourceLocation adaptedId = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                    "ada_" + path.substring("pri_".length()));
            return BuiltInRegistries.ENTITY_TYPE.containsKey(adaptedId)
                    ? BuiltInRegistries.ENTITY_TYPE.get(adaptedId) : null;
        }
        if (!isAssimilatedTier(candidate)) {
            return null;
        }
        return switch (random.nextInt(12)) {
            case 0 -> ModEntities.PRI_LONGARMS.get();
            case 1 -> ModEntities.PRI_SUMMONER.get();
            case 2 -> ModEntities.PRI_VERMIN.get();
            case 3 -> ModEntities.PRI_VISCERA.get();
            case 4 -> ModEntities.PRI_ARACHNIDA.get();
            case 5 -> ModEntities.PRI_BOLSTER.get();
            case 6 -> ModEntities.PRI_BURROWER.get();
            case 7 -> ModEntities.PRI_DEVOURER.get();
            case 8 -> ModEntities.PRI_MANDUCATER.get();
            case 9 -> ModEntities.PRI_REEKER.get();
            case 10 -> ModEntities.PRI_TOZOON.get();
            default -> ModEntities.PRI_YELLOWEYE.get();
        };
    }

    private static boolean isAssimilatedTier(LivingEntity candidate) {
        return candidate instanceof AssimilatedParasiteEntity
                || candidate instanceof AssimilatedVariantEntity
                || candidate instanceof AssimilatedEndermanEntity
                || candidate instanceof AssimilatedHeadEntity
                || candidate instanceof AssimilatedDragonEntity
                || candidate instanceof AssimilatedDragonHeadEntity
                || candidate instanceof SimAdventurerEntity
                || candidate instanceof SimAdventurerHeadEntity
                || candidate instanceof FeralParasiteEntity;
    }

    private boolean summonDispatcherDefenses(Kind activeKind, LivingEntity target) {
        if (nearbyParasiteCount(20.0D) >= activeKind.activeCap) {
            return false;
        }
        if (deployStoredParasite(target)) {
            return true;
        }
        EntityType<? extends Mob> type = random.nextFloat() < (activeKind.stage == 1 ? 0.75F : 0.50F)
                ? ModEntities.SEIZER.get() : ModEntities.SENTRY.get();
        boolean spawned = spawnMob(type, target, 5.0D);
        if (spawned && activeKind.stage == 4 && random.nextFloat() < 0.30F) {
            spawnPodVolley(target);
        }
        return spawned;
    }

    private boolean deployStoredParasite(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        while (!storedParasiteIds.isEmpty()) {
            int index = random.nextInt(storedParasiteIds.size());
            ResourceLocation id = ResourceLocation.tryParse(storedParasiteIds.get(index));
            if (id == null || BuiltInRegistries.ENTITY_TYPE.getOptional(id).isEmpty()) {
                storedParasiteIds.remove(index);
                continue;
            }
            DeterrentParasiteEntity tentacle = ModEntities.DISPATCHERTEN.get().create(serverLevel);
            if (tentacle == null) {
                return false;
            }
            for (int attempt = 0; attempt < 6; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = 3.0D + random.nextDouble() * 2.0D;
                BlockPos requested = BlockPos.containing(target.getX() + Math.cos(angle) * distance,
                        target.getY(), target.getZ() + Math.sin(angle) * distance);
                BlockPos spawnPos = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, requested);
                tentacle.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                        getYRot(), 0.0F);
                if (!serverLevel.noCollision(tentacle)) {
                    continue;
                }
                tentacle.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.MOB_SUMMONED, null);
                tentacle.setDispatchEntity(id);
                tentacle.setTarget(target);
                serverLevel.addFreshEntity(tentacle);
                storedParasiteIds.remove(index);
                return true;
            }
            return false;
        }
        return false;
    }

    private void spawnBombVolley(int count, float damage) {
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2.0D * index / count + random.nextDouble() * 0.45D;
            Vec3 start = position().add(0.0D, getBbHeight() * 0.65D, 0.0D);
            Vec3 target = start.add(Math.cos(angle) * (4.0D + random.nextDouble() * 3.0D),
                    random.nextDouble() * 1.5D, Math.sin(angle) * (4.0D + random.nextDouble() * 3.0D));
            fireProjectile(ParasiteProjectileEntity.Mode.BOMB, start, target, 0.72D, damage, 1.25D, 90);
        }
    }

    private void spawnPodVolley(LivingEntity target) {
        for (int index = 0; index < 5; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            Vec3 landing = target.position().add(Math.cos(angle) * (2.0D + random.nextDouble() * 3.0D),
                    0.0D, Math.sin(angle) * (2.0D + random.nextDouble() * 3.0D));
            Vec3 start = landing.add(0.0D, 8.0D + random.nextDouble() * 3.0D, 0.0D);
            fireProjectile(ParasiteProjectileEntity.Mode.BOMB, start, landing, 0.85D, 50.0F, 2.5D, 100);
        }
    }

    private void fireProjectile(ParasiteProjectileEntity.Mode mode, Vec3 start, Vec3 target,
                                double speed, float damage, double radius, int lifetime) {
        ParasiteProjectileEntity projectile = ModEntities.PARASITE_PROJECTILE.get().create(level());
        if (projectile == null) {
            return;
        }
        projectile.configure(this, mode, start, target, speed, damage, radius, lifetime);
        level().addFreshEntity(projectile);
    }

    private List<NexusParasiteEntity> rootmassCystsInRange(Kind rooter) {
        int range = rooter.rootmassCystRange();
        return level().getEntitiesOfClass(NexusParasiteEntity.class,
                getBoundingBox().inflate(range + 1.0D, range, range + 1.0D),
                cyst -> cyst.isAlive() && cyst.activeKind().isRooterBall());
    }

    private void spawnRootmassCysts(int maximum) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int count = 1 + random.nextInt(Math.max(1, maximum));
        for (int index = 0; index < count; index++) {
            NexusParasiteEntity rooterBall = ModEntities.ROOTERBALL.get().create(serverLevel);
            if (rooterBall == null) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 2.0D + random.nextDouble() * 3.0D;
            rooterBall.moveTo(getX() + Math.cos(angle) * distance, getY(),
                    getZ() + Math.sin(angle) * distance, getYRot(), 0.0F);
            rooterBall.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(rooterBall.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null);
            serverLevel.addFreshEntity(rooterBall);
        }
    }

    private void applyRooterSupport(int stage) {
        for (LivingEntity ally : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(16.0D + stage * 4.0D),
                entity -> entity != this && entity instanceof Parasite)) {
            if (ally instanceof NexusParasiteEntity nexus
                    && (nexus.activeKind().family == Family.ROOTER || nexus.activeKind().isRooterBall())) {
                continue;
            }
            ally.addEffect(new MobEffectInstance(ModMobEffects.PIVOT, 300, Math.max(0, stage - 1), false, false), this);
            ally.addEffect(new MobEffectInstance(ModMobEffects.PARATE, 300, Math.max(0, stage - 1), false, false), this);
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.COMPOSTER, getX(), getY() + getBbHeight() * 0.55D, getZ(),
                    16, 1.5D, 1.0D, 1.5D, 0.02D);
        }
    }

    private void createStormVortex() {
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(10.0D), this::isValidParasiteTarget)) {
            if (target.getItemBySlot(EquipmentSlot.FEET).is(ModItems.VENKROL_BOOTS)) {
                continue;
            }
            Vec3 pull = position().subtract(target.position());
            if (pull.lengthSqr() < 0.001D) {
                continue;
            }
            pull = pull.normalize().scale(0.32D);
            target.push(pull.x, 0.18D, pull.z);
        }
    }

    private void breakBlocksTowardsTarget(Kind activeKind) {
        LivingEntity target = getTarget();
        if (target == null || blockBreakCooldown > 0
                || !level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 direction = target.position().subtract(position());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 0.001D) {
            return;
        }
        horizontal = horizontal.normalize();
        BlockPos origin = BlockPos.containing(getX() + horizontal.x * activeKind.blockRange,
                getY() + getBbHeight() * 0.5D, getZ() + horizontal.z * activeKind.blockRange);
        for (BlockPos candidate : new BlockPos[] {origin, origin.above(), origin.below()}) {
            BlockState state = level().getBlockState(candidate);
            float hardness = state.getDestroySpeed(level(), candidate);
            if (state.isAir() || state.hasBlockEntity() || hardness < 0.0F || hardness > activeKind.maxBlockHardness) {
                continue;
            }
            if (level().destroyBlock(candidate, true, this)) {
                blockBreakCooldown = activeKind.stage >= 4 ? 20 : 60;
            }
            return;
        }
    }

    private boolean evolve() {
        Kind activeKind = activeKind();
        if (!(level() instanceof ServerLevel serverLevel) || activeKind.stage <= 0 || activeKind.stage >= 4) {
            return false;
        }
        NexusParasiteEntity next = createNexus(serverLevel, activeKind.family, activeKind.stage + 1);
        if (next == null) {
            return false;
        }
        next.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        next.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(next.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        next.setCustomName(getCustomName());
        next.setCustomNameVisible(isCustomNameVisible());
        if (isPersistenceRequired()) {
            next.setPersistenceRequired();
        }
        serverLevel.addFreshEntity(next);
        discard();
        return true;
    }

    private boolean spawnNexus(NexusParasiteEntity spawned, LivingEntity target, double distance) {
        if (spawned == null || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (activeKind().family == Family.BECKON && spawned.activeKind().family == Family.BECKON) {
            spawned.makeTemporaryBeckon();
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        spawned.moveTo(target.getX() + Math.cos(angle) * distance, target.getY(),
                target.getZ() + Math.sin(angle) * distance, getYRot(), 0.0F);
        spawned.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawned.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        spawned.setTarget(target);
        serverLevel.addFreshEntity(spawned);
        return true;
    }

    private void makeTemporaryBeckon() {
        temporaryLifetimeTicks = TEMPORARY_BECKON_LIFETIME;
        canGrow = false;
    }

    private boolean spawnMob(EntityType<? extends Mob> type, LivingEntity target, double distance) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Mob spawned = type.create(serverLevel);
        if (spawned == null) {
            return false;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        spawned.moveTo(target.getX() + Math.cos(angle) * distance, target.getY(),
                target.getZ() + Math.sin(angle) * distance, getYRot(), 0.0F);
        spawned.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawned.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null);
        spawned.setTarget(target);
        serverLevel.addFreshEntity(spawned);
        return true;
    }

    private int nearbyParasiteCount(double radius) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                entity -> entity != this && entity instanceof Parasite).size();
    }

    private int defaultGrowthDelay() {
        return activeKind().stage == 0 || activeKind().stage == 4 ? -1
                : STAGE_ONE_MIN_GROWTH + random.nextInt(STAGE_ONE_GROWTH_VARIANCE);
    }

    private NexusParasiteEntity createNexus(ServerLevel level, Family family, int stage) {
        return switch (family) {
            case BECKON -> switch (stage) {
                case 1 -> ModEntities.BECKON_SI.get().create(level);
                case 2 -> ModEntities.BECKON_SII.get().create(level);
                case 3 -> ModEntities.BECKON_SIII.get().create(level);
                case 4 -> ModEntities.BECKON_SIV.get().create(level);
                default -> null;
            };
            case DISPATCHER -> switch (stage) {
                case 1 -> ModEntities.DISPATCHER_SI.get().create(level);
                case 2 -> ModEntities.DISPATCHER_SII.get().create(level);
                case 3 -> ModEntities.DISPATCHER_SIII.get().create(level);
                case 4 -> ModEntities.DISPATCHER_SIV.get().create(level);
                default -> null;
            };
            case ROOTER -> switch (stage) {
                case 1 -> ModEntities.ROOTER_SI.get().create(level);
                case 2 -> ModEntities.ROOTER_SII.get().create(level);
                case 3 -> ModEntities.ROOTER_SIII.get().create(level);
                case 4 -> ModEntities.ROOTER_SIV.get().create(level);
                default -> null;
            };
            case ROOTERBALL -> null;
        };
    }

    private Kind activeKind() {
        if (kind != null) {
            return kind;
        }
        EntityType<?> type = getType();
        if (type == ModEntities.BECKON_SII.get()) return Kind.BECKON_SII;
        if (type == ModEntities.BECKON_SIII.get()) return Kind.BECKON_SIII;
        if (type == ModEntities.BECKON_SIV.get()) return Kind.BECKON_SIV;
        if (type == ModEntities.DISPATCHER_SI.get()) return Kind.DISPATCHER_SI;
        if (type == ModEntities.DISPATCHER_SII.get()) return Kind.DISPATCHER_SII;
        if (type == ModEntities.DISPATCHER_SIII.get()) return Kind.DISPATCHER_SIII;
        if (type == ModEntities.DISPATCHER_SIV.get()) return Kind.DISPATCHER_SIV;
        if (type == ModEntities.ROOTER_SI.get()) return Kind.ROOTER_SI;
        if (type == ModEntities.ROOTER_SII.get()) return Kind.ROOTER_SII;
        if (type == ModEntities.ROOTER_SIII.get()) return Kind.ROOTER_SIII;
        if (type == ModEntities.ROOTER_SIV.get()) return Kind.ROOTER_SIV;
        if (type == ModEntities.ROOTERBALL.get()) return Kind.ROOTERBALL;
        return Kind.BECKON_SI;
    }

    private enum Family {
        BECKON,
        DISPATCHER,
        ROOTER,
        ROOTERBALL
    }

    public enum Kind {
        BECKON_SI(Family.BECKON, 1, 25.0D, 4.0D, 2.5D, 4, 4, 200, 1.0F, 3.0D, 16),
        BECKON_SII(Family.BECKON, 2, 60.0D, 8.0D, 6.0D, 5, 6, 180, 1.0F, 3.0D, 32),
        BECKON_SIII(Family.BECKON, 3, 110.0D, 16.0D, 13.0D, 6, 8, 160, 1.0F, 3.0D, 64),
        BECKON_SIV(Family.BECKON, 4, 220.0D, 25.0D, 20.0D, 8, 12, 160, 5.0F, 18.0D, 220),
        DISPATCHER_SI(Family.DISPATCHER, 1, 33.0D, 7.0D, 3.0D, 4, 3, 240, 1.0F, 3.0D, 16),
        DISPATCHER_SII(Family.DISPATCHER, 2, 70.0D, 14.0D, 7.0D, 5, 5, 220, 2.0F, 6.0D, 32),
        DISPATCHER_SIII(Family.DISPATCHER, 3, 130.0D, 21.0D, 14.0D, 6, 7, 200, 3.0F, 9.0D, 64),
        DISPATCHER_SIV(Family.DISPATCHER, 4, 250.0D, 28.0D, 22.0D, 8, 9, 180, 5.0F, 18.0D, 220),
        ROOTER_SI(Family.ROOTER, 1, 25.0D, 4.0D, 2.5D, 4, 3, 240, 0.0F, 0.0D, 16),
        ROOTER_SII(Family.ROOTER, 2, 60.0D, 8.0D, 6.0D, 5, 5, 220, 2.0F, 6.0D, 32),
        ROOTER_SIII(Family.ROOTER, 3, 110.0D, 16.0D, 13.0D, 6, 7, 200, 3.0F, 9.0D, 64),
        ROOTER_SIV(Family.ROOTER, 4, 220.0D, 25.0D, 20.0D, 8, 9, 180, 5.0F, 18.0D, 220),
        ROOTERBALL(Family.ROOTERBALL, 0, 20.0D, 10.0D, 0.0D, 0, 0, 0, 0.0F, 0.0D, 0);

        private final Family family;
        private final int stage;
        private final double maxHealth;
        private final double armor;
        private final double attackDamage;
        private final int bombCount;
        private final int activeCap;
        private final int summonCooldown;
        private final float maxBlockHardness;
        private final double blockRange;
        private final int experience;

        Kind(Family family, int stage, double maxHealth, double armor, double attackDamage, int bombCount,
             int activeCap, int summonCooldown, float maxBlockHardness, double blockRange, int experience) {
            this.family = family;
            this.stage = stage;
            this.maxHealth = maxHealth;
            this.armor = armor;
            this.attackDamage = attackDamage;
            this.bombCount = bombCount;
            this.activeCap = activeCap;
            this.summonCooldown = summonCooldown;
            this.maxBlockHardness = maxBlockHardness;
            this.blockRange = blockRange;
            this.experience = experience;
        }

        private boolean isRooterBall() {
            return family == Family.ROOTERBALL;
        }

        public int stage() {
            return stage;
        }

        private int rootmassCystRange() {
            return switch (this) {
                case ROOTER_SI -> 16;
                case ROOTER_SII -> 32;
                case ROOTER_SIII -> 48;
                case ROOTER_SIV -> 128;
                default -> 0;
            };
        }

        private int rootmassCystSpawnLimit() {
            return switch (this) {
                case ROOTER_SI -> 3;
                case ROOTER_SII -> 4;
                case ROOTER_SIII -> 5;
                case ROOTER_SIV -> 6;
                default -> 0;
            };
        }
    }
}
