# 动画缺失问题追踪

## 已知缺失的动画文件

**搜索范围**: 
- 项目资源目录：`src/main/resources/assets/csrp/animations/` (129个文件)
- 动画提取目录：`D:\code\MC模组\srp生物模型和动画提取\提取结果\` (126个目录)

**结论**: 所有Untamed系列的动画文件完全缺失，需要从原模组中提取或重新创建。

### 1. UntamedPriLasherEntity
**状态**: ❌ 动画文件缺失

**实体类**: `UntamedPriLasherEntity.java`

**需要的动画**:
- idle
- walk
- idle.status_1 (charging)
- idle.status_2 (dash_prepare)
- idle.status_3 (dashing)

**缺失文件**: 
- `pri_lasher.animation.json` 或类似命名的动画文件

**搜索结果**: 
- ❌ 动画提取目录（126个文件）中未找到
- ❌ ranrac相关文件不存在
- ❌ nogla相关文件不存在
- ❌ untamed系列文件完全缺失

**解决方案**:
1. 从原模组反编译代码中确认EntityRanrac/EntityNogla的对应关系
2. 查找原模组.animation.json文件位置
3. 手动提取或重新创建动画文件
4. 或者暂时使用占位动画（如pri_arachnida的动画）

**优先级**: 中（Untamed系列相对较少遇到）

---

### 2. UntamedPriWaspEntity
**状态**: ❌ 动画文件缺失

**实体类**: `UntamedPriWaspEntity.java`

**原模组对应**: EntityRanrac（推测）

**搜索结果**:
- ❌ 动画提取目录中未找到untamed_pri_wasp.animation.json
- ❌ ranrac.animation.json不存在
- ❌ 所有untamed系列动画文件均缺失

**解决方案**: 同UntamedPriLasherEntity

**优先级**: 中

---

### 3. UntamedAdaLasher
**状态**: ❌ 实体不存在

**说明**: 经过全面搜索，该实体在原模组和当前项目中都不存在。可能是命名错误或计划中的实体。

---

### 4. RangedMoveIIEntity
**状态**: ❌ 实体不存在

**搜索结果**:
- ❌ `src/main/java/alku/csrp/entity/` 中没有该Java文件
- ❌ 动画文件目录中没有相关命名的文件
- ❌ 所有JSON和Java文件中无任何引用

**结论**: 该实体不存在，可能是：
1. 命名错误
2. 计划中但未实现的实体
3. 来自其他版本或分支

---

## 使用程序化动画的实体（无需.animation.json）

以下实体使用Java代码中的程序化动画，不需要独立的动画文件：

### 1. BeckonerIII (NexusParasiteEntity)
**实现位置**: `NexusParasiteModel.java` - `applyBeckonIIIAnimations()` 方法（第156-196行）

**动画类型**: 程序化骨骼动画

**说明**: 原模组使用Tabula系统，移植到NeoForge 1.21后通过Java代码实现动画逻辑，所有骨骼操作都在模型类中完成。

**其他可能使用程序化动画的实体**:
- 其他Nexus系列（Beckoner I/II/IV, Dispatcher系列）
- 部分特殊实体可能也使用代码动画

---

## 检查清单

在继续动画适配之前，建议检查以下实体的动画文件：

- [ ] UntamedPriWaspEntity
- [ ] UntamedPriReeferEntity  
- [ ] UntamedAdaWaspEntity
- [ ] UntamedAdaReeferEntity
- [ ] 其他Untamed系列实体

---

## 动画提取待办

如果需要补充缺失的动画，按以下步骤：

1. 在原模组反编译代码中查找实体对应关系
2. 在动画提取结果目录中搜索相关文件
3. 复制到项目资源目录：`src/main/resources/assets/csrp/animations/`
4. 在实体类中验证动画引用是否正确
5. 测试游戏中的动画播放

---

*最后更新: 2026-08-06*
*下次更新: 动画适配工作流完成后*
