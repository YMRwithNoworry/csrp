package alku.csrp.world;

import alku.csrp.Csrp;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

/**
 * 服务端启动时审计 `data/csrp/structures/*.nbt` 能否被 {@link StructureTemplate#load} 加载。
 *
 * <p>为什么需要它：结构 NBT 的调色板一旦引用了**未注册的方块**或**该方块不存在的属性**，
 * {@link StructureTemplate#load} 会抛异常，而调用方（放置器）只能看到「没生成」——
 * 这是本项目里最难发现的一类缺陷（已实测两次：`csrp:infestedbush` 被 10 个结构引用、
 * `csrp:parasitestain_feeler` 方块根本没注册，两者的表现都只是「结构不出现」）。
 *
 * <p>审计只记录日志、不阻断启动：单个结构坏掉不应该让整个模组不可用。
 * 把失败项集中成一条 `ERROR` 汇总行，便于在日志里一眼看到。
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class StructureAudit {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String STRUCTURE_DIR = "structures";

    private StructureAudit() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        List<ResourceLocation> ids = new ArrayList<>();
        server.getResourceManager()
                .listResources(STRUCTURE_DIR, path -> path.getPath().endsWith(".nbt"))
                .keySet().stream()
                .filter(id -> id.getNamespace().equals(Csrp.MODID))
                .sorted()
                .forEach(ids::add);

        if (ids.isEmpty()) {
            return;
        }

        List<String> failures = new ArrayList<>();
        for (ResourceLocation id : ids) {
            String name = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
            if (name.endsWith(".nbt")) {
                name = name.substring(0, name.length() - 4);
            }
            try {
                Resource resource = server.getResourceManager().getResourceOrThrow(id);
                CompoundTag root;
                try (var input = resource.open()) {
                    root = NbtIo.readCompressed(input);
                }
                StructureTemplate template = new StructureTemplate();
                template.load(server.registryAccess().lookupOrThrow(
                        net.minecraft.core.registries.Registries.BLOCK), root);
                if (template.getSize().equals(net.minecraft.core.Vec3i.ZERO)) {
                    failures.add(name + " (empty size)");
                }
            } catch (Throwable throwable) {
                failures.add(name + " (" + throwable.getClass().getSimpleName() + ": "
                        + throwable.getMessage() + ")");
            }
        }

        if (failures.isEmpty()) {
            LOGGER.info("Structure audit: all {} structure templates loaded successfully", ids.size());
        } else {
            LOGGER.error("Structure audit: {}/{} structure templates FAILED to load: {}",
                    failures.size(), ids.size(), failures);
        }
    }

    /** 供调试命令/测试手动触发。 */
    public static List<String> audit(MinecraftServer server) {
        List<String> failures = new ArrayList<>();
        server.getResourceManager().listResources(STRUCTURE_DIR, p -> p.getPath().endsWith(".nbt"))
                .forEach((id, resource) -> {
                    if (!id.getNamespace().equals(Csrp.MODID)) {
                        return;
                    }
                    try {
                        CompoundTag root;
                        try (var input = resource.open()) {
                            root = NbtIo.readCompressed(input);
                        }
                        StructureTemplate template = new StructureTemplate();
                        template.load(server.registryAccess().lookupOrThrow(
                                net.minecraft.core.registries.Registries.BLOCK), root);
                    } catch (Throwable throwable) {
                        failures.add(id + " -> " + throwable);
                    }
                });
        return failures;
    }

    /** 便捷入口：拿当前服务端实例（无则返回 null）。 */
    public static MinecraftServer currentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
