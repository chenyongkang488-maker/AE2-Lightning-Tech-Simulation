# 0.1.1-beta.2 验证记录

验证日期：2026-10-06。基线为已发布的 `v0.1.1-beta.1` / `582ee5a2f428bd1091b12e2cf7ad991a05784992`。
Java 21，Minecraft 1.21.1，AE2 19.2.17，GuideME 21.1.19。

| 前置组合 | NeoForge | 功能测试 | 客户端 |
| --- | --- | --- | --- |
| Reborn AE2LT 2.1.0 / Thunderbolt 2.0.0 | 21.1.252 | 153/153 通过 | 进入测试世界、按 G 打开线圈界面、滚动显示全部七项设置、正常退出 |
| AE2LT 2.1.1 / Thunderbolt 2.0.2 | 21.1.252 | 153/153 通过 | 客户端单独在下一行加载器版本实测 |
| AE2LT 2.1.1 / Thunderbolt 2.0.2 | 21.1.255（错误报告版本） | 153/153 通过 | 进入测试世界、按 G 打开线圈界面、滚动显示全部七项设置、正常退出 |

- 24 项 JUnit 测试通过，0 失败、0 错误、0 跳过；含四项实际处理后 TOML 的依赖范围回归测试。
- 修复前已重现：新前置被精确版本限制拒绝；放宽限制后，服务端状态构造器 `NoSuchMethodError` 与客户端滚轮 Mixin `InjectionError`。
- 修复后的功能测试覆盖线圈共享设备界面状态、手持槽选择及轨道炮行为保留，并验证新 EHV 标志为关闭。
- 对照原发布 JAR，215 个运行资源逐字节一致（版本/依赖元数据除外），未修改注册 ID、配方或贴图。
- 最终以最低支持的 NeoForge 21.1.252 和 Reborn API 编译；同一 Java 实现通过上述运行环境测试。
- `verifyReleaseVersion`、`build`、`scripts/verify-release.py --tag v0.1.1-beta.2` 及 `git diff --check` 通过。
- 发布 JAR 与源码 JAR 包含四份原有许可/署名文件；没有打包 Minecraft、前置 JAR 或开发测试数据包。

运行命令见 [兼容说明](curseforge-compatibility.md)。界面截图随桌面发布包附带。
以上是这些明确组合的验证结果，不代表测试过任意整合包或未来前置版本；Thunderbolt 2.0.1 位于声明范围内，但未单独运行验证。

## 安装要点

用 `overload_sim-0.1.1-beta.2.jar` 替换旧 Simulation JAR。推荐另外安装 AE2LT 2.1.1 与 Thunderbolt 2.0.2。
错误报告中的实例实际缺少这两个前置，替换本附属模组后仍需补装。Reborn 与主项目 ID 相同，每种前置只保留一份。
源码 JAR 和源码 ZIP 用于开发，不能放进游戏 mods 文件夹。本次制作了本地修复包，未自动上传 CurseForge 或 GitHub。
