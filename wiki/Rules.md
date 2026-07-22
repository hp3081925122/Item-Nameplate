# 规则参考

[English](Rules-EN)

本页说明 `config/item_nameplate_rules/` 中 JSON 规则文件当前实现支持的全部字段、写法和执行顺序。每个规则文件根对象必须包含 `entries` 数组；数组中的每个对象是一条规则。

## 一条完整规则

下面是一条可直接使用的规则。它匹配药水箭，读取物品名称，删除“之箭”和英文 `Arrow of `，最后替换常见效果名。

```json
{
  "desc": "药水箭名称牌",
  "target": {
    "type": "item",
    "value": "minecraft:tipped_arrow"
  },
  "text_source": {
    "type": "item_name"
  },
  "remove_text": ["之箭", "Arrow of "],
  "replace": {
    "瞬间治疗": "治疗",
    "Instant Health": "Healing"
  },
  "priority": 10
}
```

## 规则选择

同一个物品可能命中多条规则，但只使用一条。

1. 先比较 `priority`，数字大的优先。
2. `priority` 相同时，`entries` 中更靠前的规则优先。
3. 未写 `priority` 时默认为 `0`。

例如，同一个下界合金升级模板可以同时命中 tag 和类规则；将更具体的 tag 规则设为 `10`、通用类规则设为 `9`，就会优先使用 tag 规则。

## target：选择目标物品

`target` 是必填对象，必须包含 `type` 和 `value`。一条规则只能选择一种匹配方式。

| `type` | `value` 写法 | 匹配范围 |
| --- | --- | --- |
| `item` | `命名空间:物品路径` | 仅匹配该物品。 |
| `tag` | `命名空间:标签路径` | 匹配该物品标签内的所有物品。 |
| `class` | 物品 Java 类全名 | 匹配该类及其所有子类。 |

### 精确匹配物品：item

适合只处理一个物品。物品 ID 必须在当前游戏中真实存在。

```json
{
  "target": {
    "type": "item",
    "value": "minecraft:enchanted_book"
  }
}
```

### 匹配物品标签：tag

适合同类物品或由数据包维护的一组物品。标签 ID 不需要在加载规则时预先存在；游戏数据包加载后，标签中的物品会被匹配。

```json
{
  "target": {
    "type": "tag",
    "value": "minecraft:trim_templates"
  }
}
```

模组自带的下界合金升级模板标签写法如下：

```json
{
  "target": {
    "type": "tag",
    "value": "item_nameplate:smithing_templates"
  }
}
```

### 匹配物品类：class

适合原版或其他模组拥有稳定 Java 类层级的一类物品。类名必须能在当前客户端加载，并且该类必须继承 `net.minecraft.world.item.Item`。

```json
{
  "target": {
    "type": "class",
    "value": "net.minecraft.world.item.SpawnEggItem"
  }
}
```

上例会匹配所有刷怪蛋。所有原版锻造模板可写为：

```json
{
  "target": {
    "type": "class",
    "value": "net.minecraft.world.item.SmithingTemplateItem"
  }
}
```

## text_source：读取名称文字

`text_source` 是必填对象。支持 `item_name`、`nbt`、`tooltip` 三种来源。

| `type` | 必填字段 | 读取内容 |
| --- | --- | --- |
| `item_name` | 无 | 物品当前名称。 |
| `nbt` | `path` | 指定 NBT 路径中的字符串标签。 |
| `tooltip` | `index` | 原版普通提示框的指定行，第一行索引为 `0`。 |

### 从物品名称读取：item_name

这是最简单的来源。它读取当前物品的显示名称，能反映原版本地化和物品的自定义名称。

```json
{
  "text_source": {
    "type": "item_name"
  }
}
```

`item_name` 读取后不执行 `text_source` 内的 `prepend`、`replace`、`split`、`join`、`i18n`。要清理或缩写名称，请使用规则根级的 `remove_text` 和 `replace`。

```json
{
  "text_source": {
    "type": "item_name"
  },
  "remove_text": [" Spawn Egg", "刷怪蛋"],
  "replace": {
    "Instant Health": "Healing",
    "瞬间治疗": "治疗"
  }
}
```

### 从 NBT 读取：nbt

`nbt` 只能读取最终为字符串标签的路径。路径使用 `.` 进入复合标签，使用 `[非负索引]` 访问列表元素；不支持通配符、引号键名、负索引或直接读取数字、布尔值、复合标签和列表本身。

```json
{
  "text_source": {
    "type": "nbt",
    "path": "display.Name"
  }
}
```

下面路径读取附魔书 `StoredEnchantments` 列表第一项的 `id`：

```json
{
  "text_source": {
    "type": "nbt",
    "path": "StoredEnchantments[0].id"
  }
}
```

路径不存在、列表索引越界、最终值不是字符串时，本次读取失败，名称牌会回退显示物品原名称。

### 从提示框读取：tooltip

`tooltip.index` 是从 `0` 开始的行号。读取的是普通提示框，不包含 F3+H 高级提示信息。

```json
{
  "text_source": {
    "type": "tooltip",
    "index": 1
  }
}
```

锻造模板的第 `0` 行始终是“锻造模板 / Smithing Template”，第 `1` 行才是“下界合金升级”“哨兵盔甲纹饰”等具体名称，因此默认锻造模板规则使用 `index: 1`。

`tooltip` 与 `item_name` 一样，读取后不执行 `text_source` 内的 NBT 专用处理字段；要处理结果请使用根级 `remove_text` 和 `replace`。

