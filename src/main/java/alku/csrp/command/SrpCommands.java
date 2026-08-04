package alku.csrp.command;

import alku.csrp.Config;
import alku.csrp.Csrp;
import alku.csrp.entity.NexusParasiteEntity;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModEntities;
import alku.csrp.world.EvolutionSystem;
import alku.csrp.world.DislodgmentSystem;
import alku.csrp.world.SrpWorldData;
import alku.csrp.world.SrpCoreSystems;
import alku.csrp.world.SrpDifficulty;
import alku.csrp.world.SrpDifficultyEvents;
import alku.csrp.world.SrpWorldData.ColonyEntry;
import alku.csrp.world.SrpWorldData.DislodgmentCode;
import alku.csrp.world.SrpWorldData.NodeEntry;
import alku.csrp.world.SrpWorldData.VectorEntry;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Csrp.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class SrpCommands {
    private SrpCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(srParasites());
        dispatcher.register(srEvolution());
        dispatcher.register(srGeneration());
        dispatcher.register(srUDevelopment());
        dispatcher.register(srNodes());
        dispatcher.register(srColonies());
        dispatcher.register(srVectors());
        dispatcher.register(srDislodgment());
        dispatcher.register(srDifficulty());
        dispatcher.register(dqq());
        dispatcher.register(srHelp());
        dispatcher.register(srSummonNidus());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> srParasites() {
        return admin("srparasites")
                .then(Commands.literal("parasites").executes(SrpCommands::showParasiteStatus))
                .then(Commands.literal("setgeneration")
                        .then(Commands.argument("generation", IntegerArgumentType.integer(0, 5))
                                .executes(context -> setGeneration(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "generation")))))
                .then(Commands.literal("getgeneration")
                        .executes(context -> showGenerationStatus(context.getSource())))
                .then(Commands.literal("resetdatafile").executes(context -> {
                    data(context.getSource()).reset(context.getSource().getLevel());
                    return success(context.getSource(), "Data file of this dimension has been reset");
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> srEvolution() {
        return admin("srpevolution")
                .then(Commands.literal("getphase").executes(SrpCommands::showEvolutionStatus))
                .then(Commands.literal("addpoints")
                        .then(Commands.argument("points", IntegerArgumentType.integer())
                                .executes(context -> addEvolutionPoints(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "points")))))
                .then(Commands.literal("setcooldown")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> setCooldown(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds"), false))))
                .then(Commands.literal("addcooldown")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> setCooldown(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds"), true))))
                .then(Commands.literal("setphase")
                        .then(Commands.argument("phase", IntegerArgumentType.integer(-2, 10))
                                .executes(context -> setPhase(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "phase"), null))
                                .then(Commands.argument("generation", IntegerArgumentType.integer(0, 5))
                                        .executes(context -> setPhase(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "phase"),
                                                IntegerArgumentType.getInteger(context, "generation"))))))
                .then(Commands.literal("set_evolutiongaining")
                        .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(context -> {
                            boolean value = BoolArgumentType.getBool(context, "enabled");
                            data(context.getSource()).setCanGain(value);
                            return success(context.getSource(), "Evolution point gaining: " + value);
                        })))
                .then(Commands.literal("set_evolutionloss")
                        .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(context -> {
                            boolean value = BoolArgumentType.getBool(context, "enabled");
                            data(context.getSource()).setCanLose(value);
                            return success(context.getSource(), "Evolution point loss: " + value);
                        })))
                .then(Commands.literal("evolutionlock_getlist").executes(context -> success(context.getSource(),
                        "Locked parasite IDs: " + data(context.getSource()).lockedParasites())))
                .then(Commands.literal("evolutionlock_reset").executes(context -> {
                    data(context.getSource()).resetLockedParasites();
                    return success(context.getSource(), "Evolution lock list reset");
                }))
                .then(Commands.literal("evolutionlock_unlockall").executes(context -> {
                    data(context.getSource()).resetLockedParasites();
                    return success(context.getSource(), "All parasites unlocked");
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> srGeneration() {
        return admin("srpgeneration")
                .then(Commands.literal("setgeneration")
                        .then(Commands.argument("generation", IntegerArgumentType.integer(0, 5))
                                .executes(context -> setGeneration(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "generation")))))
                .then(Commands.literal("getgeneration")
                        .executes(context -> showGenerationStatus(context.getSource())))
                .then(Commands.literal("status")
                        .executes(context -> showGenerationStatus(context.getSource())))
                .then(Commands.literal("addticks")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer())
                                .executes(context -> {
                                    int ticks = IntegerArgumentType.getInteger(context, "ticks");
                                    data(context.getSource()).addGenerationTicks(ticks);
                                    return success(context.getSource(), "Added " + ticks + " generation ticks");
                                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> srUDevelopment() {
        return admin("srpudevelopment")
                .then(Commands.literal("getlevel").executes(context -> success(context.getSource(),
                        "Current Ubiquitous Development Level: "
                                + EvolutionSystem.ubiquitousDevelopment(context.getSource().getServer()))))
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 4)).executes(context -> {
                            int level = IntegerArgumentType.getInteger(context, "level");
                            EvolutionSystem.setUbiquitousDevelopmentOverride(context.getSource().getServer(), level);
                            return success(context.getSource(), "Changed Ubiquitous Development Level to " + level);
                        })))
                .then(Commands.literal("viewalldims").executes(SrpCommands::showAllDimensions))
                .then(Commands.literal("setdimevolution")
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .then(Commands.argument("phase", IntegerArgumentType.integer(-2, 10))
                                        .executes(context -> setDimensionEvolution(context, null))
                                        .then(Commands.argument("generation", IntegerArgumentType.integer(0, 5))
                                                .executes(context -> setDimensionEvolution(context,
                                                        IntegerArgumentType.getInteger(context, "generation")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> srNodes() {
        return admin("srpnodes")
                .then(Commands.literal("viewall").executes(context -> showNodes(context.getSource())))
                .then(Commands.literal("clearworld").executes(context -> {
                    SrpCoreSystems.clearNodes(context.getSource().getLevel());
                    return success(context.getSource(), "There are no longer nodes in this world");
                }))
                .then(Commands.literal("setnode")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("type", IntegerArgumentType.integer(-1, 4)).executes(context -> {
                                    BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
                                    int requestedType = IntegerArgumentType.getInteger(context, "type");
                                    int type = requestedType == -1 ? inferNodeType(context.getSource().getLevel(), pos)
                                            : requestedType;
                                    if (!SrpCoreSystems.placeNode(context.getSource().getLevel(), pos, type)) {
                                        return failure(context.getSource(), "Unable to place Node at " + format(pos));
                                    }
                                    return success(context.getSource(), "Node placed at " + format(pos)
                                            + " with type " + type);
                                }))))
                .then(Commands.literal("removenode")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> {
                            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
                            boolean removed = SrpCoreSystems.removeNode(context.getSource().getLevel(), pos);
                            return success(context.getSource(), removed ? "Node removed at " + format(pos)
                                    : "Node cannot be removed at " + format(pos));
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> srColonies() {
        return admin("srpcolonies")
                .then(Commands.literal("viewall").executes(context -> showColonies(context.getSource())))
                .then(Commands.literal("clearworld").executes(context -> {
                    SrpCoreSystems.clearColonies(context.getSource().getLevel());
                    return success(context.getSource(), "There are no longer colonies in this world");
                }))
                .then(Commands.literal("setcolony")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> {
                            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
                            if (!SrpCoreSystems.placeColony(context.getSource().getLevel(), pos)) {
                                return failure(context.getSource(), "Unable to place Colony at " + format(pos));
                            }
                            return success(context.getSource(), "Colony placed at " + format(pos));
                        })))
                .then(Commands.literal("removecolony")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> {
                            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
                            boolean removed = SrpCoreSystems.removeColony(context.getSource().getLevel(), pos);
                            return success(context.getSource(), removed ? "Colony removed at " + format(pos)
                                    : "Colony cannot be removed at " + format(pos));
                        })))
                .then(Commands.literal("resetglobaladaptation").executes(context -> {
                    data(context.getSource()).resetGlobalAdaptation();
                    return success(context.getSource(), "Global adaptation has been reset");
                }))
                .then(Commands.literal("viewallglobaladaptation").executes(context -> {
                    StringBuilder output = new StringBuilder("Current global adaptation (Damage type, points): ");
                    data(context.getSource()).globalAdaptations().forEach((damage, points) ->
                            output.append("[").append(damage).append(", ").append(points).append("] "));
                    return success(context.getSource(), output.toString());
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> srVectors() {
        return admin("srpvectors")
                .then(Commands.literal("viewall").executes(context -> showVectors(context.getSource())))
                .then(Commands.literal("clearworld").executes(context -> {
                    data(context.getSource()).clearVectors();
                    return success(context.getSource(),
                            "There are no longer Emerging Infestation Vectors in this world");
                }))
                .then(Commands.literal("setvector")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("health", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                                .executes(context -> {
                                                    BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
                                                    int health = IntegerArgumentType.getInteger(context, "health");
                                                    int radius = IntegerArgumentType.getInteger(context, "radius");
                                                    int result = SrpCoreSystems.placeVector(
                                                            context.getSource().getLevel(), pos, health, radius);
                                                    if (result == 6) {
                                                        return failure(context.getSource(),
                                                                "Emerging Infestation Vector is too close to another EIV");
                                                    }
                                                    if (result == 7) {
                                                        return failure(context.getSource(),
                                                                "Maximum number of Emerging Infestation Vectors reached");
                                                    }
                                                    return success(context.getSource(),
                                                            (result == 2 ? "Outbreak Infestation Vector placed at "
                                                                    : "Emerging Infestation Vector placed at ") + format(pos)
                                                                    + " with " + health + " health and " + radius
                                                                    + " radius");
                                                })))))
                .then(Commands.literal("removevector")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(context -> {
                            BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
                            boolean removed = SrpCoreSystems.removeVector(context.getSource().getLevel(), pos);
                            return success(context.getSource(), removed
                                    ? "Emerging Infestation Vector removed at " + format(pos)
                                    : "Emerging Infestation Vector cannot be removed at " + format(pos));
                        })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> srDislodgment() {
        return admin("srpdislodgment")
                .then(Commands.literal("codes_reset").executes(context -> {
                    DislodgmentSystem.clearCodes(context.getSource().getLevel());
                    return success(context.getSource(), "Dislodgment codes back to 0");
                }))
                .then(Commands.literal("viewcodes").executes(context -> showDislodgmentCodes(context.getSource())))
                .then(Commands.literal("random_code")
                        .then(Commands.argument("duration", IntegerArgumentType.integer(0))
                                .executes(context -> randomDislodgmentCode(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "duration")))))
                .then(Commands.literal("set_code")
                        .then(Commands.argument("duration", IntegerArgumentType.integer(0))
                                .then(Commands.argument("code", IntegerArgumentType.integer(0, 29))
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                .executes(context -> setDislodgmentCode(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "duration"),
                                                        IntegerArgumentType.getInteger(context, "code"),
                                                        IntegerArgumentType.getInteger(context, "value")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> srDifficulty() {
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        for (SrpDifficulty difficulty : SrpDifficulty.values()) {
            set.then(Commands.literal(difficulty.id())
                    .executes(context -> setDifficulty(context.getSource(), difficulty)));
        }
        return admin("srpdifficulty")
                .executes(context -> showDifficulty(context.getSource()))
                .then(Commands.literal("get").executes(context -> showDifficulty(context.getSource())))
                .then(set);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dqq() {
        return admin("dqq")
                .executes(context -> setEveMode(context.getSource(), null))
                .then(Commands.literal("on")
                        .executes(context -> setEveMode(context.getSource(), true)))
                .then(Commands.literal("off")
                        .executes(context -> setEveMode(context.getSource(), false)))
                .then(Commands.literal("status")
                        .executes(context -> showEveMode(context.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> srHelp() {
        return admin("srphelp")
                .executes(context -> success(context.getSource(),
                        "SRP commands: srparasites, srpevolution, srpgeneration, srpudevelopment, srpnodes, "
                                + "srpcolonies, srpvectors, srpdislodgment, srpdifficulty, dqq, srp_summon_nidus"))
                .then(helpTopic("srparasites", "Status, generation and per-dimension data reset"))
                .then(helpTopic("srpevolution", "Phase, points, cooldown and evolution locks"))
                .then(helpTopic("srpgeneration", "Parasite generation and generation ticks"))
                .then(helpTopic("srpudevelopment", "UD level and cross-dimension evolution state"))
                .then(helpTopic("srpnodes", "List, create and remove persistent nodes"))
                .then(helpTopic("srpcolonies", "List, create and remove persistent colonies"))
                .then(helpTopic("srpvectors", "List, create and remove infestation vectors"))
                .then(helpTopic("srpdislodgment", "Create, inspect and clear dislodgment codes"))
                .then(helpTopic("srpdifficulty", "View or change the active SRP difficulty"))
                .then(helpTopic("dqq", "Toggle EVE mode or query its status"))
                .then(helpTopic("srp_summon_nidus", "Summon a Beckon nexus stage at a position"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> srSummonNidus() {
        return admin("srp_summon_nidus")
                .executes(context -> summonNidus(context.getSource(),
                        BlockPos.containing(context.getSource().getPosition()), 1))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> summonNidus(context.getSource(),
                                BlockPosArgument.getBlockPos(context, "pos"), 1))
                        .then(Commands.argument("stage", IntegerArgumentType.integer(1, 4))
                                .executes(context -> summonNidus(context.getSource(),
                                        BlockPosArgument.getBlockPos(context, "pos"),
                                        IntegerArgumentType.getInteger(context, "stage")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> helpTopic(String name, String description) {
        return Commands.literal(name).executes(context -> success(context.getSource(), description));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> admin(String name) {
        return Commands.literal(name).requires(source -> source.hasPermission(2));
    }

    private static SrpWorldData data(CommandSourceStack source) {
        return SrpWorldData.get(source.getLevel());
    }

    private static int showParasiteStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Parasite) {
                count++;
            }
        }
        SrpWorldData data = SrpWorldData.get(level);
        return success(source, "Parasites: " + count + ", phase: " + data.evolutionPhase()
                + ", generation: " + data.generation() + ", difficulty: " + data.difficulty().id()
                + ", generation system: "
                + (Config.generationEnabled() ? "enabled" : "disabled") + ", adaptation: "
                + adaptationStatus(level) + ", dislodgment codes: "
                + data.activeDislodgmentCodes(level).size());
    }

    private static int showEvolutionStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        SrpWorldData data = SrpWorldData.get(level);
        success(source, "Evolution dimension: " + level.dimension().location());
        success(source, "Phase: " + data.evolutionPhase() + ", points: " + data.evolutionPoints()
                + ", cooldown seconds: " + data.cooldown(level));
        int phase = data.evolutionPhase();
        int nextThreshold = phase >= 10 ? EvolutionSystem.thresholdForPhase(10)
                : EvolutionSystem.thresholdForPhase(phase + 1);
        int currentThreshold = EvolutionSystem.thresholdForPhase(phase);
        int span = nextThreshold - currentThreshold;
        int progress = span <= 0 ? 100
                : Math.max(0, Math.min(100, (int) ((long) (data.evolutionPoints() - currentThreshold) * 100L / span)));
        success(source, "Next phase points: " + nextThreshold + ", progress: " + progress + "%");
        success(source, "Gaining: " + data.canGain() + ", loss: " + data.canLose()
                + ", generation: " + data.generation() + ", generation ticks: " + data.generationTicks()
                + ", difficulty: " + data.difficulty().id()
                + ", generation system: " + (Config.generationEnabled() ? "enabled" : "disabled")
                + ", adaptation: " + adaptationStatus(level)
                + ", UD: " + EvolutionSystem.ubiquitousDevelopment(source.getServer()));
        int parasites = 0;
        int coth = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Parasite) {
                parasites++;
            }
            if (entity instanceof LivingEntity living && living.hasEffect(alku.csrp.registry.ModMobEffects.COTH)) {
                coth++;
            }
        }
        int eivCount = data.vectors().size();
        boolean nearEiv = data.vectors().stream().anyMatch(vector ->
                vector.pos().distSqr(BlockPos.containing(source.getPosition())) <= (double) vector.radius() * vector.radius());
        success(source, "Parasite count: " + parasites + ", COTH count: " + coth
                + ", EIV count: " + eivCount + ", near EIV: " + nearEiv);
        return Command.SINGLE_SUCCESS;
    }

    /** Matches the legacy type=-1 biome-derived node placement mode. */
    private static int inferNodeType(ServerLevel level, BlockPos pos) {
        float temperature = level.getBiome(pos).value().getBaseTemperature();
        if (temperature >= 1.0F) {
            return 1;
        }
        if (temperature <= 0.15F) {
            return 4;
        }
        return 3;
    }

    private static int addEvolutionPoints(CommandSourceStack source, int points) {
        SrpWorldData data = data(source);
        if (!data.addEvolutionPoints(source.getLevel(), points, false)) {
            return failure(source, "Evolution point change was blocked by phase, cooldown, or gain/loss settings");
        }
        return success(source, "Added " + points + " evolution points; total: " + data.evolutionPoints());
    }

    private static int setCooldown(CommandSourceStack source, int seconds, boolean add) {
        SrpWorldData data = data(source);
        if (add) {
            data.addCooldown(source.getLevel(), seconds);
        } else {
            data.setCooldown(source.getLevel(), seconds);
        }
        return success(source, "Evolution cooldown seconds: " + data.cooldown(source.getLevel()));
    }

    private static int setPhase(CommandSourceStack source, int phase, Integer generation) {
        SrpWorldData data = data(source);
        data.forceEvolutionPhase(source.getLevel(), phase);
        if (generation != null) {
            data.setGeneration(generation);
        }
        return success(source, "Changed evolution phase to " + phase
                + (generation == null ? "" : " and generation to " + generation));
    }

    private static int setGeneration(CommandSourceStack source, int generation) {
        data(source).setGeneration(generation);
        return success(source, "Changed Generation of Parasites to " + generation);
    }

    private static int showGenerationStatus(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        SrpWorldData data = SrpWorldData.get(level);
        if (!Config.generationEnabled()) {
            return success(source, "Generation system: disabled, stored generation: " + data.generation()
                    + ", generation ticks: " + data.generationTicks()
                    + ", effective profile: full (generation 5), adaptation: active");
        }
        return success(source, "Generation system: enabled, generation: " + data.generation()
                + ", generation ticks: " + data.generationTicks()
                + ", adaptation: " + adaptationStatus(level));
    }

    private static String adaptationStatus(ServerLevel level) {
        return EvolutionSystem.generationProfile(level).adaptation()
                ? "active" : "locked (unlocks at generation 3)";
    }

    private static int showAllDimensions(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        success(source, "Parasite progress by dimension (dimension, phase, points, generation, difficulty):");
        for (ServerLevel level : source.getServer().getAllLevels()) {
            SrpWorldData data = SrpWorldData.get(level);
            success(source, "[" + level.dimension().location() + ", " + data.evolutionPhase() + ", "
                    + data.evolutionPoints() + ", " + data.generation() + ", "
                    + data.difficulty().id() + "]");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int showDifficulty(CommandSourceStack source) {
        SrpDifficulty difficulty = SrpWorldData.get(source.getServer().overworld()).difficulty();
        return success(source, "SRP difficulty: " + difficulty.id()
                + " (health x" + difficulty.healthMultiplier()
                + ", damage x" + difficulty.damageMultiplier()
                + ", armor x" + difficulty.armorMultiplier()
                + ", knockback x" + difficulty.knockbackMultiplier()
                + ", evolution points x" + difficulty.pointMultiplier() + ")");
    }

    private static int setDifficulty(CommandSourceStack source, SrpDifficulty difficulty) {
        SrpDifficulty previous = SrpWorldData.get(source.getServer().overworld()).difficulty();
        for (ServerLevel level : source.getServer().getAllLevels()) {
            SrpWorldData.get(level).setDifficulty(difficulty);
            SrpDifficultyEvents.refreshDifficulty(level);
        }
        return success(source, "Changed SRP difficulty from " + previous.id() + " to " + difficulty.id()
                + "; parasite attributes and evolution point gain updated in all dimensions");
    }

    private static int setEveMode(CommandSourceStack source, Boolean requestedState) {
        SrpWorldData globalData = SrpWorldData.get(source.getServer().overworld());
        boolean enabled = requestedState == null ? !globalData.eveMode() : requestedState;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            SrpWorldData.get(level).setEveMode(enabled);
        }
        return success(source, Component.translatable(enabled
                        ? "command.csrp.eve.enabled" : "command.csrp.eve.disabled",
                Component.translatable(globalData.difficulty().translationKey())));
    }

    private static int showEveMode(CommandSourceStack source) {
        SrpWorldData data = SrpWorldData.get(source.getServer().overworld());
        return success(source, Component.translatable("command.csrp.eve.status",
                Component.translatable(data.eveMode() ? "options.on" : "options.off"),
                data.evolutionPhase(), data.generation(),
                EvolutionSystem.ubiquitousDevelopment(source.getServer()),
                Component.translatable(data.difficulty().translationKey())));
    }

    private static int setDimensionEvolution(CommandContext<CommandSourceStack> context, Integer generation)
            throws CommandSyntaxException {
        ServerLevel level = DimensionArgument.getDimension(context, "dimension");
        int phase = IntegerArgumentType.getInteger(context, "phase");
        SrpWorldData data = SrpWorldData.get(level);
        data.forceEvolutionPhase(level, phase);
        if (generation != null) {
            data.setGeneration(generation);
        }
        return success(context.getSource(), "Changed " + level.dimension().location() + " phase to " + phase
                + (generation == null ? "" : " and generation to " + generation));
    }

    private static int showNodes(CommandSourceStack source) {
        List<NodeEntry> entries = data(source).nodes();
        success(source, "Current nodes (x, y, z, age, type): " + entries.size());
        entries.forEach(entry -> success(source, "[" + format(entry.pos()) + ", " + entry.age() + ", "
                + entry.type() + "]"));
        return Command.SINGLE_SUCCESS;
    }

    private static int showColonies(CommandSourceStack source) {
        List<ColonyEntry> entries = data(source).colonies();
        success(source, "Current colonies (x, y, z, points): " + entries.size());
        entries.forEach(entry -> success(source, "[" + format(entry.pos()) + ", " + entry.points() + "]"));
        return Command.SINGLE_SUCCESS;
    }

    private static int showVectors(CommandSourceStack source) {
        List<VectorEntry> entries = data(source).vectors();
        success(source, "Current Emerging Infestation Vectors (x, y, z, health, radius): " + entries.size());
        entries.forEach(entry -> success(source, "[" + format(entry.pos()) + ", " + entry.health() + ", "
                + entry.radius() + "]"));
        return Command.SINGLE_SUCCESS;
    }

    private static int showDislodgmentCodes(CommandSourceStack source) {
        List<DislodgmentCode> entries = data(source).activeDislodgmentCodes(source.getLevel());
        success(source, "Active dislodgment codes: " + entries.size());
        entries.forEach(entry -> success(source, "[code=" + entry.code() + ", value=" + entry.value()
                + ", remaining=" + Math.max(0L,
                        (entry.expiresAt() - source.getLevel().getGameTime() + 19L) / 20L) + "]"));
        return Command.SINGLE_SUCCESS;
    }

    private static int randomDislodgmentCode(CommandSourceStack source, int duration) {
        SrpWorldData data = data(source);
        for (int attempts = 0; attempts < 30; attempts++) {
            int code = source.getLevel().getRandom().nextInt(30);
            int value = source.getLevel().getRandom().nextInt(6) + 1;
            if (data.setDislodgmentCode(source.getLevel(), code, value, duration)) {
                return success(source, "Dislodgment Code: " + code + " Value: " + value
                        + " Duration: " + duration);
            }
        }
        return failure(source, "Unable to allocate a free dislodgment code");
    }

    private static int setDislodgmentCode(CommandSourceStack source, int duration, int code, int value) {
        if (!data(source).setDislodgmentCode(source.getLevel(), code, value, duration)) {
            return failure(source, "Dislodgment code is in use or the arguments are invalid");
        }
        return success(source, "Dislodgment Code: " + code + " Value: " + value + " Duration: " + duration);
    }

    private static int summonNidus(CommandSourceStack source, BlockPos pos, int stage) {
        EntityType<NexusParasiteEntity> type = switch (stage) {
            case 2 -> ModEntities.BECKON_SII.get();
            case 3 -> ModEntities.BECKON_SIII.get();
            case 4 -> ModEntities.BECKON_SIV.get();
            default -> ModEntities.BECKON_SI.get();
        };
        NexusParasiteEntity entity = type.create(source.getLevel());
        if (entity == null) {
            return failure(source, "Unable to create Nidus/Nexus entity");
        }
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                source.getLevel().getRandom().nextFloat() * 360.0F, 0.0F);
        entity.finalizeSpawn(source.getLevel(), source.getLevel().getCurrentDifficultyAt(pos),
                MobSpawnType.COMMAND, null);
        source.getLevel().addFreshEntity(entity);
        return success(source, "Summoned Nidus/Nexus at " + format(pos) + " with stage " + stage);
    }

    private static int success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int success(CommandSourceStack source, Component message) {
        source.sendSuccess(() -> message, true);
        return Command.SINGLE_SUCCESS;
    }

    private static int failure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }

    private static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
