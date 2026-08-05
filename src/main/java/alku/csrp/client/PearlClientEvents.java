package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.entity.AssimilatedEndermanEntity;
import alku.csrp.entity.FeralEndermanEntity;
import alku.csrp.entity.MarauderizedEndermanEntity;
import alku.csrp.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Holding an Eye of the Beholder emits colored particles when an Enderman
 * variant is within 60 blocks (pink = assimilated, red = feral, blue =
 * assimara).
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class PearlClientEvents {
    private PearlClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || minecraft.level.getGameTime() % 10L != 0L) {
            return;
        }
        boolean holding = minecraft.player.getMainHandItem().is(ModItems.PEARL.get())
                || minecraft.player.getOffhandItem().is(ModItems.PEARL.get());
        if (!holding) {
            return;
        }
        AABB area = minecraft.player.getBoundingBox().inflate(60.0D);
        if (!minecraft.level.getEntitiesOfClass(FeralEndermanEntity.class, area).isEmpty()) {
            spawnParticles(minecraft, ParticleTypes.FLAME);
        } else if (!minecraft.level.getEntitiesOfClass(AssimilatedEndermanEntity.class, area).isEmpty()) {
            spawnParticles(minecraft, ParticleTypes.END_ROD);
        } else if (!minecraft.level.getEntitiesOfClass(MarauderizedEndermanEntity.class, area).isEmpty()) {
            spawnParticles(minecraft, ParticleTypes.SOUL_FIRE_FLAME);
        }
    }

    private static void spawnParticles(Minecraft minecraft, net.minecraft.core.particles.ParticleOptions particle) {
        var player = minecraft.player;
        for (int i = 0; i < 4; i++) {
            minecraft.level.addParticle(particle,
                    player.getX() + (minecraft.level.random.nextDouble() - 0.5D) * 1.2D,
                    player.getY() + player.getEyeHeight() + (minecraft.level.random.nextDouble() - 0.5D) * 1.2D,
                    player.getZ() + (minecraft.level.random.nextDouble() - 0.5D) * 1.2D,
                    0.0D, 0.02D, 0.0D);
        }
    }
}
