package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Infested ore blocks. Each registered instance represents one of the eight
 * Pestilential Ore variants; they always drop themselves and are immune to
 * Fortune.
 */
public final class PestilentialOreBlock extends Block {
    public enum OreKind {
        TWISTED(null),
        COAL(Blocks.COAL_ORE),
        DIAMOND(Blocks.DIAMOND_ORE),
        EMERALD(Blocks.EMERALD_ORE),
        GOLD(Blocks.GOLD_ORE),
        IRON(Blocks.IRON_ORE),
        LAPIS(Blocks.LAPIS_ORE),
        REDSTONE(Blocks.REDSTONE_ORE);

        private final Block vanilla;

        OreKind(Block vanilla) {
            this.vanilla = vanilla;
        }

        public Block vanilla() {
            return vanilla;
        }

        public static OreKind forVanilla(Block block) {
            if (block == Blocks.DEEPSLATE_COAL_ORE) return COAL;
            if (block == Blocks.DEEPSLATE_DIAMOND_ORE) return DIAMOND;
            if (block == Blocks.DEEPSLATE_EMERALD_ORE) return EMERALD;
            if (block == Blocks.DEEPSLATE_GOLD_ORE) return GOLD;
            if (block == Blocks.DEEPSLATE_IRON_ORE) return IRON;
            if (block == Blocks.DEEPSLATE_LAPIS_ORE) return LAPIS;
            if (block == Blocks.DEEPSLATE_REDSTONE_ORE) return REDSTONE;
            for (OreKind kind : values()) {
                if (kind.vanilla != null && kind.vanilla == block) {
                    return kind;
                }
            }
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            return path.endsWith("_ore") ? TWISTED : null;
        }
    }

    private final OreKind kind;

    public PestilentialOreBlock(OreKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public OreKind kind() {
        return kind;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.2F && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ParticleTypes.CRIMSON_SPORE,
                    pos.getX() + random.nextDouble(), pos.getY() + 1.05D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.02D, 0.0D);
        }
    }
}
