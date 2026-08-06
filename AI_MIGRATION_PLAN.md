# AI系统完整迁移方案

基于原模组SRParasites-1.10.7代码分析

## 📊 差距分析

### 原模组规模
- **总实体类数**: 174个
- **专用AI类数**: 73个
- **实体分类**: 18个子类别
- **实现完整度**: 100%

### 当前项目规模
- **总实体数**: 96个
- **完整AI实体**: 6个（S级）
- **总体完成度**: **15-20%**

### 缺失系统
❌ **完整AI系统** - 73个专用AI类几乎全部缺失  
❌ **能力接口系统** - 飞行、射击等接口未实现  
❌ **特殊技能系统** - AOE、召唤、闪避等  
❌ **多部件实体系统** - EntityBody机制  
❌ **复杂动画系统** - 大部分模型动画未实现  
❌ **粒子效果系统** - 自定义粒子  
❌ **状态管理系统** - DataParameter状态追踪  
❌ **Boss系统** - BossInfo栏

---

## 🎯 核心AI系统迁移计划

### 第一优先级：基础AI框架（73个AI类）

#### 1. 攻击AI系统（10个类）

##### ✅ 已有
- `MeleeAttackGoal` - 基础近战

##### ❌ 需要实现
```java
// 1.1 AOE近战攻击
package alku.csrp.entity.ai;

public class AttackMeleeAOEGoal extends MeleeAttackGoal {
    private final double aoeRange;
    private final int minTargetsForAOE;
    
    @Override
    protected void checkAndPerformAttack(LivingEntity target) {
        // 扫描AOE范围内的目标
        List<LivingEntity> targets = level.getEntitiesOfClass(
            LivingEntity.class,
            target.getBoundingBox().inflate(aoeRange),
            this::isValidTarget
        );
        
        if (targets.size() >= minTargetsForAOE) {
            performAOEAttack(targets);
        } else {
            super.checkAndPerformAttack(target);
        }
    }
}
```

```java
// 1.2 近远程智能切换
public class AttackMeleeRangedSwitchGoal extends Goal {
    private final Mob mob;
    private final double meleeRange = 3.0D;
    private final double rangedRange = 16.0D;
    private final RangedAttackMob rangedMob;
    
    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        double distance = mob.distanceToSqr(target);
        
        if (distance <= meleeRange * meleeRange) {
            // 使用近战
            mob.doHurtTarget(target);
        } else if (distance <= rangedRange * rangedRange) {
            // 使用远程
            rangedMob.performRangedAttack(target, 1.0F);
        }
    }
}
```

```java
// 1.3 远程状态攻击（带状态效果）
public class AttackRangedStatusGoal extends Goal {
    private final RangedAttackMob mob;
    private final MobEffect statusEffect;
    private final int statusDuration;
    
    public void performAttack(LivingEntity target) {
        // 发射投射物
        Projectile projectile = createProjectile();
        // 添加状态效果到命中逻辑
        projectile.setOwner(mob);
    }
}
```

```java
// 1.4 爆炸攻击（类似爬行者）
public class AttackSwellGoal extends Goal {
    private final Mob mob;
    private final float explosionRadius;
    private int swellTicks;
    private final int maxSwellTicks = 30;
    
    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (mob.distanceTo(target) < 3.0D) {
            swellTicks++;
            if (swellTicks >= maxSwellTicks) {
                explode();
            }
        }
    }
}
```

**预计工作量**: 40小时  
**参考文件**: `EntityAIAttackMeleeStatusAOE.java`, `EntityAIAttackMeleeRangeSwitch.java` 等

---

#### 2. 飞行AI系统（6个类）

```java
// 2.1 飞行攻击AI
package alku.csrp.entity.ai;

public class FlightAttackGoal extends Goal {
    private final Mob mob;
    private final double flySpeed;
    private Vec3 targetPosition;
    
    public FlightAttackGoal(Mob mob, double flySpeed) {
        this.mob = mob;
        this.flySpeed = flySpeed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && mob.hasLineOfSight(target);
    }
    
    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        
        // 计算目标上方位置
        targetPosition = target.position().add(0, 5, 0);
        
        // 3D飞行移动
        Vec3 direction = targetPosition.subtract(mob.position()).normalize();
        mob.setDeltaMovement(direction.scale(flySpeed));
        
        // 俯冲攻击检测
        if (mob.distanceTo(target) < 3.0D) {
            mob.doHurtTarget(target);
        }
    }
}
```

