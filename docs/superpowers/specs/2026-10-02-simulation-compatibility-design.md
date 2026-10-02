# 模拟室兼容性与交互修订方案

日期：2026-10-02。基线：v0.1.0-alpha.10 / f2dcf0e，Minecraft 1.21.1、NeoForge 21.1.252、AE2 19.2.17、AE2LT 2.1.0。

本次交付为方案与源码/原版资源核查记录。实现沿用现有独立 Git 仓库与 Blockbench 素材流程。

## 已确定的规则

- 外尺寸 3³～7³、容量 N²、128 输出槽、每槽 1024、效率/时运/过载费用与基础周期保持原约定。
- 光球：成型结构中只要有效容量内有至少一个完美模拟水晶就显示；待机、缺电、输出堵塞和生产换轮均不隐藏。无水晶或解体才隐藏。不成型/缩小后保留的额外水晶不计入显示条件。
- 光源：照亮中心附近约 5 格，而非光照等级 5；预计中心光照等级 6，按实际原版光照传播验收并允许配置。
- 水晶随时可在 GUI 放入或取出；真实库存变化立即中断整个当前批次，清空进度，新库存下一 tick 重新排产。模拟查询、失败的插入/取出、客户端同步均不能中断。
- 用户已选完成时扣费：新任务中断不产物、不扣 FE/HV/EHV。完毕而缺费或无输出空间时等待，不能免费输出。
- 远古残骸熔炼：1 个远古残骸 → 2 个下界合金碎片；有熔炼模块的费用仍每参与水晶额外 2 HV。
- 龙蛋 25%×1、龙首 25%×1、龙息 100%×1，三次独立判定。凋灵建议每次 1 下界之星；坚守者默认每次幽匿催发体、回响碎片、幽匿块各 1，三项均 100%。后两项为本方案默认数量，可由数据包修改。

## 核查结果与设计选择

不建议逐个给已知生物/金属打硬编码补丁：不能解决第三方内容和其它受条件限制的掉落。也不建议创建、杀死真实生物或实际破坏矿石来获得产物：会引入世界副作用、经验/事件重复与库存复制。

建议保留原生标签与掉落表，加入专用规则解析层、受控的模拟上下文和显式例外。优先级为作者提供器/显式配方 > 显式模拟规则 > 自动发现；同一层最高优先级冲突应显示诊断并拒绝消耗材料，不能依赖加载顺序。

| 问题 | 已找到的依据 | 拟采用修订 |
|---|---|---|
| 转角不顺 | `SimulationShellModel` 在转角选择交叉条纹图块，主要靠整面旋转；`art/multiblock_visual_redesign.js` 的 corner 绘图为横竖十字叠加 | 改为有方向的 L 型转接、角部面板与连续双线，逐条边校验像素衔接 |
| 球体换轮闪烁 | `visualFlags()` 要求 paid batch、remaining>0、节点在线 | 将 hasCrystal/formed 与 processing 分开同步；球体显示只取前两项 |
| 水晶被锁 | handler insert/extract 与菜单 mayPlace/mayPickup 都检查 busy | 统一真实变更入口与批次中断事务，解除 GUI 锁，完成时结算 |
| 无线抽走水晶 | controller.automation().extractItem 的 slot<49 分支提取水晶；普通模拟室只有输出可提取 | 所有外部提取仅可返回输出，GUI 独立提取水晶 |
| 幻翼无膜 | `MobLoot.roll` 使用 generic DamageSource，不提供 LAST_DAMAGE_PLAYER；原版膜池要求 killed_by_player | 提供受控玩家击杀上下文，保留原版概率与数量 |
| 下界合金错误 | production/netherite.json 显式输出 netherite_ingot；smelt 兜底只处理 raw_materials→ingots | 输出 ancient_debris，显式熔炼为 scrap×2 |
| 第三方粗矿不匹配 | 默认仅有五种矿物的显式绑定/档案，未从标签发现新增材料 | 生成并缓存自动矿物规则，保留显式配置覆盖 |
| 第三方生物不确定 | 现有 mob binding 使用 * 且扫描所有 Mob，本来就覆盖多数第三方生物 | 区分绑定失败与空掉落；补刷怪蛋索引、例外规则与诊断，不误称已证实所有第三方生物绑定失败 |

