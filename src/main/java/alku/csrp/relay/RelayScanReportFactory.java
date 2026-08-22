package alku.csrp.relay;

import alku.csrp.util.NbtData;
import alku.csrp.entity.Parasite;
import alku.csrp.item.RelayModuleItem;
import alku.csrp.registry.ModItems;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.SrpWorldData;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Produces the same four families of immutable reports as the original Relay Tower. */
public final class RelayScanReportFactory {
    private static final int VECTOR_HALF_RANGE = 2_500;
    private static final EnumMap<RelayModuleItem.Kind, List<Tier>> PROFILES = profiles();

    private RelayScanReportFactory() {
    }

    public static List<ItemStack> createReports(
            ServerLevel level, BlockPos scanOrigin, RelayModuleItem.Kind kind) {
        return switch (kind) {
            case PHASE -> List.of(createPhaseReport(level));
            case VECTORS -> createVectorReports(level, scanOrigin);
            case DISLODGEMENT -> List.of(createDislodgementReport(level));
            default -> List.of(createScanReport(level, kind));
        };
    }

    public static boolean hasProfile(RelayModuleItem.Kind kind) {
        return kind == RelayModuleItem.Kind.PHASE || kind == RelayModuleItem.Kind.VECTORS
                || kind == RelayModuleItem.Kind.DISLODGEMENT || PROFILES.containsKey(kind);
    }

