package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.entity.AirscrewEntity;
import alku.csrp.entity.BuglinEntity;
import alku.csrp.entity.CarrierFlyingEntity;
import alku.csrp.entity.CarrierHeavyEntity;
import alku.csrp.entity.CarrierLightEntity;
import alku.csrp.entity.CruxEntity;
import alku.csrp.entity.CruxThrownBlockDamageEntity;
import alku.csrp.entity.DredgeEntity;
import alku.csrp.entity.GnatEntity;
import alku.csrp.entity.HeedEntity;
import alku.csrp.entity.IncompleteCruxEntity;
import alku.csrp.entity.LongarmsEntity;
import alku.csrp.entity.PullingBallEntity;
import alku.csrp.entity.RupterEntity;
import alku.csrp.entity.ScaryOrbEntity;
import alku.csrp.entity.SummonerEntity;
import alku.csrp.entity.ThrallEntity;
import alku.csrp.entity.VerminEntity;
import alku.csrp.entity.VisceraEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Csrp.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BuglinEntity>> BUGLIN =
            ENTITIES.register("buglin", () -> EntityType.Builder.of(BuglinEntity::new, MobCategory.MONSTER)
                    .sized(0.5F, 0.3F)
                    .clientTrackingRange(8)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "buglin").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<RupterEntity>> RUPTER =
            ENTITIES.register("rupter", () -> EntityType.Builder.of(RupterEntity::new, MobCategory.MONSTER)
                    .sized(0.85F, 1.0F)
                    .clientTrackingRange(8)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "rupter").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<LongarmsEntity>> PRI_LONGARMS =
            monster("pri_longarms", LongarmsEntity::new, 1.0F, 3.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<SummonerEntity>> PRI_SUMMONER =
            monster("pri_summoner", SummonerEntity::new, 1.3F, 2.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<VerminEntity>> PRI_VERMIN =
            monster("pri_vermin", VerminEntity::new, 1.3F, 1.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<VisceraEntity>> PRI_VISCERA =
            monster("pri_viscera", VisceraEntity::new, 1.3F, 2.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<GnatEntity>> GNAT =
            monster("gnat", GnatEntity::new, 0.55F, 0.45F);
    public static final DeferredHolder<EntityType<?>, EntityType<CarrierHeavyEntity>> CARRIER_HEAVY =
            monster("carrier_heavy", CarrierHeavyEntity::new, 1.3F, 3.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<CarrierLightEntity>> CARRIER_LIGHT =
            monster("carrier_light", CarrierLightEntity::new, 0.85F, 2.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<CarrierFlyingEntity>> CARRIER_FLYING =
            monster("carrier_flying", CarrierFlyingEntity::new, 1.4F, 2.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<CruxEntity>> CRUX =
            monster("crux", CruxEntity::new, 1.13333F, 3.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<IncompleteCruxEntity>> CRUX_INCOMPLETE =
            monster("crux_incomplete", IncompleteCruxEntity::new, 1.31F, 1.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<AirscrewEntity>> AIRSCREW =
            monster("airscrew", AirscrewEntity::new, 2.1F, 7.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<HeedEntity>> HEED =
            monster("heed", HeedEntity::new, 0.9F, 1.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<DredgeEntity>> DREDGE =
            monster("dredge", DredgeEntity::new, 0.8F, 3.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<ThrallEntity>> THRALL =
            monster("thrall", ThrallEntity::new, 0.8F, 3.05F);
    public static final DeferredHolder<EntityType<?>, EntityType<PullingBallEntity>> PULLING_BALL =
            ENTITIES.register("pulling_ball", () -> EntityType.Builder
                    .<PullingBallEntity>of(PullingBallEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "pulling_ball").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<CruxThrownBlockDamageEntity>> CRUX_BLOCK_DAMAGE =
            ENTITIES.register("crux_block_damage", () -> EntityType.Builder
                    .<CruxThrownBlockDamageEntity>of(CruxThrownBlockDamageEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(0).updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "crux_block_damage").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ScaryOrbEntity>> SCARY_ORB =
            ENTITIES.register("scary_orb", () -> EntityType.Builder.<ScaryOrbEntity>of(ScaryOrbEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "scary_orb").toString()));

    private static <T extends net.minecraft.world.entity.Mob> DeferredHolder<EntityType<?>, EntityType<T>> monster(
            String id, EntityType.EntityFactory<T> factory, float width, float height) {
        return ENTITIES.register(id, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height).clientTrackingRange(8)
                .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, id).toString()));
    }

    private ModEntities() {
    }
}
