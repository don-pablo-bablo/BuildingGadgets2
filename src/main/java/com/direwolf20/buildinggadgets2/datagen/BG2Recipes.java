package com.direwolf20.buildinggadgets2.datagen;

import com.direwolf20.buildinggadgets2.setup.Registration;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.MultiRegistryBootstrap;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.Tags;

public class BG2Recipes extends RecipeProvider {

    protected BG2Recipes(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes() {
        // Gadgets
        shaped(RecipeCategory.MISC, Registration.Building_Gadget.get())
                .pattern("iri")
                .pattern("drd")
                .pattern("ili")
                .define('r', Tags.Items.DUSTS_REDSTONE)
                .define('i', Tags.Items.INGOTS_IRON)
                .define('d', Tags.Items.GEMS_DIAMOND)
                .define('l', Tags.Items.GEMS_LAPIS)
                .group("buildinggadgets2")
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(output);

        shaped(RecipeCategory.MISC, Registration.Exchanging_Gadget.get())
                .pattern("iri")
                .pattern("dld")
                .pattern("ili")
                .define('r', Tags.Items.DUSTS_REDSTONE)
                .define('i', Tags.Items.INGOTS_IRON)
                .define('d', Tags.Items.GEMS_DIAMOND)
                .define('l', Tags.Items.GEMS_LAPIS)
                .group("buildinggadgets2")
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(output);

        shaped(RecipeCategory.MISC, Registration.CopyPaste_Gadget.get())
                .pattern("iri")
                .pattern("ere")
                .pattern("ili")
                .define('r', Tags.Items.DUSTS_REDSTONE)
                .define('i', Tags.Items.INGOTS_IRON)
                .define('e', Tags.Items.GEMS_EMERALD)
                .define('l', Tags.Items.GEMS_LAPIS)
                .group("buildinggadgets2")
                .unlockedBy("has_emerald", has(Items.EMERALD))
                .save(output);

        shaped(RecipeCategory.MISC, Registration.Destruction_Gadget.get())
                .pattern("iri")
                .pattern("ere")
                .pattern("ili")
                .define('r', Tags.Items.DUSTS_REDSTONE)
                .define('i', Tags.Items.INGOTS_IRON)
                .define('e', Tags.Items.ENDER_PEARLS)
                .define('l', Tags.Items.GEMS_LAPIS)
                .group("buildinggadgets2")
                .unlockedBy("has_ender_pearl", has(Items.ENDER_PEARL))
                .save(output);

        shaped(RecipeCategory.MISC, Registration.CutPaste_Gadget.get())
                .pattern("iri")
                .pattern("srs")
                .pattern("ili")
                .define('r', Tags.Items.DUSTS_REDSTONE)
                .define('i', Tags.Items.INGOTS_IRON)
                .define('s', Items.SHEARS)
                .define('l', Tags.Items.GEMS_LAPIS)
                .group("buildinggadgets2")
                .unlockedBy("has_shear", has(Items.SHEARS))
                .save(output);

        // Blocks
        shaped(RecipeCategory.MISC, Registration.TemplateManager.get())
                .pattern("iri")
                .pattern("prp")
                .pattern("ili")
                .define('r', Tags.Items.DUSTS_REDSTONE)
                .define('i', Tags.Items.INGOTS_IRON)
                .define('p', Items.PAPER)
                .define('l', Tags.Items.GEMS_LAPIS)
                .group("buildinggadgets2")
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output);
    }

    public static MultiRegistryBootstrap bootstrap() {
        return RecipeProvider.asBootstrap(BG2Recipes::new);
    }
}
