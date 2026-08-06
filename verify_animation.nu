#!/usr/bin/env nu

# 验证动画适配完整性脚本

cd 'D:/code/MC模组/csrp'

# 获取所有实体类
let entity_files = (ls src/main/java/alku/csrp/entity/*Entity.java | where type == file | get name)

print $"总实体类数量: ($entity_files | length)"
print ""

# 检查每个实体类是否实现GeoEntity接口
print "检查 GeoEntity 接口实现..."

let results = ($entity_files | each { |file|
    let content = (open $file)
    let entity_name = ($file | str replace 'src/main/java/alku/csrp/entity/' '' | str replace '.java' '')
    let has_geo = (($content | str contains "implements GeoEntity") or ($content | str contains "implements IAnimatable"))
    let has_register = ($content | str contains "registerControllers")
    let is_abstract = (($content | str contains "abstract class") or ($file | str contains "Abstract"))

    {
        name: $entity_name,
        has_geo: $has_geo,
        has_register: $has_register,
        is_abstract: $is_abstract
    }
})

let concrete_entities = ($results | where is_abstract == false)
let geo_entity_count = ($concrete_entities | where has_geo == true | length)
let register_count = ($concrete_entities | where has_register == true | length)
let missing_geo = ($concrete_entities | where has_geo == false | get name)
let missing_register = ($concrete_entities | where has_register == false | get name)

print $"实现 GeoEntity 的实体数: ($geo_entity_count)/($concrete_entities | length)"
if ($missing_geo | length) > 0 {
    print "未实现 GeoEntity 的实体:"
    for entity in $missing_geo {
        print $"  - ($entity)"
    }
}
print ""

print "检查 registerControllers 方法..."
print $"包含 registerControllers 的实体数: ($register_count)/($concrete_entities | length)"
if ($missing_register | length) > 0 {
    print "缺少 registerControllers 的实体:"
    for entity in $missing_register {
        print $"  - ($entity)"
    }
}
print ""

# 统计结果
print "========== 验证总结 =========="
print $"总实体类数量: ($entity_files | length)"
print $"具体实体类数量: ($concrete_entities | length)"
print $"实现 GeoEntity: ($geo_entity_count)"
print $"包含 registerControllers: ($register_count)"
print $"未完全适配: ($missing_geo | length)"
