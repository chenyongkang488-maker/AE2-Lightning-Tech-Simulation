---
navigation:
  title: 强能工作站
  icon: overload_sim:high_energy_workstation
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:high_energy_workstation
  - overload_sim:laser_frame
  - overload_sim:laser_glass
  - overload_sim:laser_optical_port
---

# 强能工作站

工作站加工当前加载的过载处理工厂配方，整合包和脚本修改会同步生效。它消耗配方中的物品与流体，激光替代原配方的 FE 和闪电费用。

专属配方为 256 个过载水晶合成 4 个[超限水晶](transcendent-crystal.md)，仅能在强能工作站中加工。

控制器在闪电装配室中制造：64 个过载处理工厂、16 个闪电坍缩矩阵、64 个过载处理器，消耗 256 个极高压与 2,000,000 FE。

## 搭建与激光输入

外尺寸为 7×7×7，需要 1 个控制器、1 个激光光口、141 个激光框架和 75 个激光通光玻璃。局部坐标 x 向右、y 向上、z 朝内，各轴范围为 0～6。控制器位于 (3,0,0)，正面朝外；光口位于 (3,3,6)。顶面、底面和背墙除这两个位置外全部使用框架；其余三面在框架边界内各填 5×5 玻璃。内部 5×5×5 空间留空。

从背墙外侧向光口照射激光。单束强度必须严格大于 1,000,000,000 FE，直接照入的多束弱光不会相加；可先通过偏折矩阵汇流，再将合并后的光束照入。不要求聚焦。材料与输出空间充足时，两个连续有效 tick 完成一批，每批最多执行 1024 次配方。断光或结构失效会重置未完成进度，不扣本批材料。

右键控制器或成型后所属框架打开菜单。自动搭建先使用玩家背包，再从控制器已连接的 AE 网络补齐；创造模式无需材料。投影按钮可预览施工位置，障碍和未加载位置会阻止搭建。创造管理员可在已加载的空区域执行 `/overload_sim workstation demo`，生成工作站与预充有限资源的激光链。

## 库存与运输

控制器有九个输入格、九个输出格，每格最多保存 1,000,000 个同类同组件物品。悬停查看精确数量；左键移动普通堆叠，右键移动一个，Shift 点击在机器与背包之间转移。

输入和输出流体槽各容纳 1,024,000,000 mB，保留流体组件，悬停查看精确余量。拿装有流体的容器点击输入槽可灌入，空容器可从任一槽取出；输出槽不能灌入。每次处理一个容器，必须有位置放置操作后的容器。

控制器任意面都接受物品和流体输入并提供产物。管道只向输入缓冲放入、只从输出缓冲抽取；不能抽走输入材料，也不能灌入输出缓冲。其它外壳可打开菜单。

工具栏的「自动弹出到相邻容器」与「自动导出到 AE 网络」是两个独立开关，默认关闭。自动输出的物品与流体上限分别为 2,147,483,647 个物品/tick、2,147,483,647 mB/tick，由 AE 导出和相邻弹出共用。实际只按容器、流体槽或网络接受的数量扣除。拒收产物留在工作站中，接收方容量与插入规则仍然生效。

## 数据包与 Java API

专属配方位于 `data/<namespace>/recipe/<path>.json`，类型为 `overload_sim:high_energy_workstation`。例如：

```json
{
  "type": "overload_sim:high_energy_workstation",
  "priority": 10,
  "inputs": [
    {"ingredient": {"tag": "c:ingots/iron"}, "count": 4},
    {"ingredient": {"item": "minecraft:redstone"}, "count": 1}
  ],
  "results": [{"id": "minecraft:gold_ingot", "count": 2}],
  "totalEnergy": 5,
  "lightningCost": 1,
  "lightningTier": "high_voltage"
}
```

最多九项带数量的输入、一项物品结果；可选 `inputFluid` 和 `resultFluid` 使用 NeoForge FluidStack 格式。至少声明一种输入和一种输出。`totalEnergy` 至少为 5，`lightningCost` 为正整数，工作站不收取这两项费用。材料保留标签、替代项和组件匹配条件。纯流体输入配方要求物品输入格全部为空；只生成配方声明的产物。

选配方时先比较可执行并行数，再依次按 `priority`、输入种类数、总物品需求量降序比较，最后按配方 ID 排序。覆盖 `data/overload_sim/recipe/transcendent_crystal.json` 可修改超限水晶专属配方。修改后执行 `/reload`。

Java 扩展在 common setup 调用 `WorkstationRecipes.registerSource(id, source)`，返回最终 RecipeManager 中当前可执行的 `Entry(id, recipe)`，保留材料判断及组件。自定义来源改变后调用 `WorkstationRecipes.invalidate()`。`WorkstationBuffer.count(slot)` 读取数量，普通提取返回合法物品堆叠。完整参考见 `docs/workstation-api.md`。
