# 闪电科技：模拟发布操作单

当前版本：**0.1.1-beta.2**；作者：**qiqi**；Minecraft 1.21.1 / NeoForge / Java 21。

## 发布目标与文件

- GitHub：<https://github.com/chenyongkang488-maker/AE2-Lightning-Tech-Simulation>，标签 `v0.1.1-beta.2`，标为 Pre-release。
- CurseForge：现有项目 `1727853`，上传为 Minecraft 1.21.1 / NeoForge / Beta 文件。
- 游戏文件：`lightning-tech-simulation-0.1.1-beta.2.jar`。
- 开发附件：`lightning-tech-simulation-0.1.1-beta.2-sources.jar`、对应源码 ZIP 和 SHA256SUMS.txt。
- 更新说明：[0.1.1-beta.2.md](release/0.1.1-beta.2.md)，中英文介绍：[中文](release/project-page.zh.md)、[English](release/project-page.en.md)。

发布基于 beta.1 的修复分支；不要误用包含 0.2.0 开发功能的源码或构件。
版本、源码提交和 JAR 必须对应。普通 JAR 上传 CurseForge；源码附件不作为游戏文件上传。

## 验证与上传

1. Java 21 下执行 `gradlew.bat verifyReleaseVersion build`，以及 `python scripts/verify-release.py --tag v0.1.1-beta.2`。
2. 核对许可、源码和文件 SHA-256，保留运行测试记录；详细结果见 [兼容验证](compatibility-verification.md)。
3. 将已验证修复提交推送至 GitHub，创建匹配源码的版本标签，不覆盖历史版本或标签。
4. 等待既有 Windows/Linux CI 检查通过，附上普通 JAR、源码 JAR、源码 ZIP 和校验表，再公开 GitHub Pre-release。
5. 在 CurseForge 现有项目上传普通 JAR，填写版本、加载器、游戏版本、更新说明和依赖关系。
6. 分别确认 GitHub Release 已公开、CurseForge 文件已提交；CurseForge 审核通过前不能称为已审核发布。

## 前置关系与许可

必需：AE2 19.2.17、GuideME 21.1.19、AE2 Lightning Tech 2.1.1、Thunderbolt Core 2.0.2。
另支持原 Reborn AE2LT 2.1.0 / Thunderbolt 2.0.0；同 ID 的新旧前置不要同时安装。
Mekanism 为 Optional Dependency。神秘农业/Cucumber 是可选兼容内容，不是强制前置。
平台依赖关系不能代替描述中的精确版本说明。

许可证字段保留 Custom：代码和原创美术 MIT，改编水晶/模块美术 CC BY-NC-SA 3.0。
保留 LICENSES.md、THIRD_PARTY_NOTICES.md 和上游资产许可。前置模组不捆绑上传。

对外名称使用“闪电科技：模拟” / “Lightning Tech: Simulation”。现有源码仓库地址保持有效，内部注册 ID 保留用于旧存档兼容。
