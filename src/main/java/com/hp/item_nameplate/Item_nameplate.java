package com.hp.item_nameplate;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(Item_nameplate.MODID)
public class Item_nameplate {
    public static final String MODID = "item_nameplate";

    public Item_nameplate() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
            ForgeRegistries.ITEMS.getValues().forEach(item -> event.register(item, ItemNameplateRenderer.INSTANCE));
        }

        @SubscribeEvent
        public static void registerShaders(RegisterShadersEvent event) {
            // 注册名称牌专用的强度字体与彩色字体描边着色器。
            NameplateShaderRenderer.registerShaders(event);
        }
    }
}
