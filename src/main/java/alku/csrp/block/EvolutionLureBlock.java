package alku.csrp.block;

import alku.csrp.entity.ParasiticScentEntity;
import alku.csrp.registry.ModBlocks;
import alku.csrp.registry.ModEntities;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class EvolutionLureBlock extends Block {
    public static final EnumProperty<Tier> TIER = EnumProperty.create("tier", Tier.class);
    private static final int CARCASS_OFFSET = 3;

    public EvolutionLureBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIER, Tier.ONE));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel) {
            activate(serverLevel, pos, state.getValue(TIER), player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.csrp.lure_empty_hand"), true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private static void activate(ServerLevel level, BlockPos center, Tier tier, Player player) {
        SrpWorldData data = SrpWorldData.get(level);
        if (isCarcass(level, center)) {
            activateCarcass(level, center, tier, player, data);
            return;
        }
        if (data.evolutionPhase() <= -1) {
            player.displayClientMessage(Component.translatable("message.csrp.lure_dormant"), true);
            return;
        }

        data.addCooldown(level, tier.cooldownSeconds());
        level.removeBlock(center, false);
        level.sendParticles(ParticleTypes.SMOKE, center.getX() + 0.5D, center.getY() + 0.5D,
                center.getZ() + 0.5D, 24, 0.35D, 0.35D, 0.35D, 0.02D);
        level.playSound(null, center, ModSounds.LURE_USE.get(), net.minecraft.sounds.SoundSource.BLOCKS,
                1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        player.displayClientMessage(Component.translatable("message.csrp.lure_cooldown_added",
                tier.cooldownSeconds()), true);
    }

    private static void activateCarcass(ServerLevel level, BlockPos center, Tier tier, Player player,
            SrpWorldData data) {
        if (data.cooldown(level) > 0) {
            player.displayClientMessage(Component.translatable("message.csrp.lure_inactive", data.cooldown(level)),
                    true);
            return;
        }
        if (!data.addEvolutionPoints(level, -tier.carcassReduction())) {
            player.displayClientMessage(Component.translatable("message.csrp.lure_dormant"), true);
            return;
        }

        removeCarcass(level, center);
        spawnVisualLightning(level, center);
        if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.isCreative()) {
            spawnScent(level, player, tier);
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.getX() + 0.5D, center.getY() + 0.8D,
                center.getZ() + 0.5D, 60, 1.5D, 0.8D, 1.5D, 0.03D);
        level.playSound(null, center, ModSounds.CARCASS_USE.get(), net.minecraft.sounds.SoundSource.BLOCKS,
                2.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.csrp.lure_points_reduced",
                tier.carcassReduction()), true);
    }

    private static boolean isCarcass(ServerLevel level, BlockPos center) {
        return isLure(level, center.offset(CARCASS_OFFSET, 0, CARCASS_OFFSET))
                && isLure(level, center.offset(CARCASS_OFFSET, 0, -CARCASS_OFFSET))
                && isLure(level, center.offset(-CARCASS_OFFSET, 0, CARCASS_OFFSET))
                && isLure(level, center.offset(-CARCASS_OFFSET, 0, -CARCASS_OFFSET));
    }

    private static boolean isLure(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.EVOLUTION_LURE);
    }

    private static void removeCarcass(ServerLevel level, BlockPos center) {
        level.removeBlock(center, false);
        level.removeBlock(center.offset(CARCASS_OFFSET, 0, CARCASS_OFFSET), false);
        level.removeBlock(center.offset(CARCASS_OFFSET, 0, -CARCASS_OFFSET), false);
        level.removeBlock(center.offset(-CARCASS_OFFSET, 0, CARCASS_OFFSET), false);
        level.removeBlock(center.offset(-CARCASS_OFFSET, 0, -CARCASS_OFFSET), false);
    }

    private static void spawnVisualLightning(ServerLevel level, BlockPos center) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);
        }
    }

    private static void spawnScent(ServerLevel level, Player player, Tier tier) {
        ParasiticScentEntity scent = ModEntities.SCENT.get().create(level);
        if (scent == null) {
            return;
        }
        scent.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        scent.setScentLevel(tier.scentLevel());
        scent.setTargetToKill(player, false);
        scent.setDieAfterKilling(true);
        scent.setCanFollow(true);
        level.addFreshEntity(scent);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(switch (state.getValue(TIER)) {
            case ONE -> ModItems.EVOLUTION_LURE_ONE.get();
            case TWO -> ModItems.EVOLUTION_LURE_TWO.get();
            case THREE -> ModItems.EVOLUTION_LURE_THREE.get();
            case FOUR -> ModItems.EVOLUTION_LURE_FOUR.get();
            case FIVE -> ModItems.EVOLUTION_LURE_FIVE.get();
            case SIX -> ModItems.EVOLUTION_LURE_SIX.get();
            case SEVEN -> ModItems.EVOLUTION_LURE_SEVEN.get();
            case EIGHT -> ModItems.EVOLUTION_LURE_EIGHT.get();
            case NINE -> ModItems.EVOLUTION_LURE_NINE.get();
            case TEN -> ModItems.EVOLUTION_LURE_TEN.get();
        });
    }

    public enum Tier implements StringRepresentable {
        ONE("one", 10, 10, 1),
        TWO("two", 20, 20, 1),
        THREE("three", 50, 50, 1),
        FOUR("four", 250, 500, 2),
        FIVE("five", 300, 4_000, 3),
        SIX("six", 600, 80_000, 4),
        SEVEN("seven", 600, 350_000, 5),
        EIGHT("eight", 1_200, 6_250_000, 6),
        NINE("nine", 1_200, 50_000_000, 7),
        TEN("ten", 1_200, 72_000_000, 8);

        private final String serializedName;
        private final int cooldownSeconds;
        private final int carcassReduction;
        private final int scentLevel;

        Tier(String serializedName, int cooldownSeconds, int carcassReduction, int scentLevel) {
            this.serializedName = serializedName;
            this.cooldownSeconds = cooldownSeconds;
            this.carcassReduction = carcassReduction;
            this.scentLevel = scentLevel;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public int cooldownSeconds() {
            return cooldownSeconds;
        }

        public int carcassReduction() {
            return carcassReduction;
        }

        public int scentLevel() {
            return scentLevel;
        }
    }
}
