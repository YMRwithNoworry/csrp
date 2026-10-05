package alku.csrp.celestial;

import alku.csrp.Csrp;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class CelestialCommands {
    private CelestialCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("srp_celestial")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("clear").executes(context -> clear(context.getSource())))
                .then(Commands.literal("all").executes(context -> all(context.getSource())))
                .then(Commands.literal("debug").executes(context -> debug(context.getSource())))
                .then(Commands.literal("dark_days_end").executes(context -> endDarkDays(context.getSource())));
        for (CelestialDefinition definition : CelestialCatalog.ALL) {
            root.then(Commands.literal(definition.id()).executes(context ->
                    toggle(context.getSource(), definition.id())));
        }
        event.getDispatcher().register(root);
    }

    private static int list(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Set<String> active = CelestialWorldData.get(level).active();
        Set<String> forced = CelestialWorldData.get(level).forced();
        source.sendSuccess(() -> Component.translatable("command.csrp.celestial.list",
                format(active), format(forced)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int clear(CommandSourceStack source) {
        CelestialSystem.clearForced(source.getLevel());
        source.sendSuccess(() -> Component.translatable("command.csrp.celestial.cleared"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int all(CommandSourceStack source) {
        CelestialSystem.forceAll(source.getLevel());
        source.sendSuccess(() -> Component.translatable("command.csrp.celestial.all"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int toggle(CommandSourceStack source, String id) {
        boolean enabled = CelestialSystem.toggleForced(source.getLevel(), id);
        source.sendSuccess(() -> Component.translatable(enabled
                ? "command.csrp.celestial.enabled" : "command.csrp.celestial.disabled", id), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int endDarkDays(CommandSourceStack source) {
        CelestialSystem.stopDarkDays(source.getLevel());
        source.sendSuccess(() -> Component.translatable("command.csrp.celestial.dark_days_end"), true);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * {@code CommandForceCelestial}'s {@code debug} branch: dump this dimension's celestial state. The
     * original printed the same fields as plain text — dimension, evolution phase, night index, time of
     * day, the active/forced counts and the dark days timers — followed by the two id lists.
     */
    private static int debug(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        CelestialWorldData data = CelestialWorldData.get(level);
        int phase = alku.csrp.world.SrpWorldData.get(level).evolutionPhase();
        long gameTime = level.getGameTime();
        source.sendSuccess(() -> Component.literal("[SERVER] dim=" + level.dimension().identifier()
                + " phase=" + phase
                + " day=" + (gameTime / 24000L)
                + " dayTime=" + (gameTime % 24000L)
                + " active=" + data.active().size()
                + " forced=" + data.forced().size()
                + " darkDaysEndTime=" + data.darkDaysEndTime()
                + " darkDaysEndingSoundPlayed=" + data.darkDaysEndingSoundPlayed()), false);
        source.sendSuccess(() -> Component.literal("[SERVER] active=" + format(data.active())), false);
        source.sendSuccess(() -> Component.literal("[SERVER] forced=" + format(data.forced())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static String format(Set<String> values) {
        return values.isEmpty() ? "-" : String.join(", ", values);
    }
}
