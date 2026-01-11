package com.cp.gui;

import com.cp.input.MouseHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class RecipeMapScreen extends Screen {
    private final RecipeMapRenderer renderer;
    private float zoom = 1.0f;
    private float offsetX = 0;
    private float offsetY = 0;

    public RecipeMapScreen() {
        super(Text.translatable("screen.map-craft.recipe_map"));
        this.renderer = new RecipeMapRenderer();
    }

    @Override
    protected void init() {
        // 初始化渲染器
        renderer.init(width, height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染背景
        this.renderBackground(context);

        // 渲染配方图
        renderer.render(context, mouseX, mouseY, delta, zoom, offsetX, offsetY);

        // 渲染UI元素
        renderUI(context, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 处理按键事件
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 处理滚轮缩放
        zoom = Math.max(0.1f, Math.min(5.0f, zoom + (float)verticalAmount * 0.1f));
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // 处理拖拽平移
        if (button == 0) { // 左键拖拽
            offsetX += deltaX;
            offsetY += deltaY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}