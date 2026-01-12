package com.graph;

import net.minecraft.item.Item;
import java.util.ArrayList;
import java.util.List;

public class RecipeGraphData {
    // 存储节点：哪个物品，在哪个位置
    public static class Node {
        public final Item item;
        public double x, y, width, height;

        public Node(Item item, double x, double y, double width, double height) {
            this.item = item;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    // 存储边：从哪个物品连向哪个物品
    public static class Edge {
        public final Item source;
        public final Item target;

        public Edge(Item source, Item target) {
            this.source = source;
            this.target = target;
        }
    }

    public final List<Node> nodes = new ArrayList<>();
    public final List<Edge> edges = new ArrayList<>();
}
