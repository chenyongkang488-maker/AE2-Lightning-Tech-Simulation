---
navigation:
  title: 过载模拟
  icon: overload_sim:overload_simulation_chamber
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
item_ids:
  - overload_sim:blank_simulation_crystal
  - overload_sim:simulation_crystal
  - overload_sim:perfect_simulation_crystal
  - overload_sim:overload_simulation_chamber
---

# 过载模拟

<ItemImage id="overload_sim:blank_simulation_crystal" scale="3" />

空白模拟电鸣水晶由电鸣水晶、福鲁伊克斯水晶和紫水晶碎片合成。

## 雷击绑定

把空白水晶放入闪电收集器。以收集器为中心，在同一高度的 5×5 平面填满 24 个相同的粗矿块；钻石和下界合金使用对应储存块。收集器成功收集自然雷击后，这些方块被消耗，水晶记录对应矿物。

作物使用 24 格耕地上方的同类作物，绑定时只消耗作物。树木使用 24 格泥土类方块上的同类树苗，保留土地。默认支持铁、铜、金、钻石、下界合金、四种农作物和七种树苗。

左手拿空白水晶受到自然雷击时，有 10% 概率记录半径 5 格内最近的存活生物。只记录生物类型，不复制装备、背包或个体数据。默认禁止凋灵和末影龙。

已绑定水晶再接受 10 次有效自然雷击后成为完美水晶。可以继续放在收集器中培养，也可以拿在左手受雷击。指令生成和人工雷击默认不推进；整合包作者可在配方中允许人工雷击。

## 模拟室

模拟室接入在线 ME 网络并取得频道，同时接收 FE 电力与 EHV 闪电能量。将完美水晶放入第一个槽，模板可重复使用。默认一次操作需要 1000 FE、10 EHV 和 200 tick；下界合金成本更高。

闪电坍缩矩阵放第二槽：无矩阵为 1 并行，每个矩阵提供 4 并行，最多 32 个即 128 并行。第三槽可放最多 4 张 AE2 加速卡，每张将耗时减半。第四槽供自定义配方消耗辅助材料。

能量和辅助材料在批次开始时按实际并行一次扣除。随机战利品每次操作独立抽取，开始后固定并随任务保存。输出堵塞时等待空间；网络离线时暂停。拆除尚未完成的任务会丢失该任务，不能提前取得产物。

自动弹出可开关，并单独开关六个方向。AE2 输入/输出总线可通过标准物品接口运输。频率按钮使用闪电科技共享的无线过载控制器界面。

生物产物按“无玩家参与的死亡”战利品表抽取，因此依赖玩家击杀、抢夺或特殊个体装备的掉落不会自动出现。

## 整合包配置

数据包可覆盖 simulation_profile、crystal_binding、mob_crystal_binding、crystal_cultivation 和 overload_simulation 配方。支持优先级、标签、概率、能耗、耗时和命名 Java 扩展提供器。修改后执行 /reload。相同最高优先级的匹配配方存在冲突时会停止处理。
