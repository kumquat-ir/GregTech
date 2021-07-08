package gregtech.api.recipes.builders;

import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.util.ValidationResult;

//todo make this work
public class AssemblyLineRecipeBuilder extends RecipeBuilder<AssemblyLineRecipeBuilder> {

    protected String data;

    public AssemblyLineRecipeBuilder() {

    }

    public AssemblyLineRecipeBuilder(Recipe recipe, RecipeMap<AssemblyLineRecipeBuilder> recipeMap) {
        super(recipe, recipeMap);
//        this.data = recipe.getRecipePropertyStorage().getRecipePropertyValue(ImplosionExplosiveProperty.getInstance(), ItemStack.EMPTY);
    }

    public AssemblyLineRecipeBuilder(RecipeBuilder<AssemblyLineRecipeBuilder> recipeBuilder) {
        super(recipeBuilder);
    }

    @Override
    public AssemblyLineRecipeBuilder copy() {
        return null;
    }

    @Override
    public ValidationResult<Recipe> build() {
        return null;
    }
}
