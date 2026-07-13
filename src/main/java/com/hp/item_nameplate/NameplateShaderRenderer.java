package com.hp.item_nameplate;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.hp.item_nameplate.mixin.BakedGlyphAccessor;
import com.hp.item_nameplate.mixin.FontAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class NameplateShaderRenderer {
    private static final Map<GlyphRenderTypes, GlyphTexture> GLYPH_TEXTURES = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<GlyphTexture, RenderType> RENDER_TYPES = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Logger LOGGER = LogUtils.getLogger();
    private static ShaderInstance intensityShader;
    private static ShaderInstance colorShader;

    private NameplateShaderRenderer() {
    }

    public static void registerShaders(RegisterShadersEvent event) {
        // 用原版文字顶点格式注册两种图集通道对应的单遍描边着色器。
        intensityShader = null;
        colorShader = null;
        GLYPH_TEXTURES.clear();
        RENDER_TYPES.clear();
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(Item_nameplate.MODID, "nameplate_outline_intensity"), DefaultVertexFormat.NEW_ENTITY), shader -> intensityShader = shader);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(Item_nameplate.MODID, "nameplate_outline_color"), DefaultVertexFormat.NEW_ENTITY), shader -> colorShader = shader);
        } catch (IOException exception) {
            LOGGER.error("Failed to load item nameplate outline shaders; using vanilla outline renderer", exception);
        }
    }

    public static void registerGlyphRenderTypes(GlyphRenderTypes renderTypes, ResourceLocation texture, boolean colored) {
        // 字体重载时会创建新的对象，用身份映射保证每个字形精确找到所属图集。
        GLYPH_TEXTURES.put(renderTypes, new GlyphTexture(texture, colored));
    }

    public static boolean draw(Font font, FormattedCharSequence line, Matrix4f pose, MultiBufferSource bufferSource, int color) {
        if (intensityShader == null || colorShader == null) {
            return false;
        }

        // 名称牌目前是无格式纯文本，这里仍按实际 Style 选择字体并计算字形步进。
        float[] cursor = new float[]{0.0F};
        line.accept((index, style, codePoint) -> {
            FontSet fontSet = ((FontAccessor) font).itemNameplate$getFontSet(style.getFont());
            GlyphInfo glyphInfo = fontSet.getGlyphInfo(codePoint, ((FontAccessor) font).itemNameplate$getFilterFishyGlyphs());
            BakedGlyph glyph = fontSet.getGlyph(codePoint);
            if (!(glyph instanceof EmptyGlyph)) {
                BakedGlyphAccessor accessor = (BakedGlyphAccessor) glyph;
                GlyphTexture glyphTexture = GLYPH_TEXTURES.get(accessor.itemNameplate$getRenderTypes());
                if (glyphTexture == null) {
                    cursor[0] += glyphInfo.getAdvance(style.isBold());
                    return true;
                }

                // 每个字形只提交一个向四周扩展一像素的四边形，描边由片元着色器采样完成。
                VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPES.computeIfAbsent(glyphTexture, NameplateShaderRenderer::createRenderType));
                float left = cursor[0] + accessor.itemNameplate$getLeft();
                float right = cursor[0] + accessor.itemNameplate$getRight();
                float top = accessor.itemNameplate$getUp() - 3.0F;
                float bottom = accessor.itemNameplate$getDown() - 3.0F;
                float u0 = accessor.itemNameplate$getU0();
                float u1 = accessor.itemNameplate$getU1();
                float v0 = accessor.itemNameplate$getV0();
                float v1 = accessor.itemNameplate$getV1();
                int minU = (int) Math.floor(u0 * 256.0F);
                int maxU = (int) Math.ceil(u1 * 256.0F);
                int minV = (int) Math.floor(v0 * 256.0F);
                int maxV = (int) Math.ceil(v1 * 256.0F);
                int red = color >> 16 & 255;
                int green = color >> 8 & 255;
                int blue = color & 255;
                int alpha = (color & 0xFC000000) == 0 ? 255 : color >>> 24;
                consumer.vertex(pose, left - 1.0F, top - 1.0F, 0.0F).color(red, green, blue, alpha).uv(u0 - 1.0F / 256.0F, v0 - 1.0F / 256.0F).overlayCoords(minU, maxU).uv2(minV, maxV).normal(0.0F, 0.0F, 1.0F).endVertex();
                consumer.vertex(pose, left - 1.0F, bottom + 1.0F, 0.0F).color(red, green, blue, alpha).uv(u0 - 1.0F / 256.0F, v1 + 1.0F / 256.0F).overlayCoords(minU, maxU).uv2(minV, maxV).normal(0.0F, 0.0F, 1.0F).endVertex();
                consumer.vertex(pose, right + 1.0F, bottom + 1.0F, 0.0F).color(red, green, blue, alpha).uv(u1 + 1.0F / 256.0F, v1 + 1.0F / 256.0F).overlayCoords(minU, maxU).uv2(minV, maxV).normal(0.0F, 0.0F, 1.0F).endVertex();
                consumer.vertex(pose, right + 1.0F, top - 1.0F, 0.0F).color(red, green, blue, alpha).uv(u1 + 1.0F / 256.0F, v0 - 1.0F / 256.0F).overlayCoords(minU, maxU).uv2(minV, maxV).normal(0.0F, 0.0F, 1.0F).endVertex();
            }
            cursor[0] += glyphInfo.getAdvance(style.isBold());
            return true;
        });
        return true;
    }

    private static RenderType createRenderType(GlyphTexture glyphTexture) {
        ShaderInstance shader = glyphTexture.colored ? colorShader : intensityShader;
        // 使用无深度测试且只写颜色的文字状态，保持名称牌位于物品材质上方。
        return RenderType.create(
                "item_nameplate_outline",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> shader))
                        .setTextureState(new RenderStateShard.TextureStateShard(glyphTexture.texture, false, false))
                        .setTransparencyState(new RenderStateShard.TransparencyStateShard("item_nameplate_transparency", () -> {
                            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                        }, () -> com.mojang.blaze3d.systems.RenderSystem.disableBlend()))
                        .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                        .setDepthTestState(new RenderStateShard.DepthTestStateShard("always", 519))
                        .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                        .createCompositeState(false)
        );
    }

    private record GlyphTexture(ResourceLocation texture, boolean colored) {
    }
}
