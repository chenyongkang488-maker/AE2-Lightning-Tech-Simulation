# 兼容数据包示例

Minecraft 1.21.1。将本目录作为数据包复制到世界的 datapacks，再 `/reload`。

矿物示例以 Mekanism 的锇为目标；使用前安装对应模组，或把示例中的物品/材料 ID 改成目标模组。标签文件使用 required=false，缺少目标块时不添加无效标签。显式矿物规则在没有目标块时不触发。自动材料 profile ID 不变，方便既有水晶沿用。

生物例子覆盖末影龙的三个独立池，以及幻翼的 player 上下文。修改 outputs 的 chance/min/max 可直接魔改。若目标模组掉落来自代码死亡逻辑，则使用命名 provider，而不是照抄默认 loot_table。

Java 的 registerOutputV2 等接口与禁止副作用的要求见 [兼容 API](../../compatibility-api.md)。诊断命令可确认匹配规则和冲突。将 priority 改成明确高于已有规则的数值；同最高优先级冲突会停止匹配。
