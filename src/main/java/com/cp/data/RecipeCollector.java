package com.cp.data;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;

public class RecipeCollector {
    private static final Map<Identifier, RecipeNode> nodes = new HashMap<>();
    private static final List<RecipeEdge> edges = new ArrayList<>();

    // 用于快速查找物品对应的配方节点
    private static final Map<Item, List<Identifier>> itemToRecipeMap = new HashMap<>();

    public static void collectAllRecipes(MinecraftServer server) {
        clearData(); // 清空之前的数据
        RecipeManager recipeManager = server.getRecipeManager();

        // 获取所有合成配方
        List<RecipeEntry<CraftingRecipe>> recipes = recipeManager.listAllOfType(RecipeType.CRAFTING);

        // 处理配方
        processRecipes(recipes, server.getRegistryManager());

        // 构建关系图
        buildGraph();
    }

    private static void processRecipes(List<RecipeEntry<CraftingRecipe>> recipes, RegistryWrapper.WrapperLookup registriesLookup) {
        if (recipes == null || recipes.isEmpty()) {
            return;
        }

        for (RecipeEntry<CraftingRecipe> recipeEntry : recipes) {
            try {
                Identifier recipeId = recipeEntry.id();
                CraftingRecipe recipe = recipeEntry.value();

                // 跳过无效或空的配方
                if (recipe == null) {
                    continue;
                }

                // 获取输出物品 - 使用 getResult() 而不是 getOutput()
                ItemStack outputStack = recipe.getResult(registriesLookup);
                if (outputStack.isEmpty()) {
                    continue;
                }

                Item outputItem = outputStack.getItem();
                int outputCount = outputStack.getCount();

                // 获取输入物品集合
                Set<Item> inputItems = extractInputItems(recipe);

                // 创建配方节点
                RecipeNode node = new RecipeNode(recipeId, recipe, inputItems, outputItem, outputCount);
                nodes.put(recipeId, node);

                // 更新物品到配方的映射（用于快速查找）
                updateItemRecipeMap(recipeId, inputItems, outputItem);

            } catch (Exception e) {
                System.err.println("处理配方时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static Set<Item> extractInputItems(CraftingRecipe recipe) {
        Set<Item> inputItems = new HashSet<>();

        try {
            DefaultedList<Ingredient> ingredients = recipe.getIngredients();

            for (Ingredient ingredient : ingredients) {
                if (ingredient.isEmpty()) {
                    continue;
                }

                ItemStack[] matchingStacks = ingredient.getMatchingStacks();
                if (matchingStacks.length > 0) {
                    // 取第一个匹配的物品作为代表（简化处理）
                    Item item = matchingStacks[0].getItem();
                    if (item != null) {
                        inputItems.add(item);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("提取输入物品时出错: " + e.getMessage());
        }

        return inputItems;
    }

    private static void updateItemRecipeMap(Identifier recipeId, Set<Item> inputItems, Item outputItem) {
        // 更新输出物品到配方的映射
        List<Identifier> outputRecipes = itemToRecipeMap.getOrDefault(outputItem, new ArrayList<>());
        outputRecipes.add(recipeId);
        itemToRecipeMap.put(outputItem, outputRecipes);

        // 更新输入物品到配方的映射
        for (Item inputItem : inputItems) {
            List<Identifier> inputRecipes = itemToRecipeMap.getOrDefault(inputItem, new ArrayList<>());
            inputRecipes.add(recipeId);
            itemToRecipeMap.put(inputItem, inputRecipes);
        }
    }

    private static void buildGraph() {
        edges.clear();

        // 第一阶段：构建直接的输入-输出关系
        buildDirectRelationships();

        // 第二阶段：构建间接关系（通过共同物品）
        buildIndirectRelationships();

        // 第三阶段：去重和验证
        deduplicateEdges();

        System.out.println("配方关系图构建完成: " + nodes.size() + " 个节点, " + edges.size() + " 条边");
    }

    private static void buildDirectRelationships() {
        // 遍历所有节点，寻找直接的输入-输出关系
        for (RecipeNode fromNode : nodes.values()) {
            // 获取这个节点的输出物品
            Item outputItem = fromNode.getOutputItem();

            // 找到所有以这个物品作为输入的配方
            List<Identifier> consumingRecipes = itemToRecipeMap.getOrDefault(outputItem, Collections.emptyList());

            for (Identifier toRecipeId : consumingRecipes) {
                RecipeNode toNode = nodes.get(toRecipeId);
                if (toNode != null && !fromNode.getRecipeId().equals(toRecipeId)) {
                    // 创建边：从生产配方到消费配方
                    RecipeEdge edge = new RecipeEdge(
                            fromNode.getRecipeId(),
                            toRecipeId,
                            "DIRECT_CONSUMPTION"
                    );
                    edges.add(edge);

                    // 同时创建反向边（可选）：从消费配方到生产配方
                    RecipeEdge reverseEdge = new RecipeEdge(
                            toRecipeId,
                            fromNode.getRecipeId(),
                            "DIRECT_PRODUCTION"
                    );
                    edges.add(reverseEdge);
                }
            }
        }
    }

    private static void buildIndirectRelationships() {
        // 构建基于共同物品的间接关系
        // 例如：两个配方使用相同的输入物品，或者产生相同的输出物品

        // 1. 寻找共享输入物品的配方（兄弟关系）
        buildSiblingRelationships();

        // 2. 寻找共享输出物品的配方（替代关系）
        buildAlternativeRelationships();

        // 3. 构建链式关系（A->B->C）
        buildChainRelationships();
    }

    private static void buildSiblingRelationships() {
        // 遍历所有物品，找到使用相同输入的配方
        for (Map.Entry<Item, List<Identifier>> entry : itemToRecipeMap.entrySet()) {
            List<Identifier> recipeIds = entry.getValue();

            if (recipeIds.size() > 1) {
                // 为所有使用相同物品的配方之间创建边
                for (int i = 0; i < recipeIds.size(); i++) {
                    for (int j = i + 1; j < recipeIds.size(); j++) {
                        RecipeEdge edge = new RecipeEdge(
                                recipeIds.get(i),
                                recipeIds.get(j),
                                "SHARED_INPUT",
                                0.5 // 降低权重，因为是间接关系
                        );
                        edges.add(edge);

                        // 双向边
                        RecipeEdge reverseEdge = new RecipeEdge(
                                recipeIds.get(j),
                                recipeIds.get(i),
                                "SHARED_INPUT",
                                0.5
                        );
                        edges.add(reverseEdge);
                    }
                }
            }
        }
    }

    private static void buildAlternativeRelationships() {
        // 对于每个物品，找到所有生产它的配方
        for (Map.Entry<Item, List<Identifier>> entry : itemToRecipeMap.entrySet()) {
            // 筛选出生产这个物品的配方
            List<Identifier> producingRecipes = new ArrayList<>();
            for (Identifier recipeId : entry.getValue()) {
                RecipeNode node = nodes.get(recipeId);
                if (node != null && node.getOutputItem().equals(entry.getKey())) {
                    producingRecipes.add(recipeId);
                }
            }

            // 如果多个配方生产同一个物品，它们之间是替代关系
            if (producingRecipes.size() > 1) {
                for (int i = 0; i < producingRecipes.size(); i++) {
                    for (int j = i + 1; j < producingRecipes.size(); j++) {
                        RecipeEdge edge = new RecipeEdge(
                                producingRecipes.get(i),
                                producingRecipes.get(j),
                                "ALTERNATIVE_OUTPUT",
                                0.3 // 替代关系的权重更低
                        );
                        edges.add(edge);

                        RecipeEdge reverseEdge = new RecipeEdge(
                                producingRecipes.get(j),
                                producingRecipes.get(i),
                                "ALTERNATIVE_OUTPUT",
                                0.3
                        );
                        edges.add(reverseEdge);
                    }
                }
            }
        }
    }

    private static void buildChainRelationships() {
        // 寻找长的生产链：A -> B -> C
        // 我们已经有了直接的A->B关系，现在寻找B->C关系来构建A->B->C的认知

        for (RecipeEdge edge1 : new ArrayList<>(edges)) {
            if ("DIRECT_CONSUMPTION".equals(edge1.getRelationshipType())) {
                // 寻找从这个边的目标节点出发的直接消费边
                for (RecipeEdge edge2 : edges) {
                    if ("DIRECT_CONSUMPTION".equals(edge2.getRelationshipType()) &&
                            edge1.getToRecipeId().equals(edge2.getFromRecipeId())) {
                        // 创建间接链式关系
                        RecipeEdge chainEdge = new RecipeEdge(
                                edge1.getFromRecipeId(),
                                edge2.getToRecipeId(),
                                "INDIRECT_CHAIN",
                                0.2 // 间接链式关系的权重很低
                        );
                        edges.add(chainEdge);
                    }
                }
            }
        }
    }

    private static void deduplicateEdges() {
        // 去重：移除重复的边
        Set<RecipeEdge> uniqueEdges = new HashSet<>(edges);
        edges.clear();
        edges.addAll(uniqueEdges);

        // 验证边的有效性：确保边连接的两个节点都存在
        List<RecipeEdge> validEdges = new ArrayList<>();
        for (RecipeEdge edge : edges) {
            if (nodes.containsKey(edge.getFromRecipeId()) &&
                    nodes.containsKey(edge.getToRecipeId())) {
                validEdges.add(edge);
            }
        }
        edges.clear();
        edges.addAll(validEdges);
    }

    private static void clearData() {
        nodes.clear();
        edges.clear();
        itemToRecipeMap.clear();
    }

    public static RecipeGraph getRecipeGraph() {
        return new RecipeGraph(nodes, edges);
    }

    // 用于调试的辅助方法
    public static void printGraphSummary() {
        System.out.println("=== 配方关系图摘要 ===");
        System.out.println("总节点数: " + nodes.size());
        System.out.println("总边数: " + edges.size());

        // 统计不同类型的边
        Map<String, Integer> edgeTypeCount = new HashMap<>();
        for (RecipeEdge edge : edges) {
            String type = edge.getRelationshipType();
            edgeTypeCount.put(type, edgeTypeCount.getOrDefault(type, 0) + 1);
        }

        System.out.println("边类型分布:");
        for (Map.Entry<String, Integer> entry : edgeTypeCount.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
    }
}