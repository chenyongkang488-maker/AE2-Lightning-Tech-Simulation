# CurseForge 前置兼容修复

0.1.1-beta.2 基于公开发布的 0.1.1-beta.1（`582ee5a`）制作，不包含 0.2.0 开发内容。

## 报告与根因

2026-10-05 23:38 的报告使用 Minecraft 1.21.1、NeoForge 21.1.255、Java 21。
模组发现列表只有 AE2 19.2.17、GuideME 21.1.19 和 Simulation 0.1.1-beta.1；
`ae2lt` 和 `thunderbolt` 均为 `MISSING`，应先安装这两个前置。

另外，beta.1 将前置精确锁在 `[2.1.0]` 和 `[2.0.0]`，会拒绝迁回主项目后的
AE2LT 2.1.1 / Thunderbolt 2.0.2。当前与原 Reborn 的内部 ID 相同，无需增加别名。

实测只放宽版本范围仍有两处错误：

- 服务端线圈设备配置状态构造器报 `NoSuchMethodError`；2.1.1 新增 EHV beam 布尔字段。
- 客户端 `CoilHubScreenMixin.settingCount` 找不到 `mouseScrolled` 内的常量 6，导致启动失败。

修复通过缓存的新旧状态构造器适配器处理新增字段，并仅在模拟线圈设置区域接管七行设置的滚动。
原生电磁轨道炮的设置与滚动继续使用上游实现。资源 ID、配方及存档数据保持原样。

## 安装

推荐组合：Minecraft 1.21.1、Java 21、NeoForge 21.1.x（不低于 21.1.252）、
AE2 19.2.17、GuideME 21.1.19、AE2LT 2.1.1、Thunderbolt 2.0.2、Simulation 0.1.1-beta.2。

- [AE2LT 2.1.1（主项目）](https://www.curseforge.com/minecraft/mc-mods/ae2-lightning-tech/files/9024605)
- [Thunderbolt 2.0.2](https://www.curseforge.com/minecraft/mc-mods/thunderbolt-core/files/9055479)
- [Reborn 迁移说明](https://www.curseforge.com/minecraft/mc-mods/ae2-lightning-tech-reborn)

也支持原 Reborn AE2LT 2.1.0 / Thunderbolt 2.0.0。不要同时放两份相同 ID 的前置。
客户端与服务器都需要安装；移走旧 Simulation JAR 再放入 beta.2。前置不捆绑进本模组。
旧闪电科技 2.0.x 不具备所需接口，不能仅改名后使用。

## 重现验证

先用 Java 21 执行 `scripts/bootstrap.ps1`，下载并校验原版与当前前置。
编译固定使用最早支持的 Reborn API；运行版本可独立选择，避免通过重新编译掩盖二进制兼容问题。

```powershell
# 原 Reborn 组合
.\gradlew.bat verifyReleaseVersion build runGameTestServer --console=plain --no-daemon

# 当前 CurseForge 组合，以及报告中的加载器
.\gradlew.bat build runGameTestServer '-Pae2ltRuntimeVersion=2.1.1' '-PthunderboltRuntimeVersion=2.0.2' '-PneoForgeVersion=21.1.255' --console=plain --no-daemon

# 客户端 Mixin 和线圈配置界面
.\gradlew.bat runClient '-Pae2ltRuntimeVersion=2.1.1' '-PthunderboltRuntimeVersion=2.0.2' '-PneoForgeVersion=21.1.255' --console=plain --no-daemon

python scripts/verify-release.py --tag v0.1.1-beta.2
```

验证结果见 [验证记录](compatibility-verification.md)，发布包中也附有 `verification.md`；不将开发环境成功等同于所有整合包都兼容。
