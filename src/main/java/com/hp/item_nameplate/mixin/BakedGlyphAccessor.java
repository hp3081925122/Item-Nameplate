package com.hp.item_nameplate.mixin;

import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BakedGlyph.class)
public interface BakedGlyphAccessor {
    @Accessor("renderTypes")
    GlyphRenderTypes itemNameplate$getRenderTypes();

    @Accessor("u0")
    float itemNameplate$getU0();

    @Accessor("u1")
    float itemNameplate$getU1();

    @Accessor("v0")
    float itemNameplate$getV0();

    @Accessor("v1")
    float itemNameplate$getV1();

    @Accessor("left")
    float itemNameplate$getLeft();

    @Accessor("right")
    float itemNameplate$getRight();

    @Accessor("up")
    float itemNameplate$getUp();

    @Accessor("down")
    float itemNameplate$getDown();
}
