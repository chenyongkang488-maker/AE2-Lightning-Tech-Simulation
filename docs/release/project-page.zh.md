# 闪电科技：模拟

作者：qiqi

**简介：** 用闪电培养模拟水晶，通过 AE2 网络生产矿物、作物、木材与生物战利品。

这是 AE2 Lightning Tech / Reborn 的独立附属模组，面向 Minecraft 1.21.1 / NeoForge。
从空白水晶开始记录模拟对象，再培养为可重复使用的完美水晶。

## 内容

- 单方块过载模拟室，最多 128 并行，支持加速卡与自动弹出。
- 3×3×3～7×7×7 粉白多方块模拟室，最多同时容纳 49 个水晶。
- 效率、时运、过载、熔炼升级，1024 件单格缓冲与 AE 产物输出。
- 可改装的过载雷鸣线圈，支持引雷、拟态挖掘及 AE/Mekanism 扳手功能。
- 矿物公共标签、耕地作物和刷怪蛋生物的通用适配。
- 数据包配方、Java 扩展接口以及 G 键游戏内指南。

## 前置与安装

Java 21、Minecraft 1.21.1、NeoForge 21.1.252～21.1.x。
必装 AE2 19.2.17、GuideME 21.1.19，以及
[AE2 Lightning Tech 2.1.1](https://www.curseforge.com/minecraft/mc-mods/ae2-lightning-tech/files/9024605) 和
[Thunderbolt Core 2.0.2](https://www.curseforge.com/minecraft/mc-mods/thunderbolt-core/files/9055479)。
也支持原 Reborn 2.1.0 / 2.0.0 组合。Reborn 已迁回主项目，内部 ID 相同，每种前置只保留一份。
早期闪电科技 2.0.x 不受支持。只安装 AE2、GuideME 与本附属模组仍会缺少前置。

Mekanism 可选，已验证 10.7.19.85；神秘农业/Cucumber 可选兼容验证版本为
8.0.28 / 8.0.16。把普通 JAR 放入客户端及服务器的 mods 文件夹，
升级前备份存档并移走旧版；sources JAR 和源码 ZIP 用于开发。

## 测试与许可

0.1.1-beta.2 为测试版，兼容当前 CurseForge 前置并修复线圈界面，玩法沿用已发布 beta.1。
已通过 24 项单元测试，三套前置/加载器环境各 153 项功能测试，并实测新旧前置的线圈客户端界面。
长期存档、多人和更多第三方模组仍需联调。遇到问题请提供版本、复现步骤和日志。

代码、文档及原创模型/贴图采用 MIT；改编自闪电科技的水晶、标志与七个模块图标
采用 CC BY-NC-SA 3.0，保留原作者署名、非商业和相同方式共享条件。
出处及修改说明见源码与 JAR 中的 LICENSES.md、THIRD_PARTY_NOTICES.md。
上游：https://github.com/AE2-Lightning-Tech-Reborn/AE2-Lightning-Tech-Reborn

源码与问题反馈：https://github.com/chenyongkang488-maker/AE2-Lightning-Tech-Simulation
