package alku.csrp.client;

import alku.csrp.Csrp;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Port of SRP 1.10.9 {@code client/gui/GuiSRPWorldPreview} — the animated panel on the right of the
 * world-settings screen: a starfield plus a planet system whose planet texture follows the chosen
 * difficulty / star type, an orbiting moon and, when meteor infection is on, an orbiting meteor.
 *
 * <p>The original seeded its random generator with the fixed value {@code 923847L} so the field is
 * stable across frames; the same seed is reused here.</p>
 */
public final class SrpWorldPreview {
    private static final Identifier EARTH_EASY = id("earth_easy");
    private static final Identifier EARTH_NORMAL = id("earth_normal");
    private static final Identifier EARTH_HARD = id("earth_hard");
    private static final Identifier EARTH_IMPOSSIBLE = id("earth_impossible");
    private static final Identifier EARTH_COLD = id("earth_cold");
    private static final Identifier EARTH_WARM = id("earth_warm");
    private static final Identifier MOON = id("moon");
    private static final Identifier METEOR = id("meteor_orbit");

    private static final long STAR_SEED = 923847L;
    private static final Random RANDOM = new Random(STAR_SEED);
    private static final int STAR_COUNT = 70;
    private static final int STREAK_COUNT = 3;
    private static final int FRAME_COLOR = 0xFF040B12;
    private static final int BORDER_LIGHT = 0xFF50566B;
    private static final int BORDER_DARK = 0xFF15161A;

