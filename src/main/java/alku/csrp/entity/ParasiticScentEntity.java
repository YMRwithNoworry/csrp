package alku.csrp.entity;

import alku.csrp.Csrp;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/** Original collective-consciousness Scent state machine and reinforcement controller. */
public final class ParasiticScentEntity extends Entity {
    public static final int SCENT_CAP = 2;
    public static final int OBSERVER_LIFE_TICKS = 60 * 20;
    private static final String SCENT_HOST_TAG = "SRPScentBuffed";
    private static final int WORLD_MOB_CAP = 40;
    private static final int WORLD_MOB_CAP_PER_PLAYER = 5;
    private static final int MIN_DISTANCE = 7;
    private static final int MAX_DISTANCE = 16;
    private static final int RANGE_CAP = 32;
    private static final int[] LEVEL_POINTS = {0, 10, 25, 75, 150, 150, 240, 360, 500};
    private static final int[] MIN_MOBS = {3, 4, 4, 3, 3, 4, 2, 2, 2};
    private static final int[] MAX_MOBS = {4, 6, 6, 4, 6, 6, 5, 5, 5};
    private static final int[] MIN_WAVES = {1, 1, 2, 3, 3, 3, 2, 2, 2};
    private static final int[] MAX_WAVES = {2, 3, 4, 6, 4, 4, 5, 5, 5};
    private static final List<List<ResourceLocation>> LEVEL_MOBS = List.of(
            ids("rupter"),
            ids("rupter"),
            ids("rupter", "sim_adventurerhead", "sim_endermanhead", "sim_humanhead",
                    "sim_horsehead", "sim_villagerhead", "sim_pighead", "sim_cowhead",
                    "sim_wolfhead", "sim_sheephead", "heed"),
            ids("rupter", "sim_adventurerhead", "sim_endermanhead", "sim_humanhead",
                    "sim_horsehead", "sim_villagerhead", "sim_pighead", "sim_cowhead",
                    "sim_wolfhead", "sim_sheephead", "sim_adventurer", "sim_enderman",
                    "sim_human", "sim_horse", "sim_villager", "sim_pig", "sim_cow",
                    "sim_wolf", "sim_sheep", "heed"),
            tierFourMobs(), tierFourMobs(), tierFourMobs(), tierFourMobs(), tierFourMobs());

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private boolean phaseOne = true;
    private byte scentState;
    private int lifeTicks = 600;
    private int currentLife;
    private int danger = 100;
    private int activity;
    private int delay;
    private byte scentLevel;
    private byte scentReaction;
    private int minimumWaves;
    private int maximumWaves;
    private int minimumMobs;
    private int maximumMobs;
    private boolean followTarget;
    private boolean dieAfterKilling;
    private int loopLife = 103;
    private double originalHostMaxHealth = -1.0D;
    private boolean hostBuffApplied;
    private UUID targetId;
    private UUID hostId;

    public ParasiticScentEntity(EntityType<? extends ParasiticScentEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
        updateScentLevel();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (phaseOne) {
            tickObserverPreparation(serverLevel);
        } else {
            tickActiveScent(serverLevel);
        }
    }

    private void tickObserverPreparation(ServerLevel level) {
        if (currentLife < 600) {
            currentLife++;
        }
        LivingEntity host = host();
        if (host == null && currentLife >= 300 && currentLife % 20 == 0) {
            selectAndBuffHost(level);
            host = host();
        } else if (host != null && !host.isAlive()) {
            finish();
            return;
        }
        bossEvent.setProgress(progress());
        if (currentLife == 600 && --danger <= 0) {
            phaseOne = false;
        }
    }

