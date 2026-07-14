package alku.csrp.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.Optional;
import java.util.function.Supplier;

public final class ManglerEvolutionTarget {
    private static Supplier<? extends EntityType<? extends Mob>> manglerType;

    private ManglerEvolutionTarget() {
    }

    public static void registerMangler(Supplier<? extends EntityType<? extends Mob>> type) {
        manglerType = type;
    }

    public static Optional<EntityType<? extends Mob>> manglerType() {
        return manglerType == null ? Optional.empty() : Optional.of(manglerType.get());
    }
}
