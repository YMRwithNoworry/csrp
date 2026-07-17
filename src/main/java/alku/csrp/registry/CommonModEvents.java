package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.entity.AirscrewEntity;
import alku.csrp.entity.BuglinEntity;
import alku.csrp.entity.CarrierFlyingEntity;
import alku.csrp.entity.CarrierHeavyEntity;
import alku.csrp.entity.CarrierLightEntity;
import alku.csrp.entity.CruxEntity;
import alku.csrp.entity.DredgeEntity;
import alku.csrp.entity.DraconiteEntity;
import alku.csrp.entity.GnatEntity;
import alku.csrp.entity.HeedEntity;
import alku.csrp.entity.HostEntity;
import alku.csrp.entity.HostIIEntity;
import alku.csrp.entity.IncompleteCruxEntity;
import alku.csrp.entity.IncompleteFormMediumEntity;
import alku.csrp.entity.IncompleteFormSmallEntity;
import alku.csrp.entity.KirinEntity;
import alku.csrp.entity.LiceEntity;
import alku.csrp.entity.LongarmsEntity;
import alku.csrp.entity.ManglerEntity;
import alku.csrp.entity.RupterEntity;
import alku.csrp.entity.SummonerEntity;
import alku.csrp.entity.ThrallEntity;
import alku.csrp.entity.VerminEntity;
import alku.csrp.entity.VisceraEntity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = Csrp.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class CommonModEvents {
    private CommonModEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BUGLIN.get(), BuglinEntity.createAttributes().build());
        event.put(ModEntities.RUPTER.get(), RupterEntity.createAttributes().build());
        event.put(ModEntities.PRI_LONGARMS.get(), LongarmsEntity.createAttributes().build());
        event.put(ModEntities.PRI_SUMMONER.get(), SummonerEntity.createAttributes().build());
        event.put(ModEntities.PRI_VERMIN.get(), VerminEntity.createAttributes().build());
        event.put(ModEntities.PRI_VISCERA.get(), VisceraEntity.createAttributes().build());
        event.put(ModEntities.GNAT.get(), GnatEntity.createAttributes().build());
        event.put(ModEntities.CARRIER_HEAVY.get(), CarrierHeavyEntity.createAttributes().build());
        event.put(ModEntities.CARRIER_LIGHT.get(), CarrierLightEntity.createAttributes().build());
        event.put(ModEntities.CARRIER_FLYING.get(), CarrierFlyingEntity.createAttributes().build());
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
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.RUPTER.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                RupterEntity::checkRupterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
