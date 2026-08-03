package alku.csrp.block;

import alku.csrp.block.entity.ParasiteLootBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ParasiteLootBlock extends Block implements EntityBlock {
    private final Tier tier;

    public ParasiteLootBlock(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ParasiteLootBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ParasiteLootBlockEntity loot)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            player.openMenu(loot);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ParasiteLootBlockEntity loot) {
            loot.clearContent();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.2F && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ParticleTypes.CRIMSON_SPORE,
                    pos.getX() + random.nextDouble(), pos.getY() + 1.05D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.02D, 0.0D);
        }
    }

    public enum Tier {
        COMMON(0.5F),
        UNCOMMON(0.1F),
        RARE(0.2F);

        private final float slotChance;

        Tier(float slotChance) {
            this.slotChance = slotChance;
        }

        public float slotChance() {
            return slotChance;
        }
    }
}
