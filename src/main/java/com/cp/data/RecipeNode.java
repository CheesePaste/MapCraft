package com.cp.data;

import net.minecraft.item.Item;
import net.minecraft.recipe.Recipe;
import net.minecraft.util.Identifier;
import java.util.*;

public class RecipeNode {
    private final Identifier recipeId;
    private final Recipe<?> recipe;
    private final Set<Item> inputItems;
    private final Item outputItem;
    private final int outputCount;

    // 元数据，用于布局和显示
    private double x, y;
    private boolean visited;

    public RecipeNode(Identifier recipeId, Recipe<?> recipe, Set<Item> inputItems, Item outputItem, int outputCount) {
        this.recipeId = recipeId;
        this.recipe = recipe;
        this.inputItems = Set.copyOf(inputItems);
        this.outputItem = outputItem;
        this.outputCount = outputCount;
        this.x = 0.0;
        this.y = 0.0;
        this.visited = false;
    }

    public Identifier getRecipeId() {
        return recipeId;
    }

    public Recipe<?> getRecipe() {
        return recipe;
    }

    public Set<Item> getInputItems() {
        return inputItems;
    }

    public Item getOutputItem() {
        return outputItem;
    }

    public int getOutputCount() {
        return outputCount;
    }

    // 布局相关方法
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public boolean isVisited() {
        return visited;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeNode that = (RecipeNode) o;
        return Objects.equals(recipeId, that.recipeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipeId);
    }

    @Override
    public String toString() {
        return "RecipeNode{" +
                "recipeId=" + recipeId +
                ", output=" + outputItem.getTranslationKey() +
                ", inputs=" + inputItems.size() +
                '}';
    }
}