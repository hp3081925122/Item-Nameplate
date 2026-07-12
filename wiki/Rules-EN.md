# Rule Reference

[中文](Rules)

## target

`target.type` selects one matching strategy.

| type | `value` | Match behavior |
| --- | --- | --- |
| `item` | Item ID | Matches one exact item. |
| `tag` | Item tag ID | Matches every item in the tag. |
| `class` | Fully qualified item Java class | Matches that item class and its subclasses. |

```json
{
  "target": {
    "type": "tag",
    "value": "minecraft:trim_templates"
  }
}
```

## text_source

| type | Required field | Source |
| --- | --- | --- |
| `item_name` | None | The current item name. |
| `nbt` | `path` | A string at the specified NBT path. |
| `tooltip` | `index` | A vanilla item tooltip line; the first line is `0`. |

### item_name

```json
{
  "text_source": {
    "type": "item_name"
  }
}
```

### nbt

NBT paths use `.` for compound tags and `[index]` for lists.

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

All smithing templates have the item name “Smithing Template”. Their specific upgrade or trim name is on tooltip line 2, so the default rules use `index: 1`.

```json
{
  "text_source": {
    "type": "tooltip",
    "index": 1
  }
}
```

## Optional text processing

| Field | Meaning |
| --- | --- |
| `prepend` | Adds text before the source value. |
| `replace` | Replacement map applied at source level. |
| `i18n` | Treats the value as a translation key when `true`. |
| `split` | Splits text by a separator and can select one segment with `index`. |
| `join` | Joins split segments with a separator and optional prefix or suffix. |
