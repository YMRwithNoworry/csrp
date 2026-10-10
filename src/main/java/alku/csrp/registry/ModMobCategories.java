package alku.csrp.registry;

import net.minecraft.world.entity.MobCategory;

/** Separate hostile spawn pool; WorldConfig supplies its actual population limit. */
public final class ModMobCategories {
    // Keep the hostile flags and 128-block despawn distance of MONSTER.
    // NaturalSpawnStateMixin bypasses this category's vanilla global and local caps.
    public static final MobCategory PARASITE =
            MobCategory.create("CSRP_PARASITE", "csrp_parasite", 70, false, false, 128);

    private ModMobCategories() {
    }
}
