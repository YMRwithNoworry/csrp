package alku.csrp.entity;

import alku.csrp.block.DispatcherNidusBlock;
import alku.csrp.block.entity.ParasiticCystBlockEntity;
import alku.csrp.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Original falling gore payload. Death-residue block branches remain disabled by user configuration. */
public final class GoreEntity extends Entity {
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(
            GoreEntity.class, EntityDataSerializers.INT);
    private static final int LIFETIME_TICKS = 200;
    private static final int CYST_THRESHOLD = 22;

    private int groundTicks;
    private byte goreType;
    private final List<String> legacyBlockNames = new ArrayList<>();
    private final List<Integer> legacyBlockCounts = new ArrayList<>();
    private final NonNullList<ItemStack> storedItems = NonNullList.create();
    private String entityName;

    public GoreEntity(EntityType<? extends GoreEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SKIN, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount % 20 == 0 && level().getFluidState(blockPosition()).is(FluidTags.WATER)) {
            discard();
            return;
        }

        Vec3 movement = getDeltaMovement();
        if (!isNoGravity()) {
            movement = movement.add(0.0D, -0.04D, 0.0D);
        }
        move(MoverType.SELF, movement);
        movement = movement.scale(0.98D);
        if (onGround()) {
            movement = new Vec3(movement.x * 0.7D, movement.y * 0.98D, movement.z * 0.7D);
            groundTicks++;
        }
        setDeltaMovement(movement);

