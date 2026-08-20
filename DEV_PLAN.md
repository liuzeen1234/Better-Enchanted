# More Good Enchanted - 开发计划

> Fabric Mod / Minecraft 1.20.4 / Java 17  
> 核心思路：将原版附魔效果迁移到食物和药水上，让食物/药水也能拥有附魔能力。

---

## 当前进度

### 一、食物类附魔（已完成）

- ✅ **锋利 (Sharpness)** — 食物食用时对食用者造成伤害（普通食物 + 蛋糕方块），伤害公式：0.5*level+0.5
- ✅ **击退 (Knockback)** — 食物食用时对食用者施加随机方向击退（普通食物 + 蛋糕方块）
- ✅ **火焰附加 (Fire Aspect)** — 食物食用时点燃食用者，每级4秒（普通食物 + 蛋糕方块）
- ✅ **效率 (Efficiency)** — 加快食用速度，每级减少10%进食时间（效率V = 50%加速）
- ✅ **冰霜行者 (Frost Walker)** — 进食后获得冰霜行者效果（等级=附魔等级），基础20s+每级10s时长，每4s受1点霜冻伤害且每级减少0.5s间隔，脚下水面结冰，血条显示蓝色冰冻样式（普通食物 + 蛋糕方块）
- ✅ **耐久 (Unbreaking)** — 食用后有概率不消耗食物，概率公式参考MC 1.20.4原版：消耗概率=1/(level+1)（普通食物 + 蛋糕方块）

### 二、药水类附魔 — 武器/弓（已完成）

- ✅ **锋利 (Sharpness)** — 掷出的药水砸中实体造成伤害，伤害公式：0.5*level+0.5，溅射范围4格内有距离衰减伤害，与力量叠加
- ✅ **力量 (Power)** — 掷出的药水砸中实体造成伤害，伤害公式参考MC 1.20.4弓力量：damage=level+1，与锋利叠加，溅射范围4格内有距离衰减
- ✅ **冲击 (Punch)** — 掷出的药水砸中实体造成击退，knockbackStrength=punchLevel*0.6，直接命中沿药水飞行方向击退，溅射范围4格内从落点向外推并按距离衰减
- ✅ **火矢 (Flame)** — 掷出的药水砸中实体时点燃目标，直接命中着火5秒，溅射范围4格内按距离衰减着火时长
- ✅ **无限 (Infinity)** — 掷出的药水不消耗；自定义冷却系统（基于NBT标记InfinityMarked），只影响带无限附魔的物品不影响同种普通物品；若有耐久则判定：成功不进冷却，失败进入30s冷却；无耐久则始终30s冷却；客户端通过网络同步显示灰色半透明冷却动画覆盖
- ✅ **耐久 (Unbreaking)** — 投掷后有概率不消耗药水，概率公式同食物耐久：消耗概率=1/(level+1)

### 三、药水类附魔 — 弩（已完成）

- ✅ **多重射击 (Multishot)** — 多等级圆锥散布系统：总投掷物=2+level（Lv I=3, Lv II=4...），第1个直飞，额外投掷物前8个均匀分布在10°圆锥圆周上，超过8个向圆内随机投掷，兼容迅投附魔所有模式，最高等级10
- ✅ **快速装填 (Quick Charge)** — 减少无限附魔的冷却时间，每级减少20%冷却时间（Lv I=24s, Lv II=18s, Lv III=12s, Lv IV=6s, Lv V=0s）
- ✅ **穿透 (Piercing)** — 参考MC 1.20.4弩穿透规则：药水可穿过level个实体（总共命中level+1个），穿透时对每个实体执行附魔效果（锋利/力量/冲击/火矢），已穿透实体不重复命中，命中方块或达到穿透上限时正常销毁并触发溅射效果

### 四、药水类附魔 — 三叉戟（已完成）

- ✅ **忠诚 (Loyalty)** — 参考MC 1.20.4三叉戟忠诚规则改编：药水掷出2s后自动返回投掷者，返回速度=0.5×level格/tick（Lv I=10格/s, Lv II=20格/s, Lv III=30格/s），掷出/返回途中碰到实体或方块则正常消耗，成功返回则归还物品给玩家，播放三叉戟回收音效，超时10s自动销毁
- ✅ **引雷 (Channeling)** — 雷暴天气时，药水落点4格范围内所有实体（需露天）各召唤一道闪电，基于MC 1.20.4三叉戟引雷规则改良为AOE版本

