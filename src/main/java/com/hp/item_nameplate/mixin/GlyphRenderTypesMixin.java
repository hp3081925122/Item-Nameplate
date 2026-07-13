package com.hp.item_nameplate.mixin;

import com.hp.item_nameplate.NameplateShaderRenderer;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlyphRenderTypes.class)
public class GlyphRenderTypesMixin {
    @Inject(method = "createForIntensityTexture", at = @At("RETURN"))
    private static void itemNameplate$captureIntensityTexture(ResourceLocation texture, CallbackInfoReturnable<GlyphRenderTypes> callback) {
        // 保存原版字形渲染类型对应的图集，供名称牌着色器复用。
        NameplateShaderRenderer.registerGlyphRenderTypes(callback.getReturnValue(), texture, false);
    }

    @Inject(method = "createForColorTexture", at = @At("RETURN"))
    private static void itemNameplate$captureColorTexture(ResourceLocation texture, CallbackInfoReturnable<GlyphRenderTypes> callback) {
        // 保存彩色字形图集类型，避免把彩色字体错误按单通道字体采样。
        NameplateShaderRenderer.registerGlyphRenderTypes(callback.getReturnValue(), texture, true);
    }
}
