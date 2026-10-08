---
navigation:
  title: 激光与粒子
  icon: overload_sim:high_energy_laser_emitter
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:high_energy_laser_emitter
  - overload_sim:laser_constraint_matrix
  - overload_sim:laser_enhancement_matrix
  - overload_sim:laser_burst_matrix
  - overload_sim:laser_deflector_matrix
  - overload_sim:laser_focus_matrix
  - overload_sim:collapse_core
  - overload_sim:particle_constrainer
  - overload_sim:laser_frame
  - overload_sim:laser_glass
  - overload_sim:laser_source_crystal
  - overload_sim:laser_optical_port
---

# 激光与粒子

沿光路依次搭建：发射器 → 矩阵 → 聚焦矩阵 → 坍缩核心 → 粒子约束器。粒子从核心另一侧沿激光前进方向离开；约束器的真实光口朝向核心，位于该侧七格以内。

光强与下表费用按每 20 tick 一批计量。G 打开本页，右键控制器可设置设备并查看精确资源值。坍缩核心为被动器件，不接受 AE 线缆和频率绑定；其它控制器可使用 AE 线缆、结构上的过载 ME 接口或共享过载频率连接网络。

## 搭建

普通直线设备向控制器朝向的反方向出光，支持六个空间方向。相邻结构表面之间至少留一格空气。

发射器外范围为 5×5×5，控制器位于后面中央，源水晶在前方两格，出光口再前方两格。框架位置可通过自动搭建投影查看。

约束、强化、爆发、聚焦矩阵共用 7×7×1 环。F=激光框架，C=矩阵控制器，.=空气。从控制器正面看：

```
..FFF..
.F...F.
F.....F
F.....F
F.....F
.F...F.
..FCF..
```

光轴中心位于控制器沿施工上方三格。水平光路的施工上方为世界上方，竖直光路的施工上方为北。

偏折矩阵为世界竖直的 7×7×7 结构，球心位于底部控制器上方三格。以球心为原点，把满足 7 ≤ x²+y²+z² ≤ 12 的外壳格填框架，东、南、西、北四个面中央换光口；顶部为框架，底部为控制器，其它格留空。只有水平光口能通光。

约束器默认外尺寸为 7×9×7。先搭两层 7×7 框架底座，上层正面中央放控制器。以控制器为原点，底座向前延伸 0～6 格，向左右各三格，底层在下方一格；球心在前方三格、施工上方四格。以球心为原点，x/y/z 各为 -3～3，满足 7 ≤ x²+y²+z² ≤ 12 的格属于球壳；其中任一坐标为零用框架，其余用玻璃，其它格留空。上游面中央的框架换为光口。默认材料为 138 框架、56 玻璃、1 光口和 1 控制器；球面其它水平面中央也可放光口。核心须与实际光口同轴，光口朝向核心。

## 光路设置

|设备|每批行为|
|---|---|
|发射器|支付 100,000 FE，发射 100,000 FE 光强，射程六格|
|约束矩阵|支付 100,000 FE，重新提供六格射程|
|强化矩阵|增加并支付所选 100,000～9,000,000 FE，步长 100,000|
|爆发矩阵|支付 64 个极高压，光强翻倍|
|偏折矩阵|把水平入射平均分到启用的输出方向|
|聚焦矩阵|汇聚到前方第七格的坍缩核心|

强化与爆发保留剩余射程，单束光强上限为 1P FE。没有有效入射的矩阵不收运行费用。

偏折方向按钮依次切换关闭、输入、输出，新设备四面默认关闭，可以设置多个输入和输出。关闭全部输出后不再产生下游光；仍在运行的上游设备按自己的规则收费。上下方向不能设置通光。

同一批次有效输入的光强相加，再均分给开启的输出口；下游收到合并后的光束。重复更新不会再次扣除同批费用。停止一个来源后，其余输入仍有效。总输入超过 1P FE 时，该偏折矩阵停止输出。

聚焦途中清除光轴上核心前的可破坏方块，不可破坏方块阻断光路。核心在达到 1G FE 时激活，累计每 512G FE 产生一粒子并保留余量。激光路径与工作中的约束场会伤害生物。约束场半径为 2.5 格，成型时阻止自然生成，只在工作时造成伤害。每两 tick 检查伤害，重叠路径或场只使用最大值；单次伤害为光强 FE ÷ 10,000 × 配置倍率，上限 1,000,000 伤害点，默认倍率为 1。

## 粒子与密封耗材

约束器容量为 125,000 粒子，FE 缓冲容量为 1024P FE。每粒子对应 512G FE 入射激光能量。投入的耗材不能通过菜单或自动化取回。一件耗材支付 100 个有效工作 tick；没有粒子或无法容纳一整次转换产能时，已支付时间暂停。

下表为每 20 个有效 tick 的平均处理能力，条件为粒子与输出空间充足。G=1,000,000,000 FE，P=1,000,000,000,000,000 FE。