```java
// 2.2 飞行高度限制
public class FlightLimitsGoal extends Goal {
    private final Mob mob;
    private final int minHeight;
    private final int maxHeight;
    
    @Override
    public void tick() {
        BlockPos pos = mob.blockPosition();
        int groundY = findGround(pos);
        int currentHeight = pos.getY() - groundY;
        
        if (currentHeight < minHeight) {
            // 上升
            mob.setDeltaMovement(mob.getDeltaMovement().add(0, 0.1D, 0));
        } else if (currentHeight > maxHeight) {
            // 下降
            mob.setDeltaMovement(mob.getDeltaMovement().add(0, -0.1D, 0));
        }
    }
}
```

**需要配合**:
- `FlyingMoveControl` - 自定义移动控制器
- `setNoGravity(true)` - 取消重力

**预计工作量**: 30小时  
**参考文件**: `EntityAIFlightAttack.java`, `EntityAIFlightLimits.java`

---

#### 3. 闪避AI系统（4个类）

```java
// 3.1 闪避冲刺
public class EvadeDashGoal extends Goal {
    private final Mob mob;
    private final double dashSpeed;
    private final double minDistance;
    private final double maxDistance;
    private int cooldown;
    
    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        double distance = mob.distanceTo(target);
        
        if (distance > minDistance && distance < maxDistance) {
            cooldown++;
        }
        
        if (cooldown >= 40) { // 2秒触发
            performDash(target);
            cooldown = 0;
        }
    }
    
    private void performDash(LivingEntity target) {
        Vec3 direction = target.position()
            .subtract(mob.position())
            .normalize();
        
        // 添加侧向随机偏移
        double offsetX = (mob.getRandom().nextDouble() - 0.5D) * 2.0D;
        double offsetZ = (mob.getRandom().nextDouble() - 0.5D) * 2.0D;
        
        Vec3 dashVec = direction.add(offsetX, 0, offsetZ)
            .normalize()
            .scale(dashSpeed);
        
        mob.setDeltaMovement(dashVec);
    }
}
```

```java
// 3.2 闪避传送
public class EvadeTeleportGoal extends Goal {
    private final Mob mob;
    private final double teleportRange;
    private int cooldown;
    
    private void performTeleport(LivingEntity target) {
        // 在目标周围随机位置传送
        for (int i = 0; i < 16; i++) {
            double x = target.getX() + (random.nextDouble() - 0.5D) * teleportRange;
            double y = target.getY() + random.nextInt(16) - 8;
            double z = target.getZ() + (random.nextDouble() - 0.5D) * teleportRange;
            
            if (canTeleportTo(x, y, z)) {
                mob.teleportTo(x, y, z);
                playTeleportEffect();
                break;
            }
        }
    }
}
```

**预计工作量**: 25小时  
**参考文件**: `EntityAIEvadeDash.java`, `EntityAIEvadeTP.java`

---

#### 4. 召唤AI系统（3个类）

```java
// 4.1 Venkrol巢穴召唤系统
public class NexusSummonGoal extends Goal {
    private final Mob mob;
    private final EntityType<?>[] summonTypes;
    private final int maxSummons;
    private final int summonCooldown;
    private int cooldownTicks;
    private List<Mob> summonedMobs = new ArrayList<>();
    
    @Override
    public boolean canUse() {
        return mob.getTarget() != null 
            && summonedMobs.size() < maxSummons
            && cooldownTicks <= 0;
    }
    
    @Override
    public void start() {
        performSummon();
        cooldownTicks = summonCooldown;
    }
    
    private void performSummon() {
        ServerLevel level = (ServerLevel) mob.level();
        EntityType<?> type = summonTypes[mob.getRandom().nextInt(summonTypes.length)];
        
        Entity entity = type.create(level);
        if (entity instanceof Mob summon) {
            // 在召唤者周围生成
            BlockPos pos = findSpawnPosition();
            summon.moveTo(pos, 0, 0);
            level.addFreshEntity(summon);
            summonedMobs.add(summon);
            
            // 播放召唤效果
            playSummonEffect(pos);
        }
    }
    
    @Override
    public void tick() {
        // 清理死亡的召唤物
        summonedMobs.removeIf(m -> !m.isAlive());
        cooldownTicks--;
    }
}
```

