package alku.csrp.entity;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Animation state exposed to the directly ported SRParasites Tabula models.
 *
 * <p>Defaults match the original models' ordinary idle/movement state. More
 * specialized entities override only the values their original model reads.
 * Reflection is retained as a compatibility bridge for legacy state fields,
 * but accessors are resolved and cached once per entity class/name rather than
 * looked up for every model part on every rendered frame.</p>
 */
public interface TabulaAnimationAccess {
    Map<AccessorKey, StateAccessor> STATE_ACCESSORS = new ConcurrentHashMap<>();
    StateAccessor MISSING_ACCESSOR = instance -> null;

    default int getParasiteStatus() {
        return Math.round(number("parasiteStatus", 0.0F));
    }

    default boolean getStillAni() {
        if (this instanceof Entity entity) {
            return entity.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D;
        }
        return bool("stillAni", false);
    }

    default float getAttackTimer() { return number("attackTimer", 0.0F); }
    default float getAttackTimerM() { return number("attackTimerM", getAttackTimer()); }
    default float getAttackTimerR() { return number("attackTimerR", getAttackTimer()); }
    default float getBODY() { return number("body", 0.0F); }
    default boolean getBack() { return bool("back", false); }
    default int getBodyNumber() { return Math.round(number("bodyNumber", 0.0F)); }
    default boolean getBodyTail() { return bool("bodyTail", false); }
    default float getBurrowTimer() { return number("burrowTimer", 0.0F); }
    default boolean getBurrowed() { return bool("burrowed", false); }
    default boolean getCloneC() { return bool("cloneC", false); }
    default float getDigModel() { return number("diggingModel", 0.0F); }
    default boolean getDigging() { return bool("digging", false); }
    default float getFloorTimer() { return number("floorTimer", -1.0F); }
    default boolean getFlyingState() { return bool("flyingState", false); }
    default float getGrowHeight() { return number("growHeight", 0.0F); }
    default float getGrowW() { return number("growW", 0.0F); }
    default float getHead() {
        if (this instanceof AssimilatedDragonEntity dragon) {
            return dragon.hasHead() ? 0.0F : 1.0F;
        }
        return number("head", 0.0F);
    }

    /** Tabula uses zero for an attached limb and non-zero for a detached limb. */
    default float getLeft() {
        if (this instanceof AdaptedVariantEntity adapted) {
            return adapted.isLeftTendrilAttached() ? 0.0F : 1.0F;
        }
        if (this instanceof PureParasiteEntity pure && pure.getKind() == PureParasiteEntity.Kind.VIGILANTE) {
            return pure.isLeftVigilanteTendrilAttached() ? 0.0F : 1.0F;
        }
        if (this instanceof AssimilatedDragonEntity dragon) {
            return dragon.hasLeftWing() ? 0.0F : 1.0F;
        }
        return number("left", 0.0F);
    }

    default boolean getOpen() { return bool("open", false); }

    default float getRight() {
        if (this instanceof AdaptedVariantEntity adapted) {
            return adapted.isRightTendrilAttached() ? 0.0F : 1.0F;
        }
        if (this instanceof PureParasiteEntity pure && pure.getKind() == PureParasiteEntity.Kind.VIGILANTE) {
            return pure.isRightVigilanteTendrilAttached() ? 0.0F : 1.0F;
        }
        if (this instanceof AssimilatedDragonEntity dragon) {
            return dragon.hasRightWing() ? 0.0F : 1.0F;
        }
        return number("right", 0.0F);
    }

    default boolean getRTTS() { return bool("rtts", false); }
    default float getTHeigh() { return number("tHeigh", 0.0F); }
    default Entity getTargetedEntity() {
        Object target = read("targetedEntity");
        return target instanceof Entity entity ? entity : null;
    }
    default float getaaa() { return number("aaa", 0.0F); }
    default boolean helmetSlot() { return bool("helmetSlot", false); }
    default boolean isCrawling() { return bool("crawling", false); }
    default boolean livingTENLA() { return bool("livingTENLA", false); }
    default boolean livingTENRA() { return bool("livingTENRA", false); }
    default boolean livingTENUL() { return bool("livingTENUL", false); }
    default boolean livingTENUR() { return bool("livingTENUR", false); }
    default int shakingC() { return Math.round(number("shakingC", 0.0F)); }
    default float showC() { return number("showC", 0.0F); }
    default int getVomitTicks() { return Math.round(number("vomit", 0.0F)); }
    default boolean isRaining() { return bool("raining", false); }
    default boolean getScreaming() { return bool("screaming", false); }
    default boolean getChargeFlag() { return bool("chargeFlag", false); }
    default float getVerticalVelocity() {
        return this instanceof Entity entity ? (float) entity.getDeltaMovement().y : 0.0F;
    }

    static TabulaAnimationAccess adapt(Entity entity) {
        return new TabulaAnimationAccess() {
            @Override public float getVerticalVelocity() { return (float) entity.getDeltaMovement().y; }
            @Override public boolean getStillAni() {
                return entity.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D;
            }
        };
    }

    private float number(String name, float fallback) {
        Object value = read(name);
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private boolean bool(String name, boolean fallback) {
        Object value = read(name);
        return value instanceof Boolean bool ? bool : fallback;
    }

    private Object read(String name) {
        StateAccessor accessor = STATE_ACCESSORS.computeIfAbsent(
                new AccessorKey(getClass(), name), TabulaAnimationAccess::resolveAccessor);
        return accessor.read(this);
    }

    private static StateAccessor resolveAccessor(AccessorKey key) {
        for (Class<?> type = key.type(); type != null && type != Object.class; type = type.getSuperclass()) {
            try {
                Method method = type.getDeclaredMethod(key.name());
                if (method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    return instance -> {
                        try {
                            return method.invoke(instance);
                        } catch (ReflectiveOperationException | RuntimeException ignored) {
                            return null;
                        }
                    };
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try the same name as a state field, then continue up the hierarchy.
            }
            try {
                Field field = type.getDeclaredField(key.name());
                field.setAccessible(true);
                return instance -> {
                    try {
                        return field.get(instance);
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        return null;
                    }
                };
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Continue with the superclass.
            }
        }
        return MISSING_ACCESSOR;
    }

    record AccessorKey(Class<?> type, String name) {
    }

    @FunctionalInterface
    interface StateAccessor {
        Object read(Object instance);
    }
}
