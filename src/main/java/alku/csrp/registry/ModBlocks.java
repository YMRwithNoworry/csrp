package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.block.InfestedBlock;
import alku.csrp.block.InfestedStairBlock;
import alku.csrp.block.InfestedWallBlock;
import alku.csrp.block.BiomeHeartBlock;
import alku.csrp.block.ColonyHeartBlock;
import alku.csrp.block.ColonyStructureBlock;
import alku.csrp.block.ResidueBlock;
import alku.csrp.block.ResidueBloomingBlock;
import alku.csrp.block.InfestedResidueBlock;
import alku.csrp.block.InfestationPurifierBlock;
import alku.csrp.block.EvolutionLureBlock;
import alku.csrp.block.ParasiteTrapBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SlabBlock;
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

    public static final DeferredBlock<ResidueBloomingBlock> RESIDUE_PLANTS = BLOCKS.register("residue_plants", () -> new ResidueBloomingBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .noOcclusion()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)));
    public static final DeferredBlock<ResidueBlock> RESIDUE_BLOCK = BLOCKS.register("residue_block", () -> new ResidueBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F)
                    .sound(SoundType.ROOTED_DIRT)));
    public static final DeferredBlock<InfestedResidueBlock> INFESTED_REMAINS = BLOCKS.register("infestremain", () ->
            new InfestedResidueBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .noCollission().noOcclusion().randomTicks().instabreak().sound(SoundType.ROOTED_DIRT)));
    public static final DeferredBlock<ParasiteTrapBlock> BIOMASS_BLOCK = BLOCKS.register("biomass_block", () ->
            new ParasiteTrapBlock(ParasiteTrapBlock.Kind.BIOMASS, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN).strength(0.6F).friction(0.8F)
                    .lightLevel(state -> 6).sound(SoundType.SLIME_BLOCK)));
    public static final DeferredBlock<ParasiteTrapBlock> PARASITE_MOUTH = BLOCKS.register("parasitemouth", () ->
            new ParasiteTrapBlock(ParasiteTrapBlock.Kind.MAW, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.4F).noOcclusion().sound(SoundType.ROOTED_DIRT)));
    public static final DeferredBlock<Block> HIVESTONE_DEBRIS = BLOCKS.register("parasiterubble_stonedebris", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.3F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final DeferredBlock<InfestedBlock> INFESTED_STAIN = infested("infestedstain", MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final DeferredBlock<InfestedBlock> INFESTED_RUBBLE = infested("infestedrubble", MapColor.COLOR_RED, SoundType.STONE);
    public static final DeferredBlock<InfestedBlock> INFESTED_SAND = infested("infestedsand", MapColor.COLOR_RED, SoundType.SAND);
    public static final DeferredBlock<InfestedBlock> INFESTED_COBBLESTONE = infested("infested_cobblestone", MapColor.COLOR_RED, SoundType.STONE);
    public static final DeferredBlock<InfestedBlock> INFESTED_TRUNK = infested("infestedtrunk", MapColor.COLOR_RED, SoundType.WOOD);
    public static final DeferredBlock<InfestedBlock> INFESTED_PLANKS = infested("infested_planks", MapColor.COLOR_RED, SoundType.WOOD);
    public static final DeferredBlock<InfestedBlock> INFESTED_STONE_BRICKS = infested(
            "infested_stone_bricks", 1.5F, 10.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final DeferredBlock<InfestedBlock> INFESTED_TERRACOTTA = infested(
            "infested_terracotta", 1.25F, 4.2F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final DeferredBlock<InfestedBlock> POLISHED_INFESTED_STONE = infested(
            "infested_stone_polished", 1.5F, 10.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final DeferredBlock<InfestedBlock> RESIDUE_BRICKS = infested(
            "residue_bricks", 1.5F, 10.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final DeferredBlock<RotatedPillarBlock> INFESTED_COLUMN = BLOCKS.register(
            "infested_column", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.5F, 10.0F).sound(SoundType.STONE)));
    public static final DeferredBlock<InfestedBlock> INFESTED_SANDSTONE = infested(
            "inf_ss", 0.8F, 4.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final DeferredBlock<InfestedBlock> CHISELED_INFESTED_SANDSTONE = infested(
            "inf_ss_chiseled", 0.8F, 4.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final DeferredBlock<InfestedBlock> CUT_INFESTED_SANDSTONE = infested(
            "inf_ss_cut", 0.8F, 4.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);

    public static final DeferredBlock<SlabBlock> INFESTED_COBBLESTONE_SLAB = slab(
            "infested_cobblestone_slab", 2.0F, 3.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> INFESTED_STONE_SLAB = slab(
            "infested_stone_slab", 2.0F, 3.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> INFESTED_DIRT_SLAB = slab(
            "infested_dirt_slab", 0.5F, 0.5F, SoundType.ROOTED_DIRT);
    public static final DeferredBlock<SlabBlock> INFESTED_STONE_BRICK_SLAB = slab(
            "infested_stone_brick_slab", 2.0F, 6.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> INFESTED_TERRACOTTA_SLAB = slab(
            "infested_terracotta_slab", 1.25F, 4.2F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> POLISHED_INFESTED_STONE_SLAB = slab(
            "polished_infested_stone_slab", 2.0F, 6.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> RESIDUE_BRICK_SLAB = slab(
            "residue_brick_slab", 2.0F, 6.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> INFESTED_SANDSTONE_SLAB = slab(
            "infested_sandstone_slab", 0.8F, 4.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> INFESTED_PLANK_SLAB = slab(
            "infested_plank_slab", 2.0F, 3.0F, SoundType.WOOD);

    public static final DeferredBlock<InfestedStairBlock> INFESTED_SANDSTONE_STAIRS = infestedStairs(
            "infested_sandstone_stairs", INFESTED_SANDSTONE);
    public static final DeferredBlock<InfestedStairBlock> RESIDUE_STAIRS = infestedStairs(
            "residue_stairs", RESIDUE_BRICKS);
    public static final DeferredBlock<InfestedStairBlock> INFESTED_PLANKS_STAIRS = infestedStairs(
            "infested_planks_stairs", INFESTED_PLANKS);
    public static final DeferredBlock<InfestedStairBlock> INFESTED_STONE_BRICKS_STAIRS = infestedStairs(
            "infested_stone_bricks_stairs", INFESTED_STONE_BRICKS);
    public static final DeferredBlock<InfestedStairBlock> INFESTED_POLISHED_STONE_BRICKS_STAIRS = infestedStairs(
            "infested_polished_stone_bricks_stairs", POLISHED_INFESTED_STONE);
    public static final DeferredBlock<InfestedStairBlock> INFESTED_STONE_STAIRS = infestedStairs(
            "infested_stone_stairs", INFESTED_RUBBLE);

    public static final DeferredBlock<InfestedWallBlock> RESIDUE_WALL = infestedWall(
            "residue_wall", INFESTED_PLANKS);
    public static final DeferredBlock<InfestedWallBlock> INFESTED_PLANK_WALL = infestedWall(
            "infested_plank_wall", INFESTED_PLANKS);
    public static final DeferredBlock<InfestedWallBlock> POLISHED_INFESTED_STONE_WALL = infestedWall(
            "polished_infested_stone_wall", INFESTED_RUBBLE);
    public static final DeferredBlock<InfestedWallBlock> INFESTED_STONE_BRICK_WALL = infestedWall(
            "infested_stone_brick_wall", INFESTED_RUBBLE);
    public static final DeferredBlock<InfestedWallBlock> INFESTED_SANDSTONE_WALL = infestedWall(
            "infested_sandstone_wall", INFESTED_RUBBLE);
    public static final DeferredBlock<InfestedWallBlock> INFESTED_RUBBLE_WALL = infestedWall(
            "infestedrubble_wall", INFESTED_RUBBLE);
    public static final DeferredBlock<InfestedWallBlock> INFESTED_STAIN_WALL = infestedWall(
            "infestedstain_wall", INFESTED_STAIN);

    public static final DeferredBlock<BiomeHeartBlock> BIOMEHEART = BLOCKS.register("biomeheart", () ->
            new BiomeHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(4.0F, 20.0F).sound(SoundType.SCULK).lightLevel(state -> 5)));
    public static final DeferredBlock<ColonyHeartBlock> COLONYHEART = BLOCKS.register("colonyheart", () ->
            new ColonyHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(4.0F, 20.0F).sound(SoundType.SCULK).lightLevel(state -> 3)));
    public static final DeferredBlock<ColonyStructureBlock> PARASITE_STRUCTURE = BLOCKS.register("parasitestructure", () ->
            new ColonyStructureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.5F, 6.0F).sound(SoundType.SCULK)));
    public static final DeferredBlock<InfestationPurifierBlock> INFESTATION_PURIFIER = BLOCKS.register(
            "infestation_purifier", () -> new InfestationPurifierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(5.0F).sound(SoundType.SPONGE)));
    public static final DeferredBlock<EvolutionLureBlock> EVOLUTION_LURE = BLOCKS.register(
            "evolutionlure", () -> new EvolutionLureBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).sound(SoundType.STONE)));

    private static DeferredBlock<InfestedBlock> infested(String id, MapColor color, SoundType sound) {
        return BLOCKS.register(id, () -> new InfestedBlock(BlockBehaviour.Properties.of()
                .mapColor(color).strength(1.5F, 6.0F).sound(sound)));
    }

    private static DeferredBlock<InfestedBlock> infested(
            String id, float hardness, float resistance, MapColor color, SoundType sound) {
        return BLOCKS.register(id, () -> new InfestedBlock(BlockBehaviour.Properties.of()
                .mapColor(color).strength(hardness, resistance).requiresCorrectToolForDrops().sound(sound)));
    }

    private static DeferredBlock<SlabBlock> slab(String id, float hardness, float resistance, SoundType sound) {
        return BLOCKS.register(id, () -> new SlabBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).strength(hardness, resistance)
                .requiresCorrectToolForDrops().sound(sound)));
    }

    private static DeferredBlock<InfestedStairBlock> infestedStairs(
            String id, DeferredBlock<? extends Block> baseBlock) {
        return BLOCKS.register(id, () -> new InfestedStairBlock(baseBlock.get().defaultBlockState(),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.5F, 10.0F)
                        .sound(SoundType.ROOTED_DIRT)));
    }

    private static DeferredBlock<InfestedWallBlock> infestedWall(
            String id, DeferredBlock<? extends Block> baseBlock) {
        return BLOCKS.register(id, () -> new InfestedWallBlock(
                BlockBehaviour.Properties.ofFullCopy(baseBlock.get())));
    }

    private ModBlocks() {
    }
}
