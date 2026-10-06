# 0.2.0-alpha.5 开发版前置兼容

本轮将稳定分支 `0cafe7e`（beta.2）和 `f32f146`（beta.3）的兼容修复
选择性同步到 `938cdbe` 之后的开发代码。保留激光、粒子约束器、自动搭建、
模型与美术，不用稳定版目录覆盖开发版。对外名称为“闪电科技：模拟” / 
“Lightning Tech: Simulation”，内部 `overload_sim` 和 `dev.overloadsim` 保持兼容。

## 修复内容

- AE2LT 2.1.1 为 `DeviceStatusModel` 增加了 `ehvBeamEnabled` 参数。
  `CoilStatusFactory` 在加载时选择 12 或 13 参数构造器；线圈始终禁用电磁炮的
  地形破坏、PVP、音效、连锁伤害、蓄力溅射和新 EHV 光束开关。
- 新版设备界面动态计算设置数量，不再具有旧版 `mouseScrolled` 的常量 6。
  线圈只接管自己的设置区域滚动，支持全部七行，原生电磁炮仍走上游代码。
- AE2LT / Thunderbolt 的模组 ID 和本次使用的 API 包保持一致；新版所需改动
  位于兼容桥接和客户端 Mixin。开发编译继续使用旧 API，运行时前置可以切换。

## 版本声明与实测范围

| 前置 | 允许安装的范围 | 本次使用的版本 |
| --- | --- | --- |
| Minecraft | 仅 1.21.1 | 1.21.1 |
| NeoForge | `[21.1.252,21.2)` | 21.1.252、21.1.255 |
| AE2 | `[19.2.17,20)` | 19.2.17、19.2.18 |
| AE2 Lightning Tech | `[2.1.0,3)` | Reborn 2.1.0、主项目 2.1.1 |
| Thunderbolt Core | `[2.0.0,3)` | 2.0.0、2.0.2 |
| GuideME | `[21.1.19,21.2)` | 21.1.19 |

范围允许后续兼容更新，不代表未来版本已经实测。NeoForge 加载器同时检查其它
前置的要求，最终可安装范围是所有依赖范围的交集。不可同时装同 ID 的新旧前置。
不同 Minecraft 版本、加载器和前置大版本变化仍需单独移植。

官方来源：[AE2 19.2.18](https://github.com/AppliedEnergistics/Applied-Energistics-2/releases/tag/neoforge/v19.2.18)、
[非 Reborn AE2LT 2.1.1](https://www.curseforge.com/minecraft/mc-mods/ae2-lightning-tech/files/9024605)、
[Thunderbolt Core](https://www.curseforge.com/minecraft/mc-mods/thunderbolt-core)。
实际二进制下载地址和 SHA-256 在 `scripts/dependencies.json`，不捆绑进模组 JAR。

## 重复验证

Java 21；先运行 `scripts/bootstrap.ps1` 下载旧版编译依赖和新版运行依赖。
默认开发运行环境为 AE2 19.2.18 / AE2LT 2.1.1 / Thunderbolt 2.0.2。

```powershell
.\gradlew.bat verifyReleaseVersion build runGameTestServer --console=plain
.\gradlew.bat runGameTestServer '-Pae2RuntimeVersion=19.2.17' '-Pae2ltRuntimeVersion=2.1.0' '-PthunderboltRuntimeVersion=2.0.0' --console=plain
python scripts/verify-release.py --tag v0.2.0-alpha.5
```

`-PneoForgeVersion=21.1.255` 切换同一游戏系列的测试加载器。可选环境使用
`-PmekTests -PcropTests -PfluxTests`，需另备 CONTRIBUTING 列出的对应 JAR。
更换 `ae2RuntimeVersion` / `ae2ltRuntimeVersion` / `thunderboltRuntimeVersion`
不改变编译依赖；同一组 `.class` 应显示 `compileJava UP-TO-DATE`。

八项元数据测试使用与加载器一致的版本区间解析器验证真实打包 TOML；其中的未来
版本号仅测试准入政策。游戏内集成测试检查真实前置行为，不用元数据测试替代。

完整执行结果、原始失败复现、客户端验收和构件校验见
[alpha.5 验证记录](releases/0.2.0-alpha.5-verification.md)。
