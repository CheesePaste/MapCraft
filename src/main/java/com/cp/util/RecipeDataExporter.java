package com.cp.util;

import com.cp.data.RecipeGraph;
import com.cp.data.RecipeNode;
import com.cp.data.RecipeEdge;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 配方数据导出工具类
 * 负责将配方关系图保存到文件中
 */
public class RecipeDataExporter {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    /**
     * 将配方关系图导出到文件
     * @param graph 配方关系图
     * @param exportDir 导出目录
     * @param prefix 文件名前缀
     * @return 导出文件的路径
     */
    public static String exportRecipeGraphToJson(RecipeGraph graph, String exportDir, String prefix) {
        try {
            // 创建导出目录
            Path exportPath = Paths.get(exportDir);
            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
            }

            // 生成文件名
            String timestamp = DATE_FORMAT.format(new Date());
            String fileName = String.format("%s_recipe_graph_%s.json",
                    prefix, timestamp);

            Path filePath = exportPath.resolve(fileName);

            // 构建JSON数据结构
            JsonObject rootJson = buildRecipeGraphJson(graph);

            // 写入文件
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                GSON.toJson(rootJson, writer);
                writer.flush();
            }

            // 同时生成一个简化的统计文件
            exportGraphStatistics(graph, exportPath, prefix + "_statistics_" + timestamp + ".txt");

