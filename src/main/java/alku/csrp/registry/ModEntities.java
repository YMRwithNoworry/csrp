package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.entity.BuglinEntity;
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

    private ModEntities() {
    }
}
