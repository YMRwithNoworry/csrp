package alku.csrp.entity;

import alku.csrp.Csrp;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Runtime coverage for the bounded entity query used by meteor impacts. */
@EventBusSubscriber(modid = Csrp.MODID)
public final class MeteorImpactGameTests {
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private MeteorImpactGameTests() {
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Csrp.MODID, "meteor"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, EMPTY_STRUCTURE, 20, 1, true, Rotation.NONE);
        event.registerTest(Identifier.fromNamespaceAndPath(Csrp.MODID, "meteor/impact_targets"),
                new ImpactTargetsTest(data));
    }

    private static final class ImpactTargetsTest extends GameTestInstance {
        ImpactTargetsTest(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            ServerLevel level = helper.getLevel();
            BlockPos impact = helper.absolutePos(new BlockPos(0, 1, 0));
            LivingEntity inside = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 1, 0));
            LivingEntity outside = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(6, 1, 0));

            List<LivingEntity> targets = MeteorEntity.collectImpactTargets(level, impact, 4);
            helper.assertTrue(targets.contains(inside), "local living entity was omitted from impact query");
            helper.assertFalse(targets.contains(outside), "living entity outside the AABB was returned");
            helper.succeed();
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Meteor impact target query respects its local AABB");
        }
    }
}
