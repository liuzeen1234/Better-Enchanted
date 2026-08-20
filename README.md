# Better Enchanted

> Fabric Mod | Minecraft 1.20.4 | Java 17  
> 将原版附魔效果迁移到食物和药水上，让食物与药水也能拥有附魔能力。

---

## 模组简介

**Better Enchanted** 是一个 Minecraft Fabric 模组，核心玩法是将原版武器、弓、弩、三叉戟等装备的附魔效果创造性地应用到食物和投掷药水上。食物附魔在食用时对食用者生效，药水附魔在投掷命中时对目标生效。所有效果的数值公式尽可能参考 MC 1.20.4 原版附魔机制，确保平衡性和一致性。

---

## 一、食物类附魔

食物附魔对所有可食用物品生效，同时兼容蛋糕方块（蛋糕使用专门的方块附魔存储系统）。

### 1.1 锋利 (Sharpness)

- **效果**：食用时对食用者造成伤害
- **公式**：`damage = 0.5 × level + 0.5`
  - 锋利 I = 1.0 伤害，锋利 II = 1.5，锋利 III = 2.0，锋利 IV = 2.5，锋利 V = 3.0
- **自定义伤害源**：拥有独立的死亡消息（"XXX had their throat cut by sharp food"）
- **蛋糕支持**：通过 CakeEnchantmentStorage 读取蛋糕位置的锋利等级

### 1.2 击退 (Knockback)

- **效果**：食用时对食用者施加击退
- **方向**：随机方向（360°随机角度），模拟被食物"弹飞"的效果
- **强度公式**：
  - 水平方向：`strength = 0.5 × level`
  - 竖直方向：若在地面上则给予一个小跳跃（最大0.4）
- **参考**：改编自 MC 1.20.4 `PlayerEntity.attack()` 中的击退逻辑

### 1.3 火焰附加 (Fire Aspect)

- **效果**：食用时点燃食用者
- **时长**：`level × 4` 秒
  - 火焰附加 I = 4秒，火焰附加 II = 8秒
- **参考**：与 MC 1.20.4 原版火焰附加逻辑完全一致（`target.setOnFireFor(level * 4)`）

### 1.4 效率 (Efficiency)

- **效果**：加快食用速度
- **公式**：每级减少10%进食时间
  - 效率 I = 加速10%，效率 V = 加速50%
- **实现**：通过 Mixin `Item.getMaxUseTime()` 减少食用 tick 数

### 1.5 冰霜行者 (Frost Walker)

- **效果**：进食后获得冰霜行者效果
- **时长**：基础20秒 + 每级额外10秒
  - 冰霜行者 I = 30秒，冰霜行者 II = 40秒，冰霜行者 III = 50秒
- **脚下结冰**：效果持续期间，玩家脚下的水面会结成霜冰
- **持续伤害**：每4秒受1点霜冻伤害，且每级减少0.5秒伤害间隔
  - 冰霜行者 I = 每3.5秒1点，冰霜行者 II = 每3秒1点
- **视觉效果**：血条显示为蓝色冰冻样式
- **可配置**：可通过调试菜单开关冰霜行者日志

### 1.6 耐久 (Unbreaking)

- **效果**：食用后有概率不消耗食物
- **概率公式**：参考 MC 1.20.4 原版耐久公式
  - 消耗概率 = `1 / (level + 1)`
  - 即不消耗概率 = `level / (level + 1)`
  - 耐久 I = 50%不消耗，耐久 II = 66.7%，耐久 III = 75%
- **蛋糕特殊处理**：耐久触发时会恢复蛋糕的一口（BITES减1），如果蛋糕刚好被吃完则恢复为最后一口状态

---

## 二、药水类附魔 — 武器/弓系

药水附魔在投掷型药水（喷溅药水/滞留药水）被掷出并命中时触发。所有伤害/效果对直接命中的实体全额生效，对溅射范围（4格）内的实体按距离衰减生效。

### 2.1 锋利 (Sharpness) [药水]

- **效果**：掷出的药水砸中实体造成伤害
- **公式**：`damage = 0.5 × level + 0.5`（与食物锋利一致）
- **溅射**：4格范围内有距离衰减伤害（`damage × (1 - distance/4)`）
- **叠加**：与力量附魔的伤害叠加生效
- **自定义伤害源**：独立死亡消息（"XXX was shattered by a sharp potion"）

### 2.2 力量 (Power) [药水]

