# Instrument Polish and FE Cells Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans for each owned task. Parent coordinates Gradle and shared-file integration. Steps use checkbox syntax.

**Goal:** 完成用户七项仪器反馈，按实际产量显示粒子、放宽约束器水平光口、改用 FE 单元补电，并统一强能工作站部件与流体界面。

**Architecture:** 保留现有服务端资源结算与保存 ID，在光段上同步瞬态产量和发射时间；约束器从已有光口解析模板。共享 Applied Flux 适配层直接进行整数 FE 存储交易；工作站用现有激光部件形成，兼容旧部件。

**Tech Stack:** Minecraft 1.21.1 / NeoForge 21.1.255 / Java 21。编译保持现有 AE2/AE2LT/Thunderbolt 兼容基线，验证两个既有前置组合与可选 Applied Flux 2.1.6。

**Spec:** 下方七项设计及本轮用户澄清；用户沿用拟定方案后直接制作的授权。

## 设计约定

1. 正产量才生成对应次数的有限粒子飞行动画，零产量、取消事件和亚粒子累积不生成循环粒子流。实际接收仍由服务端即时原子结算，动画不重复发料。保留公开五参数光段构造器；新数量/时间有界。
2. 亚空间稳定蓝球半径约 1.125，包住核心附件四角；首次启动的收缩过程保留。两道外环改为淡橙色、像素阶梯薄膜，爆发矩阵电弧保留。
3. 用户澄清：球形约束器控制器留在底座，球体东南西北任一面中心均可选放光口。一个或多个光口、至少一个；自动搭建默认仍只放一个，并保留已经选择的其它光口。不强制加载区块，旧结构/库存不丢失。
4. 强化矩阵用 100k～9M FE、步长 100k 的水平拖动滑条，服务器验证范围、步长、当前菜单身份和成型状态，保留显示实际选择值。
5. 所有 AE FE 补电路径使用 Applied Flux FE 存储键，不从 AE 能源服务换算 FE。缺少 Applied Flux、FE 单元为空或满时不备用读写 AE 电池。物理 FE 能力保持可用，普通网络运行的 AE 开销保持上游语义。旧结算、余量和退款账本不丢失。
6. 用户澄清：约束矩阵工作时伤害进入内部的生物；成型后阻止内部自然刷怪。使用精确球形内部范围与现有激光伤害合并，单次扫描不重复伤害。指令、刷怪蛋及已保存生物不作为自然生成阻止。
7. 强能工作站撤下三个专用部件和获取配方，模板改用激光框架、激光通光玻璃、激光光口。旧 ID 只保留兼容加载，旧结构/物品转换为对应通用部件，控制器样式统一到激光设备。顶底仍封闭不透光。界面使用真实液体材质/填充量的两个流体槽，支持液体容器操作与精确余量提示，保留 200×238 紧凑窗口布局。

## Global Constraints

- 名称闪电科技：模拟 / Lightning Tech: Simulation；新 JAR lightning-tech-simulation；保留 overload_sim、dev.overloadsim 和旧保存身份。
- 下一本地开发版 0.2.0-alpha.11，不改前置、不发布远程版本；测试 Minecraft 始终窗口化。
- 多人客户端只提出操作，材料、FE、流体、结构和伤害均由服务器决定；不丢失、复制或按动画重复结算。
- 同一工作树一次只有一个 Gradle 执行；独立任务只编辑分配的文件，共享宿主集成由父代理负责。

## Review Focus

- 源关闭、重发快照及时间回退不能重播或伪造粒子；多个生产点和最大合法批次仍有界。
- 改换一个光口而保留另一个不能留下旧路径；零光口不能成型，已有侧口不能被自动搭建替换。
- FE 单元为空而 AE 电池满时，所有仪器补电仍为零；取出后拒收/退款必须守恒，缺模组不链接可选类。
- 共用框架后，工作站和其它结构的所有权、远角菜单、拆除和自动搭建保持正确；旧结构兼容转换不得收费或吞库存。
- 流体材质包含组件语义；满物品栏、堆叠容器、错误菜单、关闭侧面和输出槽反向灌入不能复制或丢液体。

## Tasks

### A. 粒子、亚空间与约束矩阵场

- [x] 基于现有公共 API 添加零产量、事件取消、粒子实产与场内生物/自然生成回归；父代理运行 RED。
- [x] 修改 LaserSegment、LaserNetwork、LaserRenderer、LaserEnergyEffects、CoreCollapseCycle，添加独立约束场几何/事件帮助类；LaserContent 注册事件。公开旧构造器兼容，飞行只播放一次。
- [x] 添加数量/时间 codec、极限批次、快照重复、边界及活动状态验证；验证核心附件包围与薄膜像素几何。

### B. 约束器多面光口

- [x] 添加四面单口、多口、零口、路由方向、实际终点和路径失效回归；父代理运行 RED。
- [x] LaserStructureGeometry 与新端口帮助类解析模板；父代理集成 LaserMachineBlockEntity.structureTemplate 与口判定、ConstructionProjection。LaserNetwork 由 A 的代理整合接收口检查和实际终点。
- [x] 验证旧底座/旧球形结构保存恢复、自动搭建选择与材料数量、不加载区块。

### C. AE 上的整数 FE 存储

- [x] 对普通模拟室、多方块模拟室、线圈及激光仪器添加真实 FE 单元与空 FE/满 AE 的回归；父代理运行 RED。
- [x] 添加 compat/AppliedFluxEnergy；修改 SimulationChamberBlockEntity、SimulationControllerBlockEntity、CoilEnergy、LaserGridBridge。父代理集成 LaserMachineBlockEntity 如需的新余量账本接口。
- [x] 调整通用 powered fixture 使用本地预充 FE，保留专门供电测试的真实有限存储；验证缺模组、退款/满容量及接收器输出不会换算 AE。

### D. 共用工作站部件、滑条和流体槽

- [x] 添加共用部件形成/菜单/迁移、滑条服务端非法操作、容器守恒回归；先运行 RED。
- [x] 修改 WorkstationStructure/Content/PartBlock/BlockEntity/Menu，LaserPartBlock、FrameMenuAccess、ConstructionProjection；旧专用部件隐藏并兼容转换，更新资源生成/校验和指南。
- [x] LaserMenu 新增经过校验的绝对强化量操作；LaserScreen 使用拖动滑条。
- [x] HighEnergyWorkstationScreen 显示两液体槽材质与液位，菜单同步流体身份/组件并通过标准 NeoForge 容器 API 进行服务端取放，满库存无损拒绝。

### E. 验收和本地安装

- [ ] 复查共享文件、运行 JUnit、两组完整 GameTests 和资源/独立 JAR 校验，解决发现的问题。
- [ ] 窗口客户端实测滑条、四面光口、实产粒子/薄膜、FE 单元和液体槽；保存原生 F2 截图。
- [ ] 本地集成 alpha.11，重建并核对两个工作区的产物哈希；备份 alpha.10，仅替换测试实例附属 JAR，核对其余模组与配置哈希，记录验证和安装收据。