## NBT 专用文字处理

以下字段只在 `text_source.type` 为 `nbt` 时生效。它们都写在 `text_source` 内。

### prepend：添加前缀

`prepend` 会在原始 NBT 字符串前添加文本。常用于把 NBT 中的注册表 ID 转成翻译键。

```json
{
  "text_source": {
    "type": "nbt",
    "path": "StoredEnchantments[0].id",
    "prepend": "enchantment."
  }
}
```

例如 NBT 值 `minecraft:mending` 会先变为 `enchantment.minecraft:mending`。

### text_source.replace：处理原始 NBT 文字

`text_source.replace` 是“原始文字来源”级别的替换表。每个键和值必须是字符串，替换按 JSON 中书写顺序执行。

```json
{
  "text_source": {
    "type": "nbt",
    "path": "StoredEnchantments[0].id",
    "prepend": "enchantment.",
    "replace": {
      ":": "."
    }
  }
}
```

该例将 `enchantment.minecraft:mending` 改成 `enchantment.minecraft.mending`。

### i18n：把 NBT 结果作为翻译键

`i18n: true` 会把处理后的 NBT 文本传给 Minecraft 翻译系统，显示当前游戏语言的翻译结果。通常与 `prepend`、`text_source.replace` 配合使用。

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

若翻译键不存在，Minecraft 会显示该键本身；`i18n` 不会自动翻译普通英文名称。

### split：按分隔符拆分

`split.separator` 必填且不能为空。它按普通文本分隔符拆分，不是正则表达式。可选的 `split.index` 从 `0` 开始，只保留指定一段；省略 `index` 时保留全部拆分结果，供 `join` 使用。

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

当 NBT 值为 `minecraft:mending` 时，上例结果为 `minecraft`。索引越界会使读取失败并回退到物品原名称。

### join：重新拼接拆分结果

`join.separator` 必填。它将 `split` 得到的全部结果拼接；`join.prepend` 与 `join.append` 可选。根级 `text_source.prepend` 会先加入，随后再加入 `join.prepend`。

```json
{
  "text_source": {
    "type": "nbt",
    "path": "example",
    "split": {
      "separator": ":"
    },
    "join": {
      "separator": "/",
      "prepend": "[",
      "append": "]"
    }
  }
}
```

当 NBT 值为 `minecraft:mending` 时，上例结果为 `[minecraft/mending]`。

## 根级文字处理

下面字段写在规则根对象，与 `text_source` 同级，对三种文字来源都生效。

### remove_text：删除固定文字

`remove_text` 是字符串数组，按数组顺序逐项执行全量删除。适合删除中英文后缀、剂型词或无关前缀。

```json
{
  "remove_text": ["Splash Potion of ", "喷溅型", "药水"]
}
```

名称为 `Splash Potion of Healing` 时，结果为 `Healing`。删除区分大小写。

### replace：替换最终文字

根级 `replace` 是最终结果的替换表，按 JSON 中书写顺序执行。它适用于物品名称、提示框和 NBT 三种来源。

```json
{
  "replace": {
    "Instant Health": "Healing",
    "瞬间治疗": "治疗"
  }
}
```

若同时存在 `text_source.replace` 与根级 `replace`，前者先在 NBT 读取阶段执行，后者在 `remove_text` 之后执行。

## 完整处理顺序

### item_name 与 tooltip

1. 读取物品名称，或读取指定提示框行。
2. 对结果依次执行根级 `remove_text`。
3. 对结果依次执行根级 `replace`。
4. 去除首尾空白；结果为空时回退到物品原名称。
5. 按一个物品槽位的实际像素宽度裁剪后渲染。

### nbt

1. 读取 `path` 指向的字符串标签。
2. 按 `split` 拆分；若有 `split.index`，只保留该段。
3. 按 `join` 拼接，或直接取第一段；同时添加 `prepend`、`join.prepend`、`join.append`。
4. 执行 `text_source.replace`。
5. 若 `i18n` 为 `true`，按翻译键本地化。
6. 执行根级 `remove_text`。
7. 执行根级 `replace`。
8. 去除首尾空白；结果为空或读取失败时回退到物品原名称。
9. 按一个物品槽位的实际像素宽度裁剪后渲染。

## 完整示例：附魔书

```json
{
  "desc": "显示第一条附魔名称",
  "target": {
    "type": "item",
    "value": "minecraft:enchanted_book"
  },
  "text_source": {
    "type": "nbt",
    "path": "StoredEnchantments[0].id",
    "prepend": "enchantment.",
    "replace": {
      ":": "."
    },
    "i18n": true
  },
  "priority": 20
}
```

## 完整示例：所有刷怪蛋

```json
{
  "desc": "所有刷怪蛋",
  "target": {
    "type": "class",
    "value": "net.minecraft.world.item.SpawnEggItem"
  },
  "text_source": {
    "type": "item_name"
  },
  "remove_text": [" Spawn Egg", "刷怪蛋"],
  "priority": 10
}
```

## 完整示例：盔甲纹饰模板

```json
{
  "desc": "所有盔甲纹饰模板",
  "target": {
    "type": "tag",
    "value": "minecraft:trim_templates"
  },
  "text_source": {
    "type": "tooltip",
    "index": 1
  },
  "remove_text": [" Armor Trim", "盔甲纹饰"],
  "priority": 10
}
```
