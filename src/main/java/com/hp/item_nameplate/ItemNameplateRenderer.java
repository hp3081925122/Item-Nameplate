package com.hp.item_nameplate;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.Set;

public class ItemNameplateRenderer {
    private static final int NAME_COLOR = 0xFFFFFF55;
    private static final int OUTLINE_COLOR = 0xFF000000;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ResourceLocation> LOGGED_RENDER_ITEMS = new HashSet<>();

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
            String renderReason = Config.getRenderReason(stack.getItem());
            if (renderReason == null) {
                continue;
            }

            logRenderReason(stack, renderReason);
            int slotLeft = screen.getGuiLeft() + slot.x;
            int slotTop = screen.getGuiTop() + slot.y;
            Component slotLabel = buildSlotLabel(stack);
            int labelTop = getCompactLabelTop(screen, slotTop, font);
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
            String renderReason = Config.getRenderReason(stack.getItem());
            if (stack.isEmpty() || renderReason == null) {
                continue;
            }

            logRenderReason(stack, renderReason);
            Component slotLabel = buildSlotLabel(stack);
            int labelTop = Math.min(slotTop + 15, guiGraphics.guiHeight() - Mth.ceil(font.lineHeight * (float) Config.labelScale) - 1);
            renderScaledLabel(guiGraphics, font, slotLabel, hotbarItemLeft + slotIndex * 20 + 8, labelTop, NAME_COLOR, OUTLINE_COLOR, (float) Config.labelScale, 0, guiGraphics.guiWidth());
        }
    }

    private static void renderScaledLabel(GuiGraphics guiGraphics, Font font, Component line, int centerX, int topY, int color, int outlineColor, float scale, int minX, int maxX) {
        if (line == null) {
            return;
        }

        int rawWidth = font.width(line);
        float scaledWidth = rawWidth * scale;
        float left = Mth.clamp(centerX - scaledWidth / 2.0F, minX, Math.max((float) minX, maxX - scaledWidth));

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left, topY + 1, 150.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        drawOutlinedText(guiGraphics, font, line, 0, 0, color, outlineColor);
        guiGraphics.pose().popPose();
    }

    private static int getCompactLabelTop(AbstractContainerScreen<?> screen, int slotTop, Font font) {
        int belowTop = slotTop + 15;
        int scaledHeight = Mth.ceil(font.lineHeight * (float) Config.labelScale);
        int maxBottom = screen.getGuiTop() + screen.getYSize();
        return Mth.clamp(belowTop, screen.getGuiTop(), maxBottom - scaledHeight);
    }

    private static void drawOutlinedText(GuiGraphics guiGraphics, Font font, Component line, int x, int y, int color, int outlineColor) {
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

    private static void logRenderReason(ItemStack stack, String renderReason) {
        ResourceLocation itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemKey == null || !LOGGED_RENDER_ITEMS.add(itemKey)) {
            return;
        }
        LOGGER.info("物品名称牌渲染已命中：{}，原因：{}", itemKey, renderReason);
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
        if (rule.maxLength() > 0 && displayName.codePointCount(0, displayName.length()) > rule.maxLength()) {
            displayName = displayName.substring(0, displayName.offsetByCodePoints(0, rule.maxLength()));
        }

        return Component.literal(displayName);
    }

    private static String readTextSource(ItemStack stack, Config.TextSource source) {
        if (source.type().equals("item_name")) {
            return stack.getHoverName().getString();
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
