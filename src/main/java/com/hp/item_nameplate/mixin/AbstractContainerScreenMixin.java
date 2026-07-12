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
    // 在原版提示框之前绘制名称牌，使提示框只覆盖实际阻挡到的名称牌区域。
    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void itemNameplate$renderBeforeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo callbackInfo) {
        ItemNameplateRenderer.renderContainerLabels((AbstractContainerScreen<?>) (Object) this, guiGraphics);
    }
}
