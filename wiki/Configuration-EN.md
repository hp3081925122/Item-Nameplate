# Configuration

[中文](Configuration)

Rules are stored in `config/item_nameplate_rules/`. Every `.json` file in this directory and its subdirectories is loaded; name files by item namespace, such as `minecraft.json` or `create.json`. Each file's root object contains an `entries` array; every entry is one label rule.

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
2. For equal priority, files are read by relative-path alphabetical order, then the earlier entry in that file wins.

The first launch creates `minecraft.json` from bundled rules. When upgrading from an older version, `config/item_nameplate_rules.json` is moved to `config/item_nameplate_rules/minecraft.json` so existing rules remain available. Newly added bundled rule files are also copied when missing, without overwriting existing files.

See the repository-root `CONTRIBUTING.md` for the bundled compatibility-rule contribution process.

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