## 1. 转角外观

继续在 Blockbench 制作粉白素材。八个外角分别处理上下、内外与四个方向，横梁的粉色双线沿 L 型转向进入竖柱；交汇处为浅灰紫面板、粉色嵌线和小尺寸高光，参考用户图二的角部设计而保留本模组配色。

角部饰板使用同一套局部坐标与 UV 方向规则；如需立体倒角，只改变装饰模型，碰撞体和建造尺寸保持完整方块。玻璃、顶盖、控制器和原版过载接口继续与相邻线条衔接。

验收：3～7 的八角、横竖过渡、不同朝向控制器/接口；近景与远景均无截断双线、每格重复描边或透明面重叠闪动。资源校验增加相邻贴图边缘像素契约，原生客户端检查仍必需。

## 2. 常驻光球与实际光源

新增独立视觉状态 formed、hasCrystal、processing、smelting、overload。球体可见由 formed && hasCrystal 决定，颜色取当前结构模块配置：普通粉色、安装熔炼模块橙色；过载电环取过载模块配置。四角工作电弧/屏幕心电波仍取 processing，空闲为平线。动画使用世界时间与 partial tick，批次更换和 GUI 往返不重置相位。

全亮渲染不会照亮周围方块。拟增加模组自有的不可碰撞、无掉落、无物品形态的隐形中心光源方块，初始 emission=6：无遮挡情况下原版距离衰减至约 5 格，不保证穿过遮挡或将所有 5 格区域照成相同亮度。中心只允许一个受控制器身份管理的光源，偶数尺寸落在最近中心格。结构验证允许本机拥有的中心光源，不能把任意非空气内部当作合法。

只在空/非空水晶、成型/解体或亮度配置改变时更新光源，不每轮触发重算。位置与 owner UUID 保存；拆除、解体及更换控制器清理自己拥有的光源；区块加载后核验 owner。其它方块替换该格时不得被清理逻辑删掉，不强制加载区块。服务器提供实际光照，客户端负责颜色动画，不新增第三方动态光照前置。

## 3. 随时换晶与完成时结算

新批次状态：计划/加工 → 完成等待提交 → 校验输入、结构、费用及输出容量 → 严格扣 FE/HV/EHV → 将固定结果写入缓冲 → 完成通知/导出。

开始时可检查资源是否足够并选择参与水晶，但只查询，不预扣。途中网络离线或结构不合法暂停；完成时资源不足/输出满则等待，保留固定结果，不重抽。资源再次齐备后一次结算，不能部分收费后部分输出。时运仍在单次基础掉落抽取后作倍率，不能把 25% 几率重复抽 1024 次。

菜单正常点击、Shift+左键快速移动、拖拽、数字键交换和真实插入都汇入同一服务端库存变更入口。只有成功改变物品/组件/数量才取消，批量 quickMove 中可合并取消通知；无效晶体、满背包、满输入、模拟抽取不取消。一个玩家修改时，其他玩家收到服务端新进度；使用库存 revision 排除过时操作。

GUI 回收范围包括缩小结构后保留的隐藏槽；新插入只能进入当前有效容量。外部管道/AE 可以插入合法完美晶体并取消加工，但不能提取模板；GUI 始终允许取回。

完成事务继续保存费用、结果和提交标记；如果 AE 实际提取小于模拟查询，沿用按 HV/EHV 分开的退款债务。付款提交期间发生扩展回调修改库存时，应重验 revision，失败就回滚已提取资源，不能提交旧结果。

存档迁移：旧 alpha.10 的 paid=true 任务继续按已支付快照完成，不二次收费；玩家换晶则取消并退还旧任务 FE/HV/EHV。网络放不下的闪电退款继续存为债务，FE 超出本地容量的部分保留为可抵扣后续费用的持久化信用，避免溢出损失或满缓冲退款死锁。新任务带 settlement_version=2。现有已完成缓冲不回收、更改或重算。

## 4. 生物掉落统一处理

已直接核查本机 Mojang 1.21.1 客户端原版 loot_table/entities 及对应源码。静态结果见 `docs/research/2026-10-02-vanilla-loot-audit.json`，不是已通过的运行测试。

