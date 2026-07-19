package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.entity.AirscrewEntity;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.AssimilatedParasiteEntity;
import alku.csrp.entity.AssimilatedDragonEntity;
import alku.csrp.entity.AssimilatedDragonHeadEntity;
import alku.csrp.entity.AssimilatedEndermanEntity;
import alku.csrp.entity.AssimilatedHeadEntity;
import alku.csrp.entity.AssimilatedVariantEntity;
import alku.csrp.entity.BuglinEntity;
import alku.csrp.entity.CarrierFlyingEntity;
import alku.csrp.entity.CarrierHeavyEntity;
import alku.csrp.entity.CarrierLightEntity;
import alku.csrp.entity.CruxEntity;
import alku.csrp.entity.CruxThrownBlockDamageEntity;
import alku.csrp.entity.DredgeEntity;
import alku.csrp.entity.DeterrentParasiteEntity;
import alku.csrp.entity.FeralEndermanEntity;
import alku.csrp.entity.FeralParasiteEntity;
import alku.csrp.entity.DraconiteEntity;
import alku.csrp.entity.GnatEntity;
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
import alku.csrp.entity.PrimitiveVariantEntity;
import alku.csrp.entity.PureParasiteEntity;
import alku.csrp.entity.PullingBallEntity;
import alku.csrp.entity.RupterEntity;
import alku.csrp.entity.ScaryOrbEntity;
import alku.csrp.entity.SimAdventurerEntity;
import alku.csrp.entity.SimAdventurerHeadEntity;
import alku.csrp.entity.SummonerEntity;
import alku.csrp.entity.ThrallEntity;
import alku.csrp.entity.VerminEntity;
import alku.csrp.entity.VisceraEntity;
import alku.csrp.entity.MovingFleshEntity;
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
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_ARACHNIDA =
            monster("pri_arachnida", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.ARACHNIDA), 0.9F, 2.7F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_BOLSTER =
            monster("pri_bolster", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.BOLSTER), 0.9F, 2.9F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_BURROWER =
            monster("pri_burrower", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.BURROWER), 1.0F, 0.25F);
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
                    PrimitiveVariantEntity.Kind.TOZOON), 0.978F, 1.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<PrimitiveVariantEntity>> PRI_YELLOWEYE =
            monster("pri_yelloweye", (type, level) -> new PrimitiveVariantEntity(type, level,
                    PrimitiveVariantEntity.Kind.YELLOWEYE), 0.4F, 1.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_ARACHNIDA =
            monster("ada_arachnida", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.ARACHNIDA), 1.901F, 2.85F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_BOLSTER =
            monster("ada_bolster", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.BOLSTER), 1.3F, 3.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_BURROWER =
            monster("ada_burrower", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.BURROWER), 1.321F, 1.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_DEVOURER =
            monster("ada_devourer", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.DEVOURER), 0.901F, 3.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_LONGARMS =
            monster("ada_longarms", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.LONGARMS), 1.3F, 3.3F);
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
                    AdaptedVariantEntity.Kind.TOZOON), 1.321F, 1.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_VERMIN =
            monster("ada_vermin", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.VERMIN), 1.511F, 3.655F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_VISCERA =
            monster("ada_viscera", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.VISCERA), 1.1F, 1.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<AdaptedVariantEntity>> ADA_YELLOWEYE =
            monster("ada_yelloweye", (type, level) -> new AdaptedVariantEntity(type, level,
                    AdaptedVariantEntity.Kind.YELLOWEYE), 1.3F, 2.9F);
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
    public static final DeferredHolder<EntityType<?>, EntityType<LiceEntity>> LICE =
            monster("lice", LiceEntity::new, 0.85F, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<ManglerEntity>> MANGLER =
            monster("mangler", ManglerEntity::new, 1.0F, 1.0F);
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
            monster("kirin", KirinEntity::new, 2.1271334F, 8.85F);
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
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedDragonHeadEntity>> SIM_DRAGON_HEAD =
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
    public static final DeferredHolder<EntityType<?>, EntityType<AssimilatedVariantEntity>> SIM_HUMAN =
            monster("sim_human", (type, level) -> new AssimilatedVariantEntity(type, level,
                    AssimilatedVariantEntity.Kind.HUMAN), 0.6F, 1.95F);
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
    public static final DeferredHolder<EntityType<?>, EntityType<PureParasiteEntity>> VIGILANTE =
            monster("vigilante", (type, level) -> new PureParasiteEntity(type, level,
                    PureParasiteEntity.Kind.VIGILANTE), 1.6F, 3.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<PureParasiteEntity>> WARDEN =
            monster("warden", (type, level) -> new PureParasiteEntity(type, level,
                    PureParasiteEntity.Kind.WARDEN), 0.901F, 4.2F);
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
    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteProjectileEntity>> PARASITE_PROJECTILE =
            ENTITIES.register("parasite_projectile", () -> EntityType.Builder
                    .<ParasiteProjectileEntity>of(ParasiteProjectileEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F).clientTrackingRange(8).updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "parasite_projectile").toString()));

    private static <T extends net.minecraft.world.entity.Mob> DeferredHolder<EntityType<?>, EntityType<T>> monster(
            String id, EntityType.EntityFactory<T> factory, float width, float height) {
        return ENTITIES.register(id, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height).clientTrackingRange(8)
                .build(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, id).toString()));
    }

    private ModEntities() {
    }
}
