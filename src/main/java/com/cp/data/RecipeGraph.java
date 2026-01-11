package com.cp.data;

import net.minecraft.util.Identifier;
import java.util.*;

public class RecipeGraph {
    private final Map<Identifier, RecipeNode> nodes;
    private final List<RecipeEdge> edges;

    // 图的元数据
    private final long buildTimestamp;
    private int nodeCount;
    private int edgeCount;

    public RecipeGraph(Map<Identifier, RecipeNode> nodes, List<RecipeEdge> edges) {
        this.nodes = Collections.unmodifiableMap(new HashMap<>(nodes));
        this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
        this.buildTimestamp = System.currentTimeMillis();
        this.nodeCount = nodes.size();
        this.edgeCount = edges.size();
    }

    public Map<Identifier, RecipeNode> getNodes() {
        return nodes;
    }

    public List<RecipeEdge> getEdges() {
        return edges;
    }

    public RecipeNode getNodeById(Identifier recipeId) {
        return nodes.get(recipeId);
    }

    public List<RecipeEdge> getEdgesFromNode(Identifier recipeId) {
        List<RecipeEdge> result = new ArrayList<>();
        for (RecipeEdge edge : edges) {
            if (edge.getFromRecipeId().equals(recipeId)) {
                result.add(edge);
            }
        }
        return result;
    }

    public List<RecipeEdge> getEdgesToNode(Identifier recipeId) {
        List<RecipeEdge> result = new ArrayList<>();
        for (RecipeEdge edge : edges) {
            if (edge.getToRecipeId().equals(recipeId)) {
                result.add(edge);
            }
        }
        return result;
    }

    // 获取某个物品作为输入的所有配方节点
    public List<RecipeNode> getNodesByInputItem(net.minecraft.item.Item item) {
        List<RecipeNode> result = new ArrayList<>();
        for (RecipeNode node : nodes.values()) {
            if (node.getInputItems().contains(item)) {
                result.add(node);
            }
        }
        return result;
    }

    // 获取某个物品作为输出的所有配方节点
    public List<RecipeNode> getNodesByOutputItem(net.minecraft.item.Item item) {
        List<RecipeNode> result = new ArrayList<>();
        for (RecipeNode node : nodes.values()) {
            if (node.getOutputItem().equals(item)) {
                result.add(node);
            }
        }
        return result;
    }

    public long getBuildTimestamp() {
        return buildTimestamp;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    // 检查图是否包含循环依赖
    public boolean hasCycles() {
        Set<Identifier> visited = new HashSet<>();
        Set<Identifier> recursionStack = new HashSet<>();

        for (Identifier nodeId : nodes.keySet()) {
            if (hasCycleDFS(nodeId, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycleDFS(Identifier nodeId, Set<Identifier> visited, Set<Identifier> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true;
        }

        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);

        for (RecipeEdge edge : getEdgesFromNode(nodeId)) {
            if (hasCycleDFS(edge.getToRecipeId(), visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }

    @Override
    public String toString() {
        return "RecipeGraph{" +
                "nodes=" + nodeCount +
                ", edges=" + edgeCount +
                ", builtAt=" + new java.util.Date(buildTimestamp) +
                '}';
    }
}