        if (level().isClientSide && tickCount % 5 == 0 && !onGround()) {
            spawnTrailParticles();
        }
        if (tickCount >= LIFETIME_TICKS) {
            discard();
            return;
        }
        if (!level().isClientSide && groundTicks >= 1) {
            applyLandingPayload((ServerLevel) level());
            discard();
        }
    }

    private void applyLandingPayload(ServerLevel level) {
        if (goreType == 10) {
            BlockPos placed = placeReplaceable(level, ModBlocks.GLUTTONOUS_CYST.get().defaultBlockState());
            if (placed != null && level.getBlockEntity(placed) instanceof ParasiticCystBlockEntity cyst) {
                fillCyst(cyst);
            }
        } else if (goreType == 11) {
            DispatcherNidusBlock.tryPlace(level, blockPosition());
        }
        // Types 1-6, 12 and 111 were death-remains branches in 1.10.7. They intentionally
        // do not place blocks because this project disables all monster-death block generation.
    }

    private BlockPos placeReplaceable(ServerLevel level, BlockState state) {
        BlockPos origin = blockPosition();
        if (canReplaceWithPayload(level.getBlockState(origin))) {
            level.setBlockAndUpdate(origin, state);
            return origin;
        }
        int start = random.nextInt(4);
        for (int index = 0; index < 4; index++) {
            BlockPos candidate = switch ((start + index) % 4) {
                case 0 -> origin.north();
                case 1 -> origin.east();
                case 2 -> origin.west();
                default -> origin.south();
            };
            if (canReplaceWithPayload(level.getBlockState(candidate))) {
                level.setBlockAndUpdate(candidate, state);
                return candidate;
            }
        }
        return null;
    }

    private static boolean canReplaceWithPayload(BlockState state) {
        return state.isAir() || state.is(Blocks.DIRT);
    }

    private void fillCyst(ParasiticCystBlockEntity cyst) {
        int slot = 0;
        for (ItemStack stored : storedItems) {
            ItemStack remainder = stored.copy();
            while (!remainder.isEmpty() && slot < cyst.getContainerSize()) {
                int moved = Math.min(remainder.getCount(), remainder.getMaxStackSize());
                cyst.setItem(slot++, remainder.copyWithCount(moved));
                remainder.shrink(moved);
            }
        }
        if (slot == 0) {
            slot = fillLegacyCyst(cyst, slot);
        }
        if (slot > 0) {
            cyst.setChanged();
        }
    }

    private int fillLegacyCyst(ParasiticCystBlockEntity cyst, int slot) {
        for (int index = 0; index < legacyBlockNames.size() && slot < cyst.getContainerSize(); index++) {
            String encoded = legacyBlockNames.get(index);
            String name = encoded.contains(";") ? encoded.substring(0, encoded.indexOf(';')) : encoded;
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (id == null) {
                continue;
            }
            if (id.getNamespace().equals("srparasites")) {
                id = ResourceLocation.fromNamespaceAndPath("csrp", id.getPath());
            }
            Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR);
            if (block == Blocks.AIR || block.asItem() == net.minecraft.world.item.Items.AIR) {
                continue;
            }
            int remaining = index < legacyBlockCounts.size() ? legacyBlockCounts.get(index) : 1;
            while (remaining > 0 && slot < cyst.getContainerSize()) {
                int moved = Math.min(remaining, block.asItem().getDefaultMaxStackSize());
                cyst.setItem(slot++, new ItemStack(block, moved));
                remaining -= moved;
            }
        }
        return slot;
    }

    private void spawnTrailParticles() {
        if (getSkin() < 1 || getSkin() > 4) {
            return;
        }
        DustParticleOptions primary = switch (getSkin()) {
            case 1 -> dust(0.5F, 0.0F, 0.0F);
            case 2 -> dust(0.59F, 0.0F, 0.0F);
            case 3 -> dust(0.78F, 0.78F, 0.0F);
            default -> dust(0.12F, 0.02F, 0.02F);
        };
        DustParticleOptions secondary = switch (getSkin()) {
            case 3 -> dust(0.05F, 0.5F, 0.05F);
            default -> dust(0.5F, 0.0F, 0.0F);
        };
        addParticle(primary);
        if (getSkin() != 4) {
            addParticle(secondary);
        }
    }

    private static DustParticleOptions dust(float red, float green, float blue) {
        return new DustParticleOptions(new Vector3f(red, green, blue), 1.0F);
    }

    private void addParticle(DustParticleOptions particle) {
        level().addParticle(particle,
                getX() + (random.nextDouble() - 0.5D) * getBbWidth() * 2.0D,
                getY() + 0.5D + random.nextDouble() * getBbHeight(),
                getZ() + (random.nextDouble() - 0.5D) * getBbWidth() * 2.0D,
                random.nextGaussian() * 0.02D,
                random.nextGaussian() * 0.02D,
                random.nextGaussian() * 0.02D);
    }

    public void setType(byte type) {
        goreType = type;
        setSkin(type == 12 ? 4 : type == 111 ? 1 : type);
    }

    public byte getGoreType() {
        return goreType;
    }

    public void setMotion(double xSpeed, double ySpeed, double zSpeed, double capX, double capY) {
        xSpeed = Math.min(xSpeed, capX);
        ySpeed = Math.min(ySpeed, capY);
        zSpeed = Math.min(zSpeed, capX);
        setDeltaMovement(xSpeed * (Math.random() * 2.0D - 1.0D), ySpeed,
                zSpeed * (Math.random() * 2.0D - 1.0D));
    }

    public void setStoredItems(List<ItemStack> items) {
        storedItems.clear();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                storedItems.add(item.copy());
            }
        }
    }

    public int storedItemStacks() {
        return storedItems.size();
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setSkin(int skin) {
        entityData.set(SKIN, Mth.clamp(skin, 0, 10));
    }

    public int getSkin() {
        return entityData.get(SKIN);
    }

    public static int cystThreshold() {
        return CYST_THRESHOLD;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("parasitetype", getSkin());
        tag.putByte("bloodtype", goreType);
        if (entityName != null) {
            tag.putString("entityName", entityName);
        }
        ListTag items = new ListTag();
        for (ItemStack stack : storedItems) {
            items.add(stack.save(registryAccess()));
        }
        tag.put("Items", items);

        ListTag names = new ListTag();
        ListTag counts = new ListTag();
        for (int index = 0; index < legacyBlockNames.size(); index++) {
            CompoundTag name = new CompoundTag();
            name.putString("block" + index, legacyBlockNames.get(index));
            names.add(name);
            CompoundTag count = new CompoundTag();
            count.putInt("block" + index,
                    index < legacyBlockCounts.size() ? legacyBlockCounts.get(index) : 1);
            counts.add(count);
        }
        tag.put("srpinvblocksname", names);
        tag.put("srpinvblocksnumber", counts);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setSkin(tag.getInt("parasitetype"));
        goreType = tag.getByte("bloodtype");
        entityName = tag.contains("entityName") ? tag.getString("entityName") : null;
        storedItems.clear();
        for (Tag item : tag.getList("Items", Tag.TAG_COMPOUND)) {
            if (item instanceof CompoundTag compound) {
                ItemStack stack = ItemStack.parseOptional(registryAccess(), compound);
                if (!stack.isEmpty()) {
                    storedItems.add(stack);
                }
            }
        }

        legacyBlockNames.clear();
        legacyBlockCounts.clear();
        ListTag names = tag.getList("srpinvblocksname", Tag.TAG_COMPOUND);
        ListTag counts = tag.getList("srpinvblocksnumber", Tag.TAG_COMPOUND);
        if (names.size() == counts.size()) {
            for (int index = 0; index < names.size(); index++) {
                legacyBlockNames.add(names.getCompound(index).getString("block" + index));
                legacyBlockCounts.add(counts.getCompound(index).getInt("block" + index));
            }
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
