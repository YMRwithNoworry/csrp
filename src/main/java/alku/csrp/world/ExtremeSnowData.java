package alku.csrp.world;

import alku.csrp.Csrp;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Port of SRP 1.10.9 {@code world/ExtremeSnowData}: the per-dimension state behind
 * {@code /srp_extremesnow}.
 *
 * <p>The original kept one instance per dimension in the per-world storage under
 * {@code srp_extreme_snow_<dim>}; 26.3's {@link net.minecraft.world.level.storage.DimensionDataStorage}
 * is already per level, so the name is just {@code srp_extreme_snow}.  The five fields and their NBT
 * keys are the original's.</p>
 *
 * <p>One deliberate difference: the original's setters never marked the data dirty, so the state was
 * lost on save/exit.  These call {@link #setDirty()} so a configured blizzard survives a restart.</p>
 */
public final class ExtremeSnowData extends SavedData {
    /** {@code ExtremeSnowData.KEY}. */
    private static final String DATA_NAME = "srp_extreme_snow";
    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath(Csrp.MODID, DATA_NAME);
    private static final Codec<ExtremeSnowData> CODEC =
            CompoundTag.CODEC.xmap(ExtremeSnowData::load, ExtremeSnowData::save);
    private static final SavedDataType<ExtremeSnowData> TYPE =
            new SavedDataType<>(DATA_ID, ExtremeSnowData::new, CODEC);

    private boolean enabled = false;
    private float intensity = 1.0F;
    private boolean forceAnywhere = true;
    private float windDeg = 30.0F;
    private float windSpeed = 0.5F;

    public static ExtremeSnowData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private static ExtremeSnowData load(CompoundTag tag) {
        ExtremeSnowData data = new ExtremeSnowData();
        data.enabled = tag.getBooleanOr("enabled", false);
        data.intensity = tag.getFloatOr("intensity", 1.0F);
        data.forceAnywhere = tag.getBooleanOr("forceAnywhere", true);
        data.windDeg = tag.getFloatOr("windDeg", 30.0F);
        data.windSpeed = tag.getFloatOr("windSpeed", 0.5F);
        return data;
    }

    private CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("enabled", enabled);
        tag.putFloat("intensity", intensity);
        tag.putBoolean("forceAnywhere", forceAnywhere);
        tag.putFloat("windDeg", windDeg);
        tag.putFloat("windSpeed", windSpeed);
        return tag;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
        setDirty();
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float value) {
        intensity = Mth.clamp(value, 0.0F, 1.0F);
        setDirty();
    }

    public boolean isForceAnywhere() {
        return forceAnywhere;
    }

    public void setForceAnywhere(boolean value) {
        forceAnywhere = value;
        setDirty();
    }

    public float getWindDeg() {
        return windDeg;
    }

    public void setWindDeg(float value) {
        windDeg = value % 360.0F;
        setDirty();
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(float value) {
        windSpeed = Mth.clamp(value, 0.0F, 1.0F);
        setDirty();
    }
}
