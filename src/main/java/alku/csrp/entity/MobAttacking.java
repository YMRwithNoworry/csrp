package alku.csrp.entity;

import alku.csrp.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Port of the 1.12.2 {@code util/ParasiteEventEntity#checkEntity} / {@code #checkName} pair together with the
 * {@code SRPConfig.mobattackingBlackList} / {@code mobattackingBlackListWhite} / {@code mobAttackingFull} knobs
 * declared in {@code util/config/SRPConfig.java}.
 *
 * <p><b>Original semantics.</b> {@code EntityParasiteBase#canAttackClass(Class)} (out109 lines 1094-1117) is the
 * single decision point every parasite family uses:
 *
 * <pre>
 * return name.contains("srparasites")
 *     ? false
 *     : !SRPConfig.mobAttackingFull
 *       || !ParasiteEventEntity.checkName(name, SRPConfig.mobattackingBlackList, SRPConfig.mobattackingBlackListWhite);
 * </pre>
 *
 * where {@code checkName} is
 * {@code Arrays.stream(blacklist).anyMatch(potentialElement::contains) != isWhitelist}.
 *
 * <p>Read literally: a blacklisted ("listed" under the default non-inverted list) mob is <b>rejected</b> only when
 * {@code mobAttackingFull} is <b>false</b>. Setting {@code Mob Attacking Friendly = true} therefore
 * makes parasites leave every blacklisted mob alone ("fully friendly with mobs from Mob Attacking blacklist"),
 * 当 {@code mobAttackingFull} 为 true 时，寄生体会避开所有黑名单生物；默认值 false 会绕过黑名单，使这些生物仍可被选为目标，与原版表达式一致。
 *
 * <p>Note the original lists use the <b>1.12.2 entity name</b> and match with {@code String#contains}, so entries such
 * as {@code "srmonstress"} match a whole namespace rather than an exact id. The port keeps that substring rule
 * verbatim; {@code "minecraft:creeper"} still matches because {@code Identifier#toString()} contains it.
 */
public final class MobAttacking {
    /** Original {@code name.contains("srparasites")} guard, expressed against this project's namespace. */
    private static final String SRP_NAMESPACE = "csrp";

    private MobAttacking() {
    }

    /**
     * Port of {@code ParasiteEventEntity#checkName}: {@code true} when the blacklist matched and the list is not
     * inverted, i.e. the entity is <b>blacklisted</b>.
     */
    public static boolean checkName(String potentialElement, boolean isWhitelist) {
        if (potentialElement == null) {
            return false;
        }
        boolean anyMatch = false;
        for (String entry : Config.mobAttackingBlacklist()) {
            if (entry != null && !entry.isEmpty() && potentialElement.contains(entry)) {
                anyMatch = true;
                break;
            }
        }
        return anyMatch != isWhitelist;
    }

    /** Port of {@code ParasiteEventEntity#checkEntity} for a live entity. */
    public static boolean checkEntity(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return false;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return checkName(id == null ? null : id.toString(), Config.mobAttackingBlacklistInverted());
    }

    /**
     * Port of {@code EntityParasiteBase#canAttackClass}: {@code true} when a parasite may target this entity class.
     * Always {@code false} for this mod's own entities.
     */
    public static boolean canAttackEntity(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return true;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) {
            return true;
        }
        if (SRP_NAMESPACE.equals(id.getNamespace())) {
            return false;
        }
        if (!Config.mobAttackingFull()) {
            return true;
        }
        return !checkEntity(entity);
    }
}
