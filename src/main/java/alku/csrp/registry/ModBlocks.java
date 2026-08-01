package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.block.InfestedBlock;
import alku.csrp.block.BiomeHeartBlock;
import alku.csrp.block.ColonyHeartBlock;
import alku.csrp.block.ColonyStructureBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Csrp.MODID);

    public static final DeferredBlock<Block> TUNNEL = BLOCKS.register("tunnel", () -> new Block(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.5F)
                    .sound(SoundType.SCULK)));

    public static final DeferredBlock<Block> RESIDUE_PLANTS = BLOCKS.register("residue_plants", () -> new Block(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .sound(SoundType.GRASS)));

    public static final DeferredBlock<InfestedBlock> INFESTED_STAIN = infested("infestedstain", MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final DeferredBlock<InfestedBlock> INFESTED_RUBBLE = infested("infestedrubble", MapColor.COLOR_RED, SoundType.STONE);
    public static final DeferredBlock<InfestedBlock> INFESTED_SAND = infested("infestedsand", MapColor.COLOR_RED, SoundType.SAND);
    public static final DeferredBlock<InfestedBlock> INFESTED_COBBLESTONE = infested("infested_cobblestone", MapColor.COLOR_RED, SoundType.STONE);
    public static final DeferredBlock<InfestedBlock> INFESTED_TRUNK = infested("infestedtrunk", MapColor.COLOR_RED, SoundType.WOOD);
    public static final DeferredBlock<InfestedBlock> INFESTED_PLANKS = infested("infested_planks", MapColor.COLOR_RED, SoundType.WOOD);

    public static final DeferredBlock<BiomeHeartBlock> BIOMEHEART = BLOCKS.register("biomeheart", () ->
            new BiomeHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(4.0F, 20.0F).sound(SoundType.SCULK).lightLevel(state -> 5)));
    public static final DeferredBlock<ColonyHeartBlock> COLONYHEART = BLOCKS.register("colonyheart", () ->
            new ColonyHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(4.0F, 20.0F).sound(SoundType.SCULK).lightLevel(state -> 3)));
    public static final DeferredBlock<ColonyStructureBlock> PARASITE_STRUCTURE = BLOCKS.register("parasitestructure", () ->
            new ColonyStructureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.5F, 6.0F).sound(SoundType.SCULK)));

    private static DeferredBlock<InfestedBlock> infested(String id, MapColor color, SoundType sound) {
        return BLOCKS.register(id, () -> new InfestedBlock(BlockBehaviour.Properties.of()
                .mapColor(color).strength(1.5F, 6.0F).sound(sound)));
    }

    private ModBlocks() {
    }
}
