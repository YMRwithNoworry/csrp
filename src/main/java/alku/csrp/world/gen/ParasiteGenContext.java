package alku.csrp.world.gen;

import alku.csrp.Csrp;
import alku.csrp.block.ParasiteLootBlock;
import alku.csrp.block.entity.ParasiteLootBlockEntity;
import alku.csrp.registry.ModBlocks;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Shared helpers for the ported 1.10.9 parasite world-gen features.
 *
 * <p>The originals extended {@code WorldGenerator}/{@code WorldGenAbstractTree} and wrote through
 * {@code World#setBlockState(pos, state, 2)} ("notify clients, skip neighbour updates"), which is
 * exactly what {@link ServerLevel#setBlock(BlockPos, BlockState, int)} with flag 2 does here.  Every
 * write is clamped to the level's build height and every read tolerates an unloaded neighbour so a
 * decoration pass can never throw inside chunk generation.</p>
 *
 * <p>The original block/enum ids are mapped to this project's registry by looking the legacy id up
 * in {@link ModBlocks#legacyBlock(String)} and then picking the {@code variant} enum constant by its
 * serialized name, so the mapping never depends on enum ordinals.</p>
 */
public final class ParasiteGenContext {
    /** Old block metadata of {@code srparasites:parasitestain} "dirt" (harlequin grass). */
    public static final BlockState STAIN_DIRT = stain("dirt");
    public static final BlockState STAIN_MUD = stain("mud");
    public static final BlockState STAIN_FLESH = stain("flesh");
    public static final BlockState STAIN_FEELER = stain("feeler");
    public static final BlockState STAIN_SPORE = stain("spore");
    public static final BlockState STAIN_RED = stain("red");
    /** Old metadata 5 of {@code srparasites:parasitestain} (the shrouded gravel replacement). */
    public static final BlockState STAIN_SACKFLESH = stain("sackflesh");

    /** {@code srparasites:harlequinn_grass} — legacy compatibility id, no modern field. */
    public static final BlockState HARLEQUINN_GRASS = legacyState("harlequinn_grass");
    public static final BlockState PARASITE_BUSH = ModBlocks.legacyBlock("parasitebush").get()
            .defaultBlockState();

    /** {@code BlockParasiteBush.EnumType.BINE} — the vine the colony walls hang. */
    public static final BlockState BUSH_BINE = variantState(ModBlocks.legacyBlock("parasitebush").get(), "bine");

    // ---------------------------------------------------------------------------------------------
    // Colony / meteor palette (the WorldGenParasiteColonyBase floor|tacle|wall|floorColony fields)
    // ---------------------------------------------------------------------------------------------

    /** {@code SRPBlocks.ParasiteRubble} variants. */
    public static final BlockState RUBBLE_BONE = rubble("bone");
    public static final BlockState RUBBLE_BRICKS = rubble("bricks");
    public static final BlockState RUBBLE_FLESH = rubble("flesh");
    public static final BlockState RUBBLE_FUNGUS = rubble("fungus");
    public static final BlockState RUBBLE_STONE = rubble("stone");

    /**
     * {@code SRPBlocks.ParasiteRubbleDense} with {@code VARIANT = WALL}.  In 1.10.9 the dense
     * rubble was one id with four metadata variants; this project registers the wall variant as the
     * canonical {@code csrp:parasiterubbledense} block and the other three as {@code _biome},
     * {@code _colony} and {@code _heart}, so the wall maps to the plain id.
     */
    public static final BlockState DENSE_WALL = ModBlocks.PARASITERUBBLEDENSE.get().defaultBlockState();

    /** {@code SRPBlocks.ParasiteFog} — the faint nexus-protection gas. */
    public static final BlockState FOG = ModBlocks.legacyBlock("parasitefog").get().defaultBlockState();

    public static final BlockState DEAD_BLOOD = ModBlocks.DEAD_BLOOD.get().defaultBlockState();
    public static final BlockState COOKED_FLESH = ModBlocks.COOKED_FLESH.get().defaultBlockState();

    /**
     * The 1.12.2 {@code Blocks.field_189880_di} the colony shells and DNA helices used as their
     * second state.  It is a full bone-coloured block placed inside the parasite shells; the port
     * maps it to {@link Blocks#BONE_BLOCK}, the 1.10 block that field name belongs to (the five
     * blocks added in 1.10 are magma, nether wart block, red nether brick, bone block and structure
     * void, in registration order).
     */
    public static final BlockState BONE_BLOCK = Blocks.BONE_BLOCK.defaultBlockState();

    public static final BlockState STONE = Blocks.STONE.defaultBlockState();
    public static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private static BlockState rubble(String variant) {
        return variantState(ModBlocks.legacyBlock("parasiterubble").get(), variant);
    }

    private static final Map<String, Boolean> VARIANT_BLOCKS = new ConcurrentHashMap<>();

    private ParasiteGenContext() {
    }

    // ---------------------------------------------------------------------------------------------
    // Block state lookup
    // ---------------------------------------------------------------------------------------------

    private static BlockState stain(String variant) {
        return variantState(ModBlocks.legacyBlock("parasitestain").get(), variant);
    }

    private static BlockState legacyState(String id) {
        return ModBlocks.legacyBlock(id).get().defaultBlockState();
    }

    /**
     * Builds a state of a legacy compatibility block, setting its {@code variant} enum property when
     * the id has one.  {@link ModBlocks} keeps the variant value classes private, so the constant is
     * resolved from the property's own serialized names instead of a hard-coded enum reference.
     */
    public static BlockState variantState(Block block, String variant) {
        return variantState(block.defaultBlockState(), variant);
    }

    /** Same as {@link #variantState(Block, String)} but keeps any properties the base state carries. */
    public static BlockState variantState(BlockState base, String variant) {
        BlockState state = base;
        Property<?> property = variantProperty(state);
        if (property instanceof EnumProperty<?> enums) {
            for (Object value : enums.getPossibleValues()) {
                String name = variantName(value);
                if (name != null && name.equalsIgnoreCase(variant)) {
                    state = setEnum(state, enums, value);
                    break;
                }
            }
        }
        return state;
    }

    /** 26.3 removed {@code BlockState#getProperty(String)}; the property list is walked instead. */
    private static Property<?> variantProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if ("variant".equals(property.getName())) {
                return property;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setEnum(BlockState state, EnumProperty<?> property, Object value) {
        return state.setValue((Property) property, (Comparable) value);
    }

    private static String variantName(Object value) {
        if (value instanceof net.minecraft.util.StringRepresentable named) {
            return named.getSerializedName();
        }
        return value instanceof Enum<?> constant ? constant.name().toLowerCase(java.util.Locale.ROOT) : null;
    }

    /** True when the block carries a {@code variant} enum property at all. */
    public static boolean hasVariant(Block block) {
        return VARIANT_BLOCKS.computeIfAbsent(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString(),
                key -> variantProperty(block.defaultBlockState()) instanceof EnumProperty<?>);
    }

    // ---------------------------------------------------------------------------------------------
    // Writes and reads
    // ---------------------------------------------------------------------------------------------

    /** {@code World#setBlockState(pos, state, 2)} — the flag every ported feature used. */
    public static void setBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (!inWorld(level, pos)) {
            return;
        }
        if (!level.isLoaded(pos)) {
            return;
        }
        level.setBlock(pos, state, 2);
    }

    /** {@code World#setBlockToAir(pos)}. */
    public static void setAir(ServerLevel level, BlockPos pos) {
        setBlock(level, pos, Blocks.AIR.defaultBlockState());
    }

    public static BlockState get(ServerLevel level, BlockPos pos) {
        if (!inWorld(level, pos) || !level.isLoaded(pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return level.getBlockState(pos);
    }

    public static boolean inWorld(ServerLevel level, BlockPos pos) {
        return pos.getY() >= level.getMinY() && pos.getY() < level.getMaxY();
    }

    public static boolean isAir(ServerLevel level, BlockPos pos) {
        return get(level, pos).isAir();
    }

    public static boolean isAirOrLeaves(ServerLevel level, BlockPos pos) {
        BlockState state = get(level, pos);
        return state.isAir() || state.getBlock() instanceof LeavesBlock;
    }

    // ---------------------------------------------------------------------------------------------
    // Parasite loot tumors (WorldGenParasiteColonyBase#placeLoot)
    // ---------------------------------------------------------------------------------------------

    /**
     * Port of {@code WorldGenParasiteColonyBase#placeLoot}: writes the tumor and rolls its contents.
     * The original read one of the three {@code SRPConfigWorld.blockLoot*} id lists and filled every
     * slot on a 1/2 roll; this project's loot block entity owns the same pools, so the tier selects
     * both the block and the roll.
     */
    public static void placeLoot(ServerLevel level, BlockPos pos, ParasiteLootBlock.Tier tier,
            RandomSource random) {
        Block block = switch (tier) {
            case COMMON -> ModBlocks.PARASITE_LOOT_COMMON.get();
            case UNCOMMON -> ModBlocks.PARASITE_LOOT_UNCOMMON.get();
            case RARE -> ModBlocks.PARASITE_LOOT_RARE.get();
        };
        if (!inWorld(level, pos) || !level.isLoaded(pos)) {
            return;
        }
        level.setBlock(pos, block.defaultBlockState(), 2);
        if (level.getBlockEntity(pos) instanceof ParasiteLootBlockEntity loot) {
            loot.generateLoot(tier, random);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Block categories that the originals tested through 1.12.2 materials / instanceof
    // ---------------------------------------------------------------------------------------------

    /** {@code Material#isSolid()} — the "solid ground" test of the tree and spine loops. */
    public static boolean isSolidGround(ServerLevel level, BlockPos pos) {
        BlockState state = get(level, pos);
        if (state.isAir()) {
            return false;
        }
        return !state.getFluidState().is(FluidTags.WATER)
                && !state.getFluidState().is(FluidTags.LAVA);
    }

    /** {@code Block#isFullBlock} — the tall/flower/spine "floor" test. */
    public static boolean isFullFloor(ServerLevel level, BlockPos pos) {
        BlockState state = get(level, pos);
        return !state.isAir() && state.isCollisionShapeFullBlock(level, pos);
    }

    /**
     * {@code IBlockState#func_185913_b()} — the {@code Block#isFullCube} probe the colony wall and
     * column loops used to decide whether they had to plug a hole below themselves.
     */
    public static boolean isFullCube(ServerLevel level, BlockPos pos) {
        return get(level, pos).isCollisionShapeFullBlock(level, pos);
    }

    /**
     * The 1.12.2 {@code instanceof BlockBase} test: every block this mod registers.  Used by the
     * colony helpers so a structure never overwrites its own material and never floors over it.
     */
    public static boolean isModBlock(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(Csrp.MODID);
    }

    public static boolean isLeaves(ServerLevel level, BlockPos pos) {
        return get(level, pos).getBlock() instanceof LeavesBlock;
    }

    /** {@code Block#isWood} / the legacy parasite trunk, used by the replaceability tests. */
    public static boolean isWoodLike(ServerLevel level, BlockPos pos) {
        Block block = get(level, pos).getBlock();
        return block == ModBlocks.PARASITETRUNK.get()
                || block == ModBlocks.PARASITETRUNK_BALL.get()
                || block == ModBlocks.PARASITETRUNK_PLANT.get()
                || block == ModBlocks.INFESTED_TRUNK.get()
                || block == ModBlocks.legacyBlock("parasiteplank").get()
                || block == Blocks.OAK_LOG || block == Blocks.OAK_WOOD
                || block == Blocks.SPRUCE_LOG || block == Blocks.SPRUCE_WOOD
                || block == Blocks.BIRCH_LOG || block == Blocks.BIRCH_WOOD
                || block == Blocks.JUNGLE_LOG || block == Blocks.JUNGLE_WOOD
                || block == Blocks.ACACIA_LOG || block == Blocks.ACACIA_WOOD
                || block == Blocks.DARK_OAK_LOG || block == Blocks.DARK_OAK_WOOD;
    }

    /** {@code Material#field_151585_k} (PLANT) — old plants turned into "flesh" stain. */
    public static boolean isPlant(ServerLevel level, BlockPos pos) {
        Block block = get(level, pos).getBlock();
        return block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS || block == Blocks.FERN
                || block == Blocks.LARGE_FERN || block == Blocks.DEAD_BUSH || block == Blocks.VINE
                || block == Blocks.SUNFLOWER || block == Blocks.LILAC || block == Blocks.ROSE_BUSH
                || block == Blocks.PEONY || block == Blocks.DANDELION || block == Blocks.POPPY
                || block == Blocks.BLUE_ORCHID || block == Blocks.ALLIUM || block == Blocks.AZURE_BLUET
                || block == Blocks.RED_TULIP || block == Blocks.ORANGE_TULIP
                || block == Blocks.WHITE_TULIP || block == Blocks.PINK_TULIP || block == Blocks.OXEYE_DAISY
                || block == Blocks.CORNFLOWER || block == Blocks.LILY_OF_THE_VALLEY
                || block == Blocks.TORCHFLOWER || block == Blocks.PITCHER_PLANT
                || block == Blocks.SWEET_BERRY_BUSH || block == ModBlocks.legacyBlock("parasitebush").get()
                || block == ModBlocks.legacyBlock("infestedbush").get();
    }

    /** {@code Material#field_151573_f} (IRON) — old iron blocks became metal rubble. */
    public static boolean isIron(ServerLevel level, BlockPos pos) {
        Block block = get(level, pos).getBlock();
        return block == Blocks.IRON_BLOCK || block == Blocks.IRON_BARS
                || block == Blocks.IRON_DOOR || block == Blocks.IRON_TRAPDOOR
                || block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL
                || block == Blocks.CAULDRON || block == Blocks.HOPPER
                || block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE;
    }

    /** {@code Material#field_151575_d} (WOOD) — old planks and crafting tables became wood rubble. */
    public static boolean isWoodMaterial(ServerLevel level, BlockPos pos) {
        BlockState state = get(level, pos);
        Block block = state.getBlock();
        if (block == ModBlocks.INFESTED_TRUNK.get()) {
            return false;
        }
        if (block == ModBlocks.legacyBlock("parasiteplank").get()) {
            return true;
        }
        if (isWoodLike(level, pos)) {
            return true;
        }
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        String path = id.getPath();
        return path.endsWith("_planks") || path.endsWith("_fence") || path.endsWith("_fence_gate")
                || path.endsWith("_door") || path.endsWith("_trapdoor")
                || (path.endsWith("_stairs") && path.contains("wood"))
                || (path.endsWith("_slab") && path.contains("wood"))
                || block == Blocks.CRAFTING_TABLE || block == Blocks.CHEST
                || block == Blocks.TRAPPED_CHEST || block == Blocks.BOOKSHELF
                || block == Blocks.LADDER || block == Blocks.SCAFFOLDING;
    }

    /** Old {@code BlockSand} subclasses, kept for the harlequin/shrouded sand replacements. */
    public static boolean isSand(Block block) {
        return block == Blocks.SAND || block == Blocks.RED_SAND
                || block == Blocks.SUSPICIOUS_SAND;
    }

    /** Old {@code BlockSandStone} subclasses. */
    public static boolean isSandstone(Block block) {
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        return id.getPath().endsWith("sandstone");
    }

    /** {@code BlockGrave}-style gravel / {@code Material#field_151595_p}. */
    public static boolean isGravel(Block block) {
        return block == Blocks.GRAVEL || block == Blocks.SUSPICIOUS_GRAVEL;
    }

    /** {@code Material#field_151588_w} / {@code field_151598_x} — ice became bloody ice. */
    public static boolean isIce(Block block) {
        return block == Blocks.ICE || block == Blocks.FROSTED_ICE || block == Blocks.PACKED_ICE
                || block == Blocks.BLUE_ICE || block == Blocks.SNOW_BLOCK;
    }

    /** {@code Material#field_151576_e} (GROUND) — stone family. */
    public static boolean isStoneMaterial(Block block) {
        return block == Blocks.STONE || block == Blocks.GRANITE || block == Blocks.DIORITE
                || block == Blocks.ANDESITE || block == Blocks.DEEPSLATE || block == Blocks.TUFF
                || block == Blocks.CALCITE || block == Blocks.SMOOTH_STONE || block == Blocks.TERRACOTTA
                || block == Blocks.NETHERRACK || block == Blocks.END_STONE
                || block == Blocks.POLISHED_GRANITE || block == Blocks.POLISHED_DIORITE
                || block == Blocks.POLISHED_ANDESITE || block == Blocks.DRIPSTONE_BLOCK
                || block == Blocks.BASALT || block == Blocks.SMOOTH_BASALT || block == Blocks.BLACKSTONE;
    }

    /** Ore-like stone, used by the shrouded stone replacement. */
    public static boolean isOre(Block block) {
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        return id.getPath().endsWith("_ore");
    }

    public static boolean isDirtMaterial(Block block) {
        return block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.ROOTED_DIRT
                || block == Blocks.GRASS_BLOCK || block == Blocks.PODZOL || block == Blocks.MYCELIUM
                || block == Blocks.MUD || block == Blocks.CLAY || block == Blocks.FARMLAND
                || block == Blocks.DIRT_PATH || block == Blocks.MOSS_BLOCK
                || block == Blocks.SNOW || block == Blocks.SNOW_BLOCK;
    }

    public static boolean isBrick(Block block) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath()
                .contains("brick");
    }

    /** Old flower-replaceable test used by the flower decoration slots. */
    public static boolean isReplaceableForDecoration(ServerLevel level, BlockPos pos) {
        BlockState state = get(level, pos);
        return state.isAir() || state.getBlock() instanceof net.minecraft.world.level.block.FlowerBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.TallFlowerBlock
                || state.canBeReplaced();
    }

    // ---------------------------------------------------------------------------------------------
    // The original abstract generators' replaceability rules
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code WorldGenParasiteGenAbstract#canGrowInto}: air, leaves, grass, dirt, the four old log
     * kinds, podzol and the parasite bush.
     */
    public static boolean canGrowInto(BlockState state) {
        Block block = state.getBlock();
        return state.isAir() || block instanceof LeavesBlock
                || block == Blocks.GRASS_BLOCK || block == Blocks.DIRT
                || block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG
                || block == Blocks.BIRCH_LOG || block == Blocks.JUNGLE_LOG
                || block == Blocks.PODZOL
                || block == ModBlocks.legacyBlock("parasitebush").get();
    }

    /** {@code WorldGenParasiteTreeAbstract#isReplaceable} / the bush variant of the same test. */
    public static boolean isReplaceable(ServerLevel level, BlockPos pos) {
        BlockState state = get(level, pos);
        return state.isAir()
                || state.getBlock() instanceof LeavesBlock
                || isWoodLike(level, pos)
                || canGrowInto(state)
                || state.getBlock() == ModBlocks.legacyBlock("parasitebush").get();
    }

    /** {@code WorldGenParasiteTreeAbstract#setDirtAt} — grass is replaced by parasite stain. */
    public static void setDirtAt(ServerLevel level, BlockPos pos) {
        BlockState state = get(level, pos);
        if (state.getBlock() != Blocks.DIRT && state.getBlock() != Blocks.GRASS_BLOCK) {
            setBlock(level, pos, ModBlocks.INFESTED_STAIN.get().defaultBlockState());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Directions
    // ---------------------------------------------------------------------------------------------

    /** {@code BlockPos#north} / {@code east} / {@code south} / {@code west}, 0..3 like the original. */
    public static BlockPos horizontal(BlockPos pos, int choice) {
        return switch (Math.floorMod(choice, 4)) {
            case 0 -> pos.north();
            case 1 -> pos.east();
            case 2 -> pos.south();
            default -> pos.west();
        };
    }

    /**
     * The two-step "diagonal" walks of {@code directionToGrow(..., sideCurse = true)}.  The original
     * pairs {@code i * 10} with the right-hand diagonal and {@code i * 10 + 1} with the left-hand
     * one: 0/1 = north-east/north-west, 10/11 = south-east/north-east, 20/21 = south-west/south-east,
     * 30/31 = north-west/south-west.
     */
    public static BlockPos sideCurse(BlockPos pos, int choice) {
        return switch (choice) {
            case 0 -> pos.north().east();
            case 1 -> pos.north().west();
            case 10 -> pos.east().south();
            case 11 -> pos.east().north();
            case 20 -> pos.south().west();
            case 21 -> pos.south().east();
            case 30 -> pos.west().north();
            default -> pos.west().south();
        };
    }

    /**
     * Rounded down to a block, matching the original {@code BlockPos#offset(EnumFacing, int)}
     * arithmetic for the five-step helpers (each step is a whole block).
     */
    public static BlockPos steps(BlockPos pos, int choice, int times) {
        BlockPos result = pos;
        for (int i = 0; i < times; i++) {
            result = horizontal(result, choice);
        }
        return result;
    }

    /** Corner of a chunk that the original decorator used: {@code chunkPos.add(j, 0, k)}. */
    static BlockPos chunkOffset(int chunkX, int chunkZ, RandomSource random) {
        return new BlockPos((chunkX << 4) + random.nextInt(16) + 8, 0,
                (chunkZ << 4) + random.nextInt(16) + 8);
    }

    /** Surface position of a column; the original used {@code World#getTopSolidOrLiquidBlock}. */
    public static BlockPos surface(ServerLevel level, BlockPos column) {
        return level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
    }

    /**
     * Port of {@code ParasiteEventEntity#getFloor(World, BlockPos, int)}: walks down at most
     * {@code range} blocks looking for the first non-air position, or {@code null} if there is none.
     */
    public static BlockPos floor(ServerLevel level, BlockPos pos, int range) {
        BlockPos current = pos;
        int remaining = range;
        while (current.getY() > level.getMinY() && remaining >= 0) {
            remaining--;
            if (!get(level, current).isAir()) {
                return current;
            }
            current = current.below();
        }
        return null;
    }
}