**预计工作量**: 20小时  
**参考文件**: `EntityAIVenkrolSummon.java`, `EntityAIAncientSummon.java`

---

#### 5. 环境交互AI（6个类）

```java
// 5.1 方块感染AI
public class BlockInfestGoal extends Goal {
    private final Mob mob;
    private final int infestRadius;
    private final int infestCooldown;
    private int cooldownTicks;
    
    @Override
    public void tick() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }
        
        BlockPos center = mob.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-infestRadius, -infestRadius, -infestRadius),
            center.offset(infestRadius, infestRadius, infestRadius)
        )) {
            if (BlockInfestation.canConvert(mob.level(), pos)) {
                BlockInfestation.convert(mob.level(), pos, 1);
                cooldownTicks = infestCooldown;
                return;
            }
        }
    }
}
```

```java
// 5.2 光源破坏AI
public class BlockLightDestroyGoal extends Goal {
    private final Mob mob;
    private final int searchRadius;
    
    @Override
    public void tick() {
        BlockPos lightSource = findNearestLightSource();
        if (lightSource != null) {
            mob.getNavigation().moveTo(lightSource.getX(), lightSource.getY(), lightSource.getZ(), 1.0D);
            
            if (mob.distanceToSqr(Vec3.atCenterOf(lightSource)) < 4.0D) {
                mob.level().destroyBlock(lightSource, false);
            }
        }
    }
}
```

**预计工作量**: 30小时  
**参考文件**: `EntityAIBlockInfest.java`, `EntityAIBlockLight.java`

---

### 第二优先级：能力接口系统

```java
// 能力接口定义
package alku.csrp.entity.capability;

public interface ParasiteCanFly {
    boolean canFly();
    double getFlySpeed();
}

public interface ParasiteCanShoot {
    Projectile createProjectile(Level level, LivingEntity target);
    void playShootSound();
    int getShootCooldown();
}

public interface ParasiteCanSummon {
    EntityType<?>[] getSummonTypes();
    int getMaxSummons();
    int getSummonCooldown();
}

public interface ParasiteCanAOE {
    double getAOERange();
    int getMinTargetsForAOE();
    void performAOEAttack(List<LivingEntity> targets);
}

public interface ParasiteMultiPart {
    List<Entity> getBodyParts();
    void updateBodyParts();
}
```

**使用方式**:
```java
public class IkiAdaptedEntity extends PrimitiveParasiteEntity 
    implements ParasiteCanFly, ParasiteCanShoot {
    
    @Override
    public boolean canFly() {
        return true;
    }
    
    @Override
    public Projectile createProjectile(Level level, LivingEntity target) {
        if (attackCount % 4 == 3) {
            return new ExplosiveProjectile(level, this);
        } else {
            return new PoisonProjectile(level, this);
        }
    }
}
```

**预计工作量**: 15小时

---

### 第三优先级：多部件实体系统

```java
// 多部件实体基类
package alku.csrp.entity;

public abstract class MultiPartParasiteEntity extends PrimitiveParasiteEntity 
    implements ParasiteMultiPart {
    
    private final List<ParasiteBodyPartEntity> bodyParts = new ArrayList<>();
    
    @Override
    public void tick() {
        super.tick();
        updateBodyParts();
    }
    
    @Override
    public void updateBodyParts() {
        for (int i = 0; i < bodyParts.size(); i++) {
            ParasiteBodyPartEntity part = bodyParts.get(i);
            updateBodyPartPosition(part, i);
        }
    }
    
    protected abstract void updateBodyPartPosition(ParasiteBodyPartEntity part, int index);
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 伤害分摊到所有部件
        return super.hurt(source, amount * getDamageMultiplier());
    }
    
    protected float getDamageMultiplier() {
        return 1.0F / (1 + bodyParts.size());
    }
}
```

