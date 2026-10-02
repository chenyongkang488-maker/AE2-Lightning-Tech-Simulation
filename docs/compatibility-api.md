# 模拟兼容规则与诊断（alpha.11）

Minecraft 1.21.1 / NeoForge。现有档案 ID、CrystalData format=1 和 Java 扩展签名保留。普通与多方块模拟室共用产物解析器；普通模拟室仍在启动时收费，多方块在完成时收费。新批次使用重载后的规则，已启动批次保留抽取快照。

alpha.13 耕地作物自动发现、神秘农业采收和成熟状态接口见 [作物兼容 API](crop-compatibility-api.md)。

## 矿物自动发现

块标签 `c:storage_blocks/raw_<material>` 对应物品标签 `c:raw_materials/<material>`。恰好一个原矿物品时生成稳定档案 `overload_sim:auto/mineral/c/<material>`，每次基础产出一个。不同模组的同材质存储块可混用；不同材料、多个候选产物、带方块实体的材料均拒绝消耗。

同一材料存在粗矿存储块时，自动发现只启用粗矿分支，避免通用机械这类同时具有粉末、矿石标签的金属产生假冲突。精炼金属块需显式绑定规则；粗矿产物本身有歧义时仍拒绝，不回退到粉末。

无粗矿的矿物使用 `c:storage_blocks/<material>`、`c:ores/<material>` 和 `c:gems/<material>` 或 `c:dusts/<material>`。以无附魔下界合金镐读取矿石掉落表，保留基础数量变化。多个矿石来源只有完整掉落表等价时才自动选代表矿石；异质表需要显式规则。红石为 4～5、青金石为 4～9；钻石、煤、绿宝石、石英为一个。下界合金块绑定旧 `overload_sim:netherite` 档案，产远古残骸一个，熔炼产两个下界合金碎片。

解析优先级：显式结构绑定配方 > `simulation_mineral` JSON > Java mineral resolver > 自动标签。显式绑定先按物理材料匹配选最高优先级，再检查档案、自然雷限制和条件；拒绝时不会绕到自动标签。培养同样先选最高优先级，拒绝时不绕到 `any`。生产配方优先于矿物产出映射，最高优先级相同的多个匹配会报告冲突。

同一个矿物 profile 必须有唯一、明确的产物定义；不同绑定块但共享 profile 的同优先级 JSON 仍会冲突，在消耗24块之前拒绝。多个合法绑定材料可使用一个绑定标签统一声明。Java 定义只与其它 Java 定义比较冲突，一个唯一 Java 定义可以覆盖同 profile 的自动标签映射；JSON 仍优先。绑定与生产必须使用一致的 canonical 定义，不一致会返回 inconsistent_mineral_profile。

`data/<namespace>/simulation_mineral/<id>.json`：

```json
{
  "material": "c:osmium",
  "binding": {"id": "#c:storage_blocks/raw_osmium"},
  "priority": 100,
  "item": "mekanism:raw_osmium", "min": 1, "max": 1,
  "same_block": false,
  "smelting": {"input": "mekanism:raw_osmium", "result": "mekanism:ingot_osmium", "count": 2}
}
```

`profile` 可省略（自动材料 ID）；`disabled` 默认 false。输出选 `item` 或 `block_loot`（矿石块 ID）；同时提供时实际读取 `block_loot`，item 用于显示。数量 min/max 为 1..4096，熔炼 count 为 1..64。`binding.properties` 可限定块状态；`same_block=true` 要求 24 格完全同种块。禁用规则仍需 material/binding，但可以省略产物。

外部存档使用自定义 profile ID 时，应提供 `simulation_profile` 的名称/图标及客户端翻译；自动材料 ID 可通过已同步的公共物品标签显示。缺少模组或映射时保留水晶，停止新生产。

## 生物规则

记录资格为存活的 LivingEntity，并且属于 Mob、有注册刷怪蛋、或获 Java eligibility 允许；玩家排除。只保存类型，不复制实际生物装备/NBT。通用实体掉落使用未加入世界的临时实体；默认中性玩家击杀上下文、无抢夺、Luck=0，调用正常 NeoForge 掉落入口，GLM 一次。岩浆怪使用 size=2，史莱姆 size=1。

内置额外规则：凋零星一个；坚守者的幽匿催发体、回响碎片、幽匿块各一个；末影龙龙蛋和龙首分别独立 25%，龙息必出一个。幻翼、烈焰人、旋风人和另外 15 种受玩家击杀条件影响的原版表有回归覆盖。