            return filePath.toAbsolutePath().toString();

        } catch (IOException e) {
            System.err.println("导出配方数据失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 构建配方关系图的JSON结构
     */
    private static JsonObject buildRecipeGraphJson(RecipeGraph graph) {
        JsonObject root = new JsonObject();

        // 元数据
        root.addProperty("version", "1.0");
        root.addProperty("exportTime", new Date().toString());
        root.addProperty("nodeCount", graph.getNodeCount());
        root.addProperty("edgeCount", graph.getEdgeCount());
        root.addProperty("hasCycles", graph.hasCycles());
        root.addProperty("buildTimestamp", graph.getBuildTimestamp());

        // 节点列表
        JsonArray nodesArray = new JsonArray();
        for (RecipeNode node : graph.getNodes().values()) {
            JsonObject nodeJson = buildNodeJson(node);
            nodesArray.add(nodeJson);
        }
        root.add("nodes", nodesArray);

        // 边列表
        JsonArray edgesArray = new JsonArray();
        for (RecipeEdge edge : graph.getEdges()) {
            JsonObject edgeJson = buildEdgeJson(edge);
            edgesArray.add(edgeJson);
        }
        root.add("edges", edgesArray);

        // 索引数据（便于快速查找）
        JsonObject indicesJson = buildIndicesJson(graph);
        root.add("indices", indicesJson);

        return root;
    }

    /**
     * 构建节点JSON
     */
    private static JsonObject buildNodeJson(RecipeNode node) {
        JsonObject nodeJson = new JsonObject();

        nodeJson.addProperty("recipeId", node.getRecipeId().toString());
        nodeJson.addProperty("recipeType", node.getRecipe().getType().toString());

        // 输出物品信息
        JsonObject outputJson = new JsonObject();
        outputJson.addProperty("itemId", Registries.ITEM.getId(node.getOutputItem()).toString());
        outputJson.addProperty("itemName", node.getOutputItem().getName().getString());
        outputJson.addProperty("count", node.getOutputCount());
        nodeJson.add("output", outputJson);

        // 输入物品列表
        JsonArray inputsArray = new JsonArray();
        for (net.minecraft.item.Item inputItem : node.getInputItems()) {
            JsonObject inputJson = new JsonObject();
            inputJson.addProperty("itemId", Registries.ITEM.getId(inputItem).toString());
            inputJson.addProperty("itemName", inputItem.getName().getString());
            inputsArray.add(inputJson);
        }
        nodeJson.add("inputs", inputsArray);

        nodeJson.addProperty("inputCount", node.getInputItems().size());

        return nodeJson;
    }

    /**
     * 构建边JSON
     */
    private static JsonObject buildEdgeJson(RecipeEdge edge) {
        JsonObject edgeJson = new JsonObject();

        edgeJson.addProperty("fromRecipeId", edge.getFromRecipeId().toString());
        edgeJson.addProperty("toRecipeId", edge.getToRecipeId().toString());
        edgeJson.addProperty("relationshipType", edge.getRelationshipType());
        edgeJson.addProperty("weight", edge.getWeight());

        return edgeJson;
    }

    /**
     * 构建索引JSON（便于快速查找）
     */
    private static JsonObject buildIndicesJson(RecipeGraph graph) {
        JsonObject indices = new JsonObject();

        // 按输出物品索引
        JsonObject outputToRecipes = new JsonObject();
        Map<String, List<String>> outputMap = new HashMap<>();

        for (RecipeNode node : graph.getNodes().values()) {
            String itemId = Registries.ITEM.getId(node.getOutputItem()).toString();
            List<String> recipes = outputMap.getOrDefault(itemId, new ArrayList<>());
            recipes.add(node.getRecipeId().toString());
            outputMap.put(itemId, recipes);
        }

        for (Map.Entry<String, List<String>> entry : outputMap.entrySet()) {
            JsonArray recipeArray = new JsonArray();
            for (String recipeId : entry.getValue()) {
                recipeArray.add(recipeId);
            }
            outputToRecipes.add(entry.getKey(), recipeArray);
        }
        indices.add("outputToRecipes", outputToRecipes);

        return indices;
    }

    /**
     * 导出图统计信息到文本文件
     */
    private static void exportGraphStatistics(RecipeGraph graph, Path exportPath, String fileName) throws IOException {
        Path statsPath = exportPath.resolve(fileName);

        StringBuilder stats = new StringBuilder();
        stats.append("=== 配方关系图统计信息 ===\n\n");

        stats.append("基本统计:\n");
        stats.append(String.format("  总节点数: %d\n", graph.getNodeCount()));
        stats.append(String.format("  总边数: %d\n", graph.getEdgeCount()));
        stats.append(String.format("  构建时间: %s\n", new Date(graph.getBuildTimestamp())));
        stats.append(String.format("  是否存在循环依赖: %s\n\n", graph.hasCycles() ? "是" : "否"));

        // 边类型分布
        stats.append("边类型分布:\n");
        Map<String, Integer> edgeTypeCount = new HashMap<>();
        for (RecipeEdge edge : graph.getEdges()) {
            String type = edge.getRelationshipType();
            edgeTypeCount.put(type, edgeTypeCount.getOrDefault(type, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : edgeTypeCount.entrySet()) {
            stats.append(String.format("  %s: %d (%.1f%%)\n",
                    entry.getKey(),
                    entry.getValue(),
                    (entry.getValue() * 100.0) / graph.getEdgeCount()));
        }

        stats.append("\n\n");

        // 最常见的输出物品
        stats.append("最常见的输出物品（前20）:\n");
        Map<String, Integer> outputItemCount = new HashMap<>();
        for (RecipeNode node : graph.getNodes().values()) {
            String itemId = Registries.ITEM.getId(node.getOutputItem()).toString();
            outputItemCount.put(itemId, outputItemCount.getOrDefault(itemId, 0) + 1);
        }

        List<Map.Entry<String, Integer>> sortedItems = new ArrayList<>(outputItemCount.entrySet());
        sortedItems.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (int i = 0; i < Math.min(20, sortedItems.size()); i++) {
            Map.Entry<String, Integer> entry = sortedItems.get(i);
            stats.append(String.format("  %d. %s: %d 个配方\n",
                    i + 1, entry.getKey(), entry.getValue()));
        }

        // 写入文件
        try (FileWriter writer = new FileWriter(statsPath.toFile())) {
            writer.write(stats.toString());
            writer.flush();
        }
    }

    /**
     * 导出特定物品的详细配方信息
     */
    public static String exportItemDetails(RecipeGraph graph, net.minecraft.item.Item item, String exportDir, String prefix) {
        try {
            Path exportPath = Paths.get(exportDir);
            if (!Files.exists(exportPath)) {
                Files.createDirectories(exportPath);
            }

            String timestamp = DATE_FORMAT.format(new Date());
            String itemId = Registries.ITEM.getId(item).toString();
            String fileName = String.format("%s_%s_details_%s.json",
                    prefix, itemId.replace(':', '_'), timestamp);

            Path filePath = exportPath.resolve(fileName);

            JsonObject itemJson = buildItemDetailsJson(graph, item);

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                GSON.toJson(itemJson, writer);
                writer.flush();
            }

            return filePath.toAbsolutePath().toString();

        } catch (IOException e) {
            System.err.println("导出物品详细信息失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 构建特定物品的详细JSON
     */
    private static JsonObject buildItemDetailsJson(RecipeGraph graph, net.minecraft.item.Item item) {
        JsonObject root = new JsonObject();

        String itemId = Registries.ITEM.getId(item).toString();
        root.addProperty("itemId", itemId);
        root.addProperty("itemName", item.getName().getString());
        root.addProperty("exportTime", new Date().toString());

        // 生产此物品的配方
        List<RecipeNode> producers = graph.getNodesByOutputItem(item);
        JsonArray producersArray = new JsonArray();

        for (RecipeNode producer : producers) {
            JsonObject producerJson = new JsonObject();
            producerJson.addProperty("recipeId", producer.getRecipeId().toString());
            producerJson.addProperty("outputCount", producer.getOutputCount());

            JsonArray inputsArray = new JsonArray();
            for (net.minecraft.item.Item input : producer.getInputItems()) {
                inputsArray.add(Registries.ITEM.getId(input).toString());
            }
            producerJson.add("inputs", inputsArray);

            producersArray.add(producerJson);
        }
        root.add("producingRecipes", producersArray);

        // 消费此物品的配方
        List<RecipeNode> consumers = graph.getNodesByInputItem(item);
        JsonArray consumersArray = new JsonArray();

        for (RecipeNode consumer : consumers) {
            JsonObject consumerJson = new JsonObject();
            consumerJson.addProperty("recipeId", consumer.getRecipeId().toString());
            consumerJson.addProperty("consumesAsInput", true);

            consumersArray.add(consumerJson);
        }
        root.add("consumingRecipes", consumersArray);

        // 相关边
        JsonArray relatedEdges = new JsonArray();
        Set<String> processedEdges = new HashSet<>();

        for (RecipeNode producer : producers) {
            List<RecipeEdge> outgoingEdges = graph.getEdgesFromNode(producer.getRecipeId());
            for (RecipeEdge edge : outgoingEdges) {
                String edgeKey = edge.getFromRecipeId() + "->" + edge.getToRecipeId();
                if (!processedEdges.contains(edgeKey)) {
                    JsonObject edgeJson = buildEdgeJson(edge);
                    relatedEdges.add(edgeJson);
                    processedEdges.add(edgeKey);
                }
            }
        }

        root.add("relatedEdges", relatedEdges);

        return root;
    }

    /**
     * 获取默认导出目录（游戏目录下的 map-craft/exports）
     */
    public static String getDefaultExportDir() {
        return "./map-craft/exports";
    }

    /**
     * 清理旧的导出文件（保留最近N个）
     */
    public static void cleanupOldExports(String exportDir, String prefix, int keepCount) {
        try {
            Path exportPath = Paths.get(exportDir);
            if (!Files.exists(exportPath) || !Files.isDirectory(exportPath)) {
                return;
            }

            // 查找匹配前缀的文件
            List<Path> files = new ArrayList<>();
            try (var stream = Files.list(exportPath)) {
                stream.filter(path -> path.getFileName().toString().startsWith(prefix))
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(files::add);
            }

            // 按修改时间排序（最新的在前）
            files.sort((a, b) -> {
                try {
                    return Long.compare(
                            Files.getLastModifiedTime(b).toMillis(),
                            Files.getLastModifiedTime(a).toMillis()
                    );
                } catch (IOException e) {
                    return 0;
                }
            });

            // 删除超过保留数量的旧文件
            for (int i = keepCount; i < files.size(); i++) {
                try {
                    Files.delete(files.get(i));
                } catch (IOException e) {
                    System.err.println("无法删除旧文件: " + files.get(i));
                }
            }

        } catch (IOException e) {
            System.err.println("清理旧导出文件失败: " + e.getMessage());
        }
    }
}