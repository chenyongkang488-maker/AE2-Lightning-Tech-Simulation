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

在强能工作站中，**256 个过载水晶合成 4 个超限水晶**。这是工作站专属配方，不能在过载处理工厂中加工。沿用工作站的激光条件、2 tick 加工与最多 1024 并行；仅消耗材料。水晶保留本体过载水晶的像素轮廓及动画，颜色为淡蓝色。

将超限水晶放入闪电科技特斯拉线圈的耗材槽，同时在矩阵槽装入**闪电坍缩矩阵**。每次加工耗时 **5 个有效 tick**，消耗 **1 个超限水晶、500,000 FE**，向网络输出 **64 个极高压闪电**，不消耗已有高压闪电。线圈连接的网络需要有极高压存储空间。

超限水晶也可放入粒子约束器的密封耗材槽，效果与钚球、钋球相同：每件提供 **100 个有效工作 tick**，每 tick 消耗 **1 个粒子**并产生 **2048G FE**。没有粒子或缓存空间不足时暂停，保留已支付耗材的剩余时间。此用途无需安装通用机械。

配方与粒子转换规则分别位于 `data/overload_sim/recipe/transcendent_crystal.json`、`data/overload_sim/particle_conversion/transcendent_crystal.json`，整合包可用数据包覆盖。
