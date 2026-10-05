package alku.csrp.command;

import alku.csrp.network.GuiDistortionStatePayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * Port of SRP 1.10.9 {@code network/SRPCommandGuiDistortion}.
 *
 * <p>{@code /srpguidistortion <on|off|toggle|status> [player|all]} — with the original's aliases
 * {@code srpguidist} and {@code srpdistortion}, registered as its own root command at permission level
 * 2 — switches the derived-parasite GUI corruption per player.  The original kept two flags in the
 * player's persisted data ({@code SRPGuiDistortionDisabled} and
 * {@code SRPGuiDistortionCreativeOverride}) and told the client about both, because a creative or
 * spectator player only keeps the effect while the override is on; {@code toggle} therefore flips the
 * <em>effective</em> state rather than the raw flag.</p>
 */
public final class GuiDistortionCommand {
    public static final String NAME = "srpguidistortion";
    /** {@code SRPCommandGuiDistortion.aliases}. */
    public static final List<String> ALIASES = List.of("srpguidist", "srpdistortion");
    private static final String DISABLED_KEY = "SRPGuiDistortionDisabled";
    private static final String CREATIVE_OVERRIDE_KEY = "SRPGuiDistortionCreativeOverride";
    private static final String USAGE = "/" + NAME + " <on|off|toggle|status> [player|all]";
    private static final List<String> MODES = List.of("on", "off", "toggle", "status");

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(MODES, builder);
    private static final SuggestionProvider<CommandSourceStack> TARGET_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(targetNames(context.getSource()), builder);

