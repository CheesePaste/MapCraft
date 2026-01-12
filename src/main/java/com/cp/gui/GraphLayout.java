package com.cp.gui;

import com.cp.data.RecipeEdge;
import com.cp.data.RecipeGraph;
import com.cp.data.RecipeNode;
import java.util.Random;

public class GraphLayout {
    private final RecipeGraph graph;
    private final double repulsion = 8000.0;  // 增大斥力
    private final double attraction = 0.05;   // 减小引力
    private final double damping = 0.85;      // 阻尼系数 (0-1)，越小越快稳定
    private final double minDistance = 50.0;  // 最小距离限制，防止斥力过大

    // 每个节点的速度
    private double[] vx;
    private double[] vy;

    public GraphLayout(RecipeGraph graph) {
        this.graph = graph;
        initPositions();
    }

    private void initPositions() {
        Random random = new Random();
        int nodeCount = graph.getNodes().size();
        vx = new double[nodeCount];
        vy = new double[nodeCount];

        int i = 0;
        for (RecipeNode node : graph.getNodes().values()) {
            // 使用更大的初始范围，减少拥挤
            node.setPosition(random.nextDouble() * 1000 - 500, random.nextDouble() * 1000 - 500);
            vx[i] = 0;
            vy[i] = 0;
            i++;
        }
    }

    // 每一帧更新节点位置（带速度的增量计算）
    public void step() {
        int i = 0;
        double[] forcesX = new double[graph.getNodes().size()];
        double[] forcesY = new double[graph.getNodes().size()];

        // 1. 计算斥力 (所有节点之间)
        RecipeNode[] nodes = graph.getNodes().values().toArray(new RecipeNode[0]);
        for (int n1 = 0; n1 < nodes.length; n1++) {
            for (int n2 = 0; n2 < nodes.length; n2++) {
                if (n1 == n2) continue;

                double dx = nodes[n1].getX() - nodes[n2].getX();
                double dy = nodes[n1].getY() - nodes[n2].getY();
                double dist = Math.sqrt(dx * dx + dy * dy);

                // 限制最小距离，防止斥力过大
                dist = Math.max(dist, minDistance);

                double force = repulsion / (dist * dist);
                forcesX[n1] += (dx / dist) * force;
                forcesY[n1] += (dy / dist) * force;
            }
        }

        // 2. 计算引力 (仅有边连接的节点)
        for (RecipeEdge edge : graph.getEdges()) {
            RecipeNode source = graph.getNodeById(edge.getFromRecipeId());
            RecipeNode target = graph.getNodeById(edge.getToRecipeId());
            if (source == null || target == null) continue;

            double dx = target.getX() - source.getX();
            double dy = target.getY() - source.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);

            // 使用更平滑的引力计算
            double force = attraction * Math.log(dist / 50.0 + 1);

            int sourceIndex = getNodeIndex(source);
            int targetIndex = getNodeIndex(target);

            if (sourceIndex >= 0) {
                forcesX[sourceIndex] += (dx / dist) * force;
                forcesY[sourceIndex] += (dy / dist) * force;
            }
            if (targetIndex >= 0) {
                forcesX[targetIndex] -= (dx / dist) * force;
                forcesY[targetIndex] -= (dy / dist) * force;
            }
        }

        // 3. 应用速度和阻尼
        i = 0;
        for (RecipeNode node : graph.getNodes().values()) {
            // 更新速度
            vx[i] = (vx[i] + forcesX[i]) * damping;
            vy[i] = (vy[i] + forcesY[i]) * damping;

            // 限制最大速度，防止飞出
            double speed = Math.sqrt(vx[i] * vx[i] + vy[i] * vy[i]);
            double maxSpeed = 10.0;
            if (speed > maxSpeed) {
                vx[i] = (vx[i] / speed) * maxSpeed;
                vy[i] = (vy[i] / speed) * maxSpeed;
            }

            // 更新位置
            node.setPosition(node.getX() + vx[i], node.getY() + vy[i]);
            i++;
        }
    }

    private int getNodeIndex(RecipeNode node) {
        int i = 0;
        for (RecipeNode n : graph.getNodes().values()) {
            if (n == node) return i;
            i++;
        }
        return -1;
    }
}
