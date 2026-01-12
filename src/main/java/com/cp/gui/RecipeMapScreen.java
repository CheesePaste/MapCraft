package com.cp.gui;

import com.cp.MapCraft;
import com.cp.data.RecipeGraph;
import com.cp.data.RecipeNode;
import com.cp.data.RecipeEdge;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.Map;
import java.util.List;

import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

/**
 * 配方关系图主屏幕，按M键打开
 */
public class RecipeMapScreen extends Screen {
    // 图形数据
    private RecipeGraph recipeGraph;

    // 屏幕参数
    private static final int BACKGROUND_COLOR = 0xFF1A1A1A;
    private static final int GRID_COLOR = 0xFF2A2A2A;
    private static final int GRID_SIZE = 50;

    // 视图参数
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double scale = 1.0;

    // 交互状态
    private boolean isDragging = false;
    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;

    // 选中状态
    private RecipeNode selectedNode = null;

    public RecipeMapScreen() {
        super(Text.translatable("screen.mapcraft.title"));

        // 获取配方图数据
        try {
            this.recipeGraph = MapCraft.collector.getRecipeGraph();
            MapCraft.LOGGER.info("RecipeMapScreen初始化，加载了{}个节点，{}条边",
                    recipeGraph.getNodeCount(), recipeGraph.getEdgeCount());
        } catch (Exception e) {
            MapCraft.LOGGER.error("加载配方图失败: {}", e.getMessage());
            this.recipeGraph = null;
        }
    }

