package alku.nocubessrparmory;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class AddonItems {
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NoCubesSrpCombatAddon.MOD_ID);

    // Materials and utility items retained from the 3.0.0 registry.
    static final DeferredItem<Item> TWISTED_PART = simple("twistedpart");
    static final DeferredItem<Item> PESTILENT_PART = simple("pestilentpart");
    static final DeferredItem<Item> GORE_PART = simple("gorepart");
    static final DeferredItem<Item> CARAPACE_PART = simple("carapacepart");
    static final DeferredItem<Item> LIVING_PART = simple("livingpart");
    static final DeferredItem<Item> CARBON_STEEL = simple("carbonsteel");
    static final DeferredItem<Item> RAW_CARBON_STEEL = simple("rawcarbonsteel");
    static final DeferredItem<Item> DREADNAUGHT_CORE = simple("dreadnaughtcore");
    static final DeferredItem<Item> OVERLORD_CORE = simple("overlordcore");
    static final DeferredItem<Item> FLAMETHROWER_CORE = stackable("flamethrowercore", 16);
    static final DeferredItem<Item> FLAMETHROWER_FUEL_TANK = stackable("flamethrowerfueltank", 16);
    static final DeferredItem<Item> FLAMETHROWER_GRIP = stackable("flamethrowergrip", 16);
    static final DeferredItem<Item> FLAMETHROWER_IGNITER = stackable("flamethrowerigniter", 16);
    static final DeferredItem<Item> FLAMETHROWER_MECHANISM = stackable("flamethrowermechanism", 16);
    static final DeferredItem<Item> FLAMETHROWER_SHAFT = stackable("flamethrowershaft", 16);
    static final DeferredItem<Item> INCINERATOR_CORE = stackable("incineratorcore", 16);
    static final DeferredItem<Item> PLASMA_TORCH_CORE = stackable("plasmatorchcore", 16);
    static final DeferredItem<Item> INFESTED_TOOL_ROD = stackable("infestedtoolrod", 16);
    static final DeferredItem<Item> UNFINISHED_FLAMETHROWER = stackable("unfinishedflamethrower", 16);
    static final DeferredItem<Item> CAPSULE_EMPTY = stackable("capsuleempty", 64);
    static final DeferredItem<Item> CAPSULE_FUEL = stackable("capsulefuel", 64);
    static final DeferredItem<Item> INVISIBLE_PROJECTILE = stackable("invisibleprojectile", 64);
    static final DeferredItem<Item> ICON_ANY_FLAMETHROWER = stackable("iconanyflamethrower", 64);
    static final DeferredItem<Item> ICON_ARMORY_TAB = stackable("iconarmorytab", 64);

    // Seven original armor sets: names and slot layout are kept world-compatible.
    static final DeferredItem<ArmorItem> TWISTED_ARMOR_HELMET = armor("twistedarmorhelmet", AddonArmorMaterials.TWISTED, ArmorItem.Type.HELMET, 44);
    static final DeferredItem<ArmorItem> TWISTED_ARMOR_BODY = armor("twistedarmorbody", AddonArmorMaterials.TWISTED, ArmorItem.Type.CHESTPLATE, 44);
    static final DeferredItem<ArmorItem> TWISTED_ARMOR_LEGS = armor("twistedarmorlegs", AddonArmorMaterials.TWISTED, ArmorItem.Type.LEGGINGS, 44);
    static final DeferredItem<ArmorItem> TWISTED_ARMOR_BOOTS = armor("twistedarmorboots", AddonArmorMaterials.TWISTED, ArmorItem.Type.BOOTS, 44);
    static final DeferredItem<ArmorItem> PESTILENT_ARMOR_HELMET = armor("pestilentarmorhelmet", AddonArmorMaterials.PESTILENT, ArmorItem.Type.HELMET, 200);
    static final DeferredItem<ArmorItem> PESTILENT_ARMOR_BODY = armor("pestilentarmorbody", AddonArmorMaterials.PESTILENT, ArmorItem.Type.CHESTPLATE, 200);
    static final DeferredItem<ArmorItem> PESTILENT_ARMOR_LEGS = armor("pestilentarmorlegs", AddonArmorMaterials.PESTILENT, ArmorItem.Type.LEGGINGS, 200);
    static final DeferredItem<ArmorItem> PESTILENT_ARMOR_BOOTS = armor("pestilentarmorboots", AddonArmorMaterials.PESTILENT, ArmorItem.Type.BOOTS, 200);
    static final DeferredItem<ArmorItem> GORE_ARMOR_HELMET = armor("gorearmorhelmet", AddonArmorMaterials.GORE, ArmorItem.Type.HELMET, 400);
    static final DeferredItem<ArmorItem> GORE_ARMOR_BODY = armor("gorearmorbody", AddonArmorMaterials.GORE, ArmorItem.Type.CHESTPLATE, 400);
    static final DeferredItem<ArmorItem> GORE_ARMOR_LEGS = armor("gorearmorlegs", AddonArmorMaterials.GORE, ArmorItem.Type.LEGGINGS, 400);
    static final DeferredItem<ArmorItem> GORE_ARMOR_BOOTS = armor("gorearmorboots", AddonArmorMaterials.GORE, ArmorItem.Type.BOOTS, 400);
    static final DeferredItem<ArmorItem> CARAPACE_ARMOR_HELMET = armor("carapacearmorhelmet", AddonArmorMaterials.CARAPACE, ArmorItem.Type.HELMET, 1000);
    static final DeferredItem<ArmorItem> CARAPACE_ARMOR_BODY = armor("carapacearmorbody", AddonArmorMaterials.CARAPACE, ArmorItem.Type.CHESTPLATE, 1000);
    static final DeferredItem<ArmorItem> CARAPACE_ARMOR_LEGS = armor("carapacearmorlegs", AddonArmorMaterials.CARAPACE, ArmorItem.Type.LEGGINGS, 1000);
    static final DeferredItem<ArmorItem> CARAPACE_ARMOR_BOOTS = armor("carapacearmorboots", AddonArmorMaterials.CARAPACE, ArmorItem.Type.BOOTS, 1000);
    static final DeferredItem<ArmorItem> EVOLUTION_ARMOR_HELMET = armor("evolutionarmorhelmet", AddonArmorMaterials.EVOLUTION, ArmorItem.Type.HELMET, 800);
    static final DeferredItem<ArmorItem> EVOLUTION_ARMOR_BODY = armor("evolutionarmorbody", AddonArmorMaterials.EVOLUTION, ArmorItem.Type.CHESTPLATE, 800);
    static final DeferredItem<ArmorItem> EVOLUTION_ARMOR_LEGS = armor("evolutionarmorlegs", AddonArmorMaterials.EVOLUTION, ArmorItem.Type.LEGGINGS, 800);
    static final DeferredItem<ArmorItem> EVOLUTION_ARMOR_BOOTS = armor("evolutionarmorboots", AddonArmorMaterials.EVOLUTION, ArmorItem.Type.BOOTS, 800);
    static final DeferredItem<ArmorItem> LIVING_ARMOR_HELMET = armor("livingarmorhelmet", AddonArmorMaterials.LIVING, ArmorItem.Type.HELMET, 600);
    static final DeferredItem<ArmorItem> LIVING_ARMOR_BODY = armor("livingarmorbody", AddonArmorMaterials.LIVING, ArmorItem.Type.CHESTPLATE, 600);
    static final DeferredItem<ArmorItem> LIVING_ARMOR_LEGS = armor("livingarmorlegs", AddonArmorMaterials.LIVING, ArmorItem.Type.LEGGINGS, 600);
    static final DeferredItem<ArmorItem> LIVING_ARMOR_BOOTS = armor("livingarmorboots", AddonArmorMaterials.LIVING, ArmorItem.Type.BOOTS, 600);
    static final DeferredItem<ArmorItem> ADAPTIVE_ARMOR_HELMET = armor("adaptivearmorhelmet", AddonArmorMaterials.ADAPTIVE, ArmorItem.Type.HELMET, 800);
    static final DeferredItem<ArmorItem> ADAPTIVE_ARMOR_BODY = armor("adaptivearmorbody", AddonArmorMaterials.ADAPTIVE, ArmorItem.Type.CHESTPLATE, 800);
    static final DeferredItem<ArmorItem> ADAPTIVE_ARMOR_LEGS = armor("adaptivearmorlegs", AddonArmorMaterials.ADAPTIVE, ArmorItem.Type.LEGGINGS, 800);
    static final DeferredItem<ArmorItem> ADAPTIVE_ARMOR_BOOTS = armor("adaptivearmorboots", AddonArmorMaterials.ADAPTIVE, ArmorItem.Type.BOOTS, 800);

    // Melee weapons preserve the old durability and approximate 1.12 attack values.
    static final DeferredItem<SwordItem> TWISTED_DAGGER = sword("twisteddagger", AddonTiers.TWISTED, 3, -1.8f, 800);
    static final DeferredItem<AxeItem> TWISTED_GREAT_AXE = axe("twistedgreataxe", AddonTiers.TWISTED, 4, -3.2f, 800);
    static final DeferredItem<SwordItem> TWISTED_MALLET = sword("twistedmallet", AddonTiers.TWISTED, 8, -3.5f, 800);
    static final DeferredItem<SwordItem> PESTILENT_KNIFE = sword("pestilentknife", AddonTiers.PESTILENT, 4, -1.8f, 2000);
    static final DeferredItem<SwordItem> PESTILENT_SCYTHE = sword("pestilentscythe", AddonTiers.PESTILENT, 5, -2.4f, 2000);
    static final DeferredItem<SwordItem> GORE_RAPIER = sword("gorerapier", AddonTiers.GORE, 7, -2.4f, 3000);
    static final DeferredItem<AxeItem> GORE_HATCHET = axe("gorehatchet", AddonTiers.GORE, 15, -3.2f, 3000);
    static final DeferredItem<SwordItem> CARAPACE_BROADSWORD = sword("carapacebroadsword", AddonTiers.CARAPACE, 6, -2.4f, 4000);
    static final DeferredItem<AxeItem> CARAPACE_SHELLBREAKER = axe("carapaceshellbreaker", AddonTiers.CARAPACE, 10, -3.2f, 4000);
    static final DeferredItem<SwordItem> EVOLUTION_KNIFE = sword("evolutionknife", AddonTiers.EVOLUTION, 16, -2.4f, 10000);
    static final DeferredItem<SwordItem> EVOLUTION_SICKLE = sword("evolutionsickle", AddonTiers.EVOLUTION, 22, -2.8f, 10000);
    static final DeferredItem<AxeItem> EVOLUTION_AXE = axe("evolutionaxe", AddonTiers.EVOLUTION, 18, -3.2f, 100);
    static final DeferredItem<SwordItem> MIMIC_BLADE_GREEN = sword("mimicbladegreen", AddonTiers.CARAPACE, 12, -2.4f, 8000);
    static final DeferredItem<SwordItem> MIMIC_BLADE_PURPLE = sword("mimicbladepurple", AddonTiers.CARAPACE, 12, -2.4f, 8000);
    static final DeferredItem<SwordItem> MIMIC_BLADE_RED = sword("mimicbladered", AddonTiers.CARAPACE, 12, -2.4f, 8000);
    static final DeferredItem<SwordItem> MIMIC_BLADE_YELLOW = sword("mimicbladeyellow", AddonTiers.CARAPACE, 12, -2.4f, 8000);
    static final DeferredItem<ArmoryLauncherItem> OVERLORD_BLADE = launcherMelee("overlordblade", ArmoryLauncherItem.Kind.OVERLORD_BLADE, 10000, 31.0F);
    static final DeferredItem<SwordItem> THE_REAPER = sword("thereaper", AddonTiers.OVERLORD, 96, -2.4f, 8000);
    static final DeferredItem<SwordItem> THE_REAPER_TRUE = sword("thereapertrue", AddonTiers.OVERLORD, 996, -2.4f, 8000);
    static final DeferredItem<SwordItem> BOLSTER_CLAW = sword("bolsterclaw", AddonTiers.TWISTED, 8, -2.4f, 5000);

    // Ranged weapons spawn the original custom arrow entities (entitybullet*).
    static final DeferredItem<ArmoryLauncherItem> TWISTED_BOW = launcher("twistedbow", ArmoryLauncherItem.Kind.TWISTED_BOW, 800);
    static final DeferredItem<ArmoryLauncherItem> GORE_COMBAT_BOW = launcherMelee("gorecombatbow", ArmoryLauncherItem.Kind.GORE_BOW, 4000, 8.0F);
    static final DeferredItem<ArmoryLauncherItem> EVOLUTION_BOW = launcherMelee("evolutionbow", ArmoryLauncherItem.Kind.EVOLUTION_BOW, 8000, 14.0F);
    static final DeferredItem<ArmoryLauncherItem> PESTILENT_MIASM = launcherMelee("pestilentmiasm", ArmoryLauncherItem.Kind.PESTILENT_MIASM, 2000, 5.0F);
    static final DeferredItem<ArmoryLauncherItem> PESTILENT_SHURIKEN = stackLauncher("pestilentshuriken", ArmoryLauncherItem.Kind.PESTILENT_SHURIKEN, 64);
    static final DeferredItem<ArmoryLauncherItem> FLAMETHROWER = launcher("flamethrower", ArmoryLauncherItem.Kind.FLAMETHROWER, 0);
    static final DeferredItem<ArmoryLauncherItem> INCINERATOR = launcher("incinerator", ArmoryLauncherItem.Kind.INCINERATOR, 0);
    static final DeferredItem<ArmoryLauncherItem> PLASMA_TORCH = launcher("plasmatorch", ArmoryLauncherItem.Kind.PLASMA_TORCH, 0);
    static final DeferredItem<ArmoryLauncherItem> HEAD_BOMB = stackLauncher("headbomb", ArmoryLauncherItem.Kind.HEAD_BOMB, 16);
    static final DeferredItem<ArmoryLauncherItem> HOST_BOMB = stackLauncher("hostbomb", ArmoryLauncherItem.Kind.HOST_BOMB, 64);
    static final DeferredItem<ArmoryLauncherItem> HOST_TENTACLE = launcherMelee("hosttentacle", ArmoryLauncherItem.Kind.HOST_TENTACLE, 5000, 11.0F);
    static final DeferredItem<ArmoryLauncherItem> TWISTED_BOMB = stackLauncher("twistedbomb", ArmoryLauncherItem.Kind.TWISTED_BOMB, 16);

    private static DeferredItem<Item> simple(String id) {
        return ITEMS.registerItem(id, Item::new, new Item.Properties());
    }
    private static DeferredItem<Item> stackable(String id, int count) {
        return ITEMS.registerItem(id, Item::new, new Item.Properties().stacksTo(count));
    }
    private static DeferredItem<ArmorItem> armor(String id, net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> material, ArmorItem.Type type, int durability) {
        return ITEMS.registerItem(id, p -> new ArmorItem(material, type, p.durability(type.getDurability(durability))), new Item.Properties());
    }
    private static DeferredItem<SwordItem> sword(String id, net.minecraft.world.item.Tier tier, float damage, float speed, int durability) {
        return ITEMS.registerItem(id, p -> new SwordItem(tier, p.attributes(SwordItem.createAttributes(tier, damage, speed))), new Item.Properties().durability(durability));
    }
    private static DeferredItem<AxeItem> axe(String id, net.minecraft.world.item.Tier tier, float damage, float speed, int durability) {
        return ITEMS.registerItem(id, p -> new AxeItem(tier, p.attributes(DiggerItem.createAttributes(tier, damage, speed))), new Item.Properties().durability(durability));
    }
    private static DeferredItem<ArmoryLauncherItem> launcher(String id, ArmoryLauncherItem.Kind kind, int durability) {
        return ITEMS.registerItem(id, p -> new ArmoryLauncherItem(kind,
                durability > 0 ? p.durability(durability) : p), new Item.Properties());
    }
    private static DeferredItem<ArmoryLauncherItem> launcherMelee(String id, ArmoryLauncherItem.Kind kind, int durability, float attackDamage) {
        return ITEMS.registerItem(id, p -> new ArmoryLauncherItem(kind, p.durability(durability), attackDamage), new Item.Properties());
    }
    private static DeferredItem<ArmoryLauncherItem> stackLauncher(String id, ArmoryLauncherItem.Kind kind, int count) {
        return ITEMS.registerItem(id, p -> new ArmoryLauncherItem(kind, p.stacksTo(count)), new Item.Properties());
    }
    static void all(CreativeModeTab.Output output) {
        ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
    }
    private AddonItems() {}
}
