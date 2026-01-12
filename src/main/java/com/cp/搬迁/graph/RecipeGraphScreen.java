package com.graph;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;

public class RecipeGraphScreen extends Screen {
    private final RecipeGraphData graphData;

    private RecipeGraphData.Node selectedNode = null;
    private final Set<RecipeGraphData.Node> neighbors = new HashSet<>();

    private double zoom = 0.8;
    private double offsetX = 0;
    private double offsetY = 0;
    private double temperature = 1.0;
    private final java.util.Map<RecipeGraphData.Node, double[]> velocities = new java.util.HashMap<>();
    private boolean initialized = false;

    public RecipeGraphScreen(RecipeGraphData data) {
        super(Text.literal("Recipe Graph"));
        this.graphData = data;
    }

    @Override
    protected void init() {
        if (!initialized) {
            initializePositions();
            // 初始预热
            for (int i = 0; i < 150; i++) simulatePhysicsStep();
            initialized = true;
        }
    }

    private void initializePositions() {
        java.util.Random random = new java.util.Random();
        for (RecipeGraphData.Node node : graphData.nodes) {
            node.x = (random.nextDouble() - 0.5) * 200;
            node.y = (random.nextDouble() - 0.5) * 200;
            velocities.put(node, new double[]{0, 0});
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // 持续模拟物理，直到稳定
        if (temperature > 0.005) {
            simulatePhysicsStep();
        }

        // 平滑摄像机跟随 (可选：取消注释则点击节点时摄像机自动中心对齐)
        /*
        if (selectedNode != null) {
            double targetX = -selectedNode.x * zoom;
            double targetY = -selectedNode.y * zoom;
            offsetX += (targetX - offsetX) * 0.1;
            offsetY += (targetY - offsetY) * 0.1;
        }
        */

        context.getMatrices().push();
        // 渲染中心点：屏幕中点 + 偏移量
        context.getMatrices().translate(this.width / 2.0f + (float)offsetX, this.height / 2.0f + (float)offsetY, 0);
        context.getMatrices().scale((float) zoom, (float) zoom, 1.0f);

        renderEdges(context);
        renderNodes(context);

        context.getMatrices().pop();
        renderTooltips(context, mouseX, mouseY);
    }

//    private void renderEdges(DrawContext context) {
//        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
//
//        for (RecipeGraphData.Edge edge : graphData.edges) {
//            RecipeGraphData.Node src = findNode(edge.source);
//            RecipeGraphData.Node dst = findNode(edge.target);
//            if (src == null || dst == null) continue;
//
//            boolean isHighlighted = (selectedNode != null && (src == selectedNode || dst == selectedNode));
//            int alpha = (selectedNode == null) ? 0x44 : (isHighlighted ? 0xFF : 0x11);
//            int color = (alpha << 24) | (isHighlighted ? 0xFFAA00 : 0xFFFFFF);
//
//            // 连线到物体中心 (16,16 是因为图标大小32x32)
//            bufferBuilder.vertex(matrix, (float) src.x + 16, (float) src.y + 16, 0).color(color);
//            bufferBuilder.vertex(matrix, (float) dst.x + 16, (float) dst.y + 16, 0).color(color);
//        }
//        RenderSystem.enableBlend();
//        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
//        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
//    }
private void renderEdges(DrawContext context) {
    Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

    for (RecipeGraphData.Edge edge : graphData.edges) {
        RecipeGraphData.Node src = findNode(edge.source);
        RecipeGraphData.Node dst = findNode(edge.target);
        if (src == null || dst == null) continue;

        boolean isHighlighted = (selectedNode != null && (src == selectedNode || dst == selectedNode));
        int alpha = (selectedNode == null) ? 0x44 : (isHighlighted ? 0xFF : 0x11);
        int color = (alpha << 24) | (isHighlighted ? 0xFFAA00 : 0xFFFFFF);

        // --- 修改开始 ---

        // 1. 计算中心点坐标
        float srcCenterX = (float) src.x + 16;
        float srcCenterY = (float) src.y + 16;
        float dstCenterX = (float) dst.x + 16;
        float dstCenterY = (float) dst.y + 16;

        // 2. 计算距离和角度
        double dx = dstCenterX - srcCenterX;
        double dy = dstCenterY - srcCenterY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        // 只有当节点距离足够远时才绘制箭头（防止重叠时乱闪）
        if (dist > 32.0) {
            double angle = Math.atan2(dy, dx);

            // 3. 计算连线的起止点（向内收缩，留出图标半径，假设半径为18）
            float radius = 18.0f;
            float startX = (float)(srcCenterX + Math.cos(angle) * radius);
            float startY = (float)(srcCenterY + Math.sin(angle) * radius);
            float endX = (float)(dstCenterX - Math.cos(angle) * radius);
            float endY = (float)(dstCenterY - Math.sin(angle) * radius);

            // 绘制主线
            bufferBuilder.vertex(matrix, startX, startY, 0).color(color);
            bufferBuilder.vertex(matrix, endX, endY, 0).color(color);

            // 4. 绘制箭头 (两条短斜线)
            double arrowSize = 6.0; // 箭头大小
            double arrowAngle = Math.PI / 6; // 箭头张开角度 (30度)

            // 左翼
            float wing1X = (float)(endX - Math.cos(angle - arrowAngle) * arrowSize);
            float wing1Y = (float)(endY - Math.sin(angle - arrowAngle) * arrowSize);
            // 右翼
            float wing2X = (float)(endX - Math.cos(angle + arrowAngle) * arrowSize);
            float wing2Y = (float)(endY - Math.sin(angle + arrowAngle) * arrowSize);

            // 绘制箭头左半边
            bufferBuilder.vertex(matrix, endX, endY, 0).color(color);
            bufferBuilder.vertex(matrix, wing1X, wing1Y, 0).color(color);

            // 绘制箭头右半边
            bufferBuilder.vertex(matrix, endX, endY, 0).color(color);
            bufferBuilder.vertex(matrix, wing2X, wing2Y, 0).color(color);
        }
        // --- 修改结束 ---
    }
    RenderSystem.enableBlend();
    RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
}

    private void renderNodes(DrawContext context) {
        for (RecipeGraphData.Node node : graphData.nodes) {
            boolean isFocus = (selectedNode == null) || (node == selectedNode || neighbors.contains(node));
            float alpha = isFocus ? 1.0f : 0.15f;

            if (node == selectedNode) {
                // 绘制选中发光背景
                context.fill((int)node.x - 2, (int)node.y - 2, (int)node.x + 34, (int)node.y + 34, 0x55FFAA00);
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            context.drawItem(new ItemStack(node.item), (int)node.x + 8, (int)node.y + 8);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void simulatePhysicsStep() {
        double k = 60.0; // 理想间距

        // 1. 计算排斥力 (所有节点之间)
        for (RecipeGraphData.Node v : graphData.nodes) {
            double[] velV = velocities.get(v);
            for (RecipeGraphData.Node u : graphData.nodes) {
                if (v == u) continue;
                double dx = v.x - u.x;
                double dy = v.y - u.y;
                double d = Math.sqrt(dx * dx + dy * dy) + 0.01;
                double f = (k * k) / d;
                velV[0] += (dx / d) * f;
                velV[1] += (dy / d) * f;
            }
        }

        // 2. 计算引力 (边连接的节点)
        for (RecipeGraphData.Edge e : graphData.edges) {
            RecipeGraphData.Node v = findNode(e.source);
            RecipeGraphData.Node u = findNode(e.target);
            if (v == null || u == null) continue;

            double dx = v.x - u.x;
            double dy = v.y - u.y;
            double d = Math.sqrt(dx * dx + dy * dy) + 0.01;

            // 如果其中一个是选中节点，增加引力系数 (线力度加大)

            double strengthMultiplier = (v == selectedNode || u == selectedNode) ? 2.5 : 0.5;
            double f = (d * d) / (k * strengthMultiplier);

            double vx = (dx / d) * f;
            double vy = (dy / d) * f;
            velocities.get(v)[0] -= vx;
            velocities.get(v)[1] -= vy;
            velocities.get(u)[0] += vx;
            velocities.get(u)[1] += vy;
        }

        // 3. 更新位置
        for (RecipeGraphData.Node n : graphData.nodes) {
            double[] v = velocities.get(n);

            // 向心力：让整体趋向原点
            double centeringStrength = (n == selectedNode) ? 0.15 : 0.02;
            v[0] -= n.x * centeringStrength;
            v[1] -= n.y * centeringStrength;

            double speed = Math.sqrt(v[0] * v[0] + v[1] * v[1]) + 0.01;
            double capped = Math.min(speed, temperature * 100.0);

            n.x += (v[0] / speed) * capped;
            n.y += (v[1] / speed) * capped;

            // 阻尼
            v[0] *= 0.5;
            v[1] *= 0.5;
        }

        temperature *= 0.98; // 冷却
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double zoomFactor = (vertical > 0) ? 1.1 : 0.9;
        double nextZoom = MathHelper.clamp(zoom * zoomFactor, 0.05, 5.0);

        // --- 以鼠标为中心缩放的核心逻辑 ---
        // 1. 计算鼠标在当前缩放下的世界坐标 (相对于中心偏移)
        double mouseWorldX = (mouseX - (this.width / 2.0) - offsetX) / zoom;
        double mouseWorldY = (mouseY - (this.height / 2.0) - offsetY) / zoom;

        // 2. 更新缩放倍率
        this.zoom = nextZoom;

        // 3. 重新计算偏移量，使得鼠标指向的世界坐标在缩放后依然在鼠标位置
        this.offsetX = (mouseX - (this.width / 2.0)) - (mouseWorldX * zoom);
        this.offsetY = (mouseY - (this.height / 2.0)) - (mouseWorldY * zoom);

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double worldX = (mouseX - (this.width / 2.0) - offsetX) / zoom;
        double worldY = (mouseY - (this.height / 2.0) - offsetY) / zoom;

        RecipeGraphData.Node clicked = null;
        for (RecipeGraphData.Node node : graphData.nodes) {
            // 判定范围稍微扩大一点方便点击
            if (worldX >= node.x && worldX <= node.x + 32 && worldY >= node.y && worldY <= node.y + 32) {
                clicked = node;
                break;
            }
        }

        if (clicked != null) {
            this.selectedNode = clicked;
            this.neighbors.clear();
            for (RecipeGraphData.Edge edge : graphData.edges) {
                if (edge.source == clicked.item) neighbors.add(findNode(edge.target));
                else if (edge.target == clicked.item) neighbors.add(findNode(edge.source));
            }
            // 激活物理效果：重置温度让节点重新抖动排列
            this.temperature = 0.5;
        } else {
            selectedNode = null;
            neighbors.clear();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        this.offsetX += deltaX;
        this.offsetY += deltaY;
        return true;
    }

    private RecipeGraphData.Node findNode(net.minecraft.item.Item item) {
        for (RecipeGraphData.Node node : graphData.nodes) {
            if (node.item == item) return node;
        }
        return null;
    }

    private void renderTooltips(DrawContext context, int mouseX, int mouseY) {
        double worldX = (mouseX - (this.width / 2.0) - offsetX) / zoom;
        double worldY = (mouseY - (this.height / 2.0) - offsetY) / zoom;

        for (RecipeGraphData.Node node : graphData.nodes) {
            if (worldX >= node.x && worldX <= node.x + 32 && worldY >= node.y && worldY <= node.y + 32) {
                if (selectedNode != null && node != selectedNode && !neighbors.contains(node)) continue;
                context.drawItemTooltip(this.textRenderer, new ItemStack(node.item), mouseX, mouseY);
                break;
            }
        }
    }

    @Override public boolean shouldPause() { return false; }
}