### 五、自定义附魔（已完成）

- ✅ **迅投 (Swift Throw)** — 自定义药水附魔，提升投掷物初速度，每级+50%（公式：实际初速度 = 原始初速度 × (1 + 0.5 × 等级)），附魔台最高等级10（仅可附到书上），铁砧合法最高等级25；等级>20时进入射线追踪瞬移模式（隐藏药水实体+暴击粒子弹道+精确对准准星），发射角度公式 y=80/(4+x)，等级越高越平射

### 六、超级附魔金苹果系统（已完成）

- ✅ **超级附魔金苹果物品** — 自定义物品，3×3合成（金块+喷溅型药水+滞留型药水+金苹果），合成后自带迅投25附魔
- ✅ **双模式切换** — 手持时左键切换食用/投掷模式，5 tick冷却，名称动态变化
- ✅ **投掷模式** — 投掷后先触发喷溅效果（基础buff+喷溅型药水效果），再生成效果云（基础buff+滞留型药水效果）
- ✅ **食用模式** — 食用后给予基础效果+所有药水效果（100%持续时间）
- ✅ **药水效果存储** — 合成时读取药水效果并按规则合并存入NBT，Tooltip显示完整效果信息
- ✅ **铁砧惩罚锁定** — RepairCost锁定为10，防止多次铁砧操作后惩罚指数增长
- ✅ **所有已实现附魔兼容** — 投掷模式兼容锋利/力量/冲击/火矢/引雷/多重射击/无限/耐久/快速装填/迅投；食用模式兼容锋利/击退/火焰附加/效率/冰霜行者/耐久
- ⬜ **穿透/忠诚对超级金苹果实体** — 穿透和忠诚当前仅对普通PotionEntity生效，SuperGoldenAppleEntity的穿透/返回逻辑尚未实现

### 八、终极附魔金苹果系统（已完成）

- ✅ **终极附魔金苹果物品** — 自定义物品 `UltimateEnchantedGoldenAppleItem`，自带附魔：效率9、耐久10、迅投25、无限1
- ✅ **获取方式** — 通过自定义进度奖励获得（将超级附魔金苹果的14种有效附魔全部附至最高等级），奖励额外给予1000经验等级
- ✅ **双模式切换** — 手持时左键切换食用/投掷模式，2 tick冷却（比超级版更快），名称动态变化（亮金色GOLD文字）
- ✅ **食用模式** — 效率9使食用近乎瞬时（约3 tick），给予生命恢复V(60s)+抗性提升III(60s)+力量V(60s)；耐久10判定成功（91%概率）不消耗+3s冷却，判定失败消耗无冷却
- ✅ **投掷模式** — 迅投25射线追踪瞬达，落地后4格范围检测：敌对生物受100点真实伤害+无条件召唤闪电，友好生物/玩家获得生命恢复V(60s)
- ✅ **投掷消耗** — 自带无限+耐久10，投掷不消耗进入冷却（受快速装填减免），耐久判定成功免冷却
- ✅ **附魔额外效果** — 投掷落地后对4格范围实体施加锋利/力量/冲击/火矢/引雷等附魔效果
- ✅ **多重射击兼容** — 投掷模式完全兼容多重射击附魔，圆锥散布
- ✅ **进度系统** — `AdvancementRewardMixin` 监听进度完成事件，`UltimateAppleChecker` 判定14种附魔是否满级
- ✅ **自定义伤害源** — `UltimateAppleDamageSource` 提供专属死亡消息
- ✅ **投掷实体** — `UltimateGoldenAppleEntity` 继承 `ThrownItemEntity`，支持射线追踪和碰撞检测
- ✅ **Tooltip显示** — 显示双模式、食用效果、投掷效果（4格范围/100真实伤害/生命恢复V）

### 九、辅助功能/客户端功能（已完成）

