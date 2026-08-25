package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.entity.AbominationEntity;
import alku.csrp.entity.AncientParasiteEntity;
import alku.csrp.entity.AirscrewEntity;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.AssimilatedParasiteEntity;
import alku.csrp.entity.AssimilatedDragonEntity;
import alku.csrp.entity.AssimilatedDragonHeadEntity;
import alku.csrp.entity.DragonEggAssimilationEntity;
import alku.csrp.entity.AssimilatedEndermanEntity;
import alku.csrp.entity.AssimilatedHeadEntity;
import alku.csrp.entity.AssimilatedVariantEntity;
import alku.csrp.entity.BiomassEntity;
import alku.csrp.entity.BombEntity;
import alku.csrp.entity.BuglinEntity;
import alku.csrp.entity.CarrierFlyingEntity;
import alku.csrp.entity.CarrierHeavyEntity;
import alku.csrp.entity.CarrierLightEntity;
import alku.csrp.entity.CarrierWormEntity;
import alku.csrp.entity.CruxEntity;
import alku.csrp.entity.CruxThrownBlockDamageEntity;
import alku.csrp.entity.HaunterDamageEntity;
import alku.csrp.entity.HaunterHomingProjectileEntity;
import alku.csrp.entity.DredgeEntity;
import alku.csrp.entity.DeterrentParasiteEntity;
import alku.csrp.entity.FeralEndermanEntity;
import alku.csrp.entity.FeralParasiteEntity;
import alku.csrp.entity.FlamEntity;
import alku.csrp.entity.DraconiteEntity;
import alku.csrp.entity.GnatEntity;
import alku.csrp.entity.GoreEntity;
import alku.csrp.entity.HeedEntity;
import alku.csrp.entity.HiBlazeEntity;
import alku.csrp.entity.HiGolemEntity;
import alku.csrp.entity.HiSkeletonEntity;
import alku.csrp.entity.HostEntity;
import alku.csrp.entity.HostIIEntity;
import alku.csrp.entity.IncompleteCruxEntity;
import alku.csrp.entity.IncompleteFormMediumEntity;
import alku.csrp.entity.IncompleteFormSmallEntity;
import alku.csrp.entity.KirinEntity;
import alku.csrp.entity.LiceEntity;
import alku.csrp.entity.LongarmsEntity;
import alku.csrp.entity.ManglerEntity;
import alku.csrp.entity.MarauderEntity;
import alku.csrp.entity.MarauderTendrilEntity;
import alku.csrp.entity.MarauderizedBearEntity;
import alku.csrp.entity.MarauderizedCowEntity;
import alku.csrp.entity.MarauderizedEndermanEntity;
import alku.csrp.entity.MarauderizedHumanEntity;
import alku.csrp.entity.MarauderizedSheepEntity;
import alku.csrp.entity.MarauderizedVillagerEntity;
import alku.csrp.entity.ParasiteProjectileEntity;
import alku.csrp.entity.ParasiticScentEntity;
import alku.csrp.entity.PrimitiveVariantEntity;
import alku.csrp.entity.PreeminentParasiteEntity;
import alku.csrp.entity.PureParasiteEntity;
import alku.csrp.entity.PullingBallEntity;
import alku.csrp.entity.RupterEntity;
import alku.csrp.entity.RemainEntity;
import alku.csrp.entity.ScaryOrbEntity;
import alku.csrp.entity.ShockwaveEntity;
import alku.csrp.entity.KirinSlashEntity;
import alku.csrp.entity.WardenShockwaveEntity;
import alku.csrp.entity.VoidOrbEntity;
import alku.csrp.entity.SimAdventurerEntity;
import alku.csrp.entity.SimAdventurerHeadEntity;
import alku.csrp.entity.SimHumanEntity;
import alku.csrp.entity.SummonerEntity;
import alku.csrp.entity.SourceEntity;
import alku.csrp.entity.ThrallEntity;
import alku.csrp.entity.TendrilEntity;
import alku.csrp.entity.ToxicCloudEntity;
import alku.csrp.entity.VerminEntity;
import alku.csrp.entity.VisceraEntity;
import alku.csrp.entity.WaveEntity;
import alku.csrp.entity.MovingFleshEntity;
import alku.csrp.entity.NadeEntity;
import alku.csrp.entity.OrbBoomEntity;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.WorkerEntity;
import alku.csrp.entity.ArchitectEntity;
import alku.csrp.entity.AncientPodEntity;
import alku.csrp.entity.AntiInfestedBlockEntity;
import alku.csrp.entity.DreadnautTentacleEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Csrp.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BiomassEntity>> BIOMASS =
            ENTITIES.register("biomass", () -> EntityType.Builder.of(BiomassEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(4)
                    .updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "biomass").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<ShockwaveEntity>> SHOCKWAVE =
            ENTITIES.register("waveshock", () -> EntityType.Builder
                    .<ShockwaveEntity>of(ShockwaveEntity::new, MobCategory.MISC)
                    .sized(3.1F, 0.2F)
                    .clientTrackingRange(4)
                    .updateInterval(3)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "waveshock").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ShockwaveEntity>> SHOCKWAVE_LEGACY =
            ENTITIES.register("shockwave", () -> EntityType.Builder
                    .<ShockwaveEntity>of(ShockwaveEntity::new, MobCategory.MISC)
                    .sized(3.1F, 0.2F).clientTrackingRange(4).updateInterval(3)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "shockwave").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<KirinSlashEntity>> KIRIN_SLASH =
            ENTITIES.register("kirin_slash", () -> EntityType.Builder
                    .<KirinSlashEntity>of(KirinSlashEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F).clientTrackingRange(6).updateInterval(2)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "kirin_slash").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<WardenShockwaveEntity>> WARDEN_SHOCKWAVE =
            ENTITIES.register("warden_waveshock", () -> EntityType.Builder
                    .<WardenShockwaveEntity>of(WardenShockwaveEntity::new, MobCategory.MISC)
                    .sized(3.1F, 0.2F)
                    .clientTrackingRange(4)
                    .updateInterval(3)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "warden_waveshock").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<BuglinEntity>> BUGLIN =
            ENTITIES.register("buglin", () -> EntityType.Builder.of(BuglinEntity::new, MobCategory.MONSTER)
                    .sized(0.5F, 0.3F)
                    .clientTrackingRange(8)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "buglin").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<RupterEntity>> RUPTER =
            ENTITIES.register("rupter", () -> EntityType.Builder.of(RupterEntity::new, MobCategory.MONSTER)
                    .sized(0.85F, 1.0F).eyeHeight(0.5F)
                    .clientTrackingRange(8)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "rupter").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<LongarmsEntity>> PRI_LONGARMS =
            monster("pri_longarms", LongarmsEntity::new, 1.0F, 3.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<SummonerEntity>> PRI_SUMMONER =
            monster("pri_summoner", SummonerEntity::new, 1.3F, 2.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<VerminEntity>> PRI_VERMIN =
            monster("pri_vermin", VerminEntity::new, 1.1F, 1.4F, 0.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<VisceraEntity>> PRI_VISCERA =
            monster("pri_viscera", VisceraEntity::new, 1.211F, 2.351F, 1.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_ARACHNIDA =
            monster("pri_arachnida", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.ARACHNIDA), 0.9F, 2.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_BOLSTER =
            monster("pri_bolster", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.BOLSTER), 0.9F, 2.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_BURROWER =
            monster("pri_burrower", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.BURROWER), 1.0F, 0.25F, 0.25F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_DEVOURER =
            monster("pri_devourer", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.DEVOURER), 1.3F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_MANDUCATER =
            monster("pri_manducater", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.MANDUCATER), 1.3F, 1.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_REEKER =
            monster("pri_reeker", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.REEKER), 0.9F, 2.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_TOZOON =
            monster("pri_tozoon", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.TOZOON), 0.978F, 1.2F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_YELLOWEYE =
            monster("pri_yelloweye", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.YELLOWEYE), 0.4F, 1.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_ARACHNIDA =
            monster("ada_arachnida", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.ARACHNIDA), 1.901F, 2.85F, 1.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_BOLSTER =
            monster("ada_bolster", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.BOLSTER), 1.3F, 3.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_BURROWER =
            monster("ada_burrower", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.BURROWER), 1.321F, 1.2F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_DEVOURER =
            monster("ada_devourer", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.DEVOURER), 0.901F, 3.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_LONGARMS =
            monster("ada_longarms", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.LONGARMS), 0.901F, 3.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_MANDUCATER =
            monster("ada_manducater", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.MANDUCATER), 1.4F, 2.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_REEKER =
            monster("ada_reeker", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.REEKER), 1.3F, 3.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_SUMMONER =
            monster("ada_summoner", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.SUMMONER), 0.901F, 3.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_TOZOON =
            monster("ada_tozoon", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.TOZOON), 1.321F, 1.2F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_VERMIN =
            monster("ada_vermin", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.VERMIN), 1.1F, 1.4F, 0.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_VISCERA =
            monster("ada_viscera", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.VISCERA), 1.511F, 3.655F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_YELLOWEYE =
            monster("ada_yelloweye", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.YELLOWEYE), 1.3F, 2.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<GnatEntity>> GNAT =
            monster("gnat", GnatEntity::new, 0.85F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<CarrierHeavyEntity>> CARRIER_HEAVY =
            monster("carrier_heavy", CarrierHeavyEntity::new, 1.3F, 3.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<CarrierLightEntity>> CARRIER_LIGHT =
            monster("carrier_light", CarrierLightEntity::new, 0.85F, 2.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<CarrierFlyingEntity>> CARRIER_FLYING =
            monster("carrier_flying", CarrierFlyingEntity::new, 1.4F, 2.4F, 2.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<CarrierWormEntity>> CARRIER_WORM =
            monster("carrier_worm", CarrierWormEntity::new, 1.321F, 1.2F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<CruxEntity>> CRUX =
            monster("crux", CruxEntity::new, 1.13333F, 3.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<IncompleteCruxEntity>> CRUX_INCOMPLETE =
            monster("crux_incomplete", IncompleteCruxEntity::new, 1.31F, 1.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<AirscrewEntity>> AIRSCREW =
            monster("airscrew", AirscrewEntity::new, 2.1F, 7.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<HeedEntity>> HEED =
            monster("heed", HeedEntity::new, 0.9F, 1.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<DredgeEntity>> DREDGE =
            monster("dredge", DredgeEntity::new, 0.8F, 3.4F, 1.73F);
    public static final DeferredHolder<EntityType<?>, EntityType<ThrallEntity>> THRALL =
            monster("thrall", ThrallEntity::new, 0.8F, 3.05F);
    public static final DeferredHolder<EntityType<?>, EntityType<LiceEntity>> LICE =
            monster("lice", LiceEntity::new, 0.85F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<ManglerEntity>> MANGLER =
            monster("mangler", ManglerEntity::new, 1.0F, 1.0F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<HostEntity>> HOST =
            monster("host", HostEntity::new, 0.9F, 3.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<HostIIEntity>> HOSTII =
            monster("hostii", HostIIEntity::new, 1.5F, 7.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<IncompleteFormSmallEntity>> INCOMPLETEFORM_SMALL =
            monster("incompleteform_small", IncompleteFormSmallEntity::new, 0.6F, 0.85F);
    public static final DeferredHolder<EntityType<?>, EntityType<IncompleteFormMediumEntity>> INCOMPLETEFORM_MEDIUM =
            monster("incompleteform_medium", IncompleteFormMediumEntity::new, 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<DraconiteEntity>> DRACONITE =
            monster("draconite", DraconiteEntity::new, 2.4F, 3.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<KirinEntity>> KIRIN =
            ENTITIES.register("kirin", () -> EntityType.Builder.of(KirinEntity::new, MobCategory.MONSTER)
                    .sized(2.1271334F, 8.85F).eyeHeight(5.7F).clientTrackingRange(8)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "kirin").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<SimAdventurerEntity>> SIM_ADVENTURER =
            monster("sim_adventurer", SimAdventurerEntity::new, 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<SimAdventurerHeadEntity>> SIM_ADVENTURER_HEAD =
            monster("sim_adventurerhead", SimAdventurerHeadEntity::new, 0.7F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<MovingFleshEntity>> MOVINGFLESH =
            monster("movingflesh", MovingFleshEntity::new, 0.7F, 0.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedParasiteEntity>> SIM_BEAR =
            monster("sim_bear", (type, level) -> new AssimilatedParasiteEntity(type, level,
                    AssimilatedParasiteEntity.Kind.BEAR), 1.3F, 1.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedParasiteEntity>> SIM_COW =
            monster("sim_cow", (type, level) -> new AssimilatedParasiteEntity(type, level,
                    AssimilatedParasiteEntity.Kind.COW), 0.9F, 1.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedParasiteEntity>> SIM_PIG =
            monster("sim_pig", (type, level) -> new AssimilatedParasiteEntity(type, level,
                    AssimilatedParasiteEntity.Kind.PIG), 0.9F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedParasiteEntity>> SIM_SHEEP =
            monster("sim_sheep", (type, level) -> new AssimilatedParasiteEntity(type, level,
                    AssimilatedParasiteEntity.Kind.SHEEP), 0.9F, 1.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedParasiteEntity>> SIM_WOLF =
            monster("sim_wolf", (type, level) -> new AssimilatedParasiteEntity(type, level,
                    AssimilatedParasiteEntity.Kind.WOLF), 0.6F, 0.85F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedParasiteEntity>> SIM_SQUID =
            monster("sim_squid", (type, level) -> new AssimilatedParasiteEntity(type, level,
                    AssimilatedParasiteEntity.Kind.SQUID), 0.9F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedVariantEntity>> SIM_BIGSPIDER =
            monster("sim_bigspider", (type, level) -> new AssimilatedVariantEntity(type, level,
                    AssimilatedVariantEntity.Kind.BIGSPIDER), 1.9F, 2.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedDragonEntity>> SIM_DRAGONE =
            monster("sim_dragone", AssimilatedDragonEntity::new, 1.9F, 3.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<DragonEggAssimilationEntity>> DRAGON_EGG_ASSIMILATION =
            ENTITIES.register("dragon_egg_assimilation", () -> EntityType.Builder
                    .<DragonEggAssimilationEntity>of(DragonEggAssimilationEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(10).updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "dragon_egg_assimilation").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedDragonHeadEntity>> SIM_DRAGON_HEAD =
            monster("sim_dragonehead", AssimilatedDragonHeadEntity::new, 1.75F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedDragonHeadEntity>> SIM_DRAGON_HEAD_COMPAT =
            monster("sim_dragonhead", AssimilatedDragonHeadEntity::new, 1.75F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedEndermanEntity>> SIM_ENDERMAN =
            monster("sim_enderman", AssimilatedEndermanEntity::new, 0.6F, 2.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedHeadEntity>> SIM_ENDERMAN_HEAD =
            monster("sim_endermanhead", (type, level) -> new AssimilatedHeadEntity(type, level,
                    AssimilatedHeadEntity.Kind.ENDERMAN), 0.7F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedVariantEntity>> SIM_HORSE =
            monster("sim_horse", (type, level) -> new AssimilatedVariantEntity(type, level,
                    AssimilatedVariantEntity.Kind.HORSE), 1.3964844F, 1.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedHeadEntity>> SIM_HORSE_HEAD =
            monster("sim_horsehead", (type, level) -> new AssimilatedHeadEntity(type, level,
                    AssimilatedHeadEntity.Kind.HORSE), 0.7F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<SimHumanEntity>> SIM_HUMAN =
            monster("sim_human", SimHumanEntity::new, 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedHeadEntity>> SIM_HUMAN_HEAD =
            monster("sim_humanhead", (type, level) -> new AssimilatedHeadEntity(type, level,
                    AssimilatedHeadEntity.Kind.HUMAN), 0.7F, 0.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedHeadEntity>> SIM_COW_HEAD =
            monster("sim_cowhead", (type, level) -> new AssimilatedHeadEntity(type, level,
                    AssimilatedHeadEntity.Kind.COW), 0.7F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedHeadEntity>> SIM_PIG_HEAD =
            monster("sim_pighead", (type, level) -> new AssimilatedHeadEntity(type, level,
                    AssimilatedHeadEntity.Kind.PIG), 0.7F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedHeadEntity>> SIM_SHEEP_HEAD =
            monster("sim_sheephead", (type, level) -> new AssimilatedHeadEntity(type, level,
                    AssimilatedHeadEntity.Kind.SHEEP), 0.7F, 0.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedVariantEntity>> SIM_VILLAGER =
            monster("sim_villager", (type, level) -> new AssimilatedVariantEntity(type, level,
                    AssimilatedVariantEntity.Kind.VILLAGER), 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedHeadEntity>> SIM_VILLAGER_HEAD =
            monster("sim_villagerhead", (type, level) -> new AssimilatedHeadEntity(type, level,
                    AssimilatedHeadEntity.Kind.VILLAGER), 0.7F, 0.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedHeadEntity>> SIM_WOLF_HEAD =
            monster("sim_wolfhead", (type, level) -> new AssimilatedHeadEntity(type, level,
                    AssimilatedHeadEntity.Kind.WOLF), 0.7F, 0.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<FeralParasiteEntity>> FER_BEAR =
            monster("fer_bear", (type, level) -> new FeralParasiteEntity(type, level,
                    FeralParasiteEntity.Kind.BEAR), 1.3F, 1.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<FeralParasiteEntity>> FER_COW =
            monster("fer_cow", (type, level) -> new FeralParasiteEntity(type, level,
                    FeralParasiteEntity.Kind.COW), 0.9F, 1.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<FeralParasiteEntity>> FER_HORSE =
            monster("fer_horse", (type, level) -> new FeralParasiteEntity(type, level,
                    FeralParasiteEntity.Kind.HORSE), 1.3964844F, 1.75F);
    public static final DeferredHolder<EntityType<?>, EntityType<FeralParasiteEntity>> FER_HUMAN =
            monster("fer_human", (type, level) -> new FeralParasiteEntity(type, level,
                    FeralParasiteEntity.Kind.HUMAN), 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<FeralParasiteEntity>> FER_PIG =
            monster("fer_pig", (type, level) -> new FeralParasiteEntity(type, level,
                    FeralParasiteEntity.Kind.PIG), 0.9F, 0.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<FeralParasiteEntity>> FER_SHEEP =
            monster("fer_sheep", (type, level) -> new FeralParasiteEntity(type, level,
                    FeralParasiteEntity.Kind.SHEEP), 0.9F, 1.3F);
    public static final DeferredHolder<EntityType<?>, EntityType<FeralParasiteEntity>> FER_VILLAGER =
            monster("fer_villager", (type, level) -> new FeralParasiteEntity(type, level,
                    FeralParasiteEntity.Kind.VILLAGER), 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<FeralParasiteEntity>> FER_WOLF =
            monster("fer_wolf", (type, level) -> new FeralParasiteEntity(type, level,
                    FeralParasiteEntity.Kind.WOLF), 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<FeralEndermanEntity>> FER_ENDERMAN =
            monster("fer_enderman", FeralEndermanEntity::new, 0.6F, 2.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<HiBlazeEntity>> HI_BLAZE =
            monster("hi_blaze", HiBlazeEntity::new, 0.6F, 0.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<HiGolemEntity>> HI_GOLEM =
            monster("hi_golem", HiGolemEntity::new, 1.1F, 2.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<HiSkeletonEntity>> HI_SKELETON =
            monster("hi_skeleton", HiSkeletonEntity::new, 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<MarauderizedBearEntity>> MAR_BEAR =
            monster("mar_bear", MarauderizedBearEntity::new, 1.3F, 1.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<MarauderizedCowEntity>> MAR_COW =
            monster("mar_cow", MarauderizedCowEntity::new, 0.9F, 1.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<MarauderizedEndermanEntity>> MAR_ENDERMAN =
            monster("mar_enderman", MarauderizedEndermanEntity::new, 0.6F, 2.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<MarauderizedHumanEntity>> MAR_HUMAN =
            monster("mar_human", MarauderizedHumanEntity::new, 0.6F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<MarauderizedSheepEntity>> MAR_SHEEP =
            monster("mar_sheep", MarauderizedSheepEntity::new, 0.7566F, 2.85F);
    public static final DeferredHolder<EntityType<?>, EntityType<MarauderizedVillagerEntity>> MAR_VILLAGER =
            monster("mar_villager", MarauderizedVillagerEntity::new, 0.6F, 2.75F);
    public static final DeferredHolder<EntityType<?>, EntityType<MarauderEntity>> MARAUDER =
            monster("marauder", MarauderEntity::new, 0.901F, 4.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<MarauderTendrilEntity>> MARAUDER_TENDRIL =
            monster("marauder_tendril", MarauderTendrilEntity::new, 0.6F, 2.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<DeterrentParasiteEntity>> DISPATCHERTEN =
            monster("dispatcherten", (type, level) -> new DeterrentParasiteEntity(type, level,
                    DeterrentParasiteEntity.Kind.DISPATCHER_TENTACLE), 0.7F, 2.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<DeterrentParasiteEntity>> KYPHOSIS =
            monster("kyphosis", (type, level) -> new DeterrentParasiteEntity(type, level,
                    DeterrentParasiteEntity.Kind.KYPHOSIS), 0.7F, 4.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<DeterrentParasiteEntity>> SEIZER =
            monster("seizer", (type, level) -> new DeterrentParasiteEntity(type, level,
                    DeterrentParasiteEntity.Kind.SEIZER), 0.7F, 2.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<DeterrentParasiteEntity>> SENTRY =
            monster("sentry", (type, level) -> new DeterrentParasiteEntity(type, level,
                    DeterrentParasiteEntity.Kind.SENTRY), 0.7F, 4.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<DeterrentParasiteEntity>> WORM =
            monster("worm", (type, level) -> new DeterrentParasiteEntity(type, level,
                    DeterrentParasiteEntity.Kind.WORM), 1.5F, 4.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<PureParasiteEntity>> GRUNT =
            monster("grunt", (type, level) -> new PureParasiteEntity(type, level,
                    PureParasiteEntity.Kind.GRUNT), 0.7666F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<PureParasiteEntity>> BOMBER_LIGHT =
            monster("bomber_light", (type, level) -> new PureParasiteEntity(type, level,
                    PureParasiteEntity.Kind.BOMBER_LIGHT), 1.7F, 2.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<PureParasiteEntity>> MONARCH =
            monster("monarch", (type, level) -> new PureParasiteEntity(type, level,
                    PureParasiteEntity.Kind.MONARCH), 1.901F, 4.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<PureParasiteEntity>> OVERSEER =
            monster("overseer", (type, level) -> new PureParasiteEntity(type, level,
                    PureParasiteEntity.Kind.OVERSEER), 1.9F, 2.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<PureParasiteEntity>> SEEKER =
            monster("seeker", (type, level) -> new PureParasiteEntity(type, level,
                    PureParasiteEntity.Kind.SEEKER), 1.9F, 2.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<PureParasiteEntity>> VIGILANTE =
            monster("vigilante", (type, level) -> new PureParasiteEntity(type, level,
                    PureParasiteEntity.Kind.VIGILANTE), 1.6F, 3.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<PureParasiteEntity>> WARDEN =
            monster("warden", (type, level) -> new PureParasiteEntity(type, level,
                    PureParasiteEntity.Kind.WARDEN), 0.901F, 4.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<PreeminentParasiteEntity>> BOGLE =
            monster("bogle", (type, level) -> new PreeminentParasiteEntity(type, level,
                    PreeminentParasiteEntity.Kind.BOGLE), 4.0F, 4.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<PreeminentParasiteEntity>> CARRIER_COLONY =
            monster("carrier_colony", (type, level) -> new PreeminentParasiteEntity(type, level,
                    PreeminentParasiteEntity.Kind.CARRIER_COLONY), 1.75F, 3.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<PreeminentParasiteEntity>> HAUNTER =
            monster("haunter", (type, level) -> new PreeminentParasiteEntity(type, level,
                    PreeminentParasiteEntity.Kind.HAUNTER), 2.0F, 3.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<PreeminentParasiteEntity>> BOMBER_HEAVY =
            monster("bomber_heavy", (type, level) -> new PreeminentParasiteEntity(type, level,
                    PreeminentParasiteEntity.Kind.BOMBER_HEAVY), 3.7F, 4.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<PreeminentParasiteEntity>> WRAITH =
            monster("wraith", (type, level) -> new PreeminentParasiteEntity(type, level,
                    PreeminentParasiteEntity.Kind.WRAITH), 4.0F, 4.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<FlamEntity>> SUCCOR =
            monster("succor", FlamEntity::new, 1.2F, 1.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<AncientParasiteEntity>> ANC_DREADNAUT =
            monster("anc_dreadnaut", (type, level) -> new AncientParasiteEntity(type, level,
                    AncientParasiteEntity.Kind.DREADNAUT), 4.0F, 4.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<AncientParasiteEntity>> ANC_OVERLORD =
            monster("anc_overlord", (type, level) -> new AncientParasiteEntity(type, level,
                    AncientParasiteEntity.Kind.OVERLORD), 2.4F, 2.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<WorkerEntity>> WORKER =
            monster("worker", WorkerEntity::new, 0.65F, 0.65F);
    public static final DeferredHolder<EntityType<?>, EntityType<ArchitectEntity>> ARCHITECT =
            monster("architect", ArchitectEntity::new, 1.9F, 2.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<AncientPodEntity>> ANC_POD =
            monster("anc_pod", AncientPodEntity::new, 1.0F, 2.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<DreadnautTentacleEntity>> ANC_DREADNAUT_TEN =
            monster("anc_dreadnaut_ten", DreadnautTentacleEntity::new, 1.0F, 0.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> BECKON_SI =
            monster("beckon_si", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.BECKON_SI), 0.5F, 1.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> BECKON_SII =
            monster("beckon_sii", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.BECKON_SII), 0.6F, 2.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> BECKON_SIII =
            monster("beckon_siii", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.BECKON_SIII), 0.7F, 5.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> BECKON_SIV =
            monster("beckon_siv", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.BECKON_SIV), 0.8F, 6.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> DISPATCHER_SI =
            monster("dispatcher_si", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.DISPATCHER_SI), 2.7F, 2.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> DISPATCHER_SII =
            monster("dispatcher_sii", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.DISPATCHER_SII), 3.2F, 3.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> DISPATCHER_SIII =
            monster("dispatcher_siii", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.DISPATCHER_SIII), 3.9F, 4.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> DISPATCHER_SIV =
            monster("dispatcher_siv", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.DISPATCHER_SIV), 4.7F, 5.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> ROOTER_SI =
            monster("rooter_si", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.ROOTER_SI), 1.2F, 2.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> ROOTER_SII =
            monster("rooter_sii", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.ROOTER_SII), 1.2F, 5.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> ROOTER_SIII =
            monster("rooter_siii", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.ROOTER_SIII), 1.5F, 5.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> ROOTER_SIV =
            monster("rooter_siv", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.ROOTER_SIV), 1.7F, 5.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<NexusParasiteEntity>> ROOTERBALL =
            monster("rooterball", (type, level) -> new NexusParasiteEntity(type, level,
                    NexusParasiteEntity.Kind.ROOTERBALL), 1.4F, 1.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<AbominationEntity>> ABO_BODIES =
            monster("abo_bodies", (type, level) -> new AbominationEntity(type, level,
                    AbominationEntity.Kind.BODIES), 1.95154F, 2.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<AbominationEntity>> ABO_HEAD =
            monster("abo_head", (type, level) -> new AbominationEntity(type, level,
                    AbominationEntity.Kind.HEAD), 1.954F, 2.73F);
    public static final DeferredHolder<EntityType<?>, EntityType<PullingBallEntity>> PULLING_BALL =
            ENTITIES.register("pullingball", () -> EntityType.Builder
                    .<PullingBallEntity>of(PullingBallEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "pullingball").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<PullingBallEntity>> PULLING_BALL_LEGACY =
            ENTITIES.register("pulling_ball", () -> EntityType.Builder
                    .<PullingBallEntity>of(PullingBallEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "pulling_ball").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<CruxThrownBlockDamageEntity>> CRUX_BLOCK_DAMAGE =
            ENTITIES.register("crux_block_damage", () -> EntityType.Builder
                    .<CruxThrownBlockDamageEntity>of(CruxThrownBlockDamageEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(0).updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "crux_block_damage").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<HaunterDamageEntity>> HAUNTER_DAMAGE =
            ENTITIES.register("haunter_damage", () -> EntityType.Builder
                    .<HaunterDamageEntity>of(HaunterDamageEntity::new, MobCategory.MISC)
                    .sized(1.2F, 0.9F).clientTrackingRange(0).updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "haunter_damage").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ScaryOrbEntity>> SCARY_ORB =
            ENTITIES.register("orbscary", () -> EntityType.Builder.<ScaryOrbEntity>of(ScaryOrbEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "orbscary").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ScaryOrbEntity>> SCARY_ORB_LEGACY =
            ENTITIES.register("scary_orb", () -> EntityType.Builder.<ScaryOrbEntity>of(ScaryOrbEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "scary_orb").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<VoidOrbEntity>> VOID_ORB =
            ENTITIES.register("orbvoid", () -> EntityType.Builder.<VoidOrbEntity>of(VoidOrbEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "orbvoid").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiticScentEntity>> SCENT =
            ENTITIES.register("scent", () -> EntityType.Builder
                    .<ParasiticScentEntity>of(ParasiticScentEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "scent").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> PARASITE_PROJECTILE =
            ENTITIES.register("parasite_projectile", () -> EntityType.Builder
                    .<ParasiteProjectileEntity>of(ParasiteProjectileEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(8).updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "parasite_projectile").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> WEB_BALL =
            projectile("webball", ParasiteProjectileEntity.Mode.WEB, 0.3F, 0.3F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> SPINE_BALL =
            projectile("spineball", ParasiteProjectileEntity.Mode.SPINE, 0.3F, 0.3F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> NADE_BALL =
            projectile("nadeball", ParasiteProjectileEntity.Mode.ELVIA_NADE, 0.3F, 0.3F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> BALL_TALL =
            projectile("balltall", ParasiteProjectileEntity.Mode.ELVIA_BALL, 0.3F, 0.3F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> BALL_MALL =
            projectile("ballmall", ParasiteProjectileEntity.Mode.LENCIA_BALL, 0.3F, 0.3F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> HEBLU_LIGHT =
            projectile("heblu_light", ParasiteProjectileEntity.Mode.LIGHT, 0.65F, 0.65F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> METEOR =
            projectile("meteor", ParasiteProjectileEntity.Mode.METEOR, 4.5F, 4.5F, 16, 1);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> SALIVA_BALL =
            projectile("salivaball", ParasiteProjectileEntity.Mode.ALAFHA_BALL, 0.3F, 0.3F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> BALL_BALL =
            projectile("ballball", ParasiteProjectileEntity.Mode.ANGED_BALL, 0.3F, 0.3F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> ANCIENT_BALL =
            projectile("ancientball", ParasiteProjectileEntity.Mode.ANCIENT_BALL, 0.3F, 0.3F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> MISSILE =
            projectile("missile", ParasiteProjectileEntity.Mode.DRAGON_MISSILE, 0.3F, 0.3F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> SALIVA_EFFECT =
            projectile("salivaeff", ParasiteProjectileEntity.Mode.SALIVA_EFFECT, 0.3F, 0.3F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> BIOMASS_BALL =
            projectile("biomassball", ParasiteProjectileEntity.Mode.BIOMASS_BALL, 0.3F, 0.3F, 4, 3);
    public static final DeferredHolder<EntityType<?>, EntityType<AntiInfestedBlockEntity>> ANTI_INFESTED_BLOCK =
            ENTITIES.register("antiinfestedblock", () -> EntityType.Builder
                    .<AntiInfestedBlockEntity>of(AntiInfestedBlockEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "antiinfestedblock").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<OrbBoomEntity>> ORB_BOOM =
            ENTITIES.register("orbboom", () -> EntityType.Builder
                    .<OrbBoomEntity>of(OrbBoomEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(16).updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "orbboom").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<SourceEntity>> SOURCE =
            ENTITIES.register("source", () -> EntityType.Builder
                    .<SourceEntity>of(SourceEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "source").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<RemainEntity>> REMAIN =
            ENTITIES.register("remain", () -> EntityType.Builder
                    .<RemainEntity>of(RemainEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "remain").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<BombEntity>> BOMB =
            ENTITIES.register("bomb", () -> EntityType.Builder
                    .<BombEntity>of(BombEntity::new, MobCategory.MISC)
                    .sized(0.68F, 0.68F).clientTrackingRange(16).updateInterval(1).fireImmune()
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "bomb").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<ToxicCloudEntity>> CLOUD_TOXIC =
            ENTITIES.register("cloudtoxic", () -> EntityType.Builder
                    .<ToxicCloudEntity>of(ToxicCloudEntity::new, MobCategory.MISC)
                    .sized(6.0F, 0.5F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "cloudtoxic").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<GoreEntity>> GORE =
            ENTITIES.register("gore", () -> EntityType.Builder
                    .<GoreEntity>of(GoreEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F).clientTrackingRange(4).updateInterval(3).fireImmune()
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "gore").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<TendrilEntity>> TENDRIL =
            ENTITIES.register("tendril", () -> EntityType.Builder
                    .<TendrilEntity>of(TendrilEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "tendril").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<WaveEntity>> WAVE =
            ENTITIES.register("wave", () -> EntityType.Builder
                    .<WaveEntity>of(WaveEntity::new, MobCategory.MISC)
                    .sized(1.5F, 0.2F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "wave").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<NadeEntity>> NADE =
            ENTITIES.register("nade", () -> EntityType.Builder
                    .<NadeEntity>of(NadeEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(3)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "nade").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<HaunterHomingProjectileEntity>> HAUNTER_HOMING =
            ENTITIES.register("homming", () -> EntityType.Builder
                    .<HaunterHomingProjectileEntity>of(HaunterHomingProjectileEntity::new, MobCategory.MISC)
                    .sized(0.3125F, 0.3125F).clientTrackingRange(4).updateInterval(3)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "homming").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<HaunterHomingProjectileEntity>> HAUNTER_HOMING_LEGACY =
            ENTITIES.register("haunter_homing", () -> EntityType.Builder
                    .<HaunterHomingProjectileEntity>of(HaunterHomingProjectileEntity::new, MobCategory.MISC)
                    .sized(0.3125F, 0.3125F).clientTrackingRange(4).updateInterval(3)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "haunter_homing").toString()));

    private static <T extends net.minecraft.world.entity.Mob> DeferredHolder<EntityType<?>, EntityType<T>> monster(
            String id, EntityType.EntityFactory<T> factory, float width, float height) {
        return ENTITIES.register(id, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height).clientTrackingRange(8)
                .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, id).toString()));
    }

    private static DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> projectile(
            String id, ParasiteProjectileEntity.Mode mode, float width, float height,
            int trackingRange, int updateInterval) {
        return ENTITIES.register(id, () -> EntityType.Builder
                .<ParasiteProjectileEntity>of((type, level) -> new ParasiteProjectileEntity(type, level, mode),
                        MobCategory.MISC)
                .sized(width, height).clientTrackingRange(trackingRange).updateInterval(updateInterval)
                .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, id).toString()));
    }

    public static EntityType<ParasiteProjectileEntity> projectileType(ParasiteProjectileEntity.Mode mode) {
        return switch (mode) {
            case WEB -> WEB_BALL.get();
            case SPINE, YELLOWEYE_SPINE -> SPINE_BALL.get();
            case ELVIA_NADE, YELLOWEYE_NADE, ACID -> NADE_BALL.get();
            case ELVIA_BALL -> BALL_TALL.get();
            case LENCIA_BALL -> BALL_MALL.get();
            case LIGHT -> HEBLU_LIGHT.get();
            case HOMING -> PARASITE_PROJECTILE.get();
            case METEOR -> METEOR.get();
            case ALAFHA_BALL -> SALIVA_BALL.get();
            case ANGED_BALL -> BALL_BALL.get();
            case ANCIENT_BALL -> ANCIENT_BALL.get();
            case DRAGON_MISSILE -> MISSILE.get();
            case SALIVA_EFFECT -> SALIVA_EFFECT.get();
            case BIOMASS_BALL -> BIOMASS_BALL.get();
            default -> PARASITE_PROJECTILE.get();
        };
    }

    public static ParasiteProjectileEntity createProjectile(Level level, ParasiteProjectileEntity.Mode mode) {
        return projectileType(mode).create(level);
    }

    private static <T extends net.minecraft.world.entity.Mob> DeferredHolder<EntityType<?>, EntityType<T>> monster(
            String id, EntityType.EntityFactory<T> factory, float width, float height, float eyeHeight) {
        return ENTITIES.register(id, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height).eyeHeight(eyeHeight).clientTrackingRange(8)
                .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, id).toString()));
    }

    private ModEntities() {
    }
}
