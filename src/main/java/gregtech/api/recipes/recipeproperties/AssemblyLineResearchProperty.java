package gregtech.api.recipes.recipeproperties;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class AssemblyLineResearchProperty extends RecipeProperty<Boolean>{

    private static final String KEY = "research";

    private static AssemblyLineResearchProperty INSTANCE;


    private AssemblyLineResearchProperty() {
        super(KEY, Boolean.class);
    }

    public static AssemblyLineResearchProperty getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AssemblyLineResearchProperty();
        }

        return INSTANCE;
    }

    @Override
    public void drawInfo(Minecraft minecraft, int x, int y, int color, Object value) {
        if ((boolean) value)
            minecraft.fontRenderer.drawString(I18n.format("gregtech.recipe.research"), x, y, color);
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
