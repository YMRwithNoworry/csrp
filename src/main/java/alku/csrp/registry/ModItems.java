package alku.csrp.registry;

import alku.csrp.Csrp;
import alku.csrp.item.AssimilationWandItem;
import alku.csrp.item.BoughItem;
import alku.csrp.item.BookOfVengeanceItem;
import alku.csrp.item.CompendiumItem;
import alku.csrp.item.DeadBloodFluidItem;
import alku.csrp.item.EvolutionClockItem;
import alku.csrp.item.EvolutionDeviceItem;
import alku.csrp.item.EvolutionLureItem;
import alku.csrp.block.EvolutionLureBlock;
import alku.csrp.item.FalseAppleItem;
import alku.csrp.item.FishlinItem;
import alku.csrp.item.HijackedArmorItem;
import alku.csrp.item.HijackedToolItem;
import alku.csrp.item.InfestedBonemealItem;
import alku.csrp.item.LivingArmorItem;
import alku.csrp.item.LivingBowItem;
import alku.csrp.item.LivingWeaponItem;
import alku.csrp.item.LivingMaulItem;
import alku.csrp.item.LevelClockItem;
import alku.csrp.item.ModuleComponentItem;
import alku.csrp.item.QuenchItem;
import alku.csrp.item.RelayModuleItem;
import alku.csrp.item.RelayReportItem;
import alku.csrp.item.ShrimpItem;
import alku.csrp.item.SrpCompassItem;
import alku.csrp.item.TexturedSpawnEggItem;
import alku.csrp.item.TheSignCharmItem;
import alku.csrp.item.ThornshadeBerryItem;
import alku.csrp.item.ThornshadeDecanterItem;
import alku.csrp.item.InjectedPurifierItem;
import alku.csrp.item.OverlastCanteenItem;
import alku.csrp.item.OverlastFoodItem;
import alku.csrp.item.ParasiteLootBlockItem;
import alku.csrp.item.ParasiteEvolutionWandItem;
import alku.csrp.item.AlveolarFluidItem;
import alku.csrp.item.AlveoliItem;
import alku.csrp.item.VenkrolBootsItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
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
            "buglin_spawn_egg", properties -> new TexturedSpawnEggItem(ModEntities.BUGLIN.get(), 0x8B1E1E, 0xE1B85B, properties),
            new Item.Properties());
    public static final DeferredItem<SpawnEggItem> RUPTER_SPAWN_EGG = ITEMS.registerItem(
            "rupter_spawn_egg", properties -> new TexturedSpawnEggItem(ModEntities.RUPTER.get(), 0x6E1717, 0xD8B45B, properties),
            new Item.Properties());
    public static final DeferredItem<SpawnEggItem> PRI_LONGARMS_SPAWN_EGG = spawnEgg(
            "pri_longarms_spawn_egg", ModEntities.PRI_LONGARMS, 0x551C1C, 0xC9A17B);
    public static final DeferredItem<SpawnEggItem> PRI_SUMMONER_SPAWN_EGG = spawnEgg(
            "pri_summoner_spawn_egg", ModEntities.PRI_SUMMONER, 0x321818, 0xA06D50);
    public static final DeferredItem<SpawnEggItem> PRI_VERMIN_SPAWN_EGG = spawnEgg(
            "pri_vermin_spawn_egg", ModEntities.PRI_VERMIN, 0x48151B, 0xD4B75C);
    public static final DeferredItem<SpawnEggItem> PRI_VISCERA_SPAWN_EGG = spawnEgg(
            "pri_viscera_spawn_egg", ModEntities.PRI_VISCERA, 0x421517, 0xA68B69);
    public static final DeferredItem<SpawnEggItem> PRI_ARACHNIDA_SPAWN_EGG = spawnEgg(
            "pri_arachnida_spawn_egg", ModEntities.PRI_ARACHNIDA, 0x50181E, 0xC58B74);
    public static final DeferredItem<SpawnEggItem> PRI_BOLSTER_SPAWN_EGG = spawnEgg(
            "pri_bolster_spawn_egg", ModEntities.PRI_BOLSTER, 0x36191D, 0x9B6C5F);
    public static final DeferredItem<SpawnEggItem> PRI_BURROWER_SPAWN_EGG = spawnEgg(
            "pri_burrower_spawn_egg", ModEntities.PRI_BURROWER, 0x501D17, 0xC28E51);
    public static final DeferredItem<SpawnEggItem> PRI_DEVOURER_SPAWN_EGG = spawnEgg(
            "pri_devourer_spawn_egg", ModEntities.PRI_DEVOURER, 0x36161A, 0xA75F58);
    public static final DeferredItem<SpawnEggItem> PRI_MANDUCATER_SPAWN_EGG = spawnEgg(
            "pri_manducater_spawn_egg", ModEntities.PRI_MANDUCATER, 0x48241F, 0xBC855B);
    public static final DeferredItem<SpawnEggItem> PRI_REEKER_SPAWN_EGG = spawnEgg(
            "pri_reeker_spawn_egg", ModEntities.PRI_REEKER, 0x34241D, 0x8C9A63);
    public static final DeferredItem<SpawnEggItem> PRI_TOZOON_SPAWN_EGG = spawnEgg(
            "pri_tozoon_spawn_egg", ModEntities.PRI_TOZOON, 0x43272A, 0xB47864);
    public static final DeferredItem<SpawnEggItem> PRI_YELLOWEYE_SPAWN_EGG = spawnEgg(
            "pri_yelloweye_spawn_egg", ModEntities.PRI_YELLOWEYE, 0x544420, 0xD8C343);
    public static final DeferredItem<SpawnEggItem> ADA_ARACHNIDA_SPAWN_EGG = spawnEgg(
            "ada_arachnida_spawn_egg", ModEntities.ADA_ARACHNIDA, 0x5A1E1D, 0xE3B567);
    public static final DeferredItem<SpawnEggItem> ADA_BOLSTER_SPAWN_EGG = spawnEgg(
            "ada_bolster_spawn_egg", ModEntities.ADA_BOLSTER, 0x3A2021, 0xB77162);
    public static final DeferredItem<SpawnEggItem> ADA_BURROWER_SPAWN_EGG = spawnEgg(
            "ada_burrower_spawn_egg", ModEntities.ADA_BURROWER, 0x53271B, 0xD38D55);
    public static final DeferredItem<SpawnEggItem> ADA_DEVOURER_SPAWN_EGG = spawnEgg(
            "ada_devourer_spawn_egg", ModEntities.ADA_DEVOURER, 0x29404A, 0x8DB9A9);
    public static final DeferredItem<SpawnEggItem> ADA_LONGARMS_SPAWN_EGG = spawnEgg(
            "ada_longarms_spawn_egg", ModEntities.ADA_LONGARMS, 0x54201E, 0xD47C59);
    public static final DeferredItem<SpawnEggItem> ADA_MANDUCATER_SPAWN_EGG = spawnEgg(
            "ada_manducater_spawn_egg", ModEntities.ADA_MANDUCATER, 0x421F29, 0xC26582);
    public static final DeferredItem<SpawnEggItem> ADA_REEKER_SPAWN_EGG = spawnEgg(
            "ada_reeker_spawn_egg", ModEntities.ADA_REEKER, 0x3A281F, 0x95A85A);
    public static final DeferredItem<SpawnEggItem> ADA_SUMMONER_SPAWN_EGG = spawnEgg(
            "ada_summoner_spawn_egg", ModEntities.ADA_SUMMONER, 0x45202A, 0xB9679C);
    public static final DeferredItem<SpawnEggItem> ADA_TOZOON_SPAWN_EGG = spawnEgg(
            "ada_tozoon_spawn_egg", ModEntities.ADA_TOZOON, 0x412F24, 0xA48C70);
    public static final DeferredItem<SpawnEggItem> ADA_VERMIN_SPAWN_EGG = spawnEgg(
            "ada_vermin_spawn_egg", ModEntities.ADA_VERMIN, 0x311E36, 0xA370C2);
    public static final DeferredItem<SpawnEggItem> ADA_VISCERA_SPAWN_EGG = spawnEgg(
            "ada_viscera_spawn_egg", ModEntities.ADA_VISCERA, 0x51231E, 0xC78662);
    public static final DeferredItem<SpawnEggItem> ADA_YELLOWEYE_SPAWN_EGG = spawnEgg(
            "ada_yelloweye_spawn_egg", ModEntities.ADA_YELLOWEYE, 0x5A4219, 0xE2D45B);
    public static final DeferredItem<SpawnEggItem> GNAT_SPAWN_EGG = spawnEgg(
            "gnat_spawn_egg", ModEntities.GNAT, 0x4B1717, 0xB7A277);
    public static final DeferredItem<SpawnEggItem> CARRIER_HEAVY_SPAWN_EGG = spawnEgg(
            "carrier_heavy_spawn_egg", ModEntities.CARRIER_HEAVY, 0x552525, 0xBE9D60);
    public static final DeferredItem<SpawnEggItem> CARRIER_LIGHT_SPAWN_EGG = spawnEgg(
            "carrier_light_spawn_egg", ModEntities.CARRIER_LIGHT, 0x6B2D27, 0xD0AE6C);
    public static final DeferredItem<SpawnEggItem> CARRIER_FLYING_SPAWN_EGG = spawnEgg(
            "carrier_flying_spawn_egg", ModEntities.CARRIER_FLYING, 0x4A2025, 0xB98555);
    public static final DeferredItem<SpawnEggItem> CRUX_SPAWN_EGG = spawnEgg(
            "crux_spawn_egg", ModEntities.CRUX, 0x7F4000, 0xB70000);
    public static final DeferredItem<SpawnEggItem> CRUX_INCOMPLETE_SPAWN_EGG = spawnEgg(
            "crux_incomplete_spawn_egg", ModEntities.CRUX_INCOMPLETE, 0x7F4000, 0xB70000);
    public static final DeferredItem<SpawnEggItem> AIRSCREW_SPAWN_EGG = spawnEgg(
            "airscrew_spawn_egg", ModEntities.AIRSCREW, 0x7F4000, 0xB70000);
    public static final DeferredItem<SpawnEggItem> HEED_SPAWN_EGG = spawnEgg(
            "heed_spawn_egg", ModEntities.HEED, 0x7F6B80, 0x404040);
    public static final DeferredItem<SpawnEggItem> DREDGE_SPAWN_EGG = spawnEgg(
            "dredge_spawn_egg", ModEntities.DREDGE, 0x7F4000, 0xB70000);
    public static final DeferredItem<SpawnEggItem> THRALL_SPAWN_EGG = spawnEgg(
            "thrall_spawn_egg", ModEntities.THRALL, 0x7F4000, 0xB70000);
    public static final DeferredItem<SpawnEggItem> LICE_SPAWN_EGG = spawnEgg(
            "lice_spawn_egg", ModEntities.LICE, 0x4A171A, 0xD8A94E);
    public static final DeferredItem<SpawnEggItem> MANGLER_SPAWN_EGG = spawnEgg(
            "mangler_spawn_egg", ModEntities.MANGLER, 0x5A1E1E, 0xBA8D55);
    public static final DeferredItem<SpawnEggItem> HOST_SPAWN_EGG = spawnEgg(
            "host_spawn_egg", ModEntities.HOST, 0x5A3200, 0xFF00DC);
    public static final DeferredItem<SpawnEggItem> HOSTII_SPAWN_EGG = spawnEgg(
            "hostii_spawn_egg", ModEntities.HOSTII, 0x836000, 0xFF00DC);
    public static final DeferredItem<SpawnEggItem> INCOMPLETEFORM_SMALL_SPAWN_EGG = spawnEgg(
            "incompleteform_small_spawn_egg", ModEntities.INCOMPLETEFORM_SMALL, 0x641A1A, 0xA98B68);
    public static final DeferredItem<SpawnEggItem> INCOMPLETEFORM_MEDIUM_SPAWN_EGG = spawnEgg(
            "incompleteform_medium_spawn_egg", ModEntities.INCOMPLETEFORM_MEDIUM, 0x501313, 0xB99B74);
    public static final DeferredItem<SpawnEggItem> DRACONITE_SPAWN_EGG = spawnEgg(
            "draconite_spawn_egg", ModEntities.DRACONITE, 0x241414, 0xB80000);
    public static final DeferredItem<SpawnEggItem> KIRIN_SPAWN_EGG = spawnEgg(
            "kirin_spawn_egg", ModEntities.KIRIN, 0x17121F, 0xB650D8);
    public static final DeferredItem<SpawnEggItem> SIM_ADVENTURER_SPAWN_EGG = spawnEgg(
            "sim_adventurer_spawn_egg", ModEntities.SIM_ADVENTURER, 0x5E392D, 0xC58B68);
    public static final DeferredItem<SpawnEggItem> SIM_ADVENTURER_HEAD_SPAWN_EGG = spawnEgg(
            "sim_adventurerhead_spawn_egg", ModEntities.SIM_ADVENTURER_HEAD, 0x5A3228, 0xC48A68);
    public static final DeferredItem<SpawnEggItem> MOVING_FLESH_SPAWN_EGG = spawnEgg(
            "movingflesh_spawn_egg", ModEntities.MOVINGFLESH, 0x59201E, 0xB66B57);
    public static final DeferredItem<SpawnEggItem> SIM_BEAR_SPAWN_EGG = spawnEgg(
            "sim_bear_spawn_egg", ModEntities.SIM_BEAR, 0x3A211C, 0xA24A3C);
    public static final DeferredItem<SpawnEggItem> SIM_COW_SPAWN_EGG = spawnEgg(
            "sim_cow_spawn_egg", ModEntities.SIM_COW, 0x4A201D, 0xB24E3B);
    public static final DeferredItem<SpawnEggItem> SIM_PIG_SPAWN_EGG = spawnEgg(
            "sim_pig_spawn_egg", ModEntities.SIM_PIG, 0xB24F55, 0x52211D);
    public static final DeferredItem<SpawnEggItem> SIM_SHEEP_SPAWN_EGG = spawnEgg(
            "sim_sheep_spawn_egg", ModEntities.SIM_SHEEP, 0xD9D1C0, 0x732E2E);
    public static final DeferredItem<SpawnEggItem> SIM_WOLF_SPAWN_EGG = spawnEgg(
            "sim_wolf_spawn_egg", ModEntities.SIM_WOLF, 0x54545A, 0xA44137);
    public static final DeferredItem<SpawnEggItem> SIM_SQUID_SPAWN_EGG = spawnEgg(
            "sim_squid_spawn_egg", ModEntities.SIM_SQUID, 0x263A4D, 0x9E4254);
    public static final DeferredItem<SpawnEggItem> SIM_BIGSPIDER_SPAWN_EGG = spawnEgg(
            "sim_bigspider_spawn_egg", ModEntities.SIM_BIGSPIDER, 0x341B20, 0x9C4E58);
    public static final DeferredItem<SpawnEggItem> SIM_DRAGONE_SPAWN_EGG = spawnEgg(
            "sim_dragone_spawn_egg", ModEntities.SIM_DRAGONE, 0x231C2B, 0x9B4050);
    public static final DeferredItem<SpawnEggItem> SIM_DRAGONHEAD_SPAWN_EGG = spawnEgg(
            "sim_dragonhead_spawn_egg", ModEntities.SIM_DRAGON_HEAD, 0x2D1E2B, 0xC05A66);
    public static final DeferredItem<SpawnEggItem> SIM_ENDERMAN_SPAWN_EGG = spawnEgg(
            "sim_enderman_spawn_egg", ModEntities.SIM_ENDERMAN, 0x20182B, 0x7A355D);
    public static final DeferredItem<SpawnEggItem> SIM_ENDERMANHEAD_SPAWN_EGG = spawnEgg(
            "sim_endermanhead_spawn_egg", ModEntities.SIM_ENDERMAN_HEAD, 0x291B31, 0x9B4D7D);
    public static final DeferredItem<SpawnEggItem> SIM_HORSE_SPAWN_EGG = spawnEgg(
            "sim_horse_spawn_egg", ModEntities.SIM_HORSE, 0x4F2B20, 0xB75A3E);
    public static final DeferredItem<SpawnEggItem> SIM_HORSEHEAD_SPAWN_EGG = spawnEgg(
            "sim_horsehead_spawn_egg", ModEntities.SIM_HORSE_HEAD, 0x593120, 0xCE6A49);
    public static final DeferredItem<SpawnEggItem> SIM_HUMAN_SPAWN_EGG = spawnEgg(
            "sim_human_spawn_egg", ModEntities.SIM_HUMAN, 0x49302A, 0xB97356);
    public static final DeferredItem<SpawnEggItem> SIM_HUMANHEAD_SPAWN_EGG = spawnEgg(
            "sim_humanhead_spawn_egg", ModEntities.SIM_HUMAN_HEAD, 0x59352D, 0xD48662);
    public static final DeferredItem<SpawnEggItem> SIM_COWHEAD_SPAWN_EGG = spawnEgg(
            "sim_cowhead_spawn_egg", ModEntities.SIM_COW_HEAD, 0x4A201D, 0xB24E3B);
    public static final DeferredItem<SpawnEggItem> SIM_PIGHEAD_SPAWN_EGG = spawnEgg(
            "sim_pighead_spawn_egg", ModEntities.SIM_PIG_HEAD, 0xB24F55, 0x52211D);
    public static final DeferredItem<SpawnEggItem> SIM_SHEEPHEAD_SPAWN_EGG = spawnEgg(
            "sim_sheephead_spawn_egg", ModEntities.SIM_SHEEP_HEAD, 0xD9D1C0, 0x732E2E);
    public static final DeferredItem<SpawnEggItem> SIM_VILLAGER_SPAWN_EGG = spawnEgg(
            "sim_villager_spawn_egg", ModEntities.SIM_VILLAGER, 0x4D3026, 0xA7523E);
    public static final DeferredItem<SpawnEggItem> SIM_VILLAGERHEAD_SPAWN_EGG = spawnEgg(
            "sim_villagerhead_spawn_egg", ModEntities.SIM_VILLAGER_HEAD, 0x5A3529, 0xC96A4F);
    public static final DeferredItem<SpawnEggItem> SIM_WOLFHEAD_SPAWN_EGG = spawnEgg(
            "sim_wolfhead_spawn_egg", ModEntities.SIM_WOLF_HEAD, 0x54545A, 0xA44137);
    public static final DeferredItem<SpawnEggItem> FER_BEAR_SPAWN_EGG = spawnEgg(
            "fer_bear_spawn_egg", ModEntities.FER_BEAR, 0x5A2A20, 0xD86B44);
    public static final DeferredItem<SpawnEggItem> FER_COW_SPAWN_EGG = spawnEgg(
            "fer_cow_spawn_egg", ModEntities.FER_COW, 0x54231F, 0xC1583E);
    public static final DeferredItem<SpawnEggItem> FER_HORSE_SPAWN_EGG = spawnEgg(
            "fer_horse_spawn_egg", ModEntities.FER_HORSE, 0x836500, 0xFF00DC);
    public static final DeferredItem<SpawnEggItem> FER_HUMAN_SPAWN_EGG = spawnEgg(
            "fer_human_spawn_egg", ModEntities.FER_HUMAN, 0x836500, 0xFF00DC);
    public static final DeferredItem<SpawnEggItem> FER_PIG_SPAWN_EGG = spawnEgg(
            "fer_pig_spawn_egg", ModEntities.FER_PIG, 0x6E2527, 0xD76448);
    public static final DeferredItem<SpawnEggItem> FER_SHEEP_SPAWN_EGG = spawnEgg(
            "fer_sheep_spawn_egg", ModEntities.FER_SHEEP, 0x6B514A, 0xD26C46);
    public static final DeferredItem<SpawnEggItem> FER_VILLAGER_SPAWN_EGG = spawnEgg(
            "fer_villager_spawn_egg", ModEntities.FER_VILLAGER, 0x836500, 0xFF00DC);
    public static final DeferredItem<SpawnEggItem> FER_WOLF_SPAWN_EGG = spawnEgg(
            "fer_wolf_spawn_egg", ModEntities.FER_WOLF, 0x4D3535, 0xCA573D);
    public static final DeferredItem<SpawnEggItem> FER_ENDERMAN_SPAWN_EGG = spawnEgg(
            "fer_enderman_spawn_egg", ModEntities.FER_ENDERMAN, 0x20182B, 0x7A355D);
    public static final DeferredItem<SpawnEggItem> HI_BLAZE_SPAWN_EGG = spawnEgg(
            "hi_blaze_spawn_egg", ModEntities.HI_BLAZE, 0x5E201E, 0xD77B32);
    public static final DeferredItem<SpawnEggItem> HI_GOLEM_SPAWN_EGG = spawnEgg(
            "hi_golem_spawn_egg", ModEntities.HI_GOLEM, 0x362421, 0xB86549);
    public static final DeferredItem<SpawnEggItem> HI_SKELETON_SPAWN_EGG = spawnEgg(
            "hi_skeleton_spawn_egg", ModEntities.HI_SKELETON, 0x463D38, 0xC7A17C);
    public static final DeferredItem<SpawnEggItem> MAR_BEAR_SPAWN_EGG = spawnEgg(
            "mar_bear_spawn_egg", ModEntities.MAR_BEAR, 0x5A2A20, 0xD86B44);
    public static final DeferredItem<SpawnEggItem> MAR_COW_SPAWN_EGG = spawnEgg(
            "mar_cow_spawn_egg", ModEntities.MAR_COW, 0x54231F, 0xC1583E);
    public static final DeferredItem<SpawnEggItem> MAR_ENDERMAN_SPAWN_EGG = spawnEgg(
            "mar_enderman_spawn_egg", ModEntities.MAR_ENDERMAN, 0x20182B, 0x7A355D);
    public static final DeferredItem<SpawnEggItem> MAR_HUMAN_SPAWN_EGG = spawnEgg(
            "mar_human_spawn_egg", ModEntities.MAR_HUMAN, 0x836500, 0xFF00DC);
    public static final DeferredItem<SpawnEggItem> MAR_SHEEP_SPAWN_EGG = spawnEgg(
            "mar_sheep_spawn_egg", ModEntities.MAR_SHEEP, 0x6B514A, 0xD26C46);
    public static final DeferredItem<SpawnEggItem> MAR_VILLAGER_SPAWN_EGG = spawnEgg(
            "mar_villager_spawn_egg", ModEntities.MAR_VILLAGER, 0x836500, 0xFF00DC);
    public static final DeferredItem<SpawnEggItem> MARAUDER_SPAWN_EGG = spawnEgg(
            "marauder_spawn_egg", ModEntities.MARAUDER, 0x2F1111, 0xB64D32);
    public static final DeferredItem<SpawnEggItem> DISPATCHERTEN_SPAWN_EGG = spawnEgg(
            "dispatcherten_spawn_egg", ModEntities.DISPATCHERTEN, 0x313A2B, 0xA6B986);
    public static final DeferredItem<SpawnEggItem> KYPHOSIS_SPAWN_EGG = spawnEgg(
            "kyphosis_spawn_egg", ModEntities.KYPHOSIS, 0x322C29, 0xB18465);
    public static final DeferredItem<SpawnEggItem> SEIZER_SPAWN_EGG = spawnEgg(
            "seizer_spawn_egg", ModEntities.SEIZER, 0x2E352B, 0xA8C079);
    public static final DeferredItem<SpawnEggItem> SENTRY_SPAWN_EGG = spawnEgg(
            "sentry_spawn_egg", ModEntities.SENTRY, 0x27322B, 0x8CB879);
    public static final DeferredItem<SpawnEggItem> WORM_SPAWN_EGG = spawnEgg(
            "worm_spawn_egg", ModEntities.WORM, 0x392E24, 0xB68658);
    public static final DeferredItem<SpawnEggItem> GRUNT_SPAWN_EGG = spawnEgg(
            "grunt_spawn_egg", ModEntities.GRUNT, 0x342E28, 0xB76648);
    public static final DeferredItem<SpawnEggItem> BOMBER_LIGHT_SPAWN_EGG = spawnEgg(
            "bomber_light_spawn_egg", ModEntities.BOMBER_LIGHT, 0x343834, 0xC7B56E);
    public static final DeferredItem<SpawnEggItem> MONARCH_SPAWN_EGG = spawnEgg(
            "monarch_spawn_egg", ModEntities.MONARCH, 0x3A332D, 0xB77B50);
    public static final DeferredItem<SpawnEggItem> OVERSEER_SPAWN_EGG = spawnEgg(
            "overseer_spawn_egg", ModEntities.OVERSEER, 0x343B31, 0xC2A570);
    public static final DeferredItem<SpawnEggItem> VIGILANTE_SPAWN_EGG = spawnEgg(
            "vigilante_spawn_egg", ModEntities.VIGILANTE, 0x39342D, 0xA58A61);
    public static final DeferredItem<SpawnEggItem> WARDEN_SPAWN_EGG = spawnEgg(
            "warden_spawn_egg", ModEntities.WARDEN, 0x302B29, 0xB26B4D);
    public static final DeferredItem<SpawnEggItem> BOGLE_SPAWN_EGG = spawnEgg(
            "bogle_spawn_egg", ModEntities.BOGLE, 0x27312E, 0x6A98A3);
    public static final DeferredItem<SpawnEggItem> CARRIER_COLONY_SPAWN_EGG = spawnEgg(
            "carrier_colony_spawn_egg", ModEntities.CARRIER_COLONY, 0x483D36, 0xC08E5B);
    public static final DeferredItem<SpawnEggItem> HAUNTER_SPAWN_EGG = spawnEgg(
            "haunter_spawn_egg", ModEntities.HAUNTER, 0x3A3332, 0xBC6E55);
    public static final DeferredItem<SpawnEggItem> BOMBER_HEAVY_SPAWN_EGG = spawnEgg(
            "bomber_heavy_spawn_egg", ModEntities.BOMBER_HEAVY, 0x3B3B2D, 0xC8B04D);
    public static final DeferredItem<SpawnEggItem> WRAITH_SPAWN_EGG = spawnEgg(
            "wraith_spawn_egg", ModEntities.WRAITH, 0x222A3B, 0x8B9DD2);
    public static final DeferredItem<SpawnEggItem> SUCCOR_SPAWN_EGG = spawnEgg(
            "succor_spawn_egg", ModEntities.SUCCOR, 0x313442, 0xA0B4D8);
    public static final DeferredItem<SpawnEggItem> ANC_DREADNAUT_SPAWN_EGG = spawnEgg(
            "anc_dreadnaut_spawn_egg", ModEntities.ANC_DREADNAUT, 0x283038, 0xAF6752);
    public static final DeferredItem<SpawnEggItem> ANC_OVERLORD_SPAWN_EGG = spawnEgg(
            "anc_overlord_spawn_egg", ModEntities.ANC_OVERLORD, 0x3A2E2E, 0xB37457);
    public static final DeferredItem<SpawnEggItem> WORKER_SPAWN_EGG = spawnEgg(
            "worker_spawn_egg", ModEntities.WORKER, 0x34251F, 0x9B6748);
    public static final DeferredItem<SpawnEggItem> ARCHITECT_SPAWN_EGG = spawnEgg(
            "architect_spawn_egg", ModEntities.ARCHITECT, 0x332B2B, 0xAD7960);
    public static final DeferredItem<SpawnEggItem> ANC_POD_SPAWN_EGG = spawnEgg(
            "anc_pod_spawn_egg", ModEntities.ANC_POD, 0x283038, 0x7C5145);
    public static final DeferredItem<SpawnEggItem> ANC_DREADNAUT_TEN_SPAWN_EGG = spawnEgg(
            "anc_dreadnaut_ten_spawn_egg", ModEntities.ANC_DREADNAUT_TEN, 0x242B31, 0x8E5546);
    public static final DeferredItem<SpawnEggItem> BECKON_SI_SPAWN_EGG = spawnEgg(
            "beckon_si_spawn_egg", ModEntities.BECKON_SI, 0x263526, 0x7A9E57);
    public static final DeferredItem<SpawnEggItem> BECKON_SII_SPAWN_EGG = spawnEgg(
            "beckon_sii_spawn_egg", ModEntities.BECKON_SII, 0x263526, 0x82A65D);
    public static final DeferredItem<SpawnEggItem> BECKON_SIII_SPAWN_EGG = spawnEgg(
            "beckon_siii_spawn_egg", ModEntities.BECKON_SIII, 0x263526, 0x8DAA60);
    public static final DeferredItem<SpawnEggItem> BECKON_SIV_SPAWN_EGG = spawnEgg(
            "beckon_siv_spawn_egg", ModEntities.BECKON_SIV, 0x263526, 0x9CB96A);
    public static final DeferredItem<SpawnEggItem> DISPATCHER_SI_SPAWN_EGG = spawnEgg(
            "dispatcher_si_spawn_egg", ModEntities.DISPATCHER_SI, 0x30372D, 0x91A968);
    public static final DeferredItem<SpawnEggItem> DISPATCHER_SII_SPAWN_EGG = spawnEgg(
            "dispatcher_sii_spawn_egg", ModEntities.DISPATCHER_SII, 0x30372D, 0x9CB371);
    public static final DeferredItem<SpawnEggItem> DISPATCHER_SIII_SPAWN_EGG = spawnEgg(
            "dispatcher_siii_spawn_egg", ModEntities.DISPATCHER_SIII, 0x30372D, 0xA9BB76);
    public static final DeferredItem<SpawnEggItem> DISPATCHER_SIV_SPAWN_EGG = spawnEgg(
            "dispatcher_siv_spawn_egg", ModEntities.DISPATCHER_SIV, 0x30372D, 0xB9CA82);
    public static final DeferredItem<SpawnEggItem> ROOTER_SI_SPAWN_EGG = spawnEgg(
            "rooter_si_spawn_egg", ModEntities.ROOTER_SI, 0x293426, 0x78965B);
    public static final DeferredItem<SpawnEggItem> ROOTER_SII_SPAWN_EGG = spawnEgg(
            "rooter_sii_spawn_egg", ModEntities.ROOTER_SII, 0x293426, 0x84A566);
    public static final DeferredItem<SpawnEggItem> ROOTER_SIII_SPAWN_EGG = spawnEgg(
            "rooter_siii_spawn_egg", ModEntities.ROOTER_SIII, 0x293426, 0x91B16F);
    public static final DeferredItem<SpawnEggItem> ROOTER_SIV_SPAWN_EGG = spawnEgg(
            "rooter_siv_spawn_egg", ModEntities.ROOTER_SIV, 0x293426, 0xA2C47D);
    public static final DeferredItem<SpawnEggItem> ROOTERBALL_SPAWN_EGG = spawnEgg(
            "rooterball_spawn_egg", ModEntities.ROOTERBALL, 0x30402D, 0xB5D483);
    public static final DeferredItem<SpawnEggItem> ABO_BODIES_SPAWN_EGG = spawnEgg(
            "abo_bodies_spawn_egg", ModEntities.ABO_BODIES, 0x4A2D2C, 0xB86C59);
    public static final DeferredItem<SpawnEggItem> ABO_HEAD_SPAWN_EGG = spawnEgg(
            "abo_head_spawn_egg", ModEntities.ABO_HEAD, 0x45322E, 0xB97D61);
    public static final DeferredItem<Item> RUPTER_VISCERA = simple("rupter_viscera");
    public static final DeferredItem<BlockItem> TUNNEL = ITEMS.registerSimpleBlockItem("tunnel", ModBlocks.TUNNEL);
    public static final DeferredItem<BlockItem> SRP_WEB = ITEMS.registerSimpleBlockItem("srpweb", ModBlocks.SRP_WEB);
    public static final DeferredItem<BlockItem> DISPATCHER_NIDUS = ITEMS.registerSimpleBlockItem(
            "dispatcher_nidus", ModBlocks.DISPATCHER_NIDUS);
    public static final DeferredItem<BlockItem> GLUTTONOUS_CYST = ITEMS.registerSimpleBlockItem(
            "gluttonous_cyst", ModBlocks.GLUTTONOUS_CYST);
    public static final DeferredItem<BlockItem> VACUOUS_CYST = ITEMS.registerSimpleBlockItem(
            "vacuous_cyst", ModBlocks.VACUOUS_CYST);
    public static final DeferredItem<BlockItem> RESIDUE_PLANTS = ITEMS.registerSimpleBlockItem(
            "residue_plants", ModBlocks.RESIDUE_PLANTS);
    public static final DeferredItem<BlockItem> THORNSHADE_ITEM = ITEMS.registerSimpleBlockItem(
            "thornshade", ModBlocks.THORNSHADE);
    public static final DeferredItem<BlockItem> RESIDUE_BLOCK = ITEMS.registerSimpleBlockItem(
            "residue_block", ModBlocks.RESIDUE_BLOCK);
    public static final DeferredItem<BlockItem> INFESTED_REMAINS = ITEMS.registerSimpleBlockItem(
            "infestremain", ModBlocks.INFESTED_REMAINS);
    public static final DeferredItem<BlockItem> BIOMASS_BLOCK = ITEMS.registerSimpleBlockItem(
            "biomass_block", ModBlocks.BIOMASS_BLOCK);
    public static final DeferredItem<BlockItem> PARASITE_MOUTH = ITEMS.registerSimpleBlockItem(
            "parasitemouth", ModBlocks.PARASITE_MOUTH);
    public static final DeferredItem<BlockItem> HIVESTONE_DEBRIS = ITEMS.registerSimpleBlockItem(
            "parasiterubble_stonedebris", ModBlocks.HIVESTONE_DEBRIS);
    public static final DeferredItem<BlockItem> INFESTED_STAIN = ITEMS.registerSimpleBlockItem(
            "infestedstain", ModBlocks.INFESTED_STAIN);
    public static final DeferredItem<BlockItem> INFESTED_RUBBLE = ITEMS.registerSimpleBlockItem(
            "infestedrubble", ModBlocks.INFESTED_RUBBLE);
    public static final DeferredItem<BlockItem> INFESTED_SAND = ITEMS.registerSimpleBlockItem(
            "infestedsand", ModBlocks.INFESTED_SAND);
    public static final DeferredItem<BlockItem> INFESTED_COBBLESTONE = ITEMS.registerSimpleBlockItem(
            "infested_cobblestone", ModBlocks.INFESTED_COBBLESTONE);
    public static final DeferredItem<BlockItem> INFESTED_TRUNK = ITEMS.registerSimpleBlockItem(
            "infestedtrunk", ModBlocks.INFESTED_TRUNK);
    public static final DeferredItem<BlockItem> INFESTED_PLANKS = ITEMS.registerSimpleBlockItem(
            "infested_planks", ModBlocks.INFESTED_PLANKS);
    public static final DeferredItem<BlockItem> INFESTED_STONE_BRICKS = ITEMS.registerSimpleBlockItem(
            "infested_stone_bricks", ModBlocks.INFESTED_STONE_BRICKS);
    public static final DeferredItem<BlockItem> INFESTED_TERRACOTTA = ITEMS.registerSimpleBlockItem(
            "infested_terracotta", ModBlocks.INFESTED_TERRACOTTA);
    public static final DeferredItem<BlockItem> POLISHED_INFESTED_STONE = ITEMS.registerSimpleBlockItem(
            "infested_stone_polished", ModBlocks.POLISHED_INFESTED_STONE);
    public static final DeferredItem<BlockItem> RESIDUE_BRICKS = ITEMS.registerSimpleBlockItem(
            "residue_bricks", ModBlocks.RESIDUE_BRICKS);
    public static final DeferredItem<BlockItem> INFESTED_COLUMN = ITEMS.registerSimpleBlockItem(
            "infested_column", ModBlocks.INFESTED_COLUMN);
    public static final DeferredItem<BlockItem> INFESTED_SANDSTONE = ITEMS.registerSimpleBlockItem(
            "inf_ss", ModBlocks.INFESTED_SANDSTONE);
    public static final DeferredItem<BlockItem> CHISELED_INFESTED_SANDSTONE = ITEMS.registerSimpleBlockItem(
            "inf_ss_chiseled", ModBlocks.CHISELED_INFESTED_SANDSTONE);
    public static final DeferredItem<BlockItem> CUT_INFESTED_SANDSTONE = ITEMS.registerSimpleBlockItem(
            "inf_ss_cut", ModBlocks.CUT_INFESTED_SANDSTONE);
    public static final DeferredItem<BlockItem> INFESTED_COBBLESTONE_SLAB = ITEMS.registerSimpleBlockItem(
            "infested_cobblestone_slab", ModBlocks.INFESTED_COBBLESTONE_SLAB);
    public static final DeferredItem<BlockItem> INFESTED_STONE_SLAB = ITEMS.registerSimpleBlockItem(
            "infested_stone_slab", ModBlocks.INFESTED_STONE_SLAB);
    public static final DeferredItem<BlockItem> INFESTED_DIRT_SLAB = ITEMS.registerSimpleBlockItem(
            "infested_dirt_slab", ModBlocks.INFESTED_DIRT_SLAB);
    public static final DeferredItem<BlockItem> INFESTED_STONE_BRICK_SLAB = ITEMS.registerSimpleBlockItem(
            "infested_stone_brick_slab", ModBlocks.INFESTED_STONE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> INFESTED_TERRACOTTA_SLAB = ITEMS.registerSimpleBlockItem(
            "infested_terracotta_slab", ModBlocks.INFESTED_TERRACOTTA_SLAB);
    public static final DeferredItem<BlockItem> POLISHED_INFESTED_STONE_SLAB = ITEMS.registerSimpleBlockItem(
            "polished_infested_stone_slab", ModBlocks.POLISHED_INFESTED_STONE_SLAB);
    public static final DeferredItem<BlockItem> RESIDUE_BRICK_SLAB = ITEMS.registerSimpleBlockItem(
            "residue_brick_slab", ModBlocks.RESIDUE_BRICK_SLAB);
    public static final DeferredItem<BlockItem> INFESTED_SANDSTONE_SLAB = ITEMS.registerSimpleBlockItem(
            "infested_sandstone_slab", ModBlocks.INFESTED_SANDSTONE_SLAB);
    public static final DeferredItem<BlockItem> INFESTED_PLANK_SLAB = ITEMS.registerSimpleBlockItem(
            "infested_plank_slab", ModBlocks.INFESTED_PLANK_SLAB);
    public static final DeferredItem<BlockItem> INFESTED_SANDSTONE_STAIRS = ITEMS.registerSimpleBlockItem(
            "infested_sandstone_stairs", ModBlocks.INFESTED_SANDSTONE_STAIRS);
    public static final DeferredItem<BlockItem> RESIDUE_STAIRS = ITEMS.registerSimpleBlockItem(
            "residue_stairs", ModBlocks.RESIDUE_STAIRS);
    public static final DeferredItem<BlockItem> INFESTED_PLANKS_STAIRS = ITEMS.registerSimpleBlockItem(
            "infested_planks_stairs", ModBlocks.INFESTED_PLANKS_STAIRS);
    public static final DeferredItem<BlockItem> INFESTED_STONE_BRICKS_STAIRS = ITEMS.registerSimpleBlockItem(
            "infested_stone_bricks_stairs", ModBlocks.INFESTED_STONE_BRICKS_STAIRS);
    public static final DeferredItem<BlockItem> INFESTED_POLISHED_STONE_BRICKS_STAIRS = ITEMS.registerSimpleBlockItem(
            "infested_polished_stone_bricks_stairs", ModBlocks.INFESTED_POLISHED_STONE_BRICKS_STAIRS);
    public static final DeferredItem<BlockItem> INFESTED_STONE_STAIRS = ITEMS.registerSimpleBlockItem(
            "infested_stone_stairs", ModBlocks.INFESTED_STONE_STAIRS);
    public static final DeferredItem<BlockItem> RESIDUE_WALL = ITEMS.registerSimpleBlockItem(
            "residue_wall", ModBlocks.RESIDUE_WALL);
    public static final DeferredItem<BlockItem> INFESTED_PLANK_WALL = ITEMS.registerSimpleBlockItem(
            "infested_plank_wall", ModBlocks.INFESTED_PLANK_WALL);
    public static final DeferredItem<BlockItem> POLISHED_INFESTED_STONE_WALL = ITEMS.registerSimpleBlockItem(
            "polished_infested_stone_wall", ModBlocks.POLISHED_INFESTED_STONE_WALL);
    public static final DeferredItem<BlockItem> INFESTED_STONE_BRICK_WALL = ITEMS.registerSimpleBlockItem(
            "infested_stone_brick_wall", ModBlocks.INFESTED_STONE_BRICK_WALL);
    public static final DeferredItem<BlockItem> INFESTED_SANDSTONE_WALL = ITEMS.registerSimpleBlockItem(
            "infested_sandstone_wall", ModBlocks.INFESTED_SANDSTONE_WALL);
    public static final DeferredItem<BlockItem> INFESTED_RUBBLE_WALL = ITEMS.registerSimpleBlockItem(
            "infestedrubble_wall", ModBlocks.INFESTED_RUBBLE_WALL);
    public static final DeferredItem<BlockItem> INFESTED_STAIN_WALL = ITEMS.registerSimpleBlockItem(
            "infestedstain_wall", ModBlocks.INFESTED_STAIN_WALL);
    public static final DeferredItem<BlockItem> BIOMEHEART = ITEMS.registerSimpleBlockItem("biomeheart", ModBlocks.BIOMEHEART);
    public static final DeferredItem<BlockItem> COLONYHEART = ITEMS.registerSimpleBlockItem("colonyheart", ModBlocks.COLONYHEART);
    public static final DeferredItem<BlockItem> PARASITE_STRUCTURE = ITEMS.registerSimpleBlockItem(
            "parasitestructure", ModBlocks.PARASITE_STRUCTURE);
    public static final DeferredItem<BlockItem> SEMIORGANIC_BLOCK = ITEMS.registerSimpleBlockItem(
            "semiorganic_block", ModBlocks.SEMIORGANIC_BLOCK);
    public static final DeferredItem<BlockItem> NODE_REDSTONE_LAMP = ITEMS.registerSimpleBlockItem(
            "node_redstone_lamp", ModBlocks.NODE_REDSTONE_LAMP);
    public static final DeferredItem<BlockItem> RELAY_BASE = ITEMS.registerSimpleBlockItem(
            "relay_base", ModBlocks.RELAY_BASE);
    public static final DeferredItem<BlockItem> RELAY_MIDDLE = ITEMS.registerSimpleBlockItem(
            "relay_middle", ModBlocks.RELAY_MIDDLE);
    public static final DeferredItem<BlockItem> RELAY_ROOF = ITEMS.registerSimpleBlockItem(
            "relay_roof", ModBlocks.RELAY_ROOF);
    public static final DeferredItem<BlockItem> ALVEOLI = ITEMS.registerSimpleBlockItem(
            "alveoli", ModBlocks.ALVEOLI);
    public static final DeferredItem<BlockItem> SICK_ALVEOLI = ITEMS.registerSimpleBlockItem(
            "sick_alveoli", ModBlocks.SICK_ALVEOLI);
    public static final DeferredItem<BlockItem> ALVEOLI_GROWTH = ITEMS.registerSimpleBlockItem(
            "alveoli_growth", ModBlocks.ALVEOLI_GROWTH);
    public static final DeferredItem<BlockItem> SOLID_ALVEOLI_BLOCK = ITEMS.registerSimpleBlockItem(
            "solid_alveoli_block", ModBlocks.SOLID_ALVEOLI_BLOCK);
    public static final DeferredItem<BlockItem> HAIR_FOLLICLE_BLOCK = ITEMS.registerSimpleBlockItem(
            "hair_follicle_block", ModBlocks.HAIR_FOLLICLE_BLOCK);
    public static final DeferredItem<ParasiteLootBlockItem> PARASITE_LOOT_COMMON = parasiteLootBlockItem(
            "parasiteloot", ModBlocks.PARASITE_LOOT_COMMON);
    public static final DeferredItem<ParasiteLootBlockItem> PARASITE_LOOT_UNCOMMON = parasiteLootBlockItem(
            "parasiteloot_uncommon", ModBlocks.PARASITE_LOOT_UNCOMMON);
    public static final DeferredItem<ParasiteLootBlockItem> PARASITE_LOOT_RARE = parasiteLootBlockItem(
            "parasiteloot_rare", ModBlocks.PARASITE_LOOT_RARE);
    public static final DeferredItem<EvolutionLureItem> EVOLUTION_LURE_ONE = evolutionLure(
            "evolutionlure_one", EvolutionLureBlock.Tier.ONE);
    public static final DeferredItem<EvolutionLureItem> EVOLUTION_LURE_TWO = evolutionLure(
            "evolutionlure_two", EvolutionLureBlock.Tier.TWO);
    public static final DeferredItem<EvolutionLureItem> EVOLUTION_LURE_THREE = evolutionLure(
            "evolutionlure_three", EvolutionLureBlock.Tier.THREE);
    public static final DeferredItem<EvolutionLureItem> EVOLUTION_LURE_FOUR = evolutionLure(
            "evolutionlure_four", EvolutionLureBlock.Tier.FOUR);
    public static final DeferredItem<EvolutionLureItem> EVOLUTION_LURE_FIVE = evolutionLure(
            "evolutionlure_five", EvolutionLureBlock.Tier.FIVE);
    public static final DeferredItem<EvolutionLureItem> EVOLUTION_LURE_SIX = evolutionLure(
            "evolutionlure_six", EvolutionLureBlock.Tier.SIX);
    public static final DeferredItem<EvolutionLureItem> EVOLUTION_LURE_SEVEN = evolutionLure(
            "evolutionlure_seven", EvolutionLureBlock.Tier.SEVEN);
    public static final DeferredItem<EvolutionLureItem> EVOLUTION_LURE_EIGHT = evolutionLure(
            "evolutionlure_eight", EvolutionLureBlock.Tier.EIGHT);
    public static final DeferredItem<EvolutionLureItem> EVOLUTION_LURE_NINE = evolutionLure(
            "evolutionlure_nine", EvolutionLureBlock.Tier.NINE);
    public static final DeferredItem<EvolutionLureItem> EVOLUTION_LURE_TEN = evolutionLure(
            "evolutionlure_ten", EvolutionLureBlock.Tier.TEN);

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
    public static final DeferredItem<Item> TROPHY_BOOM_ORB = simple("trophy_boom_orb",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));
    public static final DeferredItem<Item> TROPHY_VOID_ORB = simple("trophy_void_orb",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    public static final DeferredItem<FalseAppleItem> FALSE_APPLE = ITEMS.registerItem(
            "false_apple", FalseAppleItem::new, new Item.Properties());
    public static final DeferredItem<FishlinItem> FISHLIN = ITEMS.registerItem(
            "fishlin", FishlinItem::new, new Item.Properties());
    public static final DeferredItem<ShrimpItem> SHRIMP = ITEMS.registerItem(
            "shrimp", ShrimpItem::new, new Item.Properties());
    public static final DeferredItem<AlveoliItem> ALVEOLIGROWTH = ITEMS.registerItem(
            "alveoligrowth", AlveoliItem::new, new Item.Properties());
    public static final DeferredItem<AlveolarFluidItem> ALVEOLAR_FLUID = ITEMS.registerItem(
            "alveolar_fluid", AlveolarFluidItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<DeadBloodFluidItem> DEADBLOOD_FLUID = ITEMS.registerItem(
            "deadblood_fluid", DeadBloodFluidItem::new, new Item.Properties());
    public static final DeferredItem<BoneMealItem> INFESTED_BONEMEAL = ITEMS.registerItem(
            "infested_bonemeal", InfestedBonemealItem::new, new Item.Properties());
    public static final DeferredItem<BlockItem> INFESTED_ORE = ITEMS.registerSimpleBlockItem(
            "infested_ore", ModBlocks.INFESTED_ORE);
    public static final DeferredItem<BlockItem> INFESTED_COAL_ORE = ITEMS.registerSimpleBlockItem(
            "infested_coal_ore", ModBlocks.INFESTED_COAL_ORE);
    public static final DeferredItem<BlockItem> INFESTED_DIAMOND_ORE = ITEMS.registerSimpleBlockItem(
            "infested_diamond_ore", ModBlocks.INFESTED_DIAMOND_ORE);
    public static final DeferredItem<BlockItem> INFESTED_EMERALD_ORE = ITEMS.registerSimpleBlockItem(
            "infested_emerald_ore", ModBlocks.INFESTED_EMERALD_ORE);
    public static final DeferredItem<BlockItem> INFESTED_GOLD_ORE = ITEMS.registerSimpleBlockItem(
            "infested_gold_ore", ModBlocks.INFESTED_GOLD_ORE);
    public static final DeferredItem<BlockItem> INFESTED_IRON_ORE = ITEMS.registerSimpleBlockItem(
            "infested_iron_ore", ModBlocks.INFESTED_IRON_ORE);
    public static final DeferredItem<BlockItem> INFESTED_LAPIS_ORE = ITEMS.registerSimpleBlockItem(
            "infested_lapis_ore", ModBlocks.INFESTED_LAPIS_ORE);
    public static final DeferredItem<BlockItem> INFESTED_REDSTONE_ORE = ITEMS.registerSimpleBlockItem(
            "infested_redstone_ore", ModBlocks.INFESTED_REDSTONE_ORE);
    public static final DeferredItem<EvolutionClockItem> EVCLOCK = ITEMS.registerItem(
            "evclock", EvolutionClockItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<LevelClockItem> LEVELCLOCK = ITEMS.registerItem(
            "levelclock", LevelClockItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<AssimilationWandItem> ITEM_ASSIMILATE = ITEMS.registerItem(
            "itemassimilate", AssimilationWandItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<ParasiteEvolutionWandItem> ITEM_EVOLVE = ITEMS.registerItem(
            "itemevolve", properties -> new ParasiteEvolutionWandItem(
                    ParasiteEvolutionWandItem.Mode.EVOLUTION, properties), new Item.Properties().stacksTo(1));
    public static final DeferredItem<ParasiteEvolutionWandItem> ITEM_DEVOLVE = ITEMS.registerItem(
            "itemdevolve", properties -> new ParasiteEvolutionWandItem(
                    ParasiteEvolutionWandItem.Mode.DEVOLUTION, properties), new Item.Properties().stacksTo(1));
    public static final DeferredItem<SrpCompassItem> NODECOMPASS = ITEMS.registerItem(
            "nodecompass", properties -> new SrpCompassItem(SrpCompassItem.Target.NODE, properties),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<SrpCompassItem> COLONYCOMPASS = ITEMS.registerItem(
            "colonycompass", properties -> new SrpCompassItem(SrpCompassItem.Target.COLONY, properties),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<SrpCompassItem> ORIGINCOMPASS = ITEMS.registerItem(
            "origincompass", properties -> new SrpCompassItem(SrpCompassItem.Target.ORIGIN, properties),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<CompendiumItem> SRP_FIELD_GUIDE = ITEMS.registerItem(
            "srp_field_guide", CompendiumItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<BookOfVengeanceItem> BOOK_OF_VENGEANCE = ITEMS.registerItem(
            "book_of_vengeance", BookOfVengeanceItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<TheSignCharmItem> THE_SIGN_CHARM = ITEMS.registerItem(
            "the_sign_charm", TheSignCharmItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<QuenchItem> ITEMTHROW = ITEMS.registerItem(
            "itemthrow", QuenchItem::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<BoughItem> BOUGH = ITEMS.registerItem(
            "bough", BoughItem::new, new Item.Properties());
    public static final DeferredItem<ThornshadeBerryItem> THORNSHADE_BERRY = ITEMS.registerItem(
            "thornshade_berry", ThornshadeBerryItem::new, new Item.Properties());
    public static final DeferredItem<ThornshadeDecanterItem> THORNSHADE_DECANTER = ITEMS.registerItem(
            "thornshade_decanter", ThornshadeDecanterItem::new, new Item.Properties());
    public static final DeferredItem<VenkrolBootsItem> VENKROL_BOOTS = ITEMS.registerItem("venkrol_boots",
            properties -> new VenkrolBootsItem(ModArmorMaterials.VENKROL, ArmorItem.Type.BOOTS,
                    properties.durability(520)),
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<ModuleComponentItem> MODULE_BASE = ITEMS.registerItem(
            "module_base", ModuleComponentItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<ModuleComponentItem> TISSUE_SPIKE = ITEMS.registerItem(
            "tissue_spike", ModuleComponentItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<ModuleComponentItem> ORGAN_SYNTH = ITEMS.registerItem(
            "organ_synth", ModuleComponentItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<RelayModuleItem> MODULE_INBORN = module(
            "module_inborn", RelayModuleItem.Kind.INBORN);
    public static final DeferredItem<RelayModuleItem> MODULE_ASSIMILATED = module(
            "module_assimilated", RelayModuleItem.Kind.ASSIMILATED);
    public static final DeferredItem<RelayModuleItem> MODULE_ASSIMARA = module(
            "module_assimara", RelayModuleItem.Kind.ASSIMARA);
    public static final DeferredItem<RelayModuleItem> MODULE_HIJACKED = module(
            "module_hijacked", RelayModuleItem.Kind.HIJACKED);
    public static final DeferredItem<RelayModuleItem> MODULE_FERAL = module(
            "module_feral", RelayModuleItem.Kind.FERAL);
    public static final DeferredItem<RelayModuleItem> MODULE_CRUDE = module(
            "module_crude", RelayModuleItem.Kind.CRUDE);
    public static final DeferredItem<RelayModuleItem> MODULE_PRIMITIVE = module(
            "module_primitive", RelayModuleItem.Kind.PRIMITIVE);
    public static final DeferredItem<RelayModuleItem> MODULE_ADAPTED = module(
            "module_adapted", RelayModuleItem.Kind.ADAPTED);
    public static final DeferredItem<RelayModuleItem> MODULE_NEXUS = module(
            "module_nexus", RelayModuleItem.Kind.NEXUS);
    public static final DeferredItem<RelayModuleItem> MODULE_DETERRENT = module(
            "module_deterrent", RelayModuleItem.Kind.DETERRENT);
    public static final DeferredItem<RelayModuleItem> MODULE_PURE = module(
            "module_pure", RelayModuleItem.Kind.PURE);
    public static final DeferredItem<RelayModuleItem> MODULE_PREEMINENT = module(
            "module_preeminent", RelayModuleItem.Kind.PREEMINENT);
    public static final DeferredItem<RelayModuleItem> MODULE_ANCIENT = module(
            "module_ancient", RelayModuleItem.Kind.ANCIENT);
    public static final DeferredItem<RelayModuleItem> MODULE_DERIVED = module(
            "module_derived", RelayModuleItem.Kind.DERIVED);
    public static final DeferredItem<RelayModuleItem> MODULE_DESMOID = module(
            "module_desmoid", RelayModuleItem.Kind.DESMOID);
    public static final DeferredItem<RelayModuleItem> MODULE_ESCHAR = module(
            "module_eschar", RelayModuleItem.Kind.ESCHAR);
    public static final DeferredItem<RelayModuleItem> MODULE_RESISTANCE = module(
            "module_resistance", RelayModuleItem.Kind.RESISTANCE);
    public static final DeferredItem<RelayModuleItem> MODULE_IDEAL = module(
            "module_ideal", RelayModuleItem.Kind.IDEAL);
    public static final DeferredItem<RelayModuleItem> MODULE_ORIGIN = module(
            "module_origin", RelayModuleItem.Kind.ORIGIN);
    public static final DeferredItem<RelayModuleItem> MODULE_PHASE = module(
            "module_phase", RelayModuleItem.Kind.PHASE);
    public static final DeferredItem<RelayModuleItem> MODULE_VECTORS = module(
            "module_vectors", RelayModuleItem.Kind.VECTORS);
    public static final DeferredItem<RelayModuleItem> MODULE_DISLODGEMENT = module(
            "module_dislodgement", RelayModuleItem.Kind.DISLODGEMENT);
    public static final DeferredItem<RelayReportItem> RELAY_SCAN_REPORT = ITEMS.registerItem(
            "relay_scan_report", properties -> new RelayReportItem(RelayReportItem.Type.SCAN, properties),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<RelayReportItem> PHASE_REPORT = ITEMS.registerItem(
            "phase_report", properties -> new RelayReportItem(RelayReportItem.Type.PHASE, properties),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<RelayReportItem> VECTOR_MAP = ITEMS.registerItem(
            "vector_map", properties -> new RelayReportItem(RelayReportItem.Type.VECTOR, properties),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<RelayReportItem> DISLODGEMENT_REPORT = ITEMS.registerItem(
            "dislodgement_report",
            properties -> new RelayReportItem(RelayReportItem.Type.DISLODGEMENT, properties),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<BlockItem> INFESTATION_PURIFIER = ITEMS.registerSimpleBlockItem(
            "infestation_purifier", ModBlocks.INFESTATION_PURIFIER);
    public static final DeferredItem<OverlastFoodItem> CHOCOLATE_SMOOTHIE = overlastFood(
            "chocolate_smoothie", OverlastFoodItem.Kind.CHOCOLATE_SMOOTHIE, 8);
    public static final DeferredItem<OverlastFoodItem> POLLUTED_HERBAL_BOWL = overlastFood(
            "polluted_herbal_bowl", OverlastFoodItem.Kind.POLLUTED_HERBAL_BOWL, 8);
    public static final DeferredItem<OverlastFoodItem> HERBAL_BOWL = overlastFood(
            "herbal_bowl", OverlastFoodItem.Kind.HERBAL_BOWL, 8);
    public static final DeferredItem<OverlastFoodItem> MELON_ICE = overlastFood(
            "melon_ice", OverlastFoodItem.Kind.MELON_ICE, 32);
    public static final DeferredItem<OverlastFoodItem> ICE_SUCKER = overlastFood(
            "ice_sucker", OverlastFoodItem.Kind.ICE_SUCKER, 32);
    public static final DeferredItem<OverlastFoodItem> DUMPLING = overlastFood(
            "dumpling", OverlastFoodItem.Kind.DUMPLING, 64);
    public static final DeferredItem<OverlastCanteenItem> DRINKING_POTION = canteen(
            "drinking_potion", OverlastCanteenItem.Dose.EMPTY);
    public static final DeferredItem<OverlastCanteenItem> PURIFYING_POTION = canteen(
            "purifying_potion", OverlastCanteenItem.Dose.PURIFY);
    public static final DeferredItem<OverlastCanteenItem> INFECTING_POTION = canteen(
            "infecting_potion", OverlastCanteenItem.Dose.INFECT);
    public static final DeferredItem<OverlastCanteenItem> STRONG_INFECTING_POTION = canteen(
            "strong_infecting_potion", OverlastCanteenItem.Dose.STRONG_INFECT);
    public static final DeferredItem<InjectedPurifierItem> INJECTED_PURIFIER = ITEMS.registerItem(
            "injected_purifier", InjectedPurifierItem::new, new Item.Properties().stacksTo(6));
    public static final DeferredItem<EvolutionDeviceItem> EVOLUTION_DEVICE = ITEMS.registerItem(
            "evolution_device", EvolutionDeviceItem::new, new Item.Properties().stacksTo(1));

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
    private static DeferredItem<ParasiteLootBlockItem> parasiteLootBlockItem(String id,
            net.neoforged.neoforge.registries.DeferredBlock<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.registerItem(id, properties -> new ParasiteLootBlockItem(block.get(), properties),
                new Item.Properties());
    }

    private static <T extends net.minecraft.world.entity.Mob> DeferredItem<SpawnEggItem> spawnEgg(String id,
            net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>,
                    net.minecraft.world.entity.EntityType<T>> type, int primary, int secondary) {
        return ITEMS.registerItem(id,
                properties -> new TexturedSpawnEggItem(type.get(), primary, secondary, properties),
                new Item.Properties());
    }
    private static DeferredItem<Item> simple(String id, Item.Properties properties) {
        return ITEMS.registerSimpleItem(id, properties);
    }

    private static DeferredItem<EvolutionLureItem> evolutionLure(String id, EvolutionLureBlock.Tier tier) {
        return ITEMS.registerItem(id, properties -> new EvolutionLureItem(ModBlocks.EVOLUTION_LURE.get(), tier,
                properties), new Item.Properties());
    }

    private static DeferredItem<RelayModuleItem> module(String id, RelayModuleItem.Kind kind) {
        return ITEMS.registerItem(id, properties -> new RelayModuleItem(kind, properties),
                new Item.Properties().stacksTo(1));
    }

    private static DeferredItem<OverlastFoodItem> overlastFood(String id, OverlastFoodItem.Kind kind, int stackSize) {
        return ITEMS.registerItem(id, properties -> new OverlastFoodItem(kind, properties),
                new Item.Properties().stacksTo(stackSize));
    }

    private static DeferredItem<OverlastCanteenItem> canteen(String id, OverlastCanteenItem.Dose dose) {
        return ITEMS.registerItem(id, properties -> new OverlastCanteenItem(dose, properties),
                dose == OverlastCanteenItem.Dose.EMPTY
                        ? new Item.Properties().stacksTo(1)
                        : new Item.Properties().durability(6));
    }

    private static DeferredItem<LivingWeaponItem> livingWeapon(String id, LivingWeaponItem.WeaponKind kind,
            float damage, float speed,
            float reach, boolean sentient, DeferredItem<? extends Item> next) {
        return ITEMS.registerItem(id, properties -> kind == LivingWeaponItem.WeaponKind.MAUL
                ? new LivingMaulItem(damage, speed, reach, sentient, next == null ? null : next::get, properties)
                : new LivingWeaponItem(kind, damage, speed, reach, sentient,
                        next == null ? null : next::get, properties),
                new Item.Properties().durability(1000));
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
