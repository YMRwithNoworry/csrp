package alku.csrp.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

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
            for (OreKind kind : values()) {
                if (kind.vanilla != null && kind.vanilla == block) {
                    return kind;
                }
            }
            return null;
        }
    }

    public PestilentialOreBlock(Properties properties) {
        super(properties);
    }
}
