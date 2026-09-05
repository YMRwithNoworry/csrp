package alku.csrp.item;

import alku.csrp.Csrp;
import alku.csrp.config.GeneralConfig;
import alku.csrp.registry.ModEntities;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Compatibility implementation of SRP 1.10's {@code ItemMobSpawner}.
 *
 * The old item family was deliberately separate from vanilla spawn eggs and
 * used the id {@code itembmobspawner_<entity>}.  Keeping that family available
 * matters for old recipes, commands and saved item stacks, so this item
 * retains the original 64-stack size, fluid right-click behaviour, custom
 * names and EntityTag application.
 */
public final class LegacyMobSpawnerItem extends Item {
    private final String legacyName;

    public LegacyMobSpawnerItem(String legacyName, Properties properties) {
        super(properties.stacksTo(64));
        this.legacyName = legacyName;
    }

    public String legacyName() {
        return legacyName;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        if (player != null && !player.mayUseItemAt(spawnPos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        Entity entity = createEntity(level, spawnPos.getX() + 0.5D,
                spawnPos.getY() + getYOffset(level, spawnPos), spawnPos.getZ() + 0.5D);
        if (entity == null) {
            return InteractionResult.PASS;
        }
        finishSpawn(level, player, stack, entity);
        if (!level.addFreshEntity(entity)) {
            return InteractionResult.PASS;
        }
        consume(player, stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos fluidPos = blockHit.getBlockPos();
        if (level.getFluidState(fluidPos).isEmpty()
                || !player.mayUseItemAt(fluidPos, blockHit.getDirection(), stack)) {
            return InteractionResultHolder.pass(stack);
        }
        Entity entity = createEntity(level, fluidPos.getX() + 0.5D,
                fluidPos.getY() + 0.5D, fluidPos.getZ() + 0.5D);
        if (entity == null) {
            return InteractionResultHolder.pass(stack);
        }
        finishSpawn(level, player, stack, entity);
        if (!level.addFreshEntity(entity)) {
            return InteractionResultHolder.pass(stack);
        }
        consume(player, stack);
        return InteractionResultHolder.success(stack);
    }

    private Entity createEntity(Level level, double x, double y, double z) {
        if (!GeneralConfig.allowMobs()) {
            return null;
        }
        String currentId = currentEntityId(legacyName);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Csrp.MODID, currentId);
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        Entity entity = type.orElse(ModEntities.CRUX.get()).create(level);
        if (entity == null) {
            return null;
        }
        entity.moveTo(x, y + (legacyName.equals("pod") ? 25.0D : 0.0D), z,
                level.random.nextFloat() * 360.0F, 0.0F);
        return entity;
    }

    private static void finishSpawn(Level level, Player player, ItemStack stack, Entity entity) {
        if (entity instanceof LivingEntity living && stack.get(DataComponents.CUSTOM_NAME) != null) {
            living.setCustomName(stack.getHoverName());
        }
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag itemTag = data.copyTag();
        if (!itemTag.contains("EntityTag", CompoundTag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag entityTag = itemTag.getCompound("EntityTag");
        CompoundTag saved = entity.saveWithoutId(new CompoundTag());
        saved.merge(entityTag);
        java.util.UUID uuid = entity.getUUID();
        entity.load(saved);
        entity.setUUID(uuid);
    }

    private static void consume(Player player, ItemStack stack) {
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static double getYOffset(Level level, BlockPos pos) {
        // Entity collision stacking is a minor visual detail in the original;
        // a zero offset is safe in modern mappings and preserves block-floor
        // placement for every legacy mob.
        return 0.0D;
    }

    /** Maps names that disappeared during the 1.21 port to the closest SRP family. */
    private static String currentEntityId(String name) {
        return switch (name) {
            case "pod" -> "anc_pod";
            case "mes", "infplayer" -> "sim_human";
            case "infplayerhead" -> "sim_humanhead";
            case "infsquid" -> "sim_squid";
            case "infbear" -> "sim_bear";
            case "infcow" -> "sim_cow";
            case "infcowhead" -> "sim_cowhead";
            case "infhuman" -> "sim_human";
            case "infhumanhead" -> "sim_humanhead";
            case "infenderman" -> "sim_enderman";
            case "infendermanhead" -> "sim_endermanhead";
            case "infsheep" -> "sim_sheep";
            case "infsheephead" -> "sim_sheephead";
            case "infwolf" -> "sim_wolf";
            case "infwolfhead" -> "sim_wolfhead";
            case "infpig" -> "sim_pig";
            case "infpighead" -> "sim_pighead";
            case "infvillager" -> "sim_villager";
            case "infvillagerhead" -> "sim_villagerhead";
            case "infhorse" -> "sim_horse";
            case "infhorsehead" -> "sim_horsehead";
            case "infdragone" -> "sim_dragone";
            case "infdragonehead" -> "sim_dragonhead";
            case "inhoos" -> "pri_vermin";
            case "inhoom" -> "hostii";
            case "quac", "cruxa" -> "crux";
            case "cruxb" -> "crux_incomplete";
            case "done" -> "heed";
            case "lodo", "mudo", "nuuh", "ata", "rathol", "gothol", "buthol" -> "pri_longarms";
            case "venkrol" -> "beckon_si";
            case "venkrolsii" -> "beckon_sii";
            case "venkrolsiii" -> "beckon_siii";
            case "venkrolsiv", "venkrolsv" -> "beckon_siv";
            case "nak" -> "seizer";
            case "dod" -> "dispatcher_si";
            case "dodsii" -> "dispatcher_sii";
            case "dodsiii" -> "dispatcher_siii";
            case "dodsiv" -> "dispatcher_siv";
            case "leem" -> "rooter_si";
            case "leemsii" -> "rooter_sii";
            case "leemsiii" -> "rooter_siii";
            case "leemsiv" -> "rooter_siv";
            case "alafha" -> "grunt";
            case "ganro" -> "monarch";
            case "omboo" -> "bomber_light";
            case "esor", "orch" -> "overseer";
            case "flog" -> "seeker";
            case "anged" -> "vigilante";
            case "jinjo" -> "marauder";
            case "vesta" -> "wraith";
            case "pheon", "lencia", "elvia" -> "haunter";
            case "heblu" -> "wraith";
            case "oronco" -> "anc_overlord";
            case "terla" -> "anc_dreadnaut";
            case "abobodies" -> "abo_bodies";
            case "abohead" -> "abo_head";
            case "marcow", "marenderman", "marvillager", "marhuman", "marsheep", "marbear" ->
                    name.replace("mar", "mar_");
            case "ferbear", "fercow", "ferhorse", "ferhuman", "ferpig", "fersheep", "ferwolf", "fervillager",
                    "ferenderman" -> "fer_" + name.substring(3);
            default -> name;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.csrp.itemmobspawner", legacyName)
                .withStyle(ChatFormatting.GRAY));
    }
}
