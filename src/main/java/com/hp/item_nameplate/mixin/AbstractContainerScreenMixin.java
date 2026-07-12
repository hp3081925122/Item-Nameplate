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
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V", shift = At.Shift.BEFORE))
    private void itemNameplate$renderBeforeTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
        ItemNameplateRenderer.renderContainerLabels((AbstractContainerScreen<?>) (Object) this, guiGraphics);
    }
}