- **效果**：掷出的药水砸中实体造成伤害
- **公式**：参考 MC 1.20.4 弓的 Power 附魔
  - `damage = level + 1`（模拟箭矢 base=2 时的力量加成）
  - 力量 I = 2.0，力量 II = 3.0，力量 III = 4.0，力量 IV = 5.0，力量 V = 6.0
- **叠加**：与锋利附魔伤害直接相加
- **伤害源**：当力量伤害≥锋利伤害时，使用力量专属死亡消息（"XXX was obliterated by a powerful potion"）

### 2.3 冲击 (Punch) [药水]

- **效果**：掷出的药水砸中实体造成击退
- **公式**：参考 MC 1.20.4 弓冲击附魔（`AbstractArrowEntity.onHit`）
  - `knockbackStrength = punchLevel × 0.6`
- **方向**：
  - 直接命中：沿药水飞行方向（水平分量归一化）击退
  - 溅射范围：从药水落点指向实体的方向击退，强度按距离衰减

### 2.4 火矢 (Flame) [药水]

- **效果**：掷出的药水砸中实体时点燃目标
- **直接命中**：着火5秒（100 ticks），与 MC 1.20.4 弓火矢规则一致
- **溅射范围**：着火时长按距离衰减（`5 × (1 - distance/4)` 秒，向上取整）
- **等级**：仅1级（与原版一致）

### 2.5 引雷 (Channeling) [药水]

- **效果**：基于 MC 1.20.4 三叉戟引雷规则改良为 AOE 版本
- **条件**：
  1. 必须为雷暴天气（`world.isThundering()`）
  2. 目标实体位置必须能看到天空（`isSkyVisible`）
- **范围**：药水落点周围4格范围内的所有活着实体
- **结果**：每个满足条件的实体位置各召唤一道闪电
- **闪电归属**：闪电的 channeler 设为投掷者（影响掉落物和经验判定）
- **等级**：仅1级（与原版一致）

---

## 三、药水类附魔 — 弩系

### 3.1 多重射击 (Multishot) [药水]

- **效果**：一次投掷产生多瓶药水，以圆锥散布
- **数量公式**：总投掷物 = `2 + level`
  - 多重射击 I = 3瓶，多重射击 II = 4瓶，多重射击 III = 5瓶
- **分布规则**：
  - 第1瓶（主弹）沿瞄准方向直飞（圆锥中心）
  - 前8个额外投掷物均匀分布在10°圆锥圆周上
  - 超过8个的部分向圆锥内部（0°~10°）随机分布（使用 sqrt 保证面积均匀）
- **消耗**：只消耗1瓶
- **兼容性**：完全兼容迅投附魔的所有模式（物理投掷/射线追踪）

### 3.2 快速装填 (Quick Charge) [药水]

- **效果**：减少无限附魔的冷却时间
- **公式**：每级减少20%冷却时间（基础30秒）
  - Lv I = 24秒，Lv II = 18秒，Lv III = 12秒，Lv IV = 6秒，Lv V = 0秒（无冷却）
- **前提**：需配合无限附魔使用，单独使用无效果

### 3.3 穿透 (Piercing) [药水]

- **效果**：参考 MC 1.20.4 弩穿透规则
- **机制**：
  - 药水可穿过 `level` 个实体（总共命中 `level + 1` 个）
  - 穿透时对每个实体执行附魔效果（锋利/力量/冲击/火矢）
  - 已穿透的实体不会被重复命中
  - 命中方块或达到穿透上限时正常销毁并触发溅射效果

---

## 四、药水类附魔 — 三叉戟系

### 4.1 忠诚 (Loyalty) [药水]

- **效果**：参考 MC 1.20.4 三叉戟忠诚规则改编
- **机制**：
  - 药水掷出2秒后自动返回投掷者
  - 返回速度 = `0.5 × level` 格/tick
    - Lv I = 10格/秒，Lv II = 20格/秒，Lv III = 30格/秒
  - 掷出/返回途中碰到实体或方块则正常消耗（不会返回）
  - 成功返回则归还药水物品给玩家
- **消耗逻辑**：投掷时立即消耗药水（因为实体自身携带返回逻辑）

---

## 五、药水通用附魔

### 5.1 无限 (Infinity) [药水]

- **效果**：投掷药水不消耗
- **冷却系统**：
  - 基于 NBT 标记（`InfinityMarked`）的自定义冷却系统
  - 只影响带无限附魔的物品，不影响同种普通物品
  - 基础冷却时间：30秒（受快速装填减少）
