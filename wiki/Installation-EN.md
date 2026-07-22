# Installation

[中文](Installation)

## Install

1. Install Minecraft 1.20.1 Forge.
2. Place the Item Nameplate jar in the client `mods` directory.
3. Launch the game once to generate the default configuration.

This is a client-side display feature. The server does not need the mod.

## Configuration paths

After the first launch, configuration files are located at:

`config/item_nameplate-common.toml`

`config/item_nameplate_rules/`

Every `.json` file in this directory is loaded. The first launch creates `minecraft.json`; create files such as `create.json` or `mekanism.json` to group rules by item namespace.

Restart the game after editing the JSON rules.

## General settings

```toml
enabled = true
labelScale = 0.7
```

`labelScale` controls label size. Its allowed range is `0.3` to `1.0`.
