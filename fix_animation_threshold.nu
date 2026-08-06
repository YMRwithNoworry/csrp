#!/usr/bin/env nu

# 修复实体原地播放移动动画的问题
# 将过小的移动阈值 0.0001 替换为更合理的 0.001

let files = [
    "src/main/java/alku/csrp/entity/AdaScuttlerEntity.java",
    "src/main/java/alku/csrp/entity/CarrierEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedVillagerEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedCowEntity.java",
    "src/main/java/alku/csrp/entity/AdaLonglegEntity.java",
    "src/main/java/alku/csrp/entity/PriManducaterEntity.java",
    "src/main/java/alku/csrp/entity/UntamedPriReekerEntity.java",
    "src/main/java/alku/csrp/entity/IncompleteFormMediumEntity.java",
    "src/main/java/alku/csrp/entity/NexusParasiteEntity.java",
    "src/main/java/alku/csrp/entity/IncompleteCruxEntity.java",
    "src/main/java/alku/csrp/entity/MovingFleshEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedDragonEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedEndermanEntity.java",
    "src/main/java/alku/csrp/entity/HiBlazeEntity.java",
    "src/main/java/alku/csrp/entity/HiGolemEntity.java",
    "src/main/java/alku/csrp/entity/SimHumanEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedSheepEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedChickenEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedWolfEntity.java",
    "src/main/java/alku/csrp/entity/HostIIEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedPigEntity.java",
    "src/main/java/alku/csrp/entity/MarauderEntity.java",
    "src/main/java/alku/csrp/entity/AdaptedVariantEntity.java",
    "src/main/java/alku/csrp/entity/UntamedPriWaspEntity.java",
    "src/main/java/alku/csrp/entity/PureParasiteEntity.java",
    "src/main/java/alku/csrp/entity/SummonerEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedParasiteEntity.java",
    "src/main/java/alku/csrp/entity/AncientParasiteEntity.java",
    "src/main/java/alku/csrp/entity/AdaWatcherEntity.java",
    "src/main/java/alku/csrp/entity/ThrallEntity.java",
    "src/main/java/alku/csrp/entity/VerminEntity.java",
    "src/main/java/alku/csrp/entity/VisceraEntity.java",
    "src/main/java/alku/csrp/entity/SimAdventurerHeadEntity.java",
    "src/main/java/alku/csrp/entity/SimAdventurerEntity.java",
    "src/main/java/alku/csrp/entity/KirinEntity.java",
    "src/main/java/alku/csrp/entity/MarauderizedEndermanEntity.java",
    "src/main/java/alku/csrp/entity/MarauderTendrilEntity.java",
    "src/main/java/alku/csrp/entity/LiceEntity.java",
    "src/main/java/alku/csrp/entity/ManglerEntity.java",
    "src/main/java/alku/csrp/entity/IncompleteFormSmallEntity.java",
    "src/main/java/alku/csrp/entity/HostEntity.java",
    "src/main/java/alku/csrp/entity/DraconiteEntity.java",
    "src/main/java/alku/csrp/entity/FlamEntity.java",
    "src/main/java/alku/csrp/entity/HeedEntity.java",
    "src/main/java/alku/csrp/entity/HijackedParasiteEntity.java",
    "src/main/java/alku/csrp/entity/GnatEntity.java",
    "src/main/java/alku/csrp/entity/FeralParasiteEntity.java",
    "src/main/java/alku/csrp/entity/FeralEndermanEntity.java",
    "src/main/java/alku/csrp/entity/DredgeEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedDragonHeadEntity.java",
    "src/main/java/alku/csrp/entity/AirscrewEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedHeadEntity.java",
    "src/main/java/alku/csrp/entity/AssimilatedVariantEntity.java",
    "src/main/java/alku/csrp/entity/LongarmsEntity.java",
    "src/main/java/alku/csrp/entity/ArchitectEntity.java",
    "src/main/java/alku/csrp/entity/AbominationEntity.java",
    "src/main/java/alku/csrp/entity/CruxEntity.java",
    "src/main/java/alku/csrp/entity/PreeminentParasiteEntity.java",
    "src/main/java/alku/csrp/entity/WorkerEntity.java",
    "src/main/java/alku/csrp/entity/PriReekerEntity.java",
    "src/main/java/alku/csrp/entity/PrimitiveVariantEntity.java",
    "src/main/java/alku/csrp/entity/RupterEntity.java",
    "src/main/java/alku/csrp/entity/PriArachnidaEntity.java",
    "src/main/java/alku/csrp/entity/BuglinEntity.java"
]

print "开始修复实体移动动画阈值..."
print $"需要修复的文件数量: ($files | length)"

mut fixed_count = 0

for file in $files {
    if ($file | path exists) {
        print $"处理: ($file)"
        let content = open $file
        let new_content = ($content | str replace --all "horizontalDistanceSqr() >= 0.0001" "horizontalDistanceSqr() >= 0.001")
        let new_content = ($new_content | str replace --all "horizontalDistanceSqr() < 0.0001" "horizontalDistanceSqr() < 0.001")
        $new_content | save -f $file
        $fixed_count = $fixed_count + 1
    } else {
        print $"警告: 文件不存在 ($file)"
    }
}

print $"修复完成! 共修复 ($fixed_count) 个文件"
print "移动阈值已从 0.0001 更改为 0.001（约 0.0316 方块/tick）"