```java
// 身体部件实体
public class ParasiteBodyPartEntity extends Entity {
    private final MultiPartParasiteEntity parent;
    private final int partIndex;
    
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 转发伤害到父实体
        return parent.hurt(source, amount);
    }
}
```

**应用示例**（Dharma Boss）:
```java
public class DharmaEntity extends MultiPartParasiteEntity {
    private ParasiteBodyPartEntity head;
    private ParasiteBodyPartEntity middle;
    
    @Override
    protected void initBodyParts() {
        head = new ParasiteBodyPartEntity(this, 0);
        middle = new ParasiteBodyPartEntity(this, 1);
        bodyParts.add(head);
        bodyParts.add(middle);
    }
    
    @Override
    protected void updateBodyPartPosition(ParasiteBodyPartEntity part, int index) {
        if (index == 0) { // head
            part.setPos(getX(), getY() + 3.0D, getZ());
        } else { // middle
            part.setPos(getX(), getY() + 1.5D, getZ());
        }
    }
}
```

**预计工作量**: 40小时  
**参考文件**: `EntityBody.java`, `EntityBodyParts.java`

---

## 📅 实施时间表

### 第一阶段（8-12周）- 核心AI系统
| 任务 | 工作量 | 优先级 |
|------|--------|--------|
| 攻击AI系统（10类） | 40h | 🔴 高 |
| 飞行AI系统（6类） | 30h | 🔴 高 |
| 闪避AI系统（4类） | 25h | 🟡 中 |
| 召唤AI系统（3类） | 20h | 🟡 中 |
| 环境交互AI（6类） | 30h | 🟢 低 |
| **小计** | **145h** | |

### 第二阶段（4-6周）- 能力接口与框架
| 任务 | 工作量 | 优先级 |
|------|--------|--------|
| 能力接口系统 | 15h | 🔴 高 |
| 多部件实体系统 | 40h | 🟡 中 |
| Boss信息栏系统 | 10h | 🟢 低 |
| **小计** | **65h** | |

### 第三阶段（8-12周）- 动画与特效
| 任务 | 工作量 | 优先级 |
|------|--------|--------|
| 高级模型动画 | 60h | 🟡 中 |
| 粒子效果系统 | 40h | 🟢 低 |
| 音效系统 | 20h | 🟢 低 |
| **小计** | **120h** | |

**总计**: 330小时（约8-12周全职工作）

---

## 🎯 立即行动计划

### 本周任务（优先级1）
1. ✅ 完成实体分析报告
2. ✅ 完成原模组代码分析
3. 🔄 完成Wiki对比报告
4. 🔄 实现寄染生物固化机制
5. ⏳ 实现 AttackMeleeAOEGoal
6. ⏳ 实现 FlightAttackGoal

### 下周任务（优先级2）
1. 实现 ParasiteCanFly 接口
2. 实现 ParasiteCanShoot 接口
3. 为 IkiAdapted 完整实现飞行+射击
4. 为 PriArachnida 实现拉拽投射物

---

## 📚 参考资源

### 原模组文件位置
- **AI类**: `D:\code\模组反编译器\decompiled\SRParasites-1.10.7\com\dhanantry\scapeandrunparasites\entity\ai\`
- **实体类**: `D:\code\模组反编译器\decompiled\SRParasites-1.10.7\com\dhanantry\scapeandrunparasites\entity\`
- **模型类**: `D:\code\模组反编译器\decompiled\SRParasites-1.10.7\com\dhanantry\scapeandrunparasites\client\models\`

### 当前项目位置
- **实体类**: `D:\code\MC模组\csrp\src\main\java\alku\csrp\entity\`
- **AI类**: `D:\code\MC模组\csrp\src\main\java\alku\csrp\entity\ai\` (需创建)

---

*最后更新: 2026-08-06*
*下次更新: AI系统第一阶段完成后*
