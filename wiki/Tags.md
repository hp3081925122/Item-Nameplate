# 自定义标签

[English](Tags-EN)

模组提供 `item_nameplate:smithing_templates` 物品标签。默认只包含下界合金升级锻造模板：

```json
{
  "replace": false,
  "values": [
    "minecraft:netherite_upgrade_smithing_template"
  ]
}
```

该默认标签位于模组资源的 `data/item_nameplate/tags/items/smithing_templates.json`。

整合包可以通过数据包扩展同名标签。下面示例将自定义模板加入现有标签；`replace` 必须保持为 `false`，否则会替换模组默认条目。

```json
{
  "replace": false,
  "values": [
    "examplemod:custom_smithing_template"
  ]
}
```

如果自定义物品不是 `SmithingTemplateItem`，它仍可由 tag 规则命中；若其提示框结构与原版模板不同，请为该物品单独添加规则并选择正确的 `text_source`。
