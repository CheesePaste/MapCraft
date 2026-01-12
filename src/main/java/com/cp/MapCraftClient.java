package com.cp;

import net.fabricmc.api.ClientModInitializer;

public class MapCraftClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MapCraft.initializeClient();
    }
}