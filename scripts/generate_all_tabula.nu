#!/usr/bin/env nu

def main [
    --dry-run # Report mappings without writing generated Java sources.
] {
let root = 'D:/code/模组反编译器/decompiled/[逃逸：寄生体] SRParasites-1.10.8'
let source_root = $"($root)/com/dhanantry/scapeandrunparasites/client/model/entity"
let source_result = (^rg --files -g 'Model*.java' $source_root | complete)
if $source_result.exit_code != 0 {
    error make {msg: $"Failed to enumerate original Tabula models:\n($source_result.stderr)"}
}
let source_files = ($source_result.stdout | lines)

let source_records = ($source_files | each {|file|
    let raw = (open --raw $file)
    if not ($raw | str contains "extends ModelSRP") {
        return null
    }
    let fields = ($raw | parse -r "public ModelRenderer (?<name>[A-Za-z0-9_]+);" | get name | uniq)
    {path: $file, fields: $fields}
} | compact)

let migrated = [buglin marauder rupter pri_bolster pri_longarms pri_summoner pri_vermin pri_viscera pri_arachnida]
let geo_files = (glob 'src/main/resources/assets/csrp/geo/*.json')
# Nu's path stem for `foo.geo.json` is `foo.geo`; strip the intermediate
# `.geo` suffix so generated Java classes have valid identifiers and match
# the entity ids used by the registries.
let ids = ($geo_files
    | each {|file| $file | path parse | get stem | str replace ".geo" "" }
    | where {|id| $id not-in $migrated})

let candidate_count = ($ids | length)
let source_count = ($source_records | length)
print $"candidates=($candidate_count) sources=($source_count)"
mut generated = []
for id in $ids {
    let geo = (open $"src/main/resources/assets/csrp/geo/($id).geo.json")
    let bones = (try { $geo | get "minecraft:geometry" | first | get bones | get name | where {|name| $name != "srp_coordinate_root"} | uniq } catch { [] })
    let best = ($source_records | each {|source|
        let common = ($source.fields | where {|name| $name in $bones} | length)
        let union = (($source.fields | append $bones) | uniq | length)
        $source | merge {common: $common, score: (if $union == 0 { 0.0 } else { $common / $union })}
    } | sort-by score --reverse | first)
    if ($best | is-empty) {
        print $"NO SOURCE ($id)"
        continue
    }
    let destination = $"src/main/java/alku/csrp/client/model/tabula/generated/ModelTabula_($id).java"
    let model_name = $"ModelTabula_($id)"
    if not $dry_run {
        let port_result = (^nu scripts/port_tabula_model.nu $best.path $destination alku.csrp.client.model.tabula.generated Mob net.minecraft.world.entity.Mob $model_name | complete)
        if $port_result.exit_code != 0 {
            error make {msg: $"Failed to port ($id) from ($best.path):\n($port_result.stderr)"}
        }
    }
    $generated = ($generated | append {id: $id, source: ($best.path | path basename), score: ($best.score | math round --precision 3)})
}

let generated_count = ($generated | length)
print $"generated=($generated_count)"
# Summary table intentionally omitted when no candidates are generated.
if ($generated | is-not-empty) {
    $generated | sort-by score | first 25 | table | print
}
}