    @Override
    protected void init() {
        super.init();

        // 初始化视图居中
        if (recipeGraph != null && recipeGraph.getNodeCount() > 0) {
            // 简单居中所有节点（后续将由布局算法处理）
            offsetX = width / 2.0;
            offsetY = height / 2.0;
        }

        // 添加调试文本
        if (recipeGraph == null) {
            MapCraft.LOGGER.warn("RecipeGraph未加载，屏幕将显示错误信息");
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制背景和网格
        renderBackground(context);

        if (recipeGraph == null) {
            // 显示错误信息
            renderErrorMessage(context);
            return;
        }

        // 开始图形渲染
        renderGraph(context, mouseX, mouseY);

        // 绘制界面信息
        renderUIOverlay(context);

        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * 绘制背景和网格
     */
    private void renderBackground(DrawContext context) {
        // 纯色背景
        context.fill(0, 0, width, height, BACKGROUND_COLOR);

        // 绘制网格
        int gridStartX = (int) ((offsetX % GRID_SIZE) * scale);
        int gridStartY = (int) ((offsetY % GRID_SIZE) * scale);

        for (int x = gridStartX; x < width; x += GRID_SIZE * scale) {
            context.drawVerticalLine(x, 0, height, GRID_COLOR);
        }

        for (int y = gridStartY; y < height; y += GRID_SIZE * scale) {
            context.drawHorizontalLine(0, width, y, GRID_COLOR);
        }
    }

    /**
     * 绘制配方关系图
     */
    private void renderGraph(DrawContext context, int mouseX, int mouseY) {
        // 保存当前变换状态
        context.getMatrices().push();
        context.getMatrices().translate(offsetX, offsetY, 0);
        context.getMatrices().scale((float) scale, (float) scale, 1.0f);

        // 先绘制所有连线
        renderAllEdges(context);

        // 再绘制所有节点（节点在连线上方）
        renderAllNodes(context, mouseX, mouseY);

        // 恢复变换状态
        context.getMatrices().pop();
    }

    /**
     * 绘制所有连线
     */
    private void renderAllEdges(DrawContext context) {
        if (recipeGraph == null) return;

        for (RecipeEdge edge : recipeGraph.getEdges()) {
            RecipeNode fromNode = recipeGraph.getNodeById(edge.getFromRecipeId());
            RecipeNode toNode = recipeGraph.getNodeById(edge.getToRecipeId());

            if (fromNode != null && toNode != null) {
                renderEdge(context, fromNode, toNode, edge);
            }
        }
    }

    /**
     * 绘制单条连线
     */
    private void renderEdge(DrawContext context, RecipeNode from, RecipeNode to, RecipeEdge edge) {
        int color = edge.isHighlighted() ? 0xFFFFA500 : 0x80FFFFFF; // 高亮为橙色，正常为半透明白色

        // 计算屏幕坐标
        int x1 = (int) from.getX();
        int y1 = (int) from.getY();
        int x2 = (int) to.getX();
        int y2 = (int) to.getY();

        // 绘制连线
        drawLine(context, x1, y1, x2, y2, color, 2);

        // 绘制箭头（TODO: 后续优化）
        drawArrow(context, x1, y1, x2, y2, color);
    }

    /**
     * 绘制所有节点
     */
    private void renderAllNodes(DrawContext context, int mouseX, int mouseY) {
        if (recipeGraph == null) return;

        // 转换鼠标坐标到图坐标空间
        double graphMouseX = (mouseX - offsetX) / scale;
        double graphMouseY = (mouseY - offsetY) / scale;

        // 先绘制未选中的节点
        for (RecipeNode node : recipeGraph.getNodes().values()) {
            if (node != selectedNode) {
                renderNode(context, node, graphMouseX, graphMouseY);
            }
        }

        // 最后绘制选中的节点（在最上层）
        if (selectedNode != null) {
            renderNode(context, selectedNode, graphMouseX, graphMouseY);
        }
    }

    /**
     * 绘制单个节点
     */
    private void renderNode(DrawContext context, RecipeNode node, double mouseX, double mouseY) {
        int x = (int) node.getX();
        int y = (int) node.getY();
        int size = 16; // 节点大小

        // 检查鼠标是否悬停
        boolean isHovered = Math.abs(x - mouseX) < size && Math.abs(y - mouseY) < size;
        boolean isSelected = node == selectedNode;

        // 确定颜色
        int borderColor;
        if (isSelected) {
            borderColor = 0xFFFFFF00; // 选中：黄色
        } else if (isHovered) {
            borderColor = 0xFF00FFFF; // 悬停：青色
        } else if (node.isVisited()) {
            borderColor = 0xFF00FF00; // 已访问：绿色
        } else {
            borderColor = 0xFF666666; // 默认：灰色
        }

        int fillColor = 0xFF333333; // 填充色

        // 绘制节点外框
        int halfSize = size / 2;
        context.fill(x - halfSize, y - halfSize, x + halfSize, y + halfSize, fillColor);
        context.drawBorder(x - halfSize, y - halfSize, size, size, borderColor);

        // TODO: 后续在此处绘制物品图标

        // 绘制节点标签
        String label = node.getOutputItem().getName().getString();
        int textWidth = textRenderer.getWidth(label);
        int textX = x - textWidth / 2;
        int textY = y + halfSize + 2;

        context.drawTextWithShadow(textRenderer, label, textX, textY, 0xFFFFFFFF);

        // 如果节点被选中，显示详细信息
        if (isSelected) {
            renderNodeDetails(context, node, x, y);
        }
    }

    /**
     * 绘制节点详细信息
     */
    private void renderNodeDetails(DrawContext context, RecipeNode node, int nodeX, int nodeY) {
        // 详细信息面板
        int panelWidth = 150;
        int panelHeight = 100;
        int panelX = nodeX + 20;
        int panelY = nodeY - panelHeight / 2;

        // 确保面板在屏幕内
        if (panelX + panelWidth > width) {
            panelX = nodeX - panelWidth - 20;
        }
        if (panelY + panelHeight > height) {
            panelY = height - panelHeight - 10;
        }
        if (panelY < 10) {
            panelY = 10;
        }

        // 绘制面板背景
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC000000);
        context.drawBorder(panelX, panelY, panelWidth, panelHeight, 0xFFFFFFFF);

        // 绘制节点信息
        int textX = panelX + 5;
        int textY = panelY + 5;

        context.drawText(textRenderer, "配方: " + node.getRecipeId().getPath(), textX, textY, 0xFFFFFF, false);
        textY += 12;
        context.drawText(textRenderer, "输出: " + node.getOutputItem().getName().getString(), textX, textY, 0xFFFFFF, false);
        textY += 12;
        context.drawText(textRenderer, "数量: " + node.getOutputCount(), textX, textY, 0xFFFFFF, false);
        textY += 12;
        context.drawText(textRenderer, "输入数: " + node.getInputItems().size(), textX, textY, 0xFFFFFF, false);
    }

    /**
     * 绘制界面叠加信息
     */
    private void renderUIOverlay(DrawContext context) {
        // 绘制标题
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);

        // 绘制统计信息
        if (recipeGraph != null) {
            String stats = String.format("节点: %d | 连线: %d | 缩放: %.2fx",
                    recipeGraph.getNodeCount(), recipeGraph.getEdgeCount(), scale);
            context.drawTextWithShadow(textRenderer, stats, 10, 10, 0xFFFFFF);
        }

        // 绘制操作提示
        String controls = "WASD/鼠标拖动: 移动 | 鼠标滚轮: 缩放 | 点击节点: 选择 | R: 重置视图";
        context.drawTextWithShadow(textRenderer, controls, 10, height - 20, 0xAAAAAA);
    }

    /**
     * 绘制错误信息
     */
    private void renderErrorMessage(DrawContext context) {
        String error = "配方图数据加载失败";
        context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height / 2 - 10, 0xFF5555);

