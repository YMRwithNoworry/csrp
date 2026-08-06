# SRParasites 动画资源总结报告

## 一、原模组动画控制器实现模式

### 1.1 渲染器层次结构
原模组使用传统的Minecraft模型系统，**不使用GeckoLib动画控制器**。

**基础渲染器继承链：**
```
RenderMalleable<T> extends RenderSRP<T>
    └── 用于有发光效果的生物（大多数寄生体）
    
RenderCosmical<T> extends RenderSRP<T>
    └── 用于宇宙/特殊生物
    
RenderSRP<T> extends Render<T>
    └── 基础渲染器
```

**示例：RenderHost.java**
```java
public class RenderHost extends RenderMalleable<EntityHost> {
   public static final ResourceLocation TEXTUREM = new ResourceLocation("srparasites:textures/entity/monster/host.png");
   public static final ResourceLocation TEXTURE_FROZEN = new ResourceLocation("srparasites:textures/entity/monster/snowvariants/test.png");

   public RenderHost(RenderManager manager) {
      super(manager, new ModelHost(), 0.0F);
   }

   protected ResourceLocation getEntityTexture(EntityHost entity) {
      switch (entity.getSkin()) {
         case 120:
            return TEXTURE_FROZEN;
         default:
            return TEXTUREM;
      }
   }
}
```

### 1.2 模型实现模式
**示例：ModelHost.java**
```java
public class ModelHost extends ModelSRP {
    // 声明所有模型部件（骨骼）- 通常100+个
    public ModelRenderer mainbody;
    public ModelRenderer bodyl0;
    public ModelRenderer jointB1;
    public ModelRenderer taclejoint00;
    // ... 更多骨骼
    
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, 
                      float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        // 使用原版Minecraft模型渲染
    }
}
```

### 1.3 ModelSRP基类提供的动画工具方法
```java
public abstract class ModelSRP extends ModelBase {
    // 设置旋转角度
    public void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z);
    
    // X轴摆动动画
    public void swingX(ModelRenderer modelRenderer, float speed, float degree, 
                      int invert, float limbSwing, float limbSwingAmount);
    
    // Y轴摆动动画
    public void swingY(ModelRenderer modelRenderer, float speed, float degree, 
                      int invert, float limbSwing, float limbSwingAmount);
    
    // Z轴摆动动画
    public void swingZ(ModelRenderer modelRenderer, float speed, float degree, 
                      int invert, float limbSwing, float limbSwingAmount);
    
    // Y轴位置移动
    public void moveY(ModelRenderer modelRenderer, float speed, int invert, 
                     float f, float f1, float distance);
}
```

**关键发现：原模组使用原版Minecraft的程序化动画，而非GeckoLib的JSON动画系统**

---

## 二、当前项目(CSRP)动画实现模式

### 2.1 使用GeckoLib 4.x动画系统
```java
public abstract class ParasiteGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final float MOVING_ROTATION_SCALE = 0.72F;

    @Override
    public void setCustomAnimations(T animatable, long instanceId, 
                                   AnimationState<T> animationState) {
        // 通过JSON动画文件驱动
        // 支持运动时阻尼旋转效果
        
        if (!animationState.isMoving()) {
            return;
        }
        
        // 对移动中的生物应用旋转阻尼
        for (GeoBone bone : getAnimationProcessor().getRegisteredBones()) {
            bone.setRotX(dampenRotation(initialRotation, animatedRotation));
            // ...
        }
    }
}
```

### 2.2 动画文件结构
**格式：Bedrock Edition 1.8.0 格式**
```json
{
  "format_version": "1.8.0",
  "animations": {
    "animation.生物名.动作名": {
      "loop": true,
      "animation_length": 4.566667,
      "bones": {
        "骨骼名": {
          "rotation": [X表达式, Y表达式, Z表达式],
          "position": [X表达式, Y表达式, Z表达式]
        }
      }
    }
  }
}
```

**Molang表达式特征：**
- `query.anim_time` - 动画时间（秒）
- `math.cos()`, `math.sin()` - 三角函数
- `57.29577951308232` - 弧度转角度常量 (180/π)
- 条件表达式：`(query.anim_time>=X)?(表达式1):(表达式2)`
- `query.anim_time*20` - 转换为游戏刻

---

## 三、可用动画文件清单（124个生物）

### 3.1 按类别分类

