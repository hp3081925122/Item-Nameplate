# Default Rules

[中文](Default-Rules)

The default `item_nameplate_rules/minecraft.json` contains these rules when it is generated for the first time. Existing user files are never overwritten.

| Target | Match method | Text source | Result |
| --- | --- | --- | --- |
| Enchanted books | `minecraft:enchanted_book` | First enchantment NBT entry | Localized enchantment name. |
| Spawn eggs | `SpawnEggItem` class | Item name | Removes “Spawn Egg”. |
| Regular, splash, and lingering potions | Three vanilla item IDs | Item name | Removes potion-form wording and handles common Chinese and English effects. |
| Smithing upgrade templates | `item_nameplate:smithing_templates` tag | Tooltip line 2 | Displays the specific upgrade name. |
| All smithing templates | `SmithingTemplateItem` class | Tooltip line 2 | Displays the specific upgrade or trim name. |
| Armor trim templates | `minecraft:trim_templates` tag | Tooltip line 2 | Displays the specific trim name. |
| Tipped arrows | `minecraft:tipped_arrow` | Item name | Removes “Arrow of” and matching Chinese wording. |

The smithing upgrade tag rule has priority `10`; the all-template class rule has priority `9`. Therefore, an item matching both uses the tag rule first.

Default text removals and replacements include both Simplified Chinese and English. Add your own `remove_text` or `replace` values for other languages.
