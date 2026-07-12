# 规则参考

[English](Rules-EN)

## target

`target.type` 只能选择一种匹配方式。

| type | value 内容 | 匹配方式 |
| --- | --- | --- |
| `item` | 物品 ID | 精确匹配一个物品。 |
| `tag` | 物品标签 ID | 匹配标签中的所有物品。 |
| `class` | 物品 Java 类全名 | 匹配该类及其子类。 |

下面示例匹配原版的全部盔甲纹饰模板标签。

```json
{
  "target": {
    "type": "tag",
    "value": "minecraft:trim_templates"
  }
}
```

## text_source

| type | 必填字段 | 读取内容 |
| --- | --- | --- |
| `item_name` | 无 | 物品当前名称。 |
| `nbt` | `path` | 指定 NBT 路径中的字符串。 |
| `tooltip` | `index` | 原版物品提示框的指定行，第一行索引为 `0`。 |

### item_name

下面示例直接使用物品名称。

```json
{
  "text_source": {
    "type": "item_name"
  }
}
```

### nbt

NBT 路径支持 `.` 分隔的复合标签和 `[索引]` 访问列表。例如附魔书第一条附魔 ID：

```json
{
  "text_source": {
    "type": "nbt",
    "path": "StoredEnchantments[0].id",
    "prepend": "enchantment.",
    "replace": {
      ":": "."
    },
    "i18n": true
  }
}
```

### tooltip

锻造模板的物品名称统一为“锻造模板 / Smithing Template”，具体升级或纹饰名称位于提示框第二行，因此使用 `index: 1`。

```json
{
  "text_source": {
    "type": "tooltip",
    "index": 1
  }
}
```

## text_source 的可选处理

| 字段 | 说明 |
| --- | --- |
| `prepend` | 在读取内容前添加字符串。 |
| `replace` | 在读取后、规则处理前执行的替换表。 |
| `i18n` | 为 `true` 时，将读取结果作为翻译键本地化。 |
| `split` | 按分隔符拆分；可用 `index` 取其中一段。 |
| `join` | 将拆分结果以指定分隔符重新拼接，可添加前后缀。 |

下面示例读取冒号前的内容。

```json
{
  "text_source": {
    "type": "nbt",
    "path": "example",
    "split": {
      "separator": ":",
      "index": 0
    }
  }
}
```
