// RecipeProcessor.java - 配方处理器
package com.cp.data;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;

public class RecipeProcessor {
    private final RecipeDataManager dataManager;

    public RecipeProcessor(RecipeDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void processAllRecipes(MinecraftServer server) {
        RecipeManager recipeManager = server.getRecipeManager();
        RegistryWrapper.WrapperLookup registries = server.getRegistryManager();

        List<RecipeEntry<CraftingRecipe>> recipes =
                recipeManager.listAllOfType(RecipeType.CRAFTING);

        if (recipes == null || recipes.isEmpty()) return;

        for (RecipeEntry<CraftingRecipe> recipeEntry : recipes) {
            processRecipe(recipeEntry, registries);
        }
    }

    private void processRecipe(RecipeEntry<CraftingRecipe> recipeEntry,
                               RegistryWrapper.WrapperLookup registries) {
        try {
            Identifier recipeId = recipeEntry.id();
            CraftingRecipe recipe = recipeEntry.value();

            if (recipe == null) return;

            ItemStack outputStack = recipe.getResult(registries);
            if (outputStack.isEmpty()) return;

            Item outputItem = outputStack.getItem();
            int outputCount = outputStack.getCount();
            Set<Item> inputItems = extractInputItems(recipe);

            RecipeNode node = new RecipeNode(recipeId, recipe, inputItems, outputItem, outputCount);
            dataManager.addNode(node);

            updateItemMappings(recipeId, inputItems, outputItem);

        } catch (Exception e) {
            System.err.println("处理配方时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Set<Item> extractInputItems(CraftingRecipe recipe) {
        Set<Item> inputItems = new HashSet<>();
        DefaultedList<Ingredient> ingredients = recipe.getIngredients();

        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;

            ItemStack[] matchingStacks = ingredient.getMatchingStacks();
            if (matchingStacks.length > 0) {
                Item item = matchingStacks[0].getItem();
                if (item != null) {
                    inputItems.add(item);
                }
            }
        }
        return inputItems;
    }

    private void updateItemMappings(Identifier recipeId, Set<Item> inputItems, Item outputItem) {
        // 输出物品映射
        dataManager.registerItemRecipeMapping(outputItem, recipeId);

        // 输入物品映射
        for (Item inputItem : inputItems) {
            dataManager.registerItemRecipeMapping(inputItem, recipeId);
        }
    }
}