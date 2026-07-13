package com.hp.item_nameplate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = Item_nameplate.MODID, value = Dist.CLIENT)
public class ItemNameplateDebugBenchmark {
    private static final int ITEM_COUNT = 200;
    private static final int COLUMNS = 20;
    private static final int ROWS = 10;
    private static final int CELL_SIZE = 18;
    private static boolean enabled;
    private static RendererMode rendererMode = RendererMode.OPTIMIZED;
    private static List<ItemStack> testStacks = List.of();
    private static long measurementStartNanos;
    private static long measuredFrames;
    private static boolean profilingNameplate;
    private static long sampledFrames;
    private static long itemNanos;
    private static long decorationNanos;
    private static long ruleNanos;
    private static long textNanos;
    private static long layoutNanos;
    private static long drawNanos;
    private static long finalFlushNanos;
    private static long gridNanos;

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        // 注册仅客户端可用的基准测试开关，避免影响服务端指令树。
        event.getDispatcher().register(Commands.literal("item_nameplate_debug")
                .executes(context -> {
                    enabled = !enabled;
                    if (enabled && !prepareTestStacks()) {
                        enabled = false;
                        context.getSource().sendFailure(Component.literal("No items match the current nameplate rules"));
                        return 0;
                    }
                    rendererMode = RendererMode.OPTIMIZED;
                    resetMeasurement();
                    context.getSource().sendSuccess(() -> Component.literal("Item nameplate benchmark: " + (enabled ? "ON" : "OFF")), false);
                    return 1;
                })
                .then(Commands.literal("on").executes(context -> {
                    if (!prepareTestStacks()) {
                        context.getSource().sendFailure(Component.literal("No items match the current nameplate rules"));
                        return 0;
                    }
                    rendererMode = RendererMode.OPTIMIZED;
                    enabled = true;
                    resetMeasurement();
                    context.getSource().sendSuccess(() -> Component.literal("Item nameplate benchmark: ON"), false);
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    enabled = false;
                    testStacks = List.of();
                    resetMeasurement();
                    context.getSource().sendSuccess(() -> Component.literal("Item nameplate benchmark: OFF"), false);
                    return 1;
                }))
                .then(Commands.literal("legacy").executes(context -> {
                    if (!prepareTestStacks()) {
                        context.getSource().sendFailure(Component.literal("No items match the current nameplate rules"));
                        return 0;
                    }
                    rendererMode = RendererMode.LEGACY;
                    enabled = true;
                    resetMeasurement();
                    context.getSource().sendSuccess(() -> Component.literal("Item nameplate benchmark: LEGACY"), false);
                    return 1;
                }))
                .then(Commands.literal("optimized").executes(context -> {
                    if (!prepareTestStacks()) {
                        context.getSource().sendFailure(Component.literal("No items match the current nameplate rules"));
                        return 0;
                    }
                    rendererMode = RendererMode.OPTIMIZED;
                    enabled = true;
                    resetMeasurement();
                    context.getSource().sendSuccess(() -> Component.literal("Item nameplate benchmark: OPTIMIZED"), false);
                    return 1;
                }))
                .then(Commands.literal("shader").executes(context -> {
                    if (!prepareTestStacks()) {
                        context.getSource().sendFailure(Component.literal("No items match the current nameplate rules"));
                        return 0;
                    }
                    rendererMode = RendererMode.SHADER;
                    enabled = true;
                    resetMeasurement();
                    context.getSource().sendSuccess(() -> Component.literal("Item nameplate benchmark: SHADER"), false);
                    return 1;
                })));
    }

    static boolean useLegacyRenderer() {
        return enabled && rendererMode == RendererMode.LEGACY;
    }

    static boolean useShaderRenderer() {
        return enabled ? rendererMode == RendererMode.SHADER : Config.useShader;
    }

    static boolean isProfilingNameplate() {
        return profilingNameplate;
    }

    static void recordRuleNanos(long nanos) {
        ruleNanos += nanos;
    }

    static void recordTextNanos(long nanos) {
        textNanos += nanos;
    }

    static void recordLayoutNanos(long nanos) {
        layoutNanos += nanos;
    }

    static void recordDrawNanos(long nanos) {
        drawNanos += nanos;
    }

    private static void resetMeasurement() {
        measurementStartNanos = System.nanoTime();
        measuredFrames = 0;
        profilingNameplate = false;
        sampledFrames = 0;
        itemNanos = 0;
        decorationNanos = 0;
        ruleNanos = 0;
        textNanos = 0;
        layoutNanos = 0;
        drawNanos = 0;
        finalFlushNanos = 0;
        gridNanos = 0;
    }

    private static boolean prepareTestStacks() {
        // 只在开启测试时扫描一次规则，避免把注册表查找开销计入每帧结果。
        List<ItemStack> matchedStacks = new ArrayList<>();
        ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> Config.getNameplateRule(item).isPresent())
                .map(ItemStack::new)
                .forEach(matchedStacks::add);
        if (matchedStacks.isEmpty()) {
            testStacks = List.of();
            return false;
        }

        // 生成两百个独立物品栈，使每个格子都走真实物品装饰器渲染路径。
        List<ItemStack> generatedStacks = new ArrayList<>(ITEM_COUNT);
        for (int index = 0; index < ITEM_COUNT; index++) {
            generatedStacks.add(matchedStacks.get(index % matchedStacks.size()).copy());
        }
        testStacks = List.copyOf(generatedStacks);
        return true;
    }

    @SubscribeEvent
    public static void renderBenchmark(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!enabled || testStacks.size() != ITEM_COUNT || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        measuredFrames++;
        boolean sampleFrame = measuredFrames % 20L == 1L;
        long frameStart = sampleFrame ? System.nanoTime() : 0L;
        long frameItemNanos = 0L;
        long frameDecorationNanos = 0L;
        int gridWidth = COLUMNS * CELL_SIZE;
        int gridHeight = ROWS * CELL_SIZE;
        float scale = Math.min(1.0F, Math.min((guiGraphics.guiWidth() - 8.0F) / gridWidth, (guiGraphics.guiHeight() - 26.0F) / gridHeight));
        scale = Math.max(scale, 0.1F);
        int left = Mth.floor((guiGraphics.guiWidth() / scale - gridWidth) / 2.0F);
        int top = Mth.floor((guiGraphics.guiHeight() / scale - gridHeight) / 2.0F);

        // 用统一矩阵缩放完整网格，保证不同 GUI 缩放设置下仍同时显示两百个物品。
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0F);
        for (int index = 0; index < ITEM_COUNT; index++) {
            int x = left + index % COLUMNS * CELL_SIZE;
            int y = top + index / COLUMNS * CELL_SIZE;
            ItemStack stack = testStacks.get(index);
            if (sampleFrame) {
                long phaseStart = System.nanoTime();
                guiGraphics.renderItem(stack, x, y);
                frameItemNanos += System.nanoTime() - phaseStart;
                profilingNameplate = true;
                phaseStart = System.nanoTime();
                guiGraphics.renderItemDecorations(minecraft.font, stack, x, y);
                frameDecorationNanos += System.nanoTime() - phaseStart;
                profilingNameplate = false;
            } else {
                guiGraphics.renderItem(stack, x, y);
                guiGraphics.renderItemDecorations(minecraft.font, stack, x, y);
            }
        }
        guiGraphics.pose().popPose();

        long flushStart = sampleFrame ? System.nanoTime() : 0L;
        guiGraphics.flush();
        if (sampleFrame) {
            sampledFrames++;
            itemNanos += frameItemNanos;
            decorationNanos += frameDecorationNanos;
            finalFlushNanos += System.nanoTime() - flushStart;
            gridNanos += System.nanoTime() - frameStart;
        }

        // 在未缩放的 HUD 坐标中显示帧率与分阶段采样结果。
        double measuredSeconds = Math.max((System.nanoTime() - measurementStartNanos) / 1_000_000_000.0D, 0.001D);
        int averageFps = Mth.floor(measuredFrames / measuredSeconds);
        double divisor = Math.max(sampledFrames, 1L) * 1_000_000.0D;
        double averageGridMs = gridNanos / divisor;
        double averageDecorationMs = decorationNanos / divisor;
        double averageNameplateMs = (ruleNanos + textNanos + layoutNanos + drawNanos) / divisor;
        double averageDecorationOtherMs = Math.max(0.0D, averageDecorationMs - averageNameplateMs);
        List<String> stats = List.of(
                "Mode: " + rendererMode + " | FPS: " + minecraft.getFps() + " | Avg: " + averageFps + " | Sample: 1/20",
                String.format(Locale.ROOT, "Grid total: %.3f ms", averageGridMs),
                String.format(Locale.ROOT, "Items: %.3f ms (%.1f%%)", itemNanos / divisor, percentage(itemNanos / divisor, averageGridMs)),
                String.format(Locale.ROOT, "Decorations: %.3f ms (%.1f%%)", averageDecorationMs, percentage(averageDecorationMs, averageGridMs)),
                String.format(Locale.ROOT, "  Nameplate measured: %.3f ms | Other: %.3f ms", averageNameplateMs, averageDecorationOtherMs),
                String.format(Locale.ROOT, "  Rule: %.3f ms | Text: %.3f ms", ruleNanos / divisor, textNanos / divisor),
                String.format(Locale.ROOT, "  Layout: %.3f ms | Draw: %.3f ms", layoutNanos / divisor, drawNanos / divisor),
                String.format(Locale.ROOT, "Final flush: %.3f ms (%.1f%%)", finalFlushNanos / divisor, percentage(finalFlushNanos / divisor, averageGridMs))
        );
        int panelWidth = stats.stream().mapToInt(minecraft.font::width).max().orElse(0) + 12;
        guiGraphics.fill(3, 3, 3 + panelWidth, 9 + stats.size() * 10, 0xB0000000);
        for (int index = 0; index < stats.size(); index++) {
            guiGraphics.drawString(minecraft.font, stats.get(index), 6, 6 + index * 10, 0xFFFFFFFF, false);
        }
    }

    private static double percentage(double value, double total) {
        return total <= 0.0D ? 0.0D : value * 100.0D / total;
    }

    private enum RendererMode {
        LEGACY,
        OPTIMIZED,
        SHADER
    }
}
