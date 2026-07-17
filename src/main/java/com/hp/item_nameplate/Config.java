package com.hp.item_nameplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
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

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path RULES_PATH = FMLPaths.CONFIGDIR.get().resolve("item_nameplate_rules.json");
    private static final String DEFAULT_RULES = """
            {
              "entries": [
                {
                  "desc": "原版附魔书：读取第一条附魔并本地化显示",
                  "target": {
                    "type": "item",
                    "value": "minecraft:enchanted_book"
                  },
                  "text_source": {
                    "type": "component",
                    "component": "minecraft:stored_enchantments",
                    "path": "levels.$keys[0]",
                    "prepend": "enchantment.",
                    "replace": {
                      ":": "."
                    },
                    "i18n": true
                  },
                  "priority": 20
                },
                {
                  "desc": "原版刷怪蛋：兼容简体中文和英文名称",
                  "target": {
                    "type": "class",
                    "value": "net.minecraft.world.item.SpawnEggItem"
                  },
                  "text_source": { "type": "item_name" },
                  "remove_text": ["刷怪蛋", " Spawn Egg"],
                  "priority": 10
                },
                {
                  "desc": "原版普通药水：兼容简体中文和英文名称",
                  "target": {
                    "type": "item",
                    "value": "minecraft:potion"
                  },
                  "text_source": { "type": "item_name" },
                  "remove_text": ["Potion of ", " Potion", "药水"],
                  "replace": {
                    "瞬间治疗": "治疗",
                    "瞬间伤害": "伤害",
                    "Instant Health": "Healing",
                    "Instant Damage": "Harming"
                  },
                  "priority": 10
                },
                {
                  "desc": "原版喷溅型药水：兼容简体中文和英文名称",
                  "target": {
                    "type": "item",
                    "value": "minecraft:splash_potion"
                  },
                  "text_source": { "type": "item_name" },
                  "remove_text": ["Splash Potion of ", "Splash ", " Potion", "喷溅型", "药水"],
                  "replace": {
                    "瞬间治疗": "治疗",
                    "瞬间伤害": "伤害",
                    "Instant Health": "Healing",
                    "Instant Damage": "Harming"
                  },
                  "priority": 10
                },
                {
                  "desc": "原版滞留型药水：兼容简体中文和英文名称",
                  "target": {
                    "type": "item",
                    "value": "minecraft:lingering_potion"
                  },
                  "text_source": { "type": "item_name" },
                  "remove_text": ["Lingering Potion of ", "Lingering ", " Potion", "滞留型", "药水"],
                  "replace": {
                    "瞬间治疗": "治疗",
                    "瞬间伤害": "伤害",
                    "Instant Health": "Healing",
                    "Instant Damage": "Harming"
                  },
                  "priority": 10
                },
                {
                  "desc": "锻造升级模板：从提示框读取具体模板名称",
                  "target": {
                    "type": "tag",
                    "value": "item_nameplate:smithing_templates"
                  },
                  "text_source": {
                    "type": "tooltip",
                    "index": 1
                  },
                  "remove_text": ["升级", " Upgrade"],
                  "priority": 10
                },
                {
                  "desc": "所有锻造模板：从提示框读取具体模板名称",
                  "target": {
                    "type": "class",
                    "value": "net.minecraft.world.item.SmithingTemplateItem"
                  },
                  "text_source": {
                    "type": "tooltip",
                    "index": 1
                  },
                  "remove_text": ["升级", " Upgrade", "盔甲纹饰", " Armor Trim"],
                  "priority": 9
                },
                {
                  "desc": "盔甲纹饰锻造模板：从提示框读取具体纹饰名称",
                  "target": {
                    "type": "tag",
                    "value": "minecraft:trim_templates"
                  },
                  "text_source": {
                    "type": "tooltip",
                    "index": 1
                  },
                  "remove_text": ["盔甲纹饰", " Armor Trim"],
                  "priority": 10
                },
                {
                  "desc": "原版药水箭：兼容简体中文和英文名称",
                  "target": {
                    "type": "item",
                    "value": "minecraft:tipped_arrow"
                  },
                  "text_source": { "type": "item_name" },
                  "remove_text": ["Arrow of ", " Tipped Arrow", "之箭", "药箭"],
                  "replace": {
                    "瞬间治疗": "治疗",
                    "瞬间伤害": "伤害",
                    "Instant Health": "Healing",
                    "Instant Damage": "Harming"
                  },
                  "priority": 10
                }
              ]
            }
            """;
    private static final ModConfigSpec.BooleanValue ENABLED = BUILDER.comment("是否启用物品栏名称牌渲染").define("enabled", true);
    private static final ModConfigSpec.DoubleValue LABEL_SCALE = BUILDER.comment("槽位内紧凑标签缩放").defineInRange("labelScale", 0.6D, 0.3D, 1.0D);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static double labelScale;
    private static List<NameplateRule> nameplateRules = List.of();

    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getType() != ModConfig.Type.CLIENT) {
            return;
        }
        enabled = ENABLED.get();
        labelScale = LABEL_SCALE.get();
        loadNameplateRules();
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
                        targetItem = BuiltInRegistries.ITEM.get(targetId);
                        if (targetItem == null || !BuiltInRegistries.ITEM.containsKey(targetId)) {
                            LOGGER.warn("Skipped nameplate rule at index {} because item {} does not exist", order, targetId);
                            order++;
                            continue;
                        }
                    } else {
                        targetTag = TagKey.create(Registries.ITEM, targetId);
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
                DataComponentType<?> componentType = null;
                Integer tooltipIndex = null;
                if (sourceType.equals("nbt") || sourceType.equals("component")) {
                    if (sourceType.equals("component")) {
                        if (!source.has("component") || !source.get("component").isJsonPrimitive()) {
                            LOGGER.warn("Skipped nameplate rule at index {} because component text_source.component is required", order);
                            order++;
                            continue;
                        }
                        ResourceLocation componentId = ResourceLocation.tryParse(source.get("component").getAsString());
                        if (componentId == null || !BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(componentId)) {
                            LOGGER.warn("Skipped nameplate rule at index {} because data component {} does not exist", order, source.get("component").getAsString());
                            order++;
                            continue;
                        }
                        componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(componentId);
                        if (componentType == null || componentType.isTransient()) {
                            LOGGER.warn("Skipped nameplate rule at index {} because data component {} is not persistent", order, componentId);
                            order++;
                            continue;
                        }
                    }
                    if (!source.has("path") || !source.get("path").isJsonPrimitive()) {
                        LOGGER.warn("Skipped nameplate rule at index {} because {} text_source.path is required", order, sourceType);
                        order++;
                        continue;
                    }
                    path = parseNbtPath(source.get("path").getAsString());
                    if (path.isEmpty()) {
                        LOGGER.warn("Skipped nameplate rule at index {} because text_source.path is invalid", order);
                        order++;
                        continue;
                    }
                } else if (sourceType.equals("tooltip")) {
                    if (!source.has("index") || !source.get("index").isJsonPrimitive()) {
                        LOGGER.warn("Skipped nameplate rule at index {} because tooltip text_source.index is required", order);
                        order++;
                        continue;
                    }
                    tooltipIndex = source.get("index").getAsInt();
                    if (tooltipIndex < 0) {
                        LOGGER.warn("Skipped nameplate rule at index {} because tooltip text_source.index cannot be negative", order);
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
                TextSource textSource = new TextSource(sourceType, componentType, path, tooltipIndex, splitSeparator, splitIndex, joinSeparator, joinPrepend, joinAppend, sourceReplacements, i18n);

                int priority = entry.has("priority") ? entry.get("priority").getAsInt() : 0;
                loadedRules.add(new NameplateRule(targetItem, targetTag, targetClass, textSource, removeText, ruleReplacements, priority, order));
                order++;
            }

            loadedRules.sort((first, second) -> {
                int priorityComparison = Integer.compare(second.priority(), first.priority());
                return priorityComparison != 0 ? priorityComparison : Integer.compare(first.order(), second.order());
            });
            nameplateRules = List.copyOf(loadedRules);
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

    public record NameplateRule(Item item, TagKey<Item> tag, Class<? extends Item> itemClass, TextSource textSource, List<String> removeText, Map<String, String> replacements, int priority, int order) {
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

    public record TextSource(String type, DataComponentType<?> component, List<NbtPathPart> path, Integer tooltipIndex, String splitSeparator, Integer splitIndex, String joinSeparator, String joinPrepend, String joinAppend, Map<String, String> replacements, boolean i18n) {
    }
}
