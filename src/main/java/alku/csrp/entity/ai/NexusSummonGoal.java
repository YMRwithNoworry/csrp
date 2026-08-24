package alku.csrp.entity.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** Shared summon goal with a hard cap and cleanup of dead or unloaded summons. */
public class NexusSummonGoal extends Goal {
    private final Mob summoner;
    private final EntityType<? extends Mob> summonType;
    private final int maxSummons;
    private final int cooldownTicks;
    private final double spawnRadius;
    private final List<UUID> summons = new ArrayList<>();
    private int cooldown;

    public NexusSummonGoal(Mob summoner, EntityType<? extends Mob> summonType, int maxSummons,
                           int cooldownTicks, double spawnRadius) {
        this.summoner = summoner;
        this.summonType = summonType;
        this.maxSummons = Math.max(1, maxSummons);
        this.cooldownTicks = Math.max(1, cooldownTicks);
        this.spawnRadius = Math.max(1.0D, spawnRadius);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        pruneSummons();
        LivingEntity target = summoner.getTarget();
        return summoner.level() instanceof ServerLevel && target != null && target.isAlive()
                && summons.size() < maxSummons;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        cooldown = cooldownTicks;
        if (!(summoner.level() instanceof ServerLevel level)) return;
        Mob summon = summonType.create(level);
        if (summon == null) return;
        double angle = summoner.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = 1.0D + summoner.getRandom().nextDouble() * (spawnRadius - 1.0D);
        Vec3 position = summoner.position().add(Math.cos(angle) * distance, 0.0D,
                Math.sin(angle) * distance);
        summon.moveTo(position.x, position.y, position.z, summoner.getRandom().nextFloat() * 360.0F, 0.0F);
        summon.finalizeSpawn(level, level.getCurrentDifficultyAt(summon.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null, null);
        LivingEntity target = summoner.getTarget();
        if (target != null) summon.setTarget(target);
        if (level.addFreshEntity(summon)) summons.add(summon.getUUID());
    }

    private void pruneSummons() {
        summons.removeIf(id -> !(summoner.level() instanceof ServerLevel level)
                || !(level.getEntity(id) instanceof Mob mob) || !mob.isAlive());
    }
}
