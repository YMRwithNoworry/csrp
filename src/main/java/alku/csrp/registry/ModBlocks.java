package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.block.InfestedBlock;
import alku.csrp.block.InfestedStairBlock;
import alku.csrp.block.InfestedWallBlock;
import alku.csrp.block.NodeLampBlock;
import alku.csrp.block.BiomeHeartBlock;
import alku.csrp.block.ColonyHeartBlock;
import alku.csrp.block.ColonyStructureBlock;
import alku.csrp.block.DeadBloodBlock;
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
import alku.csrp.block.BladderSacBlock;
import alku.csrp.block.GrotesqueLumpBlock;
import alku.csrp.block.InfuserFurnaceBlock;
import alku.csrp.block.SickAlveoliBlock;
import alku.csrp.block.SrpWebBlock;
import alku.csrp.block.ThornshadeBlock;
import alku.csrp.block.TrophyBlock;
import alku.csrp.block.TunnelBlock;
import alku.csrp.block.VacuousCystBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.util.DeferredSoundType;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Csrp.MODID);
    private static final SoundType TUNNEL_SOUND_TYPE = new DeferredSoundType(1.5F, 1.0F,
            () -> ModSounds.get("block.tunnel.dig"),
            () -> ModSounds.get("block.flesh.step"),
            () -> ModSounds.get("block.flesh.place"),
            () -> ModSounds.get("block.flesh.hit"),
            () -> ModSounds.get("block.flesh.fall"));

    public static final DeferredBlock<TunnelBlock> TUNNEL = BLOCKS.register("tunnel", () -> new TunnelBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .noOcclusion()
                    .strength(0.1F, 0.1F)
                    .sound(TUNNEL_SOUND_TYPE)));

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
    public static final DeferredBlock<Block> HIVESTONE_DEBRIS = BLOCKS.register("parasiterubble_stonedebris", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(2.3F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
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
                    .strength(1.0F).noOcclusion().sound(SoundType.SLIME_BLOCK)));
    public static final DeferredBlock<SickAlveoliBlock> SICK_ALVEOLI = BLOCKS.register("sick_alveoli", () ->
            new SickAlveoliBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0F).noOcclusion().sound(SoundType.SLIME_BLOCK)));
    public static final DeferredBlock<AlveoliGrowthBlock> ALVEOLI_GROWTH = BLOCKS.register("alveoli_growth", () ->
            new AlveoliGrowthBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .instabreak().noCollission().noOcclusion().sound(SoundType.GRASS)));
    public static final DeferredBlock<Block> SOLID_ALVEOLI_BLOCK = BLOCKS.register("solid_alveoli_block", () ->
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.0F).noOcclusion().sound(SoundType.SLIME_BLOCK)));
    public static final DeferredBlock<RotatedPillarBlock> HAIR_FOLLICLE_BLOCK = BLOCKS.register(
            "hair_follicle_block", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.ROOTED_DIRT)));
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
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_ORE = infestedOre("infested_ore");
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_COAL_ORE = infestedOre("infested_coal_ore");
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_DIAMOND_ORE = infestedOre("infested_diamond_ore");
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_EMERALD_ORE = infestedOre("infested_emerald_ore");
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_GOLD_ORE = infestedOre("infested_gold_ore");
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_IRON_ORE = infestedOre("infested_iron_ore");
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_LAPIS_ORE = infestedOre("infested_lapis_ore");
    public static final DeferredBlock<PestilentialOreBlock> INFESTED_REDSTONE_ORE = infestedOre("infested_redstone_ore");

    private static DeferredBlock<PestilentialOreBlock> infestedOre(String id) {
        return BLOCKS.register(id, () -> new PestilentialOreBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)));
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
            "trophy_void_orb", () -> new TrophyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.0F)
                    .sound(SoundType.SCULK)));
    public static final DeferredBlock<TrophyBlock> DRACONITE_TROPHY = BLOCKS.register(
            "trophy_boom_orb", () -> new TrophyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .sound(SoundType.SCULK)));

    public static final java.util.Map<String, DeferredBlock<EscaBulbBlock>> ESCA_BULBS = registerEscaBulbs();

    public static final DeferredBlock<FogBlock> FOG = BLOCKS.register("fog", () ->
            new FogBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .noCollission()
                    .noOcclusion()
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()));
    public static final DeferredBlock<FogNullifierBlock> FOG_NULLIFIER = BLOCKS.register(
            "fog_nullifier", () -> new FogNullifierBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(5.0F, 20.0F)
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

    private static java.util.Map<String, DeferredBlock<EscaBulbBlock>> registerEscaBulbs() {
        java.util.Map<String, DeferredBlock<EscaBulbBlock>> bulbs = new java.util.LinkedHashMap<>();
        String[] colors = {"", "white", "light_gray", "gray", "black", "brown", "red", "orange",
                "yellow", "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"};
        for (String color : colors) {
            String id = color.isEmpty() ? "esca_bulb" : "esca_bulb_" + color;
            bulbs.put(color.isEmpty() ? "base" : color, BLOCKS.register(id, () ->
                    new EscaBulbBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.3F)
                            .lightLevel(state -> 14)
                            .sound(SoundType.GLASS))));
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
