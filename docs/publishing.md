# 发布操作单

当前候选版本：**0.1.1-beta.1**；作者昵称：**qiqi**。
本地准备阶段没有创建线上仓库或提交平台审核。GitHub 账号和仓库名仍需由作者确定；
昵称 qiqi 不被自动当成 GitHub 用户名。

## GitHub 首次开源

1. 登录自己的 GitHub，创建一个空的 Public 仓库。建议仓库名
   `AE2-Lightning-Tech-Simulation`；不要自动初始化 README、许可证或 .gitignore，
   因为本地项目已经包含这些文件。
2. 为本地仓库添加自己刚创建的地址，并推送当前发布分支。若使用 main 作为默认分支，
   可将当前分支推送到 main，不需要改写已有提交或标签。
3. 推送本次版本标签 `v0.1.1-beta.1`。不要批量推送或覆盖历史标签。
4. 等待 GitHub Actions 的 Windows/Linux 构建通过。该工作流只构建与上传 CI 产物，
   不会自动公开 Release。
5. 创建 Release，选择该标签，标题为
   `AE2 Lightning Tech: Simulation 0.1.1-beta.1`，勾选 Pre-release，
   粘贴 `docs/release/0.1.1-beta.1.md`，附件使用发布包中的普通 JAR、
   sources JAR 和 SHA256SUMS.txt。源码 ZIP 也可以附上。

下列命令中的地址必须换成自己的真实仓库地址：

```powershell
git remote add origin https://github.com/YOUR_ACCOUNT/AE2-Lightning-Tech-Simulation.git
git push -u origin HEAD:main
git push origin v0.1.1-beta.1
```

线上仓库建立后，将真实源码页和 Issues 地址填入平台项目页；如需写入
`neoforge.mods.toml`，应另建下一版本，保留本次已打包文件和标签。

操作依据：[创建仓库](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-new-repository)、
[管理 Release](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)。

## CurseForge

在作者后台建立 Minecraft **Mods** 项目。英文标题、简介及正文可直接使用
`docs/release/project-page.en.md`；中文正文作为补充。选择 Minecraft 1.21.1、
NeoForge、Beta，并上传**普通模组 JAR**。sources JAR 和源码 ZIP 不作为游戏文件。

将 AE2 19.2.17、AE2 Lightning Tech Reborn 2.1.0、Thunderbolt Core Reborn 2.0.0、
GuideME 21.1.19 标为 Required Dependency；Mekanism 标为 Optional Dependency。
项目关系通常不能替代精确版本说明，正文应保留兼容表。神秘农业/Cucumber 只是可选
兼容内容，不是本模组强制前置。

许可证字段使用 Custom，填写：
`MIT for code and original artwork; CC BY-NC-SA 3.0 for adapted crystal/module artwork. See LICENSES.md and THIRD_PARTY_NOTICES.md.`
不要将整个含美术资源的 JAR 简写为“全部 MIT”。不要将改编素材重新声明为原创。

图库中已备有真实游戏截图。仓库内原始水晶 logo 是 128×128；
如上传 CurseForge，应在 **Blockbench** 中将同一标志按整数倍导出为 512×512 PNG，
保持像素风及许可署名。不能直接上传 128×128 图标。
平台要求及审核说明见
[官方提交指南](https://support.curseforge.com/support/solutions/articles/9000199552-project-submission-guide-and-tips)。

## Modrinth

使用相同标题、描述、支持版本和拆分许可说明。先确认四个固定版本前置是否在
Modrinth 可用，再设置平台依赖；若只有其它平台提供，应在描述中明确手动安装地址，
不要填错为旧版闪电科技。无需同时发布全部平台，先完成 GitHub 和 CurseForge 即可。

## 文件与后续版本

桌面发布包含普通 JAR、sources JAR、Git 源码 ZIP、SHA-256 校验表、中英介绍、
发布说明、许可与图库；不包含游戏本体、存档、启动器或前置二进制。
本次流程不会替换用户游戏实例的已安装模组。

未来改动：更新 `mod_version` 与 CHANGELOG，运行 README 中的检查，再创建新标签。
完整检验范围与仍需人工测试的部分见验证记录和本次发布说明。
