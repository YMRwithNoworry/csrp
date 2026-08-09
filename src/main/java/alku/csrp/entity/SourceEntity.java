package alku.csrp.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Original unused Source charge bar entity. Its completed attack is intentionally empty. */
public final class SourceEntity extends Entity {
    private final ServerBossEvent bossEvent = new ServerBossEvent(getName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private byte sourceType;
    private float total = 100.0F;
    private float charging;

    public SourceEntity(EntityType<? extends SourceEntity> type, Level level) {
        super(type, level);
        bossEvent.setDarkenScreen(false);
    }

    @Override
    public void tick() {
        super.tick();
        bossEvent.setProgress(Mth.clamp(charging / total, 0.0F, 1.0F));
        if (!level().isClientSide && tickCount % 20 == 0) {
            charging++;
            if (charging > total) {
                attack();
            }
        }
    }

    private void attack() {
        if (charging > 200.0F) {
            discard();
        }
    }

    @Override
    public Component getName() {
        return hasCustomName() ? getCustomName() : Component.literal("The Source");
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // SRP 1.10.7 intentionally does not persist Source progress or type.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // SRP 1.10.7 intentionally does not persist Source progress or type.
    }

    public byte getSourceType() {
        return sourceType;
    }

    public float getTotal() {
        return total;
    }

    public float getCharging() {
        return charging;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
