# 超级附魔金苹果 — 设计文档

> Mod: More Good Enchanted (Fabric / MC 1.20.4 / Java 17)

---

## 一、物品概述

| 属性 | 说明 |
|------|------|
| 物品名称（食用模式） | 超级附魔金苹果 |
| 物品名称（投掷模式） | 投掷型超级附魔金苹果 |
| 贴图 | MC 1.8 版本附魔金苹果贴图（紫色光效版） |
| 合成后自带附魔 | 迅投 (Swift Throw) 25 |
| 消耗规则 | 使用后消耗 1 个，受已有附魔效果影响（耐久/无限等） |

---

## 二、合成配方

3×3 工作台：

```
金块          滞留型药水    金块
喷溅型药水     金苹果      喷溅型药水
金块          滞留型药水    金块
```

- 四角：金块 ×4
- 上、下中：滞留型药水 ×2（任意药水类型均可，类型决定效果云给予的额外效果）
- 左、右中：喷溅型药水 ×2（任意药水类型均可，类型决定喷溅给予的额外效果）
- 正中心：金苹果 ×1

产出：超级附魔金苹果 ×1（自带迅投 25 附魔，NBT 中存储药水效果数据）

### 合成时药水效果存储规则

合成时读取配方中喷溅型药水和滞留型药水的药水效果，按以下规则合并后存入产物 NBT：

**持续性效果合并规则：**
- 两瓶药水类型相同、等级相同 → 持续时间叠加
- 两瓶药水类型相同、等级不同 → 只保留高等级的效果（时间取高等级那瓶的时间）
- 两瓶药水类型不同 → 两个效果都保留，各自独立

**瞬时效果（如治疗、伤害）：**
- 不受上述合并规则影响
- 每瓶药水的瞬时效果单独处理一次（即两瓶瞬时治疗 = 触发两次治疗）

---

## 三、模式切换机制

| 项目 | 说明 |
|------|------|
| 切换方式 | 手持超级附魔金苹果时左键 |
| 切换冷却 | 切换后进入 5 游戏刻（0.25秒）冷却 |
| 冷却期间 | 手持该物品左键无任何效果 |
| 名称变化 | 切换至投掷模式时物品名称变为"投掷型超级附魔金苹果" |

---

## 四、投掷模式

### 基本行为

- 右键投掷
- 初速度与原版喷溅型药水初速度相同
- 投掷后消耗 1 个（受附魔影响，见第七节）

### 落地后行为

1. **先触发喷溅效果** — 范围内所有实体获得基础 buff + 喷溅型药水额外效果，距离越远持续时间/效果强度越低
2. **再生成效果云** — 在落点生成 AreaEffectCloud，效果为基础 buff + 滞留型药水额外效果

### 基础效果（固定，始终给予）

| 效果 | 等级 | 最大持续时间（直接命中） |
|------|------|--------------------------|
| 生命恢复 (Regeneration) | V | 30 秒 |
| 伤害吸收 (Absorption) | IV | 2 分钟 |
| 抗性提升 (Resistance) | I | 5 分钟 |
| 抗火 (Fire Resistance) | I | 5 分钟 |

### 喷溅型药水额外效果

由合成时使用的喷溅型药水决定，随喷溅一起作用于范围内实体。

### 喷溅时间/强度衰减

参考 MC 1.20.4 喷溅型药水规则：
- 直接命中实体：100% 持续时间 / 100% 效果强度
- 溅射范围内实体：
  - 持续性效果：持续时间 = 最大时间 × (1 - 距离/4)
  - 瞬时效果（治疗/伤害）：效果强度按距离衰减，参考原版 MC 1.20.4 喷溅型药水对瞬时效果的处理规则

### 效果云规则

参考 MC 1.20.4 滞留型药水的 AreaEffectCloud 机制：
- 效果云持续时间固定（30秒）
- 效果云给予的效果 = 基础效果 + 滞留型药水额外效果
- 实体进入云中时获得效果
- 每次给予效果后，云的 duration 缩短（durationOnUse）
- 云中获得的效果持续时间 = 云剩余时间比例 × 最大效果时间
- 瞬时效果：参考原版 MC 1.20.4 滞留型药水对瞬时效果的 AreaEffectCloud 处理规则

