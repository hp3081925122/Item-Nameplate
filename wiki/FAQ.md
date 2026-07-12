# 常见问题

[English](FAQ-EN)

## 名称牌没有显示

检查 `config/item_nameplate-common.toml` 中 `enabled = true`，并确认对应物品能命中 `item_nameplate_rules.json` 的规则。

## 修改 JSON 后没有变化

规则只在游戏启动时加载。保存 `item_nameplate_rules.json` 后重启游戏。

## 提示框出现时，其他名称牌为什么仍显示

名称牌在提示框之前绘制，提示框会覆盖其实际挡住的区域；未被提示框覆盖的名称牌会继续显示。

## 英文名称显示不完整

名称牌固定在一个物品槽位的可见宽度内，并按字体实际像素宽度截取。英文窄字符通常能显示更多，宽字符和较长单词会更早截断。

## JSON 配置错误后怎么办

查看 `logs/latest.log`。模组会对无效根对象、未知物品 ID、无法加载的类、无效 NBT 路径及不支持的文本源输出警告或错误。
