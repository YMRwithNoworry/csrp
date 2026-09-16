package alku.csrp.registry;

import alku.csrp.Csrp;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public final class ModTiers {
    public static final ToolMaterial LIVING = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 1000, 7.0F, 0.0F, 14,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Csrp.MODID, "living_repair")));

    public static final ToolMaterial HIJACKED_IRON = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 1561, 7.0F, 2.5F, 14,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Csrp.MODID, "hijacked_iron_repair")));

    private ModTiers() {
    }
}