---

## 五、食用模式

### 基本行为

- 长按右键食用
- 食用时间：32 tick（1.6秒），与 MC 1.20.4 附魔金苹果相同
- 食用后消耗 1 个（受附魔影响，见第七节）

### 食用后给予效果

食用效果 = 基础效果 + 喷溅型药水效果 + 滞留型药水效果（全部给予食用者）

**基础效果（固定）：**

| 效果 | 等级 | 持续时间 |
|------|------|----------|
| 生命恢复 (Regeneration) | V | 30 秒 |
| 伤害吸收 (Absorption) | IV | 2 分钟 |
| 抗性提升 (Resistance) | I | 5 分钟 |
| 抗火 (Fire Resistance) | I | 5 分钟 |

**药水额外效果：**
- 喷溅型药水带来的效果：直接给予食用者（100% 持续时间）
- 滞留型药水带来的效果：直接给予食用者（100% 持续时间）
- 瞬时效果：直接触发（每瓶药水的瞬时效果各触发一次）

---

## 六、物品 Tooltip

合成后的超级附魔金苹果在物品描述中显示包含的药水效果：

```
超级附魔金苹果
[迅投 XXV]
模式: 食用 / 投掷

喷溅效果:
  力量 II (1:30)
  速度 I (3:00)

效果云:
  治疗 II
  抗火 I (3:00)
```

- "喷溅效果" 下列出喷溅型药水带来的额外效果
- "效果云" 下列出滞留型药水带来的额外效果
- 瞬时效果不显示时间
- 持续效果显示 "等级 (分:秒)" 格式

---

## 七、附魔生效规则

### 自带附魔

| 附魔 | 生效模式 | 说明 |
|------|----------|------|
| 迅投 (Swift Throw) 25 | **仅投掷模式** | 等级 >20 进入射线追踪瞬移模式，投掷物瞬间到达目标位置 |

### 可额外附加的附魔及生效模式

以下附魔可通过铁砧等方式附加到超级附魔金苹果上：

#### 投掷模式下生效的附魔

| 附魔 | 效果 |
|------|------|
| 迅投 (Swift Throw) | 提升投掷初速度（每级+50%基础速度）/ 等级>20进入射线追踪瞬移模式 |
| 锋利 (Sharpness) | 落地喷溅时对范围内实体造成额外伤害 |
| 力量 (Power) | 落地喷溅时对范围内实体造成额外伤害（与锋利叠加） |
| 冲击 (Punch) | 落地喷溅时对范围内实体造成击退 |
| 火矢 (Flame) | 落地喷溅时点燃范围内实体 |
| 引雷 (Channeling) | 雷暴天气时，落点范围内露天实体各召唤闪电 |
| 多重射击 (Multishot) | 一次掷出多个（总数 = 2 + level），只消耗 1 个 |
| 穿透 (Piercing) | 投掷物穿透实体，对穿过的实体执行命中判定（仅对普通药水生效，超级附魔金苹果尚未实现） |
| 忠诚 (Loyalty) | 投掷时消耗物品，返回机制仅对普通药水生效（超级附魔金苹果仅实现消耗部分，返回尚未实现） |
| 无限 (Infinity) | 投掷不消耗；无耐久时进入 30s 冷却（受快速装填减免，每级-20%）；有耐久时判定成功免冷却，判定失败进入冷却 |
| 耐久 (Unbreaking) | 投掷后有概率不消耗，概率 = level/(level+1) |
| 快速装填 (Quick Charge) | 减少无限附魔冷却时间，每级 -20% |

#### 食用模式下生效的附魔

| 附魔 | 效果 |
|------|------|
| 锋利 (Sharpness) | 食用时对食用者造成伤害 |
| 击退 (Knockback) | 食用时对食用者施加随机方向击退 |
| 火焰附加 (Fire Aspect) | 食用时点燃食用者，每级 4 秒 |
| 效率 (Efficiency) | 加快食用速度，每级 -10% 食用时间 |
| 冰霜行者 (Frost Walker) | 食用后获得冰霜行者效果 |
| 耐久 (Unbreaking) | 食用后有概率不消耗，概率 = level/(level+1) |

