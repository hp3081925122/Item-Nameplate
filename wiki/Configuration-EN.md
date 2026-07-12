# Configuration

[中文](Configuration)

Rules are stored in `config/item_nameplate_rules.json`. The root object contains an `entries` array; every entry is one label rule.

```json
{
  "entries": [
    {
      "desc": "Example rule",
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

## Rule selection

Only one matching rule is used for an item.

1. Higher `priority` wins.
2. For equal priority, the earlier JSON entry wins.

## Common fields

| Field | Required | Meaning |
| --- | --- | --- |
| `desc` | No | Documentation only. |
| `target` | Yes | Match target. See [Rule Reference](Rules-EN). |
| `text_source` | Yes | Text source. See [Rule Reference](Rules-EN). |
| `remove_text` | No | Strings removed from the result in order. |
| `replace` | No | Map of source text to replacement text. |
| `priority` | No | Rule priority; defaults to `0`. |

`remove_text` runs before rule-level `replace`. If processing yields an empty label, the original item name is used.
