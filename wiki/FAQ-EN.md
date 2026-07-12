# FAQ

[中文](FAQ)

## Labels do not appear

Check that `enabled = true` in `config/item_nameplate-common.toml`, then ensure the item matches a rule in `item_nameplate_rules.json`.

## I changed the JSON but nothing changed

Rules load only during game startup. Restart the game after saving `item_nameplate_rules.json`.

## Why do other labels remain visible while a tooltip is open?

Labels render before the tooltip. The tooltip covers only the area it actually overlaps, while labels outside that area remain visible.

## English names are truncated

Labels are constrained to the visible width of one item slot and clipped using actual font width. Narrow English characters usually fit more text, while wider characters and longer words are clipped sooner.

## How do I diagnose a JSON configuration error?

Check `logs/latest.log`. The mod logs warnings or errors for invalid roots, unknown item IDs, unloadable classes, invalid NBT paths, and unsupported text sources.