#### 适应变种（Adapted）- 13个
- ada_arachnida (蛛形适应体)
- ada_bolster (增强适应体)
- ada_burrower (掘地适应体)
- ada_devourer (吞噬适应体)
- ada_longarms (长臂适应体)
- ada_manducater (咀嚼适应体)
- ada_reeker (恶臭适应体)
- ada_summoner (召唤适应体)
- ada_tozoon (寄生适应体)
- ada_vermin (害虫适应体)
- ada_viscera (内脏适应体)
- ada_yelloweye (黄眼适应体)

#### 原始变种（Primitive）- 12个
- pri_arachnida (原始蛛形体)
- pri_bolster (原始增强体)
- pri_burrower (原始掘地体)
- pri_devourer (原始吞噬体)
- pri_longarms (原始长臂体)
- pri_manducater (原始咀嚼体)
- pri_reeker (原始恶臭体)
- pri_summoner (原始召唤体)
- pri_tozoon (原始寄生体)
- pri_vermin (原始害虫体)
- pri_viscera (原始内脏体)
- pri_yelloweye (原始黄眼体)

#### 同化变种（Assimilated）- 24个
- sim_adventurer / sim_adventurerhead
- sim_bear
- sim_bigspider
- sim_cow / sim_cowhead
- sim_dragone / sim_dragonehead
- sim_enderman / sim_endermanhead
- sim_horse / sim_horsehead
- sim_human / sim_humanhead
- sim_pig / sim_pighead
- sim_sheep / sim_sheephead
- sim_squid
- sim_villager / sim_villagerhead
- sim_wolf / sim_wolfhead

#### 狂暴变种（Feral）- 9个
- fer_bear
- fer_cow
- fer_enderman
- fer_horse
- fer_human
- fer_pig
- fer_sheep
- fer_villager
- fer_wolf

#### 掠夺变种（Marauder）- 6个
- mar_bear
- mar_cow
- mar_enderman
- mar_human
- mar_sheep
- mar_villager

#### 劫持变种（Hijacked）- 3个
- hi_blaze
- hi_golem
- hi_skeleton

#### 召唤物（Beckon）- 4个
- beckon_si (阶段I)
- beckon_sii (阶段II)
- beckon_siii (阶段III)
- beckon_siv (阶段IV)

#### 派遣者（Dispatcher）- 5个
- dispatcher_si (阶段I)
- dispatcher_sii (阶段II)
- dispatcher_siii (阶段III)
- dispatcher_siv (阶段IV)
- dispatcherten (触须)

#### 扎根者（Rooter）- 5个
- rooter_si (阶段I)
- rooter_sii (阶段II)
- rooter_siii (阶段III)
- rooter_siv (阶段IV)
- rooterball (球体)

#### 载体（Carrier）- 4个
- carrier_colony (殖民地)
- carrier_flying (飞行)
- carrier_heavy (重型)
- carrier_light (轻型)

#### 爆破者（Bomber）- 2个
- bomber_heavy (重型)
- bomber_light (轻型)

#### 古代生物（Ancient）- 4个
- anc_dreadnaut (无畏舰)
- anc_dreadnaut_ten (无畏舰触须)
- anc_overlord (霸主)
- anc_pod (投放舱)

#### 基础寄生体 - 20个
- host (宿主)
- hostii (宿主II)
- heed (注意者)
- airscrew (螺旋桨)
- architect (建筑师)
- bogle (鬼怪)
- buglin (小虫)
- crux (关键体)
- crux_incomplete (不完整关键体)
- draconite (龙化体)
- dredge (疏浚者)
- gnat (蚊蚋)
- grunt (咕哝者)
- haunter (缠扰者)
- incompleteform_medium (中型不完整体)
- incompleteform_small (小型不完整体)
- lice (虱子)
- mangler (撕裂者)
- marauder (掠夺者)
- movingflesh (活动血肉)

#### 高级寄生体 - 13个
- abo_bodies (憎恶躯体)
- kirin (麒麟)
- kyphosis (驼背)
- monarch (君主)
- overseer (监督者)
- rupter (破裂者)
- seizer (抓捕者)
- sentry (哨兵)
- succor (援助者)
- thrall (奴仆)
- vigilante (自警者)
- warden (守望者)
- worker (工作者)
- worm (蠕虫)
- wraith (幽灵)

---

## 四、动画命名规范总结

### 4.1 标准动画类型
```
animation.{生物ID}.idle             - 待机动画（循环）
animation.{生物ID}.walk             - 行走动画（循环）
animation.{生物ID}.attack           - 攻击动画
animation.{生物ID}.death            - 死亡动画
```

