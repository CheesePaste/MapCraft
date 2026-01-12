package com.cp.gui;

import com.cp.MapCraft;
import com.cp.data.RecipeGraph;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class RecipeMapScreen extends Screen {
    private final RecipeGraph graph;
    private final GraphLayout layout;
    private final InteractionHandler interactionHandler;
    private boolean isDragging = false;

    public RecipeMapScreen(Text title, RecipeGraph graph) {
        super(title);
        this.graph = graph;
        this.layout = new GraphLayout(graph);
        this.interactionHandler = new InteractionHandler();
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. 物理模拟步进 (可以根据需要限制执行次数)
        layout.step();

        // 2. 渲染背景
        this.renderBackground(context, mouseX, mouseY, delta);

        // 3. 渲染图表
        RecipeMapRenderer.render(context, graph, interactionHandler, mouseX, mouseY);

        // 4. 渲染UI层
        context.drawTextWithShadow(this.textRenderer, "节点数: " + graph.getNodeCount(), 10, 10, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "滚轮缩放, 左键拖拽", 10, 20, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) { // 左键拖拽
            interactionHandler.onDrag(deltaX, deltaY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        interactionHandler.onMouseScroll(verticalAmount);
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