- **与耐久协作**：
  - 有耐久时进行判定：成功 → 不进入冷却，失败 → 进入冷却
  - 无耐久时：始终进入冷却
- **客户端表现**：通过网络同步显示灰色半透明冷却动画覆盖
- **冷却中拦截**：冷却期间无法再次使用该物品

### 5.2 耐久 (Unbreaking) [药水]

- **效果**：投掷后有概率不消耗药水
- **公式**：与食物耐久一致（消耗概率 = `1/(level+1)`）
- **与无限配合**：判定结果决定是否触发冷却（见上）

---

## 六、自定义附魔

### 6.1 迅投 (Swift Throw)

- **效果**：提升药水投掷速度和弹道平直度
- **速度公式**：`实际初速度 = 原始初速度(0.5) × (1 + 0.5 × level)`
- **角度公式**：向上偏移角度 `y = 80 / (4 + level)` 度
  - 等级越高，弹道越接近平射
  - 偏移角度<1°时直接设为0（完全平射）
- **两种模式**：
  - **等级 1-20（物理投掷模式）**：正常物理投掷，速度和方向按公式计算，受重力影响
  - **等级 >20（射线追踪模式）**：瞬间命中，通过 NBT 标记让投射物每 tick 做射线追踪传送；隐藏药水实体，无重力；沿发射方向每0.5格生成暴击粒子（最远64格）
- **兼容性**：与多重射击完全兼容，额外投掷物也使用相同的迅投模式

---

## 七、超级附魔金苹果

### 7.1 物品概述

| 属性 | 说明 |
|------|------|
| 物品ID | `hello-mod:super_enchanted_golden_apple` |
| 文字颜色 | 浅紫色 (LIGHT_PURPLE) |
| 自带附魔 | 迅投 (Swift Throw) 25 |
| 获取方式 | 3×3工作台合成（金块×4 + 喷溅型药水×2 + 滞留型药水×2 + 金苹果×1）|

### 7.2 双模式切换

- 手持左键切换食用/投掷模式，**5 游戏刻**冷却
- 名称动态变化：食用模式"超级附魔金苹果"，投掷模式"投掷型超级附魔金苹果"

### 7.3 食用模式

- 食用时间32 tick（1.6秒），受效率附魔加速
- 食用效果 = 基础效果 + 喷溅型药水效果 + 滞留型药水效果（100%持续时间）
- **基础效果**：生命恢复V(30s) + 吸收IV(2min) + 抗性I(5min) + 抗火I(5min)

### 7.4 投掷模式

- 自带迅投25（射线追踪瞬达）
- 落地后先触发**喷溅效果**（基础buff + 喷溅型药水效果，按距离衰减），再生成**效果云**（基础buff + 滞留型药水效果）
- 附魔额外效果（锋利/力量/冲击/火矢/引雷/多重射击）均可生效

### 7.5 药水效果存储

- 合成时读取配方中喷溅型/滞留型药水的效果，按规则合并后存入NBT
- Tooltip中显示完整的喷溅效果和效果云信息

### 7.6 铁砧兼容

- **无视 "Too Expensive" 39级上限**：超级附魔金苹果在铁砧操作时无视原版经验花费39级上限限制，始终可以附魔
- RepairCost锁定为10，防止惩罚指数增长
- 可通过铁砧附加额外附魔（锋利/力量/冲击/火矢/无限/耐久/快速装填/引雷/多重射击/穿透/忠诚等）

---

## 八、终极附魔金苹果

### 8.1 物品概述

| 属性 | 说明 |
|------|------|
| 物品ID | `hello-mod:ultimate_enchanted_golden_apple` |
| 文字颜色 | 亮金色 (GOLD) |
| 自带附魔 | 效率 9、耐久 10、迅投 25、无限 1 |
| 获取方式 | 自定义进度奖励（超级附魔金苹果集齐14种附魔满级）|

### 8.2 模式切换

- 手持左键切换食用/投掷模式，**2 游戏刻**冷却（比超级版更快）
- 名称动态变化：食用模式"终极附魔金苹果"，投掷模式"投掷型终极附魔金苹果"

### 8.3 食用模式

- 效率9使食用时间约3 tick（近乎瞬吃）
- **固定效果**（无药水NBT）：

| 效果 | 等级 | 持续时间 |
|------|------|----------|
| 生命恢复 (Regeneration) | V | 60秒 |
| 抗性提升 (Resistance) | III | 60秒 |
| 力量 (Strength) | V | 60秒 |

