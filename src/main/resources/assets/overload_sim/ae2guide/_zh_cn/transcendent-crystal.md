---
navigation:
  title: 超限水晶
  icon: overload_sim:transcendent_crystal
  parent: overload_sim:high-energy-workstation.md
item_ids:
  - overload_sim:transcendent_crystal
---

# 超限水晶

<ItemImage id="overload_sim:transcendent_crystal" scale="3" />

## 制造

[强能工作站](high-energy-workstation.md)将 256 个过载水晶转换为 4 个超限水晶。沿用工作站单束激光严格大于 1,000,000,000 FE 的条件，两个有效 tick 完成，最多 1024 并行，仅消耗材料。过载处理工厂不能加工此配方。

## 特斯拉线圈

将超限水晶放入 AE2 Lightning Tech 特斯拉线圈的耗材槽，将闪电坍缩矩阵（`ae2lt:lightning_collapse_matrix`）放入矩阵槽。线圈接入在线网络，网络需要有 64 个极高压闪电的存储空间。

每次消耗 1 个超限水晶和 500,000 FE，耗时 5 个有效 tick，产出 64 个极高压闪电，不消耗已有高压闪电。缺少矩阵、能量或网络输出空间时不能完成。

## 粒子约束器

将水晶放入约束器的密封耗材槽，可把粒子转换为 FE。投入的耗材不能取回，此用途无需安装通用机械。产能、已支付持续时间与暂停条件见[激光与粒子](laser-particle.md)。

## 数据包

覆盖 `data/overload_sim/recipe/transcendent_crystal.json` 可修改工作站制造配方，类型为 `overload_sim:high_energy_workstation`，使用带数量的 `inputs` 和 `results` 数组。完整格式见工作站页面。

覆盖 `data/overload_sim/particle_conversion/transcendent_crystal.json` 可修改约束器耗材规则，字段与限制见激光页面。修改后执行 `/reload`。
