package com.hp.item_nameplate.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Font.class)
public interface FontAccessor {
    @Invoker("getFontSet")
    FontSet itemNameplate$getFontSet(ResourceLocation id);

    @Accessor("filterFishyGlyphs")
    boolean itemNameplate$getFilterFishyGlyphs();
}
