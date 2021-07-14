package gregtech.api.recipes.recipeproperties;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class ScannerDataProperty extends RecipeProperty<String> {

    private static final String KEY = "scanner_data";

    private static ScannerDataProperty INSTANCE;

    private ScannerDataProperty() {
        super(KEY, String.class);
    }

    public static ScannerDataProperty getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ScannerDataProperty();
        }

        return INSTANCE;
    }

    @Override
    public void drawInfo(Minecraft minecraft, int x, int y, int color, Object value) {
        if (((String) value).contains("assemblyline"))
            minecraft.fontRenderer.drawString(I18n.format("gregtech.recipe.scan_target.assemblyline", value), x, y, color);
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
