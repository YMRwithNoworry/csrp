# 实体完成度计划

基于实体分析报告（2026-08-06）

## 📊 当前状态总览

- **总实体数**: 96个
- **S级（完整实现 95%+）**: 6个
- **A级（核心完整 80%+）**: 3个
- **B级（基础完整 60%+）**: ~15个
- **C级（框架存在 40%+）**: 7个
- **动画支持率**: 26% (25/96)

---

## 🎯 第一阶段：修复A级实体的遗漏功能（高优先级）

### 1. PriArachnidaEntity - 拉拽投射物
**状态**: ⭐⭐⭐⭐ (缺PullingBall)
**缺失**: `executePullSkill()` 方法为空

**任务**:
```java
private void executePullSkill() {
    LivingEntity target = getTarget();
    if (target == null) return;
    
    // 创建 PullingBallEntity 投射物
    PullingBallEntity ball = ModEntities.PULLING_BALL.get().create(level());
    ball.setOwner(this);
    ball.setTarget(target);
    // 设置投射物属性
    level().addFreshEntity(ball);
}
```

**参考**: `D:\code\模组反编译器\decompiled\SRParasites-1.10.7\...\EntityPrimArachnida.java`

---

### 2. AdaLonglegEntity - 蛛网投射物
**状态**: ⭐⭐⭐⭐ (缺WebProjectile)
**缺失**: WebPullGoal中的投射物实现

**任务**:
- 实现 WebProjectile 实体
- 在 WebPullGoal 中发射蛛网
- 添加蛛网粘性效果（减速）

**参考**: 原模组的 EntityWebProjectile

---

## 🔧 第二阶段：补全B级实体技能（中优先级）

### 3. PriManducaterEntity
**缺失技能**:
- 跳跃攻击（Leap Attack）
- 特殊咬合动画
- 击晕效果

### 4. PriReekerEntity  
**缺失技能**:
- 毒气喷射
- 范围debuff
- 尖叫音效触发

### 5. PriYelloweyeEntity
**缺失技能**:
- 飞行攻击模式
- 远程射击
- 群体侦察能力

### 6. AdaScuttlerEntity & AdaWatcherEntity
**缺失技能**:
- PullingBall 机制（继承自 Arachnida）
- 钻地优化
- 感知范围扩展

---

## 💎 第三阶段：实现C级Boss实体（低优先级/长期）

### 7. PreeminentParasiteEntity ⭐⭐
**需要实现**:
- 多形态切换系统
  - Carrier Colony 模式
  - Haunter 模式
  - 其他形态
- 阶段性Boss战机制
- 特殊召唤能力
- 独特动画序列

**预计工作量**: 40-60小时

---

### 8. CruxEntity ⭐⭐
**需要实现**:
- 投掷方块机制
  - 选取周围方块
  - 投掷轨迹计算
  - 方块伤害实体（已有CruxThrownBlockDamageEntity）
- 大型碰撞箱
- 重型攻击动画
- 地形破坏

**预计工作量**: 30-40小时

---

### 9. ArchitectEntity ⭐⭐
**需要实现**:
- 结构生成系统
  - 寄生建筑蓝图
  - 动态生成逻辑
- 指挥能力
  - 增益范围内友军
  - 战术AI决策
- 建造动画
- 防御机制

**预计工作量**: 50-80小时

---

### 10. SummonerEntity ⭐⭐
**需要实现**:
- 召唤魔法
  - 召唤不同类型寄生体
  - 召唤数量限制
  - 召唤冷却
- 远程魔法攻击
- 传送能力
- 魔法护盾

**预计工作量**: 25-35小时

---

### 11. WorkerEntity ⭐⭐
**需要实现**:
- 资源采集AI
  - 识别可采集方块
  - 采集动画
  - 资源运输
- 建造行为
  - 放置寄生方块
  - 修复结构
- 非战斗AI优化

**预计工作量**: 30-40小时

---

### 12. DraconiteEntity ⭐⭐
**需要实现**:
- 龙息攻击
  - 蓄力动画
  - 范围火焰
  - 持续伤害
- 飞行战斗AI
- 俯冲攻击
- 翅膀攻击（近战）

**预计工作量**: 35-50小时

---

### 13. AssimilatedDragonEntity ⭐⭐
**需要实现**:
- 完整末影龙机制
  - 多段身体
  - 飞行路径
  - 末地水晶交互
- Boss战阶段
- 特殊攻击模式
- 龙头实体同步（已有AssimilatedDragonHeadEntity）

**预计工作量**: 60-100小时

---

## 🎨 第四阶段：动画系统补全（持续进行）

**当前覆盖率**: 26% (25/96)
**目标覆盖率**: 90% (87/96)

### 需要添加动画的实体（优先）:
1. PriManducaterEntity - Leap、Bite动画
2. PriReekerEntity - Spit、Scream动画
3. PriYelloweyeEntity - Fly、Shoot动画
4. PreeminentParasiteEntity - 多形态动画
5. CruxEntity - Throw、Smash动画
6. ArchitectEntity - Build、Command动画
7. SummonerEntity - Cast、Summon动画
8. WorkerEntity - Mine、Place动画
9. DraconiteEntity - Breath、Fly动画
10. AssimilatedDragonEntity - 完整龙动画

**动画资源位置**: `D:\code\MC模组\srp生物模型和动画提取\提取结果`

---

## 📅 实施时间表

### 第一阶段（1-2周）
- [ ] PriArachnida 拉拽投射物
- [ ] AdaLongleg 蛛网投射物
- [ ] 修复原地移动动画（✅ 已完成）
- [ ] 肉块合成平衡（✅ 已完成）

### 第二阶段（3-6周）
- [ ] PriManducater 跳跃攻击
- [ ] PriReeker 毒气系统
- [ ] PriYelloweye 飞行攻击
- [ ] AdaScuttler/Watcher PullingBall
- [ ] 补全15个B级实体动画

### 第三阶段（7-12周）
- [ ] Summoner 召唤系统
- [ ] Worker 建造系统
- [ ] Draconite 龙息攻击

### 第四阶段（13-20周）
- [ ] Crux 投掷方块
- [ ] Architect 建筑系统
- [ ] PreeminentParasite 多形态
- [ ] AssimilatedDragon 末影龙机制

---

## 🔍 参考资源

1. **原模组代码**: `D:\code\模组反编译器\decompiled\SRParasites-1.10.7`
2. **动画资源**: `D:\code\MC模组\srp生物模型和动画提取\提取结果`
3. **官方Wiki**: `D:\code\MC模组\csrp\srp官方wiki`
4. **当前项目**: `D:\code\MC模组\csrp\src\main\java\alku\csrp\entity`

---

## ✅ 已完成项目

- [x] 原地移动动画修复（54个文件）
- [x] 肉块合成平衡（移除吞噬兽）
- [x] 基础寄生体框架（PrimitiveParasiteEntity）
- [x] 伤害适应系统
- [x] 钻地系统（BurrowingVariantEntity）
- [x] 自爆系统（CarrierEntity）
- [x] 6个S级实体完整实现

---

*最后更新: 2026-08-06*
*下次更新: 待Wiki对比报告完成*
