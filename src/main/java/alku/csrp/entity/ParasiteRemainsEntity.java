package alku.csrp.entity;

import alku.csrp.infection.BlockInfestation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ParasiteRemainsEntity extends Entity {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(
            ParasiteRemainsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> SOURCE_TYPE = SynchedEntityData.defineId(
            ParasiteRemainsEntity.class, EntityDataSerializers.STRING);
    private static final int LIFETIME_TICKS = 20 * 20;

    public ParasiteRemainsEntity(EntityType<? extends ParasiteRemainsEntity> type, Level level) {
        super(type, level);
    }

    public void initialize(LivingEntity source, int variant, Vec3 velocity) {
        entityData.set(VARIANT, variant);
        entityData.set(SOURCE_TYPE, BuiltInRegistries.ENTITY_TYPE.getKey(source.getType()).toString());
        setDeltaMovement(velocity);
        hasImpulse = true;
    }

    public int variant() {
        return entityData.get(VARIANT);
    }

    public ResourceLocation sourceTypeId() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(SOURCE_TYPE));
        return id == null ? ResourceLocation.withDefaultNamespace("pig") : id;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 movement = getDeltaMovement();
        if (!isNoGravity()) {
            movement = movement.add(0.0D, -0.04D, 0.0D);
        }
        move(MoverType.SELF, movement);

        double x = movement.x;
        double y = movement.y;
        double z = movement.z;
        if (horizontalCollision) {
            x *= -0.42D;
            z *= -0.42D;
        }
        if (onGround()) {
            y = Math.abs(y) > 0.08D ? -y * 0.34D : 0.0D;
            x *= 0.72D;
            z *= 0.72D;
        } else {
            x *= 0.98D;
            z *= 0.98D;
        }
        setDeltaMovement(x, y * 0.98D, z);

        if (!level().isClientSide && tickCount >= LIFETIME_TICKS && level() instanceof ServerLevel serverLevel) {
            contaminate(serverLevel);
            discard();
        }
    }

    private void contaminate(ServerLevel level) {
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos center = blockPosition();
        candidates.add(center.below());
        candidates.add(center.north());
        candidates.add(center.south());
        candidates.add(center.east());
        candidates.add(center.west());
        Collections.shuffle(candidates, new java.util.Random(random.nextLong()));
        int converted = 0;
        for (BlockPos candidate : candidates) {
            if (BlockInfestation.convert(level, candidate, 1) && ++converted >= 2) {
                break;
            }
        }
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!(source.getEntity() instanceof Player player)) {
            return false;
        }
        if (!level().isClientSide) {
            if (player.getMainHandItem().is(ItemTags.AXES) && random.nextFloat() < 0.4F
                    && level() instanceof ServerLevel serverLevel) {
                dropSourceLoot(serverLevel, player);
            }
            discard();
        }
        return true;
    }

    private void dropSourceLoot(ServerLevel level, Player player) {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(SOURCE_TYPE));
        if (id == null) {
            return;
        }
        EntityType<?> sourceType = BuiltInRegistries.ENTITY_TYPE.get(id);
        Entity created = sourceType.create(level);
        if (!(created instanceof LivingEntity source)) {
            return;
        }
        source.setPos(position());
        var damageSource = level.damageSources().playerAttack(player);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, source)
                .withParameter(LootContextParams.ORIGIN, position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, player)
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
                .create(LootContextParamSets.ENTITY);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(source.getLootTable());
        List<ItemStack> drops = table.getRandomItems(params);
        for (ItemStack drop : drops) {
            spawnAtLocation(drop.copy());
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(VARIANT, 0);
        builder.define(SOURCE_TYPE, "minecraft:pig");
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(VARIANT, Mth.clamp(tag.getInt("variant"), 0, 255));
        entityData.set(SOURCE_TYPE, tag.getString("source_type"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("variant", entityData.get(VARIANT));
        tag.putString("source_type", entityData.get(SOURCE_TYPE));
    }
}
