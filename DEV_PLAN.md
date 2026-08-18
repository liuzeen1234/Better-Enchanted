# More Good Enchanted - 开发计划

> Fabric Mod / Minecraft 1.20.4 / Java 17  
> 核心思路：将原版附魔效果迁移到食物和药水上，让食物/药水也能拥有附魔能力。

---

## 当前进度

已实现：
- ✅ **锋利 (Sharpness)** — 食物食用时对食用者造成伤害（普通食物 + 蛋糕方块）
- ✅ **击退 (Knockback)** — 食物食用时对食用者施加随机方向击退（普通食物 + 蛋糕方块）
- ✅ **火焰附加 (Fire Aspect)** — 食物食用时点燃食用者，每级4秒（普通食物 + 蛋糕方块）
- ✅ **效率 (Efficiency)** — 加快食用速度，每级减少10%进食时间（效率V = 50%加速）
- ✅ **冰霜行者 (Frost Walker)** — 进食后获得冰霜行者效果（等级=附魔等级），基础20s+每级10s时长，每4s受1点霜冻伤害且每级减少0.5s间隔，脚下水面结冰，血条显示蓝色冰冻样式（普通食物 + 蛋糕方块）
- ✅ 蛋糕附魔存储系统（CakeEnchantmentStorage，支持多种附魔）
- ✅ BlockEntity 基础框架

---

## 需要实现的附魔效果总览

### 一、食物类附魔（对食物物品生效）

| 附魔 | 效果 | 难度 |
|------|------|------|
| ~~锋利 (Sharpness)~~ | ~~食用者受伤~~ | ✅ 已完成 |
| ~~击退 (Knockback)~~ | ~~随机方向击退食用者~~ | ✅ 已完成 |
| ~~火焰附加 (Fire Aspect)~~ | ~~点燃食用者~~ | ✅ 已完成 |
| ~~效率 (Efficiency)~~ | ~~加快食用速度~~ | ✅ 已完成 |
| ~~冰霜行者 (Frost Walker)~~ | ~~进食后获得冰霜行者效果，每级+10s时长，每级-0.5s伤害间隔，蓝色血条~~ | ✅ 已完成 |
| 耐久 (Unbreaking) | 食用后有概率不消耗食物 | ⭐⭐ |

### 二、药水类附魔 — 武器/弓（对掷出的药水生效）

| 附魔 | 效果 | 难度 |
|------|------|------|
| 锋利 (Sharpness) | 掷出的药水砸中实体造成伤害，与力量叠加 | ⭐⭐ |
| 力量 (Power) | 掷出的药水砸中实体造成伤害 | ⭐⭐ |
| 冲击 (Punch) | 掷出的药水砸中实体造成击退 | ⭐⭐ |
| 火矢 (Flame) | 掷出的药水点燃砸中的实体 | ⭐⭐ |
| 无限 (Infinity) | 掷出的药水不消耗，进入10s冷却；与耐久互斥 | ⭐⭐⭐ |
| 耐久 (Unbreaking) | 使用后有概率不消耗药水；与无限互斥 | ⭐⭐ |

### 三、药水类附魔 — 弩（对掷出的药水生效）

| 附魔 | 效果 | 难度 |
|------|------|------|
| 多重射击 (Multishot) | 一次掷出3个相同药水瓶（消耗1个），水平扇形散开，中间直飞，左右各偏移10° | ⭐⭐⭐ |
| 穿透 (Piercing) | 掷出的药水穿透实体，对穿过的实体进行击中判定 | ⭐⭐⭐ |

### 四、药水类附魔 — 三叉戟（对掷出的药水生效）

| 附魔 | 效果 | 难度 |
|------|------|------|
| 忠诚 (Loyalty) | 药水掷出5s后自动返回，掷出/返回时碰到实体或方块则消耗掉 | ⭐⭐⭐⭐ |
| 引雷 (Channeling) | 雷暴时药水砸中实体在药水所在位置召唤闪电 | ⭐⭐⭐ |

---

## 推荐开发顺序

按照 **依赖关系** 和 **难度递增** 安排，先搭基础框架再逐步添加功能。

### 阶段一：完善食物附魔基础系统（简单效果）

> 目标：在已有锋利的基础上，快速扩展其他简单的食物附魔效果。

1. **击退 (Knockback)** — 食用时对食用者施加击退  
   - 实现方式：在 `PlayerEatFoodMixin.onEatFood` 中加入击退逻辑
   - 使用 `player.setVelocity()` + `player.velocityModified = true`
   - 公式参考：原版击退等级 × 向后方向的力

2. **火焰附加 (Fire Aspect)** — 食用时点燃食用者  
   - 实现方式：在 `PlayerEatFoodMixin.onEatFood` 中调用 `player.setOnFireFor()`
   - 时长：等级 × 4秒（与原版一致）

3. **耐久 (Unbreaking) [食物]** — 食用后有概率不消耗  
   - 实现方式：在 `eatFood` 的 RETURN 注入，根据等级概率恢复 stack 数量
   - 概率公式：`1 / (等级 + 1)` 的概率消耗（即不消耗概率 = `等级 / (等级 + 1)`）

### 阶段二：食物附魔进阶效果

4. **效率 (Efficiency)** — 加快食用速度  
   - 实现方式：Mixin 到食用时间计算逻辑，减少 `getMaxUseTime()` 返回值
   - 需要 Mixin `Item` 或 `FoodComponent` 相关方法
   - 每级减少若干 tick 的食用时间

5. **冰霜行者 (Frost Walker)** — 进食后获得20s冰霜行者效果，每4s受1点霜冻伤害  
   - 实现方式：食用时给玩家加一个自定义的状态效果或使用 tick 事件
   - 需要实现持续伤害逻辑（计时器 / tick 事件回调）
   - 需要实现脚下水面结冰逻辑

