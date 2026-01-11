package com.cp.data;

import net.minecraft.util.Identifier;

import java.util.Objects;

public class RecipeEdge {
    private final Identifier fromRecipeId;
    private final Identifier toRecipeId;
    private final String relationshipType;
    private final double weight;

    // 布局和渲染相关
    private boolean highlighted;

    public RecipeEdge(Identifier fromRecipeId, Identifier toRecipeId, String relationshipType) {
        this(fromRecipeId, toRecipeId, relationshipType, 1.0);
    }

    public RecipeEdge(Identifier fromRecipeId, Identifier toRecipeId, String relationshipType, double weight) {
        this.fromRecipeId = fromRecipeId;
        this.toRecipeId = toRecipeId;
        this.relationshipType = relationshipType;
        this.weight = weight;
        this.highlighted = false;
    }

    public Identifier getFromRecipeId() {
        return fromRecipeId;
    }

    public Identifier getToRecipeId() {
        return toRecipeId;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public double getWeight() {
        return weight;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecipeEdge that = (RecipeEdge) o;
        return Objects.equals(fromRecipeId, that.fromRecipeId) &&
                Objects.equals(toRecipeId, that.toRecipeId) &&
                Objects.equals(relationshipType, that.relationshipType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromRecipeId, toRecipeId, relationshipType);
    }

    @Override
    public String toString() {
        return "RecipeEdge{" +
                "from=" + fromRecipeId +
                ", to=" + toRecipeId +
                ", type='" + relationshipType + '\'' +
                ", weight=" + weight +
                '}';
    }
}