    private SrpWorldPreview() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Csrp.MODID, "textures/gui/worldsettings/" + path + ".png");
    }

    /**
     * @param difficulty  ordinal of the chosen difficulty preset (0 easy … 3 impossible)
     * @param starType    ordinal of the chosen star type (0 normal, 1 cold, 2 warm)
     * @param meteorEnabled whether meteor infection is on for this world
     */
    public static void draw(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            int difficulty, boolean meteorEnabled, int starType, float partialTick) {
        if (width <= 0 || height <= 0) {
            return;
        }
        float time = (System.currentTimeMillis() % 3_600_000L) / 1000.0F;

        graphics.fill(x, y, x + width, y + height, FRAME_COLOR);
        graphics.fill(x - 1, y - 1, x + width + 1, y, BORDER_LIGHT);
        graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, BORDER_DARK);
        graphics.fill(x - 1, y, x, y + height, BORDER_LIGHT);
        graphics.fill(x + width, y, x + width + 1, y + height, BORDER_DARK);

        graphics.enableScissor(x, y, x + width, y + height);
        drawStars(graphics, x, y, width, height, time);
        drawPlanetSystem(graphics, x, y, width, height, difficulty, meteorEnabled, starType, time);
        graphics.disableScissor();
    }

    private static void drawStars(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            float time) {
        RANDOM.setSeed(STAR_SEED);
        for (int i = 0; i < STAR_COUNT; i++) {
            int baseX = RANDOM.nextInt(Math.max(1, width));
            int baseY = RANDOM.nextInt(Math.max(1, height));
            float depth = 0.35F + RANDOM.nextFloat() * 0.65F;
            float speed = 0.05F + depth * 0.18F;
            float phase = RANDOM.nextFloat() * (float) (Math.PI * 2);
            boolean big = RANDOM.nextInt(12) == 0;
            int brightness = 185 + RANDOM.nextInt(45);
            float twinkle = 0.6F + 0.4F * (float) Math.sin(time * (0.2F + depth * 0.18F) + phase);
            int alpha = (int) (6.0F + depth * 12.0F + twinkle * 14.0F);
            int starX = x + wrap((int) (baseX - time * speed * 3.0F), width);
            int starY = y + baseY;
            drawPrettyStar(graphics, starX, starY, brightness, alpha, big);
        }
        for (int i = 0; i < STREAK_COUNT; i++) {
            int baseX = RANDOM.nextInt(Math.max(1, width));
            int baseY = RANDOM.nextInt(Math.max(1, height));
            float speed = 0.2F + RANDOM.nextFloat() * 0.15F;
            int starX = x + wrap((int) (baseX - time * speed * 4.0F), width);
            int starY = y + baseY;
            int alpha = 4 + RANDOM.nextInt(4);
            int length = 6 + RANDOM.nextInt(5);
            graphics.fill(starX - length / 2, starY, starX + length / 2, starY + 1,
                    argb(alpha, 190, 205, 235));
        }
    }

    private static void drawPrettyStar(GuiGraphicsExtractor graphics, int x, int y, int brightness,
            int alpha, boolean big) {
        int outer = argb(Math.max(2, alpha / 6), brightness, brightness, 255);
        int glow = argb(Math.max(4, alpha / 3), brightness, brightness, 255);
        int core = argb(Math.min(255, alpha), 255, 255, 255);
        graphics.fill(x - 1, y, x + 2, y + 1, outer);
        graphics.fill(x, y - 1, x + 1, y + 2, outer);
        if (big) {
            graphics.fill(x - 1, y - 1, x + 2, y + 2, glow);
        }
        graphics.fill(x, y, x + 1, y + 1, core);
    }

    private static void drawPlanetSystem(GuiGraphicsExtractor graphics, int x, int y, int width,
            int height, int difficulty, boolean meteorEnabled, int starType, float time) {
        float centerX = x + width / 2.0F;
        float centerY = y + height / 2.0F + 4.0F;
        int earthSize = Math.max(44, Math.min(Math.min(width, height) / 2, 70));
        blit(graphics, earthTexture(difficulty, starType),
                (int) (centerX - earthSize / 2.0F), (int) (centerY - earthSize / 2.0F), earthSize, earthSize);

        float moonAngle = time * 0.65F;
        float moonX = centerX + (float) Math.cos(moonAngle) * (earthSize / 2.0F + 22.0F);
        float moonY = centerY + (float) Math.sin(moonAngle) * (earthSize / 3.0F + 12.0F);
        int moonSize = Math.max(10, earthSize / 5);
        blitRotated(graphics, MOON, moonX, moonY, moonSize,
                (float) Math.toDegrees(Math.atan2(centerY - moonY, centerX - moonX)));

        if (meteorEnabled) {
            float meteorAngle = time * -1.15F + 1.7F;
            float meteorX = centerX + (float) Math.cos(meteorAngle) * (earthSize / 2.0F + 34.0F);
            float meteorY = centerY + (float) Math.sin(meteorAngle) * (earthSize / 3.0F + 20.0F);
            int meteorSize = Math.max(8, earthSize / 7);
            blitRotated(graphics, METEOR, meteorX, meteorY, meteorSize,
                    (float) Math.toDegrees(Math.atan2(centerY - meteorY, centerX - meteorX)));
        }
    }

    private static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y,
            int size, int size2) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size2, size, size2);
    }

    private static void blitRotated(GuiGraphicsExtractor graphics, Identifier texture, float centerX,
            float centerY, int size, float angleDegrees) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().rotate((float) Math.toRadians(angleDegrees));
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, -size / 2, -size / 2, 0.0F, 0.0F,
                size, size, size, size);
        graphics.pose().popMatrix();
    }

    private static Identifier earthTexture(int difficulty, int starType) {
        if (starType == 1) {
            return EARTH_COLD;
        }
        if (starType == 2) {
            return EARTH_WARM;
        }
        return switch (difficulty) {
            case 0 -> EARTH_EASY;
            case 2 -> EARTH_HARD;
            case 3 -> EARTH_IMPOSSIBLE;
            default -> EARTH_NORMAL;
        };
    }

    private static int wrap(int value, int max) {
        if (max <= 0) {
            return 0;
        }
        int wrapped = value % max;
        return wrapped < 0 ? wrapped + max : wrapped;
    }

    private static int argb(int a, int r, int g, int b) {
        return clamp(a) << 24 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
    }

    private static int clamp(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 255);
    }
}
