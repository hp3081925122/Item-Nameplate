package com.hp.item_nameplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path RULES_DIRECTORY = FMLPaths.CONFIGDIR.get().resolve("item_nameplate_rules");
    private static final Path LEGACY_RULES_PATH = FMLPaths.CONFIGDIR.get().resolve("item_nameplate_rules.json");
    private static final Path DEFAULT_RULES_PATH = RULES_DIRECTORY.resolve("minecraft.json");
    private static final String DEFAULT_RULES_RESOURCE_DIRECTORY = "item_nameplate/default_rules/";
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
            Files.createDirectories(RULES_DIRECTORY);
            if (Files.exists(LEGACY_RULES_PATH) && Files.notExists(DEFAULT_RULES_PATH)) {
                Files.move(LEGACY_RULES_PATH, DEFAULT_RULES_PATH);
            } else if (Files.exists(LEGACY_RULES_PATH) && Files.notExists(RULES_DIRECTORY.resolve("legacy.json"))) {
                Files.move(LEGACY_RULES_PATH, RULES_DIRECTORY.resolve("legacy.json"));
            }

            URL bundledRulesUrl = Config.class.getClassLoader().getResource(DEFAULT_RULES_RESOURCE_DIRECTORY);
            if (bundledRulesUrl == null) {
                LOGGER.error("Missing bundled nameplate rules directory {}", DEFAULT_RULES_RESOURCE_DIRECTORY);
            } else if (bundledRulesUrl.getProtocol().equals("file")) {
                Path bundledRulesPath = Path.of(bundledRulesUrl.toURI());
                try (Stream<Path> bundledRulePaths = Files.walk(bundledRulesPath)) {
                    List<Path> jsonPaths = bundledRulePaths
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".json"))
                            .sorted()
                            .toList();
                    for (Path bundledRulePath : jsonPaths) {
                        Path targetPath = RULES_DIRECTORY.resolve(bundledRulesPath.relativize(bundledRulePath)).normalize();
                        if (Files.exists(targetPath)) {
                            continue;
                        }
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(bundledRulePath, targetPath);
                    }
                }
            } else if (bundledRulesUrl.getProtocol().equals("jar")) {
                JarURLConnection connection = (JarURLConnection) bundledRulesUrl.openConnection();
                connection.setUseCaches(false);
                String resourcePrefix = connection.getEntryName();
                try (JarFile jarFile = connection.getJarFile()) {
                    List<String> jsonPaths = jarFile.stream()
                            .filter(entry -> !entry.isDirectory() && entry.getName().startsWith(resourcePrefix) && entry.getName().endsWith(".json"))
                            .map(entry -> entry.getName().substring(resourcePrefix.length()))
                            .sorted()
                            .toList();
                    for (String jsonPath : jsonPaths) {
                        Path targetPath = RULES_DIRECTORY.resolve(jsonPath).normalize();
                        if (Files.exists(targetPath)) {
                            continue;
                        }
                        Files.createDirectories(targetPath.getParent());
                        try (InputStream bundledRuleStream = jarFile.getInputStream(jarFile.getJarEntry(resourcePrefix + jsonPath))) {
                            Files.copy(bundledRuleStream, targetPath);
                        }
                    }
                }
            } else {
                LOGGER.error("Unsupported bundled nameplate rules protocol {}", bundledRulesUrl.getProtocol());
            }

            List<NameplateRule> loadedRules = new ArrayList<>();
            int order = 0;
            try (Stream<Path> rulePaths = Files.walk(RULES_DIRECTORY)) {
                List<Path> jsonPaths = rulePaths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted(Comparator.comparing(path -> RULES_DIRECTORY.relativize(path).toString(), String.CASE_INSENSITIVE_ORDER))
                        .toList();
                for (Path rulePath : jsonPaths) {
                    JsonElement root;
                    try {
                        root = JsonParser.parseString(Files.readString(rulePath, StandardCharsets.UTF_8));
                    } catch (IOException | JsonParseException exception) {
                        LOGGER.error("Failed to read nameplate rules from {}", rulePath, exception);
                        continue;
                    }
                    if (!root.isJsonObject() || !root.getAsJsonObject().has("entries") || !root.getAsJsonObject().get("entries").isJsonArray()) {
                        LOGGER.error("Invalid nameplate rules root in {}", rulePath);
                        continue;
                    }

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
                }
            }

            loadedRules.sort((first, second) -> {
                int priorityComparison = Integer.compare(second.priority(), first.priority());
                return priorityComparison != 0 ? priorityComparison : Integer.compare(first.order(), second.order());
            });
            nameplateRules = List.copyOf(loadedRules);
        } catch (IOException | IllegalStateException | URISyntaxException exception) {
            LOGGER.error("Failed to load nameplate rules from {}", RULES_DIRECTORY, exception);
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
