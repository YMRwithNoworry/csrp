package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.block.InfestedBlock;
import alku.csrp.block.InfestedStairBlock;
import alku.csrp.block.InfestedFenceBlock;
import alku.csrp.block.InfestedSlabBlock;
import alku.csrp.block.InfestedWallBlock;
import alku.csrp.block.NodeLampBlock;
import alku.csrp.block.BiomeHeartBlock;
import alku.csrp.block.ColonyHeartBlock;
import alku.csrp.block.ColonyStructureBlock;
import alku.csrp.block.DeadBloodBlock;
import alku.csrp.block.DeadheadLeavesBlock;
import alku.csrp.block.DispatcherNidusBlock;
import alku.csrp.block.DiseasedSpongeBlock;
import alku.csrp.block.EscaBulbBlock;
import alku.csrp.block.FogBlock;
import alku.csrp.block.FogNullifierBlock;
import alku.csrp.block.GluttonousCystBlock;
import alku.csrp.block.ResidueBlock;
import alku.csrp.block.ResidueBloomingBlock;
import alku.csrp.block.RelayTerminalBlock;
import alku.csrp.block.RelayTowerPartBlock;
import alku.csrp.block.InfestedResidueBlock;
import alku.csrp.block.InfestationPurifierBlock;
import alku.csrp.block.EvolutionLureBlock;
import alku.csrp.block.ParasiteTrapBlock;
import alku.csrp.block.ParasiteThinBlock;
import alku.csrp.block.PestilentialOreBlock;
import alku.csrp.block.ParasiteLootBlock;
import alku.csrp.block.AlveoliBlock;
import alku.csrp.block.AlveoliGrowthBlock;
import alku.csrp.block.AssimilatedJackOLanternBlock;
import alku.csrp.block.AssimilatedPumpkinBlock;
import alku.csrp.block.AssimilatedReedBlock;
import alku.csrp.block.BiomePurifierBlock;
import alku.csrp.block.BladderSacBlock;
import alku.csrp.block.GrotesqueLumpBlock;
import alku.csrp.block.InfestedGlassBlock;
import alku.csrp.block.InfuserFurnaceBlock;
import alku.csrp.block.SickAlveoliBlock;
import alku.csrp.block.SrpWebBlock;
import alku.csrp.block.ThornshadeBlock;
import alku.csrp.block.TrophyBlock;
import alku.csrp.block.TunnelBlock;
import alku.csrp.block.VacuousCystBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.util.DeferredSoundType;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Csrp.MODID);
    private static final SoundType FLESH_SOUND_TYPE = new DeferredSoundType(1.5F, 1.0F,
            () -> ModSounds.get("block.flesh.dig"),
            () -> ModSounds.get("block.flesh.step"),
            () -> ModSounds.get("block.flesh.place"),
            () -> ModSounds.get("block.flesh.hit"),
            () -> ModSounds.get("block.flesh.fall"));
    private static final SoundType TUNNEL_SOUND_TYPE = new DeferredSoundType(1.5F, 1.0F,
            () -> ModSounds.get("block.tunnel.dig"),
            () -> ModSounds.get("block.flesh.step"),
            () -> ModSounds.get("block.flesh.place"),
            () -> ModSounds.get("block.flesh.hit"),
            () -> ModSounds.get("block.flesh.fall"));
    private static final SoundType INFESTED_ORE_SOUND_TYPE = new DeferredSoundType(1.0F, 0.5F,
            () -> ModSounds.get("blockinfest.break"),
            () -> ModSounds.get("blockinfest.step"),
            () -> ModSounds.get("blockinfest.place"),
            () -> ModSounds.get("blockinfest.hit"),
            () -> SoundEvents.STONE_FALL);
    private static final SoundType FLESH_LIGHT_SOUND_TYPE = new DeferredSoundType(1.5F, 1.0F,
            () -> ModSounds.get("block.flesh_light.dig"),
            () -> ModSounds.get("block.flesh_light.step"),
            () -> ModSounds.get("block.flesh_light.place"),
            () -> ModSounds.get("block.flesh_light.hit"),
            () -> ModSounds.get("block.flesh_light.fall"));

    public static final DeferredBlock<TunnelBlock> TUNNEL = BLOCKS.register("tunnel", () -> new TunnelBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.1F, 0.1F)
                    .sound(TUNNEL_SOUND_TYPE)));

    /** 活体寄生囊肿（原版 canisteractive）：寄生体超距消失时落下的资源囊肿。 */
    public static final DeferredBlock<alku.csrp.block.ParasiteCanisterActiveBlock> CANISTER_ACTIVE =
            BLOCKS.register("canisteractive", () -> new alku.csrp.block.ParasiteCanisterActiveBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .noCollission()
                            .noOcclusion()
                            .strength(1.5F)
                            .sound(SoundType.GRASS)));

    // ==================== 批次2：残骸方块体系（原版 parasiterubble/dense/stain/trunk 系） ====================
    public static final DeferredBlock<Block> PARASITERUBBLE_BONE = BLOCKS.register("parasiterubble_bone", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_FLESH = BLOCKS.register("parasiterubble_flesh", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_STONE = BLOCKS.register("parasiterubble_stone", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_WEATHB = BLOCKS.register("parasiterubble_weathb", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_WEATHBC = BLOCKS.register("parasiterubble_weathbc", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_WEATHFS = BLOCKS.register("parasiterubble_weathfs", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_STONEDEBRIS = BLOCKS.register("parasiterubble_stonedebris", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_WOOD = BLOCKS.register("parasiterubble_wood", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> PARASITERUBBLE_BRICKS = BLOCKS.register("parasiterubble_bricks", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_METAL = BLOCKS.register("parasiterubble_metal", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F, 8.0F).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> PARASITERUBBLE_OBSIDIAN = BLOCKS.register("parasiterubble_obsidian", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(5.0F, 1_200.0F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_FUNGUS = BLOCKS.register("parasiterubble_fungus", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.6F).sound(SoundType.FUNGUS)));
    public static final DeferredBlock<Block> PARASITERUBBLEDENSE = BLOCKS.register("parasiterubbledense", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLEDENSE_BIOME = BLOCKS.register("parasiterubbledense_biome", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLEDENSE_COLONY = BLOCKS.register("parasiterubbledense_colony", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLEDENSE_HEART = BLOCKS.register("parasiterubbledense_heart", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESTAIN_FLESH = BLOCKS.register("parasitestain_flesh", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESTAIN_DIRT = BLOCKS.register("parasitestain_dirt", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESTAIN_MUD = BLOCKS.register("parasitestain_mud", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESTAIN_FEELER = BLOCKS.register("parasitestain_feeler", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITETRUNK = BLOCKS.register("parasitetrunk", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> PARASITETRUNK_BALL = BLOCKS.register("parasitetrunk_ball", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> PARASITETRUNK_PLANT = BLOCKS.register("parasitetrunk_plant", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.WOOD)));
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLE_BONESTAIRS = infestedStairs("parasiterubble_bonestairs", PARASITERUBBLE_BONE);
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLE_FLESHSTAIRS = infestedStairs("parasiterubble_fleshstairs", PARASITERUBBLE_FLESH);
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLE_STONESTAIRS = infestedStairs("parasiterubble_stonestairs", PARASITERUBBLE_STONE);
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLE_STONEDEBRISSTAIRS = infestedStairs("parasiterubble_stonedebrisstairs", PARASITERUBBLE_STONEDEBRIS);
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLE_WOODSTAIRS = infestedStairs("parasiterubble_woodstairs", PARASITERUBBLE_WOOD);
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLE_BRICKSSTAIRS = infestedStairs("parasiterubble_bricksstairs", PARASITERUBBLE_BRICKS);
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLE_METALSTAIRS = infestedStairs("parasiterubble_metalstairs", PARASITERUBBLE_METAL);
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLE_OBSIDIANSTAIRS = infestedStairs("parasiterubble_obsidianstairs", PARASITERUBBLE_OBSIDIAN);
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLE_FUNGUSSTAIRS = infestedStairs("parasiterubble_fungusstairs", PARASITERUBBLE_FUNGUS);
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLEDENSE_WALLSTAIRS = infestedStairs("parasiterubbledense_wallstairs", PARASITERUBBLEDENSE);
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLEDENSE_BIOMESTAIRS = infestedStairs("parasiterubbledense_biomestairs", PARASITERUBBLEDENSE_BIOME);
    public static final DeferredBlock<InfestedStairBlock> PARASITERUBBLEDENSE_COLONYSTAIRS = infestedStairs("parasiterubbledense_colonystairs", PARASITERUBBLEDENSE_COLONY);
    public static final DeferredBlock<InfestedStairBlock> PARASITETRUNK_TREESTAIRS = infestedStairs("parasitetrunk_treestairs", PARASITETRUNK);
    public static final DeferredBlock<InfestedStairBlock> PARASITETRUNK_BALLSTAIRS = infestedStairs("parasitetrunk_ballstairs", PARASITETRUNK_BALL);
    public static final DeferredBlock<InfestedStairBlock> PARASITETRUNK_PLANTSTAIRS = infestedStairs("parasitetrunk_plantstairs", PARASITETRUNK_PLANT);
    public static final DeferredBlock<SlabBlock> PARASITERUBBLESLABHALF_BONE = slab("parasiterubbleslabhalf_bone", 1.6F, 6.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> PARASITERUBBLESLABHALF_FLESH = slab("parasiterubbleslabhalf_flesh", 1.6F, 6.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> PARASITERUBBLESLABHALF_STONE = slab("parasiterubbleslabhalf_stone", 1.6F, 6.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> PARASITERUBBLESLABHALF_STONEDEBRIS = slab("parasiterubbleslabhalf_stonedebris", 1.6F, 6.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> PARASITERUBBLESLABHALF_WOOD = slab("parasiterubbleslabhalf_wood", 1.6F, 6.0F, SoundType.WOOD);
    public static final DeferredBlock<SlabBlock> PARASITERUBBLESLABHALF_BRICKS = slab("parasiterubbleslabhalf_bricks", 1.6F, 6.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> PARASITERUBBLESLABHALF_METAL = slab("parasiterubbleslabhalf_metal", 3.0F, 8.0F, SoundType.METAL);
    public static final DeferredBlock<SlabBlock> PARASITERUBBLESLABHALF_OBSIDIAN = slab("parasiterubbleslabhalf_obsidian", 5.0F, 1_200.0F, SoundType.STONE);
    public static final DeferredBlock<SlabBlock> PARASITERUBBLESLABHALF_FUNGUS = slab("parasiterubbleslabhalf_fungus", 0.6F, 1.0F, SoundType.FUNGUS);
    public static final DeferredBlock<InfestedWallBlock> PARASITERUBBLE_FLESH_WALL = infestedWall("parasiterubble_flesh_wall", PARASITERUBBLE_FLESH);
    public static final DeferredBlock<InfestedWallBlock> PARASITERUBBLE_WEATHB_WALL = infestedWall("parasiterubble_weathb_wall", PARASITERUBBLE_WEATHB);
    public static final DeferredBlock<InfestedWallBlock> PARASITERUBBLE_WEATHBC_WALL = infestedWall("parasiterubble_weathbc_wall", PARASITERUBBLE_WEATHBC);
    public static final DeferredBlock<InfestedWallBlock> PARASITERUBBLE_WEATHFS_WALL = infestedWall("parasiterubble_weathfs_wall", PARASITERUBBLE_WEATHFS);
    public static final DeferredBlock<InfestedWallBlock> PARASITERUBBLE_BRICKS_WALL = infestedWall("parasiterubble_bricks_wall", PARASITERUBBLE_BRICKS);
    public static final DeferredBlock<InfestedWallBlock> PARASITERUBBLE_METAL_WALL = infestedWall("parasiterubble_metal_wall", PARASITERUBBLE_METAL);
    public static final DeferredBlock<InfestedWallBlock> PARASITERUBBLEDENSE_BIOME_WALL = infestedWall("parasiterubbledense_biome_wall", PARASITERUBBLEDENSE_BIOME);
    public static final DeferredBlock<InfestedWallBlock> PARASITERUBBLEDENSE_COLONY_WALL = infestedWall("parasiterubbledense_colony_wall", PARASITERUBBLEDENSE_COLONY);
    public static final DeferredBlock<ParasiteThinBlock> PARASITETHIN = BLOCKS.register("parasitethin", () ->
            new ParasiteThinBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.2F)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> PARASITETHIN_TREEBASE = BLOCKS.register("parasitethin_treebase", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITETHIN_TREENESW = BLOCKS.register("parasitethin_treenesw", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESAPLING_TREE = BLOCKS.register("parasitesapling_tree", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<Block> PARASITESAPLING_TREETHIN = BLOCKS.register("parasitesapling_treethin", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<Block> PARASITESAPLING_FLOWERTALL = BLOCKS.register("parasitesapling_flowertall", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<Block> GOTH_STEM = BLOCKS.register("goth_stem", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.WOOD)));
    public static final DeferredBlock<CraftingTableBlock> INFESTED_WORKBENCH = BLOCKS.register("infested_workbench", () -> new CraftingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<CraftingTableBlock> CONSUMED_WORKBENCH = BLOCKS.register("consumed_workbench", () -> new CraftingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.5F).sound(SoundType.WOOD)));

    // ==================== 批次3：木系建材（门/活板门/栅栏） ====================
    public static final DeferredBlock<DoorBlock> GOTH_DOOR = BLOCKS.register("goth_door", () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<DoorBlock> BRUSEWOOD_DOOR = BLOCKS.register("brusewood_door", () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<DoorBlock> CONSUMED_DOOR = BLOCKS.register("consumed_door", () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<DoorBlock> INFESTED_DOOR = BLOCKS.register("infested_door", () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<DoorBlock> FLESH_DOOR = BLOCKS.register("flesh_door", () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<DoorBlock> COOKED_FLESH_DOOR = BLOCKS.register("cooked_flesh_door", () -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> GOTH_TRAPDOOR = BLOCKS.register("goth_trapdoor", () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<TrapDoorBlock> BRUSEWOOD_TRAPDOOR = BLOCKS.register("brusewood_trapdoor", () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<TrapDoorBlock> CONSUMED_TRAPDOOR = BLOCKS.register("consumed_trapdoor", () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<TrapDoorBlock> INFESTED_TRAPDOOR = BLOCKS.register("infested_trapdoor", () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<TrapDoorBlock> FLESH_TRAPDOOR = BLOCKS.register("flesh_trapdoor", () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<TrapDoorBlock> COOKED_FLESH_TRAPDOOR = BLOCKS.register("cooked_flesh_trapdoor", () -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<FenceBlock> GOTH_FENCE = BLOCKS.register("goth_fence", () -> new FenceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> INFESTED_FENCE = BLOCKS.register("infested_fence", () -> new FenceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> CONSUMED_FENCE = BLOCKS.register("consumed_fence", () -> new FenceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> FLESH_FENCE = BLOCKS.register("flesh_fence", () -> new FenceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> DEADHEAD_FENCE = BLOCKS.register("deadhead_fence", () -> new FenceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));

    public static final DeferredBlock<ResidueBloomingBlock> RESIDUE_PLANTS = BLOCKS.register("residue_plants", () -> new ResidueBloomingBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .noOcclusion()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)));
    public static final DeferredBlock<ThornshadeBlock> THORNSHADE = BLOCKS.register("thornshade", () ->
            new ThornshadeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .sound(SoundType.SWEET_BERRY_BUSH)));
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
    /** Compatibility alias for the 1.10.8 hivestone debris id.  The id is
     * registered once above as {@code parasiterubble_stonedebris}; registering
     * it a second time causes a duplicate-key failure during mod loading. */
    public static final DeferredBlock<Block> HIVESTONE_DEBRIS = PARASITERUBBLE_STONEDEBRIS;
    public static final DeferredBlock<ParasiteLootBlock> PARASITE_LOOT_COMMON = parasiteLoot(
            "parasiteloot", ParasiteLootBlock.Tier.COMMON);
    public static final DeferredBlock<ParasiteLootBlock> PARASITE_LOOT_UNCOMMON = parasiteLoot(
            "parasiteloot_uncommon", ParasiteLootBlock.Tier.UNCOMMON);
    public static final DeferredBlock<ParasiteLootBlock> PARASITE_LOOT_RARE = parasiteLoot(
            "parasiteloot_rare", ParasiteLootBlock.Tier.RARE);

    public static final DeferredBlock<InfestedBlock> INFESTED_STAIN = infested("infestedstain", MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final DeferredBlock<InfestedBlock> INFESTED_RUBBLE = infested("infestedrubble", MapColor.COLOR_RED, SoundType.STONE);
    public static final DeferredBlock<InfestedBlock> INFESTED_SAND = infested("infestedsand", MapColor.COLOR_RED, SoundType.SAND);
    public static final DeferredBlock<InfestedBlock> INFESTED_COBBLESTONE = infested("infested_cobblestone", MapColor.COLOR_RED, SoundType.STONE);
    public static final DeferredBlock<InfestedBlock> INFESTED_TRUNK = infested("infestedtrunk", MapColor.COLOR_RED, SoundType.WOOD);
    public static final DeferredBlock<InfestedBlock> INFESTED_PLANKS = infested("infested_planks", MapColor.COLOR_RED, SoundType.WOOD);
    public static final DeferredBlock<ButtonBlock> INFESTED_BUTTON = woodButton("infested_button");
    public static final DeferredBlock<PressurePlateBlock> INFESTED_PRESSURE_PLATE = woodPressurePlate(
            "infested_pressure_plate");
    public static final DeferredBlock<LadderBlock> INFESTED_LADDER = woodLadder("infested_ladder");
    public static final DeferredBlock<Block> INFESTED_BOOKSHELF = woodBookshelf("infested_bookshelf");
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
    public static final DeferredBlock<Block> SEMIORGANIC_BLOCK = BLOCKS.register("semiorganic_block", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<NodeLampBlock> NODE_REDSTONE_LAMP = BLOCKS.register("node_redstone_lamp", () ->
            new NodeLampBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.35F).lightLevel(state -> state.getValue(NodeLampBlock.POWERED) ? 12 : 0)
                    .sound(SoundType.GLASS)));
    public static final DeferredBlock<RelayTerminalBlock> RELAY_BASE = BLOCKS.register("relay_base", () ->
            new RelayTerminalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<RelayTowerPartBlock> RELAY_MIDDLE = BLOCKS.register("relay_middle", () ->
            new RelayTowerPartBlock(1, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<RelayTowerPartBlock> RELAY_ROOF = BLOCKS.register("relay_roof", () ->
            new RelayTowerPartBlock(2, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<InfestationPurifierBlock> INFESTATION_PURIFIER = BLOCKS.register(
            "infestation_purifier", () -> new InfestationPurifierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(5.0F).sound(SoundType.SPONGE)));
    public static final DeferredBlock<EvolutionLureBlock> EVOLUTION_LURE = BLOCKS.register(
            "evolutionlure", () -> new EvolutionLureBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).sound(SoundType.STONE)));
    public static final DeferredBlock<AlveoliBlock> ALVEOLI = BLOCKS.register("alveoli", () ->
            new AlveoliBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0F).noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final DeferredBlock<SickAlveoliBlock> SICK_ALVEOLI = BLOCKS.register("sick_alveoli", () ->
            new SickAlveoliBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0F).noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final DeferredBlock<AlveoliGrowthBlock> ALVEOLI_GROWTH = BLOCKS.register("alveoli_growth", () ->
            new AlveoliGrowthBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .instabreak().noCollission().noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final DeferredBlock<Block> SOLID_ALVEOLI_BLOCK = BLOCKS.register("solid_alveoli_block", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0F).noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final DeferredBlock<RotatedPillarBlock> HAIR_FOLLICLE_BLOCK = BLOCKS.register(
            "hair_follicle_block", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.STONE)));
    public static final DeferredBlock<SrpWebBlock> SRP_WEB = BLOCKS.register("srpweb", () ->
            new SrpWebBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .noCollission()
                    .instabreak()
                    .noOcclusion()
                    .randomTicks()
                    .sound(SoundType.COBWEB)
                    .noLootTable()));
    public static final DeferredBlock<DispatcherNidusBlock> DISPATCHER_NIDUS = BLOCKS.register(
            "dispatcher_nidus", () -> new DispatcherNidusBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 12.0F)
                    .sound(SoundType.SCULK)
                    .noLootTable()));
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_ORE = infestedOre("infested_ore", PestilentialOreBlock.OreKind.TWISTED);
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_COAL_ORE = infestedOre("infested_coal_ore", PestilentialOreBlock.OreKind.COAL);
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_DIAMOND_ORE = infestedOre("infested_diamond_ore", PestilentialOreBlock.OreKind.DIAMOND);
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_EMERALD_ORE = infestedOre("infested_emerald_ore", PestilentialOreBlock.OreKind.EMERALD);
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_GOLD_ORE = infestedOre("infested_gold_ore", PestilentialOreBlock.OreKind.GOLD);
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_IRON_ORE = infestedOre("infested_iron_ore", PestilentialOreBlock.OreKind.IRON);
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_LAPIS_ORE = infestedOre("infested_lapis_ore", PestilentialOreBlock.OreKind.LAPIS);
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_REDSTONE_ORE = infestedOre("infested_redstone_ore", PestilentialOreBlock.OreKind.REDSTONE);

    private static DeferredBlock<PestilentialOreBlock> infestedOre(String id, PestilentialOreBlock.OreKind kind) {
        return BLOCKS.register(id, () -> new PestilentialOreBlock(kind, BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(INFESTED_ORE_SOUND_TYPE)));
    }
    public static final DeferredBlock<GluttonousCystBlock> GLUTTONOUS_CYST = BLOCKS.register(
            "gluttonous_cyst", () -> new GluttonousCystBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(20.0F, 2000.0F)
                    .sound(SoundType.SLIME_BLOCK)
                    .noLootTable()));
    public static final DeferredBlock<VacuousCystBlock> VACUOUS_CYST = BLOCKS.register(
            "vacuous_cyst", () -> new VacuousCystBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F)
                    .randomTicks()
                    .sound(SoundType.SLIME_BLOCK)
                    .noLootTable()));
    public static final DeferredBlock<AssimilatedPumpkinBlock> ASSIMILATED_PUMPKIN = BLOCKS.register(
            "assimilated_pumpkin", () -> new AssimilatedPumpkinBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<AssimilatedJackOLanternBlock> ASSIMILATED_JACK_O_LANTERN = BLOCKS.register(
            "assimilated_jack_o_lantern", () -> new AssimilatedJackOLanternBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .lightLevel(state -> 15)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<AssimilatedReedBlock> ASSIMILATED_REED = BLOCKS.register(
            "assimilated_reed", () -> new AssimilatedReedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .instabreak()
                    .noCollission()
                    .noOcclusion()
                    .sound(SoundType.GRASS)));
    public static final DeferredBlock<BladderSacBlock> BLADDER_SAC = BLOCKS.register(
            "bladder_sac", () -> new BladderSacBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.8F)
                    .sound(SoundType.SLIME_BLOCK)));
    public static final DeferredBlock<GrotesqueLumpBlock> GROTESQUE_LUMP = BLOCKS.register(
            "grotesque_lump", () -> new GrotesqueLumpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.8F)
                    .sound(SoundType.SLIME_BLOCK)));
    public static final DeferredBlock<TrophyBlock> KIRIN_TROPHY = BLOCKS.register(
            "trophy_void_orb", () -> new TrophyBlock(TrophyBlock.Kind.VOID, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0F)
                    .sound(SoundType.SCULK)));
    public static final DeferredBlock<TrophyBlock> DRACONITE_TROPHY = BLOCKS.register(
            "trophy_boom_orb", () -> new TrophyBlock(TrophyBlock.Kind.BOOM, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .sound(SoundType.SCULK)));

    public static final java.util.Map<String, DeferredBlock<EscaBulbBlock>> ESCA_BULBS = registerEscaBulbs();

    public static final DeferredBlock<FogBlock> FOG = BLOCKS.register("fog", () ->
            new FogBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .noCollission()
                    .noOcclusion()
                    .replaceable()
                    .forceSolidOff()
                    .randomTicks()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()));
    public static final DeferredBlock<FogNullifierBlock> FOG_NULLIFIER = BLOCKS.register(
            "fog_nullifier", () -> new FogNullifierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<DeadBloodBlock> DEAD_BLOOD = BLOCKS.register(
            "deadblood", () -> new DeadBloodBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .strength(100.0F)
                    .noLootTable()));
    public static final DeferredBlock<Block> VISCERAL_MUD = BLOCKS.register("visceral_mud", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.5F)
                    .sound(SoundType.MUD)));
    public static final DeferredBlock<Block> BLEEDING_OBSIDIAN = BLOCKS.register("bleeding_obsidian", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
    public static final DeferredBlock<DiseasedSpongeBlock> DISEASED_SPONGE = BLOCKS.register(
            "diseased_sponge", () -> new DiseasedSpongeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.6F)
                    .sound(SoundType.SPONGE)));
    public static final DeferredBlock<InfuserFurnaceBlock> INFUSER_FURNACE = BLOCKS.register(
            "infuser_furnace", () -> new InfuserFurnaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.5F, 10.0F)
                    .lightLevel(state -> 13)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));
    public static final DeferredBlock<BiomePurifierBlock> BIOME_PURIFIER = BLOCKS.register(
            "biomepurifier", () -> new BiomePurifierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(5.0F, 20.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> HARLESKINN_BLOCK = BLOCKS.register("harleskinn_block", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> POLAND_SKIN_BLOCK = BLOCKS.register("poland_skin_block", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.SLIME_BLOCK)));
    public static final DeferredBlock<Block> LOCS_BLOCK = BLOCKS.register("locs_block", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)));
    public static final DeferredBlock<InfestedGlassBlock> INFESTED_GLASS = BLOCKS.register(
            "infested_glass", () -> new InfestedGlassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));
    public static final DeferredBlock<InfestedGlassBlock> BLOODY_GLASS = tintedGlass("bloody_glass");
    public static final DeferredBlock<InfestedGlassBlock> ASHEN_GLASS = tintedGlass("ashen_glass");
    public static final DeferredBlock<InfestedGlassBlock> SEPIA_GLASS = tintedGlass("sepia_glass");
    public static final DeferredBlock<InfestedGlassBlock> HARLEQUINN_GLASS = tintedGlass("harlequinn_glass");
    public static final DeferredBlock<InfestedGlassBlock> SHROUDED_GLASS = tintedGlass("shrouded_glass");
    public static final DeferredBlock<InfestedGlassBlock> MOODY_GLASS = tintedGlass("moody_glass");
    public static final DeferredBlock<InfestedGlassBlock> SHADE_GLASS = tintedGlass("shade_glass");
    public static final DeferredBlock<IronBarsBlock> INFESTED_GLASS_PANE = glassPane("infested_glass_pane");
    public static final DeferredBlock<IronBarsBlock> BLOODY_GLASS_PANE = glassPane("bloody_glass_pane");
    public static final DeferredBlock<IronBarsBlock> ASHEN_GLASS_PANE = glassPane("ashen_glass_pane");
    public static final DeferredBlock<IronBarsBlock> SEPIA_GLASS_PANE = glassPane("sepia_glass_pane");
    public static final DeferredBlock<IronBarsBlock> HARLEQUINN_GLASS_PANE = glassPane("harlequinn_glass_pane");
    public static final DeferredBlock<IronBarsBlock> SHROUDED_GLASS_PANE = glassPane("shrouded_glass_pane");
    public static final DeferredBlock<IronBarsBlock> MOODY_GLASS_PANE = glassPane("moody_glass_pane");
    public static final DeferredBlock<IronBarsBlock> SHADE_GLASS_PANE = glassPane("shade_glass_pane");
    public static final DeferredBlock<Block> GOTHSHROOM = BLOCKS.register("gothshroom", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instabreak()
                    .noCollission()
                    .noOcclusion()
                    .sound(SoundType.FUNGUS)));
    public static final DeferredBlock<InfestedBlock> COOKED_FLESH = BLOCKS.register("cooked_flesh", () ->
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 5.0F).requiresCorrectToolForDrops().sound(TUNNEL_SOUND_TYPE)));
    public static final DeferredBlock<InfestedBlock> COOKED_FLESH_PLANKS = parasiticPlanks(
            "cooked_flesh_planks", MapColor.COLOR_RED);
    public static final DeferredBlock<ButtonBlock> COOKED_FLESH_BUTTON = woodButton("cooked_flesh_button");
    public static final DeferredBlock<PressurePlateBlock> COOKED_FLESH_PRESSURE_PLATE = woodPressurePlate(
            "cooked_flesh_pressure_plate");
    public static final DeferredBlock<LadderBlock> COOKED_FLESH_LADDER = woodLadder("cooked_flesh_ladder");
    public static final DeferredBlock<Block> COOKED_FLESH_BOOKSHELF = woodBookshelf(
            "cooked_flesh_bookshelf");
    public static final DeferredBlock<InfestedBlock> FLESH_PLANKS = parasiticPlanks(
            "flesh_planks", MapColor.COLOR_RED);
    public static final DeferredBlock<ButtonBlock> FLESH_BUTTON = woodButton("flesh_button");
    public static final DeferredBlock<PressurePlateBlock> FLESH_PRESSURE_PLATE = woodPressurePlate(
            "flesh_pressure_plate");
    public static final DeferredBlock<LadderBlock> FLESH_LADDER = woodLadder("flesh_ladder");
    public static final DeferredBlock<Block> FLESH_BOOKSHELF = woodBookshelf("flesh_bookshelf");
    public static final DeferredBlock<InfestedBlock> GOTH_PLANKS = parasiticPlanks(
            "goth_planks", MapColor.COLOR_PURPLE);
    public static final DeferredBlock<ButtonBlock> GOTH_BUTTON = woodButton("goth_button");
    public static final DeferredBlock<PressurePlateBlock> GOTH_PRESSURE_PLATE = woodPressurePlate(
            "goth_pressure_plate");
    public static final DeferredBlock<LadderBlock> GOTH_LADDER = woodLadder("goth_ladder");
    public static final DeferredBlock<Block> GOTH_BOOKSHELF = woodBookshelf("goth_bookshelf");
    public static final DeferredBlock<InfestedBlock> BRUSEWOOD_PLANKS = parasiticPlanks(
            "brusewood_planks", MapColor.COLOR_PURPLE);
    public static final DeferredBlock<ButtonBlock> BRUCEWOOD_BUTTON = woodButton("brucewood_button");
    public static final DeferredBlock<PressurePlateBlock> BRUSEWOOD_PRESSURE_PLATE = woodPressurePlate(
            "brusewood_pressure_plate");
    public static final DeferredBlock<LadderBlock> BRUISEWOOD_LADDER = woodLadder("bruisewood_ladder");
    public static final DeferredBlock<Block> BRUISEWOOD_BOOKSHELF = woodBookshelf("bruisewood_bookshelf");
    public static final DeferredBlock<InfestedBlock> CONSUMED_PLANKS = parasiticPlanks(
            "consumed_planks", MapColor.COLOR_GRAY);
    public static final DeferredBlock<ButtonBlock> CONSUMED_BUTTON = woodButton("consumed_button");
    public static final DeferredBlock<PressurePlateBlock> CONSUMED_PRESSURE_PLATE = woodPressurePlate(
            "consumed_pressure_plate");
    public static final DeferredBlock<LadderBlock> CONSUMED_LADDER = woodLadder("consumed_ladder");
    public static final DeferredBlock<Block> CONSUMED_BOOKSHELF = woodBookshelf("consumed_bookshelf");
    public static final DeferredBlock<InfestedBlock> DEADHEAD_PLANKS = parasiticPlanks(
            "parasiteplank_deadhead", MapColor.COLOR_BROWN);
    public static final DeferredBlock<ButtonBlock> DEADHEAD_BUTTON = woodButton("deadhead_button");
    public static final DeferredBlock<PressurePlateBlock> DEADHEAD_PRESSURE_PLATE = woodPressurePlate(
            "deadhead_pressure_plate");
    public static final DeferredBlock<LadderBlock> DEADHEAD_LADDER = woodLadder("deadhead_ladder");
    public static final DeferredBlock<Block> DEADHEAD_BOOKSHELF = woodBookshelf("deadhead_bookshelf");
    public static final DeferredBlock<InfestedStairBlock> COOKED_FLESH_STAIRS = BLOCKS.register(
            "cooked_flesh_stairs", () -> new InfestedStairBlock(COOKED_FLESH_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                            .strength(1.5F, 10.0F).sound(TUNNEL_SOUND_TYPE)));
    public static final DeferredBlock<InfestedSlabBlock> COOKED_FLESH_SLAB = BLOCKS.register(
            "cooked_flesh_slab", () -> new InfestedSlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(TUNNEL_SOUND_TYPE)));
    public static final DeferredBlock<InfestedFenceBlock> COOKED_FLESH_FENCE = BLOCKS.register(
            "cooked_flesh_fence", () -> new InfestedFenceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.5F, 3.0F).sound(TUNNEL_SOUND_TYPE)));
    public static final DeferredBlock<DeadheadLeavesBlock> DEADHEAD_LEAVES = BLOCKS.register(
            "deadhead_leaves", () -> new DeadheadLeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.2F)
                    .randomTicks()
                    .noOcclusion()
                    .sound(SoundType.GRASS)));

    /**
     * The 1.10.8 jar shipped a number of legacy block ids which are referenced
     * by world saves and structure templates but were not represented by a
     * dedicated modern class.  Keep those ids available using the closest
     * vanilla/infested state shape so old worlds load without missing blocks.
     */
    private static final java.util.Map<String, DeferredBlock<? extends Block>> LEGACY_BLOCKS =
            registerLegacyBlocks();

    private static java.util.Map<String, DeferredBlock<? extends Block>> registerLegacyBlocks() {
        String[] ids = {
                "assimilated_blossom", "bloodyice", "bruisewood_fence",
                "bruisewood_plank_slab", "bruisewood_plank_slab_double", "bruisewood_plank_stairs",
                "bruisewood_plank_wall", "colonyoutpost", "consumed_plank_slab",
                "consumed_plank_slab_double", "consumed_plank_wall", "consumed_planks_stairs",
                "consumed_pot", "cooked_flesh_slab_double", "dead_head_plank_slab",
                "dead_head_plank_slab_double", "deadhead_plank_stairs", "dermoid_cyst",
                "dispatchern", "epitome_infestation_warp_diffuser", "flesh_slab", "flesh_slab_double",
                "flesh_stairs", "frost_weathered_stone_slab", "frost_weathered_stone_slab_double",
                "frost_weathered_stone_stairs", "goreada", "gorefer", "goremar", "gorepri",
                "gorepur", "goresim", "goth_plank_slab", "goth_plank_slab_double", "goth_plank_wall",
                "goth_planks_stairs", "harlequinn_grass", "harleskinn_fence", "harleskinn_slab",
                "harleskinn_slab_double", "harleskinn_stairs", "hirsute_hair", "infested_cactus",
                "infested_cobblestone_slab_double", "infested_dirt_slab_double", "infested_furnace",
                "infested_furnace_lit", "infested_leaves", "infested_leaves_fast",
                "infested_plank_slab_double", "infested_pot", "infested_sandstone_slab_double",
                "infested_stone_brick_slab_double", "infested_stone_slab_double",
                "infested_terracotta_slab_double", "infestedbush", "infestedore", "infestedremain",
                "infestedrubblestairs", "infestedstainstairs", "infestedtrunkstairs", "lipoma_mass",
                "locs_block_slab", "locs_block_slab_double", "noderelay", "parasite_barrier",
                "parasitebush", "parasitecanister", "parasitecanister_bag_wall", "parasitefog",
                "parasiteplank", "parasiteplank_deadhead_wall", "parasiterubble",
                "parasiterubbleslabdouble", "parasiterubbleslabhalf", "parasitesapling", "parasitestain",
                "parasitestain_dirtstairs", "parasitestain_feelerstairs", "parasitestain_flesh_wall",
                "parasitestain_fleshstairs", "parasitestain_mudstairs", "parasitestainslabdouble",
                "parasitestainslabhalf", "parasitetendril", "parasitic_colony_core_slab",
                "parasitic_colony_core_slab_double", "parasitic_compressed_colony_stone_slab",
                "parasitic_compressed_colony_stone_slab_double", "poland_skin_slab",
                "poland_skin_slab_double", "polished_infested_stone_slab_double",
                "potted_assimilated_blossom", "potted_consumed_assimilated_blossom",
                "reinforced_hivestone_slab", "reinforced_hivestone_slab_double", "relay_controller_dummy",
                "relaycontroller", "residue_brick_slab_double", "sac_of_flesh_slab",
                "sac_of_flesh_slab_double", "tresses_hair", "weathered_bricks_slab",
                "weathered_bricks_slab_double", "weathered_cobblestone_slab",
                "weathered_cobblestone_slab_double", "wheathered_bricks_stairs",
                "wheathered_cobblestone_stairs"
        };
        java.util.Map<String, DeferredBlock<? extends Block>> result = new java.util.LinkedHashMap<>();
        for (String id : ids) {
            DeferredBlock<? extends Block> holder;
            if (id.endsWith("_stairs") || id.endsWith("stairs")) {
                holder = BLOCKS.register(id, () -> new InfestedStairBlock(Blocks.STONE.defaultBlockState(),
                        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                                .strength(1.5F, 10.0F).sound(SoundType.ROOTED_DIRT)));
            } else if (id.endsWith("_slab") || id.endsWith("_slab_double")
                    || id.endsWith("slabhalf") || id.endsWith("slabdouble")) {
                holder = BLOCKS.register(id, () -> new InfestedSlabBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F)
                        .sound(SoundType.ROOTED_DIRT)));
            } else if (id.endsWith("_wall") || id.endsWith("wall")) {
                holder = BLOCKS.register(id, () -> new InfestedWallBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F).sound(SoundType.ROOTED_DIRT)));
            } else if (id.endsWith("_fence") || id.endsWith("fence")) {
                holder = BLOCKS.register(id, () -> new InfestedFenceBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));
            } else {
                holder = BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F).sound(SoundType.ROOTED_DIRT)));
            }
            result.put(id, holder);
        }
        return java.util.Map.copyOf(result);
    }

    private static DeferredBlock<InfestedGlassBlock> tintedGlass(String id) {
        return BLOCKS.register(id, () -> new InfestedGlassBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .noOcclusion()));
    }

    private static DeferredBlock<IronBarsBlock> glassPane(String id) {
        return BLOCKS.register(id, () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .noOcclusion()));
    }

    private static DeferredBlock<ButtonBlock> woodButton(String id) {
        return BLOCKS.register(id, () -> new ButtonBlock(
                BlockSetType.OAK, 30, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED).noCollission().strength(0.5F).sound(SoundType.WOOD)));
    }

    private static DeferredBlock<PressurePlateBlock> woodPressurePlate(String id) {
        return BLOCKS.register(id, () -> new PressurePlateBlock(
                BlockSetType.OAK, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED).noCollission().strength(0.5F).sound(SoundType.WOOD)));
    }

    private static DeferredBlock<LadderBlock> woodLadder(String id) {
        return BLOCKS.register(id, () -> new LadderBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).noCollission().strength(0.4F).sound(SoundType.LADDER)));
    }

    private static DeferredBlock<Block> woodBookshelf(String id) {
        return BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).strength(1.5F).sound(SoundType.WOOD)));
    }

    private static DeferredBlock<InfestedBlock> parasiticPlanks(String id, MapColor color) {
        return BLOCKS.register(id, () -> new InfestedBlock(BlockBehaviour.Properties.of()
                .mapColor(color).strength(2.0F, 5.0F)
                .requiresCorrectToolForDrops().sound(TUNNEL_SOUND_TYPE)));
    }

    private static java.util.Map<String, DeferredBlock<EscaBulbBlock>> registerEscaBulbs() {
        java.util.Map<String, DeferredBlock<EscaBulbBlock>> bulbs = new java.util.LinkedHashMap<>();
        String[] colors = {"", "white", "light_gray", "gray", "black", "brown", "red", "orange",
                "yellow", "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"};
        for (String color : colors) {
            String id = color.isEmpty() ? "esca_bulb" : "esca_bulb_" + color;
            bulbs.put(color.isEmpty() ? "base" : color, BLOCKS.register(id, () ->
                    new EscaBulbBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.0F)
                            .noOcclusion()
                            .lightLevel(state -> 15)
                            .sound(FLESH_LIGHT_SOUND_TYPE))));
        }
        return java.util.Map.copyOf(bulbs);
    }

    private static DeferredBlock<InfestedBlock> infested(String id, MapColor color, SoundType sound) {
        return BLOCKS.register(id, () -> new InfestedBlock(BlockBehaviour.Properties.of()
                .mapColor(color).strength(1.5F, 6.0F).sound(sound)));
    }

    private static DeferredBlock<ParasiteLootBlock> parasiteLoot(String id, ParasiteLootBlock.Tier tier) {
        return BLOCKS.register(id, () -> new ParasiteLootBlock(tier, BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).strength(2.0F, 8.0F).sound(SoundType.SCULK)));
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