18 种原版生物的部分掉落受玩家击杀条件限制：

| 生物 | 当前遗漏的玩家限定部分 |
|---|---|
| 幻翼、烈焰人、旋风人 | 幻翼膜、烈焰棒、旋风棒 |
| 蜘蛛、洞穴蜘蛛 | 蜘蛛眼 |
| 凋灵骷髅、兔子 | 凋灵骷髅头、兔子脚 |
| 僵尸、尸壳、僵尸村民 | 铁锭/胡萝卜/马铃薯稀有池 |
| 溺尸、僵尸猪灵 | 铜锭、金锭 |
| 流浪者、沼骸 | 特殊药箭 |
| 守卫者、远古守卫者 | 限定额外鱼池；远古守卫者还有湿海绵 |
| 唤魔者、卫道士 | 绿宝石；唤魔者的不死图腾并非这项条件限定 |

其它类别：岩浆怪的默认临时实体 size=1，而岩浆膏要求 size≥2；史莱姆球要求 size=1，不能把所有史莱姆类统一设大。苦力怕唱片要求骷髅击杀，蛙明灯要求青蛙击杀岩浆怪；装备掉落如溺尸三叉戟不在基础死亡掉落表中。普通空池或概率抽到零不能被自动认定为错误。

默认实体掉落采用专用、无抢夺附魔的 FakePlayer 击杀上下文，提供 LAST_DAMAGE_PLAYER、ATTACKING_ENTITY、DIRECT_ATTACKING_ENTITY 及对应 DAMAGE_SOURCE；临时实体只用于表计算，不加入世界、不 finalizeSpawn、不执行真实死亡，不复制实际生物装备、背包或 NBT。玩家限定掉落使用原版概率；例如幻翼仍可能单次抽到 0，不改成每次必出膜。保留 NeoForge 正常掉落表与 Global Loot Modifier 入口，每次只应用一次。

加入 `simulation_mob` 显式规则：实体 ID/标签、priority、disabled、loot_table/outputs/provider、上下文策略和受控 template。岩浆怪 template size=2、史莱姆 size=1；有特殊变体/条件的模组由作者适配器初始化，不允许任意玩家 NBT 进入模板。

凋灵/末影龙解除内置绑定黑名单，但保留整合包可重新禁用的配置。它们原版实体掉落表为空，因此使用独立规则：凋灵下界之星×1；末影龙三个独立输出池按已确定几率；坚守者以显式 outputs 替换原表，防止催发体重复。现有生物水晶只保存 entity_type 的稳定身份可继续使用。

默认玩家上下文不会生成唱片、蛙明灯或复制装备。上述特殊击杀/装备产物可由作者使用 outputs/provider 加入。这是有意的规则边界，不应宣称仅靠刷怪蛋能推导所有死亡代码和事件掉落。

刷怪蛋索引遍历物品注册表中的 SpawnEggItem（含 NeoForge 实现）及实际默认物品的实体类型；缓存支持重建。不建立原版实体白名单：原有存活 Mob 默认绑定行为保留；具有刷怪蛋的可模拟 LivingEntity 由通用入口支持，非 Mob 或特殊蛋实现可通过新实体资格适配器接入。原有 MobPredicate API 保留，不改变参数类型破坏二进制兼容。附近目标仍为 5 格内最近者、33% 转化，原版雷击/人工雷规则不变。

## 5. 外部输出隔离

外部 capabilities 的 49 个输入槽仍可接受合法水晶，但 extractItem 无论 simulate=true/false 均返回空；其后 128 输出槽可以正常提取。GUI 使用独立库存访问，解锁 GUI 不等于解锁外部提取。网络导出只枚举 BulkOutputBuffer，不从晶体 handler 枚举产物。

沿用现有外部槽号避免管道过滤设置失效，不改变 native AE2LT interface 的库存、频道与 block entity。验收既包括结构内部接口主动导出，也包括远处过载接口无线抽取，以及普通 AE 出口、其它物品管道；只能拿走产物。输出中即使是某个配方合法产出的水晶也按槽身份处理，不能靠“排除所有水晶物品 ID”掩盖问题。

