package alku.csrp.command;

import alku.csrp.world.ParasiteBiomeDecorator;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Port of SRP 1.10.9 {@code network/CommandHarlequinHere}.
 *
 * <p>{@code /harlequin_here [radiusChunks=1]} at permission level 2: every chunk in the
 * {@code (2r+1)x(2r+1)} square around the caller is switched to the Harlequin biome and the change is
 * pushed to nearby clients, then the count and the caller's new biome are reported.</p>
 *
 * <p>The original filled {@code Chunk#getBiomeArray()} outright and hand-rolled the chunk-biome packet.
 * 26.3 stores biomes per 4x4x4 section, so the equivalent is
 * {@link ChunkAccess#fillBiomesFromNoise} — the same primitive {@code /fillbiome} uses — followed by
 * {@code ChunkMap#resendBiomesForChunks} for the client update.  Vanilla's own
 * {@code FillBiomeCommand#fill} is deliberately <em>not</em> used: it rejects volumes over 32768 blocks,
 * which a full-height chunk square far exceeds, whereas the original had no such limit.</p>
 */
public final class HarlequinCommand {
    public static final String HERE = "harlequin_here";
    /** {@code CommandHarlequinHere}: {@code [radiusChunks=1]}. */
    private static final int DEFAULT_RADIUS_CHUNKS = 1;
    private static final int MAX_RADIUS_CHUNKS = 32;

    private HarlequinCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> hereCommand() {
        return Commands.literal(HERE)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> here(context.getSource(), DEFAULT_RADIUS_CHUNKS))
                .then(Commands.argument("radiusChunks",
                                IntegerArgumentType.integer(0, MAX_RADIUS_CHUNKS))
                        .executes(context -> here(context.getSource(),
                                IntegerArgumentType.getInteger(context, "radiusChunks"))));
    }

    private static int here(CommandSourceStack source, int radiusChunks) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Holder<Biome> harlequin = harlequinBiome(level);

        BlockPos playerPos = player.blockPosition();
        int changed = applyHarlequinBiome(level, playerPos.getX() >> 4, playerPos.getZ() >> 4,
                radiusChunks, harlequin);

        int side = radiusChunks * 2 + 1;
        source.sendSuccess(() -> Component.literal("Applied Harlequin to " + changed
                + " chunk(s) in a " + side + "x" + side + " square."), true);

        String biomeName = level.getBiome(playerPos).unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("unknown");
        source.sendSuccess(() -> Component.literal("Server biome at your position now: " + biomeName),
                false);
        return Command.SINGLE_SUCCESS;
    }

    public static Holder<Biome> harlequinBiome(ServerLevel level) {
        return level.registryAccess()
                .lookupOrThrow(Registries.BIOME)
                .getOrThrow(ParasiteBiomeDecorator.HARLEQUIN_BIOME);
    }

    /**
     * The original's per-chunk biome write, plus the client resend.
     *
     * @return how many chunks were rewritten
     */
    public static int applyHarlequinBiome(ServerLevel level, int baseChunkX, int baseChunkZ,
            int radiusChunks, Holder<Biome> harlequin) {
        List<ChunkAccess> changed = new ArrayList<>();
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                LevelChunk chunk = level.getChunk(baseChunkX + dx, baseChunkZ + dz);
                chunk.fillBiomesFromNoise((x, y, z) -> harlequin);
                chunk.markUnsaved();
                changed.add(chunk);
            }
        }
        if (!changed.isEmpty()) {
            level.getChunkSource().chunkMap.resendBiomesForChunks(changed);
        }
        return changed.size();
    }
}
