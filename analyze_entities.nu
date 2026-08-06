#!/usr/bin/env nu

# 分析实体类的完整度
def analyze_entity [file: string] {
    let content = open $file
    let filename = ($file | path basename)

    let has_registerGoals = ($content | str contains "registerGoals()")
    let has_doHurtTarget = ($content | str contains "doHurtTarget(")
    let has_hurt = ($content | str contains "hurt(")
    let has_tick = ($content | str contains "tick()")
    let has_registerControllers = ($content | str contains "registerControllers(")
    let has_special_ability = (
        ($content | str contains "冲刺") or
        ($content | str contains "飞行") or
        ($content | str contains "钻地") or
        ($content | str contains "burrow") or
        ($content | str contains "charge") or
        ($content | str contains "fly") or
        ($content | str contains "projectile") or
        ($content | str contains "summon") or
        ($content | str contains "teleport")
    )

    # 计算完整度分数
    let score = (
        (if $has_registerGoals { 1 } else { 0 }) +
        (if $has_doHurtTarget { 1 } else { 0 }) +
        (if $has_hurt { 1 } else { 0 }) +
        (if $has_tick { 1 } else { 0 }) +
        (if $has_registerControllers { 1 } else { 0 }) +
        (if $has_special_ability { 1 } else { 0 })
    )

    {
        file: $filename,
        registerGoals: $has_registerGoals,
        doHurtTarget: $has_doHurtTarget,
        hurt: $has_hurt,
        tick: $has_tick,
        registerControllers: $has_registerControllers,
        specialAbility: $has_special_ability,
        score: $score,
        completeness: ($score * 100 / 6)
    }
}

# 主程序
let entity_dir = "D:/code/MC模组/csrp/src/main/java/alku/csrp/entity"
let entity_files = (ls $"($entity_dir)/*Entity.java" | get name)

print $"找到 ($entity_files | length) 个实体类文件"
print ""

let results = ($entity_files | each {|file| analyze_entity $file})

# 按完整度排序
let sorted = ($results | sort-by completeness -r)

# 输出结果
print "===== 实体类实现完整度分析 ====="
print ""

$sorted | each {|r|
    print $"【($r.file)】 完整度: ($r.completeness)%"
    print $"  registerGoals: ($r.registerGoals)"
    print $"  doHurtTarget: ($r.doHurtTarget)"
    print $"  hurt: ($r.hurt)"
    print $"  tick: ($r.tick)"
    print $"  registerControllers: ($r.registerControllers)"
    print $"  specialAbility: ($r.specialAbility)"
    print ""
}

# 统计信息
let avg_completeness = ($results | get completeness | math avg)
let high_completeness = ($results | where completeness >= 80 | length)
let medium_completeness = ($results | where completeness >= 50 and completeness < 80 | length)
let low_completeness = ($results | where completeness < 50 | length)

print "===== 统计摘要 ====="
print $"平均完整度: ($avg_completeness | math round -p 1)%"
print $"高完整度 (≥80%): ($high_completeness)"
print $"中完整度 (50-80%): ($medium_completeness)"
print $"低完整度 (<50%): ($low_completeness)"
