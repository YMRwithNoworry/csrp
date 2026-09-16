package alku.csrp.block.entity;

import alku.csrp.block.FogNullifierBlock;
import alku.csrp.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(USES_TAG, usesRemaining);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        usesRemaining = Math.max(0, input.getIntOr(USES_TAG, FogNullifierBlock.MAX_USES));
    }
}