    private static ItemStack createScanReport(ServerLevel level, RelayModuleItem.Kind kind) {
        List<Tier> tiers = PROFILES.getOrDefault(kind, List.of());
        Map<Tier, Integer> counts = new LinkedHashMap<>();
        int mobTotal = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Mob) {
                mobTotal++;
                String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
                for (Tier tier : tiers) {
                    if (tier.entityIds.contains(id)) {
                        counts.merge(tier, 1, Integer::sum);
                    }
                }
            }
        }
        int parasiteTotal = counts.values().stream().mapToInt(Integer::intValue).sum();
        int nonParasites = Math.max(0, mobTotal - parasiteTotal);
        int divisor = gcd(parasiteTotal, nonParasites);

        CompoundTag tag = metadata(level);
        tag.putString("Dimension", dimensionName(level));
        tag.putInt("TotalMobs", mobTotal);
        tag.putInt("TotalParasites", parasiteTotal);
        tag.putInt("ShareTenths", mobTotal == 0 ? 0
                : Mth.floor(parasiteTotal * 1_000.0D / mobTotal));
        tag.putString("Ratio", parasiteTotal / divisor + ":" + nonParasites / divisor);
        ListTag tierList = new ListTag();
        for (Tier tier : tiers) {
            tierList.add(StringTag.valueOf(tier.id));
            tag.putInt("Tier_" + tier.id, counts.getOrDefault(tier, 0));
        }
        tag.put("Tiers", tierList);
        return report(ModItems.RELAY_SCAN_REPORT.get().getDefaultInstance(), tag);
    }

    private static ItemStack createPhaseReport(ServerLevel level) {
        SrpWorldData data = SrpWorldData.get(level);
        int parasites = 0;
        int coth = 0;
        int mobs = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                mobs++;
                if (living instanceof Parasite) {
                    parasites++;
                } else if (living.hasEffect(ModMobEffects.COTH.get())) {
                    coth++;
                }
            }
        }
        int phase = data.evolutionPhase();
        int points = data.evolutionPoints();
        int nextPoints = phase >= 10 ? EvolutionSystem.MAX_EVOLUTION_POINTS
                : EvolutionSystem.thresholdForPhase(phase + 1);
        int neededGenerationTicks = Math.max(0,
                EvolutionSystem.generationNeededTicks(data.generation(), phase) - data.generationTicks());

        CompoundTag tag = metadata(level);
        tag.putString("Dimension", dimensionName(level));
        tag.putInt("Phase", phase);
        tag.putInt("Points", points);
        tag.putInt("NextPoints", nextPoints);
        tag.putInt("ProgressTenths", nextPoints <= 0 ? 0
                : Mth.floor(Math.max(0.0D, Math.min(1_000.0D, points * 1_000.0D / nextPoints))));
        tag.putInt("Cooldown", data.cooldown(level));
        tag.putBoolean("CanGain", data.canGain());
        tag.putBoolean("CanLose", data.canLose());
        tag.putInt("MobCap", 40 + level.players().size() * 5);
        tag.putInt("Generation", data.generation());
        tag.putInt("GenerationTicks", neededGenerationTicks);
        tag.putInt("ParasiteCount", parasites);
        tag.putInt("CothCount", coth);
        tag.putInt("TotalMobs", mobs);
        return report(ModItems.PHASE_REPORT.get().getDefaultInstance(), tag);
    }

    private static List<ItemStack> createVectorReports(ServerLevel level, BlockPos origin) {
        List<SrpWorldData.VectorEntry> vectors = SrpWorldData.get(level).vectors().stream()
                .filter(vector -> Math.abs(vector.pos().getX() - origin.getX()) <= VECTOR_HALF_RANGE
                        && Math.abs(vector.pos().getZ() - origin.getZ()) <= VECTOR_HALF_RANGE)
                .toList();
        int total = Math.max(1, vectors.size());
        List<ItemStack> reports = new ArrayList<>(total);
        if (vectors.isEmpty()) {
            CompoundTag tag = vectorMetadata(level, origin, 1, total);
            tag.putBoolean("Found", false);
            reports.add(report(ModItems.VECTOR_MAP.get().getDefaultInstance(), tag));
            return reports;
        }
        for (int index = 0; index < vectors.size(); index++) {
            SrpWorldData.VectorEntry vector = vectors.get(index);
            CompoundTag tag = vectorMetadata(level, origin, index + 1, total);
            tag.putBoolean("Found", true);
            tag.putInt("VectorX", vector.pos().getX());
            tag.putInt("VectorY", vector.pos().getY());
            tag.putInt("VectorZ", vector.pos().getZ());
            tag.putInt("Radius", vector.radius());
            tag.putInt("Health", vector.health());
            tag.putInt("Distance", Mth.floor(Math.sqrt(vector.pos().distSqr(origin))));
            reports.add(report(ModItems.VECTOR_MAP.get().getDefaultInstance(), tag));
        }
        return reports;
    }

    private static CompoundTag vectorMetadata(ServerLevel level, BlockPos origin, int index, int total) {
        CompoundTag tag = metadata(level);
        tag.putString("Dimension", dimensionName(level));
        tag.putInt("CenterX", origin.getX());
        tag.putInt("CenterZ", origin.getZ());
        tag.putInt("Index", index);
        tag.putInt("Total", total);
        return tag;
    }

    private static ItemStack createDislodgementReport(ServerLevel level) {
        CompoundTag tag = metadata(level);
        tag.putString("Dimension", dimensionName(level));
        ListTag events = new ListTag();
        for (SrpWorldData.DislodgmentCode code : SrpWorldData.get(level).activeDislodgmentCodes(level)) {
            CompoundTag event = new CompoundTag();
            event.putInt("Code", code.code());
            event.putInt("Value", code.value());
            event.putInt("Seconds", (int) Math.max(0L,
                    (code.expiresAt() - level.getGameTime() + 19L) / 20L));
            event.putInt("Threat", threat(code.code()));
            events.add(event);
        }
        tag.put("Events", events);
        return report(ModItems.DISLODGEMENT_REPORT.get().getDefaultInstance(), tag);
    }

    private static CompoundTag metadata(ServerLevel level) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("PrintDay", (int) (level.getDayTime() / 24_000L));
        tag.putInt("PrintTime", (int) (level.getDayTime() % 24_000L));
        return tag;
    }

    private static ItemStack report(ItemStack stack, CompoundTag data) {
        NbtData.update(stack, tag -> tag.merge(data));
        return stack;
    }

    private static String dimensionName(ServerLevel level) {
        if (level.dimension() == Level.NETHER) {
            return "-1";
        }
        if (level.dimension() == Level.OVERWORLD) {
            return "0";
        }
        if (level.dimension() == Level.END) {
            return "1";
        }
        return level.dimension().location().toString();
    }

    private static int gcd(int first, int second) {
        first = Math.abs(first);
        second = Math.abs(second);
        while (second != 0) {
            int remainder = first % second;
            first = second;
            second = remainder;
        }
        return first == 0 ? 1 : first;
    }

    private static int threat(int code) {
        return switch (code) {
            case 1, 4, 8, 10, 13, 17, 18, 22 -> 2;
            case 3, 11, 12, 14, 21, 25 -> 3;
            default -> 1;
        };
    }

    private static EnumMap<RelayModuleItem.Kind, List<Tier>> profiles() {
        EnumMap<RelayModuleItem.Kind, List<Tier>> profiles = new EnumMap<>(RelayModuleItem.Kind.class);
        profiles.put(RelayModuleItem.Kind.INBORN, List.of(Tier.INBORN));
        profiles.put(RelayModuleItem.Kind.ASSIMILATED, List.of(Tier.ASSIMILATED));
        profiles.put(RelayModuleItem.Kind.ASSIMARA, List.of(Tier.ASSIMARA));
        profiles.put(RelayModuleItem.Kind.HIJACKED, List.of(Tier.HIJACKED));
        profiles.put(RelayModuleItem.Kind.FERAL, List.of(Tier.FERAL));
        profiles.put(RelayModuleItem.Kind.CRUDE, List.of(Tier.CRUDE));
        profiles.put(RelayModuleItem.Kind.PRIMITIVE, List.of(Tier.PRIMITIVE));
        profiles.put(RelayModuleItem.Kind.ADAPTED, List.of(Tier.ADAPTED));
        profiles.put(RelayModuleItem.Kind.NEXUS, List.of(Tier.NEXUS));
        profiles.put(RelayModuleItem.Kind.DETERRENT, List.of(Tier.DETERRENT));
        profiles.put(RelayModuleItem.Kind.PURE, List.of(Tier.PURE));
        profiles.put(RelayModuleItem.Kind.PREEMINENT, List.of(Tier.PREEMINENT));
        profiles.put(RelayModuleItem.Kind.ANCIENT, List.of(Tier.ANCIENT));
        profiles.put(RelayModuleItem.Kind.DERIVED, List.of(Tier.DERIVED));
        profiles.put(RelayModuleItem.Kind.DESMOID,
                List.of(Tier.INBORN, Tier.ASSIMARA, Tier.ASSIMILATED, Tier.HIJACKED));
        profiles.put(RelayModuleItem.Kind.ESCHAR, List.of(Tier.FERAL, Tier.CRUDE, Tier.PRIMITIVE));
        profiles.put(RelayModuleItem.Kind.RESISTANCE,
                List.of(Tier.ADAPTED, Tier.NEXUS, Tier.DETERRENT));
        profiles.put(RelayModuleItem.Kind.IDEAL,
                List.of(Tier.PURE, Tier.PREEMINENT, Tier.DERIVED, Tier.ANCIENT));
        profiles.put(RelayModuleItem.Kind.ORIGIN, List.of(Tier.values()));
        return profiles;
    }

    private enum Tier {
        INBORN("inborn", "carrier_heavy", "carrier_light", "carrier_flying", "buglin", "rupter",
                "movingflesh", "worker", "mangler", "gnat", "lice"),
        ASSIMILATED("assimilated", "sim_bigspider", "sim_squid", "sim_human", "sim_cow", "sim_sheep",
                "sim_wolf", "sim_pig", "sim_villager", "sim_adventurer", "sim_horse", "sim_bear",
                "sim_enderman", "sim_dragone", "sim_sheephead", "sim_wolfhead", "sim_cowhead",
                "sim_pighead", "sim_villagerhead", "sim_horsehead", "sim_humanhead", "sim_endermanhead",
                "sim_dragonehead", "sim_adventurerhead"),
        ASSIMARA("assimara", "mar_enderman", "mar_cow", "mar_villager", "mar_human", "mar_sheep", "mar_bear"),
        HIJACKED("hijacked", "hi_blaze", "hi_golem", "hi_skeleton"),
        FERAL("feral", "fer_bear", "fer_cow", "fer_enderman", "fer_horse", "fer_human", "fer_pig",
                "fer_sheep", "fer_villager", "fer_wolf"),
        CRUDE("crude", "incompleteform_small", "incompleteform_medium", "host", "hostii", "heed", "crux",
                "crux_incomplete", "thrall", "dredge", "airscrew", "carrier_worm"),
        PRIMITIVE("primitive", "pri_longarms", "pri_manducater", "pri_reeker", "pri_yelloweye",
                "pri_summoner", "pri_bolster", "pri_tozoon", "pri_arachnida", "pri_devourer",
                "pri_vermin", "pri_viscera", "pri_burrower"),
        ADAPTED("adapted", "ada_longarms", "ada_manducater", "ada_reeker", "ada_yelloweye",
                "ada_summoner", "ada_bolster", "ada_tozoon", "ada_arachnida", "ada_devourer",
                "ada_vermin", "ada_viscera", "ada_burrower"),
        NEXUS("nexus", "beckon_si", "beckon_sii", "beckon_siii", "beckon_siv", "dispatcherten",
                "dispatcher_si", "dispatcher_sii", "dispatcher_siii", "dispatcher_siv", "rooterball",
                "rooter_si", "rooter_sii", "rooter_siii", "rooter_siv"),
        DETERRENT("deterrent", "kyphosis", "sentry", "seizer", "worm"),
        PURE("pure", "overseer", "vigilante", "warden", "bomber_light", "marauder", "monarch", "grunt"),
        PREEMINENT("preeminent", "bomber_heavy", "wraith", "bogle", "haunter", "carrier_colony",
                "succor", "seeker", "architect"),
        DERIVED("derived", "draconite", "kirin"),
        ANCIENT("ancient", "anc_dreadnaut", "anc_overlord", "anc_pod", "anc_dreadnaut_ten");

        private final String id;
        private final Set<String> entityIds;

        Tier(String id, String... entityIds) {
            this.id = id;
            this.entityIds = Set.of(entityIds);
        }
    }
}
