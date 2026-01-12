package com.graph;

import net.minecraft.registry.Registries;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class RecipeGraphDebugger {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void printGraphSummary(RecipeGraphData data) {
        if (data == null) {
            LOGGER.error("[RecipeGraph] 数据为空，无法打印信息！");
            return;
        }

        LOGGER.info("================  Recipe Graph Debug  ================");
        LOGGER.info("节点总数 (物品): {}", data.nodes.size());
        LOGGER.info("连线总数 (合成): {}", data.edges.size());

        // 1. 计算图表的整体边界（用于判断布局范围是否合理）
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

        for (RecipeGraphData.Node node : data.nodes) {
            minX = Math.min(minX, node.x);
            minY = Math.min(minY, node.y);
            maxX = Math.max(maxX, node.x + node.width);
            maxY = Math.max(maxY, node.y + node.height);
        }

        LOGGER.info("画布边界: X轴[{}, {}], Y轴[{}, {}]", (int)minX, (int)maxX, (int)minY, (int)maxY);
        LOGGER.info("建议画布尺寸: {} x {}", (int)(maxX - minX), (int)(maxY - minY));

        // 2. 打印前 10 个节点的信息作为样例
        LOGGER.info("--- 节点样例 (前10个) ---");
        int nodeCount = 0;
        for (RecipeGraphData.Node node : data.nodes) {
            if (nodeCount++ >= 10) break;
            String itemId = Registries.ITEM.getId(node.item).toString();
            LOGGER.info("Item: {} -> 坐标: ({}, {})", itemId, (int)node.x, (int)node.y);
        }

        // 3. 打印前 10 条边的信息作为样例
        LOGGER.info("--- 连线样例 (前10个) ---");
        int edgeCount = 0;
        for (RecipeGraphData.Edge edge : data.edges) {
            if (edgeCount++ >= 10) break;
            String srcId = Registries.ITEM.getId(edge.source).toString();
            String tgtId = Registries.ITEM.getId(edge.target).toString();
            LOGGER.info("合成关系: {} ===> {}", srcId, tgtId);
        }

        LOGGER.info("====================================================");
    }
}