`data/<namespace>/simulation_mob/<id>.json`：

```json
{
  "entity": "minecraft:ender_dragon", "priority": 200,
  "outputs": [
    {"item": "minecraft:dragon_egg", "chance": 0.25},
    {"item": "minecraft:dragon_head", "chance": 0.25},
    {"item": "minecraft:dragon_breath", "chance": 1}
  ]
}
```

entity 为 ID、`#实体类型标签` 或 `*`。outputs、loot_table、provider 三种输出方式互斥。每条 outputs 独立抽取，min/max 默认 1（1..4096），chance 默认 1（0..1）；最多 64 条，仍受单次总量 4096 的输出保护。`context` 为 player（默认）或 environment；`template` 为已注册的初始化器 ID，仅对掉落表模式使用。`disabled=true` 拒绝记录/新生产。同优先级匹配冲突时拒绝。

`overload_sim:simulation_mob_blacklist` 为实体类型标签；`simulation_mineral_blacklist` 为块标签。标签文件路径为 `tags/entity_type`、`tags/block`，增补使用 replace=false。标签、配方和规则重载统一使缓存失效。

## Java 接口

在 common setup 的 enqueueWork 注册；重复 ID 抛错。旧 registerOutput、registerBindingCondition、registerMobPredicate 保留。

```java
SimulationExtensions.registerOutputV2(ResourceLocation.parse("example:bonus"), c -> {
    // c.level / position / crystal / random / machine / rule
    return c.random().nextDouble() < 0.25
        ? List.of(new ItemStack(Items.DIAMOND)) : List.of();
});
SimulationExtensions.registerEntityEligibility(ResourceLocation.parse("example:eligibility"),
    entity -> entity.getType() == YOUR_LIVING_ENTITY_TYPE);
SimulationExtensions.registerEntityTemplate(ResourceLocation.parse("example:preset"),
    entity -> { /* 初始化临时实体；禁止 addFreshEntity、真实击杀、复制个体背包 */ });
SimulationExtensions.registerMineralResolver(ResourceLocation.parse("example:mineral"),
    (level, state) -> Optional.empty()); // 返回 Optional<ResolvedMineral>
```

OutputContextV2 的 machine 为 single 或 multiblock，rule 标明调用规则；由提供器自行使用明确 RNG。矿物 resolver 应是快速、确定、无副作用的映射，缓存重建会扫描块状态以恢复存档中的 profile ID。两个 resolver 返回同一状态、或同一 profile 返回不同定义会拒绝，不按注册顺序决定。

服务器查档案可用 `SimulationData.profile(ServerLevel, ResourceLocation)`，它会先初始化当前规则缓存。机器在冷启动和恢复任务时也使用此路径，不要求先进行一次水晶绑定；旧 `profile(ResourceLocation)` 查询保留，适用于已初始化缓存及客户端显示。

ResolvedMineral 保存 profile/material/binding、item/min/max、ore（block_loot）、smelting 和 sameBlock；不扣费、不破坏块。模板只在掉落表模式运行，输出/provider 模式不要求模板存在。GLM 只参与表模式；直接 outputs/provider 如需其它行为，由作者明确实现。

需要真实装备掉落、唱片、蛙明灯、变种或复杂代码死亡逻辑的生物，应写 outputs/provider/template，不通过模拟击杀真实实体补偿。固定抽样为空可能是正常随机结果，并不表示绑定失败。

## 诊断

- `/overload_sim explain`：检查主手水晶，报告档案、规则、材料/代表矿石、数量、上下文、输出模式与缺失/冲突原因。
- `/overload_sim explain_block x y z`：只检查已加载块的矿物映射；不测试整个 24 格阵列或调用结构条件。
- `/overload_sim audit`：需要权限等级 2；将当前材料档案、绑定块和实体表信息写到世界目录 `overload_sim-audit.json`。

诊断不抽随机数、不扣费、不改物品、不调用输出提供器或实体初始化器。spawn_egg 只报告刷怪蛋索引，完整记录资格还包括 Mob/Java adapter。loot_status 区分空基础表、已配置基础表、动态/缺失表、不可读表；GLM 可能给空表添加产物。无法通过静态查询预测本轮概率结果；random=not_evaluated 会明确指出。

完整可复制的数据包例子在 [examples/compatibility](examples/compatibility/README.md)。原版条件与公共标签参考 [NeoForge 1.21.1 标签](https://docs.neoforged.net/docs/1.21.1/resources/server/tags/) 和 [Global Loot Modifiers](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/glm/)。
