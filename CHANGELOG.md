# 更新日志

## v1.2.0（相对于 v1.1.1）

### 架构重构

#### 调试菜单拆分为独立 Mod
- 将调试菜单、HUD 显示、行为日志系统从主 Mod 中完全剥离，迁移至独立的 **debug-menu** Mod
- 主 Mod 不再包含任何 UI 屏幕（DebugMenuScreen、HealthHudSettingScreen 等）、HUD 渲染（EntityHealthHud、ItemCountHud）、行为日志 Mixin（5个）、实体 NBT 网络同步包
- 新增 Debug Menu API：任何 Mod 可通过 `DebugMenuApi.register()` 注册调试开关，由 debug-menu Mod 统一管理和展示
- 主 Mod 通过 `FabricLoader.isModLoaded("debug-menu")` 检测，仅在调试 Mod 存在时注册调试开关
- 未安装 debug-menu 时主 Mod 正常运行，调试日志默认全关，无任何额外开销

### 删除的文件（从主 Mod 移除）

- `client/DebugMenuScreen.java` — 调试菜单 UI
- `client/DebugLogSettingScreen.java` — 日志开关设置 UI
- `client/HealthHudSettingScreen.java` — 血量 HUD 设置 UI
- `client/ItemHudSettingScreen.java` — 物品 HUD 设置 UI
- `client/EntityHealthHud.java` — 实体血量 HUD 渲染
- `config/ModConfig.java` — 旧配置管理（拆分为 DebugLogConfig 独立管理）
- `network/EntityNbtRequestC2SPacket.java` — 实体 NBT 请求包
- `network/EntityNbtResponseS2CPacket.java` — 实体 NBT 响应包
- `network/EntityNbtCache.java` — 客户端 NBT 缓存
- `mixin/PlayerBehaviorLogMixin.java` — 玩家行为日志（服务端）
- `mixin/PlayerBlockInteractLogMixin.java` — 方块交互日志（服务端）
- `mixin/client/BehaviorLogClientMixin.java` — 客户端界面日志
- `mixin/client/BehaviorLogKeyboardMixin.java` — 客户端键盘日志
- `mixin/client/BehaviorLogMouseMixin.java` — 客户端鼠标日志

### 新增文件

- `DebugToggleRegistration.java` — 可选的调试开关注册（仅在 debug-menu 存在时生效）
- `com/debugmenu/api/DebugMenuApi.java` — API 存根
- `com/debugmenu/api/DebugToggleEntry.java` — API 存根

### 配置变更

- 调试日志开关从 `config/hello-mod.json` 迁移至 `config/better-enchanted-debug.json`
- HUD 设置迁移至 debug-menu Mod 的 `config/debug-menu.json`

### 体积优化

- 主 Mod 减少 14 个 Java 源文件、5 个 Mixin 注入点
- 预计 jar 体积减小约 30-40%

---

## v1.1.1（相对于 v1.1.0）

### 新功能

#### 食物 / 药水附魔台支持
- 食物和药水现在可以直接在附魔台中附魔（附魔台会提供对应的可用附魔列表）
- 食物和药水现在可以在铁砧上通过附魔书获得附魔
- 为食物/药水提供合理的 enchantability 值（普通食物 10、药水 15、超级/终极附魔金苹果 22）
- 可附魔范围——食物：锋利/击退/火焰附加/效率/冰霜行者/耐久；药水：锋利/力量/冲击/火矢/无限/耐久/多重射击/快速装填/穿透/引雷/忠诚/迅投；超级·终极金苹果同时支持食物+药水全部附魔

### 功能增强

#### 玩家行为日志系统
- 新增玩家行为实时追踪日志，记录攻击、交互、受伤、死亡、物品丢弃、跳跃、手切换、疾跑/潜行状态变化、移动、物品拾取等动作
- 新增方块交互日志，记录放置、破坏、交互方块事件
- 新增客户端行为日志（键盘输入、鼠标点击、客户端 tick 事件）
- 调试功能菜单新增"玩家行为日志"开关按钮（BehaviorLog toggle）
- 新增 `playerBehaviorLogEnabled` 配置项，支持持久化保存
- 添加中英文本地化文本

---

## v1.1.0（相对于 v1.0.0）

### 新物品

#### 超级附魔金苹果
- 新增超级附魔金苹果物品，支持双模式切换（食用模式/投掷模式）
- 支持铁砧附魔系统，可叠加各种附魔效果
- 铁砧合成绕过原版"Too Expensive"（过于昂贵）限制
- 铁砧附魔惩罚（RepairCost）锁定为固定值 10，避免多次附魔后成本飙升
- 支持药水效果存储：合成时可关联药水，食用/投掷时释放对应效果
- 物品 Tooltip 显示当前存储的药水效果信息

#### 终极附魔金苹果
- 新增终极附魔金苹果物品，作为超级附魔金苹果的进阶版本
- 投掷命中敌对生物时无条件召唤闪电
- 自定义伤害类型（绕过护甲和附魔保护）
- 新增终极金苹果挑战进度（Achievement），通过进度奖励监听触发

### 附魔调整

#### 迅投附魔
- 附魔台最高可附魔 10 级，且仅可附到书上（需通过铁砧转移到物品）
- 铁砧最高等级改为 19，防止触发射线追踪模式
- 修复迅投等级 >20 射线追踪模式的 pitchOffset 偏移问题，投掷物精确对准准星

### 功能增强

#### 实体血量 HUD
- 设置菜单新增"显示详细信息"按钮
- 开启后显示实体完整 NBT 信息（0.5x 缩放，靠右对齐）
- ActiveEffects（药水效果）优先显示在血量行下方（绿色高亮）
- 通过服务端网络包同步完整 NBT 数据（含药水效果）
- 无血量实体显示 `[-/-]`
- 手持物品 NBT 显示同步缩放为 0.5x

### Bug 修复
- 修复超级金苹果铁砧 RepairCost 锁定值错误的问题
- 修复迅投高等级投掷偏移不对准准星的问题

### 其他
- 新增超级附魔金苹果设计文档 (`SUPER_GOLDEN_APPLE_DESIGN.md`)
- 新增终极附魔金苹果设计文档 (`ULTIMATE_GOLDEN_APPLE_DESIGN.md`)
- 更新 README 和 DEV_PLAN，补充新功能章节及实现细节