- **消耗规则**：耐久10判定成功（91%概率）→ 不消耗 + 3秒使用冷却；判定失败（9%概率）→ 消耗1个，无冷却

### 8.4 投掷模式

- 迅投25（>20级）→ 射线追踪瞬达模式
- **落地后4格范围检测**：
  - 敌对生物：100点真实伤害（无视护甲）+ 无条件召唤闪电
  - 友好生物/玩家：生命恢复 V (60秒)
- 附魔额外效果（锋利/力量/冲击/火矢/引雷）对全范围实体有效
- **消耗规则**：自带无限+耐久10，投掷不消耗进入30s冷却（受快速装填减免），耐久判定成功免冷却

### 8.5 铁砧兼容

- 与超级版共享铁砧机制：**无视 "Too Expensive" 39级上限**，RepairCost锁定为10
- 可通过铁砧附加额外附魔（与超级版相同的附魔列表）

### 8.6 进度获取条件

- 将一个超级附魔金苹果的14种有效附魔全部附至合法最高等级：
  - 迅投25、锋利V、力量V、冲击II、火矢I、引雷I、多重射击10、无限I、耐久III、快速装填III、击退II、火焰附加II、效率V、冰霜行者II
- 触发时机：玩家在铁砧中取出满足条件的超级附魔金苹果时，由 `SuperAppleAnvilMixin` 检测并授予进度
- 进度完成后由 `AdvancementRewardMixin` 发放奖励：1个终极附魔金苹果（自带4种附魔）+ 1000经验等级
- 如果玩家背包已满，奖励物品会掉落在地面

---

## 九、辅助功能

### 9.1 实体血量 HUD

- 准星对准实体时在屏幕上显示目标的当前血量 / 最大血量
- 检测距离可配置（默认128格）
- 可通过调试菜单独立开关

### 9.2 手持物品 HUD

- 屏幕左上角显示主手物品名称和数量
- **高级模式**：额外显示物品耐久度和完整 NBT 标签数据
- 可通过调试菜单独立开关

### 9.3 调试菜单

- 通过自定义按键（默认无绑定，可在按键设置中配置）打开
- 提供以下子菜单：
  - 实体血量显示设置（开关 + 检测距离调节）
  - 手持物品显示设置（开关 + 高级模式开关）
  - 调试日志开关（独立控制蛋糕/放置/食物/药水/伤害/迅投/客户端/冰霜行者/无限冷却共9个日志模块）

### 9.4 配置持久化

- 配置文件位于 `config/hello-mod.json`
- 所有设置变更自动保存，启动时自动加载
- 格式为可读的 JSON，支持手动编辑

---

## 十、技术架构

### 10.1 核心技术栈

| 组件 | 技术选型 |
|------|----------|
| Mod框架 | Fabric Loader 0.15.3 + Fabric API 0.97.1 |
| 游戏版本 | Minecraft 1.20.4 |
| Java版本 | Java 17 |
| 构建工具 | Gradle + Fabric Loom 1.7 |
| 代码修改 | Mixin 注入 |
| 映射表 | Yarn 1.20.4+build.3:v2 |

### 10.2 Mixin 注入点

| Mixin 类 | 目标 | 用途 |
|-----------|------|------|
| PlayerEatFoodMixin | `PlayerEntity.eatFood()` | 食物附魔效果触发（锋利/击退/火焰/冰霜） |
| CakeBlockMixin | `CakeBlock.tryEat()` | 蛋糕附魔效果触发 |
| CakePlaceMixin | `CakeBlock.onPlaced()` | 放置蛋糕时存储附魔数据 |
| EfficientEatingMixin | `Item.getMaxUseTime()` | 效率附魔加速食用 |
| UnbreakingFoodMixin | `PlayerEntity.eatFood()` RETURN | 食物耐久判定 |
| PotionItemMixin | `ThrowablePotionItem.use()` | 药水投掷逻辑（无限/耐久/多重射击/消耗） |
| PotionEntityMixin | `PotionEntity.onCollision()` | 药水命中效果（锋利/力量/冲击/火矢/引雷） |
| SwiftThrowTickMixin | `PotionEntity.tick()` | 迅投射线追踪模式 |
| MultishotEnchantmentMixin | `Enchantment.getMaxLevel()` | 多重射击最高等级提升至10级 |
| PiercingPotionMixin | `PotionEntity` 碰撞 | 穿透逻辑 |
| LoyaltyPotionMixin | `ThrownEntity.tick()` | 忠诚返回逻辑 |
| LoyaltyCollisionMixin | `PotionEntity` 碰撞 | 忠诚返回途中碰撞处理 |
| LivingEntityDamageCooldownMixin | `LivingEntity.damage()` | 药水/闪电伤害无敌帧清除 |
| SuperAppleCraftingMixin | `CraftingScreenHandler.updateResult` | 超级金苹果合成时添加迅投+药水数据 |
| SuperAppleAnvilMixin | `AnvilScreenHandler.updateResult` | 铁砧无视39级上限+RepairCost锁定+进度触发 |
| AdvancementRewardMixin | `PlayerAdvancementTracker.grantCriterion()` | 终极金苹果进度完成奖励发放 |
| client/DrawContextCooldownMixin | `DrawContext.drawItemInSlot()` | 无限冷却覆盖渲染 |
| client/SwiftThrowRenderMixin | 客户端 | 射线追踪模式隐藏药水实体 |
| client/SuperAppleAttackMixin | `MinecraftClient.doAttack()` | 金苹果左键模式切换 |

