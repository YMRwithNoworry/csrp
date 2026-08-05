package alku.csrp.client;

import alku.csrp.Csrp;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/** Client-side proximity model and first-person glow for the Eye of the Beholder. */
@EventBusSubscriber(modid = Csrp.MODID, value = Dist.CLIENT)
public final class PearlClientEvents {
    private static final double SCAN_RADIUS = 100.0D;
    private static final long UPDATE_INTERVAL = 6L;
    private static final Map<Integer, Cache> CACHE = new HashMap<>();
    private static int glitchFrames;
    private static int lastTickSeen = -1;

    private PearlClientEvents() {
    }

    public static float pearlState(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        if (level == null || entity == null || !isHoldingPearl(entity)) {
            if (entity != null) {
                CACHE.remove(entity.getId());
            }
            return 0.0F;
        }

        long now = level.getGameTime();
        Cache cache = CACHE.computeIfAbsent(entity.getId(), ignored -> new Cache());
        cache.lastAccessTick = now;
        if (now < cache.nextScanTick) {
            return cache.lastValue;
        }
        cache.nextScanTick = now + UPDATE_INTERVAL;
        if ((now & 255L) == 0L) {
            long cutoff = now - 600L;
            CACHE.entrySet().removeIf(entry -> entry.getValue().lastAccessTick < cutoff);
        }

        AABB area = entity.getBoundingBox().inflate(SCAN_RADIUS);
        int tier = 0;
        double nearestDistanceSqr = Double.POSITIVE_INFINITY;
        for (Entity candidate : level.getEntities(entity, area, PearlClientEvents::isBeholder)) {
            int candidateTier = beholderTier(candidate);
            double distanceSqr = candidate.distanceToSqr(entity);
            if (candidateTier > tier || candidateTier == tier && distanceSqr < nearestDistanceSqr) {
                tier = candidateTier;
                nearestDistanceSqr = distanceSqr;
                if (tier == 3) {
                    break;
                }
            }
        }

        if (tier == 0) {
            cache.lastValue = 0.0F;
            return 0.0F;
        }
        float proximity = (float) Mth.clamp((SCAN_RADIUS - Math.sqrt(nearestDistanceSqr)) / SCAN_RADIUS,
                0.0D, 1.0D);
        long time = now + entity.tickCount;
        float frequency = 1.5F + tier + 4.0F * proximity;
        float oscillation = Mth.sin(time * frequency * Mth.TWO_PI / 20.0F);
        float amplitude = 0.12F + 0.15F * tier + 0.25F * proximity;
        float value = tier + 0.1F + amplitude * oscillation;
        cache.lastValue = Mth.clamp(value, tier + 0.05F, tier + 0.45F);
        return cache.lastValue;
    }

