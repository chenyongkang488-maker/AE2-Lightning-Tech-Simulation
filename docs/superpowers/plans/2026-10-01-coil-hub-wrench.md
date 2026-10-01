# 线圈设备界面与扳手 Implementation Plan

> 执行方式：superpowers:executing-plans，当前会话逐项完成。

Goal: 原生共享 G 页面、汇聚电弧、原生模块风格及 AE/Mek 扳手。
Architecture: 条件 Mixin 为原生 hub 的武器页识别本物品；服务器同步独立配置，Mek 类型仅在可选桥接内加载。
Tech Stack: Java 21 / NeoForge 21.1.252 / AE2 19.2.17 / AE2LT 2.1.0 / optional Mekanism 10.7.19.85 / Blockbench。
Spec: ../specs/2026-10-01-coil-hub-wrench-design.md

1. 先增加真实 GameTests：原生 hub 识别线圈/枪主手优先、设置兼容、模块安装/开关/核心失效、释放禁雷、AE 工具行为；观察 RED。
2. CoilSettings/CoilConfiguration/CoilWrench 及可选 MekanismCoilCompat；模块和配方注册；固定编译依赖且常规 runtime 不包含 Mek。用 AE InteractionUtil 条件桥接避免永久 tag。
3. CoilHubAccess 与 Hub host/menu/status/client Mixins、配置校验、原生状态同步；沿用上游原生背景、列表、复选框与滚动条。旧独立菜单保留兼容，G 路由改为共享 hub。
4. 有 Mek 的 GameTests 验证真实机器侧面设置、旋转/清空、管道及安全；无 Mek 的完整回归。非法配置与切换手持工具拒绝。
5. Blockbench 本地工坊绘制七个模块；两层八帧中心汇聚电弧，补素材署名；游戏内视觉检查共享页面、模型、电弧与模块。
6. 整体只读复核；构建 alpha.7、校验 jar 和 PCL 前置 hash，备份安装；Git 提交与标签。

Review focus: 可选 Mek 缺失不能触发 classloading；关扳手不能有常驻 wrench tag；迟到配置包不能改换手工具；原生枪/装备动作不能落在线圈；保护事件、容器内容与权限沿用原生工具。