#### 模式限定规则

- 投掷模式下：仅上方"投掷模式下生效的附魔"表中的附魔会触发，食用类附魔（击退/火焰附加/效率/冰霜行者）不触发
- 食用模式下：仅上方"食用模式下生效的附魔"表中的附魔会触发，投掷类附魔（迅投/多重射击/穿透/忠诚/引雷等）不触发
- 耐久 (Unbreaking)：两种模式下均生效

---

## 八、消耗规则总结

| 模式 | 默认消耗 | 耐久附魔影响 | 无限附魔影响 | 忠诚附魔影响 |
|------|----------|--------------|--------------|--------------|
| 投掷模式 | 消耗 1 个 | 有概率不消耗（概率=level/(level+1)） | 不消耗，但进入 30s 冷却（受快速装填减免） | 立即消耗（返回机制尚未实现） |
| 食用模式 | 消耗 1 个 | 有概率不消耗（概率=level/(level+1)） | — | — |

---

## 九、NBT 数据结构

合成后物品 NBT 中存储的药水效果数据：

```nbt
{
  SuperAppleMode: "eat",           // 当前模式
  SplashEffects: [                 // 喷溅型药水效果列表（合并后）
    {Id: "minecraft:strength", Amplifier: 1, Duration: 1800},
    {Id: "minecraft:speed", Amplifier: 0, Duration: 3600}
  ],
  CloudEffects: [                  // 滞留型药水效果列表（合并后）
    {Id: "minecraft:instant_health", Amplifier: 1, Duration: 0, Instant: true},
    {Id: "minecraft:fire_resistance", Amplifier: 0, Duration: 3600}
  ],
  SplashInstantCount: [            // 瞬时效果触发次数（喷溅）
    {Id: "minecraft:instant_health", Amplifier: 1, Count: 2}
  ],
  CloudInstantCount: [             // 瞬时效果触发次数（效果云）
    {Id: "minecraft:instant_health", Amplifier: 1, Count: 2}
  ]
}
```

说明：
- `SplashEffects` / `CloudEffects`：合并后的持续性效果列表
- `SplashInstantCount` / `CloudInstantCount`：瞬时效果单独记录触发次数（每瓶药水各触发一次，两瓶相同瞬时效果 = Count 2）

---

## 十、技术实现要点

