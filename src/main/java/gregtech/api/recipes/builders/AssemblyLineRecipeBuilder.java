package gregtech.api.recipes.builders;

import com.google.common.collect.ImmutableMap;
import gregtech.api.recipes.*;
import gregtech.api.recipes.recipeproperties.AssemblyLineResearchProperty;
import gregtech.api.util.EnumValidationResult;
import gregtech.api.util.GTLog;
import gregtech.api.util.ValidationResult;
import gregtech.common.items.MetaItems;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang3.builder.ToStringBuilder;
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
        //todo handle no research such that items don't go in data slot
//        if (key.equals("no_research")) {
//            noResearch();
//            return true;
//        }
        if (key.equals("research")) {
            researchItem((ItemStack) value);
            return true;
        }
        return false;
    }

    //todo handle no research such that items don't go in data slot
//    @ZenMethod
//    public AssemblyLineRecipeBuilder noResearch() {
//        this.hasResearch = false;
//        return this;
//    }

    @ZenMethod
    public AssemblyLineRecipeBuilder researchItem(ItemStack researchItem) {
        this.hasResearch = true;
        if (researchItem == null) {
            GTLog.logger.error("Assemblyline research data cannot be null", new IllegalArgumentException());
            recipeStatus = EnumValidationResult.INVALID;
        }
        this.researchItem = researchItem;

        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        nbtTagCompound.setString("assemblyline", this.outputs.get(0).getDisplayName());

        ItemStack itemStack = MetaItems.TOOL_DATA_STICK.getStackForm();
        itemStack.setTagCompound(nbtTagCompound);

        CountableIngredient ci = CountableIngredient.from(itemStack, 0);
        this.inputs.add(0, ci);
//        this.notConsumable(itemStack);
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
        //todo make items not go in data stick slot

        super.buildAndRegister();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append(AssemblyLineResearchProperty.getInstance().getKey(), hasResearch)
                .toString();
    }
}