## 6. 矿物自动发现与数量

按实际注册的通用 c 标签建立材料关系，块标签和物品标签分别读取，不能互相替代：

- 粗矿绑定：`c:storage_blocks/raw_<material>` → 物品 `c:raw_materials/<material>`。
- 无粗矿资源绑定：`c:storage_blocks/<material>`，结合 `c:ores/<material>` 及 `c:gems/<material>`、`c:dusts/<material>` 等资源标签。
- 只发现矿物相关存储块，不把所有 c:storage_blocks（食物、合金等）都视作可模拟矿物。缺少关联矿石/资源时需显式规则。下界石英等未必具有对应存储块标签，内置显式映射可提供石英块绑定。

自动粗矿规则默认单次 raw item×1，显式配方可以改数量。无粗矿规则采用选定矿石的原版 block loot table，用无时运、无精准的工具上下文，得到真实基础数量；不实际在世界放置/破坏矿石，不掉经验。多种矿石共享标签而掉落不同，或输出物品不唯一时，只有规则能确定代表矿石/首选输出才启用，否则给出歧义诊断而不随机选第一个。

内置矿物预期：

| 绑定材料（24 格） | 无熔炼时基础产物 | 安装熔炼模块 |
|---|---|---|
| 粗铁/粗铜/粗金存储块 | 原铁/原铜/原金×1 | 对应锭×2 |
| 钻石块 | 钻石×1 | 保持 |
| 红石块 | 红石粉×4～5 | 保持 |
| 青金石块 | 青金石×4～9 | 保持 |
| 煤炭块/绿宝石块/石英块 | 煤炭/绿宝石/下界石英×1 | 保持 |
| 下界合金块 | 远古残骸×1 | 下界合金碎片×2 |

绑定仍消耗水平 5×5 中心以外 24 格，检查材料一致，混入不同材料或带方块实体则不转换、不销毁。自动规则按同一明确材料键判断，可支持不同模组提供的同材质块；作者可以配置为要求完全相同方块 ID。

矿物时运模块沿用倍率：先抽基础数量，再熔炼转换，最后乘 2^模块数，上限 ×1024；不重复叠加原版工具 Fortune。第三方原矿熔炉配方若结果属于 c:ingots，默认按两锭转换；有组件、冲突、多产物或非标准熔炼时走显式映射/Java provider。下界残骸不依赖 c:raw_materials，使用明确 debris→scrap×2 例外。

红石/青金石等大基础数量在 ×1024 时会占用多个输出槽；保持原 128×1024 缓冲上限，按真实输出容量选择本轮参与晶体并公平轮转，不能强行溢出或丢弃余量。产物数量使用 long 与正常物品 prototype 保存，不创建 count=1024 的非法 ItemStack。

## 7. 作者 API 与可诊断性

保留现有 CrystalData format=1、档案 ID、SimulationExtensions.registerOutput/registerBindingCondition/registerMobPredicate，以及原配方默认值。已存在的原铁等档案与 netherite 档案不改 ID；下界合金档案显示可改为“远古残骸（下界合金矿物）”。旧晶体重新开始任务即使用新规则，旧已付款任务仍使用旧快照。

自动档案以材料身份生成稳定 ID，例如 `overload_sim:auto/mineral/c/osmium`；不能随 mod 加载顺序改变或给每种矿物注册新物品。缺失模组/档案时水晶保留并显示原因。`TagsUpdatedEvent`、配方及规则重载后统一重建缓存，不能只监听一类数据重载。

新增的数据入口（拟定，不是 alpha.10 已有 API）：

- `data/<namespace>/simulation_mineral/<id>.json`：binding 块/标签、material key、原矿输出或 block_loot、基础数量范围、熔炼映射、优先级/禁用。
- `data/<namespace>/simulation_mob/<id>.json`：entity/实体标签、context、template、loot_table/outputs/provider、独立 chance、数量范围、priority/disabled。
- 公共标签 `overload_sim:simulation_mineral_blacklist`、`simulation_mob_blacklist`，新增条目不覆盖整个内置标签；作者显式规则可选择禁用自动发现。

拟新增 Java 接口，最终方法名在实现前写入独立实现计划：

