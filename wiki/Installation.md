# 安装与使用

[English](Installation-EN)

## 安装

1. 安装 Minecraft 1.20.1 Forge。
2. 将 Item Nameplate 的 jar 放入客户端 `mods` 文件夹。
3. 启动游戏一次，模组会生成默认配置。

这是纯客户端显示功能；服务器无需安装。

## 配置位置

首次启动后，配置文件位于：

`config/item_nameplate-common.toml`

`config/item_nameplate_rules/`

该目录下的每个 `.json` 文件都会被读取。首次启动会生成 `minecraft.json`；可按物品命名空间创建文件，例如 `create.json`、`mekanism.json`。

修改 JSON 后需要重启游戏，使规则重新加载。

## 通用配置

下面配置控制总开关和文字缩放。`labelScale` 越小，文字越小；有效范围为 `0.3` 到 `1.0`。

```toml
enabled = true
labelScale = 0.7
```
