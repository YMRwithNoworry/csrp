package alku.csrp.world;

import alku.csrp.Csrp;
import alku.csrp.entity.RemainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Keeps legacy ids loadable, but replaces every old remains block with an independent entity. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class RemainsMigration {
    private static final ConcurrentHashMap<ServerLevel, ConcurrentLinkedQueue<ChunkPos>> PENDING =
            new ConcurrentHashMap<>();
    private RemainsMigration() {
    }

    public static int appearance(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!id.getNamespace().equals(Csrp.MODID)) {
            return -1;
        }
        String tier = switch (id.getPath()) {
            case "goresim" -> "infected";
            case "gorepri" -> "primitive";
            case "goreada" -> "adapted";
            case "gorepur" -> "pure";
            case "gorefer" -> "feral";
            case "goremar" -> "assimara";
            case "infestremain", "infestedremain" -> "infested";
            default -> "";
        };
        if (tier.isEmpty()) {
            return -1;
        }
        if (tier.equals("infested")) {
            return RemainEntity.INFESTED_APPEARANCE;
        }
        String variant = state.getValues().entrySet().stream()
                .filter(entry -> entry.getKey().getName().equals("variant"))
                .map(entry -> entry.getValue() instanceof StringRepresentable named
                        ? named.getSerializedName() : "flat")
                .findFirst().orElse("flat");
        return RemainEntity.appearance(tier, variant);
    }

    public static void convert(ServerLevel level, BlockPos pos, BlockState state) {
        int appearance = appearance(state);
        if (appearance < 0 || level.getBlockState(pos) != state) {
            return;
        }
        // Reuse an existing rebuild counter so converting old worlds cannot reset its progress.
        RemainEntity remains = level.getEntitiesOfClass(RemainEntity.class, new AABB(pos).inflate(0.1D),
                entity -> entity.blockPosition().equals(pos)).stream().findFirst().orElse(null);
        if (remains == null) {
            String tier = appearance == RemainEntity.INFESTED_APPEARANCE ? "infested"
                    : new String[] {"sim", "pri", "ada", "pure", "fer", "mar"}[appearance / 3];
            String variant = new String[] {"flat", "small", "big"}[appearance % 3];
            remains = RemainEntity.spawn(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, tier, variant);
        }
        if (remains != null) {
            remains.setAppearance(appearance);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    @SubscribeEvent
    public static void convertLoadedChunk(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            PENDING.computeIfAbsent(level, ignored -> new ConcurrentLinkedQueue<>()).add(chunk.getPos());
        }
    }

    @SubscribeEvent
    public static void tickMigration(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var pending = PENDING.get(level);
        if (pending == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            ChunkPos pos = pending.poll();
            if (pos == null) {
                break;
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null) {
                convertChunk(level, chunk);
            }
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PENDING.remove(level);
        }
    }

    private static void convertChunk(ServerLevel level, LevelChunk chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (!section.maybeHas(state -> appearance(state) >= 0)) {
                continue;
            }
            int baseY = chunk.getMinBuildHeight() + sectionIndex * 16;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (appearance(state) >= 0) {
                            pos.set(chunk.getPos().getMinBlockX() + x, baseY + y,
                                    chunk.getPos().getMinBlockZ() + z);
                            convert(level, pos, state);
                        }
                    }
                }
            }
        }
    }
}
