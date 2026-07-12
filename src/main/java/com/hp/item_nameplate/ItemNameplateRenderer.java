package com.hp.item_nameplate;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemNameplateRenderer {
    private static final int NAME_COLOR = 0xFFFFFF55;
    private static final int OUTLINE_COLOR = 0xFF000000;
    private static final int LEVEL_COLOR = 0xFFF6D64A;
    private static final float LEVEL_SCALE = 0.6F;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ResourceLocation> LOGGED_RENDER_ITEMS = new HashSet<>();

    @SubscribeEvent
    public void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!Config.enabled) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = minecraft.font;
        Slot hoveredSlot = null;

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.hasItem() || !slot.isActive()) {
                continue;
            }

            ItemStack stack = slot.getItem();
            String renderReason = Config.getRenderReason(stack.getItem());
            if (renderReason == null) {
                continue;
            }

            logRenderReason(stack, renderReason);
            int slotLeft = screen.getGuiLeft() + slot.x;
            int slotTop = screen.getGuiTop() + slot.y;
            if (isMouseOverSlot(event.getMouseX(), event.getMouseY(), slotLeft, slotTop)) {
                hoveredSlot = slot;
            }

            SlotLabel slotLabel = buildSlotLabel(stack);
            int labelTop = getCompactLabelTop(screen, slotTop, font);
            renderScaledLabel(guiGraphics, font, slotLabel.name(), slotLeft + 8, labelTop, NAME_COLOR, OUTLINE_COLOR, (float) Config.labelScale, screen.getGuiLeft(), screen.getGuiLeft() + screen.getXSize());
            if (!slotLabel.level().equals(CommonComponents.EMPTY)) {
                renderLevelLabel(guiGraphics, font, slotLabel.level(), slotLeft + 13, Mth.clamp(slotTop - 1, screen.getGuiTop(), screen.getGuiTop() + screen.getYSize() - 6));
            }
        }

        if (hoveredSlot != null && Config.showHoverDetails) {
            renderHoverDetails(guiGraphics, font, screen, hoveredSlot);
        }
    }

    private void renderHoverDetails(GuiGraphics guiGraphics, Font font, AbstractContainerScreen<?> screen, Slot slot) {
        List<Component> lines = buildLines(slot.getItem());
        if (lines.isEmpty()) {
            return;
        }

        int slotLeft = screen.getGuiLeft() + slot.x;
        int slotTop = screen.getGuiTop() + slot.y;
        int centerX = slotLeft + 8;
        int topY = getHoverDetailTop(screen, slotTop, lines.size(), font);
        int lineHeight = font.lineHeight + 1;
        int minX = screen.getGuiLeft();
        int maxX = screen.getGuiLeft() + screen.getXSize();

        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int lineWidth = font.width(line);
            int lineX = Mth.clamp(centerX - lineWidth / 2, minX, Math.max(minX, maxX - lineWidth));
            int lineY = topY + i * lineHeight;
            drawOutlinedText(guiGraphics, font, line, lineX, lineY, NAME_COLOR, OUTLINE_COLOR);
        }
    }

    private void renderScaledLabel(GuiGraphics guiGraphics, Font font, Component line, int centerX, int topY, int color, int outlineColor, float scale, int minX, int maxX) {
        if (line == null) {
            return;
        }

        int rawWidth = font.width(line);
        float scaledWidth = rawWidth * scale;
        float left = Mth.clamp(centerX - scaledWidth / 2.0F, minX, Math.max((float) minX, maxX - scaledWidth));

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left, topY + 1, 200.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        drawOutlinedText(guiGraphics, font, line, 0, 0, color, outlineColor);
        guiGraphics.pose().popPose();
    }

    private void renderLevelLabel(GuiGraphics guiGraphics, Font font, Component line, int x, int y) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 210.0F);
        guiGraphics.pose().scale(LEVEL_SCALE, LEVEL_SCALE, 1.0F);
        drawOutlinedText(guiGraphics, font, line, 0, 0, LEVEL_COLOR, OUTLINE_COLOR);
        guiGraphics.pose().popPose();
    }

    private int getCompactLabelTop(AbstractContainerScreen<?> screen, int slotTop, Font font) {
        int belowTop = slotTop + 10;
        int scaledHeight = Mth.ceil(font.lineHeight * (float) Config.labelScale);
        int maxBottom = screen.getGuiTop() + screen.getYSize();
        return Mth.clamp(belowTop, screen.getGuiTop(), maxBottom - scaledHeight);
    }

    private int getHoverDetailTop(AbstractContainerScreen<?> screen, int slotTop, int lineCount, Font font) {
        int lineHeight = font.lineHeight + 1;
        int blockHeight = Math.max(1, lineCount) * lineHeight;
        int belowTop = slotTop + 21;
        int maxBottom = screen.getGuiTop() + screen.getYSize();
        if (belowTop + blockHeight <= maxBottom) {
            return belowTop;
        }
        return Math.max(screen.getGuiTop(), slotTop - blockHeight - 2);
    }

    private void drawOutlinedText(GuiGraphics guiGraphics, Font font, Component line, int x, int y, int color, int outlineColor) {
        guiGraphics.drawString(font, line, x, y - 1, outlineColor, false);
        guiGraphics.drawString(font, line, x, y + 1, outlineColor, false);
        guiGraphics.drawString(font, line, x - 1, y, outlineColor, false);
        guiGraphics.drawString(font, line, x + 1, y, outlineColor, false);
        guiGraphics.drawString(font, line, x - 1, y - 1, outlineColor, false);
        guiGraphics.drawString(font, line, x + 1, y - 1, outlineColor, false);
        guiGraphics.drawString(font, line, x - 1, y + 1, outlineColor, false);
        guiGraphics.drawString(font, line, x + 1, y + 1, outlineColor, false);
        guiGraphics.drawString(font, line, x, y, color, false);
    }

    private void logRenderReason(ItemStack stack, String renderReason) {
        ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemKey == null || !LOGGED_RENDER_ITEMS.add(itemKey)) {
            return;
        }
        LOGGER.info("物品名称牌渲染已命中：{}，原因：{}", itemKey, renderReason);
    }

    private boolean isMouseOverSlot(double mouseX, double mouseY, int slotLeft, int slotTop) {
        return mouseX >= slotLeft && mouseX < slotLeft + 16 && mouseY >= slotTop && mouseY < slotTop + 16;
    }

    private List<Component> buildLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        SlotLabel slotLabel = buildSlotLabel(stack);
        lines.add(slotLabel.name());
        if (!slotLabel.level().equals(CommonComponents.EMPTY)) {
            lines.add(slotLabel.level().copy().withStyle(ChatFormatting.GOLD));
        }
        return lines;
    }

    private SlotLabel buildSlotLabel(ItemStack stack) {
        if (stack.getItem() instanceof EnchantedBookItem) {
            List<EnchantmentInstance> enchantments = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack))
                    .entrySet()
                    .stream()
                    .map(entry -> new EnchantmentInstance(entry.getKey(), entry.getValue()))
                    .toList();
            if (!enchantments.isEmpty()) {
                EnchantmentInstance first = enchantments.get(0);
                String fullName = first.enchantment.getFullname(first.level).getString();
                String levelText = toRoman(first.level);
                String enchantName = fullName.endsWith(" " + levelText)
                        ? fullName.substring(0, fullName.length() - levelText.length() - 1)
                        : fullName;
                return new SlotLabel(Component.literal(enchantName), Component.literal(levelText));
            }
        }

        MutableComponent name = Component.literal(stack.getHoverName().getString());
        if (Config.showStackCount && stack.getCount() > 1) {
            name.append(Component.literal(" x" + stack.getCount()).withStyle(ChatFormatting.YELLOW));
        }

        Component enchantLevel = buildEnchantmentLevelLabel(stack);
        return new SlotLabel(name, enchantLevel);
    }

    private Component buildEnchantmentLevelLabel(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) {
            return CommonComponents.EMPTY;
        }

        int maxLevel = enchantments.values()
                .stream()
                .max(Comparator.naturalOrder())
                .orElse(0);
        if (maxLevel <= 0) {
            return CommonComponents.EMPTY;
        }

        return Component.literal(toRoman(maxLevel));
    }

    private String toRoman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(level);
        };
    }

    private record SlotLabel(Component name, Component level) {
    }
}