### 10.3 包结构

```
com.example.hellomod
├── HelloMod.java                    # 主入口，注册事件和系统
├── advancement/                     # 进度判定
│   └── UltimateAppleChecker.java    # 终极金苹果附魔检查工具类
├── block/                           # 蛋糕附魔存储、方块实体
│   ├── CakeEnchantmentStorage.java  # 内存中的蛋糕附魔数据管理
│   ├── EnchantedCakeBlockEntity.java
│   └── HelloModBlockEntities.java
├── client/                          # 客户端功能
│   ├── HelloModClient.java          # 客户端入口
│   ├── EmptyEntityRenderer.java     # 空渲染器（投掷实体不可见，避免高速视觉bug）
│   ├── EntityHealthHud.java         # 实体血量HUD
│   ├── InfinityCooldownClientState.java # 无限冷却客户端同步
│   ├── DebugMenuScreen.java         # 调试菜单界面
│   ├── DebugLogSettingScreen.java   # 日志开关界面
│   ├── HealthHudSettingScreen.java  # 血量HUD设置界面
│   └── ItemHudSettingScreen.java    # 物品HUD设置界面
├── config/                          # 配置持久化
│   └── ModConfig.java
├── damage/                          # 自定义伤害源
│   ├── ModDamageTypes.java
│   ├── SharpFoodDamageSource.java
│   ├── SharpPotionDamageSource.java
│   ├── PowerPotionDamageSource.java
│   └── UltimateAppleDamageSource.java
├── debug/                           # 调试日志控制
│   └── DebugLogConfig.java
├── effect/                          # 自定义效果
│   └── FrostWalkerFoodEffect.java   # 冰霜行者食物效果（tick事件）
├── enchantment/                     # 附魔系统
│   ├── ModEnchantments.java         # 自定义附魔注册
│   ├── SwiftThrowEnchantment.java   # 迅投附魔定义
│   ├── InfinityCooldownManager.java # 无限冷却服务端管理
│   └── InfinityCooldownSync.java    # 冷却状态网络同步
├── entity/                          # 自定义实体
│   ├── ModEntities.java             # 实体注册
│   ├── SuperGoldenAppleEntity.java  # 超级金苹果投掷实体
│   └── UltimateGoldenAppleEntity.java # 终极金苹果投掷实体
├── item/                            # 自定义物品
│   ├── ModItems.java                # 物品注册
│   ├── SuperEnchantedGoldenAppleItem.java  # 超级附魔金苹果
│   ├── UltimateEnchantedGoldenAppleItem.java # 终极附魔金苹果
│   ├── SuperAppleCraftingHandler.java      # 合成工具（旧，兼容）
│   └── SuperApplePotionMerger.java         # 药水效果合并工具
├── network/                         # 网络通信
│   ├── SuperAppleModeSwitchC2SPacket.java  # 金苹果模式切换同步
│   ├── EntityNbtRequestC2SPacket.java      # 实体NBT请求（C2S）
│   ├── EntityNbtResponseS2CPacket.java     # 实体NBT响应（S2C）
│   └── EntityNbtCache.java                 # 客户端NBT缓存（500ms过期）
└── mixin/                           # 所有Mixin注入类
    ├── client/                      # 客户端Mixin
    └── ...                          # 见上表
```

### 10.4 关键设计决策

