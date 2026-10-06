package alku.csrp.client.weather;

import alku.csrp.Csrp;
import alku.csrp.config.WorldConfig;
import alku.csrp.registry.ModFluidTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The client half of 1.10.9's parasite fog, driven by {@code SRPEventHandlerBus} on the client.
 *
 * <p>The original had two hooks:</p>
 * <pre>
 * FogDensity:  if (entity.isInsideOfMaterial(SRPBlocks.PARASITEBLOOD)) { setFog(FogMode.EXP); density = 1.0F; }
 *              else if (fog &gt; 0 &amp;&amp; !nightVision)                { setFog(FogMode.EXP); density = fog; }
 * FogColors:   if (entity.isInsideOfMaterial(SRPBlocks.PARASITEBLOOD)) { 0.1, 0.6, 0.2 }
 *              else if (fog &gt; biomeFogDensity * 0.2 &amp;&amp; !nightVision) { fogRed, fogGreen, fogBlue }
 * </pre>
 *
 * <p>{@code SRPBlocks.PARASITEBLOOD} is a {@code Material}, not a block — it is the fluid material of
 * the dead-blood fluid — so "inside parasite blood" is {@link LocalPlayer#isInFluidType} on the
 * port's {@code csrp:deadblood}.  Only that branch is wired up here; the biome branch needs the
 * node-influence radius query and {@code biomeFogDensity}, which the project does not have yet
 * (tracked in {@code PORT_BACKLOG.md}, "网络包对照审计" → 寄生群系雾).</p>
 *
 * <p>26.3 note: {@code EntityViewRenderEvent.FogDensity} no longer exists.  Its exponential density
 * is expressed through the linear near/far planes of {@link ViewportEvent.RenderFog}, using the same
 * conversion the project's extreme-snow whiteout uses
 * ({@code density = sqrt(ln 50) / visibility}, {@code ClientExtremeSnow.density()}).  The original
 * overrode the fog for every fog mode, so no {@code FogType} filter is applied here.</p>
 */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class ClientParasiteFog {
    /** {@code event.setDensity(1.0F)} of the parasite-blood branch. */
    private static final float BLOOD_DENSITY = 1.0F;
    /** {@code FogColors}: {@code setRed(0.1F) / setGreen(0.6F) / setBlue(0.2F)}. */
    private static final float BLOOD_RED = 0.1F;
    private static final float BLOOD_GREEN = 0.6F;
    private static final float BLOOD_BLUE = 0.2F;
    /** The near plane the project's other fog paths use, as a fraction of the far plane. */
    private static final double NEAR_PLANE_RATIO = 0.08D;

    /** {@code SRPEventHandlerBus.fog / fogRed / fogGreen / fogBlue} — the synced biome fog. */
    private static float biomeFog;
    private static float biomeRed;
    private static float biomeGreen;
    private static float biomeBlue;
    /** The level the synced values belong to; the original cleared the statics on world unload. */
    private static ClientLevel trackedLevel;

    private ClientParasiteFog() {
    }

    /** {@code densityForVisibility}: the visibility in blocks at which the exponential fog is 1/50. */
    static float visibilityFor(float density) {
        if (density <= 0.0F) {
            return Float.MAX_VALUE;
        }
        return (float) Mth.clamp(Math.sqrt(Math.log(50.0D)) / density, 0.0D, 512.0D);
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog event) {
        float density = activeDensity();
        if (density <= 0.0F) {
            return;
        }
        float far = visibilityFor(density);
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance((float) (far * NEAR_PLANE_RATIO));
    }

    @SubscribeEvent
    public static void computeFogColor(ViewportEvent.ComputeFogColor event) {
        if (inParasiteBlood()) {
            event.setRed(BLOOD_RED);
            event.setGreen(BLOOD_GREEN);
            event.setBlue(BLOOD_BLUE);
            return;
        }
        // `if (fog > SRPConfigWorld.biomeFogDensity * 0.2 && !nightVision)`.
        if (biomeFog > WorldConfig.biomeFogDensity() * 0.2F && !hasNightVision()) {
            event.setRed(biomeRed);
            event.setGreen(biomeGreen);
            event.setBlue(biomeBlue);
        }
    }

    /**
     * The density both hooks act on: {@code 1.0F} inside parasite blood (the original exempts only the
     * biome branch from night vision), otherwise the synced biome fog while it is non-zero.
     */
    private static float activeDensity() {
        forgetOtherLevels();
        if (inParasiteBlood()) {
            return BLOOD_DENSITY;
        }
        return biomeFog > 0.0F && !hasNightVision() ? biomeFog : 0.0F;
    }

    /** {@code SRPPacketFog.Handler}: {@code SRPEventHandlerBus.fog = message.rFog}, and its three colours. */
    public static void setBiomeFog(float density, float red, float green, float blue) {
        biomeFog = density;
        biomeRed = red;
        biomeGreen = green;
        biomeBlue = blue;
    }

    /** {@code SRPEventHandlerBus#onWorldLoad(Unload)} cleared the fog statics. */
    private static void forgetOtherLevels() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != trackedLevel) {
            trackedLevel = level;
            biomeFog = 0.0F;
            biomeRed = 0.0F;
            biomeGreen = 0.0F;
            biomeBlue = 0.0F;
        }
    }

    /** {@code !Minecraft.getMinecraft().thePlayer.isPotionActive(MobEffects.field_76440_q)} (night vision). */
    private static boolean hasNightVision() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.hasEffect(MobEffects.NIGHT_VISION);
    }

    /** {@code entity.isInsideOfMaterial(SRPBlocks.PARASITEBLOOD)}. */
    static boolean inParasiteBlood() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isInFluidType(ModFluidTypes.DEAD_BLOOD.get());
    }
}
