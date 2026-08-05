package alku.csrp.compendium;

import java.util.List;

public final class CompendiumCatalog {
    public static final List<String> BLOCKS = List.of(
            "harleskinn_block", "locs_block", "fog_nullifier", "dispatchern", "assimilated_reed",
            "infuser_furnace", "biomass_block", "residue_block", "sick_alveoli", "alveoli",
            "hair_follicle_block", "parasite_barrier", "assimilated_pumpkin", "esca_bulb",
            "node_redstone_lamp", "parasiteloot", "infestation_purifier", "parasitemouth",
            "relay_base", "relay_middle", "relay_roof", "biomepurifier", "infestremain", "diseased_sponge");
    public static final List<String> CELESTIALS = List.of(
            "blip", "pulse", "eight", "arrow", "twenty_seven", "three", "eighty_three", "four_comet",
            "dark_days", "jupiter", "mars", "mercury", "neptune", "pluto", "saturn", "uranus", "venus");
    public static final List<String> SYSTEMS = List.of(
            "reinforcement", "merge", "status_effects", "eiv", "evolution", "collective_consciousness",
            "scent", "ubiquitous_development", "dislodgment", "generations", "vectors", "colonies", "nodes",
            "hives", "nests", "variants", "derived_distortion");

    private CompendiumCatalog() {
    }
}
