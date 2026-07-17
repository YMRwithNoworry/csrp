package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.item.BoughItem;
import alku.csrp.item.FalseAppleItem;
import alku.csrp.item.FishlinItem;
import alku.csrp.item.HijackedArmorItem;
import alku.csrp.item.HijackedToolItem;
import alku.csrp.item.LivingArmorItem;
import alku.csrp.item.LivingBowItem;
import alku.csrp.item.LivingWeaponItem;
import alku.csrp.item.QuenchItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Csrp.MODID);

    public static final DeferredItem<SpawnEggItem> BUGLIN_SPAWN_EGG = ITEMS.registerItem(
            "buglin_spawn_egg", properties -> new SpawnEggItem(ModEntities.BUGLIN.get(), 0x8B1E1E, 0xE1B85B, properties),
            new Item.Properties());
    public static final DeferredItem<SpawnEggItem> RUPTER_SPAWN_EGG = ITEMS.registerItem(
            "rupter_spawn_egg", properties -> new SpawnEggItem(ModEntities.RUPTER.get(), 0x6E1717, 0xD8B45B, properties),
            new Item.Properties());
    public static final DeferredItem<SpawnEggItem> PRI_LONGARMS_SPAWN_EGG = spawnEgg(
            "pri_longarms_spawn_egg", ModEntities.PRI_LONGARMS, 0x551C1C, 0xC9A17B);
    public static final DeferredItem<SpawnEggItem> PRI_SUMMONER_SPAWN_EGG = spawnEgg(
            "pri_summoner_spawn_egg", ModEntities.PRI_SUMMONER, 0x321818, 0xA06D50);
    public static final DeferredItem<SpawnEggItem> PRI_VERMIN_SPAWN_EGG = spawnEgg(
            "pri_vermin_spawn_egg", ModEntities.PRI_VERMIN, 0x48151B, 0xD4B75C);
    public static final DeferredItem<SpawnEggItem> PRI_VISCERA_SPAWN_EGG = spawnEgg(
            "pri_viscera_spawn_egg", ModEntities.PRI_VISCERA, 0x421517, 0xA68B69);
    public static final DeferredItem<SpawnEggItem> GNAT_SPAWN_EGG = spawnEgg(
            "gnat_spawn_egg", ModEntities.GNAT, 0x4B1717, 0xB7A277);
    public static final DeferredItem<SpawnEggItem> CARRIER_HEAVY_SPAWN_EGG = spawnEgg(
            "carrier_heavy_spawn_egg", ModEntities.CARRIER_HEAVY, 0x552525, 0xBE9D60);
    public static final DeferredItem<SpawnEggItem> CARRIER_LIGHT_SPAWN_EGG = spawnEgg(
            "carrier_light_spawn_egg", ModEntities.CARRIER_LIGHT, 0x6B2D27, 0xD0AE6C);
    public static final DeferredItem<SpawnEggItem> CARRIER_FLYING_SPAWN_EGG = spawnEgg(
            "carrier_flying_spawn_egg", ModEntities.CARRIER_FLYING, 0x4A2025, 0xB98555);
    public static final DeferredItem<Item> RUPTER_VISCERA = simple("rupter_viscera");
    public static final DeferredItem<BlockItem> TUNNEL = ITEMS.registerSimpleBlockItem("tunnel", ModBlocks.TUNNEL);
    public static final DeferredItem<BlockItem> RESIDUE_PLANTS = ITEMS.registerSimpleBlockItem(
            "residue_plants", ModBlocks.RESIDUE_PLANTS);

    public static final DeferredItem<Item> ASSIMILATED_FLESH = simple("assimilated_flesh");
    public static final DeferredItem<Item> BONE = simple("bone");
    public static final DeferredItem<Item> BECKON_DROP = simple("beckon_drop");
    public static final DeferredItem<Item> DISPATCHER_DROP = simple("dispatcher_drop");
    public static final DeferredItem<Item> ADA_LONGARMS_DROP = simple("ada_longarms_drop");
    public static final DeferredItem<Item> ADA_SUMMONER_DROP = simple("ada_summoner_drop");
    public static final DeferredItem<Item> ADA_YELLOWEYE_DROP = simple("ada_yelloweye_drop");
    public static final DeferredItem<Item> ADA_REEKER_DROP = simple("ada_reeker_drop");
    public static final DeferredItem<Item> ADA_MANDUCATER_DROP = simple("ada_manducater_drop");
    public static final DeferredItem<Item> ADA_BOLSTER_DROP = simple("ada_bolster_drop");
    public static final DeferredItem<Item> ADA_ARACHNIDA_DROP = simple("ada_arachnida_drop");
    public static final DeferredItem<Item> ADA_DEVOURER_DROP = simple("ada_devourer_drop");
    public static final DeferredItem<Item> ADA_VERMIN_DROP = simple("ada_vermin_drop");
    public static final DeferredItem<Item> ADA_VISCERA_DROP = simple("ada_viscera_drop");
    public static final DeferredItem<Item> HIJACKED_DROP = simple("hijacked_drop");
    public static final DeferredItem<Item> HIVE_SCRAP = simple("hive_scrap");
    public static final DeferredItem<Item> BLOODY_IRON_INGOT = simple("bloody_iron_ingot");
    public static final DeferredItem<Item> BLOODY_ROD = simple("bloody_rod");
    public static final DeferredItem<Item> BLOODY_BONE = simple("bloody_bone");
    public static final DeferredItem<Item> LURECOMPONENT1 = simple("lurecomponent1");
    public static final DeferredItem<Item> LURECOMPONENT2 = simple("lurecomponent2");
    public static final DeferredItem<Item> LURECOMPONENT3 = simple("lurecomponent3");
    public static final DeferredItem<Item> LURECOMPONENT4 = simple("lurecomponent4");
    public static final DeferredItem<Item> LURECOMPONENT5 = simple("lurecomponent5");
    public static final DeferredItem<Item> LURECOMPONENT6 = simple("lurecomponent6");
    public static final DeferredItem<Item> DRIED_TENDONS = simple("dried_tendons");
    public static final DeferredItem<Item> HARDENED_BONE_HANDLE = simple("hardened_bone_handle");
    public static final DeferredItem<Item> INFECTIOUS_BLADE_FRAGMENT = simple("infectious_blade_fragment");
    public static final DeferredItem<Item> LIVING_CORE = simple("living_core");
    public static final DeferredItem<Item> VILE_SHELL = simple("vile_shell");
    public static final DeferredItem<Item> SEMIORGANIC_INGOT = simple("semiorganic_ingot");

    public static final DeferredItem<FalseAppleItem> FALSE_APPLE = ITEMS.registerItem(
            "false_apple", FalseAppleItem::new, new Item.Properties());
    public static final DeferredItem<FishlinItem> FISHLIN = ITEMS.registerItem(
            "fishlin", FishlinItem::new, new Item.Properties());
    public static final DeferredItem<Item> SHRIMP = simple("shrimp");
    public static final DeferredItem<Item> ALVEOLIGROWTH = simple("alveoligrowth");
    public static final DeferredItem<BoneMealItem> INFESTED_BONEMEAL = ITEMS.registerItem(
            "infested_bonemeal", BoneMealItem::new, new Item.Properties());
    public static final DeferredItem<Item> EVCLOCK = simple("evclock", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> LEVELCLOCK = simple("levelclock", new Item.Properties().stacksTo(1));
    public static final DeferredItem<CompassItem> NODECOMPASS = ITEMS.registerItem(
            "nodecompass", CompassItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<CompassItem> COLONYCOMPASS = ITEMS.registerItem(
            "colonycompass", CompassItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<CompassItem> ORIGINCOMPASS = ITEMS.registerItem(
            "origincompass", CompassItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SRP_FIELD_GUIDE = simple("srp_field_guide", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> BOOK_OF_VENGEANCE = simple("book_of_vengeance", new Item.Properties().stacksTo(1));
    public static final DeferredItem<QuenchItem> ITEMTHROW = ITEMS.registerItem(
            "itemthrow", QuenchItem::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<BoughItem> BOUGH = ITEMS.registerItem(
            "bough", BoughItem::new, new Item.Properties());
    public static final DeferredItem<Item> THORNSHADE_DECANTER = simple("thornshade_decanter", new Item.Properties().stacksTo(1));
    public static final DeferredItem<ArmorItem> VENKROL_BOOTS = ITEMS.registerItem("venkrol_boots",
            properties -> new ArmorItem(ModArmorMaterials.VENKROL, ArmorItem.Type.BOOTS, properties.durability(520)),
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> MODULE_BASE = simple("module_base", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> TISSUE_SPIKE = simple("tissue_spike", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> ORGAN_SYNTH = simple("organ_synth", new Item.Properties().stacksTo(1));

    public static final DeferredItem<LivingWeaponItem> WEAPON_SCYTHE_SENTIENT = livingWeapon(
            "weapon_scythe_sentient", LivingWeaponItem.WeaponKind.SCYTHE, 34.0F, -3.3F, 5.0F, true, null);
    public static final DeferredItem<LivingWeaponItem> WEAPON_SCYTHE = livingWeapon(
            "weapon_scythe", LivingWeaponItem.WeaponKind.SCYTHE, 17.0F, -3.1F, 4.0F, false, WEAPON_SCYTHE_SENTIENT);
    public static final DeferredItem<LivingWeaponItem> WEAPON_AXE_SENTIENT = livingWeapon(
            "weapon_axe_sentient", LivingWeaponItem.WeaponKind.AXE, 42.0F, -3.3F, 5.0F, true, null);
    public static final DeferredItem<LivingWeaponItem> WEAPON_AXE = livingWeapon(
            "weapon_axe", LivingWeaponItem.WeaponKind.AXE, 21.0F, -3.1F, 4.0F, false, WEAPON_AXE_SENTIENT);
    public static final DeferredItem<LivingWeaponItem> WEAPON_SWORD_SENTIENT = livingWeapon(
            "weapon_sword_sentient", LivingWeaponItem.WeaponKind.SWORD, 38.0F, -3.3F, 6.0F, true, null);
    public static final DeferredItem<LivingWeaponItem> WEAPON_SWORD = livingWeapon(
            "weapon_sword", LivingWeaponItem.WeaponKind.SWORD, 19.0F, -3.1F, 4.5F, false, WEAPON_SWORD_SENTIENT);
    public static final DeferredItem<LivingWeaponItem> WEAPON_CLEAVER_SENTIENT = livingWeapon(
            "weapon_cleaver_sentient", LivingWeaponItem.WeaponKind.CLEAVER, 38.0F, -3.3F, 6.0F, true, null);
    public static final DeferredItem<LivingWeaponItem> WEAPON_CLEAVER = livingWeapon(
            "weapon_cleaver", LivingWeaponItem.WeaponKind.CLEAVER, 19.0F, -3.1F, 4.5F, false, WEAPON_CLEAVER_SENTIENT);
    public static final DeferredItem<LivingWeaponItem> WEAPON_MAUL_SENTIENT = livingWeapon(
            "weapon_maul_sentient", LivingWeaponItem.WeaponKind.MAUL, 42.0F, -3.3F, 5.0F, true, null);
    public static final DeferredItem<LivingWeaponItem> WEAPON_MAUL = livingWeapon(
            "weapon_maul", LivingWeaponItem.WeaponKind.MAUL, 21.0F, -3.1F, 4.0F, false, WEAPON_MAUL_SENTIENT);
    public static final DeferredItem<LivingWeaponItem> WEAPON_LANCE_SENTIENT = livingWeapon(
            "weapon_lance_sentient", LivingWeaponItem.WeaponKind.LANCE, 34.0F, -3.3F, 7.0F, true, null);
    public static final DeferredItem<LivingWeaponItem> WEAPON_LANCE = livingWeapon(
            "weapon_lance", LivingWeaponItem.WeaponKind.LANCE, 17.0F, -3.1F, 5.0F, false, WEAPON_LANCE_SENTIENT);
    public static final DeferredItem<LivingBowItem> WEAPON_BOW_SENTIENT = ITEMS.registerItem(
            "weapon_bow_sentient", properties -> new LivingBowItem(true, null, properties), new Item.Properties());
    public static final DeferredItem<LivingBowItem> WEAPON_BOW = ITEMS.registerItem(
            "weapon_bow", properties -> new LivingBowItem(false, WEAPON_BOW_SENTIENT, properties), new Item.Properties());

    public static final DeferredItem<LivingArmorItem> ARMOR_HELM_SENTIENT = livingArmor(
            "armor_helm_sentient", ArmorItem.Type.HELMET, true, null);
    public static final DeferredItem<LivingArmorItem> ARMOR_CHEST_SENTIENT = livingArmor(
            "armor_chest_sentient", ArmorItem.Type.CHESTPLATE, true, null);
    public static final DeferredItem<LivingArmorItem> ARMOR_PANTS_SENTIENT = livingArmor(
            "armor_pants_sentient", ArmorItem.Type.LEGGINGS, true, null);
    public static final DeferredItem<LivingArmorItem> ARMOR_BOOTS_SENTIENT = livingArmor(
            "armor_boots_sentient", ArmorItem.Type.BOOTS, true, null);
    public static final DeferredItem<LivingArmorItem> ARMOR_HELM = livingArmor(
            "armor_helm", ArmorItem.Type.HELMET, false, ARMOR_HELM_SENTIENT);
    public static final DeferredItem<LivingArmorItem> ARMOR_CHEST = livingArmor(
            "armor_chest", ArmorItem.Type.CHESTPLATE, false, ARMOR_CHEST_SENTIENT);
    public static final DeferredItem<LivingArmorItem> ARMOR_PANTS = livingArmor(
            "armor_pants", ArmorItem.Type.LEGGINGS, false, ARMOR_PANTS_SENTIENT);
    public static final DeferredItem<LivingArmorItem> ARMOR_BOOTS = livingArmor(
            "armor_boots", ArmorItem.Type.BOOTS, false, ARMOR_BOOTS_SENTIENT);

    public static final DeferredItem<HijackedArmorItem> HIJACKED_IRON_HELMET = hijackedArmor(
            "hijacked_iron_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<HijackedArmorItem> HIJACKED_IRON_CHESTPIECE = hijackedArmor(
            "hijacked_iron_chestpiece", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<HijackedArmorItem> HIJACKED_IRON_LEGGINGS = hijackedArmor(
            "hijacked_iron_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<HijackedArmorItem> HIJACKED_IRON_BOOTS = hijackedArmor(
            "hijacked_iron_boots", ArmorItem.Type.BOOTS);
    public static final DeferredItem<HijackedToolItem> HIJACKED_IRON_SWORD = ITEMS.registerItem(
            "hijacked_iron_sword", HijackedToolItem::new, new Item.Properties());
    public static final DeferredItem<AxeItem> HIJACKED_IRON_AXE = ITEMS.registerItem("hijacked_iron_axe",
            properties -> new AxeItem(ModTiers.HIJACKED_IRON, properties.attributes(
                    DiggerItem.createAttributes(ModTiers.HIJACKED_IRON, 7.5F, -3.05F))), new Item.Properties());
    public static final DeferredItem<PickaxeItem> HIJACKED_IRON_PICKAXE = ITEMS.registerItem("hijacked_iron_pickaxe",
            properties -> new PickaxeItem(ModTiers.HIJACKED_IRON, properties.attributes(
                    DiggerItem.createAttributes(ModTiers.HIJACKED_IRON, 1.0F, -2.8F))), new Item.Properties());
    public static final DeferredItem<ShovelItem> HIJACKED_IRON_SHOVEL = ITEMS.registerItem("hijacked_iron_shovel",
            properties -> new ShovelItem(ModTiers.HIJACKED_IRON, properties.attributes(
                    DiggerItem.createAttributes(ModTiers.HIJACKED_IRON, 1.5F, -3.0F))), new Item.Properties());
    public static final DeferredItem<HoeItem> HIJACKED_IRON_HOE = ITEMS.registerItem("hijacked_iron_hoe",
            properties -> new HoeItem(ModTiers.HIJACKED_IRON, properties.attributes(
                    DiggerItem.createAttributes(ModTiers.HIJACKED_IRON, -2.5F, -1.0F))), new Item.Properties());

    private static DeferredItem<Item> simple(String id) { return simple(id, new Item.Properties()); }
    private static <T extends net.minecraft.world.entity.Mob> DeferredItem<SpawnEggItem> spawnEgg(String id,
            net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
                    net.minecraft.world.entity.EntityType<T>> type, int primary, int secondary) {
        return ITEMS.registerItem(id, properties -> new SpawnEggItem(type.get(), primary, secondary, properties),
                new Item.Properties());
    }
    private static DeferredItem<Item> simple(String id, Item.Properties properties) {
        return ITEMS.registerSimpleItem(id, properties);
    }

    private static DeferredItem<LivingWeaponItem> livingWeapon(String id, LivingWeaponItem.WeaponKind kind,
            float damage, float speed,
            float reach, boolean sentient, DeferredItem<? extends Item> next) {
        return ITEMS.registerItem(id, properties -> new LivingWeaponItem(kind, damage, speed, reach, sentient,
                next == null ? null : next::get, properties), new Item.Properties().durability(sentient ? 1500 : 1000));
    }

    private static DeferredItem<LivingArmorItem> livingArmor(String id, ArmorItem.Type type,
            boolean sentient, DeferredItem<? extends Item> next) {
        return ITEMS.registerItem(id, properties -> new LivingArmorItem(
                sentient ? ModArmorMaterials.SENTIENT : ModArmorMaterials.LIVING, type, sentient,
                next == null ? null : next::get, properties), new Item.Properties());
    }

    private static DeferredItem<HijackedArmorItem> hijackedArmor(String id, ArmorItem.Type type) {
        return ITEMS.registerItem(id, properties -> new HijackedArmorItem(ModArmorMaterials.HIJACKED_IRON,
                type, properties.durability(type.getDurability(40))), new Item.Properties());
    }

    private ModItems() {
    }
}
