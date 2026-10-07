package alku.csrp.event;

import alku.csrp.Csrp;
import alku.csrp.Config;
import alku.csrp.entity.Parasite;
import alku.csrp.registry.ModMobEffects;
import alku.csrp.world.ParasiteLootTables;
import alku.csrp.world.SrpWorldData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Port of {@code util/handlers/SRPEventHandlerBus#setLoot} plus its private
 * {@code loot(LivingDropsEvent, String[])} helper (out109, lines 1583-2240).
 *
 * <p>The 1.12.2 mod did not use vanilla loot tables for its parasites: every parasite death went
 * through this handler, which read the per-mob {@code SRPConfigMobs.<x>Loot} string arrays and rolled
 * them itself. The 1.20.1 sibling port replaced that with data-driven loot tables, and this project
 * inherited those tables; several of them dropped different items (or different chances) than the
 * 1.10.9 configs. This handler restores the original algorithm over the transcribed defaults in
 * {@link ParasiteLootTables}, so the drop behaviour matches 1.10.9 again.
 *
 * <p>Original algorithm, reproduced verbatim:
 * <ul>
 *   <li>entries flagged {@code always=true} are each rolled independently:
 *       {@code rand.nextInt(100) <= chance - 1}, and on success drop {@code rand.nextInt(count) + 1};</li>
 *   <li>entries flagged {@code always=false} form one pool: a single entry is chosen uniformly,
 *       then the same chance/count roll decides whether it drops at all.</li>
 * </ul>
 * Item ids that do not resolve (for example the unregistered {@code srparasites:ada_burrower_drop},
 * or addon items such as {@code srpmeshi:vilebeefr}) drop nothing, exactly as
 * {@code Item.func_111206_d} returned {@code null} in the original.
 *
 * <p>Runs at {@link net.neoforged.bus.api.EventPriority#HIGHEST} so it replaces the data-driven loot
 * table result before the other drop handlers (celestial shrimp, vengeance book, OverLast bonus)
 * append their extras.
 */
@EventBusSubscriber(modid = Csrp.MODID)
public final class ParasiteLootEvents {
    private ParasiteLootEvents() {
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Parasite)) {
            return;
        }
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !level.getGameRules().get(GameRules.MOB_DROPS)) {
            return;
        }
        // Original guards before the switch.
        if (event.getEntity().hasEffect(ModMobEffects.DEBAR)) {
            return;
        }
        if (Config.useDislodgment() && Config.disloLootXpCancel() && dislodgmentLootSuppressed(level)) {
            return;
        }

        String path = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).getPath();
        List<ParasiteLootTables.Entry> entries = ParasiteLootTables.forEntity(path);
        if (entries == null || entries.isEmpty()) {
            return;
        }

        // The original handler is the only source of parasite drops, so the data-driven table
        // result is discarded and replaced by the config-driven roll.
        event.getDrops().clear();
        for (ItemStack stack : roll(entries, new Random())) {
            event.getDrops().add(new ItemEntity(event.getEntity().level(),
                    event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), stack));
        }
    }

    /**
     * Pure port of {@code loot(LivingDropsEvent, String[])}'s rolling half, split out so the drop
     * table can be exercised deterministically. Returns one stack per dropped item (the original
     * spawned a separate {@code EntityItem} for each).
     */
    public static List<ItemStack> roll(List<ParasiteLootTables.Entry> entries, Random random) {
        List<ItemStack> result = new ArrayList<>();
        List<ParasiteLootTables.Entry> pooled = new ArrayList<>();
        for (ParasiteLootTables.Entry entry : entries) {
            if (entry.always()) {
                rollOne(entry, random, result);
            } else {
                pooled.add(entry);
            }
        }
        if (!pooled.isEmpty()) {
            rollOne(pooled.get(random.nextInt(pooled.size())), random, result);
        }
        return result;
    }

    private static void rollOne(ParasiteLootTables.Entry entry, Random random, List<ItemStack> out) {
        if (random.nextInt(100) > entry.chance() - 1) {
            return;
        }
        Item item = resolve(entry.item());
        if (item == null) {
            return;
        }
        int quantity = Math.max(0, entry.count());
        int realQuantity = quantity == 0 ? 0 : random.nextInt(quantity);
        for (int j = 0; j <= realQuantity; j++) {
            out.add(new ItemStack(item));
        }
    }

    /** Original {@code Item.func_111206_d}: resolve {@code srparasites:} ids against this mod's namespace. */
    private static Item resolve(String raw) {
        String id = raw.startsWith("srparasites:") ? Csrp.MODID + ":" + raw.substring("srparasites:".length()) : raw;
        Identifier key = Identifier.tryParse(id);
        if (key == null) {
            return null;
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(key);
        return item.orElse(null);
    }

    /** Original {@code mob.disloNumberEighteen && getCurrentCode(dim, 18) > 0}. */
    private static boolean dislodgmentLootSuppressed(ServerLevel level) {
        SrpWorldData data = SrpWorldData.get(level);
        return alku.csrp.world.DislodgmentSystem.activeValue(data, 18) > 0;
    }
}
