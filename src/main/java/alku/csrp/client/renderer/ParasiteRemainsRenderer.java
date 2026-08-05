package alku.csrp.client.renderer;

import alku.csrp.Csrp;
import alku.csrp.entity.ParasiteRemainsEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ParasiteRemainsRenderer extends EntityRenderer<ParasiteRemainsEntity> {
    private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "textures/entity/movingflesh.png");
    private static final ResourceLocation FALLBACK_MODEL = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
            "geo/movingflesh.geo.json");
    private static final Map<ResourceLocation, ResourceLocation> TEXTURE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, List<GeoCube>> CUBE_CACHE = new HashMap<>();

    public ParasiteRemainsRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.14F;
    }

    @Override
    public void render(ParasiteRemainsEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        GeoCube cube = selectCube(entity);
        if (cube == null) {
            return;
        }

        int variant = entity.variant();
        float age = entity.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(age * (8.0F + variant % 7 * 1.7F)));
        poseStack.mulPose(Axis.YP.rotationDegrees(age * (11.0F + variant % 9 * 1.3F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * (6.0F + variant % 5 * 1.1F)));
        renderModelCube(cube, poseStack,
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))), packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static GeoCube selectCube(ParasiteRemainsEntity entity) {
        ResourceLocation source = entity.sourceTypeId();
        List<GeoCube> cubes = CUBE_CACHE.computeIfAbsent(source, ParasiteRemainsRenderer::loadModelCubes);
        if (cubes.isEmpty()) {
            return null;
        }
        int index = Math.floorMod(source.hashCode() * 31 + entity.variant() * 17, cubes.size());
        return cubes.get(index);
    }

    private static List<GeoCube> loadModelCubes(ResourceLocation source) {
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                "geo/" + source.getPath() + ".geo.json");
        BakedGeoModel model = GeckoLibCache.getBakedModels().get(modelId);
        if (model == null) {
            model = GeckoLibCache.getBakedModels().get(FALLBACK_MODEL);
        }
        if (model == null) {
            return List.of();
        }

        List<GeoCube> cubes = new ArrayList<>();
        for (GeoBone bone : model.topLevelBones()) {
            collectCubes(bone, cubes);
        }
        cubes.removeIf(cube -> cube.quads().length == 0 || cubeVolume(cube) < 0.0005D);
        cubes.sort(Comparator.comparingDouble(ParasiteRemainsRenderer::cubeVolume).reversed());
        if (cubes.size() > 32) {
            return List.copyOf(cubes.subList(0, 32));
        }
        return List.copyOf(cubes);
    }

    private static void collectCubes(GeoBone bone, List<GeoCube> cubes) {
        cubes.addAll(bone.getCubes());
        for (GeoBone child : bone.getChildBones()) {
            collectCubes(child, cubes);
        }
    }

    private static double cubeVolume(GeoCube cube) {
        return Math.abs(cube.size().x * cube.size().y * cube.size().z);
    }

    private static void renderModelCube(GeoCube cube, PoseStack poseStack, VertexConsumer consumer, int packedLight) {
        Bounds bounds = bounds(cube);
        if (bounds == null || bounds.maxDimension() <= 0.0001F) {
            return;
        }

        float targetSize = Mth.clamp(bounds.maxDimension(), 0.10F, 0.55F);
        float scale = targetSize / bounds.maxDimension();
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-bounds.centerX(), -bounds.centerY(), -bounds.centerZ());
        PoseStack.Pose pose = poseStack.last();
        Matrix3f normalMatrix = pose.normal();

        for (GeoQuad quad : cube.quads()) {
            if (quad == null) {
                continue;
            }
            Vector3f normal = normalMatrix.transform(new Vector3f(quad.normal()));
            for (GeoVertex vertex : quad.vertices()) {
                Vector3f position = vertex.position();
                consumer.addVertex(pose, position.x, position.y, position.z)
                        .setColor(255, 255, 255, 255)
                        .setUv(vertex.texU(), vertex.texV())
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(normal.x, normal.y, normal.z);
            }
        }
    }

    private static Bounds bounds(GeoCube cube) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        boolean found = false;
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) {
                continue;
            }
            for (GeoVertex vertex : quad.vertices()) {
                Vector3f position = vertex.position();
                minX = Math.min(minX, position.x);
                minY = Math.min(minY, position.y);
                minZ = Math.min(minZ, position.z);
                maxX = Math.max(maxX, position.x);
                maxY = Math.max(maxY, position.y);
                maxZ = Math.max(maxZ, position.z);
                found = true;
            }
        }
        return found ? new Bounds(minX, minY, minZ, maxX, maxY, maxZ) : null;
    }

    @Override
    public ResourceLocation getTextureLocation(ParasiteRemainsEntity entity) {
        return TEXTURE_CACHE.computeIfAbsent(entity.sourceTypeId(), source -> {
            ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(Csrp.MODID,
                    "textures/entity/" + source.getPath() + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(candidate).isPresent()
                    ? candidate : FALLBACK_TEXTURE;
        });
    }

    private record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        float centerX() {
            return (minX + maxX) * 0.5F;
        }

        float centerY() {
            return (minY + maxY) * 0.5F;
        }

        float centerZ() {
            return (minZ + maxZ) * 0.5F;
        }

        float maxDimension() {
            return Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        }
    }
}
