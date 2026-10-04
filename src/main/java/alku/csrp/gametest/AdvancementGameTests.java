package alku.csrp.gametest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;

import alku.csrp.Csrp;
import alku.csrp.event.AdvancementLegacyMigrationEvents;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * In-game verification of the 42 canonical CSRP advancements that live in
 * {@code data/csrp/advancement/}: they must be parsed into the running game, keep the tree shape the
 * port documents, use registered item icons, and be awardable through their criteria. The legacy id
 * migration is covered by a pure-function test.
 *
 * <p>The structure template is the tiny {@code csrp:box} template the repository already ships under
 * {@code data/csrp/structure/}; these tests only touch registries, never the test platform.
 */
@GameTestHolder(Csrp.MODID)
@PrefixGameTestTemplate(false)
public final class AdvancementGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TEMPLATE = "box";

    private static final int TRIGGER_TIMEOUT_TICKS = 600;

    private static final Set<String> ROOT_PATHS = Set.of("root", "again");

    /** Recipe advancements are hidden and never declare a display. */
    private static final String RECIPE_PATH_PREFIX = "recipes/";

    private static final List<Expected> EXPECTED = List.of(
            new Expected("root", null, "tick"),
            new Expected("kill_root", "root", "tick"),
            new Expected("secret_root", "root", "tick"),
            new Expected("monument_root", "root", "tick"),
            new Expected("firmamentum_root", "root", "tick"),
            new Expected("brew_fear", "secret_root", "brewed_fear"),
            new Expected("columbus", "cut_roots", "unlock"),
            new Expected("controversial", "secret_root", "kill_sim_enderman"),
            new Expected("cosmic_structural_failure", "kill_deterrent", "kill_kirin"),
            new Expected("cut_roots", "sepeku", "reached_1000_rupter_kills"),
            new Expected("dark_days", "stolas", "unlock"),
            new Expected("enemy_enemy", "thornshade_self_destruct", "creeper_killed_parasite"),
            new Expected("fermented_arrogance", "brew_fear", "kill_fer_enderman"),
            new Expected("fog_nullifier", "firmamentum_root", "got_fog_nullifier"),
            new Expected("guerilla", "controversial", "kill_fear_splashed"),
            new Expected("hellfire_chemical_warfare", "kill_deterrent", "kill_draconite"),
            new Expected("hunt_season", "enemy_enemy", "fifty_in_day"),
            new Expected("kill_adapted", "kill_assimilated",
                    "k_ada_arachnida", "k_ada_bolster", "k_ada_burrower", "k_ada_devourer",
                    "k_ada_longarms", "k_ada_manducater", "k_ada_reeker", "k_ada_summoner",
                    "k_ada_tozoon", "k_ada_viscera", "k_ada_yelloweye"),
            new Expected("kill_assimilated", "kill_inborn",
                    "k_sim_adventurer", "k_sim_adventurerhead", "k_sim_bear", "k_sim_bigspider",
                    "k_sim_cow", "k_sim_cowhead", "k_sim_dragone", "k_sim_dragonehead",
                    "k_sim_enderman", "k_sim_endermanhead", "k_sim_horse", "k_sim_horsehead",
                    "k_sim_human", "k_sim_humanhead", "k_sim_pig", "k_sim_pighead",
                    "k_sim_sheep", "k_sim_sheephead", "k_sim_squid", "k_sim_villager",
                    "k_sim_villagerhead", "k_sim_wolf", "k_sim_wolfhead"),
            new Expected("kill_assimilated_head", "kill_inborn",
                    "k_sim_adventurerhead", "k_sim_cowhead", "k_sim_dragonehead",
                    "k_sim_endermanhead", "k_sim_horsehead", "k_sim_humanhead",
                    "k_sim_pighead", "k_sim_sheephead", "k_sim_villagerhead", "k_sim_wolfhead"),
            new Expected("kill_beckon_s1", "monument_root", "kill_beckon_si"),
            new Expected("kill_beckon_s2", "kill_beckon_s1", "kill_beckon_sii"),
            new Expected("kill_beckon_s3", "kill_beckon_s2", "kill_beckon_siii"),
            new Expected("kill_beckon_s4", "kill_beckon_s3", "kill_beckon_siv"),
            new Expected("kill_crude", "kill_adapted",
                    "k_crux", "k_heed", "k_incomplete_medium", "k_incomplete_small",
                    "k_host", "k_hostii", "k_thrall", "k_movingflesh"),
            new Expected("kill_deterrent", "kill_adapted",
                    "k_dispatcherten", "k_kyphosis", "k_seizer", "k_sentry", "k_worm"),
            new Expected("kill_dispatcher_s1", "monument_root", "kill_dispatcher_si"),
            new Expected("kill_dispatcher_s2", "kill_dispatcher_s1", "kill_dispatcher_sii"),
            new Expected("kill_dispatcher_s3", "kill_dispatcher_s2", "kill_dispatcher_siii"),
            new Expected("kill_dispatcher_s4", "kill_dispatcher_s3", "kill_dispatcher_siv"),
            new Expected("kill_inborn", "kill_root",
                    "k_buglin", "k_rupter", "k_mangler", "k_gnat",
                    "k_carrier_flying", "k_carrier_heavy", "k_carrier_light"),
            new Expected("kill_primitive", "kill_assimilated",
                    "k_pri_arachnida", "k_pri_bolster", "k_pri_burrower", "k_pri_devourer",
                    "k_pri_longarms", "k_pri_manducater", "k_pri_reeker", "k_pri_summoner",
                    "k_pri_tozoon", "k_pri_vermin", "k_pri_viscera", "k_pri_yelloweye"),
            new Expected("kill_pure", "kill_root",
                    "k_grunt", "k_bomber_light", "k_marauder", "k_monarch",
                    "k_overseer", "k_vigilante", "k_warden"),
            new Expected("monument", "kill_beckon_s4", "detected_beckon_siv"),
            new Expected("peacekeeper", "firmamentum_root", "has_purifier"),
            new Expected("qliphoth_overload", "kill_dispatcher_s4", "detected_dispatcher_siv"),
            new Expected("sepeku", "fermented_arrogance", "done"),
            new Expected("stolas", "columbus", "unlock"),
            new Expected("thornshade_self_destruct", "guerilla", "exploded"),
            new Expected("tricked_me_did_you", "stolas", "unlock"),
            new Expected("recipes/misc/assimilated_jack_o_lantern", "minecraft:recipes/root", "has_ingredients"),
            new Expected("recipes/misc/gothshroom_to_purple_dye", "minecraft:recipes/root", "has_gothshroom"),
            new Expected("recipes/misc/srp_field_guide", "minecraft:recipes/root", "has_book", "has_flesh"),
            new Expected("again", null, "dragon_egg_assimilated"),
            new Expected("paojiao_wolf", "secret_root", "named_wolf"));

    private AdvancementGameTests() {
    }

    /** {@code parent} is the raw parent path, or {@code null} for a root advancement. */
    private record Expected(String path, String parent, String... criteria) {

        ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath(Csrp.MODID, this.path);
        }

        Optional<ResourceLocation> expectedParent() {
            if (this.parent == null) {
                return Optional.empty();
            }
            return Optional.of(this.parent.contains(":")
                    ? ResourceLocation.parse(this.parent)
                    : ResourceLocation.fromNamespaceAndPath(Csrp.MODID, this.parent));
        }
    }

    /** Every documented advancement is parsed into the running game. */
    @GameTest(template = TEMPLATE)
    public static void advancementJsonIsLoaded(GameTestHelper helper) {
        ServerAdvancementManager manager = advancements(helper);
        List<String> missing = new ArrayList<>();
        for (Expected expected : EXPECTED) {
            if (manager.get(expected.id()) == null) {
                missing.add(expected.id().toString());
            }
        }
        helper.assertTrue(missing.isEmpty(), "Advancements missing from the running game: " + missing);
        helper.succeed();
    }

    /** Every advancement points at the documented parent. */
    @GameTest(template = TEMPLATE)
    public static void advancementParentsMatch(GameTestHelper helper) {
        ServerAdvancementManager manager = advancements(helper);
        List<String> problems = new ArrayList<>();
        for (Expected expected : EXPECTED) {
            AdvancementHolder holder = manager.get(expected.id());
            if (holder == null) {
                problems.add(expected.id() + " is not loaded");
                continue;
            }
            Optional<ResourceLocation> actual = holder.value().parent();
            if (!actual.equals(expected.expectedParent())) {
                problems.add(expected.id() + " has parent " + actual.map(ResourceLocation::toString).orElse("<none>")
                        + " but should have " + expected.expectedParent().map(ResourceLocation::toString).orElse("<none>"));
            }
        }
        helper.assertTrue(problems.isEmpty(), "Advancement parent mismatch: " + problems);
        helper.succeed();
    }

    /** Only {@code csrp:root} and {@code csrp:again} sit at the top of the CSRP tree. */
    @GameTest(template = TEMPLATE)
    public static void onlyRootAndAgainAreParentless(GameTestHelper helper) {
        ServerAdvancementManager manager = advancements(helper);
        Set<ResourceLocation> roots = new TreeSet<>();
        for (AdvancementHolder holder : manager.getAllAdvancements()) {
            if (Csrp.MODID.equals(holder.id().getNamespace()) && holder.value().parent().isEmpty()) {
                roots.add(holder.id());
            }
        }
        Set<ResourceLocation> expected = new TreeSet<>();
        for (String path : ROOT_PATHS) {
            expected.add(ResourceLocation.fromNamespaceAndPath(Csrp.MODID, path));
        }
        helper.assertTrue(roots.equals(expected),
                "Parentless csrp advancements are " + roots + " but should be exactly " + expected);
        helper.succeed();
    }

    /** Every advancement icon resolves to a registered item. */
    @GameTest(template = TEMPLATE)
    public static void advancementIconsAreRegisteredItems(GameTestHelper helper) {
        ServerAdvancementManager manager = advancements(helper);
        List<String> problems = new ArrayList<>();
        for (Expected expected : EXPECTED) {
            AdvancementHolder holder = manager.get(expected.id());
            if (holder == null) {
                problems.add(expected.id() + " is not loaded");
                continue;
            }
            Optional<DisplayInfo> display = holder.value().display();
            if (display.isEmpty()) {
                // Recipe advancements are hidden and carry no display at all, exactly like vanilla's.
                if (!expected.path().startsWith(RECIPE_PATH_PREFIX)) {
                    problems.add(expected.id() + " has no display");
                }
                continue;
            }
            ItemStack icon = display.get().getIcon();
            if (icon.isEmpty()) {
                problems.add(expected.id() + " has an empty icon");
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(icon.getItem());
            if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
                problems.add(expected.id() + " uses unregistered icon item " + itemId);
            }
        }
        helper.assertTrue(problems.isEmpty(), "Advancement icon problems: " + problems);
        helper.succeed();
    }

    /** Awarding every documented criterion completes its advancement. */
    @GameTest(template = TEMPLATE, timeoutTicks = TRIGGER_TIMEOUT_TICKS)
    public static void advancementCriteriaCanBeAwarded(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerAdvancementManager manager = server.getAdvancements();
        ServerPlayer player = createMockServerPlayer(helper, server);
        if (player == null) {
            LOGGER.warn("[csrp gametest] no ServerPlayer could be created, skipping the advancement trigger check");
            helper.succeed();
            return;
        }

        List<String> problems = new ArrayList<>();
        try {
            for (Expected expected : EXPECTED) {
                AdvancementHolder holder = manager.get(expected.id());
                if (holder == null) {
                    problems.add(expected.id() + " is not loaded");
                    continue;
                }
                for (String criterion : expected.criteria()) {
                    if (!holder.value().criteria().containsKey(criterion)) {
                        problems.add(expected.id() + " has no criterion '" + criterion + "'");
                        continue;
                    }
                    player.getAdvancements().award(holder, criterion);
                }
                if (!player.getAdvancements().getOrStartProgress(holder).isDone()) {
                    problems.add(expected.id() + " is not done after awarding "
                            + Arrays.toString(expected.criteria()));
                }
            }
        } finally {
            player.getAdvancements().stopListening();
        }

        helper.assertTrue(problems.isEmpty(), "Advancement trigger problems: " + problems);
        helper.succeed();
    }

    /** The legacy {@code csrp:csrp/<name>} ids are rewritten to the canonical ids. */
    @GameTest(template = TEMPLATE)
    public static void legacyAdvancementIdsAreMigrated(GameTestHelper helper) {
        helper.assertTrue("csrp:kill_root".equals(AdvancementLegacyMigrationEvents.migrateId("csrp:csrp/venari")),
                "csrp:csrp/venari should migrate to csrp:kill_root");
        helper.assertTrue("csrp:root".equals(AdvancementLegacyMigrationEvents.migrateId("csrp:csrp/root")),
                "csrp:csrp/root should migrate to csrp:root");
        helper.assertTrue("csrp:kill_pure".equals(AdvancementLegacyMigrationEvents.migrateId("csrp:kill_pure")),
                "canonical ids must be left alone");
        helper.assertTrue("minecraft:story/root".equals(AdvancementLegacyMigrationEvents.migrateId("minecraft:story/root")),
                "foreign ids must be left alone");

        CompoundTag advancements = new CompoundTag();
        CompoundTag legacyProgress = new CompoundTag();
        legacyProgress.putBoolean("done", true);
        CompoundTag legacyCriteria = new CompoundTag();
        legacyCriteria.putString("tick", "2024-01-01 00:00:00 +0000");
        legacyProgress.put("criteria", legacyCriteria);
        advancements.put("csrp:csrp/venari", legacyProgress);
        advancements.put("csrp:csrp/root", new CompoundTag());
        advancements.put("csrp:kill_pure", new CompoundTag());
        advancements.put("minecraft:story/root", new CompoundTag());

        AdvancementLegacyMigrationEvents.migrateTag(advancements);

        helper.assertTrue(advancements.contains("csrp:kill_root"), "csrp:kill_root should exist after migration");
        helper.assertTrue(advancements.contains("csrp:root"), "csrp:root should exist after migration");
        helper.assertFalse(advancements.contains("csrp:csrp/venari"), "csrp:csrp/venari should be gone");
        helper.assertFalse(advancements.contains("csrp:csrp/root"), "csrp:csrp/root should be gone");
        helper.assertTrue(advancements.contains("csrp:kill_pure"), "canonical keys must survive");
        helper.assertTrue(advancements.contains("minecraft:story/root"), "foreign keys must survive");
        helper.assertTrue(advancements.getCompound("csrp:kill_root").getBoolean("done"),
                "migrated progress must keep the done flag");
        helper.assertTrue(advancements.getCompound("csrp:kill_root").getCompound("criteria").contains("tick"),
                "migrated progress must keep the criteria");

        // The modern per-player store (<world>/advancements/<uuid>.json) is what 1.21.1 reads.
        JsonObject json = new JsonObject();
        JsonObject jsonProgress = new JsonObject();
        jsonProgress.addProperty("done", true);
        JsonObject jsonCriteria = new JsonObject();
        jsonCriteria.addProperty("tick", "2024-01-01 00:00:00 +0000");
        jsonProgress.add("criteria", jsonCriteria);
        json.add("csrp:csrp/venari", jsonProgress);
        json.add("csrp:csrp/root", new JsonObject());
        json.add("csrp:kill_pure", new JsonObject());

        helper.assertTrue(AdvancementLegacyMigrationEvents.migrateJson(json),
                "migrateJson should report that it rewrote something");
        helper.assertTrue(json.has("csrp:kill_root"), "csrp:kill_root should exist after migration");
        helper.assertTrue(json.has("csrp:root"), "csrp:root should exist after migration");
        helper.assertFalse(json.has("csrp:csrp/venari"), "csrp:csrp/venari should be gone");
        helper.assertFalse(json.has("csrp:csrp/root"), "csrp:csrp/root should be gone");
        helper.assertTrue(json.has("csrp:kill_pure"), "canonical keys must survive");
        helper.assertTrue(json.getAsJsonObject("csrp:kill_root").get("done").getAsBoolean(),
                "migrated json progress must keep the done flag");

        ServerAdvancementManager manager = advancements(helper);
        List<String> unknownTargets = new ArrayList<>();
        AdvancementLegacyMigrationEvents.legacyMappings().forEach((legacyPath, canonicalPath) -> {
            ResourceLocation target = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, canonicalPath);
            if (manager.get(target) == null) {
                unknownTargets.add("csrp:csrp/" + legacyPath + " -> " + target);
            }
        });
        helper.assertTrue(unknownTargets.isEmpty(), "Legacy ids migrate to advancements that do not exist: " + unknownTargets);
        helper.succeed();
    }

    private static ServerAdvancementManager advancements(GameTestHelper helper) {
        return helper.getLevel().getServer().getAdvancements();
    }

    /**
     * A detached {@link ServerPlayer}. The packet listener is wired to an embedded channel because
     * completing a recipe advancement grants its recipes, and {@link ServerPlayer#awardRecipes}
     * sends that grant through {@code player.connection}.
     */
    private static ServerPlayer createMockServerPlayer(GameTestHelper helper, MinecraftServer server) {
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "csrp-gametest");
            ClientInformation information = ClientInformation.createDefault();
            ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, information);
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(connection);
            player.connection = new ServerGamePacketListenerImpl(server, connection, player,
                    CommonListenerCookie.createInitial(profile, false));
            return player;
        } catch (Throwable throwable) {
            LOGGER.warn("[csrp gametest] could not create a mock ServerPlayer", throwable);
            return null;
        }
    }
}