1. **蛋糕特殊处理**：蛋糕是方块而非物品，无法直接读取 ItemStack NBT。采用 `CakeEnchantmentStorage` 将附魔数据绑定到方块坐标，放置时写入、吃完时清除。

2. **药水附魔数据传递**：附魔存储在药水 ItemStack 的 NBT 中，投掷时通过 `potionEntity.setItem(stack)` 将完整 ItemStack 传递给投射物实体，命中时通过 `EnchantmentHelper.getLevel()` 从实体的 stack 中读取。

3. **无限冷却系统**：不使用原版 `ItemCooldownManager`（会影响同类所有物品），而是自定义基于玩家 UUID 的冷却管理器 + NBT 标记（`InfinityMarked`），仅影响带标记的特定物品。

4. **迅投射线追踪**：高等级迅投（>20）时物理投掷速度过快会导致穿墙/碰撞检测失效，改为在 tick 中做逐步射线追踪传送，确保碰撞判定正确。

5. **铁砧无视 "Too Expensive" 上限**：原版铁砧在经验花费超过39级时拒绝操作。`SuperAppleAnvilMixin` 通过在 `updateResult` 开始时临时将玩家标记为创造模式来绕过此检查，结束后恢复原始状态，确保超级/终极金苹果始终可在铁砧操作。

6. **投掷实体空渲染**：超级/终极金苹果的投掷实体使用 `EmptyEntityRenderer`（`shouldRender()` 返回 false），因为迅投25级射线追踪模式下实体速度极快，渲染贴图会产生视觉异常。玩家看到的是暴击粒子弹道而非飞行中的物品实体。

7. **药水/闪电伤害无敌帧移除**：`LivingEntityDamageCooldownMixin` 在 `damage()` 方法头部检测伤害源，若为 `sharp_potion`、`power_potion` 或 `lightning_bolt`，则重置 `timeUntilRegen` 和 `hurtTime` 为 0，确保同一 tick 内多种药水附魔伤害和引雷伤害可以正确叠加。

8. **多重射击等级上限修改**：`MultishotEnchantmentMixin` 注入 `Enchantment.getMaxLevel()`，将多重射击附魔的原版最高等级从1级提升至10级，以支持更多的散布投掷物。

9. **实体 NBT 网络同步**：实体详细信息 HUD 需要显示服务端的完整 NBT 数据（含药水效果），通过 `EntityNbtRequestC2SPacket`（客户端请求）→ `EntityNbtResponseS2CPacket`（服务端响应）→ `EntityNbtCache`（客户端缓存，500ms 过期）三者协作实现跨端数据同步。

---

## 十一、附魔效果一览表

| 附魔 | 食物 | 药水 | 最高等级 | 备注 |
|------|:----:|:----:|:--------:|------|
| 锋利 (Sharpness) | ✅ | ✅ | V | 食物伤食用者，药水伤目标 |
| 击退 (Knockback) | ✅ | — | II | 随机方向 |
| 火焰附加 (Fire Aspect) | ✅ | — | II | 每级4秒 |
| 效率 (Efficiency) | ✅ | — | V | 每级-10%进食时间 |
| 冰霜行者 (Frost Walker) | ✅ | — | III | 结冰+持续伤害 |
| 耐久 (Unbreaking) | ✅ | ✅ | III | 概率不消耗 |
| 力量 (Power) | — | ✅ | V | 与锋利叠加 |
| 冲击 (Punch) | — | ✅ | II | 沿飞行方向击退 |
| 火矢 (Flame) | — | ✅ | I | 着火5秒 |
| 无限 (Infinity) | — | ✅ | I | 不消耗+冷却 |
| 引雷 (Channeling) | — | ✅ | I | 雷暴AOE闪电 |
| 多重射击 (Multishot) | — | ✅ | 10 | 圆锥散布（原版上限1级，通过Mixin提升至10级） |
| 快速装填 (Quick Charge) | — | ✅ | V | 减少无限冷却 |
| 穿透 (Piercing) | — | ✅ | IV | 穿过多个实体 |
| 忠诚 (Loyalty) | — | ✅ | III | 2秒后返回 |
| 迅投 (Swift Throw) | — | ✅ | ∞ | 自定义附魔 |

---

## 十二、运行环境与依赖

- **Minecraft**：1.20.4
- **Fabric Loader**：≥ 0.15.0
- **Fabric API**：任意兼容版本
- **Java**：≥ 17
- **许可证**：MIT
