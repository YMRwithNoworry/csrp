package alku.csrp.entity;

import alku.csrp.Csrp;
import alku.csrp.block.InfestedBlock;
import alku.csrp.block.entity.ParasiteCanisterBlockEntity;
import alku.csrp.config.WorldConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

/**
 * 还原原版超距消失机制（EntityParasiteBase.func_70623_bb + spawnCyst/storeBefDes）：
 * 寄生体将被自然规则移除时，若附近有调度柱则回收入库（storeBefDes，之后可再部署），
 * 否则在脚下生成活体囊肿（spawnCyst），避免凭空消失。
 *
 * <p>同时补齐原版 {@code EntityParasiteBase.canD} 的「能否自然消失」门控：
 * 原版 Nexus 家族（{@code EntityPStationaryArchitect} 及其子类 Dispatcher/Rooter/Beckon，
 * 以及覆写过的 {@code EntityDodSIV}/{@code EntityVenkrolSIV}）在构造函数里写入
 * {@code SRPConfig.rsDespawn}（默认 false），因此它们<strong>永不自然消失</strong>；
 * 其余族系使用 primitivedespawn/adapteddespawn/... （默认均为 true），与本模组现状一致。
 */
public final class ParasiteDespawnHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParasiteDespawnHandler.class);
    private static final double DISPATCHER_RECALL_RANGE = 48.0D;

    private ParasiteDespawnHandler() {
    }

    /** 是否为本模组的寄生体实体；回收/囊肿流程只处理这些实体。 */
    public static boolean isCsrpParasite(Mob mob) {
        if (!(mob instanceof Parasite)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return id != null && Csrp.MODID.equals(id.getNamespace());
    }

    /**
     * 原版 {@code EntityParasiteBase.func_70692_ba()}（canD）语义：该寄生体是否允许被自然规则移除。
     *
     * <p>Nexus 家族默认不允许（{@code SRPConfig.rsDespawn = false}）；但原版
     * {@code EntityPStationaryArchitect} 在寄生生物群系内会回到「可回收」状态，
     * 本模组没有 BiomeParasiteBase 这种生物群系类型，按既有约定以「脚下是寄生方块」
     * 近似该判定（与 {@code ParasiteCombatRules} 的区域判定保持一致）。
     */
    public static boolean canDespawnNaturally(Mob mob) {
        if (!(mob instanceof NexusParasiteEntity nexus) || nexus.getKind() == NexusParasiteEntity.Kind.ROOTERBALL) {
            // rooterball 在原版是 EntityLeemB extends EntityPStationary，canD 保持默认 true。
            return true;
        }
        if (WorldConfig.nexusDespawn()) {
            // 原版 SRPConfig.rsDespawn 开关（默认 false）；开启后 Nexus 与普通怪物一样可自然消失。
            return true;
        }
        return mob.level() instanceof ServerLevel level && !level.isClientSide
                && inParasiteRegion(level, mob);
    }

    /**
     * 自然消失前的收尾流程（原版 spawnCyst + storeBefDes）。
     *
     * <p>本模组约定寄生体<strong>绝不凭空消失</strong>：回收入调度柱或原地落下活体囊肿，
     * 两者都做不到时返回 {@code false}，调用方必须放弃这次移除。
     *
     * @return true 表示已经留下痕迹（或该实体不受本流程管辖），可以安全移除
     */
    public static boolean leaveDespawnTrace(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || level.isClientSide || !isCsrpParasite(mob)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        NexusParasiteEntity dispatcher = findNearestDispatcher(level, mob);
        if (dispatcher != null && dispatcher.recallParasite(id.toString())) {
            level.sendParticles(ParticleTypes.PORTAL,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(),
                    16, 0.3D, 0.4D, 0.3D, 0.02D);
            return true;
        }
        if (ParasiteCanisterBlockEntity.placeFromDespawn(level, mob.blockPosition())) {
            return true;
        }
        LOGGER.debug("CSRP parasite {} at {} could not be recalled or leave a cyst; despawn refused",
                id, mob.blockPosition());
        return false;
    }

    private static boolean inParasiteRegion(ServerLevel level, Mob mob) {
        return level.getBlockState(mob.blockPosition().below()).getBlock() instanceof InfestedBlock;
    }

    private static NexusParasiteEntity findNearestDispatcher(ServerLevel level, Mob mob) {
        List<NexusParasiteEntity> dispatchers = level.getEntitiesOfClass(NexusParasiteEntity.class,
                mob.getBoundingBox().inflate(DISPATCHER_RECALL_RANGE),
                entity -> entity.isAlive() && entity.isDispatcherFamily());
        return dispatchers.stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }
}