    @SubscribeEvent
    public static void renderHeldPearl(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.getItemStack().is(ModItems.PEARL.get()) || minecraft.player == null
                || minecraft.level == null
                || !(event.getMultiBufferSource() instanceof MultiBufferSource.BufferSource buffers)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND
                ? minecraft.player.getMainArm() : minecraft.player.getMainArm().getOpposite();
        boolean rightHand = arm == HumanoidArm.RIGHT;
        ItemDisplayContext context = rightHand
                ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        float state = pearlState(stack, minecraft.level, minecraft.player, 0);
        float strength = state >= 3.0F ? 2.4F : state >= 2.0F ? 1.65F : state >= 1.0F ? 1.0F : 0.35F;
        PoseStack poseStack = event.getPoseStack();

        event.setCanceled(true);
        poseStack.pushPose();
        applyHandTransform(poseStack, arm, event.getEquipProgress(), event.getSwingProgress());
        renderItem(minecraft, stack, context, !rightHand, poseStack, buffers, event.getPackedLight());
        poseStack.popPose();
        buffers.endBatch();

        int tick = minecraft.player.tickCount;
        if (tick != lastTickSeen) {
            lastTickSeen = tick;
            float chance = 0.09F * (0.75F + 0.5F * strength);
            if (glitchFrames <= 0 && minecraft.level.random.nextFloat() < chance) {
                glitchFrames = 2 + minecraft.level.random.nextInt(4);
            }
        }

        float time = (minecraft.player.tickCount + event.getPartialTick()) * 0.44F;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        try {
            for (int layer = 0; layer < 2; layer++) {
                float baseRadius = 0.0075F + layer * 0.0035F;
                float alpha = 0.4F * (1.0F - layer * 0.2F);
                for (int sample = 0; sample < 16; sample++) {
                    float angle = sample / 16.0F * Mth.TWO_PI
                            + Mth.sin(time * 0.9F + sample * 0.7F) * 0.24F;
                    float radius = baseRadius
                            + Mth.sin(time * 1.7F + sample * 1.3F + layer * 0.8F) * 0.0032F;
                    float jitterX = (Mth.sin(time * 19.0F + sample * 0.73F)
                            + Mth.cos(time * 31.0F + sample * 0.41F)) * 0.0006F * strength;
                    float jitterY = (Mth.cos(time * 16.53F + sample * 0.31F)
                            + Mth.sin(time * 34.72F + sample * 0.27F)) * 0.0006F * strength;
                    poseStack.pushPose();
                    poseStack.translate(Mth.cos(angle) * radius + jitterX,
                            Mth.sin(angle) * radius + jitterY, 0.0F);
                    applyHandTransform(poseStack, arm, event.getEquipProgress(), event.getSwingProgress());
                    RenderSystem.setShaderColor(0.85F, 0.95F, 1.0F, alpha);
                    renderItem(minecraft, stack, context, !rightHand, poseStack, buffers, event.getPackedLight());
                    poseStack.popPose();
                    buffers.endBatch();
                }
            }

            if (glitchFrames > 0) {
                float shake = Mth.sin((minecraft.player.tickCount + event.getPartialTick())
                        * 27.0F * (0.9F + 0.2F * strength)) * 0.0045F * (0.75F + 0.6F * strength);
                for (int pass = 0; pass < 7; pass++) {
                    float spread = 0.016F * (0.6F + 0.6F * strength);
                    float offsetX = (minecraft.level.random.nextFloat() * 2.0F - 1.0F) * spread + shake;
                    float offsetY = (minecraft.level.random.nextFloat() * 2.0F - 1.0F) * spread + shake;
                    float rotation = (minecraft.level.random.nextFloat() * 2.0F - 1.0F)
                            * 14.0F * (0.75F + 0.4F * strength);
                    float scale = 1.0F + minecraft.level.random.nextFloat() * 0.09F;
                    poseStack.pushPose();
                    poseStack.translate(offsetX, offsetY, 0.0F);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
                    poseStack.scale(scale, scale, scale);
                    applyHandTransform(poseStack, arm, event.getEquipProgress(), event.getSwingProgress());
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.6F);
                    renderItem(minecraft, stack, context, !rightHand, poseStack, buffers, event.getPackedLight());
                    poseStack.popPose();
                    buffers.endBatch();
                }
                glitchFrames--;
            }
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static void renderItem(Minecraft minecraft, ItemStack stack, ItemDisplayContext context,
            boolean leftHand, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        minecraft.getItemRenderer().renderStatic(minecraft.player, stack, context, leftHand, poseStack,
                buffers, minecraft.level, packedLight, OverlayTexture.NO_OVERLAY, minecraft.player.getId());
    }

    private static void applyHandTransform(PoseStack poseStack, HumanoidArm arm,
            float equipProgress, float swingProgress) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float swingCurve = Mth.sin(swingProgress * swingProgress * Mth.PI);
        float swingRoot = Mth.sin(Mth.sqrt(swingProgress) * Mth.PI);
        float x = -0.4F * swingRoot;
        float y = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * Mth.TWO_PI);
        float z = -0.2F * Mth.sin(swingProgress * Mth.PI);
        poseStack.translate(direction * x, y, z);
        poseStack.translate(direction * 0.56F, -0.52F - equipProgress * 0.6F, -0.72F);
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * (45.0F - swingCurve * 20.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * swingRoot * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(swingRoot * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * -45.0F));
    }

    private static boolean isHoldingPearl(LivingEntity entity) {
        return entity.getMainHandItem().is(ModItems.PEARL.get())
                || entity.getOffhandItem().is(ModItems.PEARL.get());
    }

    private static boolean isBeholder(Entity entity) {
        return beholderTier(entity) > 0;
    }

    private static int beholderTier(Entity entity) {
        if (entity.getType() == ModEntities.MAR_ENDERMAN.get()) {
            return 3;
        }
        if (entity.getType() == ModEntities.FER_ENDERMAN.get()) {
            return 2;
        }
        if (entity.getType() == ModEntities.SIM_ENDERMAN.get()
                || entity.getType() == ModEntities.SIM_ENDERMAN_HEAD.get()) {
            return 1;
        }
        return 0;
    }

    private static final class Cache {
        private long nextScanTick;
        private long lastAccessTick;
        private float lastValue;
    }
}