### 阶段三：建立药水附魔框架

> 目标：建立药水投掷附魔的核心机制，使药水瓶可以附魔并在投掷时读取附魔。

6. **药水附魔数据系统**  
   - 实现将附魔存储到药水 ItemStack 的 NBT 中
   - 在投掷药水时将附魔信息传递给 `ThrownPotionEntity`
   - Mixin `ThrownPotionEntity` 或 `PotionItem` 以读取附魔数据

7. **锋利 (Sharpness) [药水]** — 药水砸中实体造成伤害  
   - 实现方式：Mixin `ThrownPotionEntity` 的 `onEntityHit`
   - 与力量效果叠加

8. **力量 (Power) [药水]** — 类似锋利，增加药水砸中伤害  
   - 与锋利共用伤害计算逻辑，但使用弓的力量公式

### 阶段四：药水命中效果

9. **冲击 (Punch)** — 药水砸中实体造成击退  
   - Mixin `ThrownPotionEntity` 的命中逻辑
   - 计算击退方向（药水飞行方向）

10. **火矢 (Flame)** — 药水点燃砸中的实体  
    - 在命中逻辑中加入 `entity.setOnFireFor()`

11. **引雷 (Channeling)** — 雷暴时药水砸中位置召唤闪电  
    - 检查天气状态 `world.isThundering()`
    - 在药水落点 spawn `LightningEntity`

### 阶段五：药水投掷行为修改

12. **无限 (Infinity)** — 投掷不消耗药水，10s冷却  
    - Mixin 药水投掷逻辑，取消物品消耗
    - 使用 `player.getItemCooldownManager().set()` 设置冷却
    - 实现与耐久的互斥检查

13. **耐久 (Unbreaking) [药水]** — 使用后概率不消耗  
    - 类似食物耐久逻辑，阻止投掷后的物品减少
    - 实现与无限的互斥检查

14. **多重射击 (Multishot)** — 一次掷出3瓶药水  
    - Mixin 投掷逻辑，额外 spawn 2个药水实体
    - 计算左右偏移10°的飞行方向（仅水平偏移，不上下散开）
    - 只消耗1瓶

15. **穿透 (Piercing)** — 药水穿透实体  
    - 修改药水实体的碰撞逻辑，命中后不消失
    - 对穿过的每个实体进行一次击中判定
    - 需要记录已击中的实体避免重复判定

### 阶段六：复杂药水行为

16. **忠诚 (Loyalty)** — 药水投掷5s后自动返回  
    - 最复杂的效果之一
    - 药水实体需要自定义 tick 逻辑：5s后改变速度方向飞回玩家
    - 返回途中碰到实体或方块则消耗掉
    - 可能需要自定义 Entity 或 Mixin `ThrownPotionEntity` 的 tick

---

## 不需要实现的附魔（文档中未标注具体效果的）

以下附魔在 `enchanted` 文件中未缩进/未标注自定义效果，意味着暂不修改或保持原版行为：

- 亡灵杀手 (Smite)
- 节肢杀手 (Bane of Arthropods)
- 抢夺 (Looting)
- 横扫之刃 (Sweeping Edge)
- 快速装填 (Quick Charge)
- 激流 (Riptide)
- 穿刺 (Impaling)
- 保护系列 (Protection / Fire Protection / Blast Protection / Projectile Protection)
- 荆棘 (Thorns)
- 水下呼吸 (Respiration)
- 水下速掘 (Aqua Affinity)
- 摔落保护 (Feather Falling)
- 深海探索者 (Depth Strider)
- 灵魂疾行 (Soul Speed)
- 精准采集 (Silk Touch)
- 时运 (Fortune)
- 海之眷顾 (Luck of the Sea)
- 饵钓 (Lure)
- 经验修补 (Mending)
- 消失诅咒 (Curse of Vanishing)
- 绑定诅咒 (Curse of Binding)
- 迅捷潜行 (Swift Sneak)

---

## 技术要点提醒

1. **蛋糕的特殊处理**：蛋糕是方块而非普通食物，附魔存储需要使用 `CakeEnchantmentStorage`（已有）。后续新增的食物附魔也要兼顾蛋糕。

2. **药水附魔框架**：阶段三是关键，需要设计好：
   - 如何把附魔附加到药水上（铁砧？附魔台？自定义方式？）
   - 投掷时如何将 ItemStack 上的附魔信息传递给投射物 Entity
   - Entity 的 NBT 持久化

3. **互斥关系**：无限 ↔ 耐久（药水上）

4. **Mixin 注入点参考**：
   - 食物食用：`PlayerEntity.eatFood()`
   - 食用速度：`Item.getMaxUseTime()` 或 `LivingEntity.getActiveItem()` 相关
   - 药水投掷：`PotionItem.use()` / `SplashPotionItem`
   - 药水命中：`ThrownPotionEntity.onCollision()` / `onEntityHit()`
   - 蛋糕：`CakeBlock.tryEat()`（已有）

---

## 预估工作量

| 阶段 | 内容 | 预计时间 |
|------|------|----------|
| 阶段一 | 击退 + 火焰附加 + 耐久(食物) | 1-2天 |
| 阶段二 | 效率 + 冰霜行者 | 2-3天 |
| 阶段三 | 药水附魔框架 + 锋利/力量(药水) | 3-4天 |
| 阶段四 | 冲击 + 火矢 + 引雷 | 2-3天 |
| 阶段五 | 无限 + 耐久(药水) + 多重射击 + 穿透 | 4-5天 |
| 阶段六 | 忠诚 | 2-3天 |

**总计：约 14-20 天**
