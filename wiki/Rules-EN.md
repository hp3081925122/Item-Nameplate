# Rule Reference

[中文](Rules)

This page documents every field currently implemented in JSON rule files under `config/item_nameplate_rules/`, including syntax and processing order. Each file root must contain an `entries` array, with one label rule per object.

## Complete rule

This ready-to-use rule matches tipped arrows, reads their item name, removes Chinese and English arrow wording, then replaces common effect names.

```json
{
  "desc": "Tipped arrow label",
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

## Rule selection

Only one matching rule is used.

1. Higher `priority` wins.
2. For equal priority, the earlier entry in `entries` wins.
3. The default priority is `0`.

For example, a specific smithing-template tag rule can use priority `10`, while a general class rule uses `9`.

## target: choose an item

`target` is required and must contain `type` and `value`. One rule chooses exactly one matching strategy.

| `type` | `value` syntax | Match behavior |
| --- | --- | --- |
| `item` | `namespace:item_path` | One exact item. |
| `tag` | `namespace:tag_path` | Every item in the item tag. |
| `class` | Fully qualified item Java class | The class and all subclasses. |

### Exact item: item

```json
{
  "target": {
    "type": "item",
    "value": "minecraft:enchanted_book"
  }
}
```

### Item tag: tag

```json
{
  "target": {
    "type": "tag",
    "value": "minecraft:trim_templates"
  }
}
```

The built-in upgrade-template tag is:

```json
{
  "target": {
    "type": "tag",
    "value": "item_nameplate:smithing_templates"
  }
}
```

### Item class: class

The class must load on the current client and inherit `net.minecraft.world.item.Item`.

```json
{
  "target": {
    "type": "class",
    "value": "net.minecraft.world.item.SpawnEggItem"
  }
}
```

All vanilla smithing templates use:

```json
{
  "target": {
    "type": "class",
    "value": "net.minecraft.world.item.SmithingTemplateItem"
  }
}
```

## text_source: read label text

| `type` | Required field | Source |
| --- | --- | --- |
| `item_name` | None | Current item display name. |
| `nbt` | `path` | A string at an NBT path. |
| `tooltip` | `index` | A normal vanilla tooltip line; the first line is `0`. |

### item_name

```json
{
  "text_source": {
    "type": "item_name"
  }
}
```

For `item_name`, `prepend`, source-level `replace`, `split`, `join`, and `i18n` inside `text_source` are not processed. Use root-level `remove_text` and `replace` instead.

```json
{
  "text_source": { "type": "item_name" },
  "remove_text": [" Spawn Egg", "刷怪蛋"],
  "replace": {
    "Instant Health": "Healing",
    "瞬间治疗": "治疗"
  }
}
```

### nbt

`nbt` reads only a path whose final value is a string tag. Use `.` for compound tags and `[non-negative index]` for list elements. Wildcards, quoted keys, negative indexes, and non-string final values are not supported.

```json
{
  "text_source": {
    "type": "nbt",
    "path": "display.Name"
  }
}
```

The first enchantment ID in an enchanted book is:

```json
{
  "text_source": {
    "type": "nbt",
    "path": "StoredEnchantments[0].id"
  }
}
```

Missing paths, out-of-range list indexes, and non-string final values fall back to the original item name.

### tooltip

`tooltip.index` is zero-based and reads normal, non-advanced tooltip lines.

```json
{
  "text_source": {
    "type": "tooltip",
    "index": 1
  }
}
```

Smithing templates use “Smithing Template” as line `0`; their specific upgrade or trim name is line `1`. Like `item_name`, tooltip sources use root-level processing rather than NBT-only fields.

## NBT-only text operations

The fields below work only when `text_source.type` is `nbt`.

### prepend

Adds a prefix before the raw NBT value.

```json
{
  "text_source": {
    "type": "nbt",
    "path": "StoredEnchantments[0].id",
    "prepend": "enchantment."
  }
}
```

`minecraft:mending` becomes `enchantment.minecraft:mending`.

### text_source.replace

This source-level replacement map runs after NBT splitting and joining. Replacements execute in JSON order.

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

### i18n

With `i18n: true`, the processed NBT text is treated as a Minecraft translation key.

```json
{
  "text_source": {
    "type": "nbt",
    "path": "StoredEnchantments[0].id",
    "prepend": "enchantment.",
    "replace": { ":": "." },
    "i18n": true
  }
}
```

It does not translate ordinary English text automatically. A missing key displays as the key itself.

### split

`split.separator` is required and cannot be empty. It is a literal separator, not a regular expression. Optional `split.index` is zero-based; omit it to keep all segments for `join`.

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

For `minecraft:mending`, this yields `minecraft`. An invalid index falls back to the original item name.

### join

`join.separator` is required. It joins all `split` results; `join.prepend` and `join.append` are optional. Root `prepend` is applied before `join.prepend`.

```json
{
  "text_source": {
    "type": "nbt",
    "path": "example",
    "split": { "separator": ":" },
    "join": {
      "separator": "/",
      "prepend": "[",
      "append": "]"
    }
  }
}
```

For `minecraft:mending`, this yields `[minecraft/mending]`.

## Root-level text operations

These fields are peers of `text_source` and work with all three source types.

### remove_text

An ordered array of literal strings to delete everywhere in the result. Removal is case-sensitive.

```json
{
  "remove_text": ["Splash Potion of ", "喷溅型", "药水"]
}
```

`Splash Potion of Healing` becomes `Healing`.

### replace

The root-level replacement map runs after `remove_text` and works with item names, tooltips, and NBT.

```json
{
  "replace": {
    "Instant Health": "Healing",
    "瞬间治疗": "治疗"
  }
}
```

For NBT sources, `text_source.replace` runs first, then root `remove_text`, then root `replace`.

## Processing order

### item_name and tooltip

1. Read the item name or selected tooltip line.
2. Run root `remove_text` in array order.
3. Run root `replace` in JSON order.
4. Trim outer whitespace; an empty result falls back to the original item name.
5. Clip the label to the actual pixel width of one item slot and render it.

### nbt

1. Read the string at `path`.
2. Split with `split`; keep only `split.index` when provided.
3. Join with `join`, or use the first segment; add `prepend`, `join.prepend`, and `join.append`.
4. Run `text_source.replace`.
5. Localize with `i18n` when enabled.
6. Run root `remove_text`.
7. Run root `replace`.
8. Trim whitespace; on an empty result or failed read, use the original item name.
9. Clip and render to one item slot.

## Full example: enchanted book

```json
{
  "desc": "Display the first enchantment",
  "target": {
    "type": "item",
    "value": "minecraft:enchanted_book"
  },
  "text_source": {
    "type": "nbt",
    "path": "StoredEnchantments[0].id",
    "prepend": "enchantment.",
    "replace": { ":": "." },
    "i18n": true
  },
  "priority": 20
}
```

## Full example: all spawn eggs

```json
{
  "desc": "All spawn eggs",
  "target": {
    "type": "class",
    "value": "net.minecraft.world.item.SpawnEggItem"
  },
  "text_source": { "type": "item_name" },
  "remove_text": [" Spawn Egg", "刷怪蛋"],
  "priority": 10
}
```

## Full example: armor trim templates

```json
{
  "desc": "All armor trim templates",
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