    private void tickActiveScent(ServerLevel level) {
        bossEvent.setProgress(progress());
        if (scentState >= 5) {
            currentLife -= 20;
        }
        LivingEntity host = host();
        if (currentLife <= 0 || level.getDifficulty() == Difficulty.PEACEFUL || loopLife < 0
                || host == null || !host.isAlive()) {
            finish();
            return;
        }

        LivingEntity target = getTargetToKill();
        if (target == null) {
            if (scentState > 1) {
                scentState = 1;
            }
        } else if (distanceToSqr(target) > 4096.0D) {
            if (dieAfterKilling) {
                finish();
                return;
            }
            setTargetToKill(null, false);
            scentState = 1;
        }
        if (delay > 0) {
            delay--;
            return;
        }

        followPreyTarget();
        target = getTargetToKill();
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            setTargetToKill(null, false);
            return;
        }
        switch (scentState) {
            case 0 -> {
                if (random.nextInt(3) == 0) {
                    observe(level);
                }
                listen();
            }
            case 1 -> {
                observe(level);
                listen();
            }
            case 4 -> tactical();
            case 5 -> attacker(level);
            case 6 -> buildWave(level);
            default -> {
            }
        }
    }

    private void selectAndBuffHost(ServerLevel level) {
        List<Mob> parasites = level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(80.0D),
                mob -> mob instanceof Parasite && !mob.getPersistentData().getBoolean(SCENT_HOST_TAG));
        if (parasites.isEmpty()) {
            return;
        }
        LivingEntity host = parasites.get(random.nextInt(parasites.size()));
        hostId = host.getUUID();
        host.addEffect(new MobEffectInstance(MobEffects.GLOWING, currentLife, 3, false, true), this);
        AttributeInstance maximumHealth = host.getAttribute(Attributes.MAX_HEALTH);
        if (maximumHealth == null) {
            return;
        }
        originalHostMaxHealth = maximumHealth.getBaseValue();
        maximumHealth.setBaseValue(originalHostMaxHealth * 10.0D);
        host.setHealth(host.getMaxHealth());
        host.getPersistentData().putBoolean(SCENT_HOST_TAG, true);
        hostBuffApplied = true;
    }

    private void observe(ServerLevel level) {
        if (tickCount % 20 != 0 || followTarget) {
            return;
        }
        LivingEntity target = getTargetToKill();
        AABB area = getBoundingBox().inflate(80.0D);
        if (target != null) {
            activity++;
            // 使用 getEntitiesOfClass 并提前过滤，减少不必要的迭代
            List<Mob> nearbyParasites = level.getEntitiesOfClass(Mob.class, area,
                mob -> mob instanceof Parasite && mob.isAlive());

            double followRangeSq = 1024.0D; // 预计算范围的平方
            for (Mob parasite : nearbyParasites) {
                LivingEntity currentTarget = parasite.getTarget();
                if ((currentTarget == null || !currentTarget.isAlive())
                        && parasite.distanceToSqr(target) <= followRangeSq) {
                    parasite.setTarget(target);
                }
            }
            return;
        }

        LivingEntity closest = null;
        double closestDistance = 4096.0D;
        // 直接在获取实体时过滤，减少不必要的检查
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, area, this::isValidTarget)) {
            double distance = distanceToSqr(candidate);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }
        setTargetToKill(closest, false);
    }

    private void listen() {
        if (activity >= scentReaction && getTargetToKill() != null) {
            scentState = 4;
            activity = 2;
            warnPlayers("srp.msg.scent.active");
        }
    }

    private void tactical() {
        if (activity >= 12) {
            scentState = 5;
            activity -= 5;
        } else if (getTargetToKill() != null) {
            activity++;
        } else if (tickCount % 80 == 0 && --activity <= 0) {
            scentState = 1;
        }
    }

    private void attacker(ServerLevel level) {
        if (moveNearbyParasites(level) <= 6) {
            scentState = 6;
        } else {
            delay = 100;
            scentState = 4;
        }
    }

    private void buildWave(ServerLevel level) {
        LivingEntity target = getTargetToKill();
        if (target == null) {
            scentState = 1;
            return;
        }
        level.playSound(null, blockPosition(), ModSounds.SCENT_WAVE.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
        level.playSound(null, target.blockPosition(), ModSounds.SCENT_WAVE.get(), SoundSource.HOSTILE, 10.0F, 1.0F);
        loopLife--;
        int placed = 0;
        int desired = randomBetween(minimumWaves, maximumWaves);
        for (int attempt = 0; attempt < 10 && placed < desired; attempt++) {
            placed += placeWave(level);
        }
        delay = placed > 0 ? 100 + placed * 20 : 100;
        scentState = 4;
    }

    private void followPreyTarget() {
        if (!followTarget) {
            return;
        }
        LivingEntity target = getTargetToKill();
        if (target == null) {
            followTarget = false;
            return;
        }
        if (distanceToSqr(target) > 144.0D && target.hasEffect(ModMobEffects.PREY)) {
            moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        }
    }

    private int moveNearbyParasites(ServerLevel level) {
        LivingEntity target = getTargetToKill();
        if (target == null) {
            return 0;
        }
        int moved = 0;
        AABB area = getBoundingBox().inflate(80.0D);
        BlockPos centerPos = blockPosition();
        int lightLevel = level.getBrightness(LightLayer.BLOCK, centerPos);

        // 提前过滤并限制处理数量，避免一次处理过多实体
        List<Mob> parasites = level.getEntitiesOfClass(Mob.class, area,
            mob -> mob instanceof Parasite && mob.isAlive() && mob.getTarget() == null
                   && mob.hurtTime == 0 && mob.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.0D);

        // 如果光照太强，提前退出
        if (lightLevel >= 5) {
            return countParasites(level) > mobCap(level) ? 20 : moved;
        }

        // 限制每次最多移动10个寄生虫，避免性能问题
        int maxToMove = Math.min(10, parasites.size());
        for (int i = 0; i < maxToMove; i++) {
            Mob parasite = parasites.get(i);
            if (moveParasiteNearTarget(level, parasite, target)) {
                parasite.setTarget(target);
                moved++;
            }
        }
        return countParasites(level) > mobCap(level) ? 20 : moved;
    }

    private boolean moveParasiteNearTarget(ServerLevel level, Mob parasite, LivingEntity target) {
        for (int attempt = 0; attempt < 7; attempt++) {
            BlockPos floor = randomFloorAround(level, target, MAX_DISTANCE);
            if (floor == null || level.getBrightness(LightLayer.BLOCK, floor) > 4) {
                continue;
            }
            AABB exclusion = new AABB(floor).inflate(MIN_DISTANCE, 5.0D, MIN_DISTANCE);
            if (!level.getEntitiesOfClass(LivingEntity.class, exclusion,
                    living -> !(living instanceof Parasite)).isEmpty()) {
                continue;
            }
            double distance = Math.sqrt(distanceToSqr(floor.getX(), floor.getY(), floor.getZ()));
            if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) {
                continue;
            }
            parasite.teleportTo(floor.getX() + 0.5D, floor.getY(), floor.getZ() + 0.5D);
            return true;
        }
        return false;
    }

    private int placeWave(ServerLevel level) {
        LivingEntity target = getTargetToKill();
        if (target == null) {
            return 0;
        }

        // 提前检查寄生虫数量，避免不必要的计算
        int currentParasites = countParasites(level);
        int cap = mobCap(level);
        if (currentParasites > cap) {
            return 0;
        }

        BlockPos floor = randomFloorAround(level, target, MAX_DISTANCE);
        if (floor == null) {
            return 0;
        }
        double distance = Math.sqrt(distanceToSqr(floor.getX(), floor.getY(), floor.getZ()));
        if (distance < MIN_DISTANCE || distance > RANGE_CAP) {
            return 0;
        }

        // 优化：缩小搜索范围，只检查附近是否有非寄生虫实体
        AABB area = new AABB(floor).inflate(MAX_DISTANCE, 16.0D, MAX_DISTANCE);
        List<LivingEntity> living = level.getEntitiesOfClass(LivingEntity.class, area);

        // 快速检查是否只有寄生虫
        boolean hasNonParasite = false;
        for (LivingEntity entity : living) {
            if (!(entity instanceof Parasite)) {
                hasNonParasite = true;
                break;
            }
        }

        if (!hasNonParasite) {
            return 0;
        }

        updateScentLevel();
        return spawnWorm(level, target, floor, mobsForAreaLevel(level)) ? 1 : 0;
    }

    private boolean spawnWorm(ServerLevel level, LivingEntity target, BlockPos floor,
                              List<ResourceLocation> payloadTypes) {
        DeterrentParasiteEntity worm = ModEntities.WORM.get().create(level);
        if (worm == null) {
            return false;
        }
        worm.moveTo(floor.getX() + 0.5D, floor.getY(), floor.getZ() + 0.5D,
                random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(worm, worm.getBoundingBox().inflate(1.0D, 7.0D, 1.0D))) {
            return false;
        }
        worm.finalizeSpawn(level, level.getCurrentDifficultyAt(floor), MobSpawnType.MOB_SUMMONED, null);
        worm.setTarget(target);
        worm.setWormPayload(minimumMobs, maximumMobs);
        worm.setWormPayloadTypes(payloadTypes);
        return level.addFreshEntity(worm);
    }

    private List<ResourceLocation> mobsForAreaLevel(ServerLevel level) {
        int highest = scentLevel;
        for (ParasiticScentEntity scent : level.getEntitiesOfClass(ParasiticScentEntity.class,
                getBoundingBox().inflate(80.0D))) {
            highest = Math.max(highest, scent.scentLevel);
        }
        return LEVEL_MOBS.get(Mth.clamp(highest, 0, LEVEL_MOBS.size() - 1));
    }

    private void updateScentLevel() {
        int level = 0;
        for (int index = LEVEL_POINTS.length - 1; index >= 1; index--) {
            if (danger >= LEVEL_POINTS[index]) {
                level = index;
                break;
            }
        }
        scentLevel = (byte) level;
        minimumMobs = MIN_MOBS[level];
        maximumMobs = MAX_MOBS[level];
        minimumWaves = MIN_WAVES[level];
        maximumWaves = MAX_WAVES[level];
    }

    public boolean setTargetToKill(LivingEntity target, boolean checkAttributes) {
        if (target == null) {
            targetId = null;
            return true;
        }
        if (followTarget && targetId != null && !target.getUUID().equals(targetId)) {
            return false;
        }
        if (!target.isAlive() || checkAttributes && !passesAttributeCheck(target)) {
            return false;
        }
        targetId = target.getUUID();
        return true;
    }

    public LivingEntity getTargetToKill() {
        LivingEntity target = resolveLiving(targetId);
        if (target == null || !target.isAlive()) {
            targetId = null;
            return null;
        }
        return target;
    }

    public void increaseDanger(int amount, boolean add) {
        danger = add ? danger + amount : amount;
        updateScentLevel();
    }

    public int getDanger() {
        return danger;
    }

    public void setScentLevel(int level) {
        int clamped = Mth.clamp(level, 0, LEVEL_POINTS.length - 1);
        danger = LEVEL_POINTS[clamped];
        updateScentLevel();
    }

    public void increaseActivity(int amount, boolean add) {
        if (amount <= 100) {
            activity = add ? activity + amount : amount;
        }
    }

    public void setScentReaction(int reaction, boolean override) {
        if (override || reaction > scentReaction) {
            scentReaction = (byte) reaction;
        }
    }

    public void setScentLife(int lifeTicks) {
        this.lifeTicks = Math.max(1, lifeTicks);
    }

    public int getScentLife() {
        return lifeTicks;
    }

    public void setScentState(int scentState) {
        this.scentState = (byte) scentState;
    }

    public byte getScentState() {
        return scentState;
    }

    public void setCanFollow(boolean followTarget) {
        this.followTarget = followTarget;
    }

    public boolean getCanFollow() {
        return followTarget;
    }

    public void setDieAfterKilling(boolean dieAfterKilling) {
        this.dieAfterKilling = dieAfterKilling;
    }

    public byte getScentLevel() {
        return scentLevel;
    }

    public void warnPlayers(String translationKey) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(80.0D))) {
            player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }

    public static int scentBonus(int phase) {
        return switch (phase) {
            case 0 -> -200;
            case 1 -> 5;
            case 2 -> 20;
            case 3 -> 50;
            case 4 -> 90;
            case 5 -> 150;
            case 6 -> 240;
            case 7 -> 360;
            case 8 -> 500;
            case 9 -> 600;
            case 10 -> 800;
            default -> 1;
        };
    }

    public static int scentReaction(int phase) {
        return switch (phase) {
            case 0 -> 11;
            case 1 -> 10;
            case 2, 3 -> 9;
            case 4 -> 8;
            case 5 -> 6;
            case 6 -> 5;
            case 7 -> 4;
            case 8, 9, 10 -> 2;
            default -> 5;
        };
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof Parasite || entity instanceof WaterAnimal || entity instanceof Creeper) {
            return false;
        }
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return passesAttributeCheck(entity);
    }

    private boolean passesAttributeCheck(LivingEntity entity) {
        int conditions = 0;
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance armor = entity.getAttribute(Attributes.ARMOR);
        AttributeInstance damage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (health != null && health.getValue() >= 20.0D) conditions++;
        if (armor != null && armor.getValue() >= 0.0D) conditions++;
        if (damage != null && damage.getValue() >= 4.0D) conditions++;
        return conditions >= 2 || conditions == 0;
    }

    private BlockPos randomFloorAround(ServerLevel level, LivingEntity target, int range) {
        int x = Mth.floor(target.getX() - range + random.nextInt(range * 2));
        int z = Mth.floor(target.getZ() - range + random.nextInt(range * 2));
        return findFloor(level, BlockPos.containing(x, target.getY(), z), 10);
    }

    private static BlockPos findFloor(ServerLevel level, BlockPos start, int attempts) {
        BlockPos cursor = start;
        for (int attempt = 0; attempt < attempts; attempt++) {
            if (level.getBlockState(cursor).isAir()) {
                if (!level.getBlockState(cursor.below()).isAir()) return cursor;
                cursor = cursor.below();
            } else {
                cursor = cursor.above();
            }
        }
        return null;
    }

    // 缓存寄生虫数量以避免每次都遍历所有实体
    private int cachedParasiteCount = -1;
    private long lastCountTick = 0;

    private int countParasites(ServerLevel level) {
        // 每20 tick（1秒）才重新计算一次
        if (tickCount - lastCountTick >= 20 || cachedParasiteCount < 0) {
            cachedParasiteCount = 0;
            // 只在附近区域搜索，而不是整个世界
            AABB searchArea = getBoundingBox().inflate(128.0D);
            for (Entity entity : level.getEntities(null, searchArea)) {
                if (entity instanceof Parasite) cachedParasiteCount++;
            }
            lastCountTick = tickCount;
        }
        return cachedParasiteCount;
    }

    private static int mobCap(ServerLevel level) {
        return WORLD_MOB_CAP + level.players().size() * WORLD_MOB_CAP_PER_PLAYER;
    }

    private int randomBetween(int minimum, int maximum) {
        return minimum + random.nextInt(Math.max(1, maximum - minimum + 1));
    }

    private float progress() {
        return Mth.clamp((currentLife + 1.0F) / Math.max(1.0F, lifeTicks), 0.0F, 1.0F);
    }

    private LivingEntity host() {
        return resolveLiving(hostId);
    }

    private LivingEntity resolveLiving(UUID id) {
        if (id == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void finish() {
        cleanupHostBuff();
        discard();
    }

    private void cleanupHostBuff() {
        if (!hostBuffApplied) {
            return;
        }
        LivingEntity host = host();
        if (host != null) {
            AttributeInstance maximumHealth = host.getAttribute(Attributes.MAX_HEALTH);
            if (maximumHealth != null && host.isAlive()) {
                double restored = originalHostMaxHealth > 0.0D
                        ? originalHostMaxHealth : maximumHealth.getBaseValue() / 10.0D;
                maximumHealth.setBaseValue(restored);
                host.setHealth(Math.min(host.getHealth(), host.getMaxHealth()));
            }
            host.getPersistentData().remove(SCENT_HOST_TAG);
        }
        hostBuffApplied = false;
        originalHostMaxHealth = -1.0D;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy() && !level().isClientSide) {
            cleanupHostBuff();
        }
        super.remove(reason);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        phaseOne = !tag.contains("scent_phase_one") || tag.getBoolean("scent_phase_one");
        scentState = tag.getByte("scent_state");
        lifeTicks = tag.contains("scent_life") ? tag.getInt("scent_life") : 600;
        currentLife = tag.getInt("scent_current_life");
        danger = tag.contains("scent_danger") ? tag.getInt("scent_danger") : 100;
        activity = tag.getInt("scent_activity");
        delay = tag.getInt("scent_delay");
        scentReaction = tag.getByte("scent_reaction");
        loopLife = tag.contains("scent_loops") ? tag.getInt("scent_loops") : 103;
        followTarget = tag.getBoolean("scent_following");
        dieAfterKilling = tag.getBoolean("scent_die_after_killing");
        hostBuffApplied = tag.getBoolean("scent_host_buff_applied");
        originalHostMaxHealth = tag.contains("scent_host_original_max")
                ? tag.getDouble("scent_host_original_max") : -1.0D;
        targetId = tag.hasUUID("scent_target") ? tag.getUUID("scent_target") : null;
        hostId = tag.hasUUID("scent_host") ? tag.getUUID("scent_host") : null;
        updateScentLevel();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("scent_phase_one", phaseOne);
        tag.putByte("scent_state", scentState);
        tag.putInt("scent_life", lifeTicks);
        tag.putInt("scent_current_life", currentLife);
        tag.putInt("scent_danger", danger);
        tag.putInt("scent_activity", activity);
        tag.putInt("scent_delay", delay);
        tag.putByte("scent_reaction", scentReaction);
        tag.putInt("scent_loops", loopLife);
        tag.putBoolean("scent_following", followTarget);
        tag.putBoolean("scent_die_after_killing", dieAfterKilling);
        tag.putBoolean("scent_host_buff_applied", hostBuffApplied);
        tag.putDouble("scent_host_original_max", originalHostMaxHealth);
        if (targetId != null) tag.putUUID("scent_target", targetId);
        if (hostId != null) tag.putUUID("scent_host", hostId);
    }

    private static List<ResourceLocation> tierFourMobs() {
        return ids("sim_adventurer", "sim_enderman", "sim_human", "sim_horse", "sim_villager",
                "sim_pig", "sim_cow", "sim_wolf", "sim_sheep", "pri_longarms", "pri_manducater",
                "pri_reeker", "pri_yelloweye", "pri_summoner", "pri_bolster", "pri_arachnida",
                "pri_vermin", "heed", "crux");
    }

    private static List<ResourceLocation> ids(String... paths) {
        return java.util.Arrays.stream(paths)
                .map(path -> ResourceLocation.fromNamespaceAndPath(Csrp.MODID, path))
                .toList();
    }
}
