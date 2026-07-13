package com.hp.item_nameplate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.regex.Pattern;

public class ItemNameplateRenderer {
    private static final int NAME_COLOR = 0xFFFFFF55;
    private static final int OUTLINE_COLOR = 0xFF000000;
    private static final int LABEL_WIDTH = 16;

    public static void renderContainerLabels(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics) {
        if (!Config.enabled) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        Font font = minecraft.font;
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.hasItem() || !slot.isActive()) {
                continue;
            }

            ItemStack stack = slot.getItem();
            if (Config.getNameplateRule(stack.getItem()).isEmpty()) {
                continue;
            }

            int slotLeft = screen.getGuiLeft() + slot.x;
            int slotTop = screen.getGuiTop() + slot.y;
            Component slotLabel = buildSlotLabel(stack);
            int labelTop = Math.min(slotTop + 16 - Mth.ceil((font.lineHeight + 2) * (float) Config.labelScale), screen.getGuiTop() + screen.getYSize() - Mth.ceil((font.lineHeight + 2) * (float) Config.labelScale));
            renderScaledLabel(guiGraphics, font, slotLabel, slotLeft + 8, labelTop, NAME_COLOR, OUTLINE_COLOR, (float) Config.labelScale, screen.getGuiLeft(), screen.getGuiLeft() + screen.getXSize());
        }
    }

    @SubscribeEvent
    public void onHotbarRenderPost(RenderGuiOverlayEvent.Post event) {
        if (!Config.enabled || event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int hotbarItemLeft = guiGraphics.guiWidth() / 2 - 88;
        int slotTop = guiGraphics.guiHeight() - 19;
        for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
            ItemStack stack = minecraft.player.getInventory().items.get(slotIndex);
            if (stack.isEmpty() || Config.getNameplateRule(stack.getItem()).isEmpty()) {
                continue;
            }

            Component slotLabel = buildSlotLabel(stack);
            int labelTop = Math.min(slotTop + 16 - Mth.ceil((font.lineHeight + 2) * (float) Config.labelScale), guiGraphics.guiHeight() - Mth.ceil((font.lineHeight + 2) * (float) Config.labelScale));
            renderScaledLabel(guiGraphics, font, slotLabel, hotbarItemLeft + slotIndex * 20 + 8, labelTop, NAME_COLOR, OUTLINE_COLOR, (float) Config.labelScale, 0, guiGraphics.guiWidth());
        }
    }

    private static void renderScaledLabel(GuiGraphics guiGraphics, Font font, Component line, int centerX, int topY, int color, int outlineColor, float scale, int minX, int maxX) {
        if (line == null) {
            return;
        }

        // 按槽位的固定像素宽度截取文字，并为左右描边各预留一个原始字体像素。
        int maxTextWidth = Math.max(1, Mth.floor(LABEL_WIDTH / scale) - 2);
        String text = font.plainSubstrByWidth(line.getString(), maxTextWidth);
        line = Component.literal(text);
        int rawWidth = font.width(line);
        float scaledWidth = rawWidth * scale;
        float left = Mth.clamp(centerX - scaledWidth / 2.0F, minX, Math.max((float) minX, maxX - scaledWidth));

        // 使用无深度遮挡的文字渲染类型，让名称牌始终覆盖物品材质。
        guiGraphics.flush();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left, topY + 1, 200.0F);
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
