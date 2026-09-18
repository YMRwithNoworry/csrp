package alku.csrp.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Compatibility shim for the 1.10.9 {@code ParasiteEventEntity#getFloor(World, BlockPos, int)} helper
 * the mouth feature used.  The implementation lives on {@link ParasiteGenContext#floor}.
 */
public final class ParasiteFloorScan {
    private ParasiteFloorScan() {
    }

    public static BlockPos getFloor(ServerLevel level, BlockPos pos, int range) {
        return ParasiteGenContext.floor(level, pos, range);
    }
}
