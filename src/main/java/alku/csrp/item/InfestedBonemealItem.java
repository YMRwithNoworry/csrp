package alku.csrp.item;

import alku.csrp.block.PestilentialOreBlock;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Infested Bonemeal: converts parasite remains into Pestilential Ore and
 * infects vanilla ores, otherwise behaves like regular bonemeal.
 */
public final class InfestedBonemealItem extends BoneMealItem {
    public InfestedBonemealItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block == ModBlocks.INFESTED_REMAINS.get()) {
            if (!level.isClientSide) {
                level.setBlock(pos, ModBlocks.INFESTED_ORE.get().defaultBlockState(), 3);
                spawnInfectionParticles((ServerLevel) level, pos);
                consume(context);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        PestilentialOreBlock.OreKind kind = PestilentialOreBlock.OreKind.forVanilla(block);
        if (kind != null) {
            if (!level.isClientSide) {
                boolean infected = level.getRandom().nextFloat() < 0.5F;
                if (infected) {
                    level.setBlock(pos, infestedBlock(kind), 3);
                }
                spawnInfectionParticles((ServerLevel) level, pos);
                consume(context);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (block instanceof PestilentialOreBlock) {
            if (!level.isClientSide) {
                spawnInfectionParticles((ServerLevel) level, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }

    private static net.minecraft.world.level.block.state.BlockState infestedBlock(PestilentialOreBlock.OreKind kind) {
        return switch (kind) {
            case COAL -> ModBlocks.INFESTED_COAL_ORE.get().defaultBlockState();
            case DIAMOND -> ModBlocks.INFESTED_DIAMOND_ORE.get().defaultBlockState();
            case EMERALD -> ModBlocks.INFESTED_EMERALD_ORE.get().defaultBlockState();
            case GOLD -> ModBlocks.INFESTED_GOLD_ORE.get().defaultBlockState();
            case IRON -> ModBlocks.INFESTED_IRON_ORE.get().defaultBlockState();
            case LAPIS -> ModBlocks.INFESTED_LAPIS_ORE.get().defaultBlockState();
            case REDSTONE -> ModBlocks.INFESTED_REDSTONE_ORE.get().defaultBlockState();
            case TWISTED -> ModBlocks.INFESTED_ORE.get().defaultBlockState();
        };
    }

    private static void spawnInfectionParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 1.1D,
                pos.getZ() + 0.5D, 12, 0.25D, 0.15D, 0.25D, 0.02D);
    }

    private static void consume(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