### 4.2 状态修饰动画
```
animation.{生物ID}.idle.get_parasite_status_1    - 感染状态1（轻度）
animation.{生物ID}.walk.get_parasite_status_1    - 行走+感染状态1
animation.{生物ID}.walk.get_parasite_status_2    - 行走+感染状态2（中度）
animation.{生物ID}.idle.get_parasite_status_3    - 感染状态3（重度）
```

### 4.3 特殊动画修饰
```
animation.{生物ID}.idle.get_still_ani_1                        - 静止动画变体1
animation.{生物ID}.get_attack_timer                            - 攻击计时器
animation.{生物ID}.get_attack_timer.get_parasite_status_1      - 攻击+感染状态1
animation.{生物ID}.get_attack_timer.get_still_ani_1            - 攻击计时+静止
```

### 4.4 命名模式
**前缀缩写：**
- `ada_` - Adapted（适应）
- `pri_` - Primitive（原始）
- `sim_` - Assimilated（同化）
- `fer_` - Feral（狂暴）
- `mar_` - Marauder（掠夺）
- `hi_` - Hijacked（劫持）
- `anc_` - Ancient（古代）

**阶段后缀：**
- `_si` - Stage I（阶段1）
- `_sii` - Stage II（阶段2）
- `_siii` - Stage III（阶段3）
- `_siv` - Stage IV（阶段4）

---

## 五、动画数据特征分析

### 5.1 典型动画时长
- idle（待机）：4-10秒
- walk（行走）：4-8秒
- attack（攻击）：1-5秒
- 感染状态动画：通常比基础动画慢20-50%

### 5.2 常用骨骼命名模式
```
mainbody          - 主躯体
joint{部位}{编号} - 关节（如jointRA1 = 右臂关节1）
hair_joint{部位}  - 毛发关节
tackle_joint{编号} - 触须关节
```

**关节命名规律：**
- `R` = Right (右)
- `L` = Left (左)
- `F` = Front (前)
- `B` = Back (后)
- `M` = Middle (中)
- `U` = Upper (上)
- `D` = Down (下)

示例：
- `jointRA` = Right Arm (右臂)
- `jointFRLY` = Front Right Leg Y
- `jointMBRA` = Middle Back Right Arm

### 5.3 动画复杂度
- **简单生物**（如lice）：5-10个动画骨骼
- **中等生物**（如host）：30-80个动画骨骼
- **复杂生物**（如ada_arachnida）：50-100+个动画骨骼

### 5.4 Molang表达式模式

**周期性摆动：**
```javascript
(-1*((math.cos((((query.anim_time*20)*速度)*57.29577951308232))*幅度)*57.29577951308232))
```
- `query.anim_time*20` - 转换为游戏刻
- `速度` - 控制摆动频率（如0.24表示较慢）
- `幅度` - 控制摆动范围（如0.3表示30%）
- `57.29577951308232` = 180/π（弧度转角度）

**垂直位移（上下弹跳）：**
```javascript
(-1*(math.cos((((query.anim_time*20)*速度)*57.29577951308232))*距离))
```

**条件过渡动画（时间触发）：**
```javascript
((query.anim_time>=触发时间)?(三次贝塞尔插值):(默认值))
```
使用三次贝塞尔曲线实现平滑过渡：
```javascript
(系数1*((((query.anim_time-起始时间)/持续时间)^3)) + 
(系数2*((((query.anim_time-起始时间)/持续时间)^2)))
```

---

## 六、典型动画示例分析

### 6.1 ada_arachnida.walk 动画
```json
{
  "animation.ada_arachnida.walk": {
    "loop": true,
    "animation_length": 7.85,
    "bones": {
      "mainbody": {
        "rotation": [
          "(-1*((0.08*math.cos((((query.anim_time*20)*0.24)*57.29577951308232)))*57.29577951308232))",
          0,
          0
        ],
        "position": [
          0,
          "(-1*(math.cos((((query.anim_time*20)*0.48)*57.29577951308232))*0.04))",
          0
        ]
      },
      "jointRA1": {
        "rotation": [
          0,
          "((-0.3*math.cos(((((query.anim_time*20)*0.24)+-1)*57.29577951308232)))*57.29577951308232)",
          0
        ]
      }
    }
  }
}
```

**解析：**
- 主躯体X轴旋转：速度0.24，幅度0.08（轻微俯仰）
- 主躯体Y轴位移：速度0.48（是旋转速度的2倍），距离0.04（上下弹跳）
- 右臂关节Y轴旋转：速度0.24，幅度0.3，相位-1（延迟）

