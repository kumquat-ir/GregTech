package gregtech.api.recipes.builders;

import com.google.common.collect.ImmutableMap;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeBuilder;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.recipeproperties.ScannerDataProperty;
import gregtech.api.util.EnumValidationResult;
import gregtech.api.util.GTLog;
import gregtech.api.util.ValidationResult;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang3.builder.ToStringBuilder;
import stanhebben.zenscript.annotations.ZenMethod;

public class ScannerRecipeBuilder extends RecipeBuilder<ScannerRecipeBuilder> {

    protected String nbtTag;

    public ScannerRecipeBuilder() {

    }

    public ScannerRecipeBuilder(Recipe recipe, RecipeMap<ScannerRecipeBuilder> recipeMap) {
        super(recipe, recipeMap);

    }

    public ScannerRecipeBuilder(RecipeBuilder<ScannerRecipeBuilder> recipeBuilder) {
        super(recipeBuilder);
    }

    @Override
    public ScannerRecipeBuilder copy() {
        return new ScannerRecipeBuilder(this);
    }

    @Override
    public boolean applyProperty(String key, Object value) {
        if (key.equals("scanner_data")) {
            scanData((String) value);
            return true;
        }
        return false;
    }

    //todo make scanner output a fake corresponding assemblyline output item
    @ZenMethod
    public ScannerRecipeBuilder scanData(String nbtTag) {
        if (nbtTag == null) {
            GTLog.logger.error("Scanner data cannot be null", new IllegalArgumentException());
            recipeStatus = EnumValidationResult.INVALID;
        }
        this.nbtTag = nbtTag;
        return this;
    }

    @Override
    public ValidationResult<Recipe> build() {

        // apply nbt tag and data stick output
        if (nbtTag != null) {
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            String[] parts = nbtTag.split(":");
            nbtTagCompound.setString(parts[0], parts[1]);

            ItemStack output = outputs.get(0);
            output.setTagCompound(nbtTagCompound);

            outputs.set(0, output);
        }


        Recipe recipe = new Recipe(inputs, outputs, chancedOutputs, fluidInputs, fluidOutputs,
                duration, EUt, hidden);

        if (!recipe.getRecipePropertyStorage().store(ImmutableMap.of(ScannerDataProperty.getInstance(), nbtTag))) {
            return ValidationResult.newResult(EnumValidationResult.INVALID, recipe);
        }

        return ValidationResult.newResult(finalizeAndValidate(), recipe);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append(ScannerDataProperty.getInstance().getKey(), nbtTag)
                .toString();
    }
}
