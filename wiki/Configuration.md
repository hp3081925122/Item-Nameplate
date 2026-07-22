# 配置文件

[English](Configuration-EN)

规则目录是 `config/item_nameplate_rules/`。目录及其子目录中的每个 `.json` 文件都会被读取；建议按物品命名空间命名，例如 `minecraft.json`、`create.json`。每个文件的根对象都包含 `entries` 数组，每个元素是一条名称牌规则。

下面示例按单个物品匹配，并显示该物品名称。

```json
{
  "entries": [
    {
      "desc": "示例规则",
      "target": {
        "type": "item",
        "value": "minecraft:diamond"
      },
      "text_source": {
        "type": "item_name"
      },
      "priority": 10
    }
  ]
}
```

## 规则选择顺序

同一物品可以同时命中多条规则，但只使用一条：

1. `priority` 数值更大的规则优先。
2. 优先级相同时，按文件相对路径的字母顺序读取，随后使用该文件中更靠前的规则。

首次启动会从模组内置规则生成 `minecraft.json`。从旧版升级时，原 `config/item_nameplate_rules.json` 会自动移动为 `config/item_nameplate_rules/minecraft.json`，不会丢失已有规则。后续版本新增的内置规则文件也会自动补齐，但不会覆盖已有同名文件。

默认兼容规则的贡献方式见仓库根目录的 `CONTRIBUTING.md`。

## 常用字段

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `desc` | 否 | 规则说明，不影响游戏行为。 |
| `target` | 是 | 匹配目标，见[规则参考](Rules)。 |
| `text_source` | 是 | 名称文字来源，见[规则参考](Rules)。 |
| `remove_text` | 否 | 依次从结果中删除的字符串数组。 |
| `replace` | 否 | 结果文字替换表，键为原文字，值为新文字。 |
| `priority` | 否 | 优先级，默认 `0`。 |

`remove_text` 会先于规则级 `replace` 执行。处理结果为空时，会回退到原始物品名称。