|耗材|输入能力 / 20 tick|平均产能 / 20 tick|转换比例|每整粒子产能|
|---|---|---|---|---|
|下界之星|1G FE|1G FE|100%|512G FE|
|下界合金块|16G FE|16G FE|100%|512G FE|
|奇点|256G FE|128G FE|50%|256G FE|
|过载奇点|1024G FE|2048G FE|200%|1024G FE|
|钚球 / 钋球，可选通用机械|0.125P FE|0.5P FE|400%|2048G FE|
|超限水晶|0.125P FE|0.5P FE|400%|2048G FE|
|反物质球，可选通用机械|1P FE|8P FE|800%|4096G FE|

前两项能力除以 20 得到平均 FE/tick。每次转换消耗一整粒子并交付其完整产能，一个有效 tick 可在处理额度、粒子库存和输出空间允许时批量转换多粒子。不足一粒子的进度跨耗材窗口保留，并写入存档。例如下界之星在粒子充足时，累计 10,240 个有效 tick 才转换一粒并输出 512G FE，不会每 20 tick 立即输出 1G FE。单件 100 tick 耗材不保证产出其名义平均值。只保留不足一粒子的进度，缺粒子或输出空间不会积攒无限后续爆发额度。

激光设备从应用通量 FE 存储单元补电；约束器向这些单元输出，只按实际接收量扣缓存。来源为空、接收单元已满或未安装应用通量时，对应交易停止。AE 能源单元供给网络运行能量，也支持外部 FE 连接和已注册能量接收后端。

开启爆炸配置时，粒子溢出或拆除仍有粒子的约束器会触发以光学中心为圆心、半径十二格的球形爆炸，并播放爆炸音效与粒子动画。先停止上游光路并消耗库存粒子再拆解；空结构可正常调整。

## 自动搭建

放置并确定控制器朝向，打开菜单点击左侧自动搭建。生存模式先用主背包和副手，再从控制器当前活动 AE 网络补齐。未成型时可直接接线，离线也可只用背包；创造模式无需材料。

只补放缺失部件，保留合法接口与已选择的接收光口。材料不足、障碍或未加载位置会阻止施工。放置取消时退还材料，网络和背包都拒收的退款掉落给玩家。重复搭建完整结构不耗材。投影按钮预览位置，成型后清除。右键所属框架可打开控制器，潜行与扳手操作沿用原行为。

创造管理员可在已加载空区域执行 `/overload_sim laser build <kind>`；kind 为 `emitter`、`constraint`、`enhancement`、`burst`、`deflector`、`focus`、`core`、`receiver`。`/overload_sim laser demo` 生成预充有限资源的完整链。激光加工配方见[强能工作站](high-energy-workstation.md)。

## 配置与 API

`config/overload_sim-laser.toml` 提供 `emitterFePerBatch`、`constraintFePerBatch`、`burstExtremeLightningPerBatch`、`laserDamageMultiplier`、`explosionEnabled`、`explosionBlockBudgetPerTick`，默认依次为 100,000 FE、100,000 FE、64 个极高压、1、true、每 tick 扫描 1024 格。将 `explosionEnabled` 改为 false 可关闭爆炸及其音效、动画、伤害和破坏，并丢弃尚未完成的爆炸任务。旧 `explosionVerticalRadius` 不再改变球形范围。爆炸保留不可破坏方块，遵循保护事件，只处理已加载区块；剩余任务不会在重启后继续。

耗材规则位于 `data/<namespace>/particle_conversion/<id>.json`，修改后 `/reload`。例如覆盖 `data/overload_sim/particle_conversion/nether_star.json`：

```json
{
  "type": "overload_sim:particle_conversion",
  "ingredient": {"item": "minecraft:nether_star"},
  "consumable_count": 1,
  "consumable_duration_ticks": 100,
  "input_energy_per_batch": "1GFE",
  "energy_per_particle": "512GFE"
}
```

`ingredient` 指定一个物品或物品标签。`input_energy_per_batch` 是 20 tick 输入能力，`energy_per_particle` 是每整粒子的产能，后者与 512G FE 的比例决定转换效率。能量接受精确整数 FE 或 `kFE`、`GFE`、`PFE` 单位字符串。输入能力范围为每批 1 FE～1P FE，每粒子产能须为正整数 FE；产能大于 1024P FE 缓冲容量时不能加工。持续时间为 1～1,000,000 tick，每窗口消耗一件耗材，每次完成转换消耗一粒子。按稳定 ID 顺序匹配，修改内置规则应直接覆盖同一 ID。

按间隔配置的规则使用 `particle_interval_ticks`，不同时声明 `input_energy_per_batch`；两字段互斥，间隔不能大于持续时间。已付费规则被移除或变得不兼容时暂停工作，缺失可选材料只跳过自己的规则。`ParticleFuelData.diagnostics()` 提供错误诊断。

Java 在 common setup 通过 `dev.overloadsim.api.LaserExtensions` 注册扩展。`registerModifier` 修改光强和射程；`registerTarget` 接收光束，其 `deactivate` 释放来源；`registerFuelProvider` 提供可稳定 `match/resolve` 的耗材 ID；`registerEnergySink` 返回实际接收的 long FE，模拟时不能修改资源。重复 ID 拒绝注册，完整参考见 `docs/laser-particle-api.md`。
