package alku.csrp.entity;

import alku.csrp.effect.EffectStacking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Applies the six-field scary-orb effect format used by SRP 1.10.7 mob configs. */
final class ConfiguredOrbEffects {
    private ConfiguredOrbEffects() {
    }

    static void apply(PrimitiveParasiteEntity source, LivingEntity target, int nearbyEntities,
                      List<? extends String> configuredEffects) {
        for (String raw : configuredEffects) {
            String[] parts = raw.split(";", -1);
            if (parts.length != 6) {
                continue;
            }
            // NeoForge's Holder API calls this BuiltInRegistries.MOB_EFFECT.wrapAsHolder;
            // Forge 1.20.1 resolves the same holder through getOptional before EffectStacking.apply.
            try {
                int recipient = Integer.parseInt(parts[0].trim());
                int duration = Math.max(0, Integer.parseInt(parts[1].trim())) * 20;
                int amplifier = Integer.parseInt(parts[2].trim());
                int amplifierStep = Integer.parseInt(parts[4].trim());
                int durationStep = Integer.parseInt(parts[5].trim());
                ResourceLocation effectId = ResourceLocation.tryParse(parts[3].trim());
                if (effectId == null) {
                    continue;
                }
                int scaledAmplifier = amplifierStep == 0 ? amplifier
                        : amplifier + nearbyEntities / amplifierStep;
                int scaledDuration = durationStep == 0 ? duration
                        : duration + nearbyEntities / durationStep * 20;
                BuiltInRegistries.MOB_EFFECT.getOptional(effectId).ifPresent(effect -> {
                    if (recipient == 1) {
                        EffectStacking.apply(source, effect,
                                scaledDuration, scaledAmplifier);
                    } else if (recipient == 2) {
                        if (target instanceof Parasite) {
                            EffectStacking.apply(target, effect,
                                    scaledDuration, scaledAmplifier);
                        }
                    } else if (!(target instanceof Parasite)) {
                        EffectStacking.apply(target, effect,
                                scaledDuration, scaledAmplifier);
                    }
                });
            } catch (NumberFormatException ignored) {
                // The config validator rejects malformed entries; tolerate stale hand-edited files at runtime.
            }
        }
    }
}
