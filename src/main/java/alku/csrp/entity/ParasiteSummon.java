package alku.csrp.entity;

import alku.csrp.config.MobsConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Legacy {@code ParasiteSummon.spawnM}: a per-mob config string of the form
 * {@code <entity id>;<min>;<max>} summons that many mobs where the parasite died.
 *
 * <p>Only the mobs the original actually wires get a spec: the assimilated spider, cow (reused by
 * the bear, which has no key of its own), sheep, wolf and pig, plus the adventurer. The original
 * registers {@code infvillagermob} and {@code infhorsemob} but never reads them, so those two keys
 * stay unwired on purpose.
 */
public final class ParasiteSummon {
    private ParasiteSummon() {
    }

    /** Legacy {@code ParasiteSummon.spawnM(this, new String[]{spec}, 0, false, name)}. */
    public static void spawn(LivingEntity owner, String spec) {
        if (spec == null || spec.isBlank() || !(owner.level() instanceof ServerLevel level)) {
            return;
        }
        String[] parts = spec.split(";");
        if (parts.length < 3) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(parts[0]);
        if (id == null) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null) {
            return;
        }
        int min = parseInt(parts[1], 1);
        int max = Math.max(min, parseInt(parts[2], min));
        int count = min == max ? min : min + owner.getRandom().nextInt(max - min + 1);
        for (int i = 0; i < count; i++) {
            Entity spawned = type.create(level);
            if (spawned == null) {
                return;
            }
            spawned.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
            level.addFreshEntity(spawned);
        }
    }

    /** The legacy {@code SRPConfigMobs.*mob} spec of this parasite, or {@code null} when it has none. */
    public static String specFor(LivingEntity parasite) {
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(parasite.getType()).getPath();
        return switch (path) {
            case "sim_bigspider" -> MobsConfig.dorpaMobSummon();
            case "sim_cow", "sim_bear" -> MobsConfig.infcowMobSummon();
            case "sim_sheep" -> MobsConfig.infsheepMobSummon();
            case "sim_wolf" -> MobsConfig.infwolfMobSummon();
            case "sim_pig" -> MobsConfig.infpigMobSummon();
            case "sim_adventurer" -> MobsConfig.infadventurerMobSummon();
            default -> null;
        };
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException error) {
            return fallback;
        }
    }
}