    private GuiDistortionCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return build(NAME);
    }

    public static List<LiteralArgumentBuilder<CommandSourceStack>> aliasCommands() {
        List<LiteralArgumentBuilder<CommandSourceStack>> out = new ArrayList<>(ALIASES.size());
        for (String alias : ALIASES) {
            out.add(build(alias));
        }
        return out;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        return Commands.literal(name)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> usage(context.getSource()))
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests(MODE_SUGGESTIONS)
                        .executes(context -> run(context, StringArgumentType.getString(context, "mode"), null))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(context -> run(context,
                                        StringArgumentType.getString(context, "mode"),
                                        StringArgumentType.getString(context, "target")))));
    }

    private static List<String> targetNames(CommandSourceStack source) {
        List<String> names = new ArrayList<>();
        names.add("all");
        names.add("@a");
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            names.add(player.getName().getString());
        }
        return names;
    }

    private static int usage(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("commands.csrp.guidistortion.usage.text", USAGE), false);
        return 0;
    }

    private static int run(CommandContext<CommandSourceStack> context, String rawMode, String rawTarget) {
        CommandSourceStack source = context.getSource();
        String mode = rawMode.toLowerCase();
        if (!MODES.contains(mode)) {
            source.sendFailure(Component.translatable("commands.csrp.guidistortion.invalid_argument"));
            return 0;
        }

        List<ServerPlayer> targets = targets(source, rawTarget);
        if (targets.isEmpty()) {
            return 0;
        }
        boolean multiTarget = targets.size() > 1;
        for (ServerPlayer target : targets) {
            handleTarget(source, target, mode, multiTarget);
        }
        if (multiTarget && !"status".equals(mode)) {
            source.sendSuccess(() -> Component.translatable(
                    "commands.csrp.guidistortion.set_all", targets.size()), true);
        }
        return targets.size();
    }

    /** {@code SRPCommandGuiDistortion.getTargets}. */
    private static List<ServerPlayer> targets(CommandSourceStack source, String rawTarget) {
        MinecraftServer server = source.getServer();
        if (rawTarget != null) {
            if ("all".equalsIgnoreCase(rawTarget) || "@a".equalsIgnoreCase(rawTarget)) {
                return new ArrayList<>(server.getPlayerList().getPlayers());
            }
            ServerPlayer target = server.getPlayerList().getPlayerByName(rawTarget);
            if (target == null) {
                source.sendFailure(Component.translatable(
                        "commands.csrp.guidistortion.player_not_found", rawTarget));
                return List.of();
            }
            return List.of(target);
        }
        if (source.getEntity() instanceof ServerPlayer self) {
            return List.of(self);
        }
        source.sendFailure(Component.translatable("commands.csrp.guidistortion.console_requires_player"));
        return List.of();
    }

    /** {@code SRPCommandGuiDistortion.handleTarget}. */
    private static void handleTarget(CommandSourceStack source, ServerPlayer target, String mode,
            boolean multiTarget) {
        boolean disabled = target.getPersistentData().getBooleanOr(DISABLED_KEY, false);
        boolean creativeOverride = target.getPersistentData().getBooleanOr(CREATIVE_OVERRIDE_KEY, false);
        boolean creativeOrSpectator = isCreativeOrSpectator(target);

        if ("status".equals(mode)) {
            Component message = Component.translatable("commands.csrp.guidistortion.status.detailed",
                    target.getName().getString(),
                    stateText(disabled),
                    Component.translatable(creativeOverride
                            ? "commands.csrp.guidistortion.creative_override.enabled"
                            : "commands.csrp.guidistortion.creative_override.disabled"),
                    Component.translatable(creativeOrSpectator
                            ? "commands.csrp.guidistortion.gamemode.creative_or_spectator"
                            : "commands.csrp.guidistortion.gamemode.normal"));
            source.sendSuccess(() -> message, false);
            return;
        }

        boolean[] flags = computeFlags(mode, disabled, creativeOverride, creativeOrSpectator);
        boolean newDisabled = flags[0];
        boolean newCreativeOverride = flags[1];

        target.getPersistentData().putBoolean(DISABLED_KEY, newDisabled);
        target.getPersistentData().putBoolean(CREATIVE_OVERRIDE_KEY, newCreativeOverride);
        GuiDistortionStatePayload.send(target, newDisabled, newCreativeOverride);

        if (!multiTarget) {
            Component message = Component.translatable("commands.csrp.guidistortion.set_other",
                    target.getName().getString(), stateText(newDisabled));
            source.sendSuccess(() -> message, true);
        }
        if (target != source.getEntity()) {
            target.sendSystemMessage(Component.translatable(
                    "commands.csrp.guidistortion.set_self", stateText(newDisabled)));
        }
    }

    /**
     * The original's flag transition, split out from the command so the rule can be exercised at runtime
     * without a player.
     *
     * @return {@code {newDisabled, newCreativeOverride}}
     */
    public static boolean[] computeFlags(String mode, boolean disabled, boolean creativeOverride,
            boolean creativeOrSpectator) {
        boolean effectivelyDisabled = disabled || (creativeOrSpectator && !creativeOverride);
        boolean newCreativeOverride = creativeOverride;
        boolean newDisabled;
        if ("on".equals(mode)) {
            newDisabled = false;
            if (creativeOrSpectator) {
                newCreativeOverride = true;
            }
        } else if ("off".equals(mode)) {
            newDisabled = true;
            newCreativeOverride = false;
        } else if (effectivelyDisabled) {
            newDisabled = false;
            if (creativeOrSpectator) {
                newCreativeOverride = true;
            }
        } else {
            newDisabled = true;
            newCreativeOverride = false;
        }
        return new boolean[] {newDisabled, newCreativeOverride};
    }

    /** {@code effectivelyDisabled}: the raw flag, or creative/spectator without the override. */
    public static boolean isEffectivelyDisabled(boolean disabled, boolean creativeOverride,
            boolean creativeOrSpectator) {
        return disabled || (creativeOrSpectator && !creativeOverride);
    }

    private static Component stateText(boolean disabled) {
        return Component.translatable(disabled
                ? "commands.csrp.guidistortion.state.disabled"
                : "commands.csrp.guidistortion.state.enabled");
    }

    /** {@code SRPCommandGuiDistortion.isCreativeOrSpectator}. */
    private static boolean isCreativeOrSpectator(ServerPlayer player) {
        if (player.isSpectator()) {
            return true;
        }
        GameType type = player.gameMode();
        return type == GameType.CREATIVE || type == GameType.SPECTATOR;
    }
}
