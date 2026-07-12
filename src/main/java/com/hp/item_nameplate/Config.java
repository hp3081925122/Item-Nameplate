package com.hp.item_nameplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = Item_nameplate.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path RULES_PATH = FMLPaths.CONFIGDIR.get().resolve("item_nameplate_rules.json");
    private static final String DEFAULT_RULES = """
            {
              "entries": [
                {
                  "desc": "原版附魔书示例：读取第一条 StoredEnchantments 的附魔 ID 并本地化显示",
                  "target": {
                    "type": "item",
                    "value": "minecraft:enchanted_book"
                  },
                  "text_source": {
                    "type": "nbt",
                    "path": "StoredEnchantments[0].id",
                    "prepend": "enchantment.",
                    "replace": {
                      ":": "."
                    },
                    "i18n": true
                  },
                  "priority": 20,
                  "max_length": 3
                },
                {
                  "desc": "所有继承 SpawnEggItem 的刷怪蛋示例",
                  "target": {
                    "type": "class",
                    "value": "net.minecraft.world.item.SpawnEggItem"
                  },
                  "text_source": { "type": "item_name" },
                  "remove_text": ["刷怪蛋"],
                  "priority": 10,
                  "max_length": 3
                },
                {
                  "desc": "原版普通药水示例：删除剂型文字并缩写常用效果名称",
                  "target": {
                    "type": "item",
                    "value": "minecraft:potion"
                  },
                  "text_source": { "type": "item_name" },
                  "remove_text": ["药水", "喷溅型", "滞留型"],
                  "replace": {
                    "瞬间治疗": "治疗",
                    "瞬间伤害": "伤害"
                  },
                  "priority": 10,
                  "max_length": 3
                },
                {
                  "desc": "原版喷溅型药水示例",
                  "target": {
                    "type": "item",
                    "value": "minecraft:splash_potion"
                  },
                  "text_source": { "type": "item_name" },
                  "remove_text": ["药水", "喷溅型", "滞留型"],
                  "replace": {
                    "瞬间治疗": "治疗",
                    "瞬间伤害": "伤害"
                  },
                  "priority": 10,
                  "max_length": 3
                },
                {
                  "desc": "原版滞留型药水示例",
                  "target": {
                    "type": "item",
                    "value": "minecraft:lingering_potion"
                  },
                  "text_source": { "type": "item_name" },
                  "remove_text": ["药水", "喷溅型", "滞留型"],
                  "replace": {
                    "瞬间治疗": "治疗",
                    "瞬间伤害": "伤害"
                  },
                  "priority": 10,
                  "max_length": 3
                }
              ]
            }
            """;
    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER.comment("是否启用物品栏名称牌渲染").define("enabled", true);
    private static final ForgeConfigSpec.DoubleValue LABEL_SCALE = BUILDER.comment("槽位内紧凑标签缩放").defineInRange("labelScale", 0.7D, 0.3D, 1.0D);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static double labelScale;
    private static List<NameplateRule> nameplateRules = List.of();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enabled = ENABLED.get();
        labelScale = LABEL_SCALE.get();
        loadNameplateRules();
    }

    public static String getRenderReason(Item item) {
        if (getNameplateRule(item).isPresent()) {
            return "JSON名称牌规则";
        }
        return null;
    }

    public static Optional<NameplateRule> getNameplateRule(Item item) {
        for (NameplateRule rule : nameplateRules) {
            if (rule.matches(item)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    private static void loadNameplateRules() {
        try {
            Files.createDirectories(RULES_PATH.getParent());
            if (Files.notExists(RULES_PATH)) {
                Files.writeString(RULES_PATH, DEFAULT_RULES, StandardCharsets.UTF_8);
            }

            JsonElement root = JsonParser.parseString(Files.readString(RULES_PATH, StandardCharsets.UTF_8));
            if (!root.isJsonObject() || !root.getAsJsonObject().has("entries") || !root.getAsJsonObject().get("entries").isJsonArray()) {
                LOGGER.error("Invalid nameplate rules root in {}", RULES_PATH);
                nameplateRules = List.of();
                return;
            }

            List<NameplateRule> loadedRules = new ArrayList<>();
            int order = 0;
            for (JsonElement element : root.getAsJsonObject().getAsJsonArray("entries")) {
                if (!element.isJsonObject()) {
                    LOGGER.warn("Skipped non-object nameplate rule at index {}", order);
                    order++;
                    continue;
                }

                JsonObject entry = element.getAsJsonObject();
                if (!entry.has("target") || !entry.get("target").isJsonObject()) {
                    LOGGER.warn("Skipped nameplate rule at index {} because target is required", order);
                    order++;
                    continue;
                }

                JsonObject target = entry.getAsJsonObject("target");
                if (!target.has("type") || !target.has("value") || !target.get("type").isJsonPrimitive() || !target.get("value").isJsonPrimitive()) {
                    LOGGER.warn("Skipped nameplate rule at index {} because target.type and target.value are required", order);
                    order++;
                    continue;
                }

                String targetType = target.get("type").getAsString();
                String targetValue = target.get("value").getAsString();
                Item targetItem = null;
                TagKey<Item> targetTag = null;
                Class<? extends Item> targetClass = null;
                if (targetType.equals("item") || targetType.equals("tag")) {
                    ResourceLocation targetId = ResourceLocation.tryParse(targetValue);
                    if (targetId == null) {
                        LOGGER.warn("Skipped nameplate rule at index {} because target.value is not a valid resource location", order);
                        order++;
                        continue;
                    }
                    if (targetType.equals("item")) {
                        targetItem = ForgeRegistries.ITEMS.getValue(targetId);
                        if (targetItem == null) {
                            LOGGER.warn("Skipped nameplate rule at index {} because item {} does not exist", order, targetId);
                            order++;
                            continue;
                        }
                    } else {
                        targetTag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), targetId);
                    }
                } else if (targetType.equals("class")) {
                    try {
                        targetClass = Class.forName(targetValue, false, Config.class.getClassLoader()).asSubclass(Item.class);
                    } catch (ClassNotFoundException | LinkageError | ClassCastException exception) {
                        LOGGER.warn("Skipped nameplate rule at index {} because item class {} could not be loaded", order, targetValue);
                        order++;
                        continue;
                    }
                } else {
                    LOGGER.warn("Skipped nameplate rule at index {} because target.type {} is unsupported", order, targetType);
                    order++;
                    continue;
                }

                if (!entry.has("text_source") || !entry.get("text_source").isJsonObject()) {
                    LOGGER.warn("Skipped nameplate rule at index {} because text_source is required", order);
                    order++;
                    continue;
                }
                JsonObject source = entry.getAsJsonObject("text_source");
                if (!source.has("type") || !source.get("type").isJsonPrimitive()) {
                    LOGGER.warn("Skipped nameplate rule at index {} because text_source.type is required", order);
                    order++;
                    continue;
                }

                String sourceType = source.get("type").getAsString();
                List<NbtPathPart> path = List.of();
                if (sourceType.equals("nbt")) {
                    if (!source.has("path") || !source.get("path").isJsonPrimitive()) {
                        LOGGER.warn("Skipped nameplate rule at index {} because NBT text_source.path is required", order);
                        order++;
                        continue;
                    }
                    path = parseNbtPath(source.get("path").getAsString());
                    if (path.isEmpty()) {
                        LOGGER.warn("Skipped nameplate rule at index {} because text_source.path is invalid", order);
                        order++;
                        continue;
                    }

                } else if (!sourceType.equals("item_name")) {
                    LOGGER.warn("Skipped nameplate rule at index {} because text_source.type {} is unsupported", order, sourceType);
                    order++;
                    continue;
                }

                String splitSeparator = null;
                Integer splitIndex = null;
                if (source.has("split")) {
                        if (!source.get("split").isJsonObject() || !source.getAsJsonObject("split").has("separator") || !source.getAsJsonObject("split").get("separator").isJsonPrimitive()) {
                            LOGGER.warn("Skipped nameplate rule at index {} because text_source.split.separator is required", order);
                            order++;
                            continue;
                        }
                        JsonObject split = source.getAsJsonObject("split");
                        splitSeparator = split.get("separator").getAsString();
                        if (splitSeparator.isEmpty()) {
                            LOGGER.warn("Skipped nameplate rule at index {} because text_source.split.separator cannot be empty", order);
                            order++;
                            continue;
                        }
                        if (split.has("index")) {
                            splitIndex = split.get("index").getAsInt();
                            if (splitIndex < 0) {
                                LOGGER.warn("Skipped nameplate rule at index {} because text_source.split.index cannot be negative", order);
                                order++;
                                continue;
                            }
                        }
                }

                String joinSeparator = null;
                String joinPrepend = source.has("prepend") ? source.get("prepend").getAsString() : "";
                String joinAppend = "";
                if (source.has("join")) {
                        if (!source.get("join").isJsonObject() || !source.getAsJsonObject("join").has("separator") || !source.getAsJsonObject("join").get("separator").isJsonPrimitive()) {
                            LOGGER.warn("Skipped nameplate rule at index {} because text_source.join.separator is required", order);
                            order++;
                            continue;
                        }
                        JsonObject join = source.getAsJsonObject("join");
                        joinSeparator = join.get("separator").getAsString();
                        joinPrepend += join.has("prepend") ? join.get("prepend").getAsString() : "";
                        joinAppend = join.has("append") ? join.get("append").getAsString() : "";
                }

                Map<String, String> sourceReplacements = new LinkedHashMap<>();
                if (source.has("replace") && source.get("replace").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> replacement : source.getAsJsonObject("replace").entrySet()) {
                        if (!replacement.getKey().isEmpty() && replacement.getValue().isJsonPrimitive()) {
                            sourceReplacements.put(replacement.getKey(), replacement.getValue().getAsString());
                        }
                    }
                }

                List<String> removeText = new ArrayList<>();
                if (entry.has("remove_text") && entry.get("remove_text").isJsonArray()) {
                    for (JsonElement removeElement : entry.getAsJsonArray("remove_text")) {
                        if (removeElement.isJsonPrimitive() && !removeElement.getAsString().isEmpty()) {
                            removeText.add(removeElement.getAsString());
                        }
                    }
                }
                Map<String, String> ruleReplacements = new LinkedHashMap<>();
                if (entry.has("replace") && entry.get("replace").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> replacement : entry.getAsJsonObject("replace").entrySet()) {
                        if (!replacement.getKey().isEmpty() && replacement.getValue().isJsonPrimitive()) {
                            ruleReplacements.put(replacement.getKey(), replacement.getValue().getAsString());
                        }
                    }
                }
                boolean i18n = source.has("i18n") && source.get("i18n").getAsBoolean();
                TextSource textSource = new TextSource(sourceType, path, splitSeparator, splitIndex, joinSeparator, joinPrepend, joinAppend, sourceReplacements, i18n);

                int priority = entry.has("priority") ? entry.get("priority").getAsInt() : 0;
                int maxLength = entry.has("max_length") ? entry.get("max_length").getAsInt() : -1;
                if (maxLength == 0 || maxLength < -1) {
                    LOGGER.warn("Skipped nameplate rule at index {} because max_length must be positive", order);
                    order++;
                    continue;
                }
                loadedRules.add(new NameplateRule(targetItem, targetTag, targetClass, textSource, removeText, ruleReplacements, priority, order, maxLength));
                order++;
            }

            loadedRules.sort((first, second) -> {
                int priorityComparison = Integer.compare(second.priority(), first.priority());
                return priorityComparison != 0 ? priorityComparison : Integer.compare(first.order(), second.order());
            });
            nameplateRules = List.copyOf(loadedRules);
            LOGGER.info("Loaded {} nameplate rules from {}", nameplateRules.size(), RULES_PATH);
        } catch (IOException | IllegalStateException exception) {
            LOGGER.error("Failed to load nameplate rules from {}", RULES_PATH, exception);
            nameplateRules = List.of();
        }
    }

    private static List<NbtPathPart> parseNbtPath(String path) {
        if (path.isEmpty()) {
            return List.of();
        }
        List<NbtPathPart> parts = new ArrayList<>();
        for (String segment : path.split("\\.")) {
            int bracketStart = segment.indexOf('[');
            int bracketEnd = segment.indexOf(']');
            if (segment.isEmpty() || bracketStart == 0 || (bracketStart >= 0 && (bracketEnd != segment.length() - 1 || segment.indexOf('[', bracketStart + 1) >= 0))) {
                return List.of();
            }
            String key = bracketStart < 0 ? segment : segment.substring(0, bracketStart);
            Integer index = null;
            if (bracketStart >= 0) {
                try {
                    index = Integer.parseInt(segment.substring(bracketStart + 1, bracketEnd));
                    if (index < 0) {
                        return List.of();
                    }
                } catch (NumberFormatException exception) {
                    return List.of();
                }
            }
            parts.add(new NbtPathPart(key, index));
        }
        return List.copyOf(parts);
    }

    public record NameplateRule(Item item, TagKey<Item> tag, Class<? extends Item> itemClass, TextSource textSource, List<String> removeText, Map<String, String> replacements, int priority, int order, int maxLength) {
        public boolean matches(Item candidate) {
            if (item != null) {
                return item == candidate;
            }
            if (tag != null) {
                return candidate.builtInRegistryHolder().is(tag);
            }
            return itemClass.isInstance(candidate);
        }
    }

    public record NbtPathPart(String key, Integer index) {
    }

    public record TextSource(String type, List<NbtPathPart> path, String splitSeparator, Integer splitIndex, String joinSeparator, String joinPrepend, String joinAppend, Map<String, String> replacements, boolean i18n) {
    }
}
