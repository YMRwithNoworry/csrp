package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

@EventBusSubscriber(modid = Csrp.MODID)
public final class ModPermissions {
    public static final PermissionNode<Boolean> PARASITE_BARRIER_CONFIGURE = new PermissionNode<>(
            Csrp.MODID, "srpbarrier", PermissionTypes.BOOLEAN,
            (player, playerUuid, context) -> player != null
                    && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));

    private ModPermissions() {
    }

    @SubscribeEvent
    public static void registerNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(PARASITE_BARRIER_CONFIGURE);
    }
}
