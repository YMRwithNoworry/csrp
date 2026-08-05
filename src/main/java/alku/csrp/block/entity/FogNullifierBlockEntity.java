package alku.csrp.block.entity;

import alku.csrp.block.FogNullifierBlock;
import alku.csrp.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FogNullifierBlockEntity extends BlockEntity {
    public static final String USES_TAG = "UsesRemaining";
    private int usesRemaining = FogNullifierBlock.MAX_USES;
    private boolean clearing;

    public FogNullifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOG_NULLIFIER.get(), pos, state);
    }

    public int usesRemaining() {
        return usesRemaining;
    }

    public void setUsesRemaining(int uses) {
        usesRemaining = Math.max(0, uses);
        setChanged();
    }

    public boolean isClearing() {
        return clearing;
    }

    public void setClearing(boolean clearing) {
        this.clearing = clearing;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(USES_TAG, usesRemaining);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        usesRemaining = tag.contains(USES_TAG) ? Math.max(0, tag.getInt(USES_TAG)) : FogNullifierBlock.MAX_USES;
    }
}
