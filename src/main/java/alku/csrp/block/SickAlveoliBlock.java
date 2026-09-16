package alku.csrp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class SickAlveoliBlock extends Block {

    public SickAlveoliBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 0));
        }
    }
}
