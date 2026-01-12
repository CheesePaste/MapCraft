// RecipeCollector.java - 重构后的主收集器类
package com.cp.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.*;

public class RecipeCollector {
    private final RecipeDataManager dataManager = new RecipeDataManager();
    private final RecipeProcessor processor;
    private final GraphBuilder graphBuilder;

    public RecipeCollector() {
        this.processor = new RecipeProcessor(dataManager);
        this.graphBuilder = new GraphBuilder(dataManager);
    }

    public void collectAllRecipes(MinecraftServer server) {
        dataManager.clear();
        processor.processAllRecipes(server);
        graphBuilder.buildGraph();
    }

    public RecipeGraph getRecipeGraph() {
        return dataManager.createRecipeGraph();
    }

    public void printGraphSummary() {
        RecipeGraph graph = dataManager.createRecipeGraph();
        System.out.println("=== 配方关系图摘要 ===");
        System.out.println("总节点数: " + graph.getNodeCount());
        System.out.println("总边数: " + graph.getEdgeCount());

        // 统计边类型
        Map<String, Integer> edgeTypeCount = new HashMap<>();
        for (RecipeEdge edge : graph.getEdges()) {
            String type = edge.getRelationshipType();
            edgeTypeCount.put(type, edgeTypeCount.getOrDefault(type, 0) + 1);
        }

        System.out.println("边类型分布:");
        for (Map.Entry<String, Integer> entry : edgeTypeCount.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        // 检查循环
        if (graph.hasCycles()) {
            System.out.println("警告: 图中存在循环依赖");
        } else {
            System.out.println("良好: 没有发现循环依赖");
        }
    }
}