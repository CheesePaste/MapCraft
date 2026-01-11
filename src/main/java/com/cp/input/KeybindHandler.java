package com.cp.input;

import com.cp.gui.RecipeMapScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
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
    }

    public static void handleKeyInput() {
        while (openMapKeybind.wasPressed()) {
            MinecraftClient minecraftClient=MinecraftClient.getInstance();
            if (minecraftClient != null && minecraftClient.player != null) {
                minecraftClient.setScreen(new RecipeMapScreen());
            }
        }
    }
}