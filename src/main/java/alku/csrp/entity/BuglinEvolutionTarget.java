package alku.csrp.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.Optional;
import java.util.function.Supplier;

public final class BuglinEvolutionTarget {
    private static Supplier<? extends EntityType<? extends Mob>> rupterType;

    private BuglinEvolutionTarget() {
    }

    public static void registerRupter(Supplier<? extends EntityType<? extends Mob>> type) {
        rupterType = type;
    }

    public static Optional<EntityType<? extends Mob>> rupterType() {
        return rupterType == null ? Optional.empty() : Optional.of(rupterType.get());
    }
}
