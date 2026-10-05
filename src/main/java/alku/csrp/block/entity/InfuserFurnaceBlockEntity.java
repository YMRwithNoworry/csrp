package alku.csrp.block.entity;

import alku.csrp.recipe.InfuserFurnaceRecipe;
import alku.csrp.recipe.InfuserFurnaceRecipes;
import alku.csrp.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

/**
 * Metallurgical Infuser Furnace.  Port of SRP 1.10.9 {@code tileentity/TileEntityInfuserFurnace}.
 *
 * <p>The original is a furnace <em>and</em> an infuser: it runs vanilla smelting from the smelt slot
 * into the smelt output while independently running infusion from the smelt slot plus the infuse slot
 * into an infused output and an optional bottle byproduct.  Both processes share one fuel buffer and
 * advance on separate 200 tick counters.</p>
 *
 * <p>Slot layout matches the original exactly (the GUI texture's slot frames sit at these positions):
 * <ul>
 *   <li>0 {@code SLOT_SMELT_IN} — smelt input, also the infusion's smelt input</li>
 *   <li>1 {@code SLOT_FUEL} — fuel</li>
 *   <li>2 {@code SLOT_SMELT_OUT} — vanilla smelting output</li>
 *   <li>3 {@code SLOT_INFUSE_IN} — infusion reagent</li>
 *   <li>4 {@code SLOT_INFUSE_OUT} — infusion output</li>
 *   <li>5 {@code SLOT_BOTTLE_OUT} — infusion byproduct</li>
 * </ul>
 */