- `SimulationExtensions.registerMineralResolver(ResourceLocation, MineralResolver)`：返回 Optional<ResolvedMineral>；不能自行扣费/破坏材料。
- `registerEntityEligibility(ResourceLocation, EntityEligibility)`：资格判断接收 LivingEntity，补特殊刷怪蛋/实体类；不替换旧 MobPredicate。
- `registerEntityTemplate(ResourceLocation, EntityTemplateInitializer)`：只初始化未加入世界的临时实体，不能访问实际实体背包。
- 保留 registerOutput 的旧上下文；新增带明确 RNG、晶体身份、机器种类和解析规则的第二版输出接口，不静默改旧签名。
- 新 `BeforeBatchCommit` 可取消最终扣费，`BatchAborted` 只通知取消原因；保留 BeforeBatchStart/Completed 语义，Completed 仍仅在提交后触发一次。

掉落/矿物规则使用同一 resolved output 通路，普通模拟室与多方块均可使用。普通模拟室的预付款任务语义不因本次多方块完成时结算而偷偷改变。

示例：末影龙规则中 outputs 为 `[dragon_egg×1 chance=.25, dragon_head×1 chance=.25, dragon_breath×1 chance=1]`，独立抽取。作者只需覆盖对应 JSON 即可调概率；提供器复杂逻辑则用 Java API。

增加诊断命令与 UI 提示：手中水晶的匹配规则、材料键、代表矿石、基础数量、实体资格/刷怪蛋状态、掉落表/上下文、优先级冲突；空结果区分“正常概率为零”“表为空”“缺少上下文”“解析失败”。诊断查询不扣材料、不生成产物。允许输出当前整合包的矿物/生物适配报告，发现其它模组的空表和缺标签对象。

## 实施与验收顺序

1. 模板输出隔离与 GUI 交互：先复现无线抽走模板和工作时锁槽，再覆盖 simulate、Shift、满背包、多玩家及隐藏槽回收。
2. 完成时结算事务：计时期间 FE/HV/EHV 不减；成功结算只减一次；插入/取出取消且无收费；等待输出/缺费、网络部分提取、重启、旧 paid 迁移各有回归测试。
3. 生物解析层：固定种子测试幻翼/烈焰人/旋风人/岩浆怪；18 种受玩家条件影响的表进行回归检查；龙的四种 egg/head 组合和 breath 必出、凋灵/坚守者显式规则、第三方测试实体/刷怪蛋、GLM 只应用一次。
4. 矿物解析层：红石 4～5、青金石 4～9、残骸与两碎片、第三方粗矿标签、多来源歧义、24 格混材不消耗、自动档案稳定与重载、时运只乘一次、非标准熔炼覆盖。
5. Blockbench 转角、常驻光球与隐形光源：验证 3～7 全角、产出换轮无闪烁、空/非空切换、橙色/过载、服务端实际光照、光源移除和区块恢复。
6. 扩展样例/指南/迁移文档：提供模组金属、生物例外和独立随机输出的可直接复制 JSON；常规与可选 Mek 环境完整测试，原生客户端验收后才打包下一版，保留 alpha.10 回滚标签及 PCL 附属模组备份。

## 来源

- 本地源码：CrystalBinding、PlayerLightningHandler、MobLoot、SimulationControllerBlockEntity、MultiblockSimulationMenu、SimulationBatchPlanner、MultiblockData、SimulationShellModel 与默认 recipe。
- Mojang 1.21.1 原版客户端 JAR 的掉落表；NeoForm 对应 Slime/MagmaCube/WitherBoss/EnderDragon 源码。
- NeoForge 21.1.252 的 Tags.java：确认 c:storage_blocks/raw_*、c:raw_materials/*、c:ores/*、c:gems/lapis、c:dusts/redstone 等实际约定。
- [NeoForge 1.21.1 标签与缓存重载](https://docs.neoforged.net/docs/1.21.1/resources/server/tags/)
- [NeoForge 1.21.1 玩家击杀掉落条件](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/lootconditions/#minecraftkilled_by_player)
- [NeoForge 1.21.1 掉落上下文参数](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/)
- [NeoForge 1.21.1 Global Loot Modifiers](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/glm/)
