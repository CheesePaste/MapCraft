package com.cp.gui;

import com.cp.data.RecipeEdge;
import com.cp.data.RecipeGraph;
import com.cp.data.RecipeNode;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class RecipeMapRenderer {
    public static void render(DrawContext context, RecipeGraph graph, InteractionHandler handler, int mouseX, int mouseY) {
        context.getMatrices().push();
        // 应用平移和缩放
        context.getMatrices().translate(context.getScaledWindowWidth() / 2f + handler.offsetX,
                context.getScaledWindowHeight() / 2f + handler.offsetY, 0);
        context.getMatrices().scale(handler.zoom, handler.zoom, 1.0f);

        // 1. 绘制边 (线段) - 使用更精确的坐标
        for (RecipeEdge edge : graph.getEdges()) {
            RecipeNode from = graph.getNodeById(edge.getFromRecipeId());
            RecipeNode to = graph.getNodeById(edge.getToRecipeId());
            if (from != null && to != null) {
                float x1 = (float) from.getX() + 8;  // 节点中心偏移
                float y1 = (float) from.getY() + 8;
                float x2 = (float) to.getX() + 8;
                float y2 = (float) to.getY() + 8;

                // 使用 drawLineWithThickness 或分段绘制
                drawThickLine(context, x1, y1, x2, y2, 0xFF555555, 2.0f);
            }
        }

        // 2. 绘制节点 (物品)
        MinecraftClient client = MinecraftClient.getInstance();
        for (RecipeNode node : graph.getNodes().values()) {
            ItemStack stack = new ItemStack(node.getOutputItem());
            float x = (float) node.getX();
            float y = (float) node.getY();

            // 绘制物品图标
            context.drawItem(stack, (int)x, (int)y);

            // 如果鼠标悬停，绘制高亮
            if (isMouseOver(x, y, mouseX, mouseY, handler, context)) {
                context.fill((int)x, (int)y, (int)x + 16, (int)y + 16, 0x44FFFFFF);
            }
        }

        context.getMatrices().pop();
    }

    /**
     * 绘制带宽度的线条（兼容新旧版本）
     */
    private static void drawThickLine(DrawContext context, float x1, float y1, float x2, float y2, int color, float thickness) {
        // 使用 2D 渲染器绘制线条
        drawCubicBezier(context, x1, y1, x2, y2, color);
    }
    private static void drawCubicBezier(DrawContext context, float x1, float y1, float x2, float y2, int color) {
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        // 控制点逻辑：垂直方向拉伸，使曲线在纵向更平滑
        float ctrlYOffset = Math.abs(y2 - y1) * 0.5f;
        float ctrlY1 = y1 + ctrlYOffset;
        float ctrlY2 = y2 - ctrlYOffset;

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();

        int segments = 30;
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float invT = 1 - t;

            // 三次贝塞尔曲线公式
            float bX = invT * invT * invT * x1 + 3 * invT * invT * t * x1 + 3 * invT * t * t * x2 + t * t * t * x2;
            float bY = invT * invT * invT * y1 + 3 * invT * invT * t * ctrlY1 + 3 * invT * t * t * ctrlY2 + t * t * t * y2;

            bufferBuilder.vertex(matrix, bX, bY, 0).color(color);
        }

        BuiltBuffer builtBuffer = bufferBuilder.end();
        if (builtBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtBuffer);
        }
    }

    private static boolean isMouseOver(float x, float y, int mx, int my, InteractionHandler h, DrawContext ctx) {
        // 简单的逆向坐标变换检查
        double screenX = (x * h.zoom) + (ctx.getScaledWindowWidth() / 2f) + h.offsetX;
        double screenY = (y * h.zoom) + (ctx.getScaledWindowHeight() / 2f) + h.offsetY;
        return mx >= screenX && mx <= screenX + (16 * h.zoom) && my >= screenY && my <= screenY + (16 * h.zoom);
    }
}