        String hint = "请确保已生成配方数据或检查日志";
        context.drawCenteredTextWithShadow(textRenderer, hint, width / 2, height / 2 + 10, 0xAAAAAA);
    }

    /**
     * 绘制直线
     */
    private void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color, int width) {
        // 简单直线绘制（TODO: 后续使用更精确的绘制方法）
        float angle = (float) Math.atan2(y2 - y1, x2 - x1);
        float length = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

        context.getMatrices().push();
        context.getMatrices().translate(x1, y1, 0);
        context.getMatrices().multiply(new Quaternionf().fromAxisAngleDeg(0, 0, 1, (float) Math.toDegrees(angle)));

        context.fill(0, -width/2, (int) length, width/2, color);

        context.getMatrices().pop();
    }

    /**
     * 绘制箭头
     */
    private void drawArrow(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        // 箭头绘制（TODO: 后续优化）
        float angle = (float) Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = 8;

        // 箭头位置（从终点往回一些）
        int arrowX = (int) (x2 - Math.cos(angle) * 10);
        int arrowY = (int) (y2 - Math.sin(angle) * 10);

        context.getMatrices().push();
        context.getMatrices().translate(arrowX, arrowY, 0);
        context.getMatrices().multiply(new Quaternionf().fromAxisAngleDeg(0, 0, 1, (float) Math.toDegrees(angle)));

        // 绘制箭头三角形
        int[] xPoints = {0, -arrowSize, -arrowSize};
        int[] yPoints = {0, -arrowSize/2, arrowSize/2};

        for (int i = 0; i < 3; i++) {
            int next = (i + 1) % 3;
            drawLine(context, xPoints[i], yPoints[i], xPoints[next], yPoints[next], color, 2);
        }

        context.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 转换鼠标坐标到图坐标空间
        double graphMouseX = (mouseX - offsetX) / scale;
        double graphMouseY = (mouseY - offsetY) / scale;

        // 检查是否点击了节点
        if (recipeGraph != null && button == 0) { // 左键
            selectedNode = null;

            for (RecipeNode node : recipeGraph.getNodes().values()) {
                int x = (int) node.getX();
                int y = (int) node.getY();
                int size = 16;

                if (Math.abs(x - graphMouseX) < size && Math.abs(y - graphMouseY) < size) {
                    selectedNode = node;
                    node.setVisited(true);

                    // 高亮相关连线
                    highlightRelatedEdges(node);
                    MapCraft.LOGGER.debug("选中节点: {}", node.getRecipeId());
                    return true;
                }
            }

            // 如果点击了空白区域，清除高亮
            clearEdgeHighlights();
        }

        // 开始拖动
        if (button == 0) {
            isDragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging) {
            offsetX += mouseX - lastMouseX;
            offsetY += mouseY - lastMouseY;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double zoomFactor = 1.1;
        double newScale = scale;

        if (verticalAmount > 0) {
            newScale *= zoomFactor;
        } else if (verticalAmount < 0) {
            newScale /= zoomFactor;
        }

        // 限制缩放范围
        newScale = Math.max(0.1, Math.min(newScale, 5.0));

        // 以鼠标位置为中心缩放
        if (newScale != scale) {
            double scaleChange = newScale / scale;
            offsetX = mouseX - (mouseX - offsetX) * scaleChange;
            offsetY = mouseY - (mouseY - offsetY) * scaleChange;
            scale = newScale;
        }

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 处理键盘控制
        double panSpeed = 20.0;

        switch (keyCode) {
            case GLFW.GLFW_KEY_W:
                offsetY += panSpeed;
                return true;
            case GLFW.GLFW_KEY_S:
                offsetY -= panSpeed;
                return true;
            case GLFW.GLFW_KEY_A:
                offsetX += panSpeed;
                return true;
            case GLFW.GLFW_KEY_D:
                offsetX -= panSpeed;
                return true;
            case GLFW.GLFW_KEY_R:
                // 重置视图
                offsetX = width / 2.0;
                offsetY = height / 2.0;
                scale = 1.0;
                selectedNode = null;
                clearEdgeHighlights();
                return true;
            case GLFW.GLFW_KEY_ESCAPE:
                // 关闭屏幕
                this.close();
                return true;
            case GLFW.GLFW_KEY_M:
                // M键也关闭屏幕（与打开键相同）
                if ((modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
                    this.close();
                    return true;
                }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 高亮与节点相关的连线
     */
    private void highlightRelatedEdges(RecipeNode node) {
        clearEdgeHighlights();

        if (recipeGraph == null) return;

        Identifier nodeId = node.getRecipeId();

        // 高亮从该节点出发的边
        for (RecipeEdge edge : recipeGraph.getEdgesFromNode(nodeId)) {
            edge.setHighlighted(true);
        }

        // 高亮指向该节点的边
        for (RecipeEdge edge : recipeGraph.getEdgesToNode(nodeId)) {
            edge.setHighlighted(true);
        }
    }

    /**
     * 清除所有连线的高亮
     */
    private void clearEdgeHighlights() {
        if (recipeGraph == null) return;

        for (RecipeEdge edge : recipeGraph.getEdges()) {
            edge.setHighlighted(false);
        }
    }

    @Override
    public void close() {
        super.close();
        MapCraft.LOGGER.debug("配方图屏幕关闭");
    }

    @Override
    public boolean shouldPause() {
        return false; // 游戏不需要暂停
    }
}