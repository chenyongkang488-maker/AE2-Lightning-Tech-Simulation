# 过载模拟 Overload Simulation

Minecraft **1.21.1 / NeoForge 21.1.252** 的 AE2 闪电科技附属模组。当前版本为 **0.1.0-alpha.6 测试原型**。

项目在 `D:\MinecraftDev\OverloadSimulation`，独立 Git 仓库的 `codex/resonance-coil` 分支上。原来的 1.19.2 工程不参与构建。

## 游戏内容

- 三阶段模拟电鸣水晶：空白、绑定、完美。数据使用可序列化且同步的 Data Component，完美水晶身份固定，便于 AE2 模板识别。
- 闪电收集器成功接收雷击后，扫描水平 5×5 的 24 格。矿物消耗同种材料块；作物、树苗消耗植物并保留土壤。
- 左手空白水晶受雷击，10% 概率记录球形半径 5 格内最近的存活 Mob 类型。同一道雷不会重复抽取或培养。
- 绑定后额外 10 次有效雷击变成完美水晶，可重复作为模拟室模板。
- 每次操作消耗 1 个 ME 网络高压闪电（HV）；在线时持续按 AE2 的 PowerUnit 标准换算从网络充入 FE，默认每 tick 最多 10,000 FE，缓冲上限 2,000,000 FE，也支持外部 FE。矩阵 0 个为 1 并行，1 个为 4 并行，32 个为 128 并行。
- 右上角使用 AE2 原生四格展开升级栏，每格一张加速卡；可以手持加速卡直接右键机器插入。旧版堆叠卡槽自动迁移；运行中的批次保留原加速快照。
- 批次开始时按实际并行扣费并固定随机产物。任务、剩余时间和待输出物品随区块保存；网络离线暂停，输出堵塞等待。
- 标准物品与 FE 接口、六面自动弹出、闪电科技共享过载频率界面及记忆卡设置。
- 长按 G 打开 AE2/GuideME 的对应指南页面。
- 谐振雷鸣线圈：水平悬浮的粉白线圈头与八帧粉色电弧。原生过载装备工作站安装核心和模块并绑定网络；短按右键松开劈目标，蓄力 1.5 秒松开劈自己，默认消耗 10 HV 人工雷，极高压模块开启后消耗 10 EHV 自然雷。
- 拟态模块提供下界合金采集等级和电流挖掘；前置满足后可安装终极破坏、效率 X、时运 V、精准采集模块。在 G 界面配置，精准开启时优先于时运。支持原生 T1/T2/T3 能量模块、手持及工作站持续 AE FE 充电。挖掘与近战默认消耗 200 FE。

默认档案为铁、铜、金、钻石、下界合金，小麦、胡萝卜、马铃薯、甜菜根，七种树苗及生物战利品。默认排除凋灵和末影龙，可通过数据包覆盖。

矿物使用 24 个粗矿块；钻石、下界合金用对应储存块。收集器中心不计入。作物种在收集器所在高度的耕地上方，树苗种在相同高度的泥土类方块上方。默认接受自然雷、人工雷及指令 `/summon lightning_bolt`。配方可设置 `allow_artificial=false` 限制人工雷。

生物战利品按无玩家的死亡上下文抽取，不复制装备、背包、个体 NBT，也不假造玩家击杀或抢夺。拆除正在加工的机器会丢失未完成任务和已支付费用；已经加工完但尚未装入输出槽的物品会掉落。机器运行中锁定输入槽。

## 构建与运行

需要 Java 21 JDK。四个前置的下载地址及 SHA-256 固定在 `scripts/dependencies.json`，二进制依赖不会提交到 Git，也不打包进附属模组。

```powershell
.\scripts\bootstrap.ps1
.\scripts\build.ps1 -GameTests
.\scripts\build.ps1 -Client
.\scripts\install-test.ps1
```

本机脚本优先使用 D 盘已有的 Gradle 8.8；其他机器可使用 Gradle Wrapper 下载 8.8。常规构建产物为 `build/libs/overload_sim-0.1.0-alpha.6.jar`，sources jar 提供源码。

前置版本固定为 AE2 19.2.17、AE2 Lightning Tech Reborn 2.1.0、Thunderbolt Core Reborn 2.0.0、GuideME 21.1.19。升级前置后需要复测 collector 的两处 Mixin 和 高压闪电桥接接口。

## Blockbench 贴图

`art/simulation_crystals_layers.bbmodel` 保存 16×16 灰白模拟水晶、粉彩完美水晶及独立的 12 帧闪电图层。完美水晶同时用于创造标签图标和模组列表标志。水晶轮廓改编自闪电科技原贴图，其 CC BY-NC-SA 3.0 署名与许可见 [素材说明](THIRD_PARTY_NOTICES.md)。

`art/overload_simulation_chamber_hollow.bbmodel` 保存原创粉白框架、四角磁场发生器、六面透明玻璃护罩和预览水晶。玻璃保留透明中心与少量反光，并具有对应的薄层碰撞；底部玻璃覆盖原金属平台。预览水晶不导出到静态方块模型；游戏中由客户端渲染器根据槽 0 同步状态显示悬浮、旋转的完美水晶和电弧，取出水晶即消失。

本地插件 `art/overload_sim_visuals.js` 可在 Blockbench → 文件 → 插件 → 从文件加载，然后选择工具菜单 → 绘制模拟水晶与镂空模拟室。它会覆盖当前素材和分层工程，手绘修改前请先用 Git 保存。旧版 `art/overload_sim_pink_white.bbmodel` 保留供回看。

`art/resonance_coil.bbmodel` 保存 47 个部件的水平悬浮线圈、短柄和独立闪电图层；`art/resonance_coil_workshop.js` 在 Blockbench 工具菜单提供“绘制谐振雷鸣线圈”，导出模型、八帧动画与六个模块图标。这些素材为原创。

## 魔改 API

见 [数据包与 Java API](docs/api.md)，可 `/reload` 的四种专用配方与 simulation_profile、命名输出提供器、结构条件、实体条件和可取消前置事件。当前不直接依赖 KubeJS；已有 Java API 可供桥接模组使用。AE2 支持通过总线运输及具有稳定组件的模板，原型没有实现按需自动合成的原生 AE2 CPU 接口。

## Git 回滚

```powershell
git log --oneline
git diff
git switch -c codex/next-feature
git restore --source <提交号> -- <文件路径>
git revert <提交号>
```

`git revert` 会记录反向提交，适合整体撤销已保存的改动。先提交当前工作再切换历史版本。没有配置远程仓库，也没有上传或发布。

## 测试边界

JUnit 检查并行、加速、概率边界和溢出；服务器 GameTest 检查实际物品数据、方块消耗、耕地保留、培养和依赖集成。游戏内长期存档、多人权限及更多第三方生物仍需后续联调。测试报告位于 `build/test-results` 和 `run-gametest`，发布前请重新运行测试。
