package com.hp.item_nameplate;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Item_nameplate.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final TagKey<Item> DEFAULT_RENDER_TAG = TagKey.create(Registries.ITEM, ResourceLocation.tryParse(Item_nameplate.MODID + ":show_nameplate"));

    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER.comment("是否启用物品栏名称牌渲染").define("enabled", true);
    private static final ForgeConfigSpec.DoubleValue LABEL_SCALE = BUILDER.comment("槽位内紧凑标签缩放").defineInRange("labelScale", 1.0D, 0.3D, 1.0D);
    private static final ForgeConfigSpec.BooleanValue SHOW_STACK_COUNT = BUILDER.comment("是否显示堆叠数量").define("showStackCount", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_ENCHANTED_BOOK = BUILDER.comment("是否渲染附魔书名称牌").define("enableEnchantedBook", true);
    private static final ForgeConfigSpec.BooleanValue SHOW_HOVER_DETAILS = BUILDER.comment("鼠标移到槽位上时是否显示完整名称牌").define("showHoverDetails", true);
    private static final ForgeConfigSpec.BooleanValue ENABLE_DEFAULT_TAG = BUILDER.comment("是否启用模组自带标签 item_nameplate:show_nameplate").define("enableDefaultTag", true);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> RENDER_ITEM_IDS = BUILDER.comment("需要渲染名称牌的物品ID列表").defineListAllowEmpty("renderItemIds", List.of(), Config::isValidItemId);
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> RENDER_ITEM_TAGS = BUILDER.comment("需要渲染名称牌的物品标签列表").defineListAllowEmpty("renderItemTags", List.of(), Config::isValidTagId);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static double labelScale;
    public static boolean showStackCount;
    public static boolean enableEnchantedBook;
    public static boolean showHoverDetails;
    public static boolean enableDefaultTag;
    public static Set<Item> renderItems;
    public static Set<TagKey<Item>> renderItemTags;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enabled = ENABLED.get();
        labelScale = LABEL_SCALE.get();
        showStackCount = SHOW_STACK_COUNT.get();
        enableEnchantedBook = ENABLE_ENCHANTED_BOOK.get();
        showHoverDetails = SHOW_HOVER_DETAILS.get();
        enableDefaultTag = ENABLE_DEFAULT_TAG.get();
        renderItems = RENDER_ITEM_IDS.get()
                .stream()
                .map(ResourceLocation::tryParse)
                .map(ForgeRegistries.ITEMS::getValue)
                .filter(item -> item != null)
                .collect(Collectors.toSet());
        renderItemTags = RENDER_ITEM_TAGS.get()
                .stream()
                .map(ResourceLocation::tryParse)
                .filter(tagId -> tagId != null)
                .map(tagId -> TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tagId))
                .collect(Collectors.toSet());
    }

    private static boolean isValidItemId(final Object value) {
        if (!(value instanceof final String itemId)) {
            return false;
        }
        ResourceLocation itemKey = ResourceLocation.tryParse(itemId);
        return itemKey != null && ForgeRegistries.ITEMS.containsKey(itemKey);
    }

    private static boolean isValidTagId(final Object value) {
        if (!(value instanceof final String tagId)) {
            return false;
        }
        return ResourceLocation.tryParse(tagId) != null;
    }

    public static boolean shouldRenderItem(Item item) {
        return getRenderReason(item) != null;
    }

    public static String getRenderReason(Item item) {
        if (enableEnchantedBook && item == net.minecraft.world.item.Items.ENCHANTED_BOOK) {
            return "附魔书开关";
        }
        if (renderItems.contains(item)) {
            return "物品ID白名单";
        }
        if (enableDefaultTag && item.builtInRegistryHolder().is(DEFAULT_RENDER_TAG)) {
            return "默认标签 item_nameplate:show_nameplate";
        }
        for (TagKey<Item> tagKey : renderItemTags) {
            if (item.builtInRegistryHolder().is(tagKey)) {
                return "配置标签白名单 " + tagKey.location();
            }
        }
        return null;
    }
}
