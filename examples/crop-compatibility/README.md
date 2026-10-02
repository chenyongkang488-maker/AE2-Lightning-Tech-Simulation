# 作物产物覆盖示例

这份配方将神秘农业钻石作物的通用模拟产物替换为2个钻石精华，不产种子。神秘农业未安装时请勿启用。

把 `data/` 合入现有数据包根目录，再执行 `/reload`。若单独创建数据包，在根目录添加 `pack.mcmeta`，Minecraft 1.21.1 的数据包格式为48：

```json
{"pack":{"pack_format":48,"description":"Overload Simulation crop override example"}}
```

此目录不会自动安装到测试存档。其它作物的稳定档案 ID、费用和 Java 成熟状态接口见 [作物兼容说明](../../docs/crop-compatibility-api.md)。
