package alku.csrp.celestial;

import alku.csrp.celestial.network.CelestialStatePayload;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.registry.ModSounds;
import alku.csrp.world.SrpWorldData;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CelestialSystem {
    public static final String DARK_DAYS = "dark_days";
    public static final int DARK_DAYS_DURATION_TICKS = 6000;
    public static final int DARK_DAYS_INTRO_DELAY_TICKS = 160;
    public static final int DARK_DAYS_OUTRO_DELAY_TICKS = 200;
    private static final Random RANDOM = new Random();
    private static final String WITNESSED_KEY = "csrpWitnessed";
    private static final int HALF_EVENT_COUNT = 8;
    private static final int ALL_EVENT_COUNT = 16;
    private static final ResourceLocation COLUMBUS = ResourceLocation.fromNamespaceAndPath(
            alku.csrp.Csrp.MODID, "columbus");
    private static final ResourceLocation STOLAS = ResourceLocation.fromNamespaceAndPath(
            alku.csrp.Csrp.MODID, "stolas");

    private CelestialSystem() {
    }

    public static void tick(ServerLevel level) {
        if (!level.dimensionType().hasSkyLight()) return;
        CelestialWorldData data = CelestialWorldData.get(level);
        tickDarkDays(level, data);
        if (!isDarkDaysPendingOrActive(data)) rollNight(level, data);
        rollDarkDays(level, data);
        applyNightStartEffects(level, data);
        if (level.getGameTime() % 100L == 0L) {
            updateWitnessedEvents(level);
        }
    }

    private static void updateWitnessedEvents(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            Set<String> witnessed = loadWitnessed(player);
            int before = witnessed.size();
            for (String id : visible(level)) {
                if (!id.equals(DARK_DAYS)) {
                    witnessed.add(id);
                }
            }
            if (witnessed.size() == before) {
                continue;
            }
            saveWitnessed(player, witnessed);
            award(player, COLUMBUS, "witness_half", HALF_EVENT_COUNT);
            award(player, STOLAS, "witness_all", ALL_EVENT_COUNT);
        }
    }

    private static Set<String> loadWitnessed(ServerPlayer player) {
        Set<String> result = new HashSet<>();
        ListTag list = player.getPersistentData().getList(WITNESSED_KEY, Tag.TAG_STRING);
        for (Tag tag : list) {
            result.add(tag.getAsString());
        }
        return result;
    }

    private static void saveWitnessed(ServerPlayer player, Set<String> witnessed) {
        ListTag list = new ListTag();
        for (String id : witnessed) {
            list.add(StringTag.valueOf(id));
        }
        player.getPersistentData().put(WITNESSED_KEY, list);
    }

    private static void award(ServerPlayer player, ResourceLocation advancementId,
            String criterion, int threshold) {
        if (loadWitnessed(player).size() < threshold) {
            return;
        }
        AdvancementHolder holder = player.server.getAdvancements().get(advancementId);
        if (holder != null) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    private static void rollNight(ServerLevel level, CelestialWorldData data) {
        long day = level.getDayTime() / 24000L;
        if (data.nightIndex() == day) return;
        data.nightIndex(day);
        data.mutableActive().clear();
        int phase = SrpWorldData.get(level).evolutionPhase();
        long seed = level.getSeed() ^ day * 918273L;
        for (CelestialDefinition definition : CelestialCatalog.ALL) {
            if (definition.id().equals(DARK_DAYS) || !definition.allowsPhase(phase)) continue;
            RANDOM.setSeed(seed + definition.id().hashCode() * 31L);
            if (RANDOM.nextFloat() <= definition.chance()) data.mutableActive().add(definition.id());
        }
        data.changed();
        sync(level);
    }

    private static void rollDarkDays(ServerLevel level, CelestialWorldData data) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        long day = level.getDayTime() / 24000L;
        if (time < 800 || time > 999 || data.darkDaysLastRollDay() == day
                || isDarkDaysPendingOrActive(data)) return;
        data.darkDaysLastRollDay(day);
        CelestialDefinition definition = CelestialCatalog.get(DARK_DAYS);
        if (!definition.allowsPhase(SrpWorldData.get(level).evolutionPhase())) return;
        RANDOM.setSeed(level.getSeed() ^ day * 918273L ^ DARK_DAYS.hashCode() * 31L);
        if (RANDOM.nextFloat() <= definition.chance()) {
            data.mutableActive().clear();
            data.mutableForced().clear();
            long dayBase = level.getDayTime() - time;
            long delay = dayBase + 1000L - level.getDayTime();
            data.darkDaysStartTime(level.getGameTime() + Math.max(0L, delay));
            data.darkDaysEndTime(-1);
            data.darkDaysEndingSoundPlayed(false);
            level.setWeatherParameters(6400, 0, false, false);
            data.changed();
            play(level, ModSounds.DARK_DAYS_START.get(), 1.0F);
            sync(level);
        }
    }

    private static void tickDarkDays(ServerLevel level, CelestialWorldData data) {
        long now = level.getGameTime();
        if (data.darkDaysStartTime() >= 0 && now >= data.darkDaysStartTime()) {
            data.darkDaysStartTime(-1);
            data.mutableActive().clear();
            data.mutableForced().clear();
            data.mutableForced().add(DARK_DAYS);
            data.darkDaysEndTime(now + DARK_DAYS_DURATION_TICKS);
            data.changed();
            sync(level);
        }
        if (!isActive(level, DARK_DAYS) || data.darkDaysEndTime() < 0) return;
        if (!data.darkDaysEndingSoundPlayed()
                && now >= data.darkDaysEndTime() - DARK_DAYS_OUTRO_DELAY_TICKS) {
            data.darkDaysEndingSoundPlayed(true);
            play(level, ModSounds.DARK_DAYS_ENDING.get(), 1.0F);
        }
        if (now >= data.darkDaysEndTime()) finishDarkDays(level, data);
    }

    private static void applyNightStartEffects(ServerLevel level, CelestialWorldData data) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        long day = level.getDayTime() / 24000L;
        if (time < 13000L || time > 23000L || data.lastEffectNightIndex() == day) return;
        data.lastEffectNightIndex(day);
        if (!isActive(level, "twenty_seven")) return;
        for (var entity : level.getAllEntities()) {
            if (entity instanceof Parasite && entity instanceof LivingEntity living && living.isAlive()) {
                living.addEffect(new MobEffectInstance(ModMobEffects.RAGE, 12000, 1, false, false));
            }
        }
    }

    public static void startDarkDays(ServerLevel level) {
        if (!level.dimensionType().hasSkyLight()) return;
        CelestialWorldData data = CelestialWorldData.get(level);
        data.mutableActive().clear();
        data.mutableForced().clear();
        data.darkDaysStartTime(level.getGameTime() + DARK_DAYS_INTRO_DELAY_TICKS);
        data.darkDaysEndTime(-1);
        data.darkDaysEndingSoundPlayed(false);
        level.setWeatherParameters(6400, 0, false, false);
        data.changed();
        play(level, ModSounds.DARK_DAYS_START.get(), 1.0F);
        sync(level);
    }

    public static void stopDarkDays(ServerLevel level) {
        CelestialWorldData data = CelestialWorldData.get(level);
        if (data.darkDaysStartTime() >= 0) {
            finishDarkDays(level, data);
            return;
        }
        if (!isActive(level, DARK_DAYS)) return;
        data.darkDaysEndTime(level.getGameTime() + DARK_DAYS_OUTRO_DELAY_TICKS);
        if (!data.darkDaysEndingSoundPlayed()) {
            data.darkDaysEndingSoundPlayed(true);
            play(level, ModSounds.DARK_DAYS_ENDING.get(), 1.0F);
        }
        sync(level);
    }

    private static void finishDarkDays(ServerLevel level, CelestialWorldData data) {
        data.mutableActive().remove(DARK_DAYS);
        data.mutableForced().remove(DARK_DAYS);
        data.darkDaysStartTime(-1);
        data.darkDaysEndTime(-1);
        data.darkDaysEndingSoundPlayed(false);
        awardDarkDaysSurvivors(level);
        data.changed();
        sync(level);
    }

    private static void awardDarkDaysSurvivors(ServerLevel level) {
        net.minecraft.advancements.AdvancementHolder holder = level.getServer().getAdvancements()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        alku.csrp.Csrp.MODID, "dark_days"));
        if (holder == null) {
            return;
        }
        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            if (player.isAlive()) {
                player.getAdvancements().award(holder, "survived_dark_days");
            }
        }
    }

    public static boolean isActive(ServerLevel level, String id) {
        CelestialWorldData data = CelestialWorldData.get(level);
        return data.active().contains(id) || data.forced().contains(id);
    }

    public static Set<String> visible(ServerLevel level) {
        CelestialWorldData data = CelestialWorldData.get(level);
        if (data.active().contains(DARK_DAYS) || data.forced().contains(DARK_DAYS)) return Set.of(DARK_DAYS);
        Set<String> result = new LinkedHashSet<>(data.active());
        result.addAll(data.forced());
        return Set.copyOf(result);
    }

    public static boolean toggleForced(ServerLevel level, String id) {
        CelestialWorldData data = CelestialWorldData.get(level);
        boolean enabled;
        if (DARK_DAYS.equals(id)) {
            if (isDarkDaysPendingOrActive(data)) {
                stopDarkDays(level);
                return false;
            }
            startDarkDays(level);
            return true;
        }
        if (data.mutableForced().remove(id)) {
            data.mutableActive().remove(id);
            enabled = false;
        } else enabled = data.mutableForced().add(id);
        data.changed();
        sync(level);
        return enabled;
    }

    public static void forceAll(ServerLevel level) {
        CelestialWorldData data = CelestialWorldData.get(level);
        data.mutableForced().clear();
        data.mutableActive().remove(DARK_DAYS);
        data.darkDaysStartTime(-1);
        data.darkDaysEndTime(-1);
        data.darkDaysEndingSoundPlayed(false);
        CelestialCatalog.ALL.stream().map(CelestialDefinition::id)
                .filter(id -> !id.equals(DARK_DAYS)).forEach(data.mutableForced()::add);
        data.changed();
        sync(level);
    }

    public static void clearForced(ServerLevel level) {
        CelestialWorldData data = CelestialWorldData.get(level);
        data.mutableForced().clear();
        data.darkDaysStartTime(-1);
        data.darkDaysEndTime(-1);
        data.darkDaysEndingSoundPlayed(false);
        data.changed();
        sync(level);
    }

    public static void sync(ServerLevel level) {
        CelestialWorldData data = CelestialWorldData.get(level);
        CelestialStatePayload payload = new CelestialStatePayload(visible(level), data.nightIndex(), level.getGameTime());
        level.players().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    public static void sync(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        CelestialWorldData data = CelestialWorldData.get(level);
        PacketDistributor.sendToPlayer(player,
                new CelestialStatePayload(visible(level), data.nightIndex(), level.getGameTime()));
    }

    private static boolean isDarkDaysPendingOrActive(CelestialWorldData data) {
        return data.darkDaysStartTime() >= 0 || data.active().contains(DARK_DAYS)
                || data.forced().contains(DARK_DAYS);
    }

    private static void play(ServerLevel level, net.minecraft.sounds.SoundEvent sound, float volume) {
        level.players().forEach(player -> player.playNotifySound(sound, SoundSource.AMBIENT, volume, 1.0F));
    }
}
