package com.graph;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;


@Environment(EnvType.CLIENT)
public class RecipeGrapherClient implements ClientModInitializer {

    // 定义 KeyBinding
    private static KeyBinding debugRecipeGraphKey;

    @Override
    public void onInitializeClient() {
        // 1. 注册按键绑定
        debugRecipeGraphKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.recipegrapher.debug",      // 按键ID，用于翻译文件
                InputUtil.Type.KEYSYM,          // 按键类型 (KEYSYM=键盘，MOUSE=鼠标)
                GLFW.GLFW_KEY_R,                // 按键代码 (R键)
                "category.recipegrapher.keys"   // 分类ID
        ));

        // 2. 注册客户端tick事件监听
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 每次客户端tick结束时检查按键
            if (debugRecipeGraphKey.wasPressed()) {
                // 确保在游戏世界中（避免主菜单时触发）
                if (client.player != null && client.world != null) {
                    executeRecipeGraphDebug();
                }
            }
        });
    }

    private void executeRecipeGraphDebug() {
        // 记录开始时间（用于性能测试）
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("=== 开始构建合成图 ===");

            // 执行你的代码
            // 1. 先计算数据 (建议在异步线程，或者在世界加载时预算)
            RecipeGraphData data = RecipeGraphProcessor.buildLogicGraph();

// 2. 打开界面
            MinecraftClient.getInstance().setScreen(new RecipeGraphScreen(data));


            // 计算耗时
            long endTime = System.currentTimeMillis();
            System.out.println("=== 合成图构建完成，耗时 " + (endTime - startTime) + "ms ===");

        } catch (Exception e) {
            System.err.println("合成图构建失败：");
            e.printStackTrace();
        }
    }
}