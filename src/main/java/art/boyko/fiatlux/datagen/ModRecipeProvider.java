package art.boyko.fiatlux.datagen;

import java.util.concurrent.CompletableFuture;

import art.boyko.fiatlux.server.FiatLux;
import art.boyko.fiatlux.server.init.ModBlocks;
import art.boyko.fiatlux.server.init.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class ModRecipeProvider extends RecipeProvider {
    
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        // Shapeless recipes
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.EXAMPLE_ITEM.get(), 4)
                .requires(Items.APPLE)
                .requires(Items.BREAD)
                .unlockedBy("has_apple", has(Items.APPLE))
                .save(recipeOutput);

        // Shaped recipes
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.EXAMPLE_BLOCK.get())
                .pattern("XXX")
                .pattern("XYX")
                .pattern("XXX")
                .define('X', Items.STONE)
                .define('Y', Items.DIAMOND)
                .unlockedBy("has_stone", has(Items.STONE))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.LIGHT_BLOCK.get())
                .pattern("GGG")
                .pattern("GTG")
                .pattern("GGG")
                .define('G', Items.GLOWSTONE_DUST)
                .define('T', Items.TORCH)
                .unlockedBy("has_glowstone", has(Items.GLOWSTONE_DUST))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.LIGHT_SWORD.get())
                .pattern(" L ")
                .pattern(" L ")
                .pattern(" S ")
                .define('L', ModItems.LIGHT_CRYSTAL.get())
                .define('S', Items.STICK)
                .unlockedBy("has_light_crystal", has(ModItems.LIGHT_CRYSTAL.get()))
                .save(recipeOutput);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.COMPRESSED_COAL.get())
                .pattern("CCC")
                .pattern("CCC")
                .pattern("CCC")
                .define('C', Items.COAL)
                .unlockedBy("has_coal", has(Items.COAL))
                .save(recipeOutput);

        // Smelting recipes
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.DIAMOND), RecipeCategory.MISC, 
                ModItems.LIGHT_CRYSTAL.get(), 1.0f, 200)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(recipeOutput, FiatLux.MODID + ":light_crystal_from_smelting");

        // Magic gem recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MAGIC_GEM.get())
                .pattern(" E ")
                .pattern("EDE")
                .pattern(" E ")
                .define('E', Items.EMERALD)
                .define('D', Items.DIAMOND)
                .unlockedBy("has_emerald", has(Items.EMERALD))
                .save(recipeOutput);

        // Decorative block recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DECORATIVE_BLOCK.get(), 4)
                .pattern("WW")
                .pattern("WW")
                .define('W', Items.OAK_PLANKS)
                .unlockedBy("has_planks", has(Items.OAK_PLANKS))
                .save(recipeOutput);

        // Reinforced block recipe 
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.REINFORCED_BLOCK.get())
                .pattern("ONO")
                .pattern("NEN")
                .pattern("ONO")
                .define('O', Items.OBSIDIAN)
                .define('N', Items.NETHERITE_INGOT)
                .define('E', Items.END_CRYSTAL)
                .unlockedBy("has_netherite", has(Items.NETHERITE_INGOT))
                .save(recipeOutput);

        // Eternal torch recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.TORCH_ITEM.get())
                .pattern(" G ")
                .pattern(" T ")
                .pattern(" S ")
                .define('G', ModItems.MAGIC_GEM.get())
                .define('T', Items.TORCH)
                .define('S', Items.STICK)
                .unlockedBy("has_magic_gem", has(ModItems.MAGIC_GEM.get()))
                .save(recipeOutput);

        // New recipes for blocks with BlockEntity
        // Simple storage block recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SIMPLE_STORAGE_BLOCK.get())
                .pattern("WWW")
                .pattern("WCW")
                .pattern("WWW")
                .define('W', Items.OAK_PLANKS)
                .define('C', Items.CHEST)
                .unlockedBy("has_chest", has(Items.CHEST))
                .save(recipeOutput);

        // Energy storage block recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ENERGY_STORAGE_BLOCK.get())
                .pattern("RGR")
                .pattern("GMG")
                .pattern("RGR")
                .define('R', Items.REDSTONE)
                .define('G', Items.GOLD_INGOT)
                .define('M', ModItems.MAGIC_GEM.get())
                .unlockedBy("has_magic_gem", has(ModItems.MAGIC_GEM.get()))
                .save(recipeOutput);

        // MechaGrid block recipe - expensive but powerful
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MECHA_GRID_BLOCK.get())
                .pattern("DGD")
                .pattern("GMG")
                .pattern("DGD")
                .define('D', Items.DIAMOND)
                .define('G', Items.GLASS)
                .define('M', ModItems.MAGIC_GEM.get())
                .unlockedBy("has_magic_gem", has(ModItems.MAGIC_GEM.get()))
                .save(recipeOutput);
    }
}