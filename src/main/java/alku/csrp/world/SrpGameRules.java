package alku.csrp.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Read helpers for the MC 26.3 game-rule rework.
 *
 * <p>26.3 turned {@code GameRules} into a holder-style table of {@code GameRule<T>} constants, and
 * {@code Level} no longer exposes {@code getGameRules()}/{@code getBoolean(...)}: only a
 * {@link ServerLevel} owns the {@code GameRules} instance and a rule is read with
 * {@code getGameRules().get(rule)}. The mod asks for {@code mob_griefing} from entity and block
 * code that only holds a {@link Level}, so the server check lives here once instead of at all 25
 * call sites.
 */
public final class SrpGameRules {
    private SrpGameRules() {
    }

    /** Reads a boolean rule; any non-server level reports {@code false} (nothing may grief there). */
    public static boolean flag(Level level, GameRule<Boolean> rule) {
        return level instanceof ServerLevel serverLevel && serverLevel.getGameRules().get(rule);
    }

    public static boolean mobGriefing(Level level) {
        return flag(level, GameRules.MOB_GRIEFING);
    }
}
