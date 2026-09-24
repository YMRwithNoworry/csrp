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
import alku.csrp.block.DeadheadGrassShortBlock;
import alku.csrp.block.DeadheadGrassTallBlock;
import alku.csrp.block.DeadheadLeavesBlock;
import alku.csrp.block.SnowCoveredGrassBlock;
import alku.csrp.block.SnowShortGrassBlock;
import alku.csrp.block.SnowTallGrassBlock;
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
import alku.csrp.block.AssimilatedBlossomBlock;
import alku.csrp.block.AssimilatedJackOLanternBlock;
import alku.csrp.block.AssimilatedPumpkinBlock;
import alku.csrp.block.AssimilatedReedBlock;
import alku.csrp.block.BloodyIceBlock;
import alku.csrp.block.ColonyOutpostBlock;
import alku.csrp.block.DermoidCystBlock;
import alku.csrp.block.DispatcherNBlock;
import alku.csrp.block.EpitomeDiffuserBlock;
import alku.csrp.block.GoreBlock;
import alku.csrp.block.HarlequinnGrassBlock;
import alku.csrp.block.HirsuteHairBlock;
import alku.csrp.block.InfestedBushBlock;
import alku.csrp.block.InfestedCactusBlock;
import alku.csrp.block.InfestedFurnaceBlock;
import alku.csrp.block.InfestedLeavesBlock;
import alku.csrp.block.InfestedOreBlock;
import alku.csrp.block.InfestedRemainBlock;
import alku.csrp.block.LegacyFenceBlock;
import alku.csrp.block.LegacyRelayBlock;
import alku.csrp.block.LegacySlabBlock;
import alku.csrp.block.LegacyStairBlock;
import alku.csrp.block.LegacyWallBlock;
import alku.csrp.block.LegacyVariantSlabBlock;
import alku.csrp.block.LipomaMassBlock;
import alku.csrp.block.ParasiteBarrierBlock;
import alku.csrp.block.ParasiteBushBlock;
import alku.csrp.block.ParasiteCanisterBlock;
import alku.csrp.block.ParasiteFogBlock;
import alku.csrp.block.ParasitePlankBlock;
import alku.csrp.block.ParasiteRubbleBlock;
import alku.csrp.block.ParasiteSaplingBlock;
import alku.csrp.block.ParasiteStainBlock;
import alku.csrp.block.ParasiteTendrilBlock;
import alku.csrp.block.ParasiteTrunkBlock;
import alku.csrp.block.PottedSrpBlock;
import alku.csrp.block.TressesHairBlock;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
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
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
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

    public static final DeferredBlock<TunnelBlock> TUNNEL = BLOCKS.register("tunnel", key -> new TunnelBlock(
            BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .noCollision()
                    .noOcclusion()
                    .strength(0.1F, 0.1F)
                    .sound(TUNNEL_SOUND_TYPE)));

    /** 活体寄生囊肿（原版 canisteractive）：寄生体超距消失时落下的资源囊肿。 */
    public static final DeferredBlock<alku.csrp.block.ParasiteCanisterActiveBlock> CANISTER_ACTIVE =
            BLOCKS.register("canisteractive", key -> new alku.csrp.block.ParasiteCanisterActiveBlock(
                    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                            .mapColor(MapColor.COLOR_RED)
                            .noCollision()
                            .noOcclusion()
                            .strength(1.5F)
                            .sound(SoundType.GRASS)));

    // ==================== 批次2：残骸方块体系（原版 parasiterubble/dense/stain/trunk 系） ====================
    public static final DeferredBlock<Block> PARASITERUBBLE_BONE = BLOCKS.register("parasiterubble_bone", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_FLESH = BLOCKS.register("parasiterubble_flesh", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_STONE = BLOCKS.register("parasiterubble_stone", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_WEATHB = BLOCKS.register("parasiterubble_weathb", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_WEATHBC = BLOCKS.register("parasiterubble_weathbc", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_WEATHFS = BLOCKS.register("parasiterubble_weathfs", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_STONEDEBRIS = BLOCKS.register("parasiterubble_stonedebris", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_WOOD = BLOCKS.register("parasiterubble_wood", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> PARASITERUBBLE_BRICKS = BLOCKS.register("parasiterubble_bricks", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_METAL = BLOCKS.register("parasiterubble_metal", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F, 8.0F).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> PARASITERUBBLE_OBSIDIAN = BLOCKS.register("parasiterubble_obsidian", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(5.0F, 1_200.0F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLE_FUNGUS = BLOCKS.register("parasiterubble_fungus", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(0.6F).sound(SoundType.FUNGUS)));
    public static final DeferredBlock<Block> PARASITERUBBLEDENSE = BLOCKS.register("parasiterubbledense", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLEDENSE_BIOME = BLOCKS.register("parasiterubbledense_biome", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLEDENSE_COLONY = BLOCKS.register("parasiterubbledense_colony", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITERUBBLEDENSE_HEART = BLOCKS.register("parasiterubbledense_heart", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESTAIN_FLESH = BLOCKS.register("parasitestain_flesh", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESTAIN_DIRT = BLOCKS.register("parasitestain_dirt", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESTAIN_MUD = BLOCKS.register("parasitestain_mud", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESTAIN_FEELER = BLOCKS.register("parasitestain_feeler", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESTAIN_SPORE = BLOCKS.register("parasitestain_spore", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESTAIN_RED = BLOCKS.register("parasitestain_red", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESTAIN_SACKFLESH = BLOCKS.register("parasitestain_sackflesh", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    // RotatedPillarBlock so the `axis` property exists; the blockstates rotate the
    // trunk models the same way vanilla logs do.
    public static final DeferredBlock<ParasiteTrunkBlock> PARASITETRUNK = BLOCKS.register("parasitetrunk", key -> new ParasiteTrunkBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.WOOD)));
    public static final DeferredBlock<RotatedPillarBlock> PARASITETRUNK_BALL = BLOCKS.register("parasitetrunk_ball", key -> new RotatedPillarBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.WOOD)));
    public static final DeferredBlock<RotatedPillarBlock> PARASITETRUNK_PLANT = BLOCKS.register("parasitetrunk_plant", key -> new RotatedPillarBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.WOOD)));
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
    public static final DeferredBlock<ParasiteThinBlock> PARASITETHIN = BLOCKS.register("parasitethin", key ->
            new ParasiteThinBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.2F)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> PARASITETHIN_TREEBASE = BLOCKS.register("parasitethin_treebase", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITETHIN_TREENESW = BLOCKS.register("parasitethin_treenesw", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.STONE)));
    public static final DeferredBlock<Block> PARASITESAPLING_TREE = BLOCKS.register("parasitesapling_tree", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).noCollision().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<Block> PARASITESAPLING_TREETHIN = BLOCKS.register("parasitesapling_treethin", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).noCollision().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<Block> PARASITESAPLING_FLOWERTALL = BLOCKS.register("parasitesapling_flowertall", key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).noCollision().noOcclusion().instabreak().sound(SoundType.GRASS)));
    public static final DeferredBlock<RotatedPillarBlock> GOTH_STEM = BLOCKS.register("goth_stem", key -> new RotatedPillarBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.6F).sound(SoundType.WOOD)));
    public static final DeferredBlock<CraftingTableBlock> INFESTED_WORKBENCH = BLOCKS.register("infested_workbench", key -> new CraftingTableBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(2.5F).sound(SoundType.WOOD)));
    public static final DeferredBlock<CraftingTableBlock> CONSUMED_WORKBENCH = BLOCKS.register("consumed_workbench", key -> new CraftingTableBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(2.5F).sound(SoundType.WOOD)));

    // ==================== 批次3：木系建材（门/活板门/栅栏） ====================
    public static final DeferredBlock<DoorBlock> GOTH_DOOR = BLOCKS.register("goth_door", key -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<DoorBlock> BRUSEWOOD_DOOR = BLOCKS.register("brusewood_door", key -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<DoorBlock> CONSUMED_DOOR = BLOCKS.register("consumed_door", key -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<DoorBlock> INFESTED_DOOR = BLOCKS.register("infested_door", key -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<DoorBlock> FLESH_DOOR = BLOCKS.register("flesh_door", key -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<DoorBlock> COOKED_FLESH_DOOR = BLOCKS.register("cooked_flesh_door", key -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<TrapDoorBlock> GOTH_TRAPDOOR = BLOCKS.register("goth_trapdoor", key -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<TrapDoorBlock> BRUSEWOOD_TRAPDOOR = BLOCKS.register("brusewood_trapdoor", key -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<TrapDoorBlock> CONSUMED_TRAPDOOR = BLOCKS.register("consumed_trapdoor", key -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<TrapDoorBlock> INFESTED_TRAPDOOR = BLOCKS.register("infested_trapdoor", key -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<TrapDoorBlock> FLESH_TRAPDOOR = BLOCKS.register("flesh_trapdoor", key -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<TrapDoorBlock> COOKED_FLESH_TRAPDOOR = BLOCKS.register("cooked_flesh_trapdoor", key -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(3.0F).sound(SoundType.WOOD).noOcclusion().isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredBlock<FenceBlock> GOTH_FENCE = BLOCKS.register("goth_fence", key -> new FenceBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> INFESTED_FENCE = BLOCKS.register("infested_fence", key -> new FenceBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> CONSUMED_FENCE = BLOCKS.register("consumed_fence", key -> new FenceBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> FLESH_FENCE = BLOCKS.register("flesh_fence", key -> new FenceBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));
    public static final DeferredBlock<FenceBlock> DEADHEAD_FENCE = BLOCKS.register("deadhead_fence", key -> new FenceBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));

    public static final DeferredBlock<ResidueBloomingBlock> RESIDUE_PLANTS = BLOCKS.register("residue_plants", key -> new ResidueBloomingBlock(
            BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .noCollision()
                    .noOcclusion()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)));
    public static final DeferredBlock<ThornshadeBlock> THORNSHADE = BLOCKS.register("thornshade", key ->
            new ThornshadeBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .noCollision()
                    .noOcclusion()
                    .instabreak()
                    .sound(SoundType.SWEET_BERRY_BUSH)));
    public static final DeferredBlock<ResidueBlock> RESIDUE_BLOCK = BLOCKS.register("residue_block", key -> new ResidueBlock(
            BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F)
                    .sound(SoundType.ROOTED_DIRT)));
    public static final DeferredBlock<InfestedResidueBlock> INFESTED_REMAINS = BLOCKS.register("infestremain", key ->
            new InfestedResidueBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
                    .noCollision().noOcclusion().randomTicks().instabreak().sound(SoundType.ROOTED_DIRT)));
    public static final DeferredBlock<ParasiteTrapBlock> BIOMASS_BLOCK = BLOCKS.register("biomass_block", key ->
            new ParasiteTrapBlock(ParasiteTrapBlock.Kind.BIOMASS, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_GREEN).strength(0.6F).friction(0.8F)
                    .lightLevel(state -> 6).sound(SoundType.SLIME_BLOCK)));
    public static final DeferredBlock<ParasiteTrapBlock> PARASITE_MOUTH = BLOCKS.register("parasitemouth", key ->
            new ParasiteTrapBlock(ParasiteTrapBlock.Kind.MAW, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
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
            "infested_column", key -> new RotatedPillarBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
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

    public static final DeferredBlock<BiomeHeartBlock> BIOMEHEART = BLOCKS.register("biomeheart", key ->
            new BiomeHeartBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
                    .strength(4.0F, 20.0F).sound(SoundType.SCULK).lightLevel(state -> 5)));
    public static final DeferredBlock<ColonyHeartBlock> COLONYHEART = BLOCKS.register("colonyheart", key ->
            new ColonyHeartBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
                    .strength(4.0F, 20.0F).sound(SoundType.SCULK).lightLevel(state -> 3)));
    public static final DeferredBlock<ColonyStructureBlock> PARASITE_STRUCTURE = BLOCKS.register("parasitestructure", key ->
            new ColonyStructureBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
                    .strength(1.5F, 6.0F).sound(SoundType.SCULK)));
    public static final DeferredBlock<Block> SEMIORGANIC_BLOCK = BLOCKS.register("semiorganic_block", key ->
            new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<NodeLampBlock> NODE_REDSTONE_LAMP = BLOCKS.register("node_redstone_lamp", key ->
            new NodeLampBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.35F).lightLevel(state -> state.getValue(NodeLampBlock.POWERED) ? 12 : 0)
                    .sound(SoundType.GLASS)));
    public static final DeferredBlock<RelayTerminalBlock> RELAY_BASE = BLOCKS.register("relay_base", key ->
            new RelayTerminalBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<RelayTowerPartBlock> RELAY_MIDDLE = BLOCKS.register("relay_middle", key ->
            new RelayTowerPartBlock(1, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<RelayTowerPartBlock> RELAY_ROOF = BLOCKS.register("relay_roof", key ->
            new RelayTowerPartBlock(2, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<InfestationPurifierBlock> INFESTATION_PURIFIER = BLOCKS.register(
            "infestation_purifier", key -> new InfestationPurifierBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(5.0F).sound(SoundType.SPONGE)));
    public static final DeferredBlock<EvolutionLureBlock> EVOLUTION_LURE = BLOCKS.register(
            "evolutionlure", key -> new EvolutionLureBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).sound(SoundType.STONE)));
    public static final DeferredBlock<AlveoliBlock> ALVEOLI = BLOCKS.register("alveoli", key ->
            new AlveoliBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
                    .strength(1.0F).noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final DeferredBlock<SickAlveoliBlock> SICK_ALVEOLI = BLOCKS.register("sick_alveoli", key ->
            new SickAlveoliBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
                    .strength(1.0F).noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final DeferredBlock<AlveoliGrowthBlock> ALVEOLI_GROWTH = BLOCKS.register("alveoli_growth", key ->
            new AlveoliGrowthBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
                    .instabreak().noCollision().noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final DeferredBlock<Block> SOLID_ALVEOLI_BLOCK = BLOCKS.register("solid_alveoli_block", key ->
            new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
                    .strength(1.0F).noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final DeferredBlock<RotatedPillarBlock> HAIR_FOLLICLE_BLOCK = BLOCKS.register(
            "hair_follicle_block", key -> new RotatedPillarBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.STONE)));
    public static final DeferredBlock<SrpWebBlock> SRP_WEB = BLOCKS.register("srpweb", key ->
            new SrpWebBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.SNOW)
                    .noCollision()
                    .instabreak()
                    .noOcclusion()
                    .randomTicks()
                    .sound(SoundType.COBWEB)
                    .noLootTable()));
    public static final DeferredBlock<DispatcherNidusBlock> DISPATCHER_NIDUS = BLOCKS.register(
            "dispatcher_nidus", key -> new DispatcherNidusBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
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
        return BLOCKS.register(id, key -> new PestilentialOreBlock(kind, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED)
                .strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(INFESTED_ORE_SOUND_TYPE)));
    }
    public static final DeferredBlock<GluttonousCystBlock> GLUTTONOUS_CYST = BLOCKS.register(
            "gluttonous_cyst", key -> new GluttonousCystBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .strength(20.0F, 2000.0F)
                    .sound(SoundType.SLIME_BLOCK)
                    .noLootTable()));
    public static final DeferredBlock<VacuousCystBlock> VACUOUS_CYST = BLOCKS.register(
            "vacuous_cyst", key -> new VacuousCystBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F)
                    .randomTicks()
                    .sound(SoundType.SLIME_BLOCK)
                    .noLootTable()));
    public static final DeferredBlock<AssimilatedPumpkinBlock> ASSIMILATED_PUMPKIN = BLOCKS.register(
            "assimilated_pumpkin", key -> new AssimilatedPumpkinBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<AssimilatedJackOLanternBlock> ASSIMILATED_JACK_O_LANTERN = BLOCKS.register(
            "assimilated_jack_o_lantern", key -> new AssimilatedJackOLanternBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .lightLevel(state -> 15)
                    .sound(SoundType.WOOD)));
    public static final DeferredBlock<AssimilatedReedBlock> ASSIMILATED_REED = BLOCKS.register(
            "assimilated_reed", key -> new AssimilatedReedBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .instabreak()
                    .noCollision()
                    .noOcclusion()
                    .sound(SoundType.GRASS)));
    public static final DeferredBlock<BladderSacBlock> BLADDER_SAC = BLOCKS.register(
            "bladder_sac", key -> new BladderSacBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.8F)
                    .sound(SoundType.SLIME_BLOCK)));
    public static final DeferredBlock<GrotesqueLumpBlock> GROTESQUE_LUMP = BLOCKS.register(
            "grotesque_lump", key -> new GrotesqueLumpBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.8F)
                    .sound(SoundType.SLIME_BLOCK)));
    public static final DeferredBlock<TrophyBlock> KIRIN_TROPHY = BLOCKS.register(
            "trophy_void_orb", key -> new TrophyBlock(TrophyBlock.Kind.VOID, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0F)
                    .sound(SoundType.SCULK)));
    public static final DeferredBlock<TrophyBlock> DRACONITE_TROPHY = BLOCKS.register(
            "trophy_boom_orb", key -> new TrophyBlock(TrophyBlock.Kind.BOOM, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .sound(SoundType.SCULK)));

    public static final java.util.Map<String, DeferredBlock<EscaBulbBlock>> ESCA_BULBS = registerEscaBulbs();

    public static final DeferredBlock<FogBlock> FOG = BLOCKS.register("fog", key ->
            new FogBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.NONE)
                    .noCollision()
                    .noOcclusion()
                    .replaceable()
                    .forceSolidOff()
                    .randomTicks()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()));
    public static final DeferredBlock<FogNullifierBlock> FOG_NULLIFIER = BLOCKS.register(
            "fog_nullifier", key -> new FogNullifierBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<DeadBloodBlock> DEAD_BLOOD = BLOCKS.register(
            "deadblood", key -> new DeadBloodBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .noCollision()
                    .strength(100.0F)
                    .noLootTable()));
    public static final DeferredBlock<Block> VISCERAL_MUD = BLOCKS.register("visceral_mud", key ->
            new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.5F)
                    .sound(SoundType.MUD)));
    public static final DeferredBlock<Block> BLEEDING_OBSIDIAN = BLOCKS.register("bleeding_obsidian", key ->
            new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
    public static final DeferredBlock<DiseasedSpongeBlock> DISEASED_SPONGE = BLOCKS.register(
            "diseased_sponge", key -> new DiseasedSpongeBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.6F)
                    .sound(SoundType.SPONGE)));
    public static final DeferredBlock<InfuserFurnaceBlock> INFUSER_FURNACE = BLOCKS.register(
            "infuser_furnace", key -> new InfuserFurnaceBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.5F, 10.0F)
                    .lightLevel(state -> 13)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));
    public static final DeferredBlock<BiomePurifierBlock> BIOME_PURIFIER = BLOCKS.register(
            "biomepurifier", key -> new BiomePurifierBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(5.0F, 20.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> HARLESKINN_BLOCK = BLOCKS.register("harleskinn_block", key ->
            new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)));
    public static final DeferredBlock<Block> POLAND_SKIN_BLOCK = BLOCKS.register("poland_skin_block", key ->
            new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.SNOW)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.SLIME_BLOCK)));
    public static final DeferredBlock<Block> LOCS_BLOCK = BLOCKS.register("locs_block", key ->
            new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)));
    public static final DeferredBlock<InfestedGlassBlock> INFESTED_GLASS = BLOCKS.register(
            "infested_glass", key -> new InfestedGlassBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
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
    public static final DeferredBlock<Block> GOTHSHROOM = BLOCKS.register("gothshroom", key ->
            new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_BLACK)
                    .instabreak()
                    .noCollision()
                    .noOcclusion()
                    .sound(SoundType.FUNGUS)));
    public static final DeferredBlock<InfestedBlock> COOKED_FLESH = BLOCKS.register("cooked_flesh", key ->
            new InfestedBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
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
            "cooked_flesh_stairs", key -> new InfestedStairBlock(COOKED_FLESH_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
                            .strength(1.5F, 10.0F).sound(TUNNEL_SOUND_TYPE)));
    public static final DeferredBlock<InfestedSlabBlock> COOKED_FLESH_SLAB = BLOCKS.register(
            "cooked_flesh_slab", key -> new InfestedSlabBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED).strength(1.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(TUNNEL_SOUND_TYPE)));
    public static final DeferredBlock<InfestedFenceBlock> COOKED_FLESH_FENCE = BLOCKS.register(
            "cooked_flesh_fence", key -> new InfestedFenceBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_RED).strength(1.5F, 3.0F).sound(TUNNEL_SOUND_TYPE)));
    public static final DeferredBlock<DeadheadLeavesBlock> DEADHEAD_LEAVES = BLOCKS.register(
            "deadhead_leaves", key -> new DeadheadLeavesBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.2F)
                    .randomTicks()
                    .noOcclusion()
                    .sound(SoundType.GRASS)));

    /**
     * 1.10.9 cold-star vegetation.  The deadhead vines cling to deadhead wood and carry five
     * textures keyed by position; the snow grass converts the grass block under it into
     * {@link #SNOW_COVERED_GRASS} while it is alive.
     */
    public static final DeferredBlock<DeadheadGrassShortBlock> DEADHEAD_GRASS_SHORT = BLOCKS.register(
            "deadhead_grass_short", key -> new DeadheadGrassShortBlock(
                    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                            .mapColor(MapColor.COLOR_BROWN)
                            .replaceable()
                            .noCollision()
                            .instabreak()
                            .noOcclusion()
                            .sound(SoundType.GRASS)));
    public static final DeferredBlock<DeadheadGrassTallBlock> DEADHEAD_GRASS_TALL = BLOCKS.register(
            "deadhead_grass_tall", key -> new DeadheadGrassTallBlock(
                    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                            .mapColor(MapColor.COLOR_BROWN)
                            .replaceable()
                            .noCollision()
                            .instabreak()
                            .noOcclusion()
                            .sound(SoundType.GRASS)));
    public static final DeferredBlock<SnowShortGrassBlock> SNOW_SHORT_GRASS = BLOCKS.register(
            "snow_short_grass", key -> new SnowShortGrassBlock(
                    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                            .mapColor(MapColor.SNOW)
                            .replaceable()
                            .noCollision()
                            .randomTicks()
                            .strength(0.1F)
                            .noOcclusion()
                            .sound(SoundType.GRASS)));
    public static final DeferredBlock<SnowTallGrassBlock> SNOW_TALL_GRASS = BLOCKS.register(
            "snow_tall_grass", key -> new SnowTallGrassBlock(
                    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                            .mapColor(MapColor.SNOW)
                            .replaceable()
                            .noCollision()
                            .randomTicks()
                            .strength(0.1F)
                            .noOcclusion()
                            .sound(SoundType.GRASS)));
    /** Placed by {@link #SNOW_SHORT_GRASS} / {@link #SNOW_TALL_GRASS}; deliberately has no BlockItem. */
    public static final DeferredBlock<SnowCoveredGrassBlock> SNOW_COVERED_GRASS = BLOCKS.register(
            "snow_covered_grass", key -> new SnowCoveredGrassBlock(
                    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                            .mapColor(MapColor.SNOW)
                            .randomTicks()
                            .strength(0.6F)
                            .sound(SoundType.GRASS)));

    /**
     * The 1.10.8 jar shipped a number of legacy block ids which are referenced
     * by world saves and structure templates but were not represented by a
     * dedicated modern class.  Keep those ids available using the closest
     * vanilla/infested state shape so old worlds load without missing blocks.
     */

    private static final BooleanProperty END = BooleanProperty.create("end");
    private static final BooleanProperty NODE = BooleanProperty.create("node");
    private static final BooleanProperty LIT = BooleanProperty.create("lit");
    private static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 1);
    private static final IntegerProperty ACTIVE = IntegerProperty.create("active", 0, 3);
    private static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

    private enum GoreVariant implements net.minecraft.util.StringRepresentable {
        BIG("big"),
        FLAT("flat"),
        SMALL("small");

        private final String name;

        GoreVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final EnumProperty<GoreVariant> GORE_VARIANT =
            EnumProperty.create("variant", GoreVariant.class);

    private enum OreVariant implements net.minecraft.util.StringRepresentable {
        CO("co"),
        DIA("dia"),
        EME("eme"),
        GOL("gol"),
        IRO("iro"),
        LAP("lap"),
        RED("red"),
        UN("un");

        private final String name;

        OreVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final EnumProperty<OreVariant> ORE_VARIANT =
            EnumProperty.create("variant", OreVariant.class);

    private enum CanisterVariant implements net.minecraft.util.StringRepresentable {
        BAG("bag"),
        CYST("cyst"),
        LUMP("lump"),
        SAC("sac");

        private final String name;

        CanisterVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final EnumProperty<CanisterVariant> CANISTER_VARIANT =
            EnumProperty.create("variant", CanisterVariant.class);

    private enum PlankVariant implements net.minecraft.util.StringRepresentable {
        DEADHEAD("deadhead"),
        DEADHEADS("deadheads");

        private final String name;

        PlankVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final EnumProperty<PlankVariant> PLANK_VARIANT =
            EnumProperty.create("variant", PlankVariant.class);

    private enum RubbleVariant implements net.minecraft.util.StringRepresentable {
        BONE("bone"),
        BRICKS("bricks"),
        FLESH("flesh"),
        FUNGUS("fungus"),
        METAL("metal"),
        OBSIDIAN("obsidian"),
        STONE("stone"),
        STONEDEBRIS("stonedebris"),
        WEATHB("weathb"),
        WEATHBC("weathbc"),
        WEATHBCS("weathbcs"),
        WEATHBS("weathbs"),
        WEATHFS("weathfs"),
        WEATHFSS("weathfss"),
        WOOD("wood");

        private final String name;

        RubbleVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final EnumProperty<RubbleVariant> RUBBLE_VARIANT =
            EnumProperty.create("variant", RubbleVariant.class);

    private enum StainVariant implements net.minecraft.util.StringRepresentable {
        DIRT("dirt"),
        FEELER("feeler"),
        FLESH("flesh"),
        MUD("mud"),
        RED("red"),
        SACKFLESH("sackflesh"),
        SPORE("spore");

        private final String name;

        StainVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final EnumProperty<StainVariant> STAIN_VARIANT =
            EnumProperty.create("variant", StainVariant.class);

    private enum BushVariant implements net.minecraft.util.StringRepresentable {
        ARC("arc"),
        FLOWER1("flower1"),
        GRASS1("grass1"),
        INFECTED("infected"),
        SPINE("spine"),
        VINE("vine");

        private final String name;

        BushVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final EnumProperty<BushVariant> BUSH_VARIANT =
            EnumProperty.create("variant", BushVariant.class);

    private enum ParasiteBushVariant implements net.minecraft.util.StringRepresentable {
        BINE("bine"),
        DECANTER("decanter"),
        DECANTEREMPTY("decanterempty"),
        EYE("eye"),
        FROSTG("frostg"),
        FROSTGT("frostgt"),
        POP("pop"),
        TENDRIL("tendril"),
        THORN("thorn"),
        THORNDEAD("thorndead"),
        THORNDORMAT("thorndormat"),
        THORNDORMATS("thorndormats"),
        THORNTWO("thorntwo"),
        THORNTWOS("thorntwos"),
        TOOH("tooh");

        private final String name;

        ParasiteBushVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final EnumProperty<ParasiteBushVariant> PARASITE_BUSH_VARIANT =
            EnumProperty.create("variant", ParasiteBushVariant.class);

    private enum SaplingVariant implements net.minecraft.util.StringRepresentable {
        CONSUMED("consumed"),
        DEADHEAD("deadhead"),
        FLOWERTALL("flowertall"),
        INFESTED("infested"),
        TREE("tree"),
        TREETHIN("treethin");

        private final String name;

        SaplingVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final EnumProperty<SaplingVariant> SAPLING_VARIANT =
            EnumProperty.create("variant", SaplingVariant.class);

    private enum RubbleSlabVariant implements net.minecraft.util.StringRepresentable {
        BONE("bone"),
        BRICKS("bricks"),
        FLESH("flesh"),
        FUNGUS("fungus"),
        METAL("metal"),
        OBSIDIAN("obsidian"),
        STONE("stone"),
        STONEDEBRIS("stonedebris"),
        WOOD("wood");

        private final String name;

        RubbleSlabVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final EnumProperty<RubbleSlabVariant> RUBBLE_SLAB_VARIANT =
            EnumProperty.create("variant", RubbleSlabVariant.class);

    private enum StainSlabVariant implements net.minecraft.util.StringRepresentable {
        DIRT("dirt"),
        FEELER("feeler"),
        MUD("mud"),
        RED("red"),
        SACKFLESH("sackflesh"),
        SFLESH("sflesh"),
        SPORE("spore");

        private final String name;

        StainSlabVariant(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static final EnumProperty<StainSlabVariant> STAIN_SLAB_VARIANT =
            EnumProperty.create("variant", StainSlabVariant.class);

    private static final java.util.Map<String, Property<?>[]> LEGACY_STATE_PROPERTIES =
            legacyStateProperties();

    private static java.util.Map<String, Property<?>[]> legacyStateProperties() {
        java.util.Map<String, Property<?>[]> properties = new java.util.LinkedHashMap<>();
        properties.put("goreada", new Property<?>[] {GORE_VARIANT});
        properties.put("gorefer", new Property<?>[] {GORE_VARIANT});
        properties.put("goremar", new Property<?>[] {GORE_VARIANT});
        properties.put("gorepri", new Property<?>[] {GORE_VARIANT});
        properties.put("gorepur", new Property<?>[] {GORE_VARIANT});
        properties.put("goresim", new Property<?>[] {GORE_VARIANT});
        properties.put("infestedore", new Property<?>[] {ORE_VARIANT});
        properties.put("parasitecanister", new Property<?>[] {CANISTER_VARIANT});
        properties.put("parasiteplank", new Property<?>[] {PLANK_VARIANT});
        properties.put("parasiterubble", new Property<?>[] {RUBBLE_VARIANT});
        properties.put("parasitestain", new Property<?>[] {STAIN_VARIANT});
        properties.put("infestedbush", new Property<?>[] {END, NODE, BUSH_VARIANT});
        properties.put("parasitebush", new Property<?>[] {END, NODE, PARASITE_BUSH_VARIANT});
        properties.put("parasitesapling", new Property<?>[] {STAGE, SAPLING_VARIANT});
        properties.put("colonyoutpost", new Property<?>[] {ACTIVE});
        properties.put("dermoid_cyst", new Property<?>[] {FACING});
        properties.put("relaycontroller", new Property<?>[] {FACING});
        properties.put("infested_furnace", new Property<?>[] {FACING, LIT});
        properties.put("infested_furnace_lit", new Property<?>[] {FACING, LIT});
        return java.util.Map.copyOf(properties);
    }

    /** Multi-variant legacy slab ids keep their original variant metadata. */
    private static final java.util.Map<String, EnumProperty<?>> LEGACY_SLAB_VARIANTS =
            legacySlabVariants();

    private static java.util.Map<String, EnumProperty<?>> legacySlabVariants() {
        java.util.Map<String, EnumProperty<?>> variants = new java.util.LinkedHashMap<>();
        variants.put("parasiterubbleslabhalf", RUBBLE_SLAB_VARIANT);
        variants.put("parasiterubbleslabdouble", RUBBLE_SLAB_VARIANT);
        variants.put("parasitestainslabhalf", STAIN_SLAB_VARIANT);
        variants.put("parasitestainslabdouble", STAIN_SLAB_VARIANT);
        return java.util.Map.copyOf(variants);
    }

    /**
     * Builds one legacy compatibility id from the {@code scapeandrunparasites} class the 1.10.9 jar
     * used for it.  Ids without an entry fall back to the shared variant families below.
     */
    @FunctionalInterface
    private interface LegacyFactory {
        Block create(String id, Identifier key);
    }

    /** Properties shared by the gore ids — 1.12.2 {@code BlockGore}, hardness 0.4F, FLESH sound. */
    private static BlockBehaviour.Properties goreProperties(Identifier key) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED)
                .strength(0.4F)
                .sound(FLESH_SOUND_TYPE)
                .noCollision()
                .noOcclusion();
    }

    /** {@code BlockBloodyIce}: ice material, 0.7F hardness, 0.98 friction, GLASS sound. */
    private static BlockBehaviour.Properties bloodyIceProperties(Identifier key) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED)
                .strength(0.7F)
                .friction(0.98F)
                .sound(SoundType.GLASS)
                .noOcclusion();
    }

    /** {@code BlockSRPFlower}: {@code BlockBush} defaults, FLESH sound, no collision. */
    private static BlockBehaviour.Properties srpFlowerProperties(Identifier key) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED)
                .instabreak()
                .sound(FLESH_SOUND_TYPE)
                .noCollision()
                .noOcclusion();
    }

    private static java.util.Map<String, LegacyFactory> legacyDedicatedBlocks() {
        java.util.Map<String, LegacyFactory> dedicated = new java.util.LinkedHashMap<>();
        for (String id : new String[] {"goreada", "gorefer", "goremar", "gorepri", "gorepur", "goresim"}) {
            dedicated.put(id, (name, key) -> new GoreBlock(goreProperties(key)));
        }
        dedicated.put("assimilated_blossom",
                (name, key) -> new AssimilatedBlossomBlock(srpFlowerProperties(key)));
        dedicated.put("bloodyice", (name, key) -> new BloodyIceBlock(bloodyIceProperties(key)));

        // --- SRP bush family (BlockInfestedBush / BlockParasiteBush) -----------------------------
        // Hardness and sound come straight from the 1.12.2 constructors:
        //   infestedbush : BlockInfestedBush("infestedbush", 0.4F)  + SoundType.GRASS
        //   parasitebush : BlockParasiteBush("parasitebush", 0.5F)  + SoundType.GRASS
        dedicated.put("infestedbush", (name, key) -> new InfestedBushBlock(legacyPlantProperties(key, 0.4F)));
        dedicated.put("parasitebush", (name, key) -> new ParasiteBushBlock(legacyPlantProperties(key, 0.5F)));

        // --- SRP ground / material family --------------------------------------------------------
        dedicated.put("parasitestain", (name, key) -> new ParasiteStainBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(0.8F).sound(FLESH_SOUND_TYPE)));
        dedicated.put("parasiterubble", (name, key) -> new ParasiteRubbleBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(2.3F, 10.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.STONE)));
        dedicated.put("parasiteplank", (name, key) -> new ParasitePlankBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(2.2F, 5.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.WOOD)));
        dedicated.put("parasitecanister", (name, key) -> new ParasiteCanisterBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(0.7F).sound(FLESH_SOUND_TYPE)));
        dedicated.put("infestedore", (name, key) -> new InfestedOreBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(3.5F)
                        .requiresCorrectToolForDrops().sound(INFESTED_ORE_SOUND_TYPE)));
        dedicated.put("harlequinn_grass", (name, key) -> new HarlequinnGrassBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(0.6F).sound(SoundType.STONE)));

        // --- flora ------------------------------------------------------------------------------
        dedicated.put("infested_leaves", (name, key) -> new InfestedLeavesBlock(leavesProperties(key), false));
        dedicated.put("infested_leaves_fast", (name, key) -> new InfestedLeavesBlock(leavesProperties(key), true));
        dedicated.put("infested_cactus", (name, key) -> new InfestedCactusBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(0.4F, 0.4F).sound(SoundType.WOOL)));
        dedicated.put("hirsute_hair", (name, key) -> new HirsuteHairBlock(legacyPlantProperties(key, 0.0F)));
        dedicated.put("tresses_hair", (name, key) -> new TressesHairBlock(legacyPlantProperties(key, 0.0F)));
        dedicated.put("lipoma_mass", (name, key) -> new LipomaMassBlock(legacyPlantProperties(key, 0.0F)));
        dedicated.put("parasitetendril", (name, key) -> new ParasiteTendrilBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(0.5F).sound(SoundType.VINE)
                        .noCollision().noOcclusion()));
        dedicated.put("parasitesapling", (name, key) -> new ParasiteSaplingBlock(legacyPlantProperties(key, 0.0F)));
        dedicated.put("infestedremain", (name, key) -> new InfestedRemainBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(0.4F).friction(0.52F)
                        .sound(SoundType.HONEY_BLOCK).noCollision().noOcclusion()));

        // --- machines / structures ---------------------------------------------------------------
        dedicated.put("parasitefog", (name, key) -> new ParasiteFogBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.NONE).strength(0.2F).sound(SoundType.GRASS)
                        .noCollision().noOcclusion().replaceable()));
        dedicated.put("parasite_barrier", (name, key) -> new ParasiteBarrierBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.NONE).strength(-1.0F, 6_000_000.0F)
                        .sound(SoundType.STONE).noLootTable()));
        dedicated.put("dermoid_cyst", (name, key) -> new DermoidCystBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(2.5F).sound(FLESH_SOUND_TYPE)));
        dedicated.put("dispatchern", (name, key) -> new DispatcherNBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(0.1F, 0.1F).sound(FLESH_SOUND_TYPE)));
        dedicated.put("epitome_infestation_warp_diffuser", (name, key) -> new EpitomeDiffuserBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(15.0F, 120.0F)
                        .lightLevel(state -> EpitomeDiffuserBlock.LIGHT_LEVEL)
                        .sound(FLESH_SOUND_TYPE)));
        dedicated.put("colonyoutpost", (name, key) -> new ColonyOutpostBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(30.0F, 1_200.0F)
                        .sound(FLESH_SOUND_TYPE)));
        dedicated.put("infested_furnace", (name, key) -> new InfestedFurnaceBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(3.5F)
                        .lightLevel(state -> state.getValue(InfestedFurnaceBlock.LIT)
                                ? InfestedFurnaceBlock.LIT_LIGHT_LEVEL : 0)
                        .sound(SoundType.STONE)));
        dedicated.put("infested_furnace_lit", (name, key) -> new InfestedFurnaceBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(3.5F)
                        .lightLevel(state -> state.getValue(InfestedFurnaceBlock.LIT)
                                ? InfestedFurnaceBlock.LIT_LIGHT_LEVEL : 0)
                        .sound(SoundType.STONE)));
        dedicated.put("relaycontroller", (name, key) -> new LegacyRelayBlock(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0F, 6.0F)
                        .sound(SoundType.METAL), true));
        dedicated.put("relay_controller_dummy", (name, key) -> new LegacyRelayBlock.Dummy(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0F, 6.0F)
                        .sound(SoundType.METAL)));
        dedicated.put("noderelay", (name, key) -> new LegacyRelayBlock.Node(
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3.0F, 6.0F)
                        .sound(SoundType.METAL)));

        // --- shape families ---------------------------------------------------------------------
        // All 40 legacy slab ids (half and double) ship a vanilla `type` blockstate, so one
        // SlabBlock-derived class covers them (BlockSlabBase/BlockSlabRubble/BlockSlabStain/
        // BlockHarleskinnSlab -> LegacySlabBlock).
        for (String id : LEGACY_SLAB_IDS) {
            final EnumProperty<?> variant = LEGACY_SLAB_VARIANTS.get(id);
            dedicated.put(id, (name, key) -> {
                BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                        .setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(slabHardness(name), 6.0F)
                        .sound(SoundType.ROOTED_DIRT);
                if (variant == null) {
                    return new LegacySlabBlock(props);
                }
                Object defaultValue = variant == RUBBLE_SLAB_VARIANT
                        ? legacySlabVariant(name, "bone", variant)
                        : legacySlabVariant(name, "dirt", variant);
                return new LegacyVariantSlabBlock(props, variant, defaultValue);
            });
        }
        // All 12 legacy stair ids ship the vanilla facing/half/shape blockstate.
        for (String id : LEGACY_STAIR_IDS) {
            dedicated.put(id, (name, key) -> new LegacyStairBlock(Blocks.STONE.defaultBlockState(),
                    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                            .mapColor(MapColor.COLOR_RED).strength(1.5F, 10.0F)
                            .sound(SoundType.ROOTED_DIRT)));
        }
        // The six legacy wall ids ship a vanilla multipart wall blockstate.
        for (String id : LEGACY_WALL_IDS) {
            dedicated.put(id, (name, key) -> new LegacyWallBlock(
                    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                            .mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F)
                            .sound(SoundType.ROOTED_DIRT)));
        }
        // The two legacy fence ids ship a vanilla multipart fence blockstate.
        for (String id : LEGACY_FENCE_IDS) {
            dedicated.put(id, (name, key) -> new LegacyFenceBlock(
                    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                            .mapColor(MapColor.COLOR_RED).strength(2.0F, 3.0F)
                            .sound(FLESH_SOUND_TYPE)));
        }
        // The four pot ids (BlockPottedSRPFlower).
        for (String id : LEGACY_POT_IDS) {
            dedicated.put(id, (name, key) -> new PottedSrpBlock(
                    BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                            .mapColor(MapColor.COLOR_RED).instabreak().sound(SoundType.STONE)
                            .noOcclusion()));
        }
        return java.util.Map.copyOf(dedicated);
    }

    /** The 40 legacy ids whose blockstate is a vanilla slab ({@code type=bottom/top/double}). */
    private static final String[] LEGACY_SLAB_IDS = {
            "bruisewood_plank_slab", "bruisewood_plank_slab_double", "consumed_plank_slab",
            "consumed_plank_slab_double", "cooked_flesh_slab_double", "dead_head_plank_slab",
            "dead_head_plank_slab_double", "flesh_slab", "flesh_slab_double",
            "frost_weathered_stone_slab", "frost_weathered_stone_slab_double", "goth_plank_slab",
            "goth_plank_slab_double", "harleskinn_slab", "harleskinn_slab_double",
            "infested_cobblestone_slab_double", "infested_dirt_slab_double",
            "infested_plank_slab_double", "infested_sandstone_slab_double",
            "infested_stone_brick_slab_double", "infested_stone_slab_double",
            "infested_terracotta_slab_double", "locs_block_slab", "locs_block_slab_double",
            "parasiterubbleslabdouble", "parasiterubbleslabhalf", "parasitestainslabdouble",
            "parasitestainslabhalf", "parasitic_colony_core_slab", "parasitic_colony_core_slab_double",
            "parasitic_compressed_colony_stone_slab", "parasitic_compressed_colony_stone_slab_double",
            "poland_skin_slab", "poland_skin_slab_double", "polished_infested_stone_slab_double",
            "reinforced_hivestone_slab", "reinforced_hivestone_slab_double",
            "residue_brick_slab_double", "sac_of_flesh_slab", "sac_of_flesh_slab_double",
            "weathered_bricks_slab", "weathered_bricks_slab_double", "weathered_cobblestone_slab",
            "weathered_cobblestone_slab_double"
    };

    /** The 12 legacy ids whose blockstate is a vanilla stair ({@code facing/half/shape}). */
    private static final String[] LEGACY_STAIR_IDS = {
            "bruisewood_plank_stairs", "consumed_planks_stairs", "deadhead_plank_stairs",
            "flesh_stairs", "frost_weathered_stone_stairs", "goth_planks_stairs", "harleskinn_stairs",
            "infestedrubblestairs", "infestedstainstairs", "infestedtrunkstairs",
            "parasitestain_dirtstairs", "parasitestain_feelerstairs", "parasitestain_fleshstairs",
            "parasitestain_mudstairs", "wheathered_bricks_stairs", "wheathered_cobblestone_stairs"
    };

    /** The six legacy ids whose blockstate is a vanilla multipart wall. */
    private static final String[] LEGACY_WALL_IDS = {
            "bruisewood_plank_wall", "consumed_plank_wall", "goth_plank_wall",
            "parasitecanister_bag_wall", "parasiteplank_deadhead_wall", "parasitestain_flesh_wall"
    };

    /** The two legacy ids whose blockstate is a vanilla multipart fence. */
    private static final String[] LEGACY_FENCE_IDS = {
            "bruisewood_fence", "harleskinn_fence"
    };

    /** The four legacy flower-pot ids (BlockPottedSRPFlower). */
    private static final String[] LEGACY_POT_IDS = {
            "consumed_pot", "infested_pot", "potted_assimilated_blossom",
            "potted_consumed_assimilated_blossom"
    };

    /** Per-id hardness of the original slab constructors (2.3F rubble, 0.8F stain, 2.0F wood). */
    private static float slabHardness(String id) {
        if (id.startsWith("parasiterubble")) {
            return 2.3F;
        }
        if (id.startsWith("parasitestain")) {
            return 0.8F;
        }
        return 2.0F;
    }

    /** Resolves one constant of a legacy slab variant enum by its serialized name. */
    private static Object legacySlabVariant(String id, String serializedName, EnumProperty<?> property) {
        for (Object value : property.getPossibleValues()) {
            if (value instanceof net.minecraft.util.StringRepresentable named
                    && named.getSerializedName().equals(serializedName)) {
                return value;
            }
        }
        throw new IllegalStateException("No " + serializedName + " variant for legacy slab " + id);
    }

    /** Shared properties for the two SRP bush ids. */
    private static BlockBehaviour.Properties legacyPlantProperties(Identifier key, float hardness) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED)
                .strength(hardness)
                .sound(SoundType.GRASS)
                .noCollision()
                .noOcclusion();
    }

    /** {@code BlockLeafLike}: hardness 0.2F, {@code SoundType.GRASS}, no light, cutout render. */
    private static BlockBehaviour.Properties leavesProperties(Identifier key) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED)
                .strength(0.2F)
                .sound(SoundType.GRASS)
                .noOcclusion();
    }

    private static final java.util.Map<String, LegacyFactory> LEGACY_DEDICATED_BLOCKS =
            legacyDedicatedBlocks();

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
            LegacyFactory dedicated = LEGACY_DEDICATED_BLOCKS.get(id);
            if (dedicated != null) {
                holder = BLOCKS.register(id, key -> dedicated.create(id, key));
            } else if (id.endsWith("_stairs") || id.endsWith("stairs")) {
                holder = BLOCKS.register(id, key -> new InfestedStairBlock(Blocks.STONE.defaultBlockState(),
                        BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
                                .strength(1.5F, 10.0F).sound(SoundType.ROOTED_DIRT)));
            } else if (id.endsWith("_slab") || id.endsWith("_slab_double")
                    || id.endsWith("slabhalf") || id.endsWith("slabdouble")) {
                final EnumProperty<?> variant = LEGACY_SLAB_VARIANTS.get(id);
                holder = variant == null
                        ? BLOCKS.register(id, key -> new InfestedSlabBlock(legacyProperties(key)))
                        : BLOCKS.register(id, key -> new InfestedSlabBlock(legacyProperties(key)) {
                            @Override
                            protected void createBlockStateDefinition(
                                    StateDefinition.Builder<Block, BlockState> builder) {
                                super.createBlockStateDefinition(builder);
                                builder.add(variant);
                            }
                        });
            } else if (id.endsWith("_wall") || id.endsWith("wall")) {
                holder = BLOCKS.register(id, key -> new InfestedWallBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F).sound(SoundType.ROOTED_DIRT)));
            } else if (id.endsWith("_fence") || id.endsWith("fence")) {
                holder = BLOCKS.register(id, key -> new InfestedFenceBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.WOOD)));
            } else if (id.equals("infested_cactus")) {
                holder = BLOCKS.register(id, key -> new CactusBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).randomTicks().strength(0.4F)
                        .sound(SoundType.WOOL)));
            } else if (id.equals("parasitetendril")) {
                holder = BLOCKS.register(id, key -> new VineBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).noCollision().randomTicks()
                        .strength(0.2F).sound(SoundType.VINE)));
            } else if (id.equals("tresses_hair")) {
                holder = BLOCKS.register(id, key -> new DoublePlantBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).noCollision().instabreak().sound(SoundType.GRASS)));
            } else if (id.equals("parasitefog")) {
                holder = BLOCKS.register(id, key -> new FogBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).strength(0.2F).randomTicks()
                        .noOcclusion().sound(SoundType.GRASS)));
            } else if (LEGACY_STATE_PROPERTIES.containsKey(id)) {
                holder = BLOCKS.register(id, key -> legacyStateBlock(id, key));
            } else {
                holder = BLOCKS.register(id, key -> new Block(legacyProperties(key)));
            }
            result.put(id, holder);
        }
        return java.util.Map.copyOf(result);
    }

    /**
     * Compatibility ids are registered in a batch ({@link #LEGACY_BLOCKS}) rather than as
     * individual fields; this exposes one so it can still receive a matching {@code BlockItem}.
     */
    public static DeferredBlock<? extends Block> legacyBlock(String id) {
        DeferredBlock<? extends Block> block = LEGACY_BLOCKS.get(id);
        if (block == null) {
            throw new IllegalArgumentException("Not a legacy block id: " + id);
        }
        return block;
    }

    /** Base properties shared by the compatibility ids. */
    private static BlockBehaviour.Properties legacyProperties(Identifier key) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED)
                .strength(1.5F, 6.0F).sound(SoundType.ROOTED_DIRT);
    }

    /**
     * The 1.10.8 jar drove a handful of decorative blocks from block metadata
     * ({@code variant}, {@code end}, {@code node}, ...).  Old worlds and the
     * ported structure templates still store those values, so those ids are
     * rebuilt with matching modern properties instead of silently dropping the
     * extra state.
     */
    private static Block legacyStateBlock(String id, Identifier key) {
        final Property<?>[] stateProperties = LEGACY_STATE_PROPERTIES.get(id);
        return new Block(legacyProperties(key)) {
            @Override
            protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                for (Property<?> property : stateProperties) {
                    builder.add(property);
                }
            }
        };
    }


    private static DeferredBlock<InfestedGlassBlock> tintedGlass(String id) {
        return BLOCKS.register(id, key -> new InfestedGlassBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED)
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .noOcclusion()));
    }

    private static DeferredBlock<IronBarsBlock> glassPane(String id) {
        return BLOCKS.register(id, key -> new IronBarsBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED)
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .noOcclusion()));
    }

    private static DeferredBlock<ButtonBlock> woodButton(String id) {
        return BLOCKS.register(id, key -> new ButtonBlock(
                BlockSetType.OAK, 30, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).noCollision().strength(0.5F).sound(SoundType.WOOD)));
    }

    private static DeferredBlock<PressurePlateBlock> woodPressurePlate(String id) {
        return BLOCKS.register(id, key -> new PressurePlateBlock(
                BlockSetType.OAK, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                        .mapColor(MapColor.COLOR_RED).noCollision().strength(0.5F).sound(SoundType.WOOD)));
    }

    private static DeferredBlock<LadderBlock> woodLadder(String id) {
        return BLOCKS.register(id, key -> new LadderBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED).noCollision().strength(0.4F).sound(SoundType.LADDER)));
    }

    private static DeferredBlock<Block> woodBookshelf(String id) {
        return BLOCKS.register(id, key -> new Block(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED).strength(1.5F).sound(SoundType.WOOD)));
    }

    private static DeferredBlock<InfestedBlock> parasiticPlanks(String id, MapColor color) {
        return BLOCKS.register(id, key -> new InfestedBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(color).strength(2.0F, 5.0F)
                .requiresCorrectToolForDrops().sound(TUNNEL_SOUND_TYPE)));
    }

    private static java.util.Map<String, DeferredBlock<EscaBulbBlock>> registerEscaBulbs() {
        java.util.Map<String, DeferredBlock<EscaBulbBlock>> bulbs = new java.util.LinkedHashMap<>();
        String[] colors = {"", "white", "light_gray", "gray", "black", "brown", "red", "orange",
                "yellow", "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"};
        for (String color : colors) {
            String id = color.isEmpty() ? "esca_bulb" : "esca_bulb_" + color;
            bulbs.put(color.isEmpty() ? "base" : color, BLOCKS.register(id, key ->
                    new EscaBulbBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.0F)
                            .noOcclusion()
                            .lightLevel(state -> 15)
                            .sound(FLESH_LIGHT_SOUND_TYPE))));
        }
        return java.util.Map.copyOf(bulbs);
    }

    private static DeferredBlock<InfestedBlock> infested(String id, MapColor color, SoundType sound) {
        return BLOCKS.register(id, key -> new InfestedBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(color).strength(1.5F, 6.0F).sound(sound)));
    }

    private static DeferredBlock<ParasiteLootBlock> parasiteLoot(String id, ParasiteLootBlock.Tier tier) {
        return BLOCKS.register(id, key -> new ParasiteLootBlock(tier, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED).strength(2.0F, 8.0F).sound(SoundType.SCULK)));
    }

    private static DeferredBlock<InfestedBlock> infested(
            String id, float hardness, float resistance, MapColor color, SoundType sound) {
        return BLOCKS.register(id, key -> new InfestedBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(color).strength(hardness, resistance).requiresCorrectToolForDrops().sound(sound)));
    }

    private static DeferredBlock<SlabBlock> slab(String id, float hardness, float resistance, SoundType sound) {
        return BLOCKS.register(id, key -> new SlabBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key))
                .mapColor(MapColor.COLOR_RED).strength(hardness, resistance)
                .requiresCorrectToolForDrops().sound(sound)));
    }

    private static DeferredBlock<InfestedStairBlock> infestedStairs(
            String id, DeferredBlock<? extends Block> baseBlock) {
        return BLOCKS.register(id, key -> new InfestedStairBlock(baseBlock.get().defaultBlockState(),
                BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.COLOR_RED).strength(1.5F, 10.0F)
                        .sound(SoundType.ROOTED_DIRT)));
    }

    private static DeferredBlock<InfestedWallBlock> infestedWall(
            String id, DeferredBlock<? extends Block> baseBlock) {
        return BLOCKS.register(id, key -> new InfestedWallBlock(
                BlockBehaviour.Properties.ofFullCopy(baseBlock.get()).setId(ResourceKey.create(Registries.BLOCK, key))));
    }

    private ModBlocks() {
    }
}
