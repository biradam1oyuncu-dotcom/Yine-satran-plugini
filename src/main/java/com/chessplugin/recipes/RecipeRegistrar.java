package com.chessplugin.recipes;

import com.chessplugin.items.ChessItems;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class RecipeRegistrar {

    private final Plugin plugin;
    private final ChessItems items;

    public RecipeRegistrar(Plugin plugin, ChessItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    public void registerAll() {
        registerPouchRecipe();
        registerBoardRecipe();
    }

    // 1 Derin Kayrak Tasi (Deepslate) + 1 Kuvars Blogu -> "Taslar" kesesi
    private void registerPouchRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "chess_pieces_pouch");
        ItemStack result = items.createPouch();
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        recipe.addIngredient(1, Material.DEEPSLATE);
        recipe.addIngredient(1, Material.QUARTZ_BLOCK);
        plugin.getServer().addRecipe(recipe);
    }

    // D Q D
    // Q D Q
    // P P P   (P = herhangi bir tahta/plank turu)
    private void registerBoardRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "chess_board");
        ItemStack result = items.createBoardItem();
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("DQD", "QDQ", "PPP");
        recipe.setIngredient('D', Material.DEEPSLATE);
        recipe.setIngredient('Q', Material.QUARTZ_BLOCK);

        Set<Material> planks = new HashSet<>();
        try {
            planks.addAll(Tag.PLANKS.getValues());
        } catch (Throwable t) {
            // Tag.PLANKS bu API surumunde yoksa yaygin tahta turlerini manuel ekle
            for (String name : new String[]{
                    "OAK_PLANKS", "SPRUCE_PLANKS", "BIRCH_PLANKS", "JUNGLE_PLANKS",
                    "ACACIA_PLANKS", "DARK_OAK_PLANKS", "MANGROVE_PLANKS", "CHERRY_PLANKS",
                    "BAMBOO_PLANKS", "CRIMSON_PLANKS", "WARPED_PLANKS", "PALE_OAK_PLANKS"}) {
                Material m = Material.matchMaterial(name);
                if (m != null) planks.add(m);
            }
        }
        recipe.setIngredient('P', new RecipeChoice.MaterialChoice(new java.util.ArrayList<>(planks)));

        plugin.getServer().addRecipe(recipe);
    }
}
