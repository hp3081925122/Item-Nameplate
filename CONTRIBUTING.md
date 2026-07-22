# 默认兼容贡献

默认兼容规则位于 `src/main/resources/item_nameplate/default_rules/`。每个 JSON 文件会随模组 jar 发布，并在游戏启动时复制到玩家的 `config/item_nameplate_rules/` 目录；已有同名文件不会被覆盖。

## 添加规则

1. 在 `src/main/resources/item_nameplate/default_rules/` 新建以目标物品命名空间命名的 JSON 文件，例如 `create.json`。
2. 使用现有规则格式编写 `entries`，并为每条规则填写清楚的 `desc`。
3. 不要修改与本次兼容无关的默认规则文件。

下面是 `create.json` 的最小示例，实际提交时请替换为真实物品 ID 和规则：

```json
{
  "entries": [
    {
      "desc": "Create 示例规则",
      "target": {
        "type": "item",
        "value": "create:example_item"
      },
      "text_source": {
        "type": "item_name"
      },
      "priority": 10
    }
  ]
}
```

旧版的 `config/item_nameplate_rules.json` 在升级时会迁移为 `config/item_nameplate_rules/minecraft.json`。内置规则新增或更新时，只会补充玩家缺失的文件，不会覆盖玩家已有内容。
