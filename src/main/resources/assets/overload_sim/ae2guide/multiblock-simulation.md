---
navigation:
  title: 多方块模拟室
  icon: overload_sim:simulation_controller
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:simulation_controller
  - overload_sim:simulation_frame
  - overload_sim:simulation_efficiency_t1
  - overload_sim:simulation_efficiency_t2
  - overload_sim:simulation_efficiency_t3
  - overload_sim:simulation_fortune_module
  - overload_sim:simulation_overload_module
  - overload_sim:simulation_smelting_module
---

# 多方块模拟室

外部尺寸为 3×3×3 至 7×7×7。水晶容量依次为 9、16、25、36、49，存放在控制器中；内部空间必须为空气。

12 条边框使用模拟室框架。底部非角边框处放置**一个控制器**，使正面朝外。可以在另一处底部非角边框替换为**一个原版过载 ME 接口**。顶面及四侧面中央使用 AE 聚能石英玻璃；底面中央用框架或升级模块填满。

控制器会自动检测并成型。右键打开界面；G 键查看本页。放入完美模拟电鸣水晶，接入有电且有高压闪电库存的 AE 网络即可生产。

## 生产与升级

每个参加生产的水晶每 180 tick 执行一次现有模拟配方，每次扣 1000 FE + 1 高压。费用按水晶计算。

|底部模块|效果|
|---|---|
|效率 T1 / T2 / T3|每个减少 2 / 4 / 8 tick，总共最多减 104 tick|
|时运|每个使一次实际产物 ×2；最多 ×1024（10 个）|
|过载（最多一个）|最终时间减半；每个水晶额外消耗 1 极高压，显示环绕电弧|
|熔炼（最多一个）|每个水晶额外消耗 2 高压；原铁/铜/金变成双倍锭，显示火焰；其他产物类型保持|

7 阶底面有 25 个升级位：13 个 T3 + 10 个时运 + 1 个过载 + 1 个熔炼，可达 38 tick 周期。49 个铁水晶可产 100352 个铁锭，费用 49000 FE、147 高压、49 极高压。

## 输出和拆解

输出缓冲有 128 格、四页，每格 1024 个同类同组件物品。左键取正常一组、右键取一个、Shift 将物品按正常堆叠填入背包。管道也只提取正常堆叠。

装有过载接口时，接口绑定的无线过载频道把整机接入网络，产物自动导入网络；不能装入的部分留在缓冲。没有接口时，可通过控制器连接 AE 线缆供电/取闪电，并用管道取出产物。

断电、区块卸载或拆掉结构会暂停已付款任务；恢复结构后继续，不重复扣费或抽取战利品。生存模式拆掉控制器会把库存、缓冲、能量和未完成任务保存在控制器物品里，重新放置后恢复。缩小结构后超出容量的水晶仍可取回。

## 分层搭建

F=框架；C=控制器（正面向北）；G=聚能石英玻璃；U=框架或底部模块；.=空气。I 过载接口可替换任意底部非角 F，不能替换 C。

### 3×3×3（9 水晶，1 升级位）

底层 y=0

```
FCF
FUF
FFF
```

中间层 y=1…1

```
FGF
G.G
FGF
```

顶层 y=2

```
FFF
FGF
FFF
```

### 4×4×4（16 水晶，4 升级位）

底层 y=0

```
FCFF
FUUF
FUUF
FFFF
```

中间层 y=1…2

```
FGGF
G..G
G..G
FGGF
```

顶层 y=3

```
FFFF
FGGF
FGGF
FFFF
```

### 5×5×5（25 水晶，9 升级位）

底层 y=0

```
FCFFF
FUUUF
FUUUF
FUUUF
FFFFF
```

中间层 y=1…3

```
FGGGF
G...G
G...G
G...G
FGGGF
```

顶层 y=4

```
FFFFF
FGGGF
FGGGF
FGGGF
FFFFF
```

### 6×6×6（36 水晶，16 升级位）

底层 y=0

```
FCFFFF
FUUUUF
FUUUUF
FUUUUF
FUUUUF
FFFFFF
```

中间层 y=1…4

```
FGGGGF
G....G
G....G
G....G
G....G
FGGGGF
```

顶层 y=5

```
FFFFFF
FGGGGF
FGGGGF
FGGGGF
FGGGGF
FFFFFF
```

### 7×7×7（49 水晶，25 升级位）

底层 y=0

```
FCFFFFF
FUUUUUF
FUUUUUF
FUUUUUF
FUUUUUF
FUUUUUF
FFFFFFF
```

中间层 y=1…5

```
FGGGGGF
G.....G
G.....G
G.....G
G.....G
G.....G
FGGGGGF
```

顶层 y=6

```
FFFFFFF
FGGGGGF
FGGGGGF
FGGGGGF
FGGGGGF
FGGGGGF
FFFFFFF
```
