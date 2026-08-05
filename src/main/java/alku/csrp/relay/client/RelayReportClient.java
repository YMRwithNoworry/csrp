package alku.csrp.relay.client;

import alku.csrp.item.RelayReportItem;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public final class RelayReportClient {
    private RelayReportClient() {
    }

    public static void open(String type, CompoundTag data) {
        Minecraft.getInstance().setScreen(new RelayReportScreen(RelayReportItem.Type.byId(type), data));
    }
}
