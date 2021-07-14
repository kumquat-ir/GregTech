package gregtech.api.recipes.builders;

import com.google.common.collect.ImmutableMap;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.recipes.recipeproperties.AssemblyLineResearchProperty;
import gregtech.api.util.EnumValidationResult;
import gregtech.api.util.GTLog;
import gregtech.api.util.ValidationResult;
import gregtech.common.items.MetaItems;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenMethod;

public class AssemblyLineRecipeBuilder extends RecipeBuilder<AssemblyLineRecipeBuilder> {

    protected boolean hasResearch = true;
    protected ItemStack researchItem;

    public AssemblyLineRecipeBuilder() {

    }

    public AssemblyLineRecipeBuilder(Recipe recipe, RecipeMap<AssemblyLineRecipeBuilder> recipeMap) {
        super(recipe, recipeMap);
    }

    public AssemblyLineRecipeBuilder(RecipeBuilder<AssemblyLineRecipeBuilder> recipeBuilder) {
        super(recipeBuilder);
    }

    @Override
    public AssemblyLineRecipeBuilder copy() {
        return new AssemblyLineRecipeBuilder(this);
    }

    @Override
    public boolean applyProperty(String key, Object value) {
        if (key.equals("no_research")) {
            noResearch();
            return true;
        }
        if (key.equals("research")) {
            researchItem((ItemStack) value);
            return true;
        }
        return false;
    }

    @ZenMethod
    public AssemblyLineRecipeBuilder noResearch() {
        this.hasResearch = false;
        return this;
    }

    @ZenMethod
    public AssemblyLineRecipeBuilder researchItem(ItemStack researchItem) {
        this.hasResearch = true;
        if (hasResearch && researchItem == null) {
            GTLog.logger.error("Assemblyline research data cannot be null", new IllegalArgumentException());
            recipeStatus = EnumValidationResult.INVALID;
        }
        this.researchItem = researchItem;
        return this;
    }

    @Override
    public ValidationResult<Recipe> build() {
        Recipe recipe = new Recipe(inputs, outputs, chancedOutputs, fluidInputs, fluidOutputs,
                duration, EUt, hidden);
        if (hasResearch && !recipe.getRecipePropertyStorage().store(ImmutableMap.of(AssemblyLineResearchProperty.getInstance(), hasResearch))) {
            return ValidationResult.newResult(EnumValidationResult.INVALID, recipe);
        }

        return ValidationResult.newResult(finalizeAndValidate(), recipe);
    }

    @Override
    public void buildAndRegister() {
        if (hasResearch && researchItem != null) {
            RecipeMaps.SCANNER_RECIPES.recipeBuilder().duration(144000)
                    .inputs(MetaItems.TOOL_DATA_STICK.getStackForm())
                    .inputs(this.researchItem)
                    .scanData("assemblyline:" + this.outputs.get(0).getDisplayName())
                    .outputs(MetaItems.TOOL_DATA_STICK.getStackForm())
                    .buildAndRegister();
        }

        super.buildAndRegister();
    }
}
