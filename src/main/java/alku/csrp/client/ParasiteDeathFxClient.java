package alku.csrp.client;

import alku.csrp.network.ParasiteDeathFxPayload;
import com.lowdragmc.photon.client.fx.BlockEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.gameobject.emitter.data.EmissionSetting;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.RandomConstant;
import com.lowdragmc.photon.client.gameobject.emitter.data.shape.Sphere;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class ParasiteDeathFxClient {
    private ParasiteDeathFxClient() {
    }

    public static void play(ParasiteDeathFxPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        ParticleEmitter emitter = new ParticleEmitter();
        emitter.setName("parasite_death_explosion");
        emitter.config.setDuration(8);
        emitter.config.setLooping(false);
        emitter.config.setStartLifetime(new RandomConstant(12, 24));
        emitter.config.setStartSpeed(new RandomConstant(1.2F, 2.8F));
        emitter.config.setStartSize(new com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3(
                0.18F, 0.18F, 0.18F));
        emitter.config.setStartColor(NumberFunction.color(0xFF9C1018));
        emitter.config.emission.setEmissionRate(NumberFunction.ZERO);
        EmissionSetting.Burst burst = new EmissionSetting.Burst();
        burst.setCount(NumberFunction.constant(Math.max(24, Math.round(42.0F * payload.scale()))));
        emitter.config.emission.getBursts().add(burst);
        Sphere sphere = new Sphere();
        sphere.setRadius(0.2F);
        sphere.setRadiusThickness(1.0F);
        emitter.config.shape.setShape(sphere);
        emitter.config.physics.setEnable(true);
        emitter.config.physics.setGravity(NumberFunction.constant(0.08F));
        emitter.config.physics.setBounceRate(NumberFunction.constant(0.45F));
        emitter.config.physics.setCollidedFriction(NumberFunction.constant(0.65F));

        FX fx = new FX();
        fx.getFxData().objects().add(emitter);
        BlockPos anchor = BlockPos.containing(payload.x(), payload.y(), payload.z());
        BlockEffectExecutor executor = new BlockEffectExecutor(fx, level, anchor);
        executor.setOffset(new Vector3f(
                (float) (payload.x() - anchor.getX() - 0.5D),
                (float) (payload.y() - anchor.getY() - 0.5D),
                (float) (payload.z() - anchor.getZ() - 0.5D)));
        executor.setScale(new Vector3f(payload.scale()));
        executor.start();
    }
}