### 6.2 pri_longarms.get_attack_timer 动画
```json
{
  "animation.pri_longarms.get_attack_timer": {
    "loop": true,
    "animation_length": 4,
    "bones": {
      "jointLA": {
        "rotation": [
          "(-1*((((0.5*(1-math.cos(((6.283185*min(1,query.anim_time))*57.29577951308232))))>0)?
            (0-min(0.4,(0.5*(1-math.cos(...))))):0)*57.29577951308232))",
          "((((0.5*(1-math.cos(...)))>0)?(0-min(2,...)):0)*57.29577951308232)",
          0
        ]
      }
    }
  }
}
```

**解析：**
- 使用余弦函数的反转 `1-cos` 创建0→1→0的过渡
- `6.283185` = 2π（完整周期）
- `min(1, query.anim_time)` 限制在第1秒内完成动作
- 条件表达式确保只在正值时应用动画

---

## 七、关键路径总结

| 类型 | 路径 |
|------|------|
| 原模组反编译代码 | `D:\code\模组反编译器\decompiled\SRParasites-1.10.7` |
| 反编译渲染器 | `...\com\dhanantry\scapeandrunparasites\client\renderer\entity` |
| 反编译模型 | `...\com\dhanantry\scapeandrunparasites\client\model\entity` |
| 提取的动画资源 | `D:\code\MC模组\srp生物模型和动画提取\提取结果` |
| CSRP项目动画目录 | `D:\code\MC模组\csrp\src\main\resources\assets\csrp\animations` |
| CSRP项目模型目录 | `D:\code\MC模组\csrp\src\main\resources\assets\csrp\geo` |
| CSRP渲染器代码 | `D:\code\MC模组\csrp\src\main\java\alku\csrp\client\renderer` |
| CSRP模型代码 | `D:\code\MC模组\csrp\src\main\java\alku\csrp\client\model` |

---

## 八、文件清单统计

- **总生物数量**：124个
- **动画文件总数**：124个 .animation.json
- **模型文件总数**：124个 .geo.json
- **当前项目已导入**：约40个
- **待导入**：约84个

---

## 九、迁移建议

### 9.1 原模组→CSRP适配方案
1. **保留JSON动画**：已提取的.animation.json文件可直接使用
2. **GeckoLib兼容**：Bedrock 1.8.0格式与GeckoLib 4.x完全兼容
3. **需要调整的内容**：
   - 动画播放速度可能需要微调
   - 运动阻尼系数（当前为0.72F）可按需调整
   - 状态动画的触发条件需要在代码中实现

### 9.2 命名映射规则
原模组Entity类名 → CSRP动画ID：
```
EntityHost → host
EntityBanoAdapted → ada_[具体变种]
EntityPrimitiveLongarms → pri_longarms
EntitySimHuman → sim_human
```

### 9.3 实现步骤
1. 从提取结果目录复制对应生物的 .animation.json 和 .geo.json
2. 创建对应的 GeoModel 类继承 ParasiteGeoModel
3. 创建对应的 Renderer 类继承 ParasiteGeoRenderer
4. 在 AnimationController 中配置动画状态机
5. 测试并调整动画参数

---

## 十、代码示例：如何使用动画

### 10.1 创建GeoModel
```java
public class HostModel extends ParasiteGeoModel<HostEntity> {
    @Override
    public ResourceLocation getModelResource(HostEntity entity) {
        return new ResourceLocation("csrp", "geo/host.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HostEntity entity) {
        return new ResourceLocation("csrp", "textures/entity/host.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HostEntity entity) {
        return new ResourceLocation("csrp", "animations/host.animation.json");
    }
}
```

### 10.2 配置动画控制器
```java
@Override
public void registerControllers(AnimationData data) {
    data.addAnimationController(new AnimationController<>(this, "controller", 0, event -> {
        if (event.isMoving()) {
            event.getController().setAnimation(
                RawAnimation.begin().thenLoop("animation.host.walk")
            );
        } else {
            event.getController().setAnimation(
                RawAnimation.begin().thenLoop("animation.host.idle")
            );
        }
        return PlayState.CONTINUE;
    }));
}
```

---

## 总结

原模组使用传统的程序化动画系统，而CSRP项目已成功转换为基于GeckoLib的JSON动画系统。124个生物的完整动画资源已提取完毕，采用Bedrock Edition 1.8.0格式，使用Molang表达式驱动骨骼变换。动画命名遵循清晰的规范，支持多种状态组合（idle/walk/attack + 感染状态1/2/3）。当前项目已导入约40个生物动画，剩余84个可直接复制使用。
