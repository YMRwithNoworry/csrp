package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.registry.ModItems;
import alku.csrp.world.BlockPurification;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;

/** Quench impact purification: immediate 15x15 core followed by expanding shells. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class QuenchProjectileEvents {
    private static final String IMPACTED_TAG = "csrp_quench_impacted";
    private static final int INITIAL_RADIUS = 7;
    private static final int MAX_RADIUS = 15;
    private static final Map<ServerLevel, List<Pulse>> PULSES = new WeakHashMap<>();

    private QuenchProjectileEvents() {
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Snowball snowball)
                || !(snowball.level() instanceof ServerLevel level)
                || !snowball.getItem().is(ModItems.ITEMTHROW.get())
                || snowball.getPersistentData().getBoolean(IMPACTED_TAG)) {
            return;
        }
        snowball.getPersistentData().putBoolean(IMPACTED_TAG, true);
        BlockPos center = BlockPos.containing(event.getRayTraceResult().getLocation());
        purifyVolume(level, center, 0, INITIAL_RADIUS);
        PULSES.computeIfAbsent(level, ignored -> new ArrayList<>())
                .add(new Pulse(center.immutable(), INITIAL_RADIUS + 1, 3));
        level.sendParticles(ParticleTypes.FLAME, center.getX() + 0.5D, center.getY() + 0.5D,
                center.getZ() + 0.5D, 40, 1.5D, 1.5D, 1.5D, 0.04D);
        level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.5F, 0.8F);
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {return;}
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        List<Pulse> pulses = PULSES.get(level);
        if (pulses == null) {
            return;
        }
        Iterator<Pulse> iterator = pulses.iterator();
        while (iterator.hasNext()) {
            Pulse pulse = iterator.next();
            if (pulse.delay > 0) {
                pulse.delay--;
                continue;
            }
            purifyVolume(level, pulse.center, pulse.radius, pulse.radius);
            level.sendParticles(ParticleTypes.SMOKE, pulse.center.getX() + 0.5D,
                    pulse.center.getY() + 0.5D, pulse.center.getZ() + 0.5D,
                    10 + pulse.radius, pulse.radius * 0.5D, 0.5D, pulse.radius * 0.5D, 0.01D);
            pulse.radius++;
            pulse.delay = 3;
            if (pulse.radius > MAX_RADIUS) {
                iterator.remove();
            }
        }
        if (pulses.isEmpty()) {
            PULSES.remove(level);
        }
    }

    private static void purifyVolume(ServerLevel level, BlockPos center, int innerRadius, int outerRadius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int y = -outerRadius; y <= outerRadius; y++) {
                for (int z = -outerRadius; z <= outerRadius; z++) {
                    int shell = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
                    if (shell < innerRadius || shell > outerRadius) {
                        continue;
                    }
                    cursor.setWithOffset(center, x, y, z);
                    BlockPurification.purify(level, cursor);
                }
            }
        }
        level.levelEvent(2001, center, Block.getId(Blocks.SPONGE.defaultBlockState()));
    }

    private static final class Pulse {
        private final BlockPos center;
        private int radius;
        private int delay;

        private Pulse(BlockPos center, int radius, int delay) {
            this.center = center;
            this.radius = radius;
            this.delay = delay;
        }
    }
}
