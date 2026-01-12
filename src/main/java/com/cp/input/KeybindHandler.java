package com.cp.input;

import com.cp.MapCraft;
import com.cp.data.RecipeGraph;
import com.cp.gui.RecipeMapScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {
    public static KeyBinding openMapKeybind;

    public static void registerKeybinds() {
        openMapKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.map-craft.open_map",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.map-craft.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMapKeybind.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    // 打开配方图屏幕
                    RecipeGraph graph = MapCraft.collector.getRecipeGraph();
                    client.setScreen(new RecipeMapScreen(Text.of("gui.map-craft.title"), graph));
                }
            }
        });
    }
}