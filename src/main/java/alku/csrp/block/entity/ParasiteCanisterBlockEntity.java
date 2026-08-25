package alku.csrp.block.entity;

import alku.csrp.registry.ModBlockEntities;
import alku.csrp.registry.ModItems;
import alku.csrp.world.EvolutionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 活体寄生囊肿容器。对应原版 TileEntityCanister：消化内部物品换取进化点
 * （约 25 秒后每秒 1 个，每件 +2 点），清空后囊肿自行消失。
 * 原版物品来源是寄生体吃掉的方块；本项目以生物质近似填充。
 */
public final class ParasiteCanisterBlockEntity extends BlockEntity implements Container {
    public static final int START_DELAY = 600;
    public static final int SLOT_COUNT = 9;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int age;

    public ParasiteCanisterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARASITE_CANISTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(blockEntity instanceof ParasiteCanisterBlockEntity cyst) || level.isClientSide) {
            return;
        }
        cyst.age++;
        if (cyst.age <= START_DELAY || cyst.age % 20 != 0) {
            return;
        }
        if (cyst.consumeOne()) {
            EvolutionSystem.addPoints((ServerLevel) level, 2, EvolutionSystem.PointSource.CYST);
            if (level.random.nextFloat() < 0.5F) {
                ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL,
                        pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D,
                        4, 0.2D, 0.2D, 0.2D, 0.02D);
            }
            return;
        }
        level.removeBlock(pos, false);
        ((ServerLevel) level).sendParticles(ParticleTypes.POOF,
                pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D, 8, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    private boolean consumeOne() {
        for (int index = 0; index < items.size(); index++) {
            ItemStack stack = items.get(index);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                setChanged();
                return true;
            }
        }
        return false;
    }

    /** 原版 spawnCyst 的落地放置：找地面放下囊肿并填充生物质。 */
    public static boolean placeFromDespawn(ServerLevel level, BlockPos origin) {
        BlockPos ground = origin;
        for (int depth = 0; depth < 6 && !level.getBlockState(ground.below()).isFaceSturdy(level,
                ground.below(), net.minecraft.core.Direction.UP); depth++) {
            ground = ground.below();
        }
        BlockState existing = level.getBlockState(ground);
        if (!existing.isAir() && !existing.canBeReplaced()) {
            return false;
        }
        level.setBlock(ground, alku.csrp.registry.ModBlocks.CANISTER_ACTIVE.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(ground) instanceof ParasiteCanisterBlockEntity cyst)) {
            return true;
        }
        RandomSource random = level.getRandom();
        int flesh = 2 + random.nextInt(4);
        cyst.items.set(0, new ItemStack(ModItems.ASSIMILATED_FLESH.get(), flesh));
        if (random.nextInt(2) == 0) {
            cyst.items.set(1, new ItemStack(ModItems.HIVE_SCRAP.get(), 1 + random.nextInt(2)));
        }
        cyst.setChanged();
        level.playSound(null, ground, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 0.7F);
        level.sendParticles(ParticleTypes.PORTAL,
                ground.getX() + 0.5D, ground.getY() + 0.5D, ground.getZ() + 0.5D, 12, 0.3D, 0.3D, 0.3D, 0.02D);
        return true;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack split = ContainerHelper.removeItem(items, index, count);
        if (!split.isEmpty()) {
            setChanged();
        }
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = ContainerHelper.takeItem(items, index);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        stack.limitSize(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Age", age);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        age = tag.getInt("Age");
    }
}
