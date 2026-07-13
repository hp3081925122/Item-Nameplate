package com.hp.item_nameplate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.client.IItemDecorator;

import java.util.regex.Pattern;

public class ItemNameplateRenderer implements IItemDecorator {
    public static final ItemNameplateRenderer INSTANCE = new ItemNameplateRenderer();
    private static final int NAME_COLOR = 0xFFFFFF55;
    private static final int OUTLINE_COLOR = 0xFF000000;
    private static final int LABEL_WIDTH = 16;

    private ItemNameplateRenderer() {
    }

    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        if (!Config.enabled) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || Config.getNameplateRule(stack.getItem()).isEmpty()) {
            return false;
        }

        int labelTop = yOffset + 16 - Mth.ceil((font.lineHeight + 2) * (float) Config.labelScale);
        renderScaledLabel(guiGraphics, font, buildSlotLabel(stack), xOffset + 8, labelTop, NAME_COLOR, OUTLINE_COLOR, (float) Config.labelScale);
        return false;
    }

    private static void renderScaledLabel(GuiGraphics guiGraphics, Font font, Component line, int centerX, int topY, int color, int outlineColor, float scale) {
        if (line == null) {
            return;
        }

        // 按槽位的固定像素宽度截取文字，并为左右描边各预留一个原始字体像素。
        int maxTextWidth = Math.max(1, Mth.floor(LABEL_WIDTH / scale) - 2);
        String text = font.plainSubstrByWidth(line.getString(), maxTextWidth);
        line = Component.literal(text);
        int rawWidth = font.width(line);
        float scaledWidth = rawWidth * scale;
        float left = centerX - scaledWidth / 2.0F;

        // 使用无深度遮挡的文字渲染类型，让名称牌始终覆盖物品材质。
        guiGraphics.flush();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left, topY + 1, 300.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        drawOutlinedText(guiGraphics, font, line, 0, 0, color, outlineColor);
        guiGraphics.pose().popPose();
        guiGraphics.flush();
    }

    private static void drawOutlinedText(GuiGraphics guiGraphics, Font font, Component line, int x, int y, int color, int outlineColor) {
        font.drawInBatch(line, x, y - 1, outlineColor, false, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        font.drawInBatch(line, x, y + 1, outlineColor, false, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        font.drawInBatch(line, x - 1, y, outlineColor, false, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        font.drawInBatch(line, x + 1, y, outlineColor, false, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        font.drawInBatch(line, x - 1, y - 1, outlineColor, false, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        font.drawInBatch(line, x + 1, y - 1, outlineColor, false, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        font.drawInBatch(line, x - 1, y + 1, outlineColor, false, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        font.drawInBatch(line, x + 1, y + 1, outlineColor, false, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        font.drawInBatch(line, x, y, color, false, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 15728880);
    }

    private static Component buildSlotLabel(ItemStack stack) {
        String originalName = stack.getHoverName().getString();
        Config.NameplateRule rule = Config.getNameplateRule(stack.getItem()).orElse(null);
        String displayName = readTextSource(stack, rule.textSource());
        if (displayName == null || displayName.isEmpty()) {
            displayName = originalName;
        }
        for (String removeText : rule.removeText()) {
            displayName = displayName.replace(removeText, "");
        }
        for (var replacement : rule.replacements().entrySet()) {
            displayName = displayName.replace(replacement.getKey(), replacement.getValue());
        }
        displayName = displayName.strip();
        if (displayName.isEmpty()) {
            displayName = originalName;
        }
        return Component.literal(displayName);
    }

    private static String readTextSource(ItemStack stack, Config.TextSource source) {
        if (source.type().equals("item_name")) {
            return stack.getHoverName().getString();
        }
        if (source.type().equals("tooltip")) {
            var tooltipLines = stack.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.NORMAL);
            return source.tooltipIndex() < tooltipLines.size() ? tooltipLines.get(source.tooltipIndex()).getString() : null;
        }
        Tag current = stack.getTag();
        if (current == null) {
            return null;
        }

        for (Config.NbtPathPart pathPart : source.path()) {
            if (!(current instanceof CompoundTag compound) || !compound.contains(pathPart.key())) {
                return null;
            }
            current = compound.get(pathPart.key());
            if (pathPart.index() != null) {
                if (!(current instanceof ListTag list) || pathPart.index() >= list.size()) {
                    return null;
                }
                current = list.get(pathPart.index());
            }
        }

        if (!(current instanceof net.minecraft.nbt.StringTag)) {
            return null;
        }

        String text = current.getAsString();
        String[] values = new String[]{text};
        if (source.splitSeparator() != null) {
            values = text.split(Pattern.quote(source.splitSeparator()), -1);
            if (source.splitIndex() != null) {
                if (source.splitIndex() >= values.length) {
                    return null;
                }
                values = new String[]{values[source.splitIndex()]};
            }
        }
        if (source.joinSeparator() != null) {
            text = source.joinPrepend() + String.join(source.joinSeparator(), values) + source.joinAppend();
        } else {
            text = source.joinPrepend() + values[0] + source.joinAppend();
        }
        for (var replacement : source.replacements().entrySet()) {
            text = text.replace(replacement.getKey(), replacement.getValue());
        }
        return source.i18n() ? Component.translatable(text).getString() : text;
    }
}
