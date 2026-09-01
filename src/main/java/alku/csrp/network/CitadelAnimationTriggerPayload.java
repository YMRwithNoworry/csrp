package alku.csrp.network;

import alku.csrp.Csrp;
import alku.csrp.animation.CitadelAnimatedEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Synchronizes one-shot Tabula animation triggers to tracking clients. */
public record CitadelAnimationTriggerPayload(int entityId, String controller, String animation)
        implements CustomPacketPayload {
    public static final Type<CitadelAnimationTriggerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Csrp.MODID, "citadel_animation_trigger"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CitadelAnimationTriggerPayload> STREAM_CODEC =
            StreamCodec.ofMember(CitadelAnimationTriggerPayload::encode,
                    CitadelAnimationTriggerPayload::decode);

    private void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeUtf(controller, 64);
        buffer.writeUtf(animation, 64);
    }

    private static CitadelAnimationTriggerPayload decode(RegistryFriendlyByteBuf buffer) {
        return new CitadelAnimationTriggerPayload(buffer.readVarInt(),
                buffer.readUtf(64), buffer.readUtf(64));
    }

    public static void handle(CitadelAnimationTriggerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Entity entity = context.player().level().getEntity(payload.entityId());
            if (entity instanceof CitadelAnimatedEntity animated) {
                animated.getCitadelAnimationCache().trigger(
                        payload.controller(), payload.animation(), entity.tickCount);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