public final class InfuserFurnaceBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_SMELT_IN = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_SMELT_OUT = 2;
    public static final int SLOT_INFUSE_IN = 3;
    public static final int SLOT_INFUSE_OUT = 4;
    public static final int SLOT_BOTTLE_OUT = 5;
    public static final int CONTAINER_SIZE = 6;
    /** {@code TileEntityInfuserFurnace.COOK_TIME_TOTAL}. */
    public static final int COOK_TIME_TOTAL = 200;

    /** Legacy aliases kept for the call sites written against the earlier four slot layout. */
    public static final int IRON_SLOT = SLOT_SMELT_IN;
    public static final int BLOOD_SLOT = SLOT_INFUSE_IN;
    public static final int FUEL_SLOT = SLOT_FUEL;
    public static final int OUTPUT_SLOT = SLOT_INFUSE_OUT;
    public static final int PROCESS_TIME = COOK_TIME_TOTAL;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int burnTime;
    private int currentBurnTime;
    private int cookTimeSmelt;
    private int cookTimeInfuse;

    /** {@code getField(id)}: 0 burnTime, 1 currentBurnTime, 2 cookSmelt, 3 cookInfuse. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> currentBurnTime;
                case 2 -> cookTimeSmelt;
                case 3 -> cookTimeInfuse;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> currentBurnTime = value;
                case 2 -> cookTimeSmelt = value;
                case 3 -> cookTimeInfuse = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public InfuserFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFUSER_FURNACE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (!(blockEntity instanceof InfuserFurnaceBlockEntity furnace) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        furnace.tick(serverLevel);
    }

    /** {@code TileEntityInfuserFurnace.update()}. */
    private void tick(ServerLevel level) {
        boolean wasBurning = isBurning();
        boolean dirty = false;
        if (isBurning()) {
            burnTime--;
        }

        InfuserFurnaceRecipe recipe = currentRecipe();
        if (!isBurning() && (canSmelt(level) || canInfuse(recipe))) {
            int fuelBurn = fuelBurnTime(level, items.get(SLOT_FUEL));
            if (fuelBurn > 0) {
                burnTime = fuelBurn;
                currentBurnTime = fuelBurn;
                dirty = true;
                consumeFuel();
            }
        }

        if (isBurning() && canSmelt(level)) {
            cookTimeSmelt++;
            if (cookTimeSmelt >= COOK_TIME_TOTAL) {
                cookTimeSmelt = 0;
                doSmelt(level);
                dirty = true;
            }
        } else {
            cookTimeSmelt = 0;
        }

        if (isBurning() && canInfuse(recipe)) {
            cookTimeInfuse++;
            if (cookTimeInfuse >= COOK_TIME_TOTAL) {
                cookTimeInfuse = 0;
                doInfuse(recipe);
                dirty = true;
            }
        } else {
            cookTimeInfuse = 0;
        }

        if (wasBurning != isBurning()) {
            dirty = true;
        }
        if (dirty) {
            setChanged();
        }
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    private InfuserFurnaceRecipe currentRecipe() {
        return InfuserFurnaceRecipes.find(items.get(SLOT_SMELT_IN), items.get(SLOT_INFUSE_IN));
    }

    /** {@code canSmelt()}: a vanilla smelting recipe must exist and fit in the smelt output. */
    private boolean canSmelt(ServerLevel level) {
        ItemStack in = items.get(SLOT_SMELT_IN);
        if (in.isEmpty()) {
            return false;
        }
        ItemStack result = smeltResult(level, in);
        if (result.isEmpty()) {
            return false;
        }
        return canMerge(items.get(SLOT_SMELT_OUT), result);
    }

    private void doSmelt(ServerLevel level) {
        ItemStack in = items.get(SLOT_SMELT_IN);
        ItemStack result = smeltResult(level, in).copy();
        items.set(SLOT_SMELT_OUT, pushToOutput(items.get(SLOT_SMELT_OUT), result));
        in.shrink(1);
    }

    private static ItemStack smeltResult(ServerLevel level, ItemStack input) {
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, recipeInput, level)
                .map(holder -> holder.value().assemble(recipeInput))
                .orElse(ItemStack.EMPTY);
    }

    /** {@code canInfuse(r)}. */
    private boolean canInfuse(InfuserFurnaceRecipe recipe) {
        if (recipe == null) {
            return false;
        }
        return canMerge(items.get(SLOT_INFUSE_OUT), recipe.output())
                && canMerge(items.get(SLOT_BOTTLE_OUT), recipe.byproduct());
    }

    /** {@code doInfuse(r)}: one from each input, main output plus the bottle byproduct. */
    private void doInfuse(InfuserFurnaceRecipe recipe) {
        if (!canInfuse(recipe)) {
            return;
        }
        items.get(SLOT_SMELT_IN).shrink(1);
        items.get(SLOT_INFUSE_IN).shrink(1);
        mergeInto(SLOT_INFUSE_OUT, recipe.output());
        mergeInto(SLOT_BOTTLE_OUT, recipe.byproduct());
    }

    /** {@code canMerge(slotStack, add)}: empty always fits, otherwise same item with room. */
    private static boolean canMerge(ItemStack slotStack, ItemStack add) {
        if (add.isEmpty() || slotStack.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(slotStack, add)) {
            return false;
        }
        return slotStack.getCount() + add.getCount() <= slotStack.getMaxStackSize();
    }

    /** {@code mergeInto(slot, add)}. */
    private void mergeInto(int slot, ItemStack add) {
        if (add.isEmpty()) {
            return;
        }
        ItemStack current = items.get(slot);
        if (current.isEmpty()) {
            items.set(slot, add.copy());
        } else {
            current.grow(add.getCount());
            items.set(slot, current);
        }
    }

    /** {@code pushToOutput(existing, toAdd)}. */
    private static ItemStack pushToOutput(ItemStack existing, ItemStack toAdd) {
        if (existing.isEmpty()) {
            return toAdd;
        }
        existing.grow(toAdd.getCount());
        return existing;
    }

    /** {@code TileEntityInfuserFurnace.getContainerItem} for the spent fuel, e.g. a lava bucket. */
    private void consumeFuel() {
        ItemStack fuel = items.get(SLOT_FUEL);
        ItemStackTemplate remainder = fuel.getItem().getCraftingRemainder();
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            if (remainder != null) {
                items.set(SLOT_FUEL, remainder.create());
            } else if (fuel.is(Items.LAVA_BUCKET)) {
                items.set(SLOT_FUEL, new ItemStack(Items.BUCKET));
            } else {
                items.set(SLOT_FUEL, ItemStack.EMPTY);
            }
        }
    }

    private int fuelBurnTime(ServerLevel level, ItemStack stack) {
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime,
                getLootContext(level, stack), 0);
    }

    /** {@code isItemValidForSlot}: outputs reject, the fuel slot wants fuel, the rest accept. */
    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index == SLOT_SMELT_OUT || index == SLOT_INFUSE_OUT || index == SLOT_BOTTLE_OUT) {
            return false;
        }
        if (index == SLOT_FUEL) {
            return stack.has(DataComponents.COOKING_FUEL);
        }
        return true;
    }

    public ContainerData dataAccess() {
        return data;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.csrp.infuser_furnace");
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new alku.csrp.inventory.InfuserFurnaceMenu(containerId, inventory, this, data);
    }

    /** NBT keys follow the original: BurnTime / CurrentBurnTime / CookSmelt / CookInfuse. */
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("BurnTime", burnTime);
        output.putInt("CurrentBurnTime", currentBurnTime);
        output.putInt("CookSmelt", cookTimeSmelt);
        output.putInt("CookInfuse", cookTimeInfuse);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        burnTime = input.getIntOr("BurnTime", 0);
        currentBurnTime = input.getIntOr("CurrentBurnTime", 0);
        cookTimeSmelt = input.getIntOr("CookSmelt", 0);
        cookTimeInfuse = input.getIntOr("CookInfuse", 0);
    }
}
