package alku.csrp.command;

import alku.csrp.network.ExtremeSnowStatePayload;
import alku.csrp.world.ExtremeSnowData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Port of SRP 1.10.9 {@code util/CommandExtremeSnow}.
 *
 * <p>{@code /srp_extremesnow <on|off|toggle> [intensity 0..1] [anywhere 0|1] [windDeg 0..359]
 * [windSpeed 0..1]} at permission level 2.  Each optional argument falls back to the stored value, and
 * the new state is broadcast to the dimension's clients (the original's
 * {@code ExtremeSnowNetwork.broadcast}) and reported with the original's message.</p>
 *
 * <p><b>Deliberately not registered.</b> The original never passed this class to
 * {@code registerServerCommand} — the only call sites of {@code ExtremeSnowData#setEnabled} are inside
 * it — and its language file has no {@code srphelp.srp_extremesnow} entry, unlike the harlequin commands.
 * The forced extreme snow is therefore unreachable in 1.10.9 as shipped: {@link ExtremeSnowData} stays
 * {@code enabled = false} forever and {@link alku.csrp.world.ExtremeSnowServer} never places snow.
 * Registering it here would hand players an admin command the original never had, so the class is kept
 * for parity and left off the dispatcher (asserted by {@code scripts/verify-extreme-snow.cjs}).</p>
 */
public final class ExtremeSnowCommand {
    public static final String NAME = "srp_extremesnow";
    private static final String USAGE =
            "/srp_extremesnow <on|off|toggle> [intensity 0..1] [anywhere 0|1] [windDeg 0..359] [windSpeed 0..1]";

    private ExtremeSnowCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal(NAME)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> usage(context.getSource()))
                .then(mode("on"))
                .then(mode("off"))
                .then(mode("toggle"));
    }

    /** The original read up to four optional numbers after the mode, each defaulting to the stored value. */
    private static LiteralArgumentBuilder<CommandSourceStack> mode(String mode) {
        return Commands.literal(mode)
                .executes(context -> apply(context, mode, null, null, null, null))
                .then(Commands.argument("intensity", FloatArgumentType.floatArg(0.0F, 1.0F))
                        .executes(context -> apply(context, mode,
                                FloatArgumentType.getFloat(context, "intensity"), null, null, null))
                        .then(Commands.argument("anywhere", IntegerArgumentType.integer(0, 1))
                                .executes(context -> apply(context, mode,
                                        FloatArgumentType.getFloat(context, "intensity"),
                                        IntegerArgumentType.getInteger(context, "anywhere"), null, null))
                                .then(Commands.argument("windDeg", FloatArgumentType.floatArg(0.0F, 359.0F))
                                        .executes(context -> apply(context, mode,
                                                FloatArgumentType.getFloat(context, "intensity"),
                                                IntegerArgumentType.getInteger(context, "anywhere"),
                                                FloatArgumentType.getFloat(context, "windDeg"), null))
                                        .then(Commands.argument("windSpeed",
                                                        FloatArgumentType.floatArg(0.0F, 1.0F))
                                                .executes(context -> apply(context, mode,
                                                        FloatArgumentType.getFloat(context, "intensity"),
                                                        IntegerArgumentType.getInteger(context, "anywhere"),
                                                        FloatArgumentType.getFloat(context, "windDeg"),
                                                        FloatArgumentType.getFloat(context, "windSpeed")))))));
    }

    private static int usage(CommandSourceStack source) {
        source.sendFailure(Component.literal(USAGE));
        return 0;
    }

    private static int apply(CommandContext<CommandSourceStack> context, String mode, Float intensity,
            Integer anywhere, Float windDeg, Float windSpeed) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        ExtremeSnowData data = ExtremeSnowData.get(level);

        boolean enabled = switch (mode) {
            case "on" -> true;
            case "off" -> false;
            default -> !data.isEnabled();
        };
        float newIntensity = intensity != null ? intensity : data.getIntensity();
        boolean newAnywhere = anywhere != null ? anywhere == 1 : data.isForceAnywhere();
        float newWindDeg = windDeg != null ? windDeg : data.getWindDeg();
        float newWindSpeed = windSpeed != null ? windSpeed : data.getWindSpeed();

        data.setEnabled(enabled);
        data.setIntensity(newIntensity);
        data.setForceAnywhere(newAnywhere);
        data.setWindDeg(newWindDeg);
        data.setWindSpeed(newWindSpeed);
        ExtremeSnowStatePayload.broadcast(level, enabled, newIntensity, newAnywhere, newWindDeg,
                newWindSpeed);

        source.sendSuccess(() -> Component.literal(String.format(
                "Extreme Snow: %s (intensity=%.2f, anywhere=%s, windDeg=%.0f, windSpeed=%.2f)",
                enabled ? "ON" : "OFF", newIntensity, newAnywhere, newWindDeg, newWindSpeed)), true);
        return Command.SINGLE_SUCCESS;
    }
}
