package alku.csrp.client.weather;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Overworld dimension effects for the cold star blizzard, and the replacement for 1.10.9's
 * {@code MixinEntityRendererBlizzard} + {@code MixinRenderGlobalBlizzardSky}. In 1.20.1 the {@code
 * EntityRenderer} and {@code RenderGlobal} classes no longer exist; the two Forge-sanctioned hooks are
 * {@link DimensionSpecialEffects#getSunriseColor(float, float)} (the only consumer of the sunrise colours
 * is {@code FogRenderer.setupColor}) and {@code IForgeDimensionSpecialEffects#renderSnowAndRain} (which
 * fully replaces vanilla precipitation when it returns {@code true}).
 *
 * <p>Registration is global for {@code minecraft:overworld}, so every method that is not
 * blizzard-specific forwards to a shared vanilla {@link DimensionSpecialEffects.OverworldEffects}
 * instance. When the star is not cold, both overridden methods take the same vanilla branch, which keeps
 * the sky, clouds, terrain fog and vanilla rain/snow pixel-identical to an unmodified client.
 */
public final class BlizzardDimensionEffects extends DimensionSpecialEffects {
    private static final DimensionSpecialEffects VANILLA = new DimensionSpecialEffects.OverworldEffects();

    public BlizzardDimensionEffects() {
        super(DimensionSpecialEffects.OverworldEffects.CLOUD_LEVEL, true, SkyType.NORMAL, false, false);
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTick) {
        if (BlizzardClient.getIntensity(partialTick) > BlizzardClient.MIN_INTENSITY) {
            return null;
        }
        return VANILLA.getSunriseColor(timeOfDay, partialTick);
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture,
            double camX, double camY, double camZ) {
        if (BlizzardClient.getIntensity(partialTick) > BlizzardClient.MIN_INTENSITY) {
            // The blizzard is drawn from RenderLevelStageEvent so it sits in the vanilla weather slot with
            // the level's pose already set up; returning true suppresses the vanilla rain/snow layers.
            return true;
        }
        return VANILLA.renderSnowAndRain(level, ticks, partialTick, lightTexture, camX, camY, camZ);
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
            double camX, double camY, double camZ, Matrix4f projection) {
        return VANILLA.renderClouds(level, ticks, partialTick, poseStack, camX, camY, camZ, projection);
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera,
            Matrix4f projection, boolean isFoggy, Runnable setupFog) {
        return VANILLA.renderSky(level, ticks, partialTick, poseStack, camera, projection, isFoggy, setupFog);
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return VANILLA.tickRain(level, ticks, camera);
    }

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTick, float skyDarken, float blockLightRedFlicker,
            float skyLight, int pixelX, int pixelY, Vector3f colors) {
        VANILLA.adjustLightmapColors(level, partialTick, skyDarken, blockLightRedFlicker, skyLight, pixelX, pixelY,
                colors);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return VANILLA.getBrightnessDependentFogColor(fogColor, brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return VANILLA.isFoggyAt(x, z);
    }

    @Override
    public float getCloudHeight() {
        return VANILLA.getCloudHeight();
    }

    @Override
    public boolean hasGround() {
        return VANILLA.hasGround();
    }

    @Override
    public SkyType skyType() {
        return VANILLA.skyType();
    }

    @Override
    public boolean forceBrightLightmap() {
        return VANILLA.forceBrightLightmap();
    }

    @Override
    public boolean constantAmbientLight() {
        return VANILLA.constantAmbientLight();
    }
}
