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
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.util.ForgeSoundType;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Csrp.MODID);
    private static final SoundType FLESH_SOUND_TYPE = new ForgeSoundType(1.5F, 1.0F,
            () -> ModSounds.get("block.flesh.dig"),
            () -> ModSounds.get("block.flesh.step"),
            () -> ModSounds.get("block.flesh.place"),
            () -> ModSounds.get("block.flesh.hit"),
            () -> ModSounds.get("block.flesh.fall"));
    private static final SoundType TUNNEL_SOUND_TYPE = new ForgeSoundType(1.5F, 1.0F,
            () -> ModSounds.get("block.tunnel.dig"),
            () -> ModSounds.get("block.flesh.step"),
            () -> ModSounds.get("block.flesh.place"),
            () -> ModSounds.get("block.flesh.hit"),
            () -> ModSounds.get("block.flesh.fall"));
    private static final SoundType INFESTED_ORE_SOUND_TYPE = new ForgeSoundType(1.0F, 0.5F,
            () -> ModSounds.get("blockinfest.break"),
            () -> ModSounds.get("blockinfest.step"),
            () -> ModSounds.get("blockinfest.place"),
            () -> ModSounds.get("blockinfest.hit"),
            () -> SoundEvents.STONE_FALL);
    private static final SoundType FLESH_LIGHT_SOUND_TYPE = new ForgeSoundType(1.5F, 1.0F,
            () -> ModSounds.get("block.flesh_light.dig"),
            () -> ModSounds.get("block.flesh_light.step"),
            () -> ModSounds.get("block.flesh_light.place"),
            () -> ModSounds.get("block.flesh_light.hit"),
            () -> ModSounds.get("block.flesh_light.fall"));

    public static final RegistryObject<TunnelBlock> TUNNEL = BLOCKS.register("tunnel", () -> new TunnelBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.1F, 0.1F)
                    .sound(TUNNEL_SOUND_TYPE)));

    public static final RegistryObject<ResidueBloomingBlock> RESIDUE_PLANTS = BLOCKS.register("residue_plants", () -> new ResidueBloomingBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .noOcclusion()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.GRASS)));
    public static final RegistryObject<ThornshadeBlock> THORNSHADE = BLOCKS.register("thornshade", () ->
            new ThornshadeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .sound(SoundType.SWEET_BERRY_BUSH)));
    public static final RegistryObject<ResidueBlock> RESIDUE_BLOCK = BLOCKS.register("residue_block", () -> new ResidueBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F)
                    .sound(SoundType.ROOTED_DIRT)));
    public static final RegistryObject<InfestedResidueBlock> INFESTED_REMAINS = BLOCKS.register("infestremain", () ->
            new InfestedResidueBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .noCollission().noOcclusion().randomTicks().instabreak().sound(SoundType.ROOTED_DIRT)));
    public static final RegistryObject<ParasiteTrapBlock> BIOMASS_BLOCK = BLOCKS.register("biomass_block", () ->
            new ParasiteTrapBlock(ParasiteTrapBlock.Kind.BIOMASS, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN).strength(0.6F).friction(0.8F)
                    .lightLevel(state -> 6).sound(SoundType.SLIME_BLOCK)));
    public static final RegistryObject<ParasiteTrapBlock> PARASITE_MOUTH = BLOCKS.register("parasitemouth", () ->
            new ParasiteTrapBlock(ParasiteTrapBlock.Kind.MAW, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.4F).noOcclusion().sound(SoundType.ROOTED_DIRT)));
    public static final RegistryObject<Block> HIVESTONE_DEBRIS = BLOCKS.register("parasiterubble_stonedebris", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.3F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistryObject<ParasiteLootBlock> PARASITE_LOOT_COMMON = parasiteLoot(
            "parasiteloot", ParasiteLootBlock.Tier.COMMON);
    public static final RegistryObject<ParasiteLootBlock> PARASITE_LOOT_UNCOMMON = parasiteLoot(
            "parasiteloot_uncommon", ParasiteLootBlock.Tier.UNCOMMON);
    public static final RegistryObject<ParasiteLootBlock> PARASITE_LOOT_RARE = parasiteLoot(
            "parasiteloot_rare", ParasiteLootBlock.Tier.RARE);

    public static final RegistryObject<InfestedBlock> INFESTED_STAIN = infested("infestedstain", MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final RegistryObject<InfestedBlock> INFESTED_RUBBLE = infested("infestedrubble", MapColor.COLOR_RED, SoundType.STONE);
    public static final RegistryObject<InfestedBlock> INFESTED_SAND = infested("infestedsand", MapColor.COLOR_RED, SoundType.SAND);
    public static final RegistryObject<InfestedBlock> INFESTED_COBBLESTONE = infested("infested_cobblestone", MapColor.COLOR_RED, SoundType.STONE);
    public static final RegistryObject<InfestedBlock> INFESTED_TRUNK = infested("infestedtrunk", MapColor.COLOR_RED, SoundType.WOOD);
    public static final RegistryObject<InfestedBlock> INFESTED_PLANKS = infested("infested_planks", MapColor.COLOR_RED, SoundType.WOOD);
    public static final RegistryObject<ButtonBlock> INFESTED_BUTTON = woodButton("infested_button");
    public static final RegistryObject<PressurePlateBlock> INFESTED_PRESSURE_PLATE = woodPressurePlate(
            "infested_pressure_plate");
    public static final RegistryObject<LadderBlock> INFESTED_LADDER = woodLadder("infested_ladder");
    public static final RegistryObject<Block> INFESTED_BOOKSHELF = woodBookshelf("infested_bookshelf");
    public static final RegistryObject<InfestedBlock> INFESTED_STONE_BRICKS = infested(
            "infested_stone_bricks", 1.5F, 10.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final RegistryObject<InfestedBlock> INFESTED_TERRACOTTA = infested(
            "infested_terracotta", 1.25F, 4.2F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final RegistryObject<InfestedBlock> POLISHED_INFESTED_STONE = infested(
            "infested_stone_polished", 1.5F, 10.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final RegistryObject<InfestedBlock> RESIDUE_BRICKS = infested(
            "residue_bricks", 1.5F, 10.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final RegistryObject<RotatedPillarBlock> INFESTED_COLUMN = BLOCKS.register(
            "infested_column", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.5F, 10.0F).sound(SoundType.STONE)));
    public static final RegistryObject<InfestedBlock> INFESTED_SANDSTONE = infested(
            "inf_ss", 0.8F, 4.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final RegistryObject<InfestedBlock> CHISELED_INFESTED_SANDSTONE = infested(
            "inf_ss_chiseled", 0.8F, 4.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);
    public static final RegistryObject<InfestedBlock> CUT_INFESTED_SANDSTONE = infested(
            "inf_ss_cut", 0.8F, 4.0F, MapColor.COLOR_RED, SoundType.ROOTED_DIRT);

    public static final RegistryObject<SlabBlock> INFESTED_COBBLESTONE_SLAB = slab(
            "infested_cobblestone_slab", 2.0F, 3.0F, SoundType.STONE);
    public static final RegistryObject<SlabBlock> INFESTED_STONE_SLAB = slab(
            "infested_stone_slab", 2.0F, 3.0F, SoundType.STONE);
    public static final RegistryObject<SlabBlock> INFESTED_DIRT_SLAB = slab(
            "infested_dirt_slab", 0.5F, 0.5F, SoundType.ROOTED_DIRT);
    public static final RegistryObject<SlabBlock> INFESTED_STONE_BRICK_SLAB = slab(
            "infested_stone_brick_slab", 2.0F, 6.0F, SoundType.STONE);
    public static final RegistryObject<SlabBlock> INFESTED_TERRACOTTA_SLAB = slab(
            "infested_terracotta_slab", 1.25F, 4.2F, SoundType.STONE);
    public static final RegistryObject<SlabBlock> POLISHED_INFESTED_STONE_SLAB = slab(
            "polished_infested_stone_slab", 2.0F, 6.0F, SoundType.STONE);
    public static final RegistryObject<SlabBlock> RESIDUE_BRICK_SLAB = slab(
            "residue_brick_slab", 2.0F, 6.0F, SoundType.STONE);
    public static final RegistryObject<SlabBlock> INFESTED_SANDSTONE_SLAB = slab(
            "infested_sandstone_slab", 0.8F, 4.0F, SoundType.STONE);
    public static final RegistryObject<SlabBlock> INFESTED_PLANK_SLAB = slab(
            "infested_plank_slab", 2.0F, 3.0F, SoundType.WOOD);

    public static final RegistryObject<InfestedStairBlock> INFESTED_SANDSTONE_STAIRS = infestedStairs(
            "infested_sandstone_stairs", INFESTED_SANDSTONE);
    public static final RegistryObject<InfestedStairBlock> RESIDUE_STAIRS = infestedStairs(
            "residue_stairs", RESIDUE_BRICKS);
    public static final RegistryObject<InfestedStairBlock> INFESTED_PLANKS_STAIRS = infestedStairs(
            "infested_planks_stairs", INFESTED_PLANKS);
    public static final RegistryObject<InfestedStairBlock> INFESTED_STONE_BRICKS_STAIRS = infestedStairs(
            "infested_stone_bricks_stairs", INFESTED_STONE_BRICKS);
    public static final RegistryObject<InfestedStairBlock> INFESTED_POLISHED_STONE_BRICKS_STAIRS = infestedStairs(
            "infested_polished_stone_bricks_stairs", POLISHED_INFESTED_STONE);
    public static final RegistryObject<InfestedStairBlock> INFESTED_STONE_STAIRS = infestedStairs(
            "infested_stone_stairs", INFESTED_RUBBLE);

    public static final RegistryObject<InfestedWallBlock> RESIDUE_WALL = infestedWall(
            "residue_wall", INFESTED_PLANKS);
    public static final RegistryObject<InfestedWallBlock> INFESTED_PLANK_WALL = infestedWall(
            "infested_plank_wall", INFESTED_PLANKS);
    public static final RegistryObject<InfestedWallBlock> POLISHED_INFESTED_STONE_WALL = infestedWall(
            "polished_infested_stone_wall", INFESTED_RUBBLE);
    public static final RegistryObject<InfestedWallBlock> INFESTED_STONE_BRICK_WALL = infestedWall(
            "infested_stone_brick_wall", INFESTED_RUBBLE);
    public static final RegistryObject<InfestedWallBlock> INFESTED_SANDSTONE_WALL = infestedWall(
            "infested_sandstone_wall", INFESTED_RUBBLE);
    public static final RegistryObject<InfestedWallBlock> INFESTED_RUBBLE_WALL = infestedWall(
            "infestedrubble_wall", INFESTED_RUBBLE);
    public static final RegistryObject<InfestedWallBlock> INFESTED_STAIN_WALL = infestedWall(
            "infestedstain_wall", INFESTED_STAIN);

    public static final RegistryObject<BiomeHeartBlock> BIOMEHEART = BLOCKS.register("biomeheart", () ->
            new BiomeHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(4.0F, 20.0F).sound(SoundType.SCULK).lightLevel(state -> 5)));
    public static final RegistryObject<ColonyHeartBlock> COLONYHEART = BLOCKS.register("colonyheart", () ->
            new ColonyHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(4.0F, 20.0F).sound(SoundType.SCULK).lightLevel(state -> 3)));
    public static final RegistryObject<ColonyStructureBlock> PARASITE_STRUCTURE = BLOCKS.register("parasitestructure", () ->
            new ColonyStructureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.5F, 6.0F).sound(SoundType.SCULK)));
    public static final RegistryObject<Block> SEMIORGANIC_BLOCK = BLOCKS.register("semiorganic_block", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistryObject<NodeLampBlock> NODE_REDSTONE_LAMP = BLOCKS.register("node_redstone_lamp", () ->
            new NodeLampBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.35F).lightLevel(state -> state.getValue(NodeLampBlock.POWERED) ? 12 : 0)
                    .sound(SoundType.GLASS)));
    public static final RegistryObject<RelayTerminalBlock> RELAY_BASE = BLOCKS.register("relay_base", () ->
            new RelayTerminalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistryObject<RelayTowerPartBlock> RELAY_MIDDLE = BLOCKS.register("relay_middle", () ->
            new RelayTowerPartBlock(1, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistryObject<RelayTowerPartBlock> RELAY_ROOF = BLOCKS.register("relay_roof", () ->
            new RelayTowerPartBlock(2, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final RegistryObject<InfestationPurifierBlock> INFESTATION_PURIFIER = BLOCKS.register(
            "infestation_purifier", () -> new InfestationPurifierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY).strength(5.0F).sound(SoundType.STONE)));
    public static final RegistryObject<EvolutionLureBlock> EVOLUTION_LURE = BLOCKS.register(
            "evolutionlure", () -> new EvolutionLureBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistryObject<AlveoliBlock> ALVEOLI = BLOCKS.register("alveoli", () ->
            new AlveoliBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0F).noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final RegistryObject<SickAlveoliBlock> SICK_ALVEOLI = BLOCKS.register("sick_alveoli", () ->
            new SickAlveoliBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0F).noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final RegistryObject<AlveoliGrowthBlock> ALVEOLI_GROWTH = BLOCKS.register("alveoli_growth", () ->
            new AlveoliGrowthBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .instabreak().noCollission().noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final RegistryObject<Block> SOLID_ALVEOLI_BLOCK = BLOCKS.register("solid_alveoli_block", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0F).noOcclusion().sound(FLESH_SOUND_TYPE)));
    public static final RegistryObject<RotatedPillarBlock> HAIR_FOLLICLE_BLOCK = BLOCKS.register(
            "hair_follicle_block", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.STONE)));
    public static final RegistryObject<SrpWebBlock> SRP_WEB = BLOCKS.register("srpweb", () ->
            new SrpWebBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .noCollission()
                    .instabreak()
                    .noOcclusion()
                    .randomTicks()
                    .sound(SoundType.WOOL)
                    .noLootTable()));
    public static final RegistryObject<DispatcherNidusBlock> DISPATCHER_NIDUS = BLOCKS.register(
            "dispatcher_nidus", () -> new DispatcherNidusBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 12.0F)
                    .sound(SoundType.SCULK)
                    .noLootTable()));
    public static final RegistryObject<PestilentialOreBlock> INFESTED_ORE = infestedOre("infested_ore", PestilentialOreBlock.OreKind.TWISTED);
    public static final RegistryObject<PestilentialOreBlock> INFESTED_COAL_ORE = infestedOre("infested_coal_ore", PestilentialOreBlock.OreKind.COAL);
    public static final RegistryObject<PestilentialOreBlock> INFESTED_DIAMOND_ORE = infestedOre("infested_diamond_ore", PestilentialOreBlock.OreKind.DIAMOND);
    public static final RegistryObject<PestilentialOreBlock> INFESTED_EMERALD_ORE = infestedOre("infested_emerald_ore", PestilentialOreBlock.OreKind.EMERALD);
    public static final RegistryObject<PestilentialOreBlock> INFESTED_GOLD_ORE = infestedOre("infested_gold_ore", PestilentialOreBlock.OreKind.GOLD);
    public static final RegistryObject<PestilentialOreBlock> INFESTED_IRON_ORE = infestedOre("infested_iron_ore", PestilentialOreBlock.OreKind.IRON);
    public static final RegistryObject<PestilentialOreBlock> INFESTED_LAPIS_ORE = infestedOre("infested_lapis_ore", PestilentialOreBlock.OreKind.LAPIS);
    public static final RegistryObject<PestilentialOreBlock> INFESTED_REDSTONE_ORE = infestedOre("infested_redstone_ore", PestilentialOreBlock.OreKind.REDSTONE);

    private static RegistryObject<PestilentialOreBlock> infestedOre(String id, PestilentialOreBlock.OreKind kind) {
        return BLOCKS.register(id, () -> new PestilentialOreBlock(kind, BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(INFESTED_ORE_SOUND_TYPE)));
    }
    public static final RegistryObject<GluttonousCystBlock> GLUTTONOUS_CYST = BLOCKS.register(
            "gluttonous_cyst", () -> new GluttonousCystBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(20.0F, 2000.0F)
                    .sound(SoundType.SLIME_BLOCK)
                    .noLootTable()));
    public static final RegistryObject<VacuousCystBlock> VACUOUS_CYST = BLOCKS.register(
            "vacuous_cyst", () -> new VacuousCystBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F)
                    .randomTicks()
                    .sound(SoundType.SLIME_BLOCK)
                    .noLootTable()));
    public static final RegistryObject<AssimilatedPumpkinBlock> ASSIMILATED_PUMPKIN = BLOCKS.register(
            "assimilated_pumpkin", () -> new AssimilatedPumpkinBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .sound(SoundType.WOOD)));
    public static final RegistryObject<AssimilatedJackOLanternBlock> ASSIMILATED_JACK_O_LANTERN = BLOCKS.register(
            "assimilated_jack_o_lantern", () -> new AssimilatedJackOLanternBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .lightLevel(state -> 15)
                    .sound(SoundType.WOOD)));
    public static final RegistryObject<AssimilatedReedBlock> ASSIMILATED_REED = BLOCKS.register(
            "assimilated_reed", () -> new AssimilatedReedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .instabreak()
                    .noCollission()
                    .noOcclusion()
                    .sound(SoundType.GRASS)));
    public static final RegistryObject<BladderSacBlock> BLADDER_SAC = BLOCKS.register(
            "bladder_sac", () -> new BladderSacBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.8F)
                    .sound(SoundType.SLIME_BLOCK)));
    public static final RegistryObject<GrotesqueLumpBlock> GROTESQUE_LUMP = BLOCKS.register(
            "grotesque_lump", () -> new GrotesqueLumpBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.8F)
                    .sound(SoundType.SLIME_BLOCK)));
    public static final RegistryObject<TrophyBlock> KIRIN_TROPHY = BLOCKS.register(
            "trophy_void_orb", () -> new TrophyBlock(TrophyBlock.Kind.VOID, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0F)
                    .sound(SoundType.SCULK)));
    public static final RegistryObject<TrophyBlock> DRACONITE_TROPHY = BLOCKS.register(
            "trophy_boom_orb", () -> new TrophyBlock(TrophyBlock.Kind.BOOM, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .sound(SoundType.SCULK)));

    public static final java.util.Map<String, RegistryObject<EscaBulbBlock>> ESCA_BULBS = registerEscaBulbs();

    public static final RegistryObject<FogBlock> FOG = BLOCKS.register("fog", () ->
            new FogBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .noCollission()
                    .noOcclusion()
                    .replaceable()
                    .forceSolidOff()
                    .randomTicks()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()));
    public static final RegistryObject<FogNullifierBlock> FOG_NULLIFIER = BLOCKS.register(
            "fog_nullifier", () -> new FogNullifierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.0F, 10.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final RegistryObject<DeadBloodBlock> DEAD_BLOOD = BLOCKS.register(
            "deadblood", () -> new DeadBloodBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .strength(100.0F)
                    .noLootTable()));
    public static final RegistryObject<Block> VISCERAL_MUD = BLOCKS.register("visceral_mud", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.5F)
                    .sound(SoundType.MUD)));
    public static final RegistryObject<Block> BLEEDING_OBSIDIAN = BLOCKS.register("bleeding_obsidian", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
    public static final RegistryObject<DiseasedSpongeBlock> DISEASED_SPONGE = BLOCKS.register(
            "diseased_sponge", () -> new DiseasedSpongeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.6F)
                    .sound(SoundType.STONE)));
    public static final RegistryObject<InfuserFurnaceBlock> INFUSER_FURNACE = BLOCKS.register(
            "infuser_furnace", () -> new InfuserFurnaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.5F, 10.0F)
                    .lightLevel(state -> 13)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));
    public static final RegistryObject<BiomePurifierBlock> BIOME_PURIFIER = BLOCKS.register(
            "biomepurifier", () -> new BiomePurifierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(5.0F, 20.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
    public static final RegistryObject<Block> HARLESKINN_BLOCK = BLOCKS.register("harleskinn_block", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)));
    public static final RegistryObject<Block> POLAND_SKIN_BLOCK = BLOCKS.register("poland_skin_block", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.SLIME_BLOCK)));
    public static final RegistryObject<Block> LOCS_BLOCK = BLOCKS.register("locs_block", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)));
    public static final RegistryObject<InfestedGlassBlock> INFESTED_GLASS = BLOCKS.register(
            "infested_glass", () -> new InfestedGlassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));
    public static final RegistryObject<InfestedGlassBlock> BLOODY_GLASS = tintedGlass("bloody_glass");
    public static final RegistryObject<InfestedGlassBlock> ASHEN_GLASS = tintedGlass("ashen_glass");
    public static final RegistryObject<InfestedGlassBlock> SEPIA_GLASS = tintedGlass("sepia_glass");
    public static final RegistryObject<InfestedGlassBlock> HARLEQUINN_GLASS = tintedGlass("harlequinn_glass");
    public static final RegistryObject<InfestedGlassBlock> SHROUDED_GLASS = tintedGlass("shrouded_glass");
    public static final RegistryObject<InfestedGlassBlock> MOODY_GLASS = tintedGlass("moody_glass");
    public static final RegistryObject<InfestedGlassBlock> SHADE_GLASS = tintedGlass("shade_glass");
    public static final RegistryObject<IronBarsBlock> INFESTED_GLASS_PANE = glassPane("infested_glass_pane");
    public static final RegistryObject<IronBarsBlock> BLOODY_GLASS_PANE = glassPane("bloody_glass_pane");
    public static final RegistryObject<IronBarsBlock> ASHEN_GLASS_PANE = glassPane("ashen_glass_pane");
    public static final RegistryObject<IronBarsBlock> SEPIA_GLASS_PANE = glassPane("sepia_glass_pane");
    public static final RegistryObject<IronBarsBlock> HARLEQUINN_GLASS_PANE = glassPane("harlequinn_glass_pane");
    public static final RegistryObject<IronBarsBlock> SHROUDED_GLASS_PANE = glassPane("shrouded_glass_pane");
    public static final RegistryObject<IronBarsBlock> MOODY_GLASS_PANE = glassPane("moody_glass_pane");
    public static final RegistryObject<IronBarsBlock> SHADE_GLASS_PANE = glassPane("shade_glass_pane");
    public static final RegistryObject<Block> GOTHSHROOM = BLOCKS.register("gothshroom", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instabreak()
                    .noCollission()
                    .noOcclusion()
                    .sound(SoundType.FUNGUS)));
    public static final RegistryObject<InfestedBlock> COOKED_FLESH = BLOCKS.register("cooked_flesh", () ->
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 5.0F).requiresCorrectToolForDrops().sound(TUNNEL_SOUND_TYPE)));
    public static final RegistryObject<InfestedBlock> COOKED_FLESH_PLANKS = parasiticPlanks(
            "cooked_flesh_planks", MapColor.COLOR_RED);
    public static final RegistryObject<ButtonBlock> COOKED_FLESH_BUTTON = woodButton("cooked_flesh_button");
    public static final RegistryObject<PressurePlateBlock> COOKED_FLESH_PRESSURE_PLATE = woodPressurePlate(
            "cooked_flesh_pressure_plate");
    public static final RegistryObject<LadderBlock> COOKED_FLESH_LADDER = woodLadder("cooked_flesh_ladder");
    public static final RegistryObject<Block> COOKED_FLESH_BOOKSHELF = woodBookshelf(
            "cooked_flesh_bookshelf");
    public static final RegistryObject<InfestedBlock> FLESH_PLANKS = parasiticPlanks(
            "flesh_planks", MapColor.COLOR_RED);
    public static final RegistryObject<ButtonBlock> FLESH_BUTTON = woodButton("flesh_button");
    public static final RegistryObject<PressurePlateBlock> FLESH_PRESSURE_PLATE = woodPressurePlate(
            "flesh_pressure_plate");
    public static final RegistryObject<LadderBlock> FLESH_LADDER = woodLadder("flesh_ladder");
    public static final RegistryObject<Block> FLESH_BOOKSHELF = woodBookshelf("flesh_bookshelf");
    public static final RegistryObject<InfestedBlock> GOTH_PLANKS = parasiticPlanks(
            "goth_planks", MapColor.COLOR_PURPLE);
    public static final RegistryObject<ButtonBlock> GOTH_BUTTON = woodButton("goth_button");
    public static final RegistryObject<PressurePlateBlock> GOTH_PRESSURE_PLATE = woodPressurePlate(
            "goth_pressure_plate");
    public static final RegistryObject<LadderBlock> GOTH_LADDER = woodLadder("goth_ladder");
    public static final RegistryObject<Block> GOTH_BOOKSHELF = woodBookshelf("goth_bookshelf");
    public static final RegistryObject<InfestedBlock> BRUSEWOOD_PLANKS = parasiticPlanks(
            "brusewood_planks", MapColor.COLOR_PURPLE);
    public static final RegistryObject<ButtonBlock> BRUCEWOOD_BUTTON = woodButton("brucewood_button");
    public static final RegistryObject<PressurePlateBlock> BRUSEWOOD_PRESSURE_PLATE = woodPressurePlate(
            "brusewood_pressure_plate");
    public static final RegistryObject<LadderBlock> BRUISEWOOD_LADDER = woodLadder("bruisewood_ladder");
    public static final RegistryObject<Block> BRUISEWOOD_BOOKSHELF = woodBookshelf("bruisewood_bookshelf");
    public static final RegistryObject<InfestedBlock> CONSUMED_PLANKS = parasiticPlanks(
            "consumed_planks", MapColor.COLOR_GRAY);
    public static final RegistryObject<ButtonBlock> CONSUMED_BUTTON = woodButton("consumed_button");
    public static final RegistryObject<PressurePlateBlock> CONSUMED_PRESSURE_PLATE = woodPressurePlate(
            "consumed_pressure_plate");
    public static final RegistryObject<LadderBlock> CONSUMED_LADDER = woodLadder("consumed_ladder");
    public static final RegistryObject<Block> CONSUMED_BOOKSHELF = woodBookshelf("consumed_bookshelf");
    public static final RegistryObject<InfestedBlock> DEADHEAD_PLANKS = parasiticPlanks(
            "parasiteplank_deadhead", MapColor.COLOR_BROWN);
    public static final RegistryObject<ButtonBlock> DEADHEAD_BUTTON = woodButton("deadhead_button");
    public static final RegistryObject<PressurePlateBlock> DEADHEAD_PRESSURE_PLATE = woodPressurePlate(
            "deadhead_pressure_plate");
    public static final RegistryObject<LadderBlock> DEADHEAD_LADDER = woodLadder("deadhead_ladder");
    public static final RegistryObject<Block> DEADHEAD_BOOKSHELF = woodBookshelf("deadhead_bookshelf");
    public static final RegistryObject<InfestedStairBlock> COOKED_FLESH_STAIRS = BLOCKS.register(
            "cooked_flesh_stairs", () -> new InfestedStairBlock(COOKED_FLESH_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                            .strength(1.5F, 10.0F).sound(TUNNEL_SOUND_TYPE)));
    public static final RegistryObject<InfestedSlabBlock> COOKED_FLESH_SLAB = BLOCKS.register(
            "cooked_flesh_slab", () -> new InfestedSlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(TUNNEL_SOUND_TYPE)));
    public static final RegistryObject<InfestedFenceBlock> COOKED_FLESH_FENCE = BLOCKS.register(
            "cooked_flesh_fence", () -> new InfestedFenceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.5F, 3.0F).sound(TUNNEL_SOUND_TYPE)));
    public static final RegistryObject<DeadheadLeavesBlock> DEADHEAD_LEAVES = BLOCKS.register(
            "deadhead_leaves", () -> new DeadheadLeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.2F)
                    .randomTicks()
                    .noOcclusion()
                    .sound(SoundType.GRASS)));

    /**
     * Names retained by the 1.12 release which do not have a dedicated modern
     * implementation yet. Keeping them registered is important for old
     * structures, recipes and saved block states. The closest vanilla shape
     * is selected from the id so these blocks remain usable in-game.
     */
    public static final java.util.Map<String, RegistryObject<Block>> LEGACY_BLOCKS = registerLegacyBlocks();

    private static java.util.Map<String, RegistryObject<Block>> registerLegacyBlocks() {
        String[] ids = {
                "assimilated_blossom", "bloodyice", "canisteractive", "colonyoutpost", "dispatchern",
                "epitome_infestation_warp_diffuser", "goreada", "gorefer", "goremar", "gorepri", "gorepur", "goresim",
                "bruisewood_fence", "bruisewood_plank_slab",
                "bruisewood_plank_slab_double", "bruisewood_plank_stairs", "bruisewood_plank_wall",
                "brusewood_door", "brusewood_trapdoor", "consumed_door", "consumed_fence",
                "consumed_plank_slab", "consumed_plank_slab_double", "consumed_plank_wall",
                "consumed_planks_stairs", "consumed_pot", "consumed_trapdoor", "consumed_workbench",
                "cooked_flesh_slab_double", "dead_head_plank_slab", "dead_head_plank_slab_double",
                "deadhead_fence", "deadhead_plank_stairs", "dermoid_cyst", "flesh_fence", "flesh_slab",
                "flesh_slab_double", "flesh_stairs", "frost_weathered_stone_slab",
                "frost_weathered_stone_slab_double", "frost_weathered_stone_stairs", "goth_door",
                "goth_fence", "goth_plank_slab", "goth_plank_slab_double", "goth_plank_wall",
                "goth_planks_stairs", "goth_stem", "harlequinn_grass", "harleskinn_fence",
                "harleskinn_slab", "harleskinn_slab_double", "harleskinn_stairs", "hirsute_hair",
                "infested_cactus", "infested_cobblestone_slab_double", "infested_dirt_slab_double",
                "infested_fence", "infested_furnace", "infested_furnace_lit", "infested_leaves", "infested_leaves_fast", "infested_plank_slab_double",
                "infested_pot", "infested_sandstone_slab_double", "infested_stone_brick_slab_double",
                "infested_stone_slab_double", "infested_terracotta_slab_double", "infested_workbench",
                "lipoma_mass", "locs_block_slab", "locs_block_slab_double", "node_relay", "parasitebush",
                "parasitecanister", "parasitecanister_bag_wall", "parasiteplank", "parasiteplank_deadhead_wall",
                "parasiterubble", "parasiterubble_bone", "parasiterubble_bricks", "parasiterubble_flesh",
                "parasiterubble_fungus", "parasiterubble_metal", "parasiterubble_obsidian", "parasiterubble_stone",
                "parasiterubble_wood", "parasitestain_dirt", "parasitestain_flesh", "parasitic_colony_core_slab",
                "parasiterubble_bricks_wall", "parasiterubble_flesh_wall", "parasiterubble_metal_wall",
                "parasiterubble_weathb_wall", "parasiterubble_weathbc_wall", "parasiterubble_weathfs_wall",
                "parasiterubbledense", "parasiterubbledense_biome_wall", "parasiterubbledense_colony_wall",
                "parasitestain",
                "parasitic_colony_core_slab_double", "parasitic_compressed_colony_stone_slab",
                "parasitic_compressed_colony_stone_slab_double", "parasitesapling",
                "parasitestain_flesh_wall", "parasitethin", "parasitetrunk", "poland_skin_slab", "poland_skin_slab_double",
                "polished_infested_stone_slab_double", "potted_assimilated_blossom",
                "potted_consumed_assimilated_blossom", "relay_controller_dummy", "relaycontroller",
                "reinforced_hivestone_slab", "reinforced_hivestone_slab_double", "residue_brick_slab_double",
                "sac_of_flesh_slab", "sac_of_flesh_slab_double", "tresses_hair", "weathered_bricks_slab",
                "weathered_bricks_slab_double", "weathered_cobblestone_slab",
                "weathered_cobblestone_slab_double", "wheathered_bricks_stairs",
                "wheathered_cobblestone_stairs", "goth_planks_stairs", "consumed_workbench",
                "infested_sandstone_stairs", "infested_stone_stairs", "infested_stone_bricks_stairs",
                "infested_polished_stone_bricks_stairs", "frost_weathered_stone_stairs", "infested_workbench",
                "infested_cobblestone_slab", "infested_stone_slab", "infested_dirt_slab",
                "infested_stone_brick_slab", "infested_terracotta_slab", "polished_infested_stone_slab",
                "residue_brick_slab", "infested_sandstone_slab", "infested_plank_slab", "residue_stairs"
                , "infestedremain", "infestedrubblestairs", "infestedstainstairs", "infestedtrunkstairs",
                "parasite_barrier", "parasitefog", "parasiterubble_bonestairs", "parasiterubble_bricksstairs",
                "parasiterubble_fleshstairs", "parasiterubble_fungusstairs", "parasiterubble_metalstairs",
                "parasiterubble_obsidianstairs", "parasiterubble_stonedebrisstairs", "parasiterubble_stonestairs",
                "parasiterubble_woodstairs", "parasiterubbledense_biomestairs", "parasiterubbledense_colonystairs",
                "parasiterubbledense_wallstairs", "parasiterubbleslabdouble", "parasiterubbleslabhalf",
                "parasitestain_dirtstairs", "parasitestain_feelerstairs", "parasitestain_fleshstairs",
                "parasitestain_mudstairs", "parasitestainslabdouble", "parasitestainslabhalf", "parasitetendril",
                "parasitetrunk_ballstairs", "parasitetrunk_plantstairs", "parasitetrunk_treestairs"
        };
        java.util.Map<String, RegistryObject<Block>> result = new java.util.LinkedHashMap<>();
        for (String id : ids) {
            if (isAlreadyRegistered(id) || result.containsKey(id)) {
                continue;
            }
            result.put(id, BLOCKS.register(id, () -> legacyBlock(id)));
        }
        return java.util.Map.copyOf(result);
    }

    private static boolean isAlreadyRegistered(String id) {
        return switch (id) {
            case "residue_stairs", "infested_sandstone_stairs", "infested_stone_stairs",
                    "infested_stone_bricks_stairs", "infested_polished_stone_bricks_stairs",
                    "infested_cobblestone_slab", "infested_stone_slab", "infested_dirt_slab",
                    "infested_stone_brick_slab", "infested_terracotta_slab", "polished_infested_stone_slab",
                    "residue_brick_slab", "infested_sandstone_slab", "infested_plank_slab" -> true;
            default -> false;
        };
    }

    private static Block legacyBlock(String id) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).strength(1.5F, 6.0F).sound(SoundType.WOOD);
        if (id.endsWith("_door")) {
            return new DoorBlock(properties, BlockSetType.OAK);
        }
        if (id.endsWith("_trapdoor")) {
            return new TrapDoorBlock(properties, BlockSetType.OAK);
        }
        if (id.endsWith("_fence")) {
            return new FenceBlock(properties);
        }
        if (id.endsWith("_wall")) {
            return new WallBlock(properties);
        }
        if (id.contains("slab")) {
            return new SlabBlock(properties);
        }
        if (id.contains("stairs")) {
            return new StairBlock(Blocks.STONE.defaultBlockState(), properties);
        }
        if (id.contains("leaves")) {
            return new LeavesBlock(properties.randomTicks().noOcclusion());
        }
        return new Block(properties);
    }

    private static RegistryObject<InfestedGlassBlock> tintedGlass(String id) {
        return BLOCKS.register(id, () -> new InfestedGlassBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .noOcclusion()));
    }

    private static RegistryObject<IronBarsBlock> glassPane(String id) {
        return BLOCKS.register(id, () -> new IronBarsBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(0.3F)
                .sound(SoundType.GLASS)
                .noOcclusion()));
    }

    private static RegistryObject<ButtonBlock> woodButton(String id) {
        return BLOCKS.register(id, () -> new ButtonBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().strength(0.5F).sound(SoundType.WOOD),
                BlockSetType.OAK, 30, false));
    }

    private static RegistryObject<PressurePlateBlock> woodPressurePlate(String id) {
        return BLOCKS.register(id, () -> new PressurePlateBlock(
                PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED).noCollission().strength(0.5F).sound(SoundType.WOOD), BlockSetType.OAK));
    }

    private static RegistryObject<LadderBlock> woodLadder(String id) {
        return BLOCKS.register(id, () -> new LadderBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).noCollission().strength(0.4F).sound(SoundType.LADDER)));
    }

    private static RegistryObject<Block> woodBookshelf(String id) {
        return BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).strength(1.5F).sound(SoundType.WOOD)));
    }

    private static RegistryObject<InfestedBlock> parasiticPlanks(String id, MapColor color) {
        return BLOCKS.register(id, () -> new InfestedBlock(BlockBehaviour.Properties.of()
                .mapColor(color).strength(2.0F, 5.0F)
                .requiresCorrectToolForDrops().sound(TUNNEL_SOUND_TYPE)));
    }

    private static java.util.Map<String, RegistryObject<EscaBulbBlock>> registerEscaBulbs() {
        java.util.Map<String, RegistryObject<EscaBulbBlock>> bulbs = new java.util.LinkedHashMap<>();
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

    private static RegistryObject<InfestedBlock> infested(String id, MapColor color, SoundType sound) {
        return BLOCKS.register(id, () -> new InfestedBlock(BlockBehaviour.Properties.of()
                .mapColor(color).strength(1.5F, 6.0F).sound(sound)));
    }

    private static RegistryObject<ParasiteLootBlock> parasiteLoot(String id, ParasiteLootBlock.Tier tier) {
        return BLOCKS.register(id, () -> new ParasiteLootBlock(tier, BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).strength(2.0F, 8.0F).sound(SoundType.SCULK)));
    }

    private static RegistryObject<InfestedBlock> infested(
            String id, float hardness, float resistance, MapColor color, SoundType sound) {
        return BLOCKS.register(id, () -> new InfestedBlock(BlockBehaviour.Properties.of()
                .mapColor(color).strength(hardness, resistance).requiresCorrectToolForDrops().sound(sound)));
    }

    private static RegistryObject<SlabBlock> slab(String id, float hardness, float resistance, SoundType sound) {
        return BLOCKS.register(id, () -> new SlabBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED).strength(hardness, resistance)
                .requiresCorrectToolForDrops().sound(sound)));
    }

    private static RegistryObject<InfestedStairBlock> infestedStairs(
            String id, RegistryObject<? extends Block> baseBlock) {
        return BLOCKS.register(id, () -> new InfestedStairBlock(baseBlock.get().defaultBlockState(),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.5F, 10.0F)
                        .sound(SoundType.ROOTED_DIRT)));
    }

    private static RegistryObject<InfestedWallBlock> infestedWall(
            String id, RegistryObject<? extends Block> baseBlock) {
        return BLOCKS.register(id, () -> new InfestedWallBlock(
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.5F, 10.0F)));
    }

    private ModBlocks() {
    }
}
