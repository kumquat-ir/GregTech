package gregtech.integration.jei.recipe.primitive;

import com.google.common.collect.ImmutableList;
import gregtech.api.GTValues;
import gregtech.api.gui.GuiTextures;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.*;
import mezz.jei.api.gui.IDrawableAnimated.StartDirection;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class OreByProductCategory extends PrimitiveRecipeCategory<OreByProduct, OreByProduct> {

    protected final IDrawable slot;
    protected final IDrawable arrowsStatic;

    protected final static ImmutableList<Integer> BASIC_PREFIX_LOCATIONS = ImmutableList.of(
            79, 4,      // ore
            79, 29,     // crushed
            92, 54,     // crushedPurified
            117, 54,    // crushedCentrifuged
            41, 54,     // dustImpure
            66, 54,     // dustPure
            79, 126     // dust
    );
    // main locations for byproducts
    protected final static ImmutableList<Integer> BYPRODUCT_LOCATIONS = ImmutableList.of(
            4, 126,
            29, 126,
            54, 126,
            105, 126,
            129, 126,
            154, 126
    );

    public OreByProductCategory(IGuiHelper guiHelper) {
        super("ore_by_product",
                "recipemap.byproductlist.name",
                guiHelper.createBlankDrawable(176, 166),
                guiHelper);

        this.slot = guiHelper.drawableBuilder(GuiTextures.SLOT.imageLocation, 0, 0, 18, 18).setTextureSize(18, 18).build();
        this.arrowsStatic = guiHelper.drawableBuilder(new ResourceLocation(GTValues.MODID + ":textures/gui/arrows/byproducts_static.png"),
                0, 0, 81, 43).setTextureSize(81, 43).build();
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, OreByProduct recipeWrapper, @Nonnull IIngredients ingredients) {
        IGuiItemStackGroup itemStackGroup = recipeLayout.getItemStacks();
        // absolute mess, will rewrite once i start messing with OreByProduct
        for (int i = 0; i < BASIC_PREFIX_LOCATIONS.size(); i+=2) {
            itemStackGroup.init(i, true, BASIC_PREFIX_LOCATIONS.get(i), BASIC_PREFIX_LOCATIONS.get(i + 1));
        }

        if (recipeWrapper.getOutputCount() > BYPRODUCT_LOCATIONS.size()/2) {
            for (int i = 0; i < BYPRODUCT_LOCATIONS.size(); i+=2) {
                itemStackGroup.init(i + BASIC_PREFIX_LOCATIONS.size(), false, BYPRODUCT_LOCATIONS.get(i), BYPRODUCT_LOCATIONS.get(i + 1));
            }
            // todo i guess? overflow past normal slots
        }
        else {
            for (int i = 0; i < recipeWrapper.getOutputCount(); i++) {
                itemStackGroup.init(i + BASIC_PREFIX_LOCATIONS.size(), false, BYPRODUCT_LOCATIONS.get(2 * i), BYPRODUCT_LOCATIONS.get((2 * i) + 1));
            }
        }


        itemStackGroup.addTooltipCallback(recipeWrapper::addTooltip);
        itemStackGroup.set(ingredients);
    }

    @Nonnull
    @Override
    public IRecipeWrapper getRecipeWrapper(@Nonnull OreByProduct recipe) {
        return recipe;
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        this.arrowsStatic.draw(minecraft, 47, 22);

        for (int i = 0; i < BASIC_PREFIX_LOCATIONS.size(); i+=2) {
            this.slot.draw(minecraft, BASIC_PREFIX_LOCATIONS.get(i), BASIC_PREFIX_LOCATIONS.get(i + 1));
        }
        for (int i = 0; i < BYPRODUCT_LOCATIONS.size(); i+=2) {
            this.slot.draw(minecraft, BYPRODUCT_LOCATIONS.get(i), BYPRODUCT_LOCATIONS.get(i + 1));
        }
    }
}
