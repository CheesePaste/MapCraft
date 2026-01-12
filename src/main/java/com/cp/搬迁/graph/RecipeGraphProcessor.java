package com.graph;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.Registries;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class RecipeGraphProcessor {

    public static RecipeGraphData buildLogicGraph() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;

        RecipeManager recipeManager = client.world.getRecipeManager();
        RecipeGraphData resultData = new RecipeGraphData();

        // 1. 初始化 JGraphX 图结构
        mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();

        // 用于追踪 Item 与 JGraphX 节点的对应关系
        Map<Item, Object> itemToVertex = new HashMap<>();

        try {
            // 2. 遍历所有合成表
            for (RecipeEntry<?> entry : recipeManager.values()) {
                // 获取产出物品
                ItemStack resultStack = entry.value().getResult(client.world.getRegistryManager());
                Item resultItem = resultStack.getItem();

                // 排除空气或无效物品
                if (resultStack.isEmpty()) continue;

                Object resultVertex = getOrCreateVertex(graph, itemToVertex, resultItem);

                // 获取所有输入原料
                for (Ingredient ingredient : entry.value().getIngredients()) {
                    // 一个 Ingredient 可能包含多种匹配物品（如所有木板）
                    // 为了简化，我们只取第一个匹配项，或者你可以遍历所有
                    ItemStack[] matchingStacks = ingredient.getMatchingStacks();
                    if (matchingStacks.length > 0) {
                        Item inputItem = matchingStacks[0].getItem();
                        Object inputVertex = getOrCreateVertex(graph, itemToVertex, inputItem);

                        // 建立边：原料 -> 产出
                        graph.insertEdge(parent, null, "", inputVertex, resultVertex);
                        resultData.edges.add(new RecipeGraphData.Edge(inputItem, resultItem));
                    }
                }
            }

            // 3. 调用布局算法 (Hierarchical 是最适合合成表的，呈树状分层)
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
            layout.setOrientation(SwingConstants.NORTH); // 从上往下排
            layout.setInterHierarchySpacing(50); // 层间距
            layout.setInterRankCellSpacing(30);   // 节点间距
            layout.execute(parent);

            // 4. 将计算后的坐标提取到我们的数据结构中
            for (Map.Entry<Item, Object> entry : itemToVertex.entrySet()) {
                mxCell cell = (mxCell) entry.getValue();
                resultData.nodes.add(new RecipeGraphData.Node(
                        entry.getKey(),
                        cell.getGeometry().getX(),
                        cell.getGeometry().getY(),
                        cell.getGeometry().getWidth(),
                        cell.getGeometry().getHeight()
                ));
            }

        } finally {
            graph.getModel().endUpdate();
        }

        return resultData;
    }

    private static Object getOrCreateVertex(mxGraph graph, Map<Item, Object> map, Item item) {
        if (!map.containsKey(item)) {
            // 默认给每个节点 32x32 的大小（适合 MC 图标渲染）
            String label = Registries.ITEM.getId(item).getPath();
            Object vertex = graph.insertVertex(graph.getDefaultParent(), null, label, 0, 0, 32, 32);
            map.put(item, vertex);
        }
        return map.get(item);
    }
}
