# 过载模拟首版实施计划

> For agentic workers: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 做出可在既有 PCL 测试实例加载的粉白风格附属模组，完成水晶绑定、培养及模拟生产。
**Architecture:** 服务端负责状态与事务；不可变水晶组件保存档案；数据包配方管理成本和产物。AE2LT 收集器仅在版本限定的 Mixin 内适配；模拟室通过公开频率 API 接入网络。
**Tech Stack:** MC 1.21.1、Java 21、NeoForge 21.1.252、AE2 19.2.17、AE2LT Reborn 2.1.0、Thunderbolt 2.0.0、GuideME 21.1.19、Blockbench。
**Spec:** ../specs/2026-09-30-overload-simulation-design.md

## Global Constraints
- 独立仓库 D:/MinecraftDev/OverloadSimulation；codex/initial-prototype 分支；本地提交，不发布。
- 32 个矩阵最多 128 并行；基础 1，安装后 4N；4 张加速卡，速度 2^A，下限 1 tick。
- 副手空白水晶真实雷击 10% 记录半径 5 格最近 Mob；同一道雷电去重。
- 绑定仪式水平 5×5 排除中心，校验全部 24 格后消费；作物/树苗保留土壤。
- 收集器只在实际捕获成功后提交；模拟水晶不可被原版培养覆盖。
- FE 与 EHV 每份费用按实际并行计费；输出堵塞保留已固定任务；不复制实体库存。
- 所有物品使用粉白风格 Blockbench 项目，导出资源随源码维护。

## Review Focus
- 雷击重复闪光、已取消事件、网络无容量：不绑定或增加培养。
- 区块未加载、材料混用、数据包移除：不消费、不更改档案。
- 重启、输出堵塞、修改升级：任务产物和费用不重抽、不重复支付。
- 战利品装备/玩家击杀限制、多个并行：使用独立基础上下文，无实体库存复制。
- 自动化部分接收、无线失联：转移实际数量，停产但保存库存。

### Task 1: 工程与核心规则
Files: build.gradle; core/SimulationRules.java; test/core/SimulationRulesTest.java。
Interfaces: parallel(int)、duration(int,int)、batchEnergy(long,int)、withinRadius(double,double)、chance(double,double)。
- [ ] 写矩阵 0/1/2/32、非法数量、速度下限、概率边界和费用溢出的测试，运行看到失败。
- [ ] 实现核心规则，测试通过，建立基线提交。

### Task 2: 数据、API 与水晶
Files: ModContent、api/CrystalData、api/CrystalDataAccess、data/SimulationRecipe、data/SimulationData、api/SimulationEvents。
Interfaces: 绑定/培养/生产四种 RecipeType 及 Codec；档案通过可重载 JSON 管理；稳定组件持久化和网络编解码。
- [ ] 验证错误数据拒绝、稳定档案和不可变组件；实现注册、tooltip、创造标签和示例配方。
- [ ] 构建检查序列化与客户端分离，提交。

### Task 3: 收集器与副手雷击
Files: compat/CollectorMixin、compat/CollectorInventoryMixin、binding/CrystalBinding、binding/PlayerLightningHandler。
Interfaces: onCaptured(collector,natural)、onPlayerStrike(player,bolt)，提交前取消事件与完成通知。
- [ ] GameTest 覆盖 24 格同类材料、错一格保持、土壤保留、培养与最近生物；同雷电重复触发只执行一次。
- [ ] 用固定 2.1.0 JAR 字节码确认注入点；实现服务端流程，提交。

### Task 4: 模拟室、升级与网络
Files: machine/SimulationChamberBlock、SimulationChamberBlockEntity、SimulationMenu、client/SimulationScreen。
Interfaces: FrequencyBindingHost、FrequencyBindingMenuHost；FE/item capabilities；持久任务快照。
- [ ] 测试并行费用、输出容量、任务恢复、邻接部分插入；实现库存、FE、EHV 提取和无线生命周期。
- [ ] GUI 展示进度、能源、实际并行与状态；矩阵/卡/水晶工作时锁定；输出六面配置。
- [ ] 生物战利品直接生成，不生成真实实体；提交。

### Task 5: Blockbench 外观与指南
Files: art/*.bbmodel；assets/overload_sim；GuideME pages。
- [ ] 参考 AE2LT 原资源的粉白配色；制作 3 种水晶和模拟室可编辑模型/贴图。
- [ ] 在 Blockbench 打开项目并检查/导出；模型资源校验；G 键指南、中文翻译和配方，提交。

### Task 6: 验证与交付
Files: README、docs/API.md、scripts/bootstrap.ps1、scripts/install-test.ps1。
- [ ] build、JUnit 与 GameTest；修复发现的问题，独立代码审查。
- [ ] 将构建 JAR 安装到既有 PCL 测试实例，实际客户端启动检查；记录未验证项。
- [ ] 版本标签和回滚说明；保留源码、Blockbench 项目与可测试 JAR。
