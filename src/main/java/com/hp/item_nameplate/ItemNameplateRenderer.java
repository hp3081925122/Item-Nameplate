package com.hp.item_nameplate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
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

        boolean profiling = ItemNameplateDebugBenchmark.isProfilingNameplate();
        long phaseStart = profiling ? System.nanoTime() : 0L;
        Minecraft minecraft = Minecraft.getInstance();
        Config.NameplateRule rule = Config.getNameplateRule(stack.getItem()).orElse(null);
        if (profiling) {
            ItemNameplateDebugBenchmark.recordRuleNanos(System.nanoTime() - phaseStart);
        }
        if (minecraft.player == null || minecraft.options.hideGui || rule == null) {
            return false;
        }

        phaseStart = profiling ? System.nanoTime() : 0L;
        Component label = buildSlotLabel(stack, rule);
        if (profiling) {
            ItemNameplateDebugBenchmark.recordTextNanos(System.nanoTime() - phaseStart);
        }
        int labelTop = yOffset + 16 - Mth.ceil((font.lineHeight + 2) * (float) Config.labelScale);
        renderScaledLabel(guiGraphics, font, label, xOffset + 8, labelTop, NAME_COLOR, OUTLINE_COLOR, (float) Config.labelScale, profiling);
        return false;
    }

    private static void renderScaledLabel(GuiGraphics guiGraphics, Font font, Component line, int centerX, int topY, int color, int outlineColor, float scale, boolean profiling) {
        if (line == null) {
            return;
        }

        long phaseStart = profiling ? System.nanoTime() : 0L;
        // 按槽位的固定像素宽度截取文字，并为左右描边各预留一个原始字体像素。
        int maxTextWidth = Math.max(1, Mth.floor(LABEL_WIDTH / scale) - 2);
        String text = font.plainSubstrByWidth(line.getString(), maxTextWidth);
        line = Component.literal(text);
        boolean legacyRenderer = ItemNameplateDebugBenchmark.useLegacyRenderer();
        boolean shaderRenderer = ItemNameplateDebugBenchmark.useShaderRenderer();
        FormattedCharSequence formattedLine = legacyRenderer ? null : line.getVisualOrderText();
        int rawWidth = legacyRenderer ? font.width(line) : font.width(formattedLine);
        float scaledWidth = rawWidth * scale;
        float left = centerX - scaledWidth / 2.0F;
        if (profiling) {
            ItemNameplateDebugBenchmark.recordLayoutNanos(System.nanoTime() - phaseStart);
            phaseStart = System.nanoTime();
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left, topY + 1, 300.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        if (legacyRenderer) {
            // 基准模式保留原来的九次文字调用与每个名称牌显式刷新。
            guiGraphics.flush();
            drawOutlinedText(guiGraphics, font, line, 0, 0, color, outlineColor);
            guiGraphics.flush();
        } else if (!shaderRenderer || !NameplateShaderRenderer.draw(font, formattedLine, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), color)) {
            // 使用原版八方向描边入口，复用一次格式化结果并交给缓冲系统批量提交。
            font.drawInBatch8xOutline(formattedLine, 0, 0, color, outlineColor, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), 15728880);
        }
        guiGraphics.pose().popPose();
        if (profiling) {
            ItemNameplateDebugBenchmark.recordDrawNanos(System.nanoTime() - phaseStart);
        }
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

    private static Component buildSlotLabel(ItemStack stack, Config.NameplateRule rule) {
        String originalName = stack.getHoverName().getString();
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
