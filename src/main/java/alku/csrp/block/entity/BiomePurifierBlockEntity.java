package alku.csrp.block.entity;

import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModBlockEntities;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.BlockPurification;
import alku.csrp.world.BiomeChunkSectionAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

/**
 * Biome Purifier: while placed it glows Nexus parasites, enrages nearby
 * parasites, and slowly purifies infected blocks when no Beckon is near.
 * Right-clicking activates a 3x3 chunk-area biome conversion to Plains.
 */
public final class BiomePurifierBlockEntity extends BlockEntity {
    private static final double RANGE = 32.0D;
    private static final int PURIFY_RADIUS = 16;

    public BiomePurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIOME_PURIFIER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            BlockEntity blockEntity) {
        if (!(blockEntity instanceof BiomePurifierBlockEntity purifier) || level.isClientSide) {
            return;
        }
        purifier.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        long time = level.getGameTime();
        if (time % 20L != 0L) {
            return;
        }
        AABB area = new AABB(getBlockPos()).inflate(RANGE);
        for (NexusParasiteEntity nexus : level.getEntitiesOfClass(NexusParasiteEntity.class,
                area, entity -> entity.getKind().name().startsWith("BECKON"))) {
            nexus.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate instanceof Parasite)) {
            entity.addEffect(new MobEffectInstance(ModMobEffects.RAGE.get(), 600, 0));
        }
        boolean beckonNearby = !level.getEntitiesOfClass(NexusParasiteEntity.class, area,
                entity -> entity.getKind().name().startsWith("BECKON")).isEmpty();
        if (!beckonNearby) {
            for (int attempt = 0; attempt < 8; attempt++) {
                BlockPos candidate = getBlockPos().offset(
                        level.getRandom().nextInt(PURIFY_RADIUS * 2 + 1) - PURIFY_RADIUS,
                        level.getRandom().nextInt(9) - 4,
                        level.getRandom().nextInt(PURIFY_RADIUS * 2 + 1) - PURIFY_RADIUS);
                BlockPurification.purify(level, candidate);
            }
        }
    }

    public void activate(ServerLevel level) {
        Holder<Biome> plains = level.registryAccess()
                .registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        int centerChunkX = getBlockPos().getX() >> 4;
        int centerChunkZ = getBlockPos().getZ() >> 4;
        for (int chunkX = centerChunkX - 1; chunkX <= centerChunkX + 1; chunkX++) {
            for (int chunkZ = centerChunkZ - 1; chunkZ <= centerChunkZ + 1; chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (LevelChunkSection section : chunk.getSections()) {
                    if (section == null) {
                        continue;
                    }
                    BiomeChunkSectionAccessor accessor =
                            (BiomeChunkSectionAccessor) (Object) section;
                    for (int x = 0; x < 4; x++) {
                        for (int y = 0; y < 4; y++) {
                            for (int z = 0; z < 4; z++) {
                                accessor.csrp$setBiome(x, y, z, plains);
                            }
                        }
                    }
                }
                chunk.setUnsaved(true);
            }
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                getBlockPos().getX() + 0.5D, getBlockPos().getY() + 1.0D,
                getBlockPos().getZ() + 0.5D, 80, 1.5D, 0.5D, 1.5D, 0.02D);
    }
}
