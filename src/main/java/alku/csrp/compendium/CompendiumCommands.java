package alku.csrp.compendium;

import alku.csrp.Csrp;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Csrp.MODID)
public final class CompendiumCommands {
    private CompendiumCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(guideCommand());
        event.getDispatcher().register(clearGuideCommand());
        event.getDispatcher().register(statsCommand("srpbestiarystats", true));
        event.getDispatcher().register(statsCommand("srpstats", false));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> guideCommand() {
        return Commands.literal("srpguide").requires(source -> source.hasPermission(2))
                .then(Commands.literal("unlockall").executes(context -> unlock(context.getSource(), Unlock.ALL)))
                .then(Commands.literal("unlockblocks").executes(context -> unlock(context.getSource(), Unlock.BLOCKS)))
                .then(Commands.literal("unlockcelestial").executes(context -> unlock(context.getSource(), Unlock.CELESTIAL)))
                .then(Commands.literal("unlockeffects").executes(context -> unlock(context.getSource(), Unlock.EFFECTS)))
                .then(Commands.literal("restore").executes(context -> restore(context.getSource())))
                .then(Commands.literal("clearblocks").executes(context -> clearCategory(context.getSource(), Unlock.BLOCKS)))
                .then(Commands.literal("clearcelestial").executes(context -> clearCategory(context.getSource(), Unlock.CELESTIAL)))
                .then(Commands.literal("cleareffects").executes(context -> clearCategory(context.getSource(), Unlock.EFFECTS)))
                .then(Commands.literal("clearall").executes(context -> clearAll(context.getSource(), self(context.getSource()))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> clearGuideCommand() {
        return Commands.literal("srpguideclear").requires(source -> source.hasPermission(2))
                .executes(context -> clearAll(context.getSource(), self(context.getSource())))
                .then(Commands.argument("player", EntityArgument.player()).executes(context ->
                        clearAll(context.getSource(), EntityArgument.getPlayer(context, "player"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> statsCommand(String name, boolean bestiary) {
        return Commands.literal(name).requires(source -> source.hasPermission(2))
                .then(Commands.literal("clear")
                        .executes(context -> clearStats(context.getSource(), self(context.getSource()), bestiary))
                        .then(Commands.argument("player", EntityArgument.player()).executes(context -> clearStats(
                                context.getSource(), EntityArgument.getPlayer(context, "player"), bestiary))));
    }

    private static int unlock(CommandSourceStack source, Unlock kind) throws CommandSyntaxException {
        ServerPlayer player = self(source);
        CompendiumSavedData data = CompendiumSavedData.get(source.getServer());
        CompendiumProgress progress = data.progress(player.getUUID());
        if (kind == Unlock.ALL) {
            progress.takeUnlockSnapshot();
            List<String> mobs = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .filter(id -> id.getNamespace().equals(Csrp.MODID)).map(Object::toString).toList();
            progress.unlockAllMobs(mobs);
        }
        if (kind == Unlock.ALL || kind == Unlock.BLOCKS) {
            List<String> blocks = CompendiumCatalog.BLOCKS.stream().map(id -> Csrp.MODID + ":" + id).toList();
            progress.unlockAllBlocks(blocks);
        }
        if (kind == Unlock.ALL || kind == Unlock.CELESTIAL) {
            progress.unlockAllCelestials();
        }
        if (kind == Unlock.ALL || kind == Unlock.EFFECTS) {
            List<String> effects = BuiltInRegistries.MOB_EFFECT.keySet().stream()
                    .filter(id -> id.getNamespace().equals(Csrp.MODID)).map(Object::toString).toList();
            progress.unlockAllEffects(effects);
        }
        data.changed();
        source.sendSuccess(() -> Component.translatable("command.csrp.compendium.unlocked", player.getName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int restore(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = self(source);
        CompendiumSavedData data = CompendiumSavedData.get(source.getServer());
        boolean restored = data.progress(player.getUUID()).restoreUnlockSnapshot();
        if (restored) {
            data.changed();
        }
        source.sendSuccess(() -> Component.translatable(restored
                ? "command.csrp.compendium.restored" : "command.csrp.compendium.no_snapshot"), false);
        return restored ? Command.SINGLE_SUCCESS : 0;
    }

    private static int clearCategory(CommandSourceStack source, Unlock kind) throws CommandSyntaxException {
        ServerPlayer player = self(source);
        CompendiumSavedData data = CompendiumSavedData.get(source.getServer());
        CompendiumProgress progress = data.progress(player.getUUID());
        switch (kind) {
            case BLOCKS -> progress.clearBlocks();
            case CELESTIAL -> progress.clearCelestials();
            case EFFECTS -> progress.clearEffects();
            default -> throw new IllegalArgumentException("Cannot clear category " + kind);
        }
        data.changed();
        source.sendSuccess(() -> Component.translatable("command.csrp.compendium.category_cleared", player.getName()),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private static int clearAll(CommandSourceStack source, ServerPlayer player) {
        CompendiumSavedData data = CompendiumSavedData.get(source.getServer());
        data.progress(player.getUUID()).clear();
        data.changed();
        source.sendSuccess(() -> Component.translatable("command.csrp.compendium.cleared", player.getName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int clearStats(CommandSourceStack source, ServerPlayer player, boolean bestiary) {
        CompendiumSavedData data = CompendiumSavedData.get(source.getServer());
        CompendiumProgress progress = data.progress(player.getUUID());
        if (bestiary) {
            progress.clearBestiaryStats();
        } else {
            progress.clearCombatStats();
        }
        data.changed();
        source.sendSuccess(() -> Component.translatable("command.csrp.compendium.stats_cleared", player.getName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static ServerPlayer self(CommandSourceStack source) throws CommandSyntaxException {
        return source.getPlayerOrException();
    }

    private enum Unlock {
        ALL,
        BLOCKS,
        CELESTIAL,
        EFFECTS
    }
}
