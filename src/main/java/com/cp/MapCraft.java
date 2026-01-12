package com.cp;

import com.cp.data.RecipeCollector;
import com.cp.data.RecipeGraph;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapCraft implements ModInitializer {
    public static final String MOD_ID = "map-craft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static RecipeCollector collector = new RecipeCollector();


    @Override
    public void onInitialize() {
        LOGGER.info("MapCraft Mod 初始化中...");

        // 初始化配置
        // com.cp.config.ModConfig.init();

        // 如果是开发环境，启用测试模块
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            LOGGER.info("开发环境检测到，启用测试模块");
            MapCraftGraphTest testModule = new MapCraftGraphTest(collector);
            testModule.onInitialize();
        }

        // 异步初始化配方图数据
        initializeRecipeGraphAsync();

        LOGGER.info("MapCraft Mod 初始化完成");
    }

    /**
     * 客户端初始化（在ClientModInitializer中调用）
     */
    public static void initializeClient() {
        LOGGER.info("初始化MapCraft客户端...");
    }

    /**
     * 异步初始化配方图数据
     */
    private static void initializeRecipeGraphAsync() {
        new Thread(() -> {
            try {
                LOGGER.info("开始异步收集配方数据...");
                RecipeGraph recipeGraph = collector.getRecipeGraph();
                if (recipeGraph != null) {
                    LOGGER.info("配方图数据收集完成: {}个节点, {}条边",
                            recipeGraph.getNodeCount(), recipeGraph.getEdgeCount());

                    // 检查图结构
                    if (recipeGraph.hasCycles()) {
                        LOGGER.warn("配方图检测到循环依赖");
                    }
                } else {
                    LOGGER.error("配方图数据收集失败，返回null");
                }
            } catch (Exception e) {
                LOGGER.error("配方数据收集失败: {}", e.getMessage(), e);
            }
        }, "MapCraft-RecipeCollector").start();
    }




    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}