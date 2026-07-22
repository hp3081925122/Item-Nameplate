# 默认规则

[English](Default-Rules-EN)

首次启动时，`item_nameplate_rules/minecraft.json` 会生成以下默认规则。已有配置文件不会自动覆盖，以避免覆盖用户自定义内容。

| 目标 | 匹配方式 | 文字来源 | 效果 |
| --- | --- | --- | --- |
| 附魔书 | `minecraft:enchanted_book` | 第一条附魔 NBT | 显示本地化的附魔名称。 |
| 刷怪蛋 | `SpawnEggItem` 类 | 物品名称 | 删除“刷怪蛋 / Spawn Egg”。 |
| 普通、喷溅、滞留药水 | 三个原版物品 ID | 物品名称 | 删除剂型词，并处理常见中英文效果名。 |
| 锻造升级模板 | `item_nameplate:smithing_templates` tag | 提示框第 2 行 | 显示下界合金升级等具体名称。 |
| 所有锻造模板 | `SmithingTemplateItem` 类 | 提示框第 2 行 | 显示具体升级或纹饰名称。 |
| 盔甲纹饰模板 | `minecraft:trim_templates` tag | 提示框第 2 行 | 显示具体纹饰名称。 |
| 药水箭 | `minecraft:tipped_arrow` | 物品名称 | 删除“之箭 / Arrow of”等词。 |

锻造升级模板的 tag 规则优先级为 `10`，所有锻造模板的类规则优先级为 `9`。因此同一物品同时命中时，优先使用 tag 规则。

默认规则同时包含简体中文和英文的删除、替换文本。其他语言可以在规则 JSON 中追加对应的 `remove_text` 或 `replace` 项。