- ✅ **实体血量HUD** — 准星指向实体时显示[名称][血量/最大血量]，无血量实体显示[-/-]，检测距离可配置（最大128格）
- ✅ **实体详细信息显示** — 设置中开启后显示实体完整NBT信息（0.5x缩放），ActiveEffects优先显示（绿色高亮），通过服务端网络包同步NBT数据
- ✅ **手持物品NBT显示** — 显示主手物品NBT标签和耐久度，0.5x缩放
- ✅ **调试功能菜单** — 二级菜单结构，统一管理HUD开关和调试日志开关（9种debug日志输出分别控制）
- ✅ **设置持久化** — 所有HUD和debug开关保存到config/hello-mod.json
- ✅ **蛋糕附魔存储系统** — CakeEnchantmentStorage + EnchantedCakeBlockEntity，支持蛋糕方块存储和读取多种附魔
- ✅ **药水附魔框架** — PotionItemMixin + PotionEntityMixin，支持附魔数据从ItemStack传递到投射物Entity
- ✅ **无限附魔冷却系统** — InfinityCooldownManager（基于玩家UUID+NBT标记），InfinityCooldownSync网络同步，客户端冷却动画覆盖
- ✅ **药水/闪电无敌帧移除** — 药水附魔伤害和闪电不再受受伤无敌帧限制

---

## 暂未规划的附魔

以下附魔目前没有设计具体的自定义效果，暂不实现：

- 亡灵杀手 (Smite)
- 节肢杀手 (Bane of Arthropods)
- 抢夺 (Looting)
- 横扫之刃 (Sweeping Edge)
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

## 待完成事项

- ⬜ 超级附魔金苹果实体的穿透 (Piercing) 逻辑
- ⬜ 超级附魔金苹果实体的忠诚 (Loyalty) 返回逻辑

---

## 技术要点

1. **蛋糕的特殊处理**：蛋糕是方块而非普通食物，附魔存储使用 `CakeEnchantmentStorage` + `EnchantedCakeBlockEntity`，所有食物附魔均兼顾蛋糕。

2. **药水附魔框架**：
   - 附魔存储在药水 ItemStack 的 NBT 中
   - 投掷时通过 `PotionItemMixin` 将附魔信息传递给 `ThrownPotionEntity`
   - Entity 的 NBT 持久化

3. **无限+耐久协作**：无限不消耗药水，耐久判定决定是否进入冷却（成功=不冷却，失败=30s冷却）

4. **迅投高等级模式**：等级>20时使用射线追踪瞬移模式，隐藏药水实体，显示暴击粒子弹道，修复高速碰撞问题

5. **超级附魔金苹果**：详见 `SUPER_GOLDEN_APPLE_DESIGN.md`

6. **终极附魔金苹果**：详见 `ULTIMATE_GOLDEN_APPLE_DESIGN.md`

7. **Mixin 注入点**：
   - 食物食用：`PlayerEatFoodMixin` → `PlayerEntity.eatFood()`
   - 食用速度：`EfficientEatingMixin` → `Item.getMaxUseTime()`
   - 食物耐久：`UnbreakingFoodMixin`
   - 药水投掷：`PotionItemMixin` → `PotionItem.use()`
   - 药水命中：`PotionEntityMixin` → `ThrownPotionEntity.onCollision()`
   - 多重射击：`MultishotEnchantmentMixin`
   - 穿透：`PiercingPotionMixin`
   - 忠诚：`LoyaltyPotionMixin` + `LoyaltyCollisionMixin`
   - 迅投tick：`SwiftThrowTickMixin`
   - 迅投渲染：`SwiftThrowRenderMixin`（客户端）
   - 蛋糕：`CakeBlockMixin` → `CakeBlock.tryEat()` + `CakePlaceMixin`
   - 超级金苹果合成：`SuperAppleCraftingMixin`
   - 超级金苹果铁砧：`SuperAppleAnvilMixin`
   - 超级金苹果左键：`SuperAppleAttackMixin`（客户端）
   - 无敌帧：`LivingEntityDamageCooldownMixin`
   - 冷却覆盖渲染：`DrawContextCooldownMixin`（客户端）
   - 进度奖励：`AdvancementRewardMixin` → `PlayerAdvancementTracker.grantCriterion()`