1. ✅ **自定义物品** — `SuperEnchantedGoldenAppleItem` 注册于 `ModItems`，合成配方 JSON，物品模型，贴图，语言文件
2. ✅ **合成时药水数据提取** — `SuperAppleCraftingMixin` 注入 `CraftingScreenHandler.updateResult`，读取合成格中喷溅型/滞留型药水的 `Potion` NBT，按合并规则（`SuperApplePotionMerger`）处理后写入产物 NBT
3. ✅ **投掷逻辑** — `SuperGoldenAppleEntity`（继承 `ThrownItemEntity`）处理投掷飞行、碰撞、喷溅和效果云生成，从 NBT 读取 `SplashEffects` 和 `CloudEffects` 应用
4. ✅ **喷溅效果** — 基础效果 + SplashEffects，持续性效果按距离衰减时间，瞬时效果按距离衰减强度（参考原版规则）
5. ✅ **效果云生成** — 基础效果 + CloudEffects，效果云持续时间固定 30s，瞬时效果参考原版滞留型药水的 AreaEffectCloud 行为
6. ✅ **食用逻辑** — 食用时给予：基础效果 + SplashEffects + CloudEffects（全部 100% 持续时间，瞬时效果直接触发对应次数）
7. ✅ **食用附魔效果** — 由 `PlayerEatFoodMixin`（锋利/击退/火焰附加/冰霜行者）和 `UnbreakingFoodMixin`（耐久）处理
8. ✅ **效率加速** — `EfficientEatingMixin` 注入 `Item.getMaxUseTime()`，带效率附魔的食物减少食用时间
9. ✅ **左键切换** — `SuperAppleAttackMixin`（客户端）拦截 `MinecraftClient.doAttack`，判断主手为超级附魔金苹果时切换模式并发送 `SuperAppleModeSwitchC2SPacket` 同步服务端
10. ✅ **5 tick 冷却** — 客户端 Mixin 内部计数器，冷却期间屏蔽左键
11. ✅ **名称动态变化** — 根据 NBT `SuperAppleMode` 标记返回不同的翻译键
12. ✅ **Tooltip 显示** — `appendTooltip` 中读取 NBT 的 `SplashEffects`、`SplashInstantCount`、`CloudEffects`、`CloudInstantCount`，格式化显示药水效果名称、等级和持续时间（瞬时效果不显示时间）
13. ✅ **合成自动附魔** — `SuperAppleCraftingMixin` 合成产出时自动添加迅投 25 + 药水效果数据
14. ✅ **无限附魔冷却** — `InfinityCooldownManager` 自定义冷却管理（基于玩家 UUID + NBT 标记 `InfinityMarked`），支持快速装填减免
15. ⬜ **穿透/忠诚** — 穿透（`PiercingPotionMixin`）和忠诚（`LoyaltyPotionMixin` + `LoyaltyCollisionMixin`）当前仅对 `PotionEntity` 生效，超级附魔金苹果实体的穿透/返回逻辑尚未实现
16. ✅ **铁砧惩罚锁定** — `SuperAppleAnvilMixin` 将超级附魔金苹果的 RepairCost 锁定为 10，防止多次铁砧操作后惩罚指数增长；同时无视原版 "Too Expensive" 39级上限，通过临时设置创造模式标记绕过检查
17. ✅ **投掷时药水数据传递** — `writeEnchantDataToEntity` 将物品 NBT 中的 SplashEffects/CloudEffects/SplashInstantCount/CloudInstantCount 完整复制到投掷实体

---

## 十一、实现文件清单

| 文件 | 用途 |
|------|------|
| `item/SuperEnchantedGoldenAppleItem.java` | 物品主类：双模式、食用/投掷逻辑、Tooltip、效果应用 |
| `item/ModItems.java` | 物品注册 |
| `item/SuperAppleCraftingHandler.java` | 合成附魔工具（旧，保留兼容） |
| `item/SuperApplePotionMerger.java` | 药水效果合并工具类（避免 Mixin 内部类问题） |
| `entity/SuperGoldenAppleEntity.java` | 投掷实体：飞行、碰撞、喷溅效果、效果云生成 |
| `entity/ModEntities.java` | 实体注册 |
| `mixin/SuperAppleCraftingMixin.java` | 合成注入：添加迅投 25 + 读取药水数据写入 NBT |
| `mixin/SuperAppleAnvilMixin.java` | 铁砧注入：无视39级上限 + 锁定 RepairCost + 进度检测 |
| `mixin/client/SuperAppleAttackMixin.java` | 客户端左键拦截：模式切换 + 5 tick 冷却 |
| `network/SuperAppleModeSwitchC2SPacket.java` | C2S 网络包：模式切换同步 |
| `enchantment/ModEnchantments.java` | 迅投附魔注册 |
| `enchantment/SwiftThrowEnchantment.java` | 迅投附魔实现 |
| `enchantment/InfinityCooldownManager.java` | 无限附魔冷却管理 |
| `client/EmptyEntityRenderer.java` | 空渲染器：投掷实体不可见（避免高速视觉bug） |
| `mixin/AdvancementRewardMixin.java` | 进度完成时发放奖励（终极金苹果+1000经验等级） |
| `advancement/UltimateAppleChecker.java` | 14种附魔满级判定工具类 |
| `resources/data/hello-mod/recipes/super_enchanted_golden_apple.json` | 合成配方 |
| `resources/assets/hello-mod/models/item/super_enchanted_golden_apple.json` | 物品模型 |
| `resources/assets/hello-mod/textures/item/super_enchanted_golden_apple.png` | 物品贴图 |
| `resources/assets/hello-mod/lang/zh_cn.json` | 中文翻译 |
| `resources/assets/hello-mod/lang/en_us.json` | 英文翻译 |
