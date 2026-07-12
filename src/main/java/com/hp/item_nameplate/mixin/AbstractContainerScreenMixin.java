package com.hp.item_nameplate.mixin;

import com.hp.item_nameplate.ItemNameplateRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
    // 在原版提示框绘制前渲染名称牌，确保其位于物品材质之上、提示框之下。
    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void itemNameplate$renderBeforeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo callbackInfo) {
        ItemNameplateRenderer.renderContainerLabels((AbstractContainerScreen<?>) (Object) this, guiGraphics);
    }
}
