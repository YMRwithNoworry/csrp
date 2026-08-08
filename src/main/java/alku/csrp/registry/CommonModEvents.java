package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.entity.AbominationEntity;
import alku.csrp.entity.AirscrewEntity;
import alku.csrp.entity.AncientParasiteEntity;
import alku.csrp.entity.WorkerEntity;
import alku.csrp.entity.ArchitectEntity;
import alku.csrp.entity.AncientPodEntity;
import alku.csrp.entity.DreadnautTentacleEntity;
import alku.csrp.entity.AdaptedVariantEntity;
import alku.csrp.entity.AssimilatedParasiteEntity;
import alku.csrp.entity.AssimilatedDragonEntity;
import alku.csrp.entity.AssimilatedDragonHeadEntity;
import alku.csrp.entity.AssimilatedEndermanEntity;
import alku.csrp.entity.AssimilatedHeadEntity;
import alku.csrp.entity.AssimilatedVariantEntity;
import alku.csrp.entity.BiomassEntity;
import alku.csrp.entity.BuglinEntity;
import alku.csrp.entity.CarrierFlyingEntity;
import alku.csrp.entity.CarrierHeavyEntity;
import alku.csrp.entity.CarrierLightEntity;
import alku.csrp.entity.CarrierWormEntity;
import alku.csrp.entity.CruxEntity;
import alku.csrp.entity.DredgeEntity;
import alku.csrp.entity.DeterrentParasiteEntity;
import alku.csrp.entity.FeralEndermanEntity;
import alku.csrp.entity.FeralParasiteEntity;
import alku.csrp.entity.FlamEntity;
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
import alku.csrp.entity.MovingFleshEntity;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.PrimitiveVariantEntity;
import alku.csrp.entity.PreeminentParasiteEntity;
import alku.csrp.entity.PureParasiteEntity;
import alku.csrp.entity.RupterEntity;
import alku.csrp.entity.SimAdventurerEntity;
import alku.csrp.entity.SimAdventurerHeadEntity;
import alku.csrp.entity.SummonerEntity;
import alku.csrp.entity.ThrallEntity;
import alku.csrp.entity.VerminEntity;
import alku.csrp.entity.VisceraEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class CommonModEvents {
    private CommonModEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BIOMASS.get(), BiomassEntity.createAttributes().build());
        event.put(ModEntities.BUGLIN.get(), BuglinEntity.createAttributes().build());
        event.put(ModEntities.RUPTER.get(), RupterEntity.createAttributes().build());
        event.put(ModEntities.PRI_LONGARMS.get(), LongarmsEntity.createAttributes().build());
        event.put(ModEntities.PRI_SUMMONER.get(), SummonerEntity.createAttributes().build());
        event.put(ModEntities.PRI_VERMIN.get(), VerminEntity.createAttributes().build());
        event.put(ModEntities.PRI_VISCERA.get(), VisceraEntity.createAttributes().build());
        event.put(ModEntities.PRI_ARACHNIDA.get(), PrimitiveVariantEntity.createAttributes(
                PrimitiveVariantEntity.Kind.ARACHNIDA).build());
        event.put(ModEntities.PRI_BOLSTER.get(), PrimitiveVariantEntity.createAttributes(
                PrimitiveVariantEntity.Kind.BOLSTER).build());
        event.put(ModEntities.PRI_BURROWER.get(), PrimitiveVariantEntity.createAttributes(
                PrimitiveVariantEntity.Kind.BURROWER).build());
        event.put(ModEntities.PRI_DEVOURER.get(), PrimitiveVariantEntity.createAttributes(
                PrimitiveVariantEntity.Kind.DEVOURER).build());
        event.put(ModEntities.PRI_MANDUCATER.get(), PrimitiveVariantEntity.createAttributes(
                PrimitiveVariantEntity.Kind.MANDUCATER).build());
        event.put(ModEntities.PRI_REEKER.get(), PrimitiveVariantEntity.createAttributes(
                PrimitiveVariantEntity.Kind.REEKER).build());
        event.put(ModEntities.PRI_TOZOON.get(), PrimitiveVariantEntity.createAttributes(
                PrimitiveVariantEntity.Kind.TOZOON).build());
        event.put(ModEntities.PRI_YELLOWEYE.get(), PrimitiveVariantEntity.createAttributes(
                PrimitiveVariantEntity.Kind.YELLOWEYE).build());
        event.put(ModEntities.ADA_ARACHNIDA.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.ARACHNIDA).build());
        event.put(ModEntities.ADA_BOLSTER.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.BOLSTER).build());
        event.put(ModEntities.ADA_BURROWER.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.BURROWER).build());
        event.put(ModEntities.ADA_DEVOURER.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.DEVOURER).build());
        event.put(ModEntities.ADA_LONGARMS.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.LONGARMS).build());
        event.put(ModEntities.ADA_MANDUCATER.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.MANDUCATER).build());
        event.put(ModEntities.ADA_REEKER.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.REEKER).build());
        event.put(ModEntities.ADA_SUMMONER.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.SUMMONER).build());
        event.put(ModEntities.ADA_TOZOON.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.TOZOON).build());
        event.put(ModEntities.ADA_VERMIN.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.VERMIN).build());
        event.put(ModEntities.ADA_VISCERA.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.VISCERA).build());
        event.put(ModEntities.ADA_YELLOWEYE.get(), AdaptedVariantEntity.createAttributes(
                AdaptedVariantEntity.Kind.YELLOWEYE).build());
        event.put(ModEntities.GNAT.get(), GnatEntity.createAttributes().build());
        event.put(ModEntities.CARRIER_HEAVY.get(), CarrierHeavyEntity.createAttributes().build());
        event.put(ModEntities.CARRIER_LIGHT.get(), CarrierLightEntity.createAttributes().build());
        event.put(ModEntities.CARRIER_FLYING.get(), CarrierFlyingEntity.createAttributes().build());
        event.put(ModEntities.CARRIER_WORM.get(), CarrierWormEntity.createAttributes().build());
        event.put(ModEntities.CRUX.get(), CruxEntity.createAttributes().build());
        event.put(ModEntities.CRUX_INCOMPLETE.get(), IncompleteCruxEntity.createAttributes().build());
        event.put(ModEntities.AIRSCREW.get(), AirscrewEntity.createAttributes().build());
        event.put(ModEntities.HEED.get(), HeedEntity.createAttributes().build());
        event.put(ModEntities.DREDGE.get(), DredgeEntity.createAttributes().build());
        event.put(ModEntities.THRALL.get(), ThrallEntity.createAttributes().build());
        event.put(ModEntities.LICE.get(), LiceEntity.createAttributes().build());
        event.put(ModEntities.MANGLER.get(), ManglerEntity.createAttributes().build());
        event.put(ModEntities.HOST.get(), HostEntity.createAttributes().build());
        event.put(ModEntities.HOSTII.get(), HostIIEntity.createAttributes().build());
        event.put(ModEntities.INCOMPLETEFORM_SMALL.get(), IncompleteFormSmallEntity.createAttributes().build());
        event.put(ModEntities.INCOMPLETEFORM_MEDIUM.get(), IncompleteFormMediumEntity.createAttributes().build());
        event.put(ModEntities.DRACONITE.get(), DraconiteEntity.createAttributes().build());
        event.put(ModEntities.KIRIN.get(), KirinEntity.createAttributes().build());
        event.put(ModEntities.SIM_ADVENTURER.get(), SimAdventurerEntity.createAttributes().build());
        event.put(ModEntities.SIM_ADVENTURER_HEAD.get(), SimAdventurerHeadEntity.createAttributes().build());
        event.put(ModEntities.MOVINGFLESH.get(), MovingFleshEntity.createAttributes().build());
        event.put(ModEntities.SIM_BEAR.get(), AssimilatedParasiteEntity.createAttributes(
                AssimilatedParasiteEntity.Kind.BEAR).build());
        event.put(ModEntities.SIM_COW.get(), AssimilatedParasiteEntity.createAttributes(
                AssimilatedParasiteEntity.Kind.COW).build());
        event.put(ModEntities.SIM_PIG.get(), AssimilatedParasiteEntity.createAttributes(
                AssimilatedParasiteEntity.Kind.PIG).build());
        event.put(ModEntities.SIM_SHEEP.get(), AssimilatedParasiteEntity.createAttributes(
                AssimilatedParasiteEntity.Kind.SHEEP).build());
        event.put(ModEntities.SIM_WOLF.get(), AssimilatedParasiteEntity.createAttributes(
                AssimilatedParasiteEntity.Kind.WOLF).build());
        event.put(ModEntities.SIM_SQUID.get(), AssimilatedParasiteEntity.createAttributes(
                AssimilatedParasiteEntity.Kind.SQUID).build());
        event.put(ModEntities.SIM_BIGSPIDER.get(), AssimilatedVariantEntity.createAttributes(
                AssimilatedVariantEntity.Kind.BIGSPIDER).build());
        event.put(ModEntities.SIM_DRAGONE.get(), AssimilatedDragonEntity.createAttributes().build());
        event.put(ModEntities.SIM_DRAGON_HEAD.get(), AssimilatedDragonHeadEntity.createAttributes().build());
        event.put(ModEntities.SIM_DRAGON_HEAD_COMPAT.get(), AssimilatedDragonHeadEntity.createAttributes().build());
        event.put(ModEntities.SIM_ENDERMAN.get(), AssimilatedEndermanEntity.createAttributes().build());
        event.put(ModEntities.SIM_ENDERMAN_HEAD.get(), AssimilatedHeadEntity.createAttributes(
                AssimilatedHeadEntity.Kind.ENDERMAN).build());
        event.put(ModEntities.SIM_HORSE.get(), AssimilatedVariantEntity.createAttributes(
                AssimilatedVariantEntity.Kind.HORSE).build());
        event.put(ModEntities.SIM_HORSE_HEAD.get(), AssimilatedHeadEntity.createAttributes(
                AssimilatedHeadEntity.Kind.HORSE).build());
        event.put(ModEntities.SIM_HUMAN.get(), AssimilatedVariantEntity.createAttributes(
                AssimilatedVariantEntity.Kind.HUMAN).build());
        event.put(ModEntities.SIM_HUMAN_HEAD.get(), AssimilatedHeadEntity.createAttributes(
                AssimilatedHeadEntity.Kind.HUMAN).build());
        event.put(ModEntities.SIM_COW_HEAD.get(), AssimilatedHeadEntity.createAttributes(
                AssimilatedHeadEntity.Kind.COW).build());
        event.put(ModEntities.SIM_PIG_HEAD.get(), AssimilatedHeadEntity.createAttributes(
                AssimilatedHeadEntity.Kind.PIG).build());
        event.put(ModEntities.SIM_SHEEP_HEAD.get(), AssimilatedHeadEntity.createAttributes(
                AssimilatedHeadEntity.Kind.SHEEP).build());
        event.put(ModEntities.SIM_VILLAGER.get(), AssimilatedVariantEntity.createAttributes(
                AssimilatedVariantEntity.Kind.VILLAGER).build());
        event.put(ModEntities.SIM_VILLAGER_HEAD.get(), AssimilatedHeadEntity.createAttributes(
                AssimilatedHeadEntity.Kind.VILLAGER).build());
        event.put(ModEntities.SIM_WOLF_HEAD.get(), AssimilatedHeadEntity.createAttributes(
                AssimilatedHeadEntity.Kind.WOLF).build());
        event.put(ModEntities.FER_BEAR.get(), FeralParasiteEntity.createAttributes(FeralParasiteEntity.Kind.BEAR).build());
        event.put(ModEntities.FER_COW.get(), FeralParasiteEntity.createAttributes(FeralParasiteEntity.Kind.COW).build());
        event.put(ModEntities.FER_HORSE.get(), FeralParasiteEntity.createAttributes(FeralParasiteEntity.Kind.HORSE).build());
        event.put(ModEntities.FER_HUMAN.get(), FeralParasiteEntity.createAttributes(FeralParasiteEntity.Kind.HUMAN).build());
        event.put(ModEntities.FER_PIG.get(), FeralParasiteEntity.createAttributes(FeralParasiteEntity.Kind.PIG).build());
        event.put(ModEntities.FER_SHEEP.get(), FeralParasiteEntity.createAttributes(FeralParasiteEntity.Kind.SHEEP).build());
        event.put(ModEntities.FER_VILLAGER.get(), FeralParasiteEntity.createAttributes(FeralParasiteEntity.Kind.VILLAGER).build());
        event.put(ModEntities.FER_WOLF.get(), FeralParasiteEntity.createAttributes(FeralParasiteEntity.Kind.WOLF).build());
        event.put(ModEntities.FER_ENDERMAN.get(), FeralEndermanEntity.createAttributes(FeralParasiteEntity.Kind.ENDERMAN).build());
        event.put(ModEntities.HI_BLAZE.get(), HiBlazeEntity.createAttributes().build());
        event.put(ModEntities.HI_GOLEM.get(), HiGolemEntity.createAttributes().build());
        event.put(ModEntities.HI_SKELETON.get(), HiSkeletonEntity.createAttributes().build());
        event.put(ModEntities.MAR_BEAR.get(), MarauderizedBearEntity.createAttributes().build());
        event.put(ModEntities.MAR_COW.get(), MarauderizedCowEntity.createAttributes().build());
        event.put(ModEntities.MAR_ENDERMAN.get(), MarauderizedEndermanEntity.createAttributes().build());
        event.put(ModEntities.MAR_HUMAN.get(), MarauderizedHumanEntity.createAttributes().build());
        event.put(ModEntities.MAR_SHEEP.get(), MarauderizedSheepEntity.createAttributes().build());
        event.put(ModEntities.MAR_VILLAGER.get(), MarauderizedVillagerEntity.createAttributes().build());
        event.put(ModEntities.MARAUDER.get(), MarauderEntity.createAttributes().build());
        event.put(ModEntities.MARAUDER_TENDRIL.get(), MarauderTendrilEntity.createAttributes().build());
        event.put(ModEntities.DISPATCHERTEN.get(), DeterrentParasiteEntity.createAttributes(
                DeterrentParasiteEntity.Kind.DISPATCHER_TENTACLE).build());
        event.put(ModEntities.KYPHOSIS.get(), DeterrentParasiteEntity.createAttributes(
                DeterrentParasiteEntity.Kind.KYPHOSIS).build());
        event.put(ModEntities.SEIZER.get(), DeterrentParasiteEntity.createAttributes(
                DeterrentParasiteEntity.Kind.SEIZER).build());
        event.put(ModEntities.SENTRY.get(), DeterrentParasiteEntity.createAttributes(
                DeterrentParasiteEntity.Kind.SENTRY).build());
        event.put(ModEntities.WORM.get(), DeterrentParasiteEntity.createAttributes(
                DeterrentParasiteEntity.Kind.WORM).build());
        event.put(ModEntities.GRUNT.get(), PureParasiteEntity.createAttributes(
                PureParasiteEntity.Kind.GRUNT).build());
        event.put(ModEntities.BOMBER_LIGHT.get(), PureParasiteEntity.createAttributes(
                PureParasiteEntity.Kind.BOMBER_LIGHT).build());
        event.put(ModEntities.MONARCH.get(), PureParasiteEntity.createAttributes(
                PureParasiteEntity.Kind.MONARCH).build());
        event.put(ModEntities.OVERSEER.get(), PureParasiteEntity.createAttributes(
                PureParasiteEntity.Kind.OVERSEER).build());
        event.put(ModEntities.SEEKER.get(), PureParasiteEntity.createAttributes(
                PureParasiteEntity.Kind.SEEKER).build());
        event.put(ModEntities.VIGILANTE.get(), PureParasiteEntity.createAttributes(
                PureParasiteEntity.Kind.VIGILANTE).build());
        event.put(ModEntities.WARDEN.get(), PureParasiteEntity.createAttributes(
                PureParasiteEntity.Kind.WARDEN).build());
        event.put(ModEntities.BOGLE.get(), PreeminentParasiteEntity.createAttributes(
                PreeminentParasiteEntity.Kind.BOGLE).build());
        event.put(ModEntities.CARRIER_COLONY.get(), PreeminentParasiteEntity.createAttributes(
                PreeminentParasiteEntity.Kind.CARRIER_COLONY).build());
        event.put(ModEntities.HAUNTER.get(), PreeminentParasiteEntity.createAttributes(
                PreeminentParasiteEntity.Kind.HAUNTER).build());
        event.put(ModEntities.BOMBER_HEAVY.get(), PreeminentParasiteEntity.createAttributes(
                PreeminentParasiteEntity.Kind.BOMBER_HEAVY).build());
        event.put(ModEntities.WRAITH.get(), PreeminentParasiteEntity.createAttributes(
                PreeminentParasiteEntity.Kind.WRAITH).build());
        event.put(ModEntities.SUCCOR.get(), FlamEntity.createAttributes().build());
        event.put(ModEntities.ANC_DREADNAUT.get(), AncientParasiteEntity.createAttributes(
                AncientParasiteEntity.Kind.DREADNAUT).build());
        event.put(ModEntities.ANC_OVERLORD.get(), AncientParasiteEntity.createAttributes(
                AncientParasiteEntity.Kind.OVERLORD).build());
        event.put(ModEntities.WORKER.get(), WorkerEntity.createAttributes().build());
        event.put(ModEntities.ARCHITECT.get(), ArchitectEntity.createAttributes().build());
        event.put(ModEntities.ANC_POD.get(), AncientPodEntity.createAttributes().build());
        event.put(ModEntities.ANC_DREADNAUT_TEN.get(), DreadnautTentacleEntity.createAttributes().build());
        event.put(ModEntities.BECKON_SI.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.BECKON_SI).build());
        event.put(ModEntities.BECKON_SII.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.BECKON_SII).build());
        event.put(ModEntities.BECKON_SIII.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.BECKON_SIII).build());
        event.put(ModEntities.BECKON_SIV.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.BECKON_SIV).build());
        event.put(ModEntities.DISPATCHER_SI.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.DISPATCHER_SI).build());
        event.put(ModEntities.DISPATCHER_SII.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.DISPATCHER_SII).build());
        event.put(ModEntities.DISPATCHER_SIII.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.DISPATCHER_SIII).build());
        event.put(ModEntities.DISPATCHER_SIV.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.DISPATCHER_SIV).build());
        event.put(ModEntities.ROOTER_SI.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.ROOTER_SI).build());
        event.put(ModEntities.ROOTER_SII.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.ROOTER_SII).build());
        event.put(ModEntities.ROOTER_SIII.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.ROOTER_SIII).build());
        event.put(ModEntities.ROOTER_SIV.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.ROOTER_SIV).build());
        event.put(ModEntities.ROOTERBALL.get(), NexusParasiteEntity.createAttributes(
                NexusParasiteEntity.Kind.ROOTERBALL).build());
        event.put(ModEntities.ABO_BODIES.get(), AbominationEntity.createAttributes(
                AbominationEntity.Kind.BODIES).build());
        event.put(ModEntities.ABO_HEAD.get(), AbominationEntity.createAttributes(
                AbominationEntity.Kind.HEAD).build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.BUGLIN.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BuglinEntity::checkBuglinSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.RUPTER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                RupterEntity::checkRupterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.CARRIER_LIGHT.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CarrierLightEntity::checkCarrierLightSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.KIRIN.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                KirinEntity::checkKirinSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.FER_ENDERMAN.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                FeralEndermanEntity::checkFeralEndermanSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.ADA_BOLSTER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(
                ModEntities.SIM_ADVENTURER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void registerBrewing(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();
        Ingredient sponge = Ingredient.of(ModItems.DISEASED_SPONGE.get());
        builder.addRecipe(potionIngredient("water"), sponge,
                new ItemStack(ModItems.DEADBLOOD_FLUID.get()));
        builder.addRecipe(potionIngredient("awkward"), sponge,
                new ItemStack(ModItems.DEADBLOOD_FLUID.get()));
    }

    private static Ingredient potionIngredient(String potionName) {
        Holder<Potion> holder = potionName.equals("awkward") ? Potions.AWKWARD : Potions.WATER;
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(holder));
        return Ingredient.of(stack);
    }
}
