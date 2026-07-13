package com.hp.item_nameplate;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Item_nameplate.MODID)
public class Item_nameplate {
    public static final String MODID = "item_nameplate";

    public Item_nameplate(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        modEventBus.addListener(Config::onLoad);
    }
}
