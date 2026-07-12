# Custom Tags

[中文](Tags)

The mod provides the `item_nameplate:smithing_templates` item tag. By default, it contains only the Netherite Upgrade Smithing Template.

```json
{
  "replace": false,
  "values": [
    "minecraft:netherite_upgrade_smithing_template"
  ]
}
```

The built-in tag is located at `data/item_nameplate/tags/items/smithing_templates.json` inside the mod resources.

Modpacks can extend the same tag with a data pack. Keep `replace` set to `false`, otherwise the built-in entry is replaced.

```json
{
  "replace": false,
  "values": [
    "examplemod:custom_smithing_template"
  ]
}
```

If a custom item does not use the vanilla tooltip layout, add a dedicated rule with an appropriate `text_source`.
