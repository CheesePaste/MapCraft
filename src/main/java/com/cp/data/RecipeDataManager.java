// RecipeDataManager.java - 数据管理器
package com.cp.data;

import net.minecraft.item.Item;
import net.minecraft.recipe.Recipe;
import net.minecraft.util.Identifier;

import java.util.*;

public class RecipeDataManager {
    private final Map<Identifier, RecipeNode> nodes = new HashMap<>();
    private final List<RecipeEdge> edges = new ArrayList<>();
    private final Map<Item, List<Identifier>> itemToRecipeMap = new HashMap<>();
    private final Set<String> processedEdges = new HashSet<>();
    private final Map<Item, Set<Item>> itemDependencyGraph = new HashMap<>();

    public void clear() {
        nodes.clear();
        edges.clear();
        itemToRecipeMap.clear();
        processedEdges.clear();
        itemDependencyGraph.clear();
    }

    public void addNode(RecipeNode node) {
        nodes.put(node.getRecipeId(), node);
    }

    public void addEdge(RecipeEdge edge) {
        edges.add(edge);
    }

    public void registerItemRecipeMapping(Item item, Identifier recipeId) {
        List<Identifier> recipes = itemToRecipeMap.computeIfAbsent(item, k -> new ArrayList<>());
        if (!recipes.contains(recipeId)) {
            recipes.add(recipeId);
        }
    }

    public RecipeNode getNode(Identifier recipeId) {
        return nodes.get(recipeId);
    }

    public Collection<RecipeNode> getAllNodes() {
        return nodes.values();
    }

    public List<Identifier> getRecipesForItem(Item item) {
        return itemToRecipeMap.getOrDefault(item, Collections.emptyList());
    }

    public boolean hasEdge(String edgeKey) {
        return processedEdges.contains(edgeKey);
    }

    public void markEdgeProcessed(String edgeKey) {
        processedEdges.add(edgeKey);
    }

    public boolean addDependency(Item fromItem, Item toItem) {
        Set<Item> deps = itemDependencyGraph.computeIfAbsent(fromItem, k -> new HashSet<>());
        return deps.add(toItem);
    }

    public boolean hasDependencyPath(Item start, Item target) {
        return new CycleDetector(this).hasDependencyPath(start, target);
    }

    public RecipeGraph createRecipeGraph() {
        return new RecipeGraph(
                Collections.unmodifiableMap(new HashMap<>(nodes)),
                Collections.unmodifiableList(new ArrayList<>(edges))
        );
    }

    // 内部类：循环检测器
    private static class CycleDetector {
        private final RecipeDataManager dataManager;

        CycleDetector(RecipeDataManager dataManager) {
            this.dataManager = dataManager;
        }

        boolean hasDependencyPath(Item start, Item target) {
            Set<Item> visited = new HashSet<>();
            return hasDependencyPathDFS(start, target, visited);
        }

        private boolean hasDependencyPathDFS(Item current, Item target, Set<Item> visited) {
            if (current.equals(target)) return true;
            if (visited.contains(current)) return false;

            visited.add(current);
            Set<Item> dependencies = dataManager.itemDependencyGraph.get(current);
            if (dependencies == null) return false;

            for (Item dep : dependencies) {
                if (hasDependencyPathDFS(dep, target, visited)) {
                    return true;
                }
            }
            return false;
        }
    }